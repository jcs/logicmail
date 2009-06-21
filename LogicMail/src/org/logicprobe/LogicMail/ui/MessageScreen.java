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

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

import net.rim.device.api.system.Application;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.system.EventLogger;
import net.rim.device.api.system.KeypadListener;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.FieldChangeListener;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Keypad;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.component.RichTextField;
import net.rim.device.api.ui.component.SeparatorField;
import net.rim.device.api.ui.component.NullField;
import net.rim.device.api.ui.component.Status;
import net.rim.device.api.ui.component.TreeField;
import net.rim.device.api.ui.component.TreeFieldCallback;
import net.rim.device.api.ui.container.VerticalFieldManager;

import org.logicprobe.LogicMail.AppInfo;
import org.logicprobe.LogicMail.LogicMailResource;
import org.logicprobe.LogicMail.conf.AccountConfig;
import org.logicprobe.LogicMail.conf.MailSettings;
import org.logicprobe.LogicMail.message.ContentPart;
import org.logicprobe.LogicMail.message.MessageContent;
import org.logicprobe.LogicMail.message.MessagePart;
import org.logicprobe.LogicMail.message.MessagePartTransformer;
import org.logicprobe.LogicMail.model.AccountNode;
import org.logicprobe.LogicMail.model.Address;
import org.logicprobe.LogicMail.model.MailManager;
import org.logicprobe.LogicMail.model.MailboxNode;
import org.logicprobe.LogicMail.model.MessageNode;
import org.logicprobe.LogicMail.model.MessageNodeEvent;
import org.logicprobe.LogicMail.model.MessageNodeListener;
import org.logicprobe.LogicMail.util.UnicodeNormalizer;

/**
 * Display an E-Mail message
 */
public class MessageScreen extends BaseScreen {
	private BorderedFieldManager addressFieldManager;
	private BorderedFieldManager subjectFieldManager;
	private TreeField attachmentsTreeField;
	private VerticalFieldManager messageFieldManager;
	
	private AccountConfig accountConfig;
    private MessageNode messageNode;
    private boolean isSentFolder;
    private boolean messageRendered;
    private ThrobberField throbberField;

    private UnicodeNormalizer unicodeNormalizer;
    
    public MessageScreen(NavigationController navigationController, MessageNode messageNode)
    {
        super(navigationController, Manager.VERTICAL_SCROLLBAR);
        this.messageNode = messageNode;
        this.accountConfig = messageNode.getParent().getParentAccount().getAccountConfig();
        
        if(MailSettings.getInstance().getGlobalConfig().getUnicodeNormalization()) {
            unicodeNormalizer = UnicodeNormalizer.getInstance();
        }

        // Determine if this screen is viewing a sent message
        int mailboxType = messageNode.getParent().getType();
        this.isSentFolder = (mailboxType == MailboxNode.TYPE_SENT) || (mailboxType == MailboxNode.TYPE_OUTBOX);
     
        initFields();
    }
    
    private void initFields() {
        // Create screen elements
        addressFieldManager = new BorderedFieldManager(
        		Manager.NO_HORIZONTAL_SCROLL
        		| Manager.NO_VERTICAL_SCROLL
        		| BorderedFieldManager.BOTTOM_BORDER_NONE);
        subjectFieldManager = new BorderedFieldManager(
        		Manager.NO_HORIZONTAL_SCROLL
        		| Manager.NO_VERTICAL_SCROLL
        		| BorderedFieldManager.BOTTOM_BORDER_LINE);
        messageFieldManager = new VerticalFieldManager();
        
        if(isSentFolder) {
        	Address[] to = messageNode.getTo();
        	if(to != null && to.length > 0) {
            	addressFieldManager.add(new RichTextField(resources.getString(LogicMailResource.MESSAGEPROPERTIES_TO) + " " + normalize(to[0].toString())));
                if(to.length > 1) {
                    for(int i=1;i<to.length;i++) {
                        if(to[i] != null) {
                        	addressFieldManager.add(new RichTextField("    " + normalize(to[i].toString())));
                        }
                    }
                }
            }
        }
        else {
        	Address[] from = messageNode.getFrom();
            if(from != null && from.length > 0) {
            	addressFieldManager.add(new RichTextField(resources.getString(LogicMailResource.MESSAGEPROPERTIES_FROM) + " " + normalize(from[0].toString())));
                if(from.length > 1) {
                    for(int i=1;i<from.length;i++) {
                        if(from[i] != null) {
                        	addressFieldManager.add(new RichTextField("      " + normalize(from[i].toString())));
                        }
                    }
                }
            }
        }
        String subject = messageNode.getSubject();
        if(subject != null) {
            subjectFieldManager.add(new RichTextField(resources.getString(LogicMailResource.MESSAGEPROPERTIES_SUBJECT) + " " + normalize(subject)));
        }

        add(addressFieldManager);
        add(subjectFieldManager);
        add(messageFieldManager);
    }
    
