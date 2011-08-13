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

import net.rim.device.api.util.IntHashtable;
import net.rim.device.api.util.IntVector;
import net.rim.device.api.util.SimpleSortingIntVector;
import net.rim.device.api.util.ToIntHashtable;

import org.logicprobe.LogicMail.LogicMailResource;
import org.logicprobe.LogicMail.mail.pop.PopClient;
import org.logicprobe.LogicMail.message.FolderMessage;

class PopFolderRefreshRequest extends NetworkMailStoreRequest implements MailStoreRequest {
    private final String statusMessage;
    private final FolderTreeItem folder;
    private final Hashtable loadedMessageMap;

    PopFolderRefreshRequest(NetworkMailStore mailStore, FolderTreeItem folder, FolderMessage[] loadedMessages) {
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
    
    protected String getInitialStatus() {
        return statusMessage + "...";
    }

    public void execute(MailClient client) throws IOException, MailException {
        PopClient incomingClient = (PopClient)client;

        checkActiveFolder(incomingClient, folder);
        
        // Get the Index-to-UID map for the folder
        ToIntHashtable uidIndexMap = incomingClient.getFolderMessageIndexMap(getProgressHandler(statusMessage));
        
        // Get configuration values that affect the rest of the process
        int initialMessageLimit = incomingClient.getAcctConfig().getInitialFolderMessages();
        int messageRetentionLimit = incomingClient.getAcctConfig().getMaximumFolderMessages();

        // Initialize collections used during processing
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
            
            FolderMessage message = (FolderMessage)loadedMessageMap.remove(uid);
            if(message != null) {
                message.setIndex(index);
                message.getMessageToken().updateMessageIndex(index);
                messagesUpdated.addElement(message);
                cachedIndexToMessageMap.put(index, message);
            }
        }
        indexVector.reSort(SimpleSortingIntVector.SORT_TYPE_NUMERIC);
        
        notifyMessageFlagUpdates(messagesUpdated);
        removeOrphanedMessages();

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
                    loadedMessageMap.put(message.getMessageToken().getMessageUid(), message);
                }
            }
        }
        removeOrphanedMessages();
        
        // Do the final request for missing messages
        if(messagesToFetch.size() > 0) {
            fetchMessageSetByIndices(incomingClient, messagesToFetch.toArray());
        }
        
        loadedMessageMap.clear();
        fireMailStoreRequestComplete();
    }

    private void notifyMessageFlagUpdates(Vector messagesUpdated) {
        if(!messagesUpdated.isEmpty()) {
            FolderMessage[] messages = new FolderMessage[messagesUpdated.size()];
            messagesUpdated.copyInto(messages);
            mailStore.fireFolderMessagesAvailable(folder, messages, true);
        }
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
    
    private void fetchMessageSetByIndices(PopClient incomingClient, int[] messageIndices) throws IOException, MailException {
        GetFolderMessageCallback clientCallback = new GetFolderMessageCallback();
        incomingClient.getFolderMessages(
                messageIndices,
                clientCallback,
                getProgressHandler(statusMessage));
    }
    
    private class GetFolderMessageCallback implements FolderMessageCallback {
        public void folderMessageUpdate(FolderMessage folderMessage) {
            if(folderMessage != null) {
                mailStore.fireFolderMessagesAvailable(folder, new FolderMessage[] { folderMessage }, false);
            }
            else {
                // This is the last update of the sequence
                mailStore.fireFolderMessagesAvailable(folder, null, false);
            }
        }
    };
}
