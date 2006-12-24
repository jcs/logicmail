/*-
 * Copyright (c) 2006, Derek Konigsberg
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

package org.logicprobe.LogicMail.controller;

import java.io.IOException;
import java.util.Vector;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.Dialog;
import org.logicprobe.LogicMail.mail.FolderTreeItem;
import org.logicprobe.LogicMail.mail.MailClient;
import org.logicprobe.LogicMail.mail.MailException;
import org.logicprobe.LogicMail.message.FolderMessage;
import org.logicprobe.LogicMail.message.MessageEnvelope;
import org.logicprobe.LogicMail.ui.MessageRenderer;
import org.logicprobe.LogicMail.util.Observable;
import org.logicprobe.LogicMail.message.Message;
import org.logicprobe.LogicMail.ui.MailClientHandler;
import org.logicprobe.LogicMail.ui.MessageScreen;

/**
 * Controller for message screens
 */
public class MessageController extends Controller implements Observable {
    private MailClient client;
    private FolderTreeItem folderItem;
    private FolderMessage folderMessage;
    private MessageEnvelope envelope;
    private Vector msgFields;
    
    /** Creates a new instance of MessageController */
    public MessageController(MailClient client,
                             FolderTreeItem folderItem,
                             FolderMessage folderMessage) {
        this.client = client;
        this.folderItem = folderItem;
        this.folderMessage = folderMessage;
        this.envelope = folderMessage.getEnvelope();
        this.msgFields = null;
    }

    public void viewMessage() {
        UiApplication.getUiApplication().pushScreen(new MessageScreen(this));
    }
    
    public void updateMessage() {
        this.client = MailboxController.getInstance().getMailClient();
        MailClientHandler clientHandler = new MailClientHandler(this.client, "Retrieving message") {
            public void runSession() throws IOException, MailException {
                // Set the active folder
                client.setActiveFolder(MessageController.this.folderItem);

                // Download the message
                Message msg = client.getMessage(folderMessage);
                
                // Prepare the UI elements
                MessageRenderer msgRenderer = new MessageRenderer();
                msg.getBody().accept(msgRenderer);
                MessageController.this.msgFields = msgRenderer.getMessageFields();
                
                notifyObservers("message");
            }
        };
        clientHandler.start();        
        
    }
    
    public void showMsgProperties() {
        int i;
        StringBuffer msg = new StringBuffer();
        msg.append("Subject:\n  " + ((envelope.subject!=null) ? envelope.subject : "") + "\n");
        msg.append("Date:\n  " + ((envelope.date!=null) ? envelope.date.toString() : "") + "\n");

        if(envelope.from != null && envelope.from.length > 0) {
            msg.append("From:\n");
            for(i=0;i<envelope.from.length;i++)
                msg.append("  " + ((envelope.from[i]!=null) ? envelope.from[i] : "") + "\n");
        }
        
        if(envelope.to != null && envelope.to.length > 0) {
            msg.append("To:\n");
            for(i=0;i<envelope.to.length;i++)
            if(envelope.to[i].length() > 0)
                msg.append("  " + ((envelope.to[i]!=null) ? envelope.to[i] : "") + "\n");
        }

        if(envelope.cc != null && envelope.cc.length > 0) {
            msg.append("CC:\n");
            for(i=0;i<envelope.cc.length;i++)
                if(envelope.cc[i].length() > 0)
                msg.append("  " + ((envelope.cc[i]!=null) ? envelope.cc[i] : "") + "\n");
        }
        
        if(envelope.bcc != null && envelope.bcc.length > 0) {
            msg.append("BCC:\n");
            for(i=0;i<envelope.bcc.length;i++)
                if(envelope.bcc[i].length() > 0)
                    msg.append("  " + ((envelope.bcc[i]!=null) ? envelope.bcc[i] : "") + "\n");
        }

        Dialog dialog = new Dialog(Dialog.D_OK, msg.toString(),
                                   0, null, Dialog.GLOBAL_STATUS);
        dialog.show();
    }
    
    public MessageEnvelope getMsgEnvelope() {
        return envelope;
    }
    
    public Vector getMessageFields() {
        return msgFields;
    }
}