    private MessageNodeListener messageNodeListener = new MessageNodeListener() {
		public void messageStatusChanged(MessageNodeEvent e) {
			messageNode_MessageStatusChanged(e);
		}
    };
    
    private FieldChangeListener fieldChangeListener = new FieldChangeListener() {
		public void fieldChanged(Field field, int context) {
			message_FieldChanged(field, context);
		}
    };
    
    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.ui.BaseScreen#onDisplay()
     */
    protected void onDisplay() {
    	super.onDisplay();
    	messageNode.addMessageNodeListener(messageNodeListener);
    	if(!messageNode.hasMessageContent()) {
    		if(messageNode.refreshMessage()) {
        		throbberField = new ThrobberField(this.getWidth() / 4, Field.FIELD_HCENTER);
        		add(throbberField);
    		}
    		else {
    			renderMessage();
    		}
    	}
    	else if(!messageRendered) {
    		renderMessage();
    	}
    }

	/* (non-Javadoc)
     * @see org.logicprobe.LogicMail.ui.BaseScreen#onUndisplay()
     */
    protected void onUndisplay() {
    	messageNode.removeMessageNodeListener(messageNodeListener);
        synchronized(Application.getEventLock()) {
    		if(throbberField != null) {
    			this.delete(throbberField);
    			throbberField = null;
    		}
        }
    	super.onUndisplay();
    }
    
