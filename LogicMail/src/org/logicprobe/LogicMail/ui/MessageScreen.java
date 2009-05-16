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

import java.util.Vector;
import net.rim.device.api.system.Application;
import net.rim.device.api.system.KeypadListener;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Keypad;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.component.RichTextField;
import net.rim.device.api.ui.component.SeparatorField;
import net.rim.device.api.ui.component.NullField;
import net.rim.device.api.ui.container.VerticalFieldManager;
import org.logicprobe.LogicMail.LogicMailResource;
import org.logicprobe.LogicMail.conf.AccountConfig;
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

/**
 * Display an E-Mail message
 */
public class MessageScreen extends BaseScreen {
	private BorderedFieldManager addressFieldManager;
	private BorderedFieldManager subjectFieldManager;
	private VerticalFieldManager messageFieldManager;
	
	private AccountConfig accountConfig;
    private MessageNode messageNode;
    private boolean isSentFolder;
    private boolean messageRendered;
    private ThrobberField throbberField;
    
    public MessageScreen(NavigationController navigationController, MessageNode messageNode)
    {
        super(navigationController, Manager.VERTICAL_SCROLLBAR);
        this.messageNode = messageNode;
        this.accountConfig = messageNode.getParent().getParentAccount().getAccountConfig();
        
        // Determine if this screen is viewing a sent message
        int mailboxType = messageNode.getParent().getType();
        this.isSentFolder = (mailboxType == MailboxNode.TYPE_SENT) || (mailboxType == MailboxNode.TYPE_OUTBOX);
        
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
            	addressFieldManager.add(new RichTextField(resources.getString(LogicMailResource.MESSAGEPROPERTIES_TO) + " " + to[0]));
                if(to.length > 1) {
                    for(int i=1;i<to.length;i++) {
                        if(to[i] != null) {
                        	addressFieldManager.add(new RichTextField("    " + to[i]));
                        }
                    }
                }
            }
        }
        else {
        	Address[] from = messageNode.getFrom();
            if(from != null && from.length > 0) {
            	addressFieldManager.add(new RichTextField(resources.getString(LogicMailResource.MESSAGEPROPERTIES_FROM) + " " + from[0]));
                if(from.length > 1) {
                    for(int i=1;i<from.length;i++) {
                        if(from[i] != null) {
                        	addressFieldManager.add(new RichTextField("      " + from[i]));
                        }
                    }
                }
            }
        }
        String subject = messageNode.getSubject();
        if(subject != null) {
            subjectFieldManager.add(new RichTextField(resources.getString(LogicMailResource.MESSAGEPROPERTIES_SUBJECT) + " " + subject));
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
    
    protected void onDisplay() {
    	super.onDisplay();
    	messageNode.addMessageNodeListener(messageNodeListener);
    	if(!messageNode.hasMessageContent()) {
    		throbberField = new ThrobberField(this.getWidth() / 4, Field.FIELD_HCENTER);
    		add(throbberField);
    		messageNode.refreshMessage();
    	}
    	else if(!messageRendered) {
    		renderMessage();
    	}
    }

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
    
    private MenuItem propsItem = new MenuItem(resources, LogicMailResource.MENUITEM_PROPERTIES, 100, 10) {
        public void run() {
        	//MessagePropertiesDialog dialog = new MessagePropertiesDialog(messageNode);
        	//dialog.doModal();
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
    
    protected void makeMenu(Menu menu, int instance) {
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

    public boolean keyChar(char key,
                           int status,
                           int time)
    {
        boolean retval = false;
        switch(key) {
            case Keypad.KEY_ENTER:
            case Keypad.KEY_SPACE:
                if(status == 0) {
                    scroll(Manager.DOWNWARD);
                    retval = true;
                }
                else if(status == KeypadListener.STATUS_ALT) {
                    scroll(Manager.UPWARD);
                    retval = true;
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
    	MessagePart[] displayableParts = MessagePartTransformer.getDisplayableParts(messageNode.getMessageStructure());
		Vector messageFields = new Vector();
    	for(int i=0; i<displayableParts.length; i++) {
    		MessageContent content = messageNode.getMessageContent(displayableParts[i]);
    		if(content != null) {
    			Field field = MessageFieldFactory.createMessageField(content);
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
        synchronized(Application.getEventLock()) {
            int size = messageFields.size();
            for(int i=0;i<size;++i) {
                if(messageFields.elementAt(i) != null) {
                    messageFieldManager.add((Field)messageFields.elementAt(i));
                }
                if(i != size-1) {
                	messageFieldManager.add(new SeparatorField());
                }
            }
            messageFieldManager.add(new NullField(Field.FOCUSABLE));
        }
    }
}
