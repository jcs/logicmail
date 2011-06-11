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

import net.rim.device.api.system.EventLogger;

import org.logicprobe.LogicMail.AppInfo;
import org.logicprobe.LogicMail.message.MessageFlags;

class LocalMessageFlagChangeRequest extends LocalMailStoreRequest implements MessageFlagChangeRequest {
    private final LocalMessageToken messageToken;
    private final MessageFlags messageFlags;
    private final boolean addOrRemove;
    
    LocalMessageFlagChangeRequest(LocalMailStore mailStore, MessageToken messageToken, MessageFlags messageFlags, boolean addOrRemove) {
        super(mailStore);
        this.messageToken = (LocalMessageToken)messageToken;
        this.messageFlags = messageFlags;
        this.addOrRemove = addOrRemove;
    }

    public MessageToken getMessageToken() {
        return messageToken;
    }
    
    public MessageFlags getMessageFlags() {
        return messageFlags;
    }
    
    public boolean isAddOrRemove() {
        return addOrRemove;
    }
    
    public void run() {
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

}