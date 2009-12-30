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

import java.util.Hashtable;
import java.util.Vector;

import net.rim.device.api.i18n.ResourceBundle;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Keypad;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.Screen;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.container.VerticalFieldManager;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.util.Comparator;

import org.logicprobe.LogicMail.LogicMailResource;
import org.logicprobe.LogicMail.conf.MailSettings;
import org.logicprobe.LogicMail.model.MailboxNode;
import org.logicprobe.LogicMail.model.MailboxNodeEvent;
import org.logicprobe.LogicMail.model.MailboxNodeListener;
import org.logicprobe.LogicMail.model.MessageNode;
import org.logicprobe.LogicMail.model.MessageNodeEvent;
import org.logicprobe.LogicMail.model.MessageNodeListener;
import org.logicprobe.LogicMail.util.EventObjectRunnable;

/**
 * Display the active mailbox listing.
 * If the supplied folder matches the configured sent folder
 * for the provided account, then the display fields will be
 * adjusted accordingly.
 */
public class MailboxScreen extends AbstractScreenProvider {
	private static final int SHORTCUT_COMPOSE = 0;
	private static final int SHORTCUT_UP = 3;
	private static final int SHORTCUT_DOWN = 4;
	
	private MailboxNode mailboxNode;
    private Vector knownMessages;
    private Hashtable messageFieldMap;
    private boolean firstDisplay = true;
    private MailSettings mailSettings;
    private VerticalFieldManager messageFieldManager;
    private Screen screen;
    private MessageActions messageActions;
    private boolean composeEnabled;

    private MenuItem selectItem;
	private MenuItem propertiesItem;
	private MenuItem compositionItem;
	private MenuItem deleteItem;
	private MenuItem undeleteItem;
    
    /**
     * Initializes a new MailboxScreen to view the provided mailbox.
     * 
     * @param mailboxNode Mailbox node to view.
     */
    public MailboxScreen(MailboxNode mailboxNode) {
    	this.mailboxNode = mailboxNode;
    	this.knownMessages = new Vector();
    	this.messageFieldMap = new Hashtable();
    	mailSettings = MailSettings.getInstance();
    }

    public String getTitle() {
    	return mailboxNode.toString();
    }
    
    /**
     * Gets the mailbox node being displayed by this screen.
     * 
     * @return The mailbox node
     */
    public MailboxNode getMailboxNode() {
    	return mailboxNode;
    }
    
    /** The mailbox node listener. */
    private MailboxNodeListener mailboxNodeListener = new MailboxNodeListener() {
		public void mailboxStatusChanged(MailboxNodeEvent e) {
			UiApplication.getUiApplication().invokeLater(new EventObjectRunnable(e) {
				public void run() {
					mailboxNode_MailboxStatusChanged((MailboxNodeEvent)getEvent());
				}
			});
		}
    };
    
    /** The message node listener. */
    private MessageNodeListener messageNodeListener = new MessageNodeListener() {
		public void messageStatusChanged(MessageNodeEvent e) {
			UiApplication.getUiApplication().invokeLater(new EventObjectRunnable(e) {
				public void run() {
					messageNode_MessageStatusChanged((MessageNodeEvent)getEvent());
				}
			});
		}
    };

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.ui.BaseScreen#onDisplay()
     */
    public void onDisplay() {
        this.mailboxNode.addMailboxNodeListener(mailboxNodeListener);
        if(firstDisplay) {
            MessageNode[] initialMessages = this.mailboxNode.getMessages();
            for(int i=0; i<initialMessages.length; i++) {
            	knownMessages.addElement(initialMessages[i]);
            	if(isMessageDisplayable(initialMessages[i])) {
            		insertDisplayableMessage(initialMessages[i]);
            	}
            }
            
        	this.mailboxNode.refreshMessages();
        	firstDisplay = false;
        }
        int size = knownMessages.size();
        for(int i=0; i<size; i++) {
        	((MessageNode)knownMessages.elementAt(i)).addMessageNodeListener(messageNodeListener);
        }
        
        composeEnabled = mailboxNode.getParentAccount().hasMailSender();
        ((StandardScreen)screen).setShortcutEnabled(SHORTCUT_COMPOSE, composeEnabled);
        
        // TODO: Support message list changes between display pushing
        // TODO: Support updating when messages are deleted
    }
    
	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.ui.BaseScreen#onUndisplay()
	 */
	public void onUndisplay() {
        this.mailboxNode.removeMailboxNodeListener(mailboxNodeListener);
        int size = knownMessages.size();
        for(int i=0; i<size; i++) {
        	((MessageNode)knownMessages.elementAt(i)).removeMessageNodeListener(messageNodeListener);
        }
    }
    
	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.ui.AbstractScreenProvider#hasShortcuts()
	 */
	public boolean hasShortcuts() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.ui.AbstractScreenProvider#getShortcuts()
	 */
	public ShortcutItem[] getShortcuts() {
		// Note: This method is only called once, during initialization of the screen,
		// and only on devices that have touchscreen support.  The strings for the
		// shortcuts are contained within the main application library's resources.
		// However, the icons are contained within the platform support library
		// containing actual touchscreen API support.
		return new ShortcutItem[] {
			new ShortcutItem(
					SHORTCUT_COMPOSE,
					resources.getString(LogicMailResource.MENUITEM_COMPOSE_EMAIL),
					"shortcut-compose.png", "shortcut-compose-d.png"),
			null,
			null,
			new ShortcutItem(
					SHORTCUT_UP,
					resources.getString(LogicMailResource.MENUITEM_SCROLL_UP),
					"shortcut-up.png", "shortcut-up-d.png"),
			new ShortcutItem(
					SHORTCUT_DOWN,
					resources.getString(LogicMailResource.MENUITEM_SCROLL_DOWN),
					"shortcut-down.png", "shortcut-down-d.png")
		};
	}
	
