/*-
 * Copyright (c) 2011, Derek Konigsberg
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution. 
 * 3. Neither the name of the project nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.logicprobe.LogicMail.model;

import java.util.Enumeration;
import java.util.Vector;

import net.rim.device.api.collection.util.BigVector;
import net.rim.device.api.util.Comparator;

import org.logicprobe.LogicMail.mail.FolderMessagesRequest;
import org.logicprobe.LogicMail.mail.FolderTreeItem;
import org.logicprobe.LogicMail.mail.MailStoreRequest;
import org.logicprobe.LogicMail.mail.MessageToken;
import org.logicprobe.LogicMail.mail.NetworkMailStore;
import org.logicprobe.LogicMail.message.FolderMessage;

/**
 * Handles folder-oriented requests for the mail store services layer, with
 * logic specific to the behavior of IMAP mail store folders.
 * <p>
 * IMAP folder refresh is a three-part operation consisting of two flag fetches
 * and a message header fetch.
 * </p>
 */
class ImapFolderRequestHandler extends FolderRequestHandler {
    /**
     * Flag that is set during an existing refresh to force a check of
     * all tokens.
     */
    private volatile boolean checkAllTokens;
    
    private Vector secondaryMessageTokensToFetch;

    public ImapFolderRequestHandler(
            NetworkMailStoreServices mailStoreServices,
            NetworkMailStore mailStore,
            FolderMessageCache folderMessageCache,
            FolderTreeItem folderTreeItem) {
        super(mailStoreServices, mailStore, folderMessageCache, folderTreeItem);
    }

    protected void prepareForUse() {
        super.prepareForUse();
        if(cleanPriorToUse) {
            secondaryMessageTokensToFetch = null;
        }
    }
    
    public void requestFolderRefreshRequired() {
        if(refreshInProgress.get()) {
            checkAllTokens = true;
        }
        else {
            cleanBeforeNextUse();
        }
    }
    
    protected void beginFolderRefreshOperation() {
        mailStoreServices.invokeLater(new Runnable() { public void run() {
            // Queue a request for new folder messages from the mail store
            processMailStoreRequest(mailStore.createFolderMessagesRecentRequest(folderTreeItem, true)
                    .setRequestCallback(new FolderRefreshRequestCallback() {
                        public void mailStoreRequestComplete(MailStoreRequest request) {
                            FolderMessagesRequest folderMessagesRequest = (FolderMessagesRequest)request;
                            Vector folderMessages = folderMessagesRequest.getResultFolderMessages();
                            initialFlagsRefreshComplete(folderMessages);
                        }
                    }));

            if(!initialRefreshComplete) {
                // Fetch messages stored in cache
                loadCachedFolderMessages();
            }
        }});
    }
    
