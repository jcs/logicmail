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
import java.util.Vector;

import net.rim.device.api.util.IntHashtable;

import org.logicprobe.LogicMail.conf.AccountConfig;
import org.logicprobe.LogicMail.mail.FolderTreeItem;
import org.logicprobe.LogicMail.mail.MailStoreRequestCallback;
import org.logicprobe.LogicMail.mail.MessageToken;
import org.logicprobe.LogicMail.mail.NetworkMailStore;
import org.logicprobe.LogicMail.message.FolderMessage;
import org.logicprobe.LogicMail.util.AtomicBoolean;

public class NetworkMailStoreServices extends MailStoreServices {
    private final NetworkMailStore mailStore;
    private final FolderMessageCache folderMessageCache;
    
    /**
     * Flag to track whether a mailbox refresh is currently in progress.
     */
    private final AtomicBoolean refreshInProgress = new AtomicBoolean();
    
    private final Vector pendingFlagUpdates = new Vector();
    
    /**
     * Set of messages that have been loaded from the cache, but no longer
     * exist on the server.
     */
    private final IntHashtable orphanedMessageSet = new IntHashtable();
    
    private Thread refreshThread;

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

    public void requestFolderRefresh(final FolderTreeItem folderTreeItem) {
        if(refreshInProgress.compareAndSet(false, true)) {
            refreshThread = new Thread() { public void run() {
                // Queue a request for new folder messages from the mail store
                mailStore.requestFolderMessagesRecent(folderTreeItem, true, new MailStoreRequestCallback() {
                    public void mailStoreRequestComplete() { }
                    public void mailStoreRequestFailed(Throwable exception) {
                        pendingFlagUpdates.removeAllElements();
                        refreshInProgress.set(false);
                    }
                });
                
                // Fetch messages stored in cache
                FolderMessage[] messages = folderMessageCache.getFolderMessages(folderTreeItem);
                if(messages.length > 0) {
                    NetworkMailStoreServices.super.fireFolderMessagesAvailable(folderTreeItem, messages, false, false);

                    // Add all the messages that have been loaded from the
                    // cache.  Server-side messages will be removed from the
                    // set later on.
                    for(int i=0; i<messages.length; i++) {
                        orphanedMessageSet.put(messages[i].getUid(), messages[i]);
                    }
                }
            }};
            refreshThread.start();
        }
    }
    
    private void flagsRefreshComplete(final FolderTreeItem folderTreeItem) {
        (new Thread() { public void run() {
            // Make sure the cache refresh is completed
            try {
                refreshThread.join();
            } catch (InterruptedException e) { }
            refreshThread = null;
            
            // Check if the first server request failed.  If it did fail,
            // then do some quick cleanup and shortcut out.
            if(!refreshInProgress.get()) {
                orphanedMessageSet.clear();
                return;
            }
            
            Vector messagesUpdated = new Vector();
            Vector messageTokensToFetch = new Vector();

            // Apply pending flag updates, while building a list of messages
            // that still need to be fetched
            int size = pendingFlagUpdates.size();
            for(int i=0; i<size; i++) {
                FolderMessage message = (FolderMessage)pendingFlagUpdates.elementAt(i);
                
                // Remove messages with received flag updates from the orphan set
                orphanedMessageSet.remove(message.getUid());
                
                if(folderMessageCache.updateFolderMessage(folderTreeItem, message)) {
                    messagesUpdated.addElement(message);
                }
                else {
                    messageTokensToFetch.addElement(message.getMessageToken());
                }
            }
            pendingFlagUpdates.removeAllElements();
            
            // Notify with any flag updates
            if(!messagesUpdated.isEmpty()) {
                FolderMessage[] messages = new FolderMessage[messagesUpdated.size()];
                messagesUpdated.copyInto(messages);
                NetworkMailStoreServices.super.fireFolderMessagesAvailable(folderTreeItem, messages, true, true);
            }
            
            // Remove orphaned messages from the cache
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
            
            // Queue a fetch for messages missing from the cache
            if(!messageTokensToFetch.isEmpty()) {
                MessageToken[] fetchArray = new MessageToken[messageTokensToFetch.size()];
                messageTokensToFetch.copyInto(fetchArray);
                messageTokensToFetch.removeAllElements();
                mailStore.requestFolderMessagesSet(folderTreeItem, fetchArray, new MailStoreRequestCallback() {
                    public void mailStoreRequestComplete() {
                        refreshInProgress.set(false);
                        folderMessageCache.commit();
                    }
                    public void mailStoreRequestFailed(Throwable exception) {
                        refreshInProgress.set(false);
                        folderMessageCache.commit();
                    }
                });
            }
            else {
                refreshInProgress.set(false);
                folderMessageCache.commit();
            }
        }}).start();
    }
    
    public void removeSavedData(FolderTreeItem[] folderTreeItems) {
        for(int i=0; i<folderTreeItems.length; i++) {
            folderMessageCache.removeFolder(folderTreeItems[i]);
        }
        folderMessageCache.commit();
        super.removeSavedData(folderTreeItems);
    }
    
    protected void fireFolderMessagesAvailable(FolderTreeItem folder, FolderMessage[] messages, boolean flagsOnly) {
        if(messages != null) {
            if(flagsOnly) {
                if(refreshInProgress.get()) {
                    for(int i=0; i<messages.length; i++) {
                        pendingFlagUpdates.addElement(messages[i]);
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
            if(flagsOnly && refreshInProgress.get()) {
                flagsRefreshComplete(folder);
            }
            else {
                folderMessageCache.commit();
                super.fireFolderMessagesAvailable(folder, messages, flagsOnly);
            }
        }
        
        
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
