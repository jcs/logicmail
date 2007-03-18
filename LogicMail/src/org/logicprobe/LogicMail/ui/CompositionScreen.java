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

package org.logicprobe.LogicMail.ui;

import java.io.IOException;
import net.rim.device.api.system.Application;
import net.rim.device.api.ui.Keypad;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.component.EditField;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.component.SeparatorField;
import net.rim.device.api.ui.container.VerticalFieldManager;
import net.rim.device.api.util.Arrays;
import org.logicprobe.LogicMail.conf.AccountConfig;
import org.logicprobe.LogicMail.conf.MailSettings;
import org.logicprobe.LogicMail.mail.MailClientFactory;
import org.logicprobe.LogicMail.mail.MailException;
import org.logicprobe.LogicMail.mail.OutgoingMailClient;
import org.logicprobe.LogicMail.message.Message;
import org.logicprobe.LogicMail.message.MessageEnvelope;
import org.logicprobe.LogicMail.message.MessagePart;
import org.logicprobe.LogicMail.message.MessagePartFactory;

/**
 * This is the message composition screen.
 */
public class CompositionScreen extends BaseScreen implements MailClientListener {
    private AccountConfig acctConfig;
    private OutgoingMailClient client;
    private VerticalFieldManager vfmRecipients;
    private EditField fldSubject;
    private EditField fldEdit;
    
    /** Creates a new instance of CompositionScreen */
    public CompositionScreen(AccountConfig acctConfig) {
        this.acctConfig = acctConfig;
        this.client = MailClientFactory.createOutgoingMailClient(acctConfig);
        vfmRecipients = new VerticalFieldManager();
        vfmRecipients.add(new EmailAddressBookEditField(EmailAddressBookEditField.ADDRESS_TO, ""));
        vfmRecipients.add(new EmailAddressBookEditField(EmailAddressBookEditField.ADDRESS_CC, ""));
        add(vfmRecipients);
        fldSubject = new EditField("Subject: ", "");
        add(fldSubject);
        add(new SeparatorField());
        fldEdit = new EditField();
        
        // Add the signature if available
        String sig = MailSettings.getInstance().getGlobalConfig().getMsgSignature();
        if(sig != null && sig.length() > 0) {
            fldEdit.insert("\r\n--\r\n"+sig);
            fldEdit.setCursorPosition(0);
        }
        add(fldEdit);
    }

    private MenuItem sendMenuItem = new MenuItem("Send", 200000, 10) {
        public void run() {
            sendMessage();
        }
    };
    private MenuItem addToMenuItem = new MenuItem("Add To:", 200110, 10) {
        public void run() {
            insertRecipientField(EmailAddressBookEditField.ADDRESS_TO);
        }
    };

    private MenuItem addCcMenuItem = new MenuItem("Add Cc:", 200120, 10) {
        public void run() {
            insertRecipientField(EmailAddressBookEditField.ADDRESS_CC);
        }
    };

    private MenuItem addBccMenuItem = new MenuItem("Add Bcc:", 200130, 10) {
        public void run() {
            insertRecipientField(EmailAddressBookEditField.ADDRESS_BCC);
        }
    };


    protected void makeMenu(Menu menu, int instance) {
        if(((EmailAddressBookEditField)vfmRecipients.getField(0)).getText().length() > 0) {
            menu.add(sendMenuItem);
            menu.addSeparator();
        }
        menu.add(addToMenuItem);
        menu.add(addCcMenuItem);
        menu.add(addBccMenuItem);
        menu.addSeparator();
    }
    
