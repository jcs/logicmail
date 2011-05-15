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

class LocalFolderExpungeRequest extends LocalMailStoreRequest implements FolderExpungeRequest, Runnable {
    private final FolderTreeItem folder;

    LocalFolderExpungeRequest(LocalMailStore mailStore, FolderTreeItem folder) {
        super(mailStore);
        this.folder = folder;
    }

    public FolderTreeItem getFolder() {
        return folder;
    }
    
    public void run() {
        FolderTreeItem requestFolder = mailStore.getMatchingFolderTreeItem(folder.getPath());
        if(requestFolder == null) {
            fireMailStoreRequestFailed(null, true);
            return;
        }
        
        Throwable throwable = null;
        boolean expunged = false;
        MaildirFolder maildirFolder = mailStore.getMaildirFolder(requestFolder);
        try {
            maildirFolder.open();
            maildirFolder.expunge();
            maildirFolder.close();
            expunged = true;
        } catch (IOException e) {
            EventLogger.logEvent(AppInfo.GUID, ("Unable to expunge folder: " + e.toString()).getBytes(), EventLogger.ERROR);
            throwable = e;
        }
        
        if(expunged) {
            fireMailStoreRequestComplete();
            mailStore.fireFolderExpunged(requestFolder, new int[0], new MessageToken[0]);
        }
        else {
            fireMailStoreRequestFailed(throwable, true);
        }
    }
}
