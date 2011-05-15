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
import java.util.Vector;

import org.logicprobe.LogicMail.LogicMailResource;
import org.logicprobe.LogicMail.message.FolderMessage;

class NetworkFolderMessagesRequest extends NetworkMailStoreRequest implements FolderMessagesRequest {
    private final int type;
    private final FolderTreeItem folder;
    
    private MessageToken firstToken;
    private int increment;
    
    private MessageToken[] messageTokens;
    private final boolean flagsOnly;
    
    private int[] messageIndices;
    
    private final Vector folderMessages = new Vector();;
    
    NetworkFolderMessagesRequest(NetworkMailStore mailStore, FolderTreeItem folder, MessageToken firstToken, int increment) {
        super(mailStore);
        // requestFolderMessagesRange
        this.type = TYPE_RANGE;
        this.folder = folder;
        this.firstToken = firstToken;
        this.increment = increment;
        this.flagsOnly = false;
    }

    NetworkFolderMessagesRequest(NetworkMailStore mailStore, FolderTreeItem folder, MessageToken[] messageTokens, boolean flagsOnly) {
        super(mailStore);
        // requestFolderMessagesSet
        this.type = TYPE_SET_TOKENS;
        this.folder = folder;
        this.messageTokens = messageTokens;
        this.flagsOnly = flagsOnly;
    }

    NetworkFolderMessagesRequest(NetworkMailStore mailStore, FolderTreeItem folder, int[] messageIndices) {
        super(mailStore);
        // requestFolderMessagesSet
        this.type = TYPE_SET_INDICES;
        this.folder = folder;
        this.messageIndices = messageIndices;
        this.flagsOnly = false;
    }

    NetworkFolderMessagesRequest(NetworkMailStore mailStore, FolderTreeItem folder, boolean flagsOnly) {
        super(mailStore);
        // requestFolderMessagesRecent
        this.type = TYPE_RECENT;
        this.folder = folder;
        this.flagsOnly = flagsOnly;
    }

    public int getType() {
        return type;
    }
    
    public FolderTreeItem getFolder() {
        return folder;
    }
    
    public boolean isFlagsOnly() {
        return flagsOnly;
    }
    
    public void execute(MailClient client) throws IOException, MailException {
        super.execute(client);
        IncomingMailClient incomingClient = (IncomingMailClient)client;

        String message = resources.getString(LogicMailResource.MAILCONNECTION_REQUEST_FOLDER_MESSAGES);
        showStatus(message + "...");
        checkActiveFolder(incomingClient, folder);
        
        GetFolderMessageCallback clientCallback = new GetFolderMessageCallback();
        
        switch(type) {
        case TYPE_RANGE:
            incomingClient.getFolderMessages(
                    firstToken, increment,
                    clientCallback,
                    getProgressHandler(message));
            break;
        case TYPE_SET_TOKENS:
            incomingClient.getFolderMessages(
                    messageTokens,
                    flagsOnly,
                    clientCallback,
                    getProgressHandler(message));
            break;
        case TYPE_SET_INDICES:
            incomingClient.getFolderMessages(
                    messageIndices,
                    clientCallback,
                    getProgressHandler(message));
            break;
        case TYPE_RECENT:
            incomingClient.getNewFolderMessages(
                    flagsOnly,
                    clientCallback,
                    getProgressHandler(message));
            break;
        default:
            fireMailStoreRequestFailed(null, true);
            break;
        }
    }
    
    private class GetFolderMessageCallback implements FolderMessageCallback {
        public void folderMessageUpdate(FolderMessage folderMessage) {
            if(folderMessage != null) {
                folderMessages.addElement(folderMessage);
                mailStore.fireFolderMessagesAvailable(folder, new FolderMessage[] { folderMessage }, flagsOnly);
            }
            else {
                // This is the last update of the sequence
                fireMailStoreRequestComplete();
                mailStore.fireFolderMessagesAvailable(folder, null, flagsOnly);
            }
        }
    };
    
    public Vector getResultFolderMessages() {
        return folderMessages;
    }
    
    public FolderMessage getResultFolderMessage() {
        return (FolderMessage)folderMessages.lastElement();
    }
}