    private void sendMessage() {
        // Simplest possible implementation for now,
        // which turns the content of the screen into
        // a message containing a single text/plain section
        MessageEnvelope env = new MessageEnvelope();
        
        // Build the recipients list
        EmailAddressBookEditField currentField;
        int size = vfmRecipients.getFieldCount();
        for(int i=0; i<size; i++) {
            currentField = (EmailAddressBookEditField)vfmRecipients.getField(i);
            if(currentField.getAddressType() == EmailAddressBookEditField.ADDRESS_TO &&
               currentField.getText().length() > 0) {
                if(env.to == null) {
                    env.to = new String[1];
                    env.to[0] = currentField.getText();
                }
                else {
                    Arrays.add(env.to, currentField.getText());
                }
            }
            else if(currentField.getAddressType() == EmailAddressBookEditField.ADDRESS_CC &&
                    currentField.getText().length() > 0) {
                if(env.cc == null) {
                    env.cc = new String[1];
                    env.cc[0] = currentField.getText();
                }
                else {
                    Arrays.add(env.cc, currentField.getText());
                }
            }
            else if(currentField.getAddressType() == EmailAddressBookEditField.ADDRESS_BCC &&
                    currentField.getText().length() > 0) {
                if(env.bcc == null) {
                    env.bcc = new String[1];
                    env.bcc[0] = currentField.getText();
                }
                else {
                    Arrays.add(env.bcc, currentField.getText());
                }
            }
        }
        
        // Set the sender
        // (this should come from global or account settings)
        env.from = new String[1];
        String fromAddress = acctConfig.getSmtpFromAddress();
        if(fromAddress == null || fromAddress.length() == 0)
            fromAddress = acctConfig.getServerUser() + "@" + acctConfig.getServerName();
        
        env.from[0] = MailSettings.getInstance().getGlobalConfig().getFullname() +
                      " <" + fromAddress + ">";
        
        // Set the subject
        env.subject = fldSubject.getText();
        
        MessagePart bodyPart = MessagePartFactory.createMessagePart("text", "plain", "7bit", "us-ascii", fldEdit.getText());
        
        Message message = new Message(env, bodyPart);
        
        SendMessageHandler sendHandler = new SendMessageHandler(message);
        sendHandler.setMailClientListener(this);
        sendHandler.start();
    }
    
    /**
     * Handle completion of sending a message.
     */
    public void mailActionComplete(MailClientHandler source, boolean result) {
        // If the mail sent successfully, close the screen.
        // Ideally, this should also store the message in a
        // configured sent-messages folder on the outgoing
        // mail server if applicable.
        if(result == true) {
            synchronized(Application.getEventLock()) {
                this.close();
            }
        }
    }

    
    private void insertRecipientField(int addressType) {
        int size = vfmRecipients.getFieldCount();
        EmailAddressBookEditField currentField;
        int i;

        // If a field of this type already exists, and is empty, move
        // focus there instead of adding a new field
        for(i=0; i<size; i++) {
            currentField = (EmailAddressBookEditField)vfmRecipients.getField(i);
            if(currentField.getAddressType() == addressType &&
               currentField.getText().length() == 0) {
                currentField.setFocus();
                return;
            }
        }
        
        // Otherwise, find the appropriate insertion point,
        // and add a new field, and give it focus
        if(addressType == EmailAddressBookEditField.ADDRESS_TO) {
            for(i=0; i<size; i++) {
                currentField = (EmailAddressBookEditField)vfmRecipients.getField(i);
                if(currentField.getAddressType() != EmailAddressBookEditField.ADDRESS_TO) {
                    currentField = new EmailAddressBookEditField(EmailAddressBookEditField.ADDRESS_TO, "");
                    vfmRecipients.insert(currentField, i);
                    currentField.setFocus();
                    return;
                }
            }
        }
        else if(addressType == EmailAddressBookEditField.ADDRESS_CC) {
            i = 0;
            while(i < size) {
                currentField = (EmailAddressBookEditField)vfmRecipients.getField(i);
                if(currentField.getAddressType() == EmailAddressBookEditField.ADDRESS_TO ||
                   currentField.getAddressType() == EmailAddressBookEditField.ADDRESS_CC)
                    i++;
                else {
                    currentField = new EmailAddressBookEditField(EmailAddressBookEditField.ADDRESS_CC, "");
                    vfmRecipients.insert(currentField, i);
                    currentField.setFocus();
                    return;
                }
            }
        }
        currentField = new EmailAddressBookEditField(addressType, "");
        vfmRecipients.add(currentField);
        currentField.setFocus();
    }

    public boolean keyChar(char key, int status, int time) {
        EmailAddressBookEditField currentField;
        int index;
        switch(key) {
        case Keypad.KEY_BACKSPACE:
            currentField = (EmailAddressBookEditField)vfmRecipients.getFieldWithFocus();
            if(currentField == null) break;
            if(vfmRecipients.getFieldWithFocusIndex() == 0) break;
            if(currentField.getText().length() > 0) break;
            index = currentField.getIndex();
            vfmRecipients.delete(currentField);
            vfmRecipients.getField(index-1).setFocus();
            return true;
        }
        return super.keyChar(key, status, time);
    }

    /**
     * Implements the handler for sending messages
     */
    private class SendMessageHandler extends MailClientHandler {
        private Message message;

        public SendMessageHandler(Message message) {
            super(CompositionScreen.this.client, "Sending message");
            this.message = message;
        }

        public void runSession() throws IOException, MailException {
            ((OutgoingMailClient)client).sendMessage(message);
            client.close();
        }
    }
}
