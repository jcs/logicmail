/*-
 * Copyright (c) 2009, Derek Konigsberg
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

import org.logicprobe.LogicMail.LogicMailResource;
import org.logicprobe.LogicMail.conf.MailSettings;
import org.logicprobe.LogicMail.mail.MessageToken;
import org.logicprobe.LogicMail.mail.NetworkMailSender;
import org.logicprobe.LogicMail.mail.imap.ImapMessageToken;
import org.logicprobe.LogicMail.message.MimeMessageContentFactory;
import org.logicprobe.LogicMail.message.MimeMessagePart;
import org.logicprobe.LogicMail.message.MultiPart;
import org.logicprobe.LogicMail.model.AccountNode;
import org.logicprobe.LogicMail.model.Address;
import org.logicprobe.LogicMail.model.MessageNode;
import org.logicprobe.LogicMail.model.OutgoingMessageNode;
import org.logicprobe.LogicMail.util.StringParser;
import org.logicprobe.LogicMail.util.UnicodeNormalizer;

import net.rim.device.api.i18n.ResourceBundle;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.FieldChangeListener;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.component.ObjectChoiceField;
import net.rim.device.api.ui.component.SeparatorField;
import net.rim.device.api.ui.component.TreeField;
import net.rim.device.api.ui.component.TreeFieldCallback;
import net.rim.device.api.ui.container.MainScreen;
import net.rim.device.api.ui.container.VerticalFieldManager;

/**
 * Screen that displays the properties of a message.
 * Currently displayed properties include header fields
 * and message structure.
 */
public class MessagePropertiesScreen extends MainScreen {
	protected static ResourceBundle resources = ResourceBundle.getBundle(LogicMailResource.BUNDLE_ID, LogicMailResource.BUNDLE_NAME);
    private UnicodeNormalizer unicodeNormalizer;
	private MessageNode messageNode;

	private ObjectChoiceField sectionChoiceField;
	private VerticalFieldManager generalPageManager;
	private VerticalFieldManager structurePageManager;
    private VerticalFieldManager outgoingPageManager;
	private TreeField structureTreeField;
	
	private static String INDENT = "    ";
	
	/**
	 * Instantiates a new message properties screen.
	 * 
	 * @param messageNode the message node
	 */
	public MessagePropertiesScreen(MessageNode messageNode) {
		super(VERTICAL_SCROLL | VERTICAL_SCROLLBAR);
		this.messageNode = messageNode;
        if(MailSettings.getInstance().getGlobalConfig().getUnicodeNormalization()) {
            unicodeNormalizer = UnicodeNormalizer.getInstance();
        }
		initFields();
	}

	private void initFields() {
	    Object[] choices;
	    if(messageNode instanceof OutgoingMessageNode) {
	        choices = new Object[] {
	                resources.getString(LogicMailResource.MESSAGEPROPERTIES_GENERAL),
	                resources.getString(LogicMailResource.MESSAGEPROPERTIES_STRUCTURE),
	                resources.getString(LogicMailResource.MESSAGEPROPERTIES_OUTGOING)
	        };
	    }
	    else {
	        choices = new Object[] {
	                resources.getString(LogicMailResource.MESSAGEPROPERTIES_GENERAL),
	                resources.getString(LogicMailResource.MESSAGEPROPERTIES_STRUCTURE)
	        };
	    }
	    sectionChoiceField = new ObjectChoiceField(
	            resources.getString(LogicMailResource.MESSAGEPROPERTIES_TITLE) + ':',
	            choices);
	    sectionChoiceField.setSelectedIndex(0);
	    sectionChoiceField.setChangeListener(new FieldChangeListener() {
	        public void fieldChanged(Field field, int context) {
	            sectionChoiceField_FieldChanged(field, context);
	        }
	    });

	    initGeneralFields();
	    initStructureFields();
	    if(messageNode instanceof OutgoingMessageNode) {
	        initOutgoingFields();
	    }

	    setTitle(sectionChoiceField);
	    add(generalPageManager);
	}

