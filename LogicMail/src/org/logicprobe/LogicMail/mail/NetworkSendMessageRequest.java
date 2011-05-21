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

import net.rim.device.api.i18n.ResourceBundle;

import org.logicprobe.LogicMail.LogicMailResource;
import org.logicprobe.LogicMail.message.Message;
import org.logicprobe.LogicMail.message.MessageEnvelope;

class NetworkSendMessageRequest implements ConnectionHandlerRequest {
    
    // This class duplicates a lot of code from AbstractMailStoreRequest, which
    // should probably be moved to a common parent class at some point.
    
    protected static ResourceBundle resources = ResourceBundle.getBundle(LogicMailResource.BUNDLE_ID, LogicMailResource.BUNDLE_NAME);
    private final NetworkMailSender mailSender;
    private final MessageEnvelope envelope;
    private final Message message;
    private boolean deliberate = true;
    
    NetworkSendMessageRequest(NetworkMailSender mailSender, MessageEnvelope envelope, Message message) {
        this.mailSender = mailSender;
        this.envelope = envelope;
        this.message = message;
    }
    
    public void setDeliberate(boolean deliberate) {
        this.deliberate = deliberate;
    }
    
    public boolean isDeliberate() {
        return deliberate;
    }
    
    public void execute(MailClient client) throws IOException, MailException {
        OutgoingMailClient outgoingClient = (OutgoingMailClient)client;
        
        showStatus(outgoingClient, resources.getString(LogicMailResource.MAILCONNECTION_REQUEST_SEND_MESSAGE));
        String messageSource = outgoingClient.sendMessage(envelope, message);
        
        mailSender.fireMessageSent(envelope, message, messageSource);
    }
    
    public void notifyConnectionRequestFailed(Throwable exception, boolean isFinal) {
        fireMailStoreRequestFailed(exception, isFinal);
    }
    
    private void showStatus(OutgoingMailClient outgoingClient, String message) {
        MailConnectionManager.getInstance().fireMailConnectionStatus(outgoingClient.getConnectionConfig(), this, message);
    }

    public void fireMailStoreRequestFailed(Throwable exception, boolean isFinal) {
        mailSender.fireMessageSendFailed(envelope, message, exception, isFinal);
    }
}
