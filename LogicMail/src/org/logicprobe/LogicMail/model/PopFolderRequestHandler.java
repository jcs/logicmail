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
package org.logicprobe.LogicMail.model;

import java.util.Date;
import java.util.Vector;

import org.logicprobe.LogicMail.mail.FolderTreeItem;
import org.logicprobe.LogicMail.mail.MessageToken;
import org.logicprobe.LogicMail.mail.NetworkMailStore;
import org.logicprobe.LogicMail.message.FolderMessage;
import org.logicprobe.LogicMail.message.MessageFlags;

/**
 * Handles folder-oriented requests for the mail store services layer, with
 * logic specific to the behavior of POP mail store folders.
 * <p>
 * POP folder refresh is a two-part operation consisting of an index map fetch
 * followed by a message header fetch. Since POP folder state is locked while
 * connected, subsequent refresh operations are not performed unless the
 * connection is broken and reestablished.
 * </p>
 */
class PopFolderRequestHandler extends FolderRequestHandler {

    public PopFolderRequestHandler(
            NetworkMailStoreServices mailStoreServices,
            NetworkMailStore mailStore,
            FolderMessageCache folderMessageCache,
            FolderTreeItem folderTreeItem) {
        super(mailStoreServices, mailStore, folderMessageCache, folderTreeItem);
    }
    
    public void setPriorFolderMessagesSeen(final Date startDate) {
        invokeAfterRefresh(new PostRefreshRunnable() {
            public void run(boolean refreshSuccessful) {
                long startTime = startDate.getTime();
                FolderMessage[] messages = folderMessageCache.getFolderMessages(folderTreeItem);
                
                Vector updatedMessages = new Vector();
                for(int i=0; i<messages.length; i++) {
                    if(messages[i].getEnvelope().date.getTime() < startTime
                            && !messages[i].isSeen()) {
                        messages[i].setSeen(true);
                        messages[i].setRecent(false);
                        if(folderMessageCache.updateFolderMessage(folderTreeItem, messages[i])) {
                            updatedMessages.addElement(new Object[] { messages[i].getMessageToken(), messages[i].getFlags() });
                        }
                    }
                }
                if(updatedMessages.size() > 0) {
                    folderMessageCache.commit();
                    
                    int count = updatedMessages.size();
                    for(int i=0; i<count; i++) {
                        Object[] element = (Object[])updatedMessages.elementAt(i);
                        mailStoreServices.fireMessageFlagsChanged((MessageToken)element[0], (MessageFlags)element[1]);
                    }
                }
            }
        }, false);
    }
}
