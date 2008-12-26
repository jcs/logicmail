/*-
 * Copyright (c) 2008, Derek Konigsberg
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

import java.util.Calendar;

import net.rim.device.api.ui.Keypad;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.component.AutoTextEditField;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.container.VerticalFieldManager;
import net.rim.device.api.util.Arrays;

import org.logicprobe.LogicMail.LogicMailResource;
import org.logicprobe.LogicMail.conf.AccountConfig;
import org.logicprobe.LogicMail.conf.IdentityConfig;
import org.logicprobe.LogicMail.message.Message;
import org.logicprobe.LogicMail.message.MessageEnvelope;
import org.logicprobe.LogicMail.message.MessageFlags;
import org.logicprobe.LogicMail.message.MessagePart;
import org.logicprobe.LogicMail.message.MessagePartFactory;
import org.logicprobe.LogicMail.message.TextPart;
import org.logicprobe.LogicMail.model.AccountNode;
import org.logicprobe.LogicMail.model.MailboxNode;
import org.logicprobe.LogicMail.model.MessageNode;


/**
 * This is the message composition screen.
 */
public class CompositionScreen extends BaseScreen {
    public final static int COMPOSE_NORMAL = 0;
    public final static int COMPOSE_REPLY = 1;
    public final static int COMPOSE_REPLY_ALL = 2;
    public final static int COMPOSE_FORWARD = 3;
    
    private AccountNode accountNode;
    private AccountConfig accountConfig;
    
    private BorderedFieldManager recipientsFieldManager;
	private BorderedFieldManager subjectFieldManager;
	private VerticalFieldManager messageFieldManager;
    private AutoTextEditField subjectEditField;
    private AutoTextEditField messageEditField;

    private String inReplyTo;
    private boolean messageSent;
    private IdentityConfig identityConfig;
    private MessageNode replyToMessageNode;
    
    private MenuItem sendMenuItem = new MenuItem(resources.getString(LogicMailResource.MENUITEM_SEND), 200000, 10) {
            public void run() {
                sendMessage();
            }
        };

    private MenuItem addToMenuItem = new MenuItem(resources.getString(LogicMailResource.MENUITEM_ADD_TO), 200110, 10) {
            public void run() {
                insertRecipientField(EmailAddressBookEditField.ADDRESS_TO);
            }
        };

    private MenuItem addCcMenuItem = new MenuItem(resources.getString(LogicMailResource.MENUITEM_ADD_CC), 200120, 10) {
            public void run() {
                insertRecipientField(EmailAddressBookEditField.ADDRESS_CC);
            }
        };

    private MenuItem addBccMenuItem = new MenuItem(resources.getString(LogicMailResource.MENUITEM_ADD_BCC), 200130, 10) {
            public void run() {
                insertRecipientField(EmailAddressBookEditField.ADDRESS_BCC);
            }
        };

    /**
     * Creates a new instance of CompositionScreen.
     *
     * @param accountNode Account node
     */
    public CompositionScreen(AccountNode accountNode) {
        this.accountNode = accountNode;
        this.accountConfig = accountNode.getAccountConfig();
        this.identityConfig = accountConfig.getIdentityConfig();

        initFields();

        // Add the signature if available
        if (identityConfig != null) {
            String sig = identityConfig.getMsgSignature();

            if ((sig != null) && (sig.length() > 0)) {
                messageEditField.insert("\r\n--\r\n" + sig);
                messageEditField.setCursorPosition(0);
            }
        }
    }

    /**
     * Creates a new instance of CompositionScreen.
     * Used for working with an already created message,
     * such as a draft, reply, or forward.
     *
     * @param accountNode Account node
     * @param messageNode Message we are composing
     * @param composeType Type of message we are creating
     */
    public CompositionScreen(AccountNode accountNode, MessageNode messageNode,
        int composeType) {
        this(accountNode);

        int i;
        this.replyToMessageNode = messageNode;

        Message message = replyToMessageNode.getMessage();

        switch (composeType) {
        case COMPOSE_REPLY:
            message = message.toReplyMessage();
            break;

        case COMPOSE_REPLY_ALL:
            message = message.toReplyAllMessage(identityConfig.getEmailAddress());
            break;

        case COMPOSE_FORWARD:
            message = message.toForwardMessage();
            break;
        }

        MessagePart body = message.getBody();
        MessageEnvelope env = message.getEnvelope();

        // Currently only all-text reply bodies are supported
        if (body instanceof TextPart) {
            messageEditField.insert("\r\n");
            messageEditField.insert(((TextPart) body).getText());
            messageEditField.setCursorPosition(0);
        }

        // Set the subject
        subjectEditField.setText(env.subject);

        // Set the recipients
        if (env.to != null) {
            for (i = 0; i < env.to.length; i++) {
                if (env.to[i].indexOf('@') != -1) {
                    insertRecipientField(EmailAddressBookEditField.ADDRESS_TO)
                        .setAddress(env.to[i]);
                }
            }
        }

        if (env.cc != null) {
            for (i = 0; i < env.cc.length; i++) {
                if (env.cc[i].indexOf('@') != -1) {
                    insertRecipientField(EmailAddressBookEditField.ADDRESS_CC)
                        .setAddress(env.cc[i]);
                }
            }
        }

        if (env.bcc != null) {
            for (i = 0; i < env.bcc.length; i++) {
                if (env.bcc[i].indexOf('@') != -1) {
                    insertRecipientField(EmailAddressBookEditField.ADDRESS_BCC)
                        .setAddress(env.bcc[i]);
                }
            }
        }
        inReplyTo = env.inReplyTo;
    }