    private void initGeneralFields() {
		generalPageManager = new VerticalFieldManager();
		generalPageManager.add(new LabelField(
		        resources.getString(LogicMailResource.MESSAGEPROPERTIES_SUBJECT) + ' '
		        + normalize(messageNode.getSubject()), Field.FOCUSABLE));
		generalPageManager.add(new SeparatorField());
		generalPageManager.add(new LabelField(
		        resources.getString(LogicMailResource.MESSAGEPROPERTIES_DATE) + ' '
		        + messageNode.getDate(), Field.FOCUSABLE));
		generalPageManager.add(new SeparatorField());
		initFieldAddress(generalPageManager,
		        resources.getString(LogicMailResource.MESSAGEPROPERTIES_FROM),
		        messageNode.getFrom());
		generalPageManager.add(new SeparatorField());
		initFieldAddress(generalPageManager,
		        resources.getString(LogicMailResource.MESSAGEPROPERTIES_REPLYTO),
		        messageNode.getReplyTo());
		generalPageManager.add(new SeparatorField());
		initFieldAddress(generalPageManager,
		        resources.getString(LogicMailResource.MESSAGEPROPERTIES_TO),
		        messageNode.getTo());
		generalPageManager.add(new SeparatorField());
		initFieldAddress(generalPageManager,
		        resources.getString(LogicMailResource.MESSAGEPROPERTIES_CC),
		        messageNode.getCc());
	}

	private void initFieldAddress(Manager manager, String prefix, Address[] addresses) {
		if(addresses != null) {
			if(addresses.length == 1) {
				manager.add(new LabelField(prefix + ' ' + normalize(addresses[0].toString()), Field.FOCUSABLE));
			}
			else if(addresses.length > 1) {
				manager.add(new LabelField(prefix));
				for(int i=0; i<addresses.length; i++) {
					manager.add(new LabelField("  " + normalize(addresses[i].toString()), Field.FOCUSABLE));
				}
			}
		}
	}
	
	private void initStructureFields() {
		structurePageManager = new VerticalFieldManager();
		
		if(messageNode.getMessageSize() > 0) {
		    LabelField sizeField = new LabelField(
		            resources.getString(LogicMailResource.MESSAGEPROPERTIES_MESSAGE_SIZE) + ' '
		            + StringParser.toDataSizeString(messageNode.getMessageSize()));
		    structurePageManager.add(sizeField);
		}
		
		structureTreeField = new TreeField(new TreeFieldCallback() {
			public void drawTreeItem(TreeField treeField, Graphics graphics, int node, int y, int width, int indent) {
				structureTreeField_DrawTreeItem(treeField, graphics, node, y, width, indent);
			}}, Field.FOCUSABLE);
		structureTreeField.setEmptyString("", 0);
		structureTreeField.setDefaultExpanded(true);
		structureTreeField.setIndentWidth(20);
		MimeMessagePart rootPart = messageNode.getMessageStructure();
		if(rootPart != null) {
			populateStructureTreeField(0, rootPart);
		}
		structurePageManager.add(structureTreeField);
	}
	
	private void populateStructureTreeField(int id, MimeMessagePart part) {
		int newId = structureTreeField.addChildNode(id, part);
		if(part instanceof MultiPart) {
			MultiPart multiPart = (MultiPart)part;
			MimeMessagePart[] children = multiPart.getParts();
			for(int i=children.length - 1; i>=0; --i) {
				populateStructureTreeField(newId, children[i]);
			}
		}
	}