    private MenuItem saveAttachmentItem = new MenuItem(resources, LogicMailResource.MENUITEM_SAVE_ATTACHMENT, 100, 10) {
        public void run() {
    		int node = attachmentsTreeField.getCurrentNode();
    		if(node != -1 && attachmentsTreeField.getCookie(node) instanceof ContentPart) {
    			saveAttachment((ContentPart)attachmentsTreeField.getCookie(node));
    		}
        }
    };
    private MenuItem propsItem = new MenuItem(resources, LogicMailResource.MENUITEM_PROPERTIES, 105, 10) {
        public void run() {
        	MessagePropertiesScreen screen = new MessagePropertiesScreen(messageNode);
        	UiApplication.getUiApplication().pushScreen(screen);
        }
    };
    private MenuItem replyItem = new MenuItem(resources, LogicMailResource.MENUITEM_REPLY, 110, 10) {
        public void run() {
            if(messageNode.hasMessageContent()) {
                getNavigationController().displayCompositionReply(messageNode.getParent().getParentAccount(), messageNode, false);
            }
        }
    };
    private MenuItem replyAllItem = new MenuItem(resources, LogicMailResource.MENUITEM_REPLYTOALL, 115, 10) {
        public void run() {
            if(messageNode.hasMessageContent()) {
                getNavigationController().displayCompositionReply(messageNode.getParent().getParentAccount(), messageNode, true);
            }
        }
    };
    private MenuItem forwardItem = new MenuItem(resources, LogicMailResource.MENUITEM_FORWARD, 120, 10) {
        public void run() {
            if(messageNode.hasMessageContent()) {
                getNavigationController().displayCompositionForward(messageNode.getParent().getParentAccount(), messageNode);
            }
        }
    };
    private MenuItem copyToItem = new MenuItem(resources, LogicMailResource.MENUITEM_COPY_TO, 125, 10) {
        public void run() {
            if(messageNode.hasMessageContent()) {
            	AccountNode[] accountNodes = MailManager.getInstance().getMailRootNode().getAccounts();
            	MailboxSelectionDialog dialog = new MailboxSelectionDialog(
            			resources.getString(LogicMailResource.MESSAGE_SELECT_FOLDER_COPY_TO),
            			accountNodes);
            	dialog.setSelectedMailboxNode(messageNode.getParent());
            	dialog.addUnselectableNode(messageNode.getParent());
            	dialog.doModal();
            	
            	MailboxNode selectedMailbox = dialog.getSelectedMailboxNode();
            	if(selectedMailbox != null && selectedMailbox != messageNode.getParent()) {
            		selectedMailbox.appendMessage(messageNode);
            	}
            }
        }
    };
    private MenuItem moveToItem = new MenuItem(resources, LogicMailResource.MENUITEM_MOVE_TO, 130, 10) {
        public void run() {
            if(messageNode.hasMessageContent()) {
            	AccountNode[] accountNodes = MailManager.getInstance().getMailRootNode().getAccounts();
            	MailboxSelectionDialog dialog = new MailboxSelectionDialog(
            			resources.getString(LogicMailResource.MESSAGE_SELECT_FOLDER_MOVE_TO),
            			accountNodes);
            	dialog.setSelectedMailboxNode(messageNode.getParent());
            	dialog.addUnselectableNode(messageNode.getParent());
            	dialog.doModal();
            	
            	MailboxNode selectedMailbox = dialog.getSelectedMailboxNode();
            	if(selectedMailbox != null && selectedMailbox != messageNode.getParent()) {
            		selectedMailbox.appendMessage(messageNode);
            		//TODO: Move To Folder should delete after append
            		//This should only be executed after the append was successful
            		//messageNode.deleteMessage();
            	}
            }
        }
    };
    private MenuItem compositionItem = new MenuItem(resources, LogicMailResource.MENUITEM_COMPOSE_EMAIL, 150, 10) {
        public void run() {
            getNavigationController().displayComposition(messageNode.getParent().getParentAccount());
        }
    };
    private MenuItem closeItem = new MenuItem(resources, LogicMailResource.MENUITEM_CLOSE, 200000, 10) {
        public void run() {
            onClose();
        }
    };
    
    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.ui.BaseScreen#makeMenu(net.rim.device.api.ui.component.Menu, int)
     */
    protected void makeMenu(Menu menu, int instance) {
    	if(this.getFieldWithFocus() == messageFieldManager
    			&& messageFieldManager.getFieldWithFocus() == attachmentsTreeField) {
    		int node = attachmentsTreeField.getCurrentNode();
    		if(node != -1 && attachmentsTreeField.getCookie(node) instanceof MessagePart) {
    			menu.add(saveAttachmentItem);
    		}
    	}
        menu.add(propsItem);
        menu.addSeparator();
        if(accountConfig != null && accountConfig.getOutgoingConfig() != null) {
            menu.add(replyItem);
            if(accountConfig.getIdentityConfig() != null) {
                menu.add(replyAllItem);
            }
            menu.add(forwardItem);
            menu.add(compositionItem);
        }
        menu.add(copyToItem);
        menu.add(moveToItem);
        menu.addSeparator();
        menu.add(closeItem);
    }

    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Screen#keyChar(char, int, int)
     */
    public boolean keyChar(char key,
                           int status,
                           int time)
    {
        boolean retval = false;
        switch(key) {
            case Keypad.KEY_ENTER:
            case Keypad.KEY_SPACE:
            	if(this.getFieldWithFocus() == messageFieldManager
            			&& messageFieldManager.getFieldWithFocus() == attachmentsTreeField) {
            		int node = attachmentsTreeField.getCurrentNode();
            		if(node != -1) {
	            		if(attachmentsTreeField.getCookie(node) instanceof ContentPart) {
	            			saveAttachment((ContentPart)attachmentsTreeField.getCookie(node));
	            			retval = true;
	            		}
	            		else if(attachmentsTreeField.getFirstChild(node) != -1) {
	            			attachmentsTreeField.setExpanded(node, !attachmentsTreeField.getExpanded(node));
	            			retval = true;
	            		}
            		}
            	}
            	else {
	            	if(status == 0) {
	                    scroll(Manager.DOWNWARD);
	                    retval = true;
	                }
	                else if(status == KeypadListener.STATUS_ALT) {
	                    scroll(Manager.UPWARD);
	                    retval = true;
	                }
            	}
                break;
	        }
        return retval;
    }
    
