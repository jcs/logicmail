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

import org.logicprobe.LogicMail.mail.FolderTreeItem;
import org.logicprobe.LogicMail.util.DataStore;
import org.logicprobe.LogicMail.util.DataStoreFactory;

public class LocalAccountNode extends AccountNode {

    LocalAccountNode(LocalMailStoreServices mailStore) {
        super(mailStore);
        
        this.status = STATUS_LOCAL;
    }
    
    /**
     * Gets the name of this account.
     *
     * @return The name.
     */
    public String toString() {
        return "Local Folders";
    }
    
    public String getProtocolName() {
        return "Local";
    }
    
    protected int getMailboxType(FolderTreeItem folderTreeItem) {
        int mailboxType;
        String path = folderTreeItem.getPath();
        if (path.equalsIgnoreCase("Outbox")) {
            mailboxType = MailboxNode.TYPE_OUTBOX;
        }
        else if (path.equalsIgnoreCase("Drafts")) {
            mailboxType = MailboxNode.TYPE_DRAFTS;
        }
        else if (path.equalsIgnoreCase("Sent")) {
            mailboxType = MailboxNode.TYPE_SENT;
        }
        else if (path.equalsIgnoreCase("Trash")) {
            mailboxType = MailboxNode.TYPE_TRASH;
        }
        else {
            mailboxType = MailboxNode.TYPE_NORMAL;
        }
        return mailboxType;
    }
    
    protected void save() {
        DataStore connectionCache = DataStoreFactory.getConnectionCacheStore();
        connectionCache.putNamedObject("LocalMailStore", getRootMailbox());
        connectionCache.save();
    }
    
    protected void load() {
        DataStore connectionCache = DataStoreFactory.getConnectionCacheStore();
        
        Object loadedObject = connectionCache.getNamedObject("LocalMailStore");
        
        if (loadedObject instanceof MailboxNode) {
            setRootMailbox((MailboxNode)loadedObject);
        }
    }

}
