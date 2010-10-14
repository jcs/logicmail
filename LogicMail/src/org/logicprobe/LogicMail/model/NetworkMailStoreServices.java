/*-
 * Copyright (c) 2010, Derek Konigsberg
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
import java.util.Hashtable;
import java.util.Vector;

import net.rim.device.api.collection.util.BigVector;
import net.rim.device.api.util.Comparator;
import net.rim.device.api.util.IntHashtable;
import net.rim.device.api.util.IntVector;
import net.rim.device.api.util.SimpleSortingIntVector;
import net.rim.device.api.util.ToIntHashtable;

import org.logicprobe.LogicMail.conf.AccountConfig;
import org.logicprobe.LogicMail.mail.FolderTreeItem;
import org.logicprobe.LogicMail.mail.MailStoreRequestCallback;
import org.logicprobe.LogicMail.mail.MessageToken;
import org.logicprobe.LogicMail.mail.NetworkMailStore;
import org.logicprobe.LogicMail.message.FolderMessage;

public class NetworkMailStoreServices extends MailStoreServices {
    private final NetworkMailStore mailStore;
    private final FolderMessageCache folderMessageCache;
    
    /** Data structure to keep folder-specific refresh state. */
    private static class FolderRefreshState {
        /**
         * Flag to track whether a folder refresh is currently in progress.
         */
        public volatile boolean refreshInProgress;
        
        /**
         * Indicates that the initial refresh has completed, and that subsequent
         * refreshes should not try to reconcile against the cache.
         */
        public boolean initialRefreshComplete;
        
        public boolean secondaryFlagsRefresh;
        public Vector messageTokensToFetch;
        
        public Vector pendingFlagUpdates = new Vector();
        
        /**
         * Set of messages that have been loaded from the cache, but no longer
         * exist on the server.
         */
        public Hashtable orphanedMessageSet = new Hashtable();
        
        public Thread refreshThread;
        
        public volatile boolean cleanPriorToUse;
        
        public void prepareForUse() {
            if(cleanPriorToUse) {
                initialRefreshComplete = false;
                secondaryFlagsRefresh = false;
                messageTokensToFetch = null;
                pendingFlagUpdates.removeAllElements();
                orphanedMessageSet.clear();
                refreshThread = null;
                cleanPriorToUse = false;
            }
        }
    }
    
    /** Map of FolderTreeItem objects to corresponding FolderRefreshState data */
    private final Hashtable folderRefreshStateMap = new Hashtable();
    
    public NetworkMailStoreServices(NetworkMailStore mailStore, FolderMessageCache folderMessageCache) {
        super(mailStore);
        this.mailStore = mailStore;
        this.folderMessageCache = folderMessageCache;
    }

    AccountConfig getAccountConfig() {
        return mailStore.getAccountConfig();
    }
    
    public void restart() {
        mailStore.restart();
    }

    public void setConnected(boolean connected) {
        // Only handle disconnects
        if(connected) { return; }
        
        synchronized(folderRefreshStateMap) {
            Enumeration e = folderRefreshStateMap.elements();
            while(e.hasMoreElements()) {
                FolderRefreshState stateData = (FolderRefreshState)e.nextElement();
                stateData.cleanPriorToUse = true;
            }
        }
    }
    
    public void requestFolderRefresh(final FolderTreeItem folderTreeItem) {
        final FolderRefreshState stateData;
        synchronized(folderRefreshStateMap) {
            FolderRefreshState tempState = (FolderRefreshState)folderRefreshStateMap.get(folderTreeItem);
            if(tempState == null) {
                stateData = new FolderRefreshState();
                folderRefreshStateMap.put(folderTreeItem, stateData);
            }
            else {
                stateData = tempState;
                stateData.prepareForUse();
                
                if(stateData.refreshInProgress) {
                    // Immediately shortcut out if a refresh of this folder is
                    // currently in progress.
                    return;
                }
            }
            
            if(mailStore.hasLockedFolders() && stateData.initialRefreshComplete) {
                // Subsequent refresh is pointless on locked-folder mail stores
                return;
            }
            stateData.refreshInProgress = true;
        }
        
        if(stateData != null) {
            stateData.refreshThread = new Thread() { public void run() {
                if(mailStore.hasFolderMessageIndexMap()) {
                    mailStore.requestFolderMessageIndexMap(folderTreeItem, new InitialFetchCallback(folderTreeItem));
                }
                else {
                    // Queue a request for new folder messages from the mail store
                    mailStore.requestFolderMessagesRecent(folderTreeItem, true, new InitialFetchCallback(folderTreeItem));
                }
                
                // Skip cache loading after the initial refresh
                if(stateData.initialRefreshComplete) { return; }
                
                // Fetch messages stored in cache
                FolderMessage[] messages = folderMessageCache.getFolderMessages(folderTreeItem);
                if(messages.length > 0) {
                    NetworkMailStoreServices.super.fireFolderMessagesAvailable(folderTreeItem, messages, false, false);
                    
                    // Add all the messages that have been loaded from the
                    // cache.  Server-side messages will be removed from the
                    // set later on.
                    for(int i=0; i<messages.length; i++) {
                        stateData.orphanedMessageSet.put(messages[i].getMessageToken().getMessageUid(), messages[i]);
                    }
                }
                
                // If a message index map was requested (POP), execution will
                // resume in indexMapFetchComplete().
                // Otherwise (IMAP), execution will resume in
                // flagsRefreshComplete().
            }};
            stateData.refreshThread.start();
        }
    }
    
    private class InitialFetchCallback implements MailStoreRequestCallback {
        private final FolderTreeItem folderTreeItem;
        
        public InitialFetchCallback(FolderTreeItem folderTreeItem) {
            this.folderTreeItem = folderTreeItem;
        }
        
        public void mailStoreRequestComplete() { }
        public void mailStoreRequestFailed(Throwable exception) {
            FolderRefreshState stateData;
            synchronized(folderRefreshStateMap) {
                stateData = (FolderRefreshState)folderRefreshStateMap.get(folderTreeItem);
            }
            if(stateData != null) {
                stateData.pendingFlagUpdates.removeAllElements();
                stateData.refreshInProgress = false;
                stateData.secondaryFlagsRefresh = false;
            }
        }
    }
    
    private void indexMapFetchComplete(final FolderTreeItem folderTreeItem, final FolderRefreshState stateData, final ToIntHashtable uidIndexMap) {
        final int initialMessageLimit = mailStore.getAccountConfig().getInitialFolderMessages();
        final int messageRetentionLimit = mailStore.getAccountConfig().getMaximumFolderMessages();
        
        (new Thread() { public void run() {
            if(!joinRefreshThread(stateData)) { return; }
            
            Vector messagesUpdated = new Vector();
            SimpleSortingIntVector indexVector = new SimpleSortingIntVector();
            IntHashtable cachedIndexToMessageMap = new IntHashtable();
            
            // Iterate through the UID-to-index map, and do the following:
            // - Remove cache-loaded messages from the orphan set if they exist on the server.
            // - Update index information for those messages that do still exist server-side.
            // - Build a sortable vector of index values
            Enumeration e = uidIndexMap.keys();
            while(e.hasMoreElements()) {
                String uid = (String)e.nextElement();
                int index = uidIndexMap.get(uid);
                indexVector.addElement(index);
                
                FolderMessage message = (FolderMessage)stateData.orphanedMessageSet.remove(uid);
                if(message != null) {
                    message.setIndex(index);
                    message.getMessageToken().updateMessageIndex(index);
                    
                    if(folderMessageCache.updateFolderMessage(folderTreeItem, message)) {
                        messagesUpdated.addElement(message);
                        cachedIndexToMessageMap.put(index, message);
                    }
                }
            }
            indexVector.reSort(SimpleSortingIntVector.SORT_TYPE_NUMERIC);
            
            notifyMessageFlagUpdates(folderTreeItem, messagesUpdated);
            removeOrphanedMessages(folderTreeItem, stateData.orphanedMessageSet);

            // Determine the fetch range
            int size = indexVector.size();
            if(size == 0) { return; }
            int fetchRangeStart = Math.max(0, size - initialMessageLimit);
            
            // Build a list of indices to fetch
            IntVector messagesToFetch = new IntVector();
            for(int i=indexVector.size() - 1; i >= fetchRangeStart; --i) {
                int index = indexVector.elementAt(i);
                if(!cachedIndexToMessageMap.containsKey(index)) {
                    messagesToFetch.addElement(index);
                }
            }

            int additionalMessageLimit = messageRetentionLimit - initialMessageLimit;
            for(int i=fetchRangeStart - 1; i >= 0; --i) {
                if(additionalMessageLimit > 0) {
                    additionalMessageLimit--;
                }
                else {
                    // Beyond the limit, add these back to the orphan set
                    FolderMessage message = (FolderMessage)cachedIndexToMessageMap.get(indexVector.elementAt(i));
                    if(message != null) {
                        stateData.orphanedMessageSet.put(message.getMessageToken().getMessageUid(), message);
                    }
                }
            }
            removeOrphanedMessages(folderTreeItem, stateData.orphanedMessageSet);
            
            // Do the final request for missing messages
            mailStore.requestFolderMessagesSet(folderTreeItem, messagesToFetch.toArray(), new FinalFetchCallback(folderTreeItem));
        }}).start();
    }
    
    private void flagsRefreshComplete(final FolderTreeItem folderTreeItem, final FolderRefreshState stateData) {
        (new Thread() { public void run() {
            if(!joinRefreshThread(stateData)) { return; }
            
            Vector messagesUpdated = new Vector();
            stateData.messageTokensToFetch = new Vector();
            MessageToken oldestFetchedToken = null;
            Comparator tokenComparator = null;
            
            // Iterate through the pending flag updates, doing the following:
            // - Remove messages from the orphan set that exist on the server
            // - Update the cache for fetched messages
            // - Build a collection of messages to provide update notifications for
            // - Build a collection of messages that need to be fetched
            // - Keep track of the oldest message in the update set
            int size = stateData.pendingFlagUpdates.size();
            for(int i=0; i<size; i++) {
                FolderMessage message = (FolderMessage)stateData.pendingFlagUpdates.elementAt(i);
                MessageToken token = message.getMessageToken();
                
                // Remove messages with received flag updates from the orphan set
                stateData.orphanedMessageSet.remove(token.getMessageUid());
                
                if(folderMessageCache.updateFolderMessage(folderTreeItem, message)) {
                    messagesUpdated.addElement(message);
                }
                else {
                    stateData.messageTokensToFetch.addElement(token);
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
            stateData.pendingFlagUpdates.removeAllElements();
            
            notifyMessageFlagUpdates(folderTreeItem, messagesUpdated);
            
            // Build a collection of messages in the cache that still need to be verified
            Vector cachedTokensToCheck = new Vector();
            if(oldestFetchedToken != null) {
                Enumeration e = stateData.orphanedMessageSet.elements();
                while(e.hasMoreElements()) {
                    MessageToken token = ((FolderMessage)e.nextElement()).getMessageToken();
                    if(tokenComparator.compare(token, oldestFetchedToken) < 0) {
                        cachedTokensToCheck.addElement(token);
                    }
                }
            }
            
            if(cachedTokensToCheck.size() > 0) {
                // Perform a second flags fetch
                MessageToken[] tokens = new MessageToken[cachedTokensToCheck.size()];
                cachedTokensToCheck.copyInto(tokens);
                stateData.secondaryFlagsRefresh = true;
                mailStore.requestFolderMessagesSet(folderTreeItem, tokens, true, new InitialFetchCallback(folderTreeItem));
            }
            else {
                removeOrphanedMessages(folderTreeItem, stateData.orphanedMessageSet);
                finalFolderMessageFetch(folderTreeItem, stateData);
            }
        }}).start();
    }
    
    private void secondaryFlagsRefreshComplete(final FolderTreeItem folderTreeItem, final FolderRefreshState stateData) {
        int size = stateData.pendingFlagUpdates.size();
        BigVector messagesUpdated = new BigVector(size);
        Comparator folderMessageComparator = FolderMessage.getComparator();
        for(int i=0; i<size; i++) {
            FolderMessage message = (FolderMessage)stateData.pendingFlagUpdates.elementAt(i);
            MessageToken token = message.getMessageToken();
            
            // Remove messages with received flag updates from the orphan set
            stateData.orphanedMessageSet.remove(token.getMessageUid());
            
            if(folderMessageCache.updateFolderMessage(folderTreeItem, message)) {
                messagesUpdated.insertElement(folderMessageComparator, message);
            }
        }
        stateData.pendingFlagUpdates.removeAllElements();
        
        // Determine the how many messages from this secondary set we can keep
        int initialMessageLimit = mailStore.getAccountConfig().getInitialFolderMessages();
        int messageRetentionLimit = mailStore.getAccountConfig().getMaximumFolderMessages();
        int additionalMessageLimit = messageRetentionLimit - initialMessageLimit;
        
        size = messagesUpdated.size();
        if(size > additionalMessageLimit) {
            // We have too many additional messages, so we need to prune the set
            messagesUpdated.optimize();
            Vector messagesToNotify = new Vector();

            int splitIndex = messagesUpdated.size() - additionalMessageLimit;
            for(int i=0; i<splitIndex; i++) {
                FolderMessage message = (FolderMessage)messagesUpdated.elementAt(i);
                stateData.orphanedMessageSet.put(message.getMessageToken().getMessageUid(), message);
            }
            for(int i=splitIndex; i<size; i++) {
                messagesToNotify.addElement(messagesUpdated.elementAt(i));
            }
            
            notifyMessageFlagUpdates(folderTreeItem, messagesToNotify);
        }
        else {
            if(!messagesUpdated.isEmpty()) {
                FolderMessage[] messages = new FolderMessage[messagesUpdated.size()];
                messagesUpdated.copyInto(0, size, messages, 0);
                NetworkMailStoreServices.super.fireFolderMessagesAvailable(folderTreeItem, messages, true, true);
            }
        }
        
        removeOrphanedMessages(folderTreeItem, stateData.orphanedMessageSet);
        finalFolderMessageFetch(folderTreeItem, stateData);
    }

    private void finalFolderMessageFetch(final FolderTreeItem folderTreeItem, final FolderRefreshState stateData) {
        // Queue a fetch for messages missing from the cache
        if(!stateData.messageTokensToFetch.isEmpty()) {
            MessageToken[] fetchArray = new MessageToken[stateData.messageTokensToFetch.size()];
            stateData.messageTokensToFetch.copyInto(fetchArray);
            stateData.messageTokensToFetch.removeAllElements();
            mailStore.requestFolderMessagesSet(folderTreeItem, fetchArray, new FinalFetchCallback(folderTreeItem));
        }
        else {
            stateData.refreshInProgress = false;
            folderMessageCache.commit();

            // Notify the end of the operation
            fireFolderMessagesAvailable(folderTreeItem, null, false, false);
        }
    }
    
    /**
     * Joins refresh thread, after which the cached messages have been loaded.
     *
     * @param stateData the refresh state data
     * @return true, if successful, false if there was a failure and we cannot go further
     */
    private boolean joinRefreshThread(final FolderRefreshState stateData) {
        // Make sure the cache refresh is completed
        try {
            stateData.refreshThread.join();
        } catch (InterruptedException e) { }
        stateData.refreshThread = null;
        
        // Check if the first server request failed.  If it did fail,
        // then do some quick cleanup and shortcut out.
        if(!stateData.refreshInProgress) {
            stateData.orphanedMessageSet.clear();
            return false;
        }
        
        // Set a flag indicating that the initial fetch completed,
        // so we know how to handle subsequent fetches
        stateData.initialRefreshComplete = true;
        
        return true;
    }    
    
    private void notifyMessageFlagUpdates(final FolderTreeItem folderTreeItem, final Vector messagesUpdated) {
        if(!messagesUpdated.isEmpty()) {
            FolderMessage[] messages = new FolderMessage[messagesUpdated.size()];
            messagesUpdated.copyInto(messages);
            NetworkMailStoreServices.super.fireFolderMessagesAvailable(folderTreeItem, messages, true, true);
        }
    }

    private void removeOrphanedMessages(final FolderTreeItem folderTreeItem, final Hashtable orphanedMessageSet) {
        Enumeration e = orphanedMessageSet.elements();
        MessageToken[] orphanedTokens = new MessageToken[orphanedMessageSet.size()];
        int index = 0;
        while(e.hasMoreElements()) {
            FolderMessage message = (FolderMessage)e.nextElement();
            folderMessageCache.removeFolderMessage(folderTreeItem, message);
            orphanedTokens[index++] = message.getMessageToken();
        }
        orphanedMessageSet.clear();
        NetworkMailStoreServices.super.fireFolderExpunged(folderTreeItem, orphanedTokens);
    }
    
    private class FinalFetchCallback implements MailStoreRequestCallback {
        private final FolderTreeItem folderTreeItem;
        
        public FinalFetchCallback(FolderTreeItem folderTreeItem) {
            this.folderTreeItem = folderTreeItem;
        }
        
        public void mailStoreRequestComplete() {
            synchronized(folderRefreshStateMap) {
                FolderRefreshState stateData = (FolderRefreshState)folderRefreshStateMap.get(folderTreeItem);
                if(stateData != null) {
                    stateData.refreshInProgress = false;
                }
            }
            folderMessageCache.commit();
        }
        public void mailStoreRequestFailed(Throwable exception) {
            synchronized(folderRefreshStateMap) {
                FolderRefreshState stateData = (FolderRefreshState)folderRefreshStateMap.get(folderTreeItem);
                if(stateData != null) {
                    stateData.refreshInProgress = false;
                }
            }
            folderMessageCache.commit();
            
            // Notify the end of the operation
            fireFolderMessagesAvailable(folderTreeItem, null, false, false);
        }
    }
    
    public void requestMoreFolderMessages(FolderTreeItem folderTreeItem, MessageToken firstToken) {
        int increment = mailStore.getAccountConfig().getFolderMessageIncrement();
        mailStore.requestFolderMessagesRange(folderTreeItem, firstToken, increment);
    }
    
    public void removeSavedData(FolderTreeItem[] folderTreeItems) {
        for(int i=0; i<folderTreeItems.length; i++) {
            folderMessageCache.removeFolder(folderTreeItems[i]);
        }
        folderMessageCache.commit();
        super.removeSavedData(folderTreeItems);
    }
    
    protected void fireFolderMessagesAvailable(FolderTreeItem folder, FolderMessage[] messages, boolean flagsOnly) {
        FolderRefreshState stateData;
        synchronized(folderRefreshStateMap) {
            stateData = (FolderRefreshState)folderRefreshStateMap.get(folder);
        }
        
        if(messages != null) {
            if(flagsOnly) {
                if(stateData != null && stateData.refreshInProgress) {
                    for(int i=0; i<messages.length; i++) {
                        stateData.pendingFlagUpdates.addElement(messages[i]);
                    }
                }
                else {
                    for(int i=0; i<messages.length; i++) {
                        folderMessageCache.updateFolderMessage(folder, messages[i]);
                    }
                    super.fireFolderMessagesAvailable(folder, messages, flagsOnly);
                }
            }
            else {
                for(int i=0; i<messages.length; i++) {
                    folderMessageCache.addFolderMessage(folder, messages[i]);
                }
                super.fireFolderMessagesAvailable(folder, messages, flagsOnly);
            }
        }
        else {
            if(flagsOnly && stateData != null && stateData.refreshInProgress) {
                if(stateData.secondaryFlagsRefresh) {
                    stateData.secondaryFlagsRefresh = false;
                    secondaryFlagsRefreshComplete(folder, stateData);
                }
                else {
                    flagsRefreshComplete(folder, stateData);
                }
            }
            else {
                folderMessageCache.commit();
                super.fireFolderMessagesAvailable(folder, messages, flagsOnly);
            }
        }
    }
    
    protected void fireFolderMessageIndexMapAvailable(FolderTreeItem folder, ToIntHashtable uidIndexMap) {
        FolderRefreshState stateData;
        synchronized(folderRefreshStateMap) {
            stateData = (FolderRefreshState)folderRefreshStateMap.get(folder);
        }
        if(!stateData.refreshInProgress) { return; }
        
        indexMapFetchComplete(folder, stateData, uidIndexMap);
    }

    protected void fireFolderExpunged(FolderTreeItem folder, int[] indices) {
        FolderMessage[] messages = folderMessageCache.getFolderMessages(folder);
        for(int i=0; i<messages.length; i++) {
            if(messages[i].isDeleted()) {
                folderMessageCache.removeFolderMessage(folder, messages[i]);
            }
        }
        folderMessageCache.commit();
        super.fireFolderExpunged(folder, indices);
    }
}
