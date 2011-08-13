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
package org.logicprobe.LogicMail.mail;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import net.rim.device.api.collection.util.BigVector;
import net.rim.device.api.util.Comparator;

import org.logicprobe.LogicMail.LogicMailResource;
import org.logicprobe.LogicMail.mail.imap.ImapClient;
import org.logicprobe.LogicMail.message.FolderMessage;

class ImapFolderRefreshRequest extends NetworkMailStoreRequest implements MailStoreRequest {
    private final String statusMessage;
    private final FolderTreeItem folder;
    private final Hashtable loadedMessageMap;
    private int messageRetentionLimit;
    private volatile boolean checkAllTokens;
    private Vector secondaryMessageTokensToFetch;
    
    ImapFolderRefreshRequest(NetworkMailStore mailStore, FolderTreeItem folder, FolderMessage[] loadedMessages) {
        super(mailStore);
        this.statusMessage = resources.getString(LogicMailResource.MAILCONNECTION_REQUEST_FOLDER_MESSAGES);
        this.folder = folder;
        this.loadedMessageMap = new Hashtable();
        if(loadedMessages != null) {
            for(int i=0; i<loadedMessages.length; i++) {
                loadedMessageMap.put(
                        loadedMessages[i].getMessageToken().getMessageUid(),
                        loadedMessages[i]);
            }
        }
    }
    
    private FolderListener folderListener = new FolderListener() {
        public void folderMessagesAvailable(FolderMessagesEvent e) { }
        public void folderStatusChanged(FolderEvent e) { }
        public void folderExpunged(FolderExpungedEvent e) { }
        public void folderRefreshRequired(FolderEvent e) {
            if(e.getFolder().equals(folder)){
                checkAllTokens = true;
            }
        }
    };
    
    protected String getInitialStatus() {
        return statusMessage + "...";
    }

    public void execute(MailClient client) throws IOException, MailException {
        ImapClient incomingClient = (ImapClient)client;
        
        mailStore.addFolderListener(folderListener);
        
        this.messageRetentionLimit = incomingClient.getAcctConfig().getMaximumFolderMessages();
        
        checkActiveFolder(incomingClient, folder);

        // Fetch new folder messages from the mail store
        Vector folderMessages = new Vector();
        incomingClient.getNewFolderMessages(
                true,
                new GetFolderMessageCallback(folderMessages, true),
                getProgressHandler(statusMessage));
        
        initialFlagsRefreshComplete(incomingClient, folderMessages);
    }

    protected void fireMailStoreRequestComplete() {
        mailStore.removeFolderListener(folderListener);
        super.fireMailStoreRequestComplete();
    }
    
    protected void fireMailStoreRequestFailed(Throwable exception, boolean isFinal) {
        mailStore.removeFolderListener(folderListener);
        super.fireMailStoreRequestFailed(exception, isFinal);
    }
    