	private void messageNode_MessageStatusChanged(MessageNodeEvent e) {
    	if(e.getType() == MessageNodeEvent.TYPE_CONTENT_LOADED) {
            synchronized(Application.getEventLock()) {
	    		if(throbberField != null) {
	    			this.delete(throbberField);
	    			throbberField = null;
	    		}
            }
    		renderMessage();
    	}
    }

    private void renderMessage() {
		Vector messageFields = new Vector();

		// Add a collapsed TreeField to show attachments, if any exist
    	MessagePart[] attachmentParts = messageNode.getAttachmentParts();
    	if(attachmentParts.length > 0) {
    		attachmentsTreeField = new TreeField(new TreeFieldCallback() {
    			public void drawTreeItem(TreeField treeField, Graphics graphics, int node, int y, int width, int indent) {
    				attachmentsTreeField_DrawTreeItem(treeField, graphics, node, y, width, indent);
    			}}, Field.FOCUSABLE);
    		attachmentsTreeField.setDefaultExpanded(false);
    		int id = attachmentsTreeField.addChildNode(0, resources.getString(LogicMailResource.MESSAGE_ATTACHMENTS));
    		for(int i=attachmentParts.length - 1; i>=0; --i) {
    			attachmentsTreeField.addChildNode(id, attachmentParts[i]);
    		}
    		messageFields.addElement(attachmentsTreeField);
    	}
    	
    	// Add fields to display the message body
    	MessagePart[] displayableParts = MessagePartTransformer.getDisplayableParts(messageNode.getMessageStructure());    	
    	for(int i=0; i<displayableParts.length; i++) {
    		MessageContent content = messageNode.getMessageContent(displayableParts[i]);
    		if(content != null) {
    			Field field = MessageFieldFactory.createMessageField(messageNode, content);
    			messageFields.addElement(field);
    		}
    	}
    	
    	if(messageFields.size() == 0) {
			messageFields.addElement(
					new RichTextField(resources.getString(LogicMailResource.MESSAGE_NOTDISPLAYABLE)));
    	}
    	
		drawMessageFields(messageFields);
		messageRendered = true;
    }

	private void drawMessageFields(Vector messageFields) {
        if(messageFields == null) {
            return;
        }
    	int size = messageFields.size();
        synchronized(Application.getEventLock()) {
        	messageFieldManager.deleteAll();
        	
            for(int i=0;i<size;++i) {
                if(messageFields.elementAt(i) != null) {
                    messageFieldManager.add((Field)messageFields.elementAt(i));
                }
                if(i != size-1) {
                	messageFieldManager.add(new SeparatorField());
                }
            }
            messageFieldManager.add(new NullField(Field.FOCUSABLE));

            for(int i=0;i<size;++i) {
            	Field field = (Field)messageFields.elementAt(i);
            	if(field != null) {
                	MessageFieldFactory.handleRenderedField(field);
            		field.setChangeListener(fieldChangeListener);
            	}
            }
        }
    }

	/**
	 * Save the message attachment.
	 * 
	 * @param contentPart Attachment part to save
	 */
    private void saveAttachment(ContentPart contentPart) {
    	// TODO: Support on-demand downloading of additional content
    	MessageContent content = messageNode.getMessageContent(contentPart);
    	if(content != null) {
        	FileSaveDialog dialog = new FileSaveDialog(contentPart.getName());
    		if(dialog.doModal() != Dialog.CANCEL) {
    			(new SaveAttachmentThread(content, dialog.getFileUrl())).start();
    			Status.show(resources.getString(LogicMailResource.MESSAGE_SAVING_ATTACHMENT));
    		}
    	}
	}