    private void initOutgoingFields() {
        OutgoingMessageNode outgoingNode = (OutgoingMessageNode)messageNode;
        outgoingPageManager = new VerticalFieldManager();
        String sendingAccount = (outgoingNode.getSendingAccount() != null) ? outgoingNode.getSendingAccount().toString() : "";
        outgoingPageManager.add(new LabelField(resources.getString(LogicMailResource.MESSAGEPROPERTIES_SENDING_ACCOUNT)));
        outgoingPageManager.add(new LabelField(INDENT + sendingAccount));

        String mailSender;
        if(outgoingNode.getMailSender() != null
                && outgoingNode.getMailSender() instanceof NetworkMailSender
                && ((NetworkMailSender)outgoingNode.getMailSender()).getOutgoingConfig() != null) {
            mailSender = ((NetworkMailSender)outgoingNode.getMailSender()).getOutgoingConfig().toString();
        }
        else {
            mailSender = "";
        }
        outgoingPageManager.add(new LabelField(resources.getString(LogicMailResource.MESSAGEPROPERTIES_MAIL_SENDER)));
        outgoingPageManager.add(new LabelField(INDENT + mailSender));
                
        if(outgoingNode.getReplyToAccount() != null && outgoingNode.getReplyToToken() != null) {
            outgoingPageManager.add(new SeparatorField());
            outgoingPageManager.add(new LabelField(
                    resources.getString(LogicMailResource.MESSAGEPROPERTIES_REPLYTO)));
            
            String replyToAccount = createMailboxString(outgoingNode.getReplyToAccount(), outgoingNode.getReplyToToken());
            outgoingPageManager.add(new LabelField(replyToAccount));
        }
    }
    
    private String createMailboxString(AccountNode accountNode, MessageToken messageToken) {
        StringBuffer buf = new StringBuffer();
        buf.append(accountNode.toString());
        buf.append(": ");
        if(messageToken instanceof ImapMessageToken) {
            buf.append(((ImapMessageToken)messageToken).getFolderPath());
        }
        else {
            buf.append(messageToken.getMessageUid());
        }
        return buf.toString();
    }
    
    private MenuItem closeItem = new MenuItem(resources, LogicMailResource.MENUITEM_CLOSE, 200000, 10) {
        public void run() {
            onClose();
        }
    };
    
    /* (non-Javadoc)
     * @see net.rim.device.api.ui.container.MainScreen#makeMenu(net.rim.device.api.ui.component.Menu, int)
     */
    protected void makeMenu(Menu menu, int instance) {
    	menu.add(closeItem);
    }

    /* (non-Javadoc)
     * @see net.rim.device.api.ui.container.MainScreen#onSavePrompt()
     */
    protected boolean onSavePrompt() {
    	return true;
    }
    
    private void sectionChoiceField_FieldChanged(Field field, int context) {
    	switch(sectionChoiceField.getSelectedIndex()) {
    	case 0:
        	this.deleteAll();
    		this.add(generalPageManager);
    		break;
    	case 1:
        	this.deleteAll();
    		this.add(structurePageManager);
    		break;
    	case 2:
            this.deleteAll();
            if(outgoingPageManager != null) {
                this.add(outgoingPageManager);
            }
            break;
        }
    }

    private void structureTreeField_DrawTreeItem(
    		TreeField treeField,
			Graphics graphics,
			int node,
			int y,
			int width,
			int indent) {
    	StringBuffer buf = new StringBuffer();
    	MimeMessagePart part = (MimeMessagePart)structureTreeField.getCookie(node);
    	buf.append(part.getMimeType());
    	buf.append('/');
    	buf.append(part.getMimeSubtype());
    	
    	int partSize = part.getSize();
    	if(partSize > 0) {
    		buf.append(" (");
    		buf.append(StringParser.toDataSizeString(partSize));
    		buf.append(')');
    	}

    	// The display rules are as follows:
    	//   Normal: multipart and supported parts that have not been loaded
    	//   Bold:   supported parts that have been loaded
    	//   Italic: unsupported parts
    	Font originalFont = graphics.getFont();
    	Font displayFont;
    	if(part instanceof MultiPart) {
    		displayFont = originalFont;
    	}
    	else if(!MimeMessageContentFactory.isContentSupported(part)) {
    		displayFont = originalFont.derive(Font.ITALIC);
    	}
    	else if(messageNode.getMessageContent(part) != null) {
    		displayFont = originalFont.derive(Font.BOLD);
    	}
    	else {
    		displayFont = originalFont;
    	}
    	
    	graphics.setFont(displayFont);
    	graphics.drawText(buf.toString(), indent, y, Graphics.ELLIPSIS, width);
    	graphics.setFont(originalFont);
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