	/**
	 * Initializes the fields.
	 */
	public void initFields(Screen screen) {
		messageFieldManager = new VerticalFieldManager(Manager.USE_ALL_WIDTH | Manager.USE_ALL_HEIGHT) {
			protected boolean navigationMovement(int dx, int dy, int status, int time) {
				// Prevent downward navigation if on the last field in the message list.
				// This is necessary to prevent weird scrolling behavior.
				if(dy > 0 && messageFieldManager.getFieldWithFocusIndex() == messageFieldManager.getFieldCount() - 1) {
					return true;
				}
				else {
					return super.navigationMovement(dx, dy, status, time);
				}
			}
		};
        screen.add(messageFieldManager);
        this.screen = screen;
    	this.messageActions = new MessageActions(navigationController);
        initMenuItems();
	}    
	
	private void initMenuItems() {
	    selectItem = new MailboxMessageMenuItem(resources, LogicMailResource.MENUITEM_SELECT, 100, 10) {
	        public void runNode(MessageNode messageNode) {
	        	messageActions.openMessage(messageNode);
	        }
	    };
	    propertiesItem = new MailboxMessageMenuItem(resources, LogicMailResource.MENUITEM_PROPERTIES, 105, 10) {
	        public void runNode(MessageNode messageNode) {
	            messageActions.openMessageProperties(messageNode);
	        }
	    };
	    compositionItem = new MenuItem(resources, LogicMailResource.MENUITEM_COMPOSE_EMAIL, 120, 10) {
	        public void run() {
	        	navigationController.displayComposition(mailboxNode.getParentAccount());
	        }
	    };
	    deleteItem = new MailboxMessageMenuItem(resources, LogicMailResource.MENUITEM_DELETE, 130, 10) {
	        public void runNode(MessageNode messageNode) {
	        	messageActions.deleteMessage(messageNode);
	        }
	    };
	    undeleteItem = new MailboxMessageMenuItem(resources, LogicMailResource.MENUITEM_UNDELETE, 135, 10) {
	        public void runNode(MessageNode messageNode) {
	        	messageActions.undeleteMessage(messageNode);
	        }
	    };
	}

	private abstract class MailboxMessageMenuItem extends MenuItem {
		public MailboxMessageMenuItem(ResourceBundle bundle, int id, int ordinal, int priority) {
			super(bundle, id, ordinal, priority);
		}

		public final void run() {
			MessageNode messageNode = getSelectedMessage();
			if(messageNode != null) {
				runNode(messageNode);
			}
		}
		