    private void initFields() {

    	recipientsFieldManager = new BorderedFieldManager(
        		Manager.NO_HORIZONTAL_SCROLL
        		| Manager.NO_VERTICAL_SCROLL
        		| BorderedFieldManager.BOTTOM_BORDER_NONE);
        recipientsFieldManager.add(new EmailAddressBookEditField(
                EmailAddressBookEditField.ADDRESS_TO, ""));
        recipientsFieldManager.add(new EmailAddressBookEditField(
                EmailAddressBookEditField.ADDRESS_CC, ""));

        subjectFieldManager = new BorderedFieldManager(
        		Manager.NO_HORIZONTAL_SCROLL
        		| Manager.NO_VERTICAL_SCROLL
        		| BorderedFieldManager.BOTTOM_BORDER_LINE);
        subjectEditField = new AutoTextEditField("Subject: ", "");
        subjectFieldManager.add(subjectEditField);
        
        messageFieldManager = new VerticalFieldManager();
        messageEditField = new AutoTextEditField();
        messageFieldManager.add(messageEditField);
        
        add(recipientsFieldManager);
        add(subjectFieldManager);
        add(messageFieldManager);
    }

    public boolean onClose() {
        if (!messageSent &&
                ((subjectEditField.getText().length() > 0) ||
                (messageEditField.getText().length() > 0))) {

        	boolean shouldClose = false;
        	MailboxNode draftMailbox = accountConfig.getDraftMailbox();
        	if(draftMailbox != null) {
        		int choice = Dialog.ask(
        				resources.getString(LogicMailResource.COMPOSITION_PROMPT_SAVE_OR_DISCARD),
        				new Object[] {
        						resources.getString(LogicMailResource.MENUITEM_SAVE_AS_DRAFT),
        						resources.getString(LogicMailResource.MENUITEM_DISCARD),
        						resources.getString(LogicMailResource.MENUITEM_CANCEL) }, 0);
        		if(choice == 0) {
        			// Save as draft, then close
        			Message message = generateMessage();
        			message.getEnvelope().date = Calendar.getInstance().getTime();
        			MessageFlags messageFlags = new MessageFlags(
        					false,
        					false,
        					false,
        					false,
        					true,
        					true,
        					false);
        			draftMailbox.appendMessage(message, messageFlags);
        			shouldClose = true;
        		}
        		else if(choice == 1) {
        			shouldClose = true;
        		}
        	}
        	else {
        		int choice =
        			Dialog.ask(
        					resources.getString(LogicMailResource.COMPOSITION_PROMPT_DISCARD_UNSENT),
        					new Object[] {
        							resources.getString(LogicMailResource.MENUITEM_DISCARD),
        							resources.getString(LogicMailResource.MENUITEM_CANCEL)}, 0);
        		if(choice == 0) { shouldClose = true; }
        	}
        	
            if (shouldClose) {
                close();
                return true;
            } else {
                return false;
            }
        } else {
            close();
            return true;
        }
    }

    protected void makeMenu(Menu menu, int instance) {
        if (((EmailAddressBookEditField) recipientsFieldManager.getField(0)).getText()
                 .length() > 0) {
            menu.add(sendMenuItem);
            menu.addSeparator();
        }

        menu.add(addToMenuItem);
        menu.add(addCcMenuItem);
        menu.add(addBccMenuItem);
        menu.addSeparator();
    }