    private void initialFlagsRefreshComplete(final Vector pendingFlagUpdates) {
        mailStoreServices.invokeLater(new Runnable() { public void run() {
            secondaryMessageTokensToFetch = new Vector();
            MessageToken oldestFetchedToken = null;
            Comparator tokenComparator = null;
            
            // Iterate through the pending flag updates, doing the following:
            // - Remove messages from the orphan set that exist on the server
            // - Update the cache for fetched messages
            // - Build a collection of messages to provide update notifications for
            // - Build a collection of messages that need to be fetched
            // - Keep track of the oldest message in the update set
            int size = pendingFlagUpdates.size();
            for(int i=0; i<size; i++) {
                FolderMessage message = (FolderMessage)pendingFlagUpdates.elementAt(i);
                MessageToken token = message.getMessageToken();
                
                // Remove messages with received flag updates from the orphan set
                orphanedMessageSet.remove(token.getMessageUid());
                
                if(!folderMessageCache.updateFolderMessage(folderTreeItem, message)) {
                    secondaryMessageTokensToFetch.addElement(token);
                }
                
                if(oldestFetchedToken == null) {
                    oldestFetchedToken = token;
                    tokenComparator = token.getComparator();
                }
                else {
                    if(tokenComparator.compare(token, oldestFetchedToken) < 0) {
                        oldestFetchedToken = token;
                    }
                }
            }
            pendingFlagUpdates.removeAllElements();
            
            // Build a collection of messages in the cache that still need to be verified
            Vector cachedTokensToCheck = new Vector();
            if(checkAllTokens) {
                for(Enumeration e = orphanedMessageSet.elements(); e.hasMoreElements() ;) {
                    MessageToken token = ((FolderMessage)e.nextElement()).getMessageToken();
                    cachedTokensToCheck.addElement(token);
                }
            }
            else if(oldestFetchedToken != null) {
                for(Enumeration e = orphanedMessageSet.elements(); e.hasMoreElements() ;) {
                    MessageToken token = ((FolderMessage)e.nextElement()).getMessageToken();
                    if(tokenComparator.compare(token, oldestFetchedToken) < 0) {
                        cachedTokensToCheck.addElement(token);
                    }
                }
            }
            checkAllTokens = false;
            
            if(cachedTokensToCheck.size() > 0) {
                // Perform a second flags fetch
                MessageToken[] tokens = new MessageToken[cachedTokensToCheck.size()];
                cachedTokensToCheck.copyInto(tokens);
                processMailStoreRequest(mailStore.createFolderMessagesSetRequest(folderTreeItem, tokens, true)
                        .setRequestCallback(new FolderRefreshRequestCallback() {
                            public void mailStoreRequestComplete(MailStoreRequest request) {
                                FolderMessagesRequest folderMessagesRequest = (FolderMessagesRequest)request;
                                Vector folderMessages = folderMessagesRequest.getResultFolderMessages();
                                secondaryFlagsRefreshComplete(folderMessages);
                            }
                        }));
            }
            else {
                removeOrphanedMessages();
                finalFolderMessageFetch();
            }
        }});
    }
    
    private void secondaryFlagsRefreshComplete(Vector pendingFlagUpdates) {
        int size = pendingFlagUpdates.size();
        BigVector messagesUpdated = new BigVector(size);
        Comparator folderMessageComparator = FolderMessage.getComparator();
        for(int i=0; i<size; i++) {
            FolderMessage message = (FolderMessage)pendingFlagUpdates.elementAt(i);
            MessageToken token = message.getMessageToken();
            
            // Remove messages with received flag updates from the orphan set
            orphanedMessageSet.remove(token.getMessageUid());
            
            if(folderMessageCache.updateFolderMessage(folderTreeItem, message)) {
                messagesUpdated.insertElement(folderMessageComparator, message);
            }
        }
        pendingFlagUpdates.removeAllElements();
        
        // Determine the how many messages from this secondary set we can keep
        int initialMessageLimit = mailStore.getAccountConfig().getInitialFolderMessages();
        int messageRetentionLimit = mailStore.getAccountConfig().getMaximumFolderMessages();
        int additionalMessageLimit = messageRetentionLimit - initialMessageLimit;
        
        size = messagesUpdated.size();
        if(size > additionalMessageLimit) {
            // We have too many additional messages, so we need to prune the set
            messagesUpdated.optimize();

            int splitIndex = messagesUpdated.size() - additionalMessageLimit;
            for(int i=0; i<splitIndex; i++) {
                FolderMessage message = (FolderMessage)messagesUpdated.elementAt(i);
                orphanedMessageSet.put(message.getMessageToken().getMessageUid(), message);
            }
        }
        
        removeOrphanedMessages();
        finalFolderMessageFetch();
    }

    private void finalFolderMessageFetch() {
        // Queue a fetch for messages missing from the cache
        if(!secondaryMessageTokensToFetch.isEmpty()) {
            MessageToken[] fetchArray = new MessageToken[secondaryMessageTokensToFetch.size()];
            secondaryMessageTokensToFetch.copyInto(fetchArray);
            secondaryMessageTokensToFetch.removeAllElements();
            processMailStoreRequest(mailStore.createFolderMessagesSetRequest(folderTreeItem, fetchArray)
                    .setRequestCallback(finalFetchCallback));
        }
        else {
            initialRefreshComplete = true;
            endFolderRefreshOperation(true);
        }
    }
}