		public abstract void runNode(MessageNode messageNode);
	}

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.ui.BaseScreen#makeMenu(net.rim.device.api.ui.component.Menu, int)
     */
    public void makeMenu(Menu menu, int instance) {
    	Field fieldWithFocus = messageFieldManager.getFieldWithFocus();
    	if(fieldWithFocus instanceof MailboxMessageField) {
    		menu.add(selectItem);
    		menu.add(propertiesItem);
    	}
        if(composeEnabled) {
            menu.add(compositionItem);
        }
        if(fieldWithFocus instanceof MailboxMessageField) {
        	MessageNode messageNode = ((MailboxMessageField)fieldWithFocus).getMessageNode();
            if((messageNode.getFlags() & MessageNode.Flag.DELETED) != 0) {
                if(mailboxNode.getParentAccount().hasUndelete()) {
                    menu.add(undeleteItem);
                }
            }
            else {
                menu.add(deleteItem);
            }
        }
    }

    
    public boolean onClose() {
        // Check for deleted messages in the mailbox
        if(mailboxNode.getParentAccount().hasExpunge()
                && mailboxNode.hasDeletedMessages()) {
            // Prompt for expunge if possible and supported
            int choice = Dialog.ask(
                    Dialog.D_YES_NO,
                    resources.getString(LogicMailResource.MAILBOX_EXPUNGE_PROMPT),
                    Dialog.YES);
            
            // Request expunge if desired
            if(choice == Dialog.YES) {
                mailboxNode.expungeDeletedMessages();
            }
        }
        
        // Close the screen
        screen.close();
        return true;
    }
    
    /**
     * Handles mailbox status change events.
     * 
     * @param e Event data.
     */
    private void mailboxNode_MailboxStatusChanged(MailboxNodeEvent e) {
        int type = e.getType();
    	if(type == MailboxNodeEvent.TYPE_NEW_MESSAGES) {
    		MessageNode[] messageNodes = e.getAffectedMessages();
    		for(int i=0; i<messageNodes.length; i++) {
    			knownMessages.addElement(messageNodes[i]);
    			
    			if(isMessageDisplayable(messageNodes[i])) {
    				// Insert the message
	    			insertDisplayableMessage(messageNodes[i]);
    			}
    			
    			if(screen != null && screen.isDisplayed()) {
    				messageNodes[i].addMessageNodeListener(messageNodeListener);
    			}
    		}
    	}
    	else if(type == MailboxNodeEvent.TYPE_DELETED_MESSAGES) {
            MessageNode[] messageNodes = e.getAffectedMessages();
            for(int i=0; i<messageNodes.length; i++) {
                if(screen != null && screen.isDisplayed()) {
                    messageNodes[i].removeMessageNodeListener(messageNodeListener);
                }
                removeDisplayableMessage(messageNodes[i])              ;  
                knownMessages.removeElement(messageNodes[i]);
            }
    	}
	}
    
    /**
     * Determines whether a message should be displayed,
     * per the configuration.
     * 
     * @param messageNode Message to check.
     * 
     * @return True if it should be displayed, false otherwise.
     */
    private boolean isMessageDisplayable(MessageNode messageNode) {
    	if((messageNode.getFlags() & MessageNode.Flag.DELETED) != 0 &&
    	   mailSettings.getGlobalConfig().getHideDeletedMsg()) {
    		return false;
    	}
    	else {
    		return true;
    	}
    }
    
    /**
     * Insert a message into the list and associated data structures.
     * This will insert into the correct order, per the configuration.
     * 
     * @param messageNode Message to insert.
     */
    private void insertDisplayableMessage(MessageNode messageNode) {
    	// TODO: Make sorting by mailbox order vs message date an option
    	int selectedIndex = messageFieldManager.getFieldWithFocusIndex();
    	
		if(messageFieldManager.getFieldCount() > 0) {
			Comparator comparator = MessageNode.getComparator();
			int index = messageFieldManager.getFieldCount();
			
			if(mailSettings.getGlobalConfig().getDispOrder()) {
				// Ascending order
				MessageNode lastMessage = getLastDisplayedMessage(index - 1);
				while(lastMessage != null && index > 0 && comparator.compare(lastMessage, messageNode) >= 0) {
					index--;
					if(index > 0) { lastMessage = getLastDisplayedMessage(index - 1); }
				}
			}
			else {
				// Descending order
				MessageNode lastMessage = getLastDisplayedMessage(index - 1);
				while(lastMessage != null && index > 0 && comparator.compare(lastMessage, messageNode) <= 0) {
					index--;
					if(index > 0) { lastMessage = getLastDisplayedMessage(index - 1); }
				}
			}
			MailboxMessageField mailboxMessageField =
				new MailboxMessageField(mailboxNode, messageNode, Field.USE_ALL_WIDTH | Field.FOCUSABLE);
			messageFieldMap.put(messageNode, mailboxMessageField);
			messageFieldManager.insert(
					mailboxMessageField,
					index);
			if(selectedIndex != -1) { messageFieldManager.getField(selectedIndex).setFocus(); }
		}
		else {
			MailboxMessageField mailboxMessageField =
				new MailboxMessageField(mailboxNode, messageNode, Field.USE_ALL_WIDTH | Field.FOCUSABLE);
			messageFieldMap.put(messageNode, mailboxMessageField);
			messageFieldManager.insert(
					mailboxMessageField,
					0);
			if(selectedIndex != -1) { messageFieldManager.getField(selectedIndex).setFocus(); }
		}
    }

