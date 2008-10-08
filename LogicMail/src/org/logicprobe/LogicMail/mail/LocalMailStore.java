/*-
 * Copyright (c) 2008, Derek Konigsberg
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

import org.logicprobe.LogicMail.message.FolderMessage;
import org.logicprobe.LogicMail.message.MessageFlags;


/**
 * This class manages local mail folders on the device.
 * It should only have a single instance, and does not
 * currently have any user configuration.
 */
public class LocalMailStore extends AbstractMailStore {
    private FolderTreeItem rootFolder;

    public LocalMailStore() {
        super();

        // Build the local folder tree, which matches a fixed layout for now.
        // Eventually it should be partially editable by the user.
        // This is also the only folder tree that should lack an INBOX.
        // The "Outbox" folder is marked to prevent the user from being able
        // to append messages to it.  However, it is a special mailbox, and the
        // object model can and does append messages to it as a spool.
        rootFolder = new FolderTreeItem("", "", "");
        rootFolder.addChild(new FolderTreeItem(rootFolder, "Outbox", "Outbox", "/", true, false));
        rootFolder.addChild(new FolderTreeItem(rootFolder, "Drafts", "Drafts", "/", true, true));
        rootFolder.addChild(new FolderTreeItem(rootFolder, "Sent", "Sent", "/", true, true));
        rootFolder.addChild(new FolderTreeItem(rootFolder, "Trash", "Trash", "/", true, true));
    }

    public void shutdown(boolean wait) {
        // This method is blank because all message operations
        // handle local data store management.
    }

    public boolean isLocal() {
        return true;
    }

    public boolean hasFolders() {
        return true;
    }

    public boolean hasFlags() {
        return true;
    }

    public boolean hasAppend() {
        return true;
    }

    public boolean hasUndelete() {
        return false;
    }

    public void requestFolderTree() {
        fireFolderTreeUpdated(rootFolder);
    }

    public void requestFolderStatus(FolderTreeItem[] folders) {
        // Make every entry in the provided array match the local folder
        // objects just in case they do not.  Then, fire change events
        // for all those folders.  The actual data to answer this
        // request should already be available.
        FolderTreeItem[] localFolders = rootFolder.children();

        for (int i = 0; i < folders.length; i++) {
            for (int j = 0; j < localFolders.length; j++) {
                if (folders[i].getPath().equals(localFolders[j].getPath())) {
                    folders[i] = localFolders[j];

                    break;
                }
            }
        }

        for (int i = 0; i < folders.length; i++) {
            fireFolderStatusChanged(folders[i]);
        }
    }

    public void requestFolderMessagesRange(FolderTreeItem folder,
        int firstIndex, int lastIndex) {
        // TODO Auto-generated method stub
    }

    public void requestFolderMessagesSet(FolderTreeItem folder, int[] indices) {
        // TODO Auto-generated method stub
    }

    public void requestFolderMessagesRecent(FolderTreeItem folder) {
        // TODO Auto-generated method stub
    }

    public void requestMessage(FolderTreeItem folder,
        FolderMessage folderMessage) {
        // TODO Auto-generated method stub
    }

    public void requestMessageDelete(FolderTreeItem folder,
        FolderMessage folderMessage) {
        // TODO Auto-generated method stub
    }

    public void requestMessageUndelete(FolderTreeItem folder,
        FolderMessage folderMessage) {
        // TODO Auto-generated method stub
    }

    public void requestMessageAnswered(FolderTreeItem folder,
        FolderMessage folderMessage) {
        // TODO Auto-generated method stub
    }

    public void requestMessageAppend(FolderTreeItem folder, String rawMessage,
        MessageFlags initialFlags) {
        // TODO Auto-generated method stub
    }
}
