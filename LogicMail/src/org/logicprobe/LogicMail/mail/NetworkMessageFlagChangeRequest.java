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

import org.logicprobe.LogicMail.LogicMailResource;
import org.logicprobe.LogicMail.message.MessageFlags;

class NetworkMessageFlagChangeRequest extends NetworkMailStoreRequest implements MessageFlagChangeRequest {
    private final MessageToken messageToken;
    private final MessageFlags messageFlags;
    private final boolean addOrRemove;
    
    NetworkMessageFlagChangeRequest(NetworkMailStore mailStore, MessageToken messageToken, MessageFlags messageFlags, boolean addOrRemove) {
        super(mailStore);
        this.messageToken = messageToken;
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
    
    public void execute(MailClient client) throws IOException, MailException {
        super.execute(client);
        IncomingMailClient incomingClient = (IncomingMailClient)client;

        if(messageFlags.isDeleted()) {
            if(addOrRemove) {
                showStatus(resources.getString(LogicMailResource.MAILCONNECTION_REQUEST_MESSAGE_DELETE));
                checkActiveFolder(incomingClient, messageToken);
                incomingClient.deleteMessage(messageToken);
            }
            else {
                showStatus(resources.getString(LogicMailResource.MAILCONNECTION_REQUEST_MESSAGE_UNDELETE));
                checkActiveFolder(incomingClient, messageToken);
                incomingClient.undeleteMessage(messageToken);
            }
        }
        else if(messageFlags.isAnswered() && addOrRemove) {
            showStatus(resources.getString(LogicMailResource.MAILCONNECTION_REQUEST_MESSAGE_UPDATING_FLAGS));
            checkActiveFolder(incomingClient, messageToken);
            incomingClient.messageAnswered(messageToken);
        }
        else if(messageFlags.isForwarded() && addOrRemove) {
            showStatus(resources.getString(LogicMailResource.MAILCONNECTION_REQUEST_MESSAGE_UPDATING_FLAGS));
            checkActiveFolder(incomingClient, messageToken);
            incomingClient.messageForwarded(messageToken);
        }
        else if(messageFlags.isSeen()) {
            showStatus(resources.getString(LogicMailResource.MAILCONNECTION_REQUEST_MESSAGE_UPDATING_FLAGS));
            checkActiveFolder(incomingClient, messageToken);
            if(addOrRemove) {
                incomingClient.messageSeen(messageToken);
            }
            else {
                incomingClient.messageUnseen(messageToken);
            }
        }

        fireMailStoreRequestComplete();

        // Notification of actual flag changes is received through the client listener
    }
}
