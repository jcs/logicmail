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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

import net.rim.device.api.system.EventLogger;

import org.logicprobe.LogicMail.AppInfo;
import org.logicprobe.LogicMail.message.Message;
import org.logicprobe.LogicMail.message.MimeMessageContent;
import org.logicprobe.LogicMail.message.MimeMessagePart;
import org.logicprobe.LogicMail.util.MailMessageParser;

class LocalMessageRequest extends LocalMailStoreRequest implements MessageRequest {
    private final LocalMessageToken messageToken;
    private final boolean useLimits;
    
    private MimeMessagePart resultStructure;
    private MimeMessageContent[] resultContent;
    
    LocalMessageRequest(LocalMailStore mailStore, MessageToken messageToken, boolean useLimits) {
        super(mailStore);
        this.messageToken = (LocalMessageToken)messageToken;
        this.useLimits = useLimits;
    }

    public int getType() {
        return MessageRequest.TYPE_MESSAGE_WHOLE;
    }
    
    public MessageToken getMessageToken() {
        return messageToken;
    }
    
    public boolean isUseLimits() {
        return useLimits;
    }

    public MimeMessagePart[] getMessageParts() {
        return null;
    }

    public void run() {
        FolderTreeItem tokenFolder = mailStore.getMatchingFolderTreeItem(messageToken.getFolderPath());
        FolderTreeItem requestFolder = mailStore.getMatchingFolderTreeItem(tokenFolder.getPath());
        if(requestFolder == null) {
            fireMailStoreRequestFailed(null, true);
            return;
        }

        Throwable throwable = null;
        String messageSource = null;
        Message message = null;
        MaildirFolder maildirFolder = mailStore.getMaildirFolder(requestFolder);
        try {
            maildirFolder.open();
            messageSource = maildirFolder.getMessageSource(messageToken);
            maildirFolder.close();
            
            // Parse the message source
            Hashtable contentMap = new Hashtable();
            MimeMessagePart rootPart = MailMessageParser.parseRawMessage(contentMap, new ByteArrayInputStream(messageSource.getBytes()));
            message = new Message(rootPart);
            Enumeration e = contentMap.keys();
            while(e.hasMoreElements()) {
                Object element = e.nextElement();
                if(!(element instanceof MimeMessagePart)) { continue; }
                MimeMessagePart part = (MimeMessagePart)element;
                MimeMessageContent content = (MimeMessageContent)contentMap.get(part);
                // Local parts are always complete, regardless of what the parser thinks
                content.setPartComplete(MimeMessageContent.PART_COMPLETE);
                message.putContent(part, content);
            }
        } catch (IOException e) {
            EventLogger.logEvent(AppInfo.GUID, ("Unable to read message: " + e.toString()).getBytes(), EventLogger.ERROR);
            message = null;
            messageSource = null;
            throwable = e;
        }
        
        if(message != null && messageSource != null) {
            resultStructure = message.getStructure();
            resultContent = message.getAllContent();
            fireMailStoreRequestComplete();
            mailStore.fireMessageAvailable(messageToken, true, resultStructure, resultContent, messageSource);
        }
        else {
            fireMailStoreRequestFailed(throwable, true);
        }
    }

    public MimeMessageContent[] getResultContent() {
        return resultContent;
    }

    public MimeMessagePart getResultStructure() {
        return resultStructure;
    }

    public boolean isResultComplete() {
        return true;
    }
}