    private static class SaveAttachmentThread extends Thread {
    	private MessageContent content;
    	private String fileUrl;
    	
    	public SaveAttachmentThread(MessageContent content, String fileUrl) {
    		this.content = content;
    		this.fileUrl = fileUrl;
    	}

    	public void run() {
    		boolean success = false;
			byte[] rawData = content.getRawData();
			if(rawData != null) {
				try {
					FileConnection fileConnection = (FileConnection)Connector.open(fileUrl);
					fileConnection.create();
					DataOutputStream outputStream = fileConnection.openDataOutputStream();
					outputStream.write(rawData);
					outputStream.close();
					fileConnection.close();
					success = true;
				} catch (IOException e) {
					EventLogger.logEvent(AppInfo.GUID, ("Unable to save: " + fileUrl).getBytes(), EventLogger.ERROR);
					success = false;
				}
			}
			else {
				// No raw data to save
				success = false;
			}
			
			if(!success) {
				synchronized(UiApplication.getEventLock()) {
					Status.show(resources.getString(LogicMailResource.MESSAGE_UNABLE_TO_SAVE_ATTACHMENT));
				}
			}
    	}
    }
    
	private void attachmentsTreeField_DrawTreeItem(
			TreeField treeField,
			Graphics graphics,
			int node,
			int y,
			int width,
			int indent) {
		Object cookie = attachmentsTreeField.getCookie(node);
		if(cookie instanceof ContentPart) {
			ContentPart messagePart = (ContentPart)cookie;
			
	    	StringBuffer buf = new StringBuffer();
	    	buf.append(messagePart.getName());
	    	if(buf.length() == 0) {
	    		buf.append(messagePart.getMimeType());
	    		buf.append('/');
	    		buf.append(messagePart.getMimeSubtype());
	    	}

	    	int partSize = messagePart.getSize();
	    	if(partSize > 0) {
	    		buf.append(" (");
	    		if(partSize < 1024) {
	    			buf.append(partSize);
	    			buf.append('B');
	    		}
	    		else {
	    			partSize /= 1024;
	    			buf.append(partSize);
	    			buf.append("kB");
	    		}
	    		buf.append(')');
	    	}

	    	Bitmap icon = MessageIcons.getIcon(messagePart);
	    	if(icon != null) {
	    		int rowHeight = treeField.getRowHeight();
	    		int fontHeight = graphics.getFont().getHeight();

	    		graphics.drawBitmap(
	    				indent + (rowHeight/2 - icon.getWidth()/2),
	    				y + (fontHeight/2 - icon.getWidth()/2),
	    				icon.getWidth(),
	    				icon.getHeight(),
	    				icon, 0, 0);
	    		
	    		indent += rowHeight;
	    	}
	    	
	    	Font originalFont = graphics.getFont();
	    	Font displayFont;
	    	if(messageNode.getMessageContent(messagePart) != null) {
	    		displayFont = originalFont.derive(Font.BOLD);
	    	}
	    	else {
	    		displayFont = originalFont;
	    	}
	    	graphics.setFont(displayFont);
	    	graphics.drawText(buf.toString(), indent, y, Graphics.ELLIPSIS, width);
	    	graphics.setFont(originalFont);
		}
		else {
			graphics.drawText(cookie.toString(), indent, y, Graphics.ELLIPSIS, width);
		}
	}

	/**
	 * Invoked by a field when a property changes. 
	 * @param field The field that changed.
	 * @param context Information specifying the origin of the change
	 * 
	 * @see FieldChangeListener#fieldChanged(Field, int)
	 */
    private void message_FieldChanged(Field field, int context) {
    	if(field instanceof BrowserFieldManager) {
    		if((context & BrowserFieldManager.ACTION_SEND_EMAIL) != 0) {
    			String address = ((BrowserFieldManager)field).getSelectedToken();
                getNavigationController().displayComposition(messageNode.getParent().getParentAccount(), address);
    		}
    	}
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
