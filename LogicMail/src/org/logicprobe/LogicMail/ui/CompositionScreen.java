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

import net.rim.device.api.ui.Keypad;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.component.AutoTextEditField;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.component.SeparatorField;
import net.rim.device.api.ui.container.VerticalFieldManager;
import net.rim.device.api.util.Arrays;
import org.logicprobe.LogicMail.conf.AccountConfig;
import org.logicprobe.LogicMail.conf.IdentityConfig;
import org.logicprobe.LogicMail.message.Message;
import org.logicprobe.LogicMail.message.MessageEnvelope;
import org.logicprobe.LogicMail.message.MessagePart;
import org.logicprobe.LogicMail.message.MessagePartFactory;
import org.logicprobe.LogicMail.message.TextPart;

/**
 * This is the message composition screen.
 */
public class CompositionScreen extends BaseScreen {
    private AccountConfig acctConfig;
    //private OutgoingMailClient client;
    private VerticalFieldManager vfmRecipients;
    private AutoTextEditField fldSubject;
    private AutoTextEditField fldEdit;
    private String inReplyTo;
    private boolean messageSent;
    private IdentityConfig identityConfig;
    
    /**
     * Creates a new instance of CompositionScreen.
     *
     * @param acctConfig Account configuration
     */
    public CompositionScreen(AccountConfig acctConfig) {
        this.acctConfig = acctConfig;
        //this.client = MailClientFactory.createOutgoingMailClient(acctConfig);
        vfmRecipients = new VerticalFieldManager();
        vfmRecipients.add(new EmailAddressBookEditField(EmailAddressBookEditField.ADDRESS_TO, ""));
        vfmRecipients.add(new EmailAddressBookEditField(EmailAddressBookEditField.ADDRESS_CC, ""));
        add(vfmRecipients);
        fldSubject = new AutoTextEditField("Subject: ", "");
        add(fldSubject);
        add(new SeparatorField());
        fldEdit = new AutoTextEditField();
        
        this.identityConfig = acctConfig.getIdentityConfig();
        
        // Add the signature if available
        if(identityConfig != null) {
            String sig = identityConfig.getMsgSignature();
            if(sig != null && sig.length() > 0) {
                fldEdit.insert("\r\n--\r\n"+sig);
                fldEdit.setCursorPosition(0);
            }
        }
        add(fldEdit);
    }

    /**
     * Creates a new instance of CompositionScreen.
     * Used for working with an already created message,
     * such as a reply or forward.
     *
     * @param acctConfig Account configuration
     * @param message Message we are composing
     */
    public CompositionScreen(AccountConfig acctConfig, Message message) {
        this(acctConfig);
        int i;

        MessagePart body = message.getBody();
        MessageEnvelope env = message.getEnvelope();
        
        // Currently only all-text reply bodies are supported
        if(body instanceof TextPart) {
            fldEdit.insert("\r\n");
            fldEdit.insert(((TextPart)body).getText());
            fldEdit.setCursorPosition(0);
        }

        // Set the subject
        fldSubject.setText(env.subject);
        
        // Set the recipients
        if(env.to != null) {
            for(i=0; i<env.to.length; i++) {
                insertRecipientField(EmailAddressBookEditField.ADDRESS_TO).setAddress(env.to[i]);
            }
        }
        if(env.cc != null) {
            for(i=0; i<env.cc.length; i++) {
                insertRecipientField(EmailAddressBookEditField.ADDRESS_CC).setAddress(env.cc[i]);
            }
        }
        if(env.bcc != null) {
            for(i=0; i<env.bcc.length; i++) {
                insertRecipientField(EmailAddressBookEditField.ADDRESS_BCC).setAddress(env.bcc[i]);
            }
        }
        
        inReplyTo = env.inReplyTo;
    }

