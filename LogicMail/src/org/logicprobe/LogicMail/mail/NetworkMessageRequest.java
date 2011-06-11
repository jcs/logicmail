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

import org.logicprobe.LogicMail.LogicMailResource;
import org.logicprobe.LogicMail.message.Message;
import org.logicprobe.LogicMail.message.MimeMessageContent;
import org.logicprobe.LogicMail.message.MimeMessagePart;

class NetworkMessageRequest extends NetworkMailStoreRequest implements MessageRequest {
    private final int type;
    private final MessageToken messageToken;
    private final boolean useLimits;
    private final MimeMessagePart[] messageParts;
    
    private boolean resultComplete;
    private MimeMessagePart resultStructure;
    private MimeMessageContent[] resultContent;
    
    NetworkMessageRequest(NetworkMailStore mailStore, MessageToken messageToken, boolean useLimits) {
        super(mailStore);
        this.type = TYPE_MESSAGE_WHOLE;
        this.messageToken = messageToken;
        this.useLimits = useLimits;
        this.messageParts = null;
    }
    
    public NetworkMessageRequest(NetworkMailStore mailStore, MessageToken messageToken, MimeMessagePart[] messageParts) {
        super(mailStore);
        this.type = TYPE_MESSAGE_PARTS;
        this.messageToken = messageToken;
        this.useLimits = false;
        this.messageParts = messageParts;
    }

    public int getType() {
        return type;
    }
    
    public MessageToken getMessageToken() {
        return messageToken;
    }
    
    public boolean isUseLimits() {
        return useLimits;
    }
    
    public MimeMessagePart[] getMessageParts() {
        return messageParts;
    }
    
    protected String getInitialStatus() {
        return resources.getString(LogicMailResource.MAILCONNECTION_REQUEST_MESSAGE);
    }
    
    public void execute(MailClient client) throws IOException, MailException {
        IncomingMailClient incomingClient = (IncomingMailClient)client;
        
        String statusMessage = resources.getString(LogicMailResource.MAILCONNECTION_REQUEST_MESSAGE);
        checkActiveFolder(incomingClient, messageToken);

        if(messageParts == null) {
            Message message = incomingClient.getMessage(messageToken, useLimits, getProgressHandler(statusMessage));

            if(message != null) {
                resultComplete = message.isComplete();
                resultStructure = message.getStructure();
                resultContent = message.getAllContent();
                
                fireMailStoreRequestComplete();
                mailStore.fireMessageAvailable(
                        messageToken,
                        resultComplete,
                        resultStructure,
                        resultContent, null);
            }
            else {
                fireMailStoreRequestFailed(null, false);
            }
        }
        else {
            MimeMessageContent[] messageContent;

            // Replace this with a more general method:
            if(incomingClient.hasMessageParts()) {
                Vector messageContentVector = new Vector();
                for(int i=0; i<messageParts.length; i++) {
                    MimeMessageContent content =
                        incomingClient.getMessagePart(messageToken, messageParts[i], getProgressHandler(statusMessage));
                    if(content != null) {
                        messageContentVector.addElement(content);
                    }
                }
                messageContent = new MimeMessageContent[messageContentVector.size()];
                messageContentVector.copyInto(messageContent);
            }
            else {
                messageContent = null;
            }

            if(messageContent != null) {
                resultContent = messageContent;
                fireMailStoreRequestComplete();
                mailStore.fireMessageContentAvailable(messageToken, resultContent);
            }
            else {
                fireMailStoreRequestFailed(null, false);
            }
        }
    }
    
    public boolean isResultComplete() {
        return resultComplete;
    }
    
    public MimeMessagePart getResultStructure() {
        return resultStructure;
    }
    
    public MimeMessageContent[] getResultContent() {
        return resultContent;
    }
}