    private Message generateMessage() {
        // Simplest possible implementation for now,
        // which turns the content of the screen into
        // a message containing a single text/plain section
        MessageEnvelope env = new MessageEnvelope();

        env.inReplyTo = inReplyTo;

        // Build the recipients list
        EmailAddressBookEditField currentField;
        int size = recipientsFieldManager.getFieldCount();

        for (int i = 0; i < size; i++) {
            currentField = (EmailAddressBookEditField) recipientsFieldManager.getField(i);

            if ((currentField.getAddressType() == EmailAddressBookEditField.ADDRESS_TO) &&
                    (currentField.getText().length() > 0)) {
                if (env.to == null) {
                    env.to = new String[1];
                    env.to[0] = currentField.getText();
                } else {
                    Arrays.add(env.to, currentField.getText());
                }
            } else if ((currentField.getAddressType() == EmailAddressBookEditField.ADDRESS_CC) &&
                    (currentField.getText().length() > 0)) {
                if (env.cc == null) {
                    env.cc = new String[1];
                    env.cc[0] = currentField.getText();
                } else {
                    Arrays.add(env.cc, currentField.getText());
                }
            } else if ((currentField.getAddressType() == EmailAddressBookEditField.ADDRESS_BCC) &&
                    (currentField.getText().length() > 0)) {
                if (env.bcc == null) {
                    env.bcc = new String[1];
                    env.bcc[0] = currentField.getText();
                } else {
                    Arrays.add(env.bcc, currentField.getText());
                }
            }
        }

        // Set the sender and reply-to addresses
        // (this comes from identity settings)
        if (identityConfig != null) {
            env.from = new String[1];

            String fullName = identityConfig.getFullName();

            if ((fullName != null) && (fullName.length() > 0)) {
                env.from[0] = "\"" + fullName + "\"" + " <" +
                    identityConfig.getEmailAddress() + ">";
            } else {
                env.from[0] = identityConfig.getEmailAddress();
            }

            String replyToAddress = identityConfig.getReplyToAddress();

            if ((replyToAddress != null) && (replyToAddress.length() > 0)) {
                env.replyTo = new String[1];
                env.replyTo[0] = replyToAddress;
            }
        } else {
            // There are rare situations where the IdentityConfig could be null,
            // such as if the user deleted their identity configuration without
            // editing their account again to force the creation of a default identity.
            // Eventually this should be prevented, but for now we will just elegantly
            // handle the case of missing identity information.
            env.from = new String[1];
            env.from[0] = accountConfig.getServerUser() + "@" +
                accountConfig.getServerName();
        }

        // Set the subject
        env.subject = subjectEditField.getText();

        MessagePart bodyPart = MessagePartFactory.createMessagePart("text",
                "plain", "7bit", "us-ascii", messageEditField.getText());

        Message message = new Message(env, bodyPart);
    	return message;
    }
    
    private void sendMessage() {
    	Message message = generateMessage();
    	
        if (replyToMessageNode != null) {
            accountNode.sendMessageReply(message, replyToMessageNode);
        } else {
            accountNode.sendMessage(message);
        }

        messageSent = true;
        setDirty(false);
        close();
    }

    /**
     * Insert a new recipient field.
     * @param addressType The type of address this field will hold
     * @return The newly added field
     */
    private EmailAddressBookEditField insertRecipientField(int addressType) {
        int size = recipientsFieldManager.getFieldCount();
        EmailAddressBookEditField currentField;
        int i;

        // If a field of this type already exists, and is empty, move
        // focus there instead of adding a new field
        for (i = 0; i < size; i++) {
            currentField = (EmailAddressBookEditField) recipientsFieldManager.getField(i);

            if ((currentField.getAddressType() == addressType) &&
                    (currentField.getText().length() == 0)) {
                currentField.setFocus();

                return currentField;
            }
        }

        // Otherwise, find the appropriate insertion point,
        // and add a new field, and give it focus
        if (addressType == EmailAddressBookEditField.ADDRESS_TO) {
            for (i = 0; i < size; i++) {
                currentField = (EmailAddressBookEditField) recipientsFieldManager.getField(i);

                if (currentField.getAddressType() != EmailAddressBookEditField.ADDRESS_TO) {
                    currentField = new EmailAddressBookEditField(EmailAddressBookEditField.ADDRESS_TO,
                            "");
                    recipientsFieldManager.insert(currentField, i);
                    currentField.setFocus();

                    return currentField;
                }
            }
        } else if (addressType == EmailAddressBookEditField.ADDRESS_CC) {
            i = 0;

            while (i < size) {
                currentField = (EmailAddressBookEditField) recipientsFieldManager.getField(i);

                if ((currentField.getAddressType() == EmailAddressBookEditField.ADDRESS_TO) ||
                        (currentField.getAddressType() == EmailAddressBookEditField.ADDRESS_CC)) {
                    i++;
                } else {
                    currentField = new EmailAddressBookEditField(EmailAddressBookEditField.ADDRESS_CC,
                            "");
                    recipientsFieldManager.insert(currentField, i);
                    currentField.setFocus();

                    return currentField;
                }
            }
        }

        currentField = new EmailAddressBookEditField(addressType, "");
        recipientsFieldManager.add(currentField);
        currentField.setFocus();

        return currentField;
    }

    public boolean keyChar(char key, int status, int time) {
        EmailAddressBookEditField currentField;
        int index;

        switch (key) {
        case Keypad.KEY_BACKSPACE:
            currentField = (EmailAddressBookEditField) recipientsFieldManager.getFieldWithFocus();

            if (currentField == null) {
                break;
            }

            if (recipientsFieldManager.getFieldWithFocusIndex() == 0) {
                break;
            }

            if (currentField.getText().length() > 0) {
                break;
            }

            index = currentField.getIndex();
            recipientsFieldManager.delete(currentField);
            recipientsFieldManager.getField(index - 1).setFocus();

            return true;
        }

        return super.keyChar(key, status, time);
    }
}