    public boolean onClose() {
        if(!messageSent && (fldSubject.getText().length() > 0 || fldEdit.getText().length() > 0)) {
            if(Dialog.ask(Dialog.D_YES_NO, "Discard unsent message?") == Dialog.YES) {
                close();
                return true;
            }
            else {
                return false;
            }
        }
        else {
            close();
            return true;
        }
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

    /**
     * Get whether the composed message was sent.
     * @return True if sent, false otherwise.
     */
    public boolean getMessageSent() {
        return messageSent;
    }

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
        
        env.inReplyTo = inReplyTo;
        
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
        
        // Set the sender and reply-to addresses
        // (this comes from identity settings)
        if(identityConfig != null) {
            env.from = new String[1];
            String fullName = identityConfig.getFullName();
            if(fullName != null && fullName.length() > 0) {
                env.from[0] = "\"" + fullName + "\"" +
                          " <" + identityConfig.getEmailAddress() + ">";
            }
            else {
                env.from[0] = identityConfig.getEmailAddress();
            }

            String replyToAddress = identityConfig.getReplyToAddress();
            if(replyToAddress != null && replyToAddress.length() > 0) {
                env.replyTo = new String[1];
                env.replyTo[0] = replyToAddress;
            }
        }
        else {
            // There are rare situations where the IdentityConfig could be null,
            // such as if the user deleted their identity configuration without
            // editing their account again to force the creation of a default identity.
            // Eventually this should be prevented, but for now we will just elegantly
            // handle the case of missing identity information.
            env.from = new String[1];
            env.from[0] = acctConfig.getServerUser() + "@" + acctConfig.getServerName();
        }
        
        // Set the subject
        env.subject = fldSubject.getText();
        
        MessagePart bodyPart = MessagePartFactory.createMessagePart("text", "plain", "7bit", "us-ascii", fldEdit.getText());
        
        Message message = new Message(env, bodyPart);
        
        message.getBody(); // prevent unused warning for now
//        SendMessageHandler sendHandler = new SendMessageHandler(message);
//        sendHandler.setListener(this);
//        sendHandler.start();
    }
    
//    /**
//     * Handle completion of sending a message.
//     */
//    public void mailActionComplete(MailClientHandler source, boolean result) {
//        // This should also store the message in a
//        // configured sent-messages folder on the outgoing
//        // mail server if applicable, then close the screen.
//        if(result == true) {
//            messageSent = true;
//            // The following method is now responsible for closing the screen
//            appendMessage(((SendMessageHandler)source).getRawMessage());
//        }
//    }

//    /**
//     * Append the sent message to the sent messages folder, if available.
//     * This method will not return until the operation has completed.
//     */
//    private void appendMessage(String rawMessage) {
//        IncomingMailClient incomingClient = MailClientFactory.createMailClient(acctConfig);
//        if(incomingClient instanceof ImapClient) {
//            String sentFolderPath = ((ImapConfig)incomingClient.getAcctConfig()).getSentFolder();
//            if(sentFolderPath != null) {
//                // The append methods require a FolderTreeItem, but only care about
//                // the path.  Since the path is the only thing easily available here,
//                // we construct a simple instance that only contains the path.
//                FolderTreeItem folderItem = new FolderTreeItem(null, sentFolderPath, null);
//
//                MailClientHandler appendMessageHandler = new AppendMessageHandler((ImapClient)incomingClient, folderItem, rawMessage);
//                appendMessageHandler.setListener(new MailClientHandlerListener() {
//                    public void mailActionComplete(MailClientHandler source, boolean result) {
//                        source.setListener(null);
//                        synchronized(Application.getEventLock()) {
//                            CompositionScreen.this.close();
//                        }
//                    }
//                });
//                appendMessageHandler.start();
//            }
//            else {
//                synchronized(Application.getEventLock()) {
//                    this.close();
//                }
//            }
//        }
//        else {
//            synchronized(Application.getEventLock()) {
//                this.close();
//            }
//        }
//    }
    
//    private class AppendMessageHandler extends MailClientHandler {
//        private FolderTreeItem folderItem;
//        private String rawMessage;
//        
//        public AppendMessageHandler(ImapClient imapClient, FolderTreeItem folderItem, String rawMessage) {
//            super(imapClient, "Storing to sent folder");
//            this.folderItem = folderItem;
//            this.rawMessage = rawMessage;
//        }
//        public void runSession(boolean retry) throws IOException, MailException {
//            ((ImapClient)client).appendMessage(folderItem, rawMessage, true, false);
//        }
//    }
    
    /**
     * Insert a new recipient field.
     * @param addressType The type of address this field will hold
     * @return The newly added field
     */
    private EmailAddressBookEditField insertRecipientField(int addressType) {
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
                return currentField;
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
                    return currentField;
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
                    return currentField;
                }
            }
        }
        currentField = new EmailAddressBookEditField(addressType, "");
        vfmRecipients.add(currentField);
        currentField.setFocus();
        return currentField;
    }

    public boolean keyChar(char key, int status, int time) {
        EmailAddressBookEditField currentField;
        int index;
        switch(key) {
        case Keypad.KEY_BACKSPACE:
            currentField = (EmailAddressBookEditField)vfmRecipients.getFieldWithFocus();
            if(currentField == null) {
                break;
            }
            if(vfmRecipients.getFieldWithFocusIndex() == 0) {
                break;
            }
            if(currentField.getText().length() > 0) {
                break;
            }
            index = currentField.getIndex();
            vfmRecipients.delete(currentField);
            vfmRecipients.getField(index-1).setFocus();
            return true;
        }
        return super.keyChar(key, status, time);
    }

//    /**
//     * Implements the handler for sending messages
//     */
//    private class SendMessageHandler extends MailClientHandler {
//        private Message message;
//        private String rawMessage;
//        
//        public SendMessageHandler(Message message) {
//            super(CompositionScreen.this.client, "Sending message");
//            this.message = message;
//        }
//
//        public void runSession(boolean retry) throws IOException, MailException {
//            rawMessage = ((OutgoingMailClient)client).sendMessage(message);
//            client.close();
//        }
//        
//        public String getRawMessage() {
//            return rawMessage;
//        }
//    }
}
