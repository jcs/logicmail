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
import org.logicprobe.LogicMail.mail.LocalMailStore;
import org.logicprobe.LogicMail.mail.MailStoreRequest;
import org.logicprobe.LogicMail.mail.MailStoreRequestCallback;
import org.logicprobe.LogicMail.mail.MessageRequest;
import org.logicprobe.LogicMail.mail.MessageToken;
import org.logicprobe.LogicMail.message.MimeMessagePart;

public class LocalMailStoreServices extends MailStoreServices {
    private final LocalMailStore mailStore;
    
    protected LocalMailStoreServices(LocalMailStore mailStore) {
        super(mailStore);
        this.mailStore = mailStore;
    }

    public void requestFolderRefresh(FolderTreeItem folderTreeItem) {
        mailStore.processRequest(mailStore.createFolderMessagesRecentRequest(folderTreeItem));
    }
    
    public void requestMoreFolderMessages(FolderTreeItem folderTreeItem, MessageToken firstToken) {
        throw new UnsupportedOperationException();
    }
    
    public boolean requestMessageRefresh(MessageToken messageToken, MimeMessagePart[] partsToSkip) {
        if(partsToSkip != null && partsToSkip.length > 0) {
            // The current local mail store implementation only loads complete
            // message content, so the presence of anything in the parts-to-skip
            // collection means we can ignore the refresh request.
            return false;
        }
        else {
            mailStore.processRequest(mailStore.createMessageRequest(messageToken, true)
                    .setRequestCallback(new MailStoreRequestCallback() {
                        public void mailStoreRequestComplete(MailStoreRequest request) {
                            requestMessageSeen(((MessageRequest)request).getMessageToken());                    
                        }
                        public void mailStoreRequestFailed(MailStoreRequest request,
                                Throwable exception, boolean isFinal) { }
                    }));
            return true;
        }
    }
    
    public boolean requestMessageRefreshCacheOnly(MessageToken messageToken) {
        return requestMessageRefresh(messageToken, null);
    }
    
    public boolean requestEntireMessageRefresh(MessageToken messageToken) {
        // Not supported
        return false;
    }
}