    private void initialFlagsRefreshComplete(ImapClient incomingClient, Vector pendingFlagUpdates) throws IOException, MailException {
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
            if(loadedMessageMap.remove(token.getMessageUid()) == null) {
                // Anything that wasn't in the set needs to be fetched
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
        messageRetentionLimit -= size;
        pendingFlagUpdates.removeAllElements();
        
        // Build a collection of messages in the cache that still need to be verified
        Vector cachedTokensToCheck = new Vector();
        if(checkAllTokens) {
            for(Enumeration e = loadedMessageMap.elements(); e.hasMoreElements() ;) {
                MessageToken token = ((FolderMessage)e.nextElement()).getMessageToken();
                cachedTokensToCheck.addElement(token);
            }
        }
        else if(oldestFetchedToken != null) {
            for(Enumeration e = loadedMessageMap.elements(); e.hasMoreElements() ;) {
                MessageToken token = ((FolderMessage)e.nextElement()).getMessageToken();
                if(tokenComparator.compare(token, oldestFetchedToken) < 0) {
                    cachedTokensToCheck.addElement(token);
                }
            }
        }
        checkAllTokens = false;
        
        if(cachedTokensToCheck.size() > 0 && messageRetentionLimit > 0) {
            // Perform a second flags fetch
            MessageToken[] tokens = new MessageToken[cachedTokensToCheck.size()];
            cachedTokensToCheck.copyInto(tokens);
            
            Vector folderMessages = new Vector();
            incomingClient.getFolderMessages(
                    tokens,
                    true,
                    new GetFolderMessageCallback(folderMessages, true),
                    getProgressHandler(statusMessage));
            
            secondaryFlagsRefreshComplete(incomingClient, folderMessages);
        }
        else {
            removeOrphanedMessages();
            finalFolderMessageFetch(incomingClient);
        }
    }
    
    private void secondaryFlagsRefreshComplete(ImapClient incomingClient, Vector pendingFlagUpdates) throws IOException, MailException {
        int size = pendingFlagUpdates.size();
        BigVector messagesUpdated = new BigVector(size);
        Comparator folderMessageComparator = FolderMessage.getComparator();
        for(int i=0; i<size; i++) {
            FolderMessage message = (FolderMessage)pendingFlagUpdates.elementAt(i);
            MessageToken token = message.getMessageToken();
            
            // Remove messages with received flag updates from the orphan set
            if(loadedMessageMap.remove(token.getMessageUid()) != null) {
                // If it was removed from the set, then we assume it exists
                // in the cache and will have been updated accordingly.
                messagesUpdated.insertElement(folderMessageComparator, message);
            }
        }
        pendingFlagUpdates.removeAllElements();
        
        // Determine the how many messages from this secondary set we can keep
        size = messagesUpdated.size();
        if(size > messageRetentionLimit) {
            // We have too many additional messages, so we need to prune the set
            messagesUpdated.optimize();

            int splitIndex = messagesUpdated.size() - messageRetentionLimit;
            for(int i=0; i<splitIndex; i++) {
                FolderMessage message = (FolderMessage)messagesUpdated.elementAt(i);
                loadedMessageMap.put(message.getMessageToken().getMessageUid(), message);
            }
        }
        
        removeOrphanedMessages();
        finalFolderMessageFetch(incomingClient);
    }

    private void finalFolderMessageFetch(ImapClient incomingClient) throws IOException, MailException {
        // Queue a fetch for messages missing from the cache
        if(!secondaryMessageTokensToFetch.isEmpty()) {
            MessageToken[] fetchArray = new MessageToken[secondaryMessageTokensToFetch.size()];
            secondaryMessageTokensToFetch.copyInto(fetchArray);
            secondaryMessageTokensToFetch.removeAllElements();
            
            incomingClient.getFolderMessages(
                    fetchArray,
                    false,
                    new GetFolderMessageCallback(false),
                    getProgressHandler(statusMessage));
        }
        loadedMessageMap.clear();
        fireMailStoreRequestComplete();
    }
    
    private void removeOrphanedMessages() {
        Enumeration e = loadedMessageMap.elements();
        MessageToken[] orphanedTokens = new MessageToken[loadedMessageMap.size()];
        int index = 0;
        while(e.hasMoreElements()) {
            FolderMessage message = (FolderMessage)e.nextElement();
            orphanedTokens[index++] = message.getMessageToken();
        }
        loadedMessageMap.clear();
        mailStore.fireFolderExpunged(folder, orphanedTokens, new MessageToken[0]);
    }

    private class GetFolderMessageCallback implements FolderMessageCallback {
        private final Vector folderMessages;
        private final boolean flagsOnly;
        public GetFolderMessageCallback(Vector folderMessages, boolean flagsOnly) {
            this.folderMessages = folderMessages;
            this.flagsOnly = flagsOnly;
        }
        public GetFolderMessageCallback(boolean flagsOnly) {
            this(null, flagsOnly);
        }

        public void folderMessageUpdate(FolderMessage folderMessage) {
            if(folderMessage != null) {
                if(folderMessages != null) {
                    folderMessages.addElement(folderMessage);
                }
                mailStore.fireFolderMessagesAvailable(folder, new FolderMessage[] { folderMessage }, flagsOnly);
            }
            else {
                // This is the last update of the sequence
                mailStore.fireFolderMessagesAvailable(folder, null, flagsOnly);
            }
        }
    };
}