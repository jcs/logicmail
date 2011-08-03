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

import net.rim.device.api.system.EventLogger;

import org.logicprobe.LogicMail.AnalyticsDataCollector;
import org.logicprobe.LogicMail.AppInfo;
import org.logicprobe.LogicMail.message.MessageFlags;

class LocalMessageFlagChangeRequest extends LocalMailStoreRequest implements MessageFlagChangeRequest {
    private final LocalMessageToken messageToken;
    private final MessageToken[] messageTokens;
    private final MessageFlags messageFlags;
    private final boolean addOrRemove;
    
    LocalMessageFlagChangeRequest(LocalMailStore mailStore, MessageToken messageToken, MessageFlags messageFlags, boolean addOrRemove) {
        super(mailStore);
        this.messageToken = (LocalMessageToken)messageToken;
        this.messageTokens = null;
        this.messageFlags = messageFlags;
        this.addOrRemove = addOrRemove;
    }
    
    LocalMessageFlagChangeRequest(LocalMailStore mailStore, MessageToken[] messageTokens, MessageFlags messageFlags, boolean addOrRemove) {
        super(mailStore);
        this.messageToken = null;
        this.messageTokens = messageTokens;
        this.messageFlags = messageFlags;
        this.addOrRemove = addOrRemove;
    }

    public MessageToken getMessageToken() {
        return messageToken;
    }
    
    public MessageToken[] getMessageTokens() {
        return messageTokens;
    }
    
    public MessageFlags getMessageFlags() {
        return messageFlags;
    }
    
    public boolean isAddOrRemove() {
        return addOrRemove;
    }
    
    public void run() {
        if(messageToken != null) {
            runWithSingleMessage();
        }
        else if(messageTokens != null && messageTokens.length > 0) {
            runWithMessageSet();
        }
        else {
            fireMailStoreRequestComplete();
        }
    }

    private void runWithSingleMessage() {
        FolderTreeItem tokenFolder = mailStore.getMatchingFolderTreeItem(messageToken.getFolderPath());
        
        FolderTreeItem requestFolder = mailStore.getMatchingFolderTreeItem(tokenFolder.getPath());
        if(requestFolder == null) {
            fireMailStoreRequestFailed(null, true);
            return;
        }
        
        Throwable throwable = null;
        MessageFlags updatedFlags = null;
        boolean success;
        MaildirFolder maildirFolder = mailStore.getMaildirFolder(requestFolder);
        try {
            maildirFolder.open();
            updatedFlags = maildirFolder.setMessageFlag(messageToken, addOrRemove, messageFlags.getFlags());
            maildirFolder.close();
            success = true;
        } catch (IOException e) {
            EventLogger.logEvent(AppInfo.GUID, ("Unable to read folder: " + e.toString()).getBytes(), EventLogger.ERROR);
            AnalyticsDataCollector.getInstance().onApplicationError("Unable to read folder: " + e.toString());
            success = false;
            throwable = e;
        }
        
        if(success) {
            fireMailStoreRequestComplete();
        }
        else {
            fireMailStoreRequestFailed(throwable, true);
        }

        if(updatedFlags != null) {
            mailStore.fireMessageFlagsChanged(messageToken, updatedFlags);
        }
    }
    
    private void runWithMessageSet() {
        FolderTreeItem tokenFolder = mailStore.getMatchingFolderTreeItem(((LocalMessageToken)messageTokens[0]).getFolderPath());
        
        FolderTreeItem requestFolder = mailStore.getMatchingFolderTreeItem(tokenFolder.getPath());
        if(requestFolder == null) {
            fireMailStoreRequestFailed(null, true);
            return;
        }
        
        Vector updatedMessages = new Vector();
        
        Throwable throwable = null;
        boolean success;
        MaildirFolder maildirFolder = mailStore.getMaildirFolder(requestFolder);
        try {
            maildirFolder.open();
            for(int i=0; i<messageTokens.length; i++) {
                MessageFlags updatedFlags = maildirFolder.setMessageFlag(
                        (LocalMessageToken)messageTokens[i], addOrRemove, messageFlags.getFlags());
                if(updatedFlags != null) {
                    updatedMessages.addElement(new Object[] { messageTokens[i], updatedFlags });
                }
            }
            maildirFolder.close();
            success = true;
        } catch (IOException e) {
            EventLogger.logEvent(AppInfo.GUID, ("Unable to read folder: " + e.toString()).getBytes(), EventLogger.ERROR);
            AnalyticsDataCollector.getInstance().onApplicationError("Unable to read folder: " + e.toString());
            success = false;
            throwable = e;
        }
        
        if(success) {
            fireMailStoreRequestComplete();
        }
        else {
            fireMailStoreRequestFailed(throwable, true);
        }

        int count = updatedMessages.size();
        for(int i=0; i<count; i++) {
            Object[] element = (Object[])updatedMessages.elementAt(i);
            mailStore.fireMessageFlagsChanged((MessageToken)element[0], (MessageFlags)element[1]);
        }
    }
}
