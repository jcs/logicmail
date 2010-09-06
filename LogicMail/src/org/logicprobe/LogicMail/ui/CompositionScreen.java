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

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

import net.rim.device.api.io.MIMETypeAssociations;
import net.rim.device.api.system.Application;
import net.rim.device.api.system.EventLogger;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.Keypad;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.Screen;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.AutoTextEditField;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.util.Arrays;
import net.rim.device.api.util.DataBuffer;

import org.logicprobe.LogicMail.AppInfo;
import org.logicprobe.LogicMail.LogicMailResource;
import org.logicprobe.LogicMail.conf.AccountConfig;
import org.logicprobe.LogicMail.conf.IdentityConfig;
import org.logicprobe.LogicMail.conf.MailSettings;
import org.logicprobe.LogicMail.message.AbstractMimeMessagePartVisitor;
import org.logicprobe.LogicMail.message.ApplicationPart;
import org.logicprobe.LogicMail.message.AudioPart;
import org.logicprobe.LogicMail.message.ContentPart;
import org.logicprobe.LogicMail.message.ImagePart;
import org.logicprobe.LogicMail.message.Message;
import org.logicprobe.LogicMail.message.MessagePart;
import org.logicprobe.LogicMail.message.MimeMessageContent;
import org.logicprobe.LogicMail.message.MimeMessageContentFactory;
import org.logicprobe.LogicMail.message.MessageEnvelope;
import org.logicprobe.LogicMail.message.MessageFlags;
import org.logicprobe.LogicMail.message.MimeMessagePart;
import org.logicprobe.LogicMail.message.MimeMessagePartFactory;
import org.logicprobe.LogicMail.message.MultiPart;
import org.logicprobe.LogicMail.message.TextContent;
import org.logicprobe.LogicMail.message.TextPart;
import org.logicprobe.LogicMail.message.UnsupportedContentException;
import org.logicprobe.LogicMail.message.VideoPart;
import org.logicprobe.LogicMail.model.Address;
import org.logicprobe.LogicMail.model.MailboxNode;
import org.logicprobe.LogicMail.model.MessageNode;
import org.logicprobe.LogicMail.model.MessageNodeEvent;
import org.logicprobe.LogicMail.model.MessageNodeListener;
import org.logicprobe.LogicMail.model.NetworkAccountNode;
import org.logicprobe.LogicMail.util.EventObjectRunnable;
import org.logicprobe.LogicMail.util.UnicodeNormalizer;


/**
 * This is the message composition screen.
 */
public class CompositionScreen extends AbstractScreenProvider {
    public final static int COMPOSE_NORMAL = 0;
    public final static int COMPOSE_REPLY = 1;
    public final static int COMPOSE_REPLY_ALL = 2;
    public final static int COMPOSE_FORWARD = 3;
    
    private int composeType = -1;
    private String initialRecipient;
    private MessageNode sourceMessageNode;
    private NetworkAccountNode accountNode;
    private AccountConfig accountConfig;
    private UnicodeNormalizer unicodeNormalizer;
    
    private BorderedFieldManager recipientsFieldManager;
	private BorderedFieldManager subjectFieldManager;
	private BorderedFieldManager messageFieldManager;
    private AutoTextEditField subjectEditField;
    private AutoTextEditField messageEditField;
    private BorderedFieldManager attachmentsFieldManager;
    
    private String inReplyTo;
    private boolean messageSent;
    private IdentityConfig identityConfig;
    private MessageNode replyToMessageNode;

    private Object generateLock = new Object();
    private Message pendingMessage;
    private Vector pendingLocalAttachments = new Vector();
    private Vector pendingRemoteAttachments = new Vector();
    private Runnable pendingMessageRunnable;
    