    /**
     * Remove a message from the list and associated data structures,
     * if that message exists in the list.
     * 
     * @param messageNode Message to insert.
     */
    private void removeDisplayableMessage(MessageNode messageNode) {
        MailboxMessageField mailboxMessageField = (MailboxMessageField)messageFieldMap.remove(messageNode);
        if(mailboxMessageField == null) { return; }

        messageFieldManager.delete(mailboxMessageField);
    }
    
    /**
     * Gets the last displayed message.
     * 
     * @param index the index
     * 
     * @return the last displayed message
     */
    private MessageNode getLastDisplayedMessage(int index) {
    	while(index >= 0) {
    		if(messageFieldManager.getField(index) instanceof MailboxMessageField) {
    			return ((MailboxMessageField)messageFieldManager.getField(index)).getMessageNode();
    		}
    		index--;
    	}
    	return null;
    }
    
    /**
     * Handles message status change events.
     * 
     * @param e Event data.
     */
	private void messageNode_MessageStatusChanged(MessageNodeEvent e) {
		if(e.getType() == MessageNodeEvent.TYPE_FLAGS) {
			MessageNode messageNode = (MessageNode)e.getSource();
			boolean currentlyDisplayed = messageFieldMap.containsKey(messageNode);
			boolean displayable = isMessageDisplayable(messageNode);
			
			if(currentlyDisplayed && !displayable) {
				// Remove from display
				MailboxMessageField mailboxMessageField = (MailboxMessageField)messageFieldMap.get(messageNode);
				messageFieldManager.delete(mailboxMessageField);
				messageFieldMap.remove(messageNode);
			}
			else if(!currentlyDisplayed && displayable) {
				// Add to display
				insertDisplayableMessage(messageNode);
			}
			else if(currentlyDisplayed) {
				// Just a visual flag update, so find and invalidate the item
				MailboxMessageField mailboxMessageField = (MailboxMessageField)messageFieldMap.get(messageNode);
				mailboxMessageField.invalidate();
			}
		}
	}

    /**
     * Gets the selected message.
     * 
     * @return the selected message
     */
    private MessageNode getSelectedMessage() {
    	Field fieldWithFocus = messageFieldManager.getFieldWithFocus();
    	if(fieldWithFocus instanceof MailboxMessageField) {
    		return ((MailboxMessageField)fieldWithFocus).getMessageNode();
    	}
    	else {
    		return null;
    	}
    }
    
    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Screen#navigationClick(int, int)
     */
    public boolean navigationClick(int status, int time) {
    	MessageNode messageNode = getSelectedMessage();
    	if(messageNode != null) {
    		messageActions.openMessage(messageNode);
    		return true;
    	}
    	else {
    		return false;
    	}
    }
    
    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Screen#keyChar(char, int, int)
     */
    public boolean keyChar(char key, int status, int time) {
        boolean retval = false;
        MessageNode messageNode;
        switch(key) {
            case Keypad.KEY_ENTER:
            	messageNode = getSelectedMessage();
            	if(messageNode != null) {
            		messageActions.openMessage(messageNode);
            		retval = true;
            	}
                break;
            case Keypad.KEY_BACKSPACE:
            	messageNode = getSelectedMessage();
            	if(messageNode != null) {
            		messageActions.deleteMessage(messageNode);
            		retval = true;
            	}
            	break;
        }
        return retval;
    }
    
    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.ui.AbstractScreenProvider#shortcutAction(org.logicprobe.LogicMail.ui.ScreenProvider.ShortcutItem)
     */
    public void shortcutAction(ShortcutItem item) {
    	switch(item.getId()) {
    	case SHORTCUT_COMPOSE:
    		compositionItem.run();
    		break;
    	case SHORTCUT_UP:
    		screen.scroll(Manager.UPWARD);
    		break;
    	case SHORTCUT_DOWN:
    		screen.scroll(Manager.DOWNWARD);
    		break;
    	}
    }
}
