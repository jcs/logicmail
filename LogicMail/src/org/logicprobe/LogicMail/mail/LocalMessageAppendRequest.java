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

import org.logicprobe.LogicMail.AnalyticsDataCollector;
import org.logicprobe.LogicMail.AppInfo;
import org.logicprobe.LogicMail.message.FolderMessage;
import org.logicprobe.LogicMail.message.MessageFlags;

class LocalMessageAppendRequest extends LocalMailStoreRequest implements MessageAppendRequest {
    private final FolderTreeItem folder;
    private final String rawMessage;
    private final MessageFlags initialFlags;
    
    LocalMessageAppendRequest(LocalMailStore mailStore, FolderTreeItem folder, String rawMessage, MessageFlags initialFlags) {
        super(mailStore);
        this.folder = folder;
        this.rawMessage = rawMessage;
        this.initialFlags = initialFlags;
    }

    public FolderTreeItem getFolder() {
        return folder;
    }
    
    public String getRawMessage() {
        return rawMessage;
    }
    
    public MessageFlags getInitialFlags() {
        return initialFlags;
    }
    
    public void run() {
        FolderTreeItem requestFolder = mailStore.getMatchingFolderTreeItem(folder.getPath());
        
        if(requestFolder == null || rawMessage == null || rawMessage.length() == 0 || initialFlags == null) {
            fireMailStoreRequestFailed(null, true);
            return;
        }
        
        Throwable throwable = null;
        FolderMessage folderMessage = null;
        MaildirFolder maildirFolder = mailStore.getMaildirFolder(requestFolder);
        try {
            maildirFolder.open();
            folderMessage = maildirFolder.appendMessage(rawMessage, initialFlags);
            maildirFolder.close();
        } catch (IOException e) {
            EventLogger.logEvent(AppInfo.GUID, ("Unable to read folder: " + e.toString()).getBytes(), EventLogger.ERROR);
            AnalyticsDataCollector.getInstance().onApplicationError("Unable to read folder: " + e.toString());
            throwable = e;
        }
        
        if(folderMessage != null) {
            fireMailStoreRequestComplete();
            mailStore.fireFolderMessagesAvailable(requestFolder, new FolderMessage[] { folderMessage }, false);
        }
        else {
            fireMailStoreRequestFailed(throwable, true);
        }
    }
}