    private MenuItem sendMenuItem = new MenuItem(resources.getString(LogicMailResource.MENUITEM_SEND), 300100, 10) {
        public void run() {
            sendMessage();
        }
    };
    private MenuItem saveDraftMenuItem = new MenuItem(resources.getString(LogicMailResource.MENUITEM_SAVE_DRAFT), 300200, 20) {
        public void run() {
            saveAsDraft();
            screen.close();
        }
    };
    private MenuItem addToMenuItem = new MenuItem(resources.getString(LogicMailResource.MENUITEM_ADD_TO), 400100, 1010) {
        public void run() {
            insertRecipientField(EmailAddressBookEditField.ADDRESS_TO);
        }
    };
    private MenuItem addCcMenuItem = new MenuItem(resources.getString(LogicMailResource.MENUITEM_ADD_CC), 400200, 1020) {
        public void run() {
            insertRecipientField(EmailAddressBookEditField.ADDRESS_CC);
        }
    };
    private MenuItem addBccMenuItem = new MenuItem(resources.getString(LogicMailResource.MENUITEM_ADD_BCC), 400300, 1030) {
        public void run() {
            insertRecipientField(EmailAddressBookEditField.ADDRESS_BCC);
        }
    };
    private MenuItem attachFileMenuItem = new MenuItem(resources.getString(LogicMailResource.MENUITEM_ATTACH_FILE), 400400, 1040) {
        public void run() {
            attachFile();
        }
    };
    private MenuItem deleteFieldMenuItem = new MenuItem(resources.getString(LogicMailResource.MENUITEM_DELETE_FIELD), 400900, 1090) {
        public void run() {
            deleteField(false);
        }
    };
    
    private MessageNodeListener messageNodeListener = new MessageNodeListener() {
        public void messageStatusChanged(MessageNodeEvent e) {
            messageNodeListener_MessageStatusChanged(e);
        }
    };

    /**
     * Creates a new instance of CompositionScreen.
     *
     * @param accountNode Account node
     */
    public CompositionScreen(NetworkAccountNode accountNode) {
        this.accountNode = accountNode;
        this.accountConfig = accountNode.getAccountConfig();
        this.identityConfig = accountConfig.getIdentityConfig();
        if(MailSettings.getInstance().getGlobalConfig().getUnicodeNormalization()) {
            unicodeNormalizer = UnicodeNormalizer.getInstance();
        }
    }

