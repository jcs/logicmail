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
import java.util.Date;
import java.util.Vector;

import net.rim.device.api.system.EventLogger;

import org.logicprobe.LogicMail.AppInfo;
import org.logicprobe.LogicMail.message.FolderMessage;
import org.logicprobe.LogicMail.message.MessageFlags;

class LocalMessageRangeFlagChangeRequest extends LocalMailStoreRequest implements MessageRangeFlagChangeRequest {
    private final FolderTreeItem folder;
    private final Date startDate;
    private final MessageFlags messageFlags;
    private final boolean addOrRemove;
    
    LocalMessageRangeFlagChangeRequest(LocalMailStore mailStore, FolderTreeItem folder, Date startDate, MessageFlags messageFlags, boolean addOrRemove) {
        super(mailStore);
        this.folder = folder;
        this.startDate = startDate;
        this.messageFlags = messageFlags;
        this.addOrRemove = addOrRemove;
    }

    public FolderTreeItem getFolder() {
        return folder;
    }

    public Date getStartDate() {
        return startDate;
    }

    public MessageFlags getMessageFlags() {
        return messageFlags;
    }

    public boolean isAddOrRemove() {
        return addOrRemove;
    }
    
    public void run() {
        long startTime = startDate.getTime();
        int flags = messageFlags.getFlags();
        Vector updatedMessages = new Vector();
        
        Throwable throwable = null;
        boolean success;
        MaildirFolder maildirFolder = mailStore.getMaildirFolder(folder);
        try {
            maildirFolder.open();
            FolderMessage[] messages = maildirFolder.getFolderMessages();
            
            for(int i=0; i<messages.length; i++) {
                if(messages[i].getEnvelope().date.getTime() < startTime) {
                    LocalMessageToken token = (LocalMessageToken)messages[i].getMessageToken();
                    MessageFlags updatedFlags = maildirFolder.setMessageFlag(token, addOrRemove, flags);
                    if(updatedFlags != null) {
                        updatedMessages.addElement(new Object[] { token, updatedFlags });
                    }
                }
            }
            
            maildirFolder.close();
            success = true;
        } catch (IOException e) {
            EventLogger.logEvent(AppInfo.GUID, ("Unable to read folder: " + e.toString()).getBytes(), EventLogger.ERROR);
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
