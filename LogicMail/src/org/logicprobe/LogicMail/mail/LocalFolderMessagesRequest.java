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

import org.logicprobe.LogicMail.AppInfo;
import org.logicprobe.LogicMail.message.FolderMessage;

class LocalFolderMessagesRequest extends LocalMailStoreRequest implements FolderMessagesRequest {
    private final FolderTreeItem folder;
    private final boolean flagsOnly;
    private final Vector resultFolderMessages = new Vector();
    
    LocalFolderMessagesRequest(LocalMailStore mailStore, FolderTreeItem folder, boolean flagsOnly) {
        super(mailStore);
        this.folder = folder;
        this.flagsOnly = flagsOnly;
        
        // The flagsOnly parameter has no effect on local mail stores,
        // and it is not likely to ever be called on them anyways.
    }
    
    public int getType() {
        return TYPE_RECENT;
    }

    public FolderTreeItem getFolder() {
        return folder;
    }
    
    public boolean isFlagsOnly() {
        return flagsOnly;
    }
    
    public void run() {
        FolderTreeItem requestFolder = mailStore.getMatchingFolderTreeItem(folder.getPath());
        if(requestFolder == null) {
            fireMailStoreRequestFailed(null, true);
            return;
        }
        
        Throwable throwable = null;
        FolderMessage[] folderMessages = null;
        MaildirFolder maildirFolder = mailStore.getMaildirFolder(requestFolder);
        try {
            maildirFolder.open();
            folderMessages = maildirFolder.getFolderMessages();
            maildirFolder.close();
        } catch (IOException e) {
            EventLogger.logEvent(AppInfo.GUID, ("Unable to read folder: " + e.toString()).getBytes(), EventLogger.ERROR);
            throwable = e;
        }
        
        if(folderMessages != null) {
            resultFolderMessages.removeAllElements();
            for(int i=0; i<folderMessages.length; i++) {
                resultFolderMessages.addElement(folderMessages[i]);
            }
            
            fireMailStoreRequestComplete();
            mailStore.fireFolderMessagesAvailable(requestFolder, folderMessages, false);
        }
        else {
            fireMailStoreRequestFailed(throwable, true);
        }
    }

    public FolderMessage getResultFolderMessage() {
        return (FolderMessage)resultFolderMessages.lastElement();
    }

    public Vector getResultFolderMessages() {
        return resultFolderMessages;
    }
}