    /**
     * Creates a new instance of CompositionScreen.
     *
     * @param accountNode Account node
     * @param recipient Message recipient address to pre-populate the "To" field with
     */
    public CompositionScreen(NetworkAccountNode accountNode, String recipient) {
    	this(accountNode);
    	this.initialRecipient = recipient;
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
    public CompositionScreen(
            NetworkAccountNode accountNode,
    		MessageNode messageNode,
    		int composeType) {
        this.accountNode = accountNode;
        this.accountConfig = accountNode.getAccountConfig();
        this.identityConfig = accountConfig.getIdentityConfig();
        if(MailSettings.getInstance().getGlobalConfig().getUnicodeNormalization()) {
            unicodeNormalizer = UnicodeNormalizer.getInstance();
        }

        this.composeType = composeType;
        this.sourceMessageNode = messageNode;
    }

    private void messageNodeListener_MessageStatusChanged(MessageNodeEvent e) {
        int type = e.getType();
        if(type == MessageNodeEvent.TYPE_STRUCTURE_LOADED
                || type == MessageNodeEvent.TYPE_CONTENT_LOADED) {
            // Immediately remove the listener to avoid redundant calls
            MessageNode messageNode = (MessageNode)e.getSource();
            messageNode.removeMessageNodeListener(messageNodeListener);

            // Schedule the UI update
            UiApplication.getUiApplication().invokeLater(new EventObjectRunnable(e) {
                public void run() {
                    MessageNode messageNode = (MessageNode)getEvent().getSource();
                    populateFromMessage(messageNode);
                    messageEditField.setEditable(true);
                }
            });
        }
    }

    private void populateFromMessage(MessageNode message) {
        int i;
        MimeMessagePart body = message.getMessageStructure();

        PopulatePartsVisitor visitor = new PopulatePartsVisitor();
        body.accept(visitor);
        
        TextPart bodyPart = visitor.getFirstTextPart();
        if(bodyPart != null) {
            MimeMessageContent content = message.getMessageContent(bodyPart);
            if(content instanceof TextContent) {
                messageEditField.insert(normalize(((TextContent)content).getText()));
                messageEditField.setCursorPosition(0);
            }
        }

        Vector attachmentParts = visitor.getAttachmentParts();
        if(attachmentParts.size() > 0) {
            if(attachmentsFieldManager != null) {
                attachmentsFieldManager.deleteAll();
            }
            else {
                attachmentsFieldManager = FieldFactory.getInstance().getBorderedFieldManager(
                        BorderedFieldManager.BOTTOM_BORDER_NORMAL
                        | BorderedFieldManager.OUTER_FILL_NONE);
                messageFieldManager.add(attachmentsFieldManager);
            }
            
            int size = attachmentParts.size();
            for(i = 0; i < size; i++) {
                ContentPart attachmentPart = (ContentPart)attachmentParts.elementAt(i);
                attachmentsFieldManager.add(new AttachmentField(message, attachmentPart));
            }
        }

        // Set the subject
        subjectEditField.setText(normalize(message.getSubject()));

        // Set the recipients
        Address[] recipients = message.getTo();
        if (recipients != null) {
            for (i = 0; i < recipients.length; i++) {
                insertRecipientField(EmailAddressBookEditField.ADDRESS_TO).setText(normalize(recipients[i].toString()));
            }
        }

        recipients = message.getCc();
        if (recipients != null) {
            for (i = 0; i < recipients.length; i++) {
                insertRecipientField(EmailAddressBookEditField.ADDRESS_CC).setText(normalize(recipients[i].toString()));
            }
        }

        recipients = message.getBcc();
        if (recipients != null) {
            for (i = 0; i < recipients.length; i++) {
                insertRecipientField(EmailAddressBookEditField.ADDRESS_BCC).setText(normalize(recipients[i].toString()));
            }
        }

        inReplyTo = message.getInReplyTo();
    }
    
    private static class PopulatePartsVisitor extends AbstractMimeMessagePartVisitor {
        private TextPart firstTextPart;
        private Vector attachmentParts = new Vector();;
        
        public TextPart getFirstTextPart() {
            return firstTextPart;
        }
        
        public Vector getAttachmentParts() {
            return attachmentParts;
        }
        
        public void visitTextPart(TextPart part) {
            if(firstTextPart == null) {
                firstTextPart = part;
            }
            else {
                attachmentParts.addElement(part);
            }
        }
        
        public void visitApplicationPart(ApplicationPart part) {
            attachmentParts.addElement(part);
        }
        public void visitAudioPart(AudioPart part) {
            attachmentParts.addElement(part);
        }
        public void visitImagePart(ImagePart part) {
            attachmentParts.addElement(part);
        }
        public void visitMessagePart(MessagePart part) {
            attachmentParts.addElement(part);
        }
        public void visitVideoPart(VideoPart part) {
            attachmentParts.addElement(part);
        }
    };
    
    private void appendSignature() {
        // Add the signature if available
        if (identityConfig != null) {
            String sig = identityConfig.getMsgSignature();

            if ((sig != null) && (sig.length() > 0)) {
                messageEditField.insert("\r\n-- \r\n" + sig);
                messageEditField.setCursorPosition(0);
            }
        }
    }
    
    public void initFields(Screen screen) {
        super.initFields(screen);

        FieldFactory fieldFactory = FieldFactory.getInstance();
    	recipientsFieldManager = fieldFactory.getBorderedFieldManager(
        		Manager.NO_HORIZONTAL_SCROLL
        		| Manager.NO_VERTICAL_SCROLL
        		| BorderedFieldManager.BOTTOM_BORDER_NONE);
        recipientsFieldManager.add(new EmailAddressBookEditField(
                EmailAddressBookEditField.ADDRESS_TO, ""));
        recipientsFieldManager.add(new EmailAddressBookEditField(
                EmailAddressBookEditField.ADDRESS_CC, ""));

        subjectFieldManager = fieldFactory.getBorderedFieldManager(
        		Manager.NO_HORIZONTAL_SCROLL
        		| Manager.NO_VERTICAL_SCROLL
        		| BorderedFieldManager.BOTTOM_BORDER_LINE);
        subjectEditField = new AutoTextEditField(resources.getString(LogicMailResource.MESSAGEPROPERTIES_SUBJECT) + ' ', "");
        subjectEditField.setFont(subjectEditField.getFont().derive(Font.BOLD));
        subjectFieldManager.add(subjectEditField);
        
        messageFieldManager = new BorderedFieldManager(
                BorderedFieldManager.BOTTOM_BORDER_NORMAL
                | BorderedFieldManager.FILL_NONE
                | Field.USE_ALL_HEIGHT);
        messageEditField = new AutoTextEditField();
		messageEditField.setEditable(false);
        messageFieldManager.add(messageEditField);
        
        screen.add(recipientsFieldManager);
        screen.add(subjectFieldManager);
        screen.add(messageFieldManager);
        
        if(sourceMessageNode == null) {
            appendSignature();
    		messageEditField.setEditable(true);

    		if(initialRecipient != null) {
    		    EmailAddressBookEditField toAddressField =
    		        (EmailAddressBookEditField)recipientsFieldManager.getField(0);
    		    toAddressField.setText(initialRecipient);
    		}
        }
        else if(composeType == COMPOSE_NORMAL) {
        	if(sourceMessageNode.getMessageStructure() != null && sourceMessageNode.hasMessageContent()) {
        		populateFromMessage(sourceMessageNode);
        		messageEditField.setEditable(true);
        	}
        	else {
        		sourceMessageNode.addMessageNodeListener(messageNodeListener);
        		sourceMessageNode.refreshMessage();
        	}
        }
        else
        {
	        this.replyToMessageNode = sourceMessageNode;
	
	        MessageNode populateMessage;
	
	        switch (composeType) {
	        case COMPOSE_REPLY:
	        	populateMessage = sourceMessageNode.toReplyMessage();
	            break;
	        case COMPOSE_REPLY_ALL:
	        	populateMessage = sourceMessageNode.toReplyAllMessage(identityConfig.getEmailAddress());
	            break;
	        case COMPOSE_FORWARD:
	            //TODO: Consider bringing along attachments when forwarding
	        	populateMessage = sourceMessageNode.toForwardMessage();
	            break;
            default:
            	populateMessage = sourceMessageNode;
            	break;
	        }
	        populateFromMessage(populateMessage);
	        appendSignature();
    		messageEditField.setEditable(true);
        }
    }

    public boolean onClose() {
        if (!messageSent &&
                ((subjectEditField.getText().length() > 0) ||
                (messageEditField.getText().length() > 0))) {

        	boolean shouldClose = false;
        	if(accountConfig.getDraftMailbox() != null) {
        		int choice = Dialog.ask(
        				resources.getString(LogicMailResource.COMPOSITION_PROMPT_SAVE_OR_DISCARD),
        				new Object[] {
        						resources.getString(LogicMailResource.MENUITEM_SAVE_AS_DRAFT),
        						resources.getString(LogicMailResource.MENUITEM_DISCARD),
        						resources.getString(LogicMailResource.MENUITEM_CANCEL) }, 0);
        		if(choice == 0) {
        			// Save as draft, then close
        			saveAsDraft();
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
            	screen.close();
                return true;
            } else {
                return false;
            }
        } else {
        	screen.close();
            return true;
        }
    }

    public void makeMenu(Menu menu, int instance) {
        if (((EmailAddressBookEditField) recipientsFieldManager.getField(0))
                .getText().length() > 0) {
            menu.add(sendMenuItem);
        }
        MailboxNode draftMailbox = accountConfig.getDraftMailbox();
        if(draftMailbox != null
                && ((subjectEditField.getText().length() > 0)
                || (messageEditField.getText().length() > 0))) {
            menu.add(saveDraftMenuItem);
        }

        menu.add(addToMenuItem);
        menu.add(addCcMenuItem);
        menu.add(addBccMenuItem);
        menu.add(attachFileMenuItem);
        
        // "Delete field" is shown if the focus is on a recipient field that is
        // not the only recipient field, or if the focus is on an attachment
        // field.
        if((recipientsFieldManager.getFieldWithFocus() != null
                && recipientsFieldManager.getFieldWithFocusIndex() > 0)
                || (attachmentsFieldManager != null
                        && attachmentsFieldManager.getFieldWithFocus() != null
                        && attachmentsFieldManager.getFieldCount() > 0)) {
            menu.add(deleteFieldMenuItem);
        }
    }
    
    public boolean keyChar(char key, int status, int time) {
        switch (key) {
        case Keypad.KEY_BACKSPACE:
            if(deleteField(true)) {
                return true;
            }
            else {
                return super.keyChar(key, status, time);
            }
        default:
            return super.keyChar(key, status, time);
        }
    }
    
    /**
     * Delete the current recipient or attachment field.
     *
     * @param onlyEmpty only delete a recipient field if its contents are empty
     * @return true, if successful
     */
    private boolean deleteField(boolean onlyEmpty) {
        if(recipientsFieldManager.getFieldWithFocus() != null
                && recipientsFieldManager.getFieldWithFocusIndex() > 0) {
            EmailAddressBookEditField currentField =
                (EmailAddressBookEditField) recipientsFieldManager.getFieldWithFocus();
            
            if(onlyEmpty && currentField.getText().length() > 0) {
                return false;
            }
            
            int index = currentField.getIndex();
            recipientsFieldManager.delete(currentField);
            recipientsFieldManager.getField(index - 1).setFocus();
            return true;
        }
        else if(attachmentsFieldManager != null
                && attachmentsFieldManager.getFieldWithFocus() != null
                && attachmentsFieldManager.getFieldCount() > 0) {
            attachmentsFieldManager.delete(attachmentsFieldManager.getFieldWithFocus());
            
            if(attachmentsFieldManager.getFieldCount() == 0) {
                messageFieldManager.delete(attachmentsFieldManager);
                attachmentsFieldManager = null;
            }
            
            return true;
        }
        else {
            return false;
        }
    }
    
    private void saveAsDraft() {
        final MailboxNode draftMailbox = accountConfig.getDraftMailbox();
        final MessageEnvelope envelope = generateEnvelope();
        generateMessage(new Runnable() {
            public void run() {
                envelope.date = Calendar.getInstance().getTime();
                MessageFlags messageFlags = new MessageFlags(
                        false,  // seen
                        false,  // answered
                        false,  // flagged
                        false,  // deleted
                        true,   // draft
                        true,   // recent
                        false,  // forwarded
                        false); // junk
                draftMailbox.appendMessage(envelope, pendingMessage, messageFlags);
                // TODO: Save reply-to information with the draft message
            }
        });
    }

    private MessageEnvelope generateEnvelope() {
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

        // Set the date
        env.date = Calendar.getInstance().getTime();
        
        return env;
    }
    
    private void generateMessage(Runnable generatedRunnable) {
        synchronized(generateLock) {
        	String contentText = messageEditField.getText();
            MimeMessagePart bodyPart = MimeMessagePartFactory.createMimeMessagePart(
            		"text", "plain", null, "7bit", "us-ascii", "", "", contentText.length());
            MimeMessageContent bodyContent;
            try {
    			bodyContent = MimeMessageContentFactory.createContentEncoded(
    					bodyPart, contentText.getBytes());
    		} catch (UnsupportedContentException e) {
    			bodyContent = null;
    		}
            
    		if(attachmentsFieldManager != null) {
    		    MultiPart multiPart = new MultiPart("mixed");
    		    pendingMessage = new Message(multiPart);
    		    multiPart.addPart(bodyPart);
    		    
    		    int count = attachmentsFieldManager.getFieldCount();
    		    for(int i=0; i<count; i++) {
    		        AttachmentField attachmentField =
    		            (AttachmentField)attachmentsFieldManager.getField(i);
                    ContentPart attachmentPart =
                        attachmentField.getMessagePart();
                    MimeMessageContent attachmentContent = null;
                    
    		        MessageNode messageNode = attachmentField.getMessageNode();
    		        if(messageNode != null && messageNode == sourceMessageNode) {
    		            attachmentContent = messageNode.getMessageContent(attachmentPart);
    		            if(attachmentContent == null) {
    		                pendingRemoteAttachments.addElement(attachmentPart);
    		            }
    		        }
    		        else {
        		        String fileUrl = attachmentPart.getTag();
        		        if(fileUrl != null && fileUrl.startsWith("file:///")) {
        		            pendingLocalAttachments.addElement(attachmentPart);
        		        }
    		        }
    		        
    		        if(attachmentContent != null) {
                        multiPart.addPart(attachmentPart);
                        pendingMessage.putContent(attachmentPart, attachmentContent);
    		        }
    		    }
    		}
    		else {
    		    pendingMessage = new Message(bodyPart);
    		}
    		
    		pendingMessage.putContent(bodyPart, bodyContent);
            
    		boolean hasAllData = true;
    		if(pendingLocalAttachments.size() > 0) {
    		    hasAllData = false;
    		    (new Thread() { public void run() {
    		        handlePendingLocalAttachments();
    		    } }).start();
    		}
    		if(pendingRemoteAttachments.size() > 0) {
    		    hasAllData = false;
    		    sourceMessageNode.addMessageNodeListener(pendingAttachmentListener);
    		    int size = pendingRemoteAttachments.size();
    		    for(int i = 0; i < size; i++) {
    		        sourceMessageNode.requestContentPart((ContentPart)pendingRemoteAttachments.elementAt(i));
    		    }
    		}
    		if(hasAllData) {
    		    generatedRunnable.run();
                pendingMessageRunnable = null;
                pendingMessage = null;
    		}
    		else {
    		    pendingMessageRunnable = generatedRunnable;
    		}
        }
    }
    
    private void handlePendingLocalAttachments() {
        synchronized(generateLock) {
            if(pendingLocalAttachments.size() == 0) {
                return;
            }
            
            int size = pendingLocalAttachments.size();
            for(int i=0; i<size; i++) {
                ContentPart attachmentPart = (ContentPart)pendingLocalAttachments.elementAt(i);
                String fileUrl = attachmentPart.getTag();
                byte[] data;
                try {
                    FileConnection fileConnection = (FileConnection)Connector.open(fileUrl);
                    DataInputStream input = fileConnection.openDataInputStream();
    
                    DataBuffer buf = new DataBuffer(1024, true);
                    byte[] rawBuf = new byte[1024];
                    int n;
                    while((n = input.read(rawBuf, 0, 1024)) != -1) {
                        buf.write(rawBuf, 0, n);
                    }
                    data = buf.toArray();
                    
                    input.close();
                    fileConnection.close();
                } catch (IOException e) {
                    EventLogger.logEvent(AppInfo.GUID,
                            ("Error: " + e.getMessage()).getBytes(),
                            EventLogger.ERROR);
                    data = null;
                }
                
                if(data != null && data.length > 0) {
                    try {
                        MimeMessageContent attachmentContent =
                            MimeMessageContentFactory.createContentRaw(attachmentPart, data);
                        MultiPart multiPart = (MultiPart)pendingMessage.getStructure();
                        multiPart.addPart(attachmentPart);
                        pendingMessage.putContent(attachmentPart, attachmentContent);
                    } catch (UnsupportedContentException e) { }
                }
            }
            pendingLocalAttachments.removeAllElements();
            
            if(pendingLocalAttachments.size() == 0 && pendingRemoteAttachments.size() == 0) {
                pendingMessageRunnable.run();
                pendingMessageRunnable = null;
                pendingMessage = null;
            }
        }
    }
    
    private MessageNodeListener pendingAttachmentListener = new MessageNodeListener() {
        public void messageStatusChanged(MessageNodeEvent e) {
            if(e.getType() == MessageNodeEvent.TYPE_CONTENT_LOADED) {
                synchronized(generateLock) {
                    Vector loadedAttachments = new Vector();
                    int size = pendingRemoteAttachments.size();
                    for(int i=0; i<size; i++) {
                        ContentPart attachmentPart = (ContentPart)pendingRemoteAttachments.elementAt(i);
                        MimeMessageContent attachmentContent =
                            sourceMessageNode.getMessageContent(attachmentPart);
                        if(attachmentContent != null) {
                            MultiPart multiPart = (MultiPart)pendingMessage.getStructure();
                            multiPart.addPart(attachmentPart);
                            pendingMessage.putContent(attachmentPart, attachmentContent);
                            loadedAttachments.addElement(attachmentPart);
                        }
                    }
                    size = loadedAttachments.size();
                    for(int i=0; i<size; i++) {
                        pendingRemoteAttachments.removeElement(loadedAttachments.elementAt(i));
                    }
                    
                    if(pendingRemoteAttachments.size() == 0) {
                        sourceMessageNode.removeMessageNodeListener(pendingAttachmentListener);
                    }
                    
                    if(pendingLocalAttachments.size() == 0 && pendingRemoteAttachments.size() == 0) {
                        pendingMessageRunnable.run();
                        pendingMessageRunnable = null;
                        pendingMessage = null;
                    }
                }
            }
        }
    };
    
    private void sendMessage() {
        final MessageEnvelope envelope = generateEnvelope();
        generateMessage(new Runnable() {
            public void run() {
                if (replyToMessageNode != null) {
                    if(composeType == COMPOSE_FORWARD) {
                        accountNode.sendMessageForwarded(envelope, pendingMessage, replyToMessageNode);
                    }
                    else {
                        accountNode.sendMessageReply(envelope, pendingMessage, replyToMessageNode);
                    }
                } else {
                    accountNode.sendMessage(envelope, pendingMessage);
                }
                
                messageSent = true;
                synchronized(Application.getEventLock()) {
                    screen.setDirty(false);
                    screen.close();
                }
            }});
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
    
    /**
     * Attach a file to the current message.
     */
    private void attachFile() {
        String fileUrl = ScreenFactory.getInstance().showFilePicker();
        if(fileUrl != null) {
            ContentPart attachmentPart = handleSelectedFile(fileUrl);
            if(attachmentPart != null) {
                if(attachmentsFieldManager == null) {
                    attachmentsFieldManager = FieldFactory.getInstance().getBorderedFieldManager(
                            BorderedFieldManager.BOTTOM_BORDER_NORMAL
                            | BorderedFieldManager.OUTER_FILL_NONE);
                    messageFieldManager.add(attachmentsFieldManager);
                }
                attachmentsFieldManager.add(new AttachmentField(null, attachmentPart));
            }
        }
    }
    
    private ContentPart handleSelectedFile(String fileUrl) {
        ContentPart mimeContentPart = null;
        try {
            FileConnection fileConnection = (FileConnection)Connector.open(fileUrl);
            if(fileConnection.canRead()) {
                String mimeType = MIMETypeAssociations.getMIMEType(fileUrl);
                if(mimeType == null) {
                    mimeType = "application/octet-stream";
                }
                
                int p = mimeType.indexOf('/');
                String mimeSubtype = mimeType.substring(p + 1);
                mimeType = mimeType.substring(0, p);
                
                p = fileUrl.lastIndexOf('/');
                String fileName = fileUrl.substring(p + 1);
                
                MimeMessagePart part =
                    MimeMessagePartFactory.createMimeMessagePart(
                            mimeType,
                            mimeSubtype,
                            fileName,
                            null,         // encoding
                            null,         // param
                            "attachment", // disposition
                            null,         // content ID
                            (int)fileConnection.fileSize(),
                            fileUrl);
                
                if(part instanceof ContentPart) {
                    mimeContentPart = (ContentPart)part;
                }
            }
            fileConnection.close();
        } catch (IOException e) {
            mimeContentPart = null;
        }
        return mimeContentPart;
    }
    
    /**
     * Run the Unicode normalizer on the provide string,
     * only if normalization is enabled in the configuration.
     * If normalization is disabled, this method returns
     * the input unmodified.
     * 
     * @param input Input string
     * @return Normalized string
     */
    private String normalize(String input) {
        if(unicodeNormalizer == null) {
            return input;
        }
        else {
            return unicodeNormalizer.normalize(input);
        }
    }
}
