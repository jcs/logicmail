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
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import net.rim.device.api.i18n.DateFormat;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.system.KeypadListener;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.FocusChangeListener;
import net.rim.device.api.ui.Keypad;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.Screen;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.container.VerticalFieldManager;
import net.rim.device.api.util.Comparator;
import net.rim.device.api.util.DateTimeUtilities;

import org.logicprobe.LogicMail.AnalyticsDataCollector;
import org.logicprobe.LogicMail.LogicMailResource;
import org.logicprobe.LogicMail.PlatformInfo;
import org.logicprobe.LogicMail.conf.GlobalConfig;
import org.logicprobe.LogicMail.conf.MailSettings;
import org.logicprobe.LogicMail.conf.MailSettingsEvent;
import org.logicprobe.LogicMail.conf.MailSettingsListener;
import org.logicprobe.LogicMail.model.MailboxNode;
import org.logicprobe.LogicMail.model.MailboxNodeEvent;
import org.logicprobe.LogicMail.model.MailboxNodeListener;
import org.logicprobe.LogicMail.model.MessageNode;
import org.logicprobe.LogicMail.model.MessageNodeEvent;
import org.logicprobe.LogicMail.model.MessageNodeListener;
import org.logicprobe.LogicMail.model.NetworkAccountNode;
import org.logicprobe.LogicMail.util.EventObjectRunnable;

/**
 * Display the active mailbox listing.
 * If the supplied folder matches the configured sent folder
 * for the provided account, then the display fields will be
 * adjusted accordingly.
 */
public class MailboxScreen extends AbstractScreenProvider {
    private static final boolean hasTouchscreen = PlatformInfo.getInstance().hasTouchscreen();
	private static final int SHORTCUT_COMPOSE = 0;
    private static final int SHORTCUT_OPEN    = 1;
    private static final int SHORTCUT_DELETE  = 2;
	private static final int SHORTCUT_UP      = 3;
	private static final int SHORTCUT_DOWN    = 4;
	
	private MailboxNode mailboxNode;
    private Vector knownMessages;
    private Hashtable messageNodeToFieldMap;
    private boolean firstDisplay = true;
    private MailSettings mailSettings;
    private GlobalConfig globalConfig;
    private VerticalFieldManager messageFieldManager;
    private boolean navigationMoved;
    private boolean displayOrder;
    private boolean hideDeleted;
    private MessageActions messageActions;
    protected boolean composeEnabled;
    protected Field currentContextField;

    protected MenuItem compositionItem;
    protected MenuItem markPriorOpenedItem;
	
    /**
     * Initializes a new MailboxScreen to view the provided mailbox.
     * 
     * @param mailboxNode Mailbox node to view.
     */
    public MailboxScreen(MailboxNode mailboxNode) {
    	this.mailboxNode = mailboxNode;
    	this.knownMessages = new Vector();
    	this.messageNodeToFieldMap = new Hashtable();
    	this.mailSettings = MailSettings.getInstance();
    	this.globalConfig = this.mailSettings.getGlobalConfig();
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
    
    private MailSettingsListener mailSettingsListener = new MailSettingsListener() {
        public void mailSettingsSaved(MailSettingsEvent e) {
            if((e.getGlobalChange() & GlobalConfig.CHANGE_TYPE_OTHER) != 0) {
                if(globalConfig.getDispOrder() != displayOrder) {
                    final MessageNode[][] gaps = mailboxNode.findMessageNodeGaps();
                    invokeLater(new EventObjectRunnable(e) {
                        public void run() {
                            displayOrder = !displayOrder;
                            displayableChanged();
                            if(gaps == null || gaps.length == 0) { return; }
                            handleMessageNodeGaps(gaps);
                        }
                    });
                }
                if(globalConfig.getHideDeletedMsg() != hideDeleted) {
                    final MessageNode[][] gaps = mailboxNode.findMessageNodeGaps();
                    invokeLater(new EventObjectRunnable(e) {
                        public void run() {
                            hideDeleted = !hideDeleted;
                            displayableChanged();
                            if(gaps == null || gaps.length == 0) { return; }
                            handleMessageNodeGaps(gaps);
                        }
                    });
                }
            }
        }
    };
    
    /** The mailbox node listener. */
    private MailboxNodeListener mailboxNodeListener = new MailboxNodeListener() {
        public void mailboxStatusChanged(MailboxNodeEvent e) {
            mailboxNode_MailboxStatusChanged(e);
        }
    };
    
    /** The message node listener. */
    private MessageNodeListener messageNodeListener = new MessageNodeListener() {
		public void messageStatusChanged(MessageNodeEvent e) {
			invokeLater(new EventObjectRunnable(e) {
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
        super.onDisplay();
        if(this.hideDeleted != globalConfig.getHideDeletedMsg()) {
            this.hideDeleted = !this.hideDeleted;
            displayableChanged();
        }
        
        if(this.displayOrder != globalConfig.getDispOrder()) {
            this.displayOrder = !this.displayOrder;
            displayableChanged();
        }
        this.mailSettings.addMailSettingsListener(mailSettingsListener);
        this.mailboxNode.addMailboxNodeListener(mailboxNodeListener);
        
        if(firstDisplay) {
            MessageNode[] initialMessages = this.mailboxNode.getMessages();
            for(int i=0; i<initialMessages.length; i++) {
            	knownMessages.addElement(initialMessages[i]);
            	if(isMessageDisplayable(initialMessages[i])) {
            		insertDisplayableMessage(initialMessages[i]);
            	}
            }
            
        	this.mailboxNode.refreshMessages(true);
        	firstDisplay = false;
        }
        int size = knownMessages.size();
        for(int i=0; i<size; i++) {
        	((MessageNode)knownMessages.elementAt(i)).addMessageNodeListener(messageNodeListener);
        }
        
        composeEnabled = (mailboxNode.getParentAccount() instanceof NetworkAccountNode)
            && ((NetworkAccountNode)mailboxNode.getParentAccount()).hasMailSender();
        
        if(hasTouchscreen) {
            ((StandardScreen)screen).setShortcutEnabled(SHORTCUT_COMPOSE, composeEnabled);
            updateShortcuts();
        }
    }
    
	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.ui.BaseScreen#onUndisplay()
	 */
	public void onUndisplay() {
        this.mailSettings.removeMailSettingsListener(mailSettingsListener);
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
		// and only on devices that have touchscreen support.
		return new ShortcutItem[] {
			new ShortcutItem(
					SHORTCUT_COMPOSE,
					resources.getString(LogicMailResource.MENUITEM_COMPOSE_EMAIL),
					"shortcut-compose.png", "shortcut-compose-d.png"),
			new ShortcutItem(
			        SHORTCUT_OPEN,
			        resources.getString(LogicMailResource.MENUITEM_SELECT),
			        "shortcut-message-open.png", "shortcut-message-open-d.png"),
            new ShortcutItem(
                    SHORTCUT_DELETE,
                    resources.getString(LogicMailResource.MENUITEM_DELETE),
                    "shortcut-message-delete.png", "shortcut-message-delete-d.png"),
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
	 * Listener to be added to all fields in the message field manager, so that
	 * focus changes can be detected to update the shortcut bar.  This is
	 * unfortunately the only reliable way to handle this situation.
	 */
    private FocusChangeListener messageFieldFocusChangeListener = new FocusChangeListener() {
        public void focusChanged(Field field, int eventType) {
            if(hasTouchscreen) {
                updateShortcuts();
            }
        }
    };
	
	/**
	 * Initializes the fields.
	 */
	public void initFields(Screen screen) {
        super.initFields(screen);

		messageFieldManager = new VerticalFieldManager(Manager.VERTICAL_SCROLL | Manager.VERTICAL_SCROLLBAR) {
		    protected boolean navigationMovement(int dx, int dy, int status, int time) {
		        navigationMoved = true;
		        return super.navigationMovement(dx, dy, status, time);
		    }
		    public void add(Field field) {
		        field.setFocusListener(messageFieldFocusChangeListener);
		        super.add(field);
		    }
		    public void insert(Field field, int index) {
                field.setFocusListener(messageFieldFocusChangeListener);
		        super.insert(field, index);
		    }
		    public void delete(Field field) {
		        field.setFocusListener(null);
		        super.delete(field);
		        updateShortcuts();
		    }
		    public void deleteRange(int start, int count) {
                for(int i=start; i < start + count; i++) {
                    getField(start).setFocusListener(null);
                }
		        super.deleteRange(start, count);
		        updateShortcuts();
		    }
		};
		
        screen.add(messageFieldManager);
    	this.messageActions = navigationController.getMessageActions();
        initMenuItems();
	}    
	
	private void updateShortcuts() {
	    int fieldCount = messageFieldManager.getFieldCount();
	    StandardScreen standardScreen = (StandardScreen)screen;
	    
	    if(fieldCount <= 1) {
	        standardScreen.setShortcutEnabled(SHORTCUT_UP, false);
	        standardScreen.setShortcutEnabled(SHORTCUT_DOWN, false);
	        standardScreen.setShortcutEnabled(SHORTCUT_OPEN, false);
	        standardScreen.setShortcutEnabled(SHORTCUT_DELETE, false);
            return;
	    }

	    boolean openEnabled = false;
	    boolean deleteEnabled = false;
        Field fieldWithFocus = messageFieldManager.getFieldWithFocus();
        if(fieldWithFocus instanceof MailboxMessageField) {
            MessageNode messageNode = ((MailboxMessageField)fieldWithFocus).getMessageNode();
            openEnabled = messageNode.existsOnServer() || messageNode.hasCachedContent();
            deleteEnabled = (messageNode.getFlags() & MessageNode.Flag.DELETED) == 0;
        }
        int focusIndex = messageFieldManager.getFieldWithFocusIndex();
        if(focusIndex == 0) {
            standardScreen.setShortcutEnabled(SHORTCUT_UP, false);
            standardScreen.setShortcutEnabled(SHORTCUT_DOWN, true);
        }
        else if(focusIndex == fieldCount - 1) {
            standardScreen.setShortcutEnabled(SHORTCUT_UP, true);
            standardScreen.setShortcutEnabled(SHORTCUT_DOWN, false);
        }
        else {
            standardScreen.setShortcutEnabled(SHORTCUT_UP, true);
            standardScreen.setShortcutEnabled(SHORTCUT_DOWN, true);
        }
        
        standardScreen.setShortcutEnabled(SHORTCUT_OPEN, openEnabled);
        standardScreen.setShortcutEnabled(SHORTCUT_DELETE, deleteEnabled);
    }

    protected void initMenuItems() {
	    compositionItem = new MenuItem(resources, LogicMailResource.MENUITEM_COMPOSE_EMAIL, 400100, 2000) {
	        public void run() {
	            AnalyticsDataCollector.getInstance().onButtonClick(getScreenPath(), getScreenName(), "composition");
	        	navigationController.displayComposition((NetworkAccountNode)mailboxNode.getParentAccount());
	        }
	    };
	    markPriorOpenedItem = new MenuItem(resources, LogicMailResource.MENUITEM_MARK_PRIOR_OPENED, 400110, 2000) {
            public void run() {
                if(!(currentContextField instanceof MessageSeparatorField)) { return; }
                
                int choice = Dialog.ask(
                        resources.getString(LogicMailResource.MAILBOX_MARK_ALL_PRIOR_ITEMS_OPENED_PROMPT),
                        new Object[] {
                                resources.getString(LogicMailResource.MENUITEM_MARK_OPENED),
                                resources.getString(LogicMailResource.MENUITEM_CANCEL)
                        },
                        new int[] { Dialog.OK, Dialog.CANCEL },
                        Dialog.CANCEL);
                
                if(choice == Dialog.OK) {
                    AnalyticsDataCollector.getInstance().onButtonClick(getScreenPath(), getScreenName(), "markPriorOpened");
                    Date separatorDate = ((MessageSeparatorField)currentContextField).getDate();
                    
                    // The separator date is midnight on the day it shows,
                    // so we pass a date that is exactly one day past it to
                    // ensure the desired effect of this request.
                    mailboxNode.markPriorMessagesOpened(new Date(separatorDate.getTime() + 86400000));
                }
            }
        };
	}

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.ui.BaseScreen#makeMenu(net.rim.device.api.ui.component.Menu, int)
     */
    public void makeMenu(Menu menu, int instance) {
    	currentContextField = messageFieldManager.getFieldWithFocus();
    	if(currentContextField instanceof MailboxMessageField) {
    	    makeMessageMenu((MailboxMessageField)currentContextField, menu, instance);
    	}
    	else if(currentContextField instanceof MessageSeparatorField) {
    	    makeSeparatorMenu((MessageSeparatorField)currentContextField, menu, instance);
    	}
    }

    protected void makeMessageMenu(MailboxMessageField messageField, Menu menu, int instance) {
        MessageNode messageNode = messageField.getMessageNode();
        if(instance == Menu.INSTANCE_DEFAULT) {
            messageActions.makeMenu(menu, instance, messageNode, false);
            if(composeEnabled) {
                menu.add(compositionItem);
            }
        }
        else {
            messageActions.makeContextMenu(menu, instance, messageNode, false);
        }
    }
    
    protected void makeSeparatorMenu(MessageSeparatorField separatorField, Menu menu, int instance) {
        if(composeEnabled) {
            menu.add(compositionItem);
        }
        menu.add(markPriorOpenedItem);
    }
    
    public boolean onClose() {
        // Check for deleted messages in the mailbox
        if(mailboxNode.getParentAccount().hasExpunge()
                && mailboxNode.hasDeletedMessages()) {
            MailSettings mailSettings = MailSettings.getInstance();
            int expungeMode = globalConfig.getExpungeMode();
            if(expungeMode == GlobalConfig.EXPUNGE_PROMPT) {
                Dialog dialog = new Dialog(
                        Dialog.D_YES_NO,
                        resources.getString(LogicMailResource.MAILBOX_EXPUNGE_PROMPT),
                        Dialog.NO,
                        Bitmap.getPredefinedBitmap(Bitmap.QUESTION), 0, true);
                int choice = dialog.doModal();

                // Request expunge if desired
                if(choice == Dialog.YES) {
                    mailboxNode.expungeDeletedMessages();
                }

                if(dialog.getDontAskAgainValue()) {
                    if(choice == Dialog.YES) {
                        globalConfig.setExpungeMode(GlobalConfig.EXPUNGE_ALWAYS);
                        mailSettings.saveSettings();
                    }
                    else if(choice == Dialog.NO) {
                        globalConfig.setExpungeMode(GlobalConfig.EXPUNGE_NEVER);
                        mailSettings.saveSettings();
                    }
                }
            }
            else if(expungeMode == GlobalConfig.EXPUNGE_ALWAYS) {
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
    private void mailboxNode_MailboxStatusChanged(final MailboxNodeEvent e) {
        int type = e.getType();
        if(type == MailboxNodeEvent.TYPE_NEW_MESSAGES) {
            invokeLater(new Runnable() {
                public void run() {
                    handleMailboxNewMessages(e.getAffectedMessages());
                }
            });
        }
        else if(type == MailboxNodeEvent.TYPE_DELETED_MESSAGES) {
            invokeLater(new Runnable() {
                public void run() {
                    handleMailboxDeletedMessages(e.getAffectedMessages());
                }
            });
        }
        else if(type == MailboxNodeEvent.TYPE_FETCH_COMPLETE) {
            // Collect the gaps before scheduling the operation for the UI
            // thread.  This is necessary because the gap-finding operation
            // could be time consuming.
            final MessageNode[][] gaps = mailboxNode.findMessageNodeGaps();
            if(gaps == null || gaps.length == 0) { return; }
            
            invokeLater(new Runnable() {
                public void run() {
                    handleMessageNodeGaps(gaps);
                }
            });
        }
    }

    private void handleMailboxNewMessages(MessageNode[] messageNodes) {
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

    private void handleMailboxDeletedMessages(MessageNode[] messageNodes) {
        for(int i=0; i<messageNodes.length; i++) {
            if(screen != null && screen.isDisplayed()) {
                messageNodes[i].removeMessageNodeListener(messageNodeListener);
            }
            removeDisplayableMessage(messageNodes[i]);
            knownMessages.removeElement(messageNodes[i]);
        }
    }

    private void handleMessageNodeGaps(MessageNode[][] gaps) {
        // Build a set of all gap fields currently on the screen
        Hashtable orphanedGapFieldSet = new Hashtable();
        int size = messageFieldManager.getFieldCount();
        for(int i=0; i<size; i++) {
            Field fieldAtIndex = messageFieldManager.getField(i);
            if(fieldAtIndex instanceof MailboxActionField) {
                orphanedGapFieldSet.put(fieldAtIndex, Boolean.TRUE);
            }
        }

        for(int i = 0; i < gaps.length; i++) {
            MailboxActionField gapField = null;
            int insertIndex;
            if(displayOrder) {
                // Ascending order

                // Find the field for message after the gap
                MailboxMessageField messageFieldAfter = (MailboxMessageField)messageNodeToFieldMap.get(gaps[i][1]);
                if(messageFieldAfter == null) { continue; }
                insertIndex = messageFieldAfter.getIndex();

                // See if there is an existing field that can be taken over
                if(insertIndex - 1 > 0) {
                    Field fieldAtIndex = messageFieldManager.getField(insertIndex - 1);
                    if(fieldAtIndex instanceof MailboxActionField) {
                        // Take over an existing field and remove it from the
                        // orphaned field set
                        gapField = (MailboxActionField)fieldAtIndex;
                        orphanedGapFieldSet.remove(gapField);
                    }
                }
            }
            else {
                // Descending order

                // Find the field for message before the gap
                MailboxMessageField messageFieldBefore = (MailboxMessageField)messageNodeToFieldMap.get(gaps[i][1]);
                if(messageFieldBefore == null) { continue; }
                insertIndex = messageFieldBefore.getIndex() + 1;

                // See if there is an existing field that can be taken over
                if(insertIndex < messageFieldManager.getFieldCount()) {
                    Field fieldAtIndex = messageFieldManager.getField(insertIndex);
                    if(fieldAtIndex instanceof MailboxActionField) {
                        // Take over an existing field and remove it from the
                        // orphaned field set
                        gapField = (MailboxActionField)fieldAtIndex;
                        orphanedGapFieldSet.remove(gapField);
                    }
                }
            }

            // Create a new field, if necessary, and set field properties
            if(gapField == null) {
                gapField = new MailboxActionField(
                        resources.getString(LogicMailResource.MAILBOX_LOAD_MORE_MESSAGES),
                        Field.USE_ALL_WIDTH | Field.FOCUSABLE);
                messageFieldManager.insert(gapField, insertIndex);
            }
            gapField.setEditable(true);
            gapField.setTagObject(gaps[i]);
        }

        // Remove orphaned gap fields
        Enumeration e = orphanedGapFieldSet.keys();
        while(e.hasMoreElements()) {
            messageFieldManager.delete((MailboxActionField)e.nextElement());
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
        if((messageNode.getFlags() & MessageNode.Flag.DELETED) != 0
                && hideDeleted) {
            return false;
        }
        else {
            return true;
        }
    }
    
    /**
     * A configuration item affecting the displayable state of messages has
     * changed.  Since this is infrequent, this operation will take the simple
     * approach of emptying and repopulating the screen.
     */
    private void displayableChanged() {
        int size = knownMessages.size();
        if(size == 0) { return; }
        
        // Clear out all the existing content from the field manager and map
        messageNodeToFieldMap.clear();
        messageFieldManager.deleteAll();
        
        // Reset the flag that controls field focus behavior
        navigationMoved = false;
        
        // Get the known messages display them if appropriate
        for(int i=0; i<size; i++) {
            MessageNode messageNode = (MessageNode)knownMessages.elementAt(i);
            if(isMessageDisplayable(messageNode)) {
                insertDisplayableMessage(messageNode);
            }
        }
    }
    
    /**
     * Insert a message into the list and associated data structures.
     * This will insert into the correct order, per the configuration.
     * 
     * @param messageNode Message to insert.
     */
    private void insertDisplayableMessage(MessageNode messageNode) {
    	Field selectedField = messageFieldManager.getFieldWithFocus();
    	
    	int fieldCount = messageFieldManager.getFieldCount();
		if(fieldCount > 0) {
			Comparator comparator = MessageNode.getComparator();
			int index = messageFieldManager.getFieldCount();
			
			if(displayOrder) {
				// Ascending order
				MessageNode lastMessage = getLastDisplayedMessage(index - 1);
				while(lastMessage != null && index > 0 && comparator.compare(lastMessage, messageNode) >= 0) {
					index--;
					if(index > 0) { lastMessage = getLastDisplayedMessage(index - 1); }
				}
                
                // Deal with the case of trying to insert right before an action field
                if(index < fieldCount - 1 && messageFieldManager.getField(index + 1) instanceof MailboxActionField) {
                    index++;
                }
			}
			else {
				// Descending order
				MessageNode lastMessage = getLastDisplayedMessage(index - 1);
				while(lastMessage != null && index > 0 && comparator.compare(lastMessage, messageNode) <= 0) {
					index--;
					if(index > 0) { lastMessage = getLastDisplayedMessage(index - 1); }
				}
				
				// Deal with the case of trying to insert right after an action field
				if(index > 0 && messageFieldManager.getField(index - 1) instanceof MailboxActionField) {
				    index--;
				}
			}
			MailboxMessageField mailboxMessageField =
				new MailboxMessageField(mailboxNode, messageNode, Field.USE_ALL_WIDTH | Field.FOCUSABLE);
			messageNodeToFieldMap.put(messageNode, mailboxMessageField);
            insertMessageField(mailboxMessageField, index);
		}
		else {
			MailboxMessageField mailboxMessageField =
				new MailboxMessageField(mailboxNode, messageNode, Field.USE_ALL_WIDTH | Field.FOCUSABLE);
			messageNodeToFieldMap.put(messageNode, mailboxMessageField);
			insertMessageField(mailboxMessageField, 0);
		}
		
		if(!navigationMoved) {
		    // Select newest message
            int count = messageFieldManager.getFieldCount();
            if(count > 0) {
                if(displayOrder) {
                    messageFieldManager.getField(count - 1).setFocus();
                }
                else {
                    messageFieldManager.getField(0).setFocus();
                }
            }
		}
		else {
		    // Keep previous selection
            if(selectedField != null) { selectedField.setFocus(); }
		}
    }

    /**
     * Remove a message from the list and associated data structures,
     * if that message exists in the list.
     * 
     * @param messageNode Message to insert.
     */
    private void removeDisplayableMessage(MessageNode messageNode) {
        MailboxMessageField mailboxMessageField = (MailboxMessageField)messageNodeToFieldMap.remove(messageNode);
        if(mailboxMessageField == null) { return; }

        deleteMessageField(mailboxMessageField);
    }
    
    private void insertMessageField(MailboxMessageField messageField, int index) {
        // It is assumed that the index will only be 0 for the first message.
        
        Date messageDate = messageField.getMessageNode().getDate();
        
        if(displayOrder) {
            // Ascending order
            if(messageFieldManager.getFieldCount() == 0) {
                messageFieldManager.add(messageField);
                messageFieldManager.add(new MessageSeparatorField(messageDate));
            }
            else {
                if(index > 0 && messageFieldManager.getField(index - 1) instanceof MessageSeparatorField) {
                    Date sepDate = ((MessageSeparatorField)messageFieldManager.getField(index - 1)).getDate();
                    if(DateTimeUtilities.isSameDate(messageDate.getTime(), sepDate.getTime())) {
                        index--;
                    }
                }
                
                Field nextField;
                if(index < messageFieldManager.getFieldCount()) {
                    nextField = messageFieldManager.getField(index);
                }
                else {
                    nextField = null;
                }
                
                if(nextField instanceof MessageSeparatorField) {
                    Date sepDate = ((MessageSeparatorField)nextField).getDate();
                    if(DateTimeUtilities.isSameDate(messageDate.getTime(), sepDate.getTime())) {
                        messageFieldManager.insert(messageField, index);
                    }
                    else {
                        messageFieldManager.insert(new MessageSeparatorField(messageDate), index + 1);
                        messageFieldManager.insert(messageField, index + 1);
                    }
                }
                else if(nextField instanceof MailboxMessageField) {
                    Date nextDate = ((MailboxMessageField)nextField).getMessageNode().getDate();
                    if(DateTimeUtilities.isSameDate(messageDate.getTime(), nextDate.getTime())) {
                        messageFieldManager.insert(messageField, index);
                    }
                    else {
                        messageFieldManager.insert(new MessageSeparatorField(messageDate), index);
                        messageFieldManager.insert(messageField, index);
                    }
                }
                else {
                    messageFieldManager.insert(new MessageSeparatorField(messageDate), index);
                    messageFieldManager.insert(messageField, index);
                }
            }
        }
        else {
            // Descending order
            if(messageFieldManager.getFieldCount() == 0) {
                messageFieldManager.add(new MessageSeparatorField(messageDate));
                messageFieldManager.add(messageField);
            }
            else  {
                Field prevField = messageFieldManager.getField(index - 1);

                if(prevField instanceof MessageSeparatorField) {
                    Date sepDate = ((MessageSeparatorField)prevField).getDate();
                    if(!DateTimeUtilities.isSameDate(messageDate.getTime(), sepDate.getTime())
                            && index > 1
                            && messageFieldManager.getField(index - 2) instanceof MailboxMessageField) {
                        index--;
                        prevField = messageFieldManager.getField(index - 1);
                    }
                }
                
                if(prevField instanceof MessageSeparatorField) {
                    Date sepDate = ((MessageSeparatorField)prevField).getDate();
                    if(DateTimeUtilities.isSameDate(messageDate.getTime(), sepDate.getTime())) {
                        messageFieldManager.insert(messageField, index);
                    }
                    else {
                        messageFieldManager.insert(new MessageSeparatorField(messageDate), index - 1);
                        messageFieldManager.insert(messageField, index);
                    }
                }
                else {
                    Date prevDate = ((MailboxMessageField)prevField).getMessageNode().getDate();
                    if(DateTimeUtilities.isSameDate(messageDate.getTime(), prevDate.getTime())) {
                        messageFieldManager.insert(messageField, index);
                    }
                    else {
                        messageFieldManager.insert(new MessageSeparatorField(messageDate), index);
                        messageFieldManager.insert(messageField, index + 1);
                    }
                }
            }
        }
    }
    
    private void deleteMessageField(MailboxMessageField messageField) {
        int fieldCount = messageFieldManager.getFieldCount();
        int index = messageField.getIndex();
        
        if(displayOrder) {
            // Ascending order
            if(messageFieldManager.getField(index + 1) instanceof MessageSeparatorField
                    && (index == 0 || messageFieldManager.getField(index - 1) instanceof MessageSeparatorField)) {
                messageFieldManager.deleteRange(index, 2);
            }
            else {
                messageFieldManager.delete(messageField);
            }
        }
        else {
            // Descending order
            if(messageFieldManager.getField(index - 1) instanceof MessageSeparatorField
                    && (index == fieldCount - 1 || messageFieldManager.getField(index + 1) instanceof MessageSeparatorField)) {
                messageFieldManager.deleteRange(index - 1, 2);
            }
            else {
                messageFieldManager.delete(messageField);
            }
        }
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
			boolean currentlyDisplayed = messageNodeToFieldMap.containsKey(messageNode);
			boolean displayable = isMessageDisplayable(messageNode);
			
			if(currentlyDisplayed && !displayable) {
				// Remove from display
			    removeDisplayableMessage(messageNode);
			}
			else if(!currentlyDisplayed && displayable) {
				// Add to display
				insertDisplayableMessage(messageNode);
			}
			else if(currentlyDisplayed) {
				// Just a visual flag update, so find and invalidate the item
				MailboxMessageField mailboxMessageField = (MailboxMessageField)messageNodeToFieldMap.get(messageNode);
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
    
    /**
     * Gets the selected message node gap, if available.
     *
     * @return the selected message gap range
     */
    private MailboxActionField getSelectedMessageGapField() {
        Field fieldWithFocus = messageFieldManager.getFieldWithFocus();
        if(fieldWithFocus instanceof MailboxActionField) {
            MailboxActionField field = (MailboxActionField)fieldWithFocus;
            if(field.getTagObject() instanceof MessageNode[]) {
                return field;
            }
        }
        return null;
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
    	MailboxActionField gapField = getSelectedMessageGapField();
    	if(gapField != null) {
    	    handleMessageGapAction(gapField);
    	    return true;
    	}
    	
    	return false;
    }

    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Screen#keyChar(char, int, int)
     */
    public boolean keyChar(char key, int status, int time) {
        MessageNode messageNode;
        
        // First, check and see if any hard-coded shortcuts are applicable
        switch(key) {
        case Keypad.KEY_ENTER:
            messageNode = getSelectedMessage();
            if(messageNode != null) {
                messageActions.openMessage(messageNode);
                return true;
            }
            MailboxActionField gapField = getSelectedMessageGapField();
            if(gapField != null) {
                handleMessageGapAction(gapField);
                return true;
            }
            break;
        case Keypad.KEY_BACKSPACE:
            messageNode = getSelectedMessage();
            if(messageNode != null) {
                messageActions.deleteMessage(messageNode);
                return true;
            }
            break;
        case Keypad.KEY_SPACE:
            if(status == 0) {
                screen.scroll(Manager.DOWNWARD);
                return true;
            }
            else if(status == KeypadListener.STATUS_ALT) {
                screen.scroll(Manager.UPWARD);
                return true;
            }
            break;
        }
        
        // Now check the keypad/locale-specific shortcuts
        int shortcut = KeyHandler.keyCharShortcut(key, status);
        switch(shortcut) {
        case KeyHandler.SCROLL_TOP:
            screen.scroll(Manager.TOPMOST);
            return true;
        case KeyHandler.SCROLL_BOTTOM:
            screen.scroll(Manager.BOTTOMMOST);
            return true;
        case KeyHandler.SCROLL_NEXT_DATE:
            scrollNextDate(displayOrder);
            return true;
        case KeyHandler.SCROLL_PREV_DATE:
            scrollNextDate(!displayOrder);
            return true;
        case KeyHandler.SCROLL_NEXT_UNOPENED:
            scrollNextUnopened();
            return true;
        case KeyHandler.MESSAGE_COMPOSE:
            if(composeEnabled) {
                compositionItem.run();
                return true;
            }
        default:
            messageNode = getSelectedMessage();
            if(messageNode != null) {
                if(messageActions.keyCharShortcut(messageNode, shortcut)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private void scrollNextDate(boolean direction) {
        int index = messageFieldManager.getFieldWithFocusIndex();
        int count = messageFieldManager.getFieldCount();
        
        if(direction) {
            // Upward
            index--;
            while(index >= 0) {
                if(messageFieldManager.getField(index) instanceof MessageSeparatorField) {
                    messageFieldManager.setFocus();
                    messageFieldManager.getField(index).setFocus();
                    break;
                }
                index--;
            }
        }
        else {
            // Downward
            index++;
            while(index < count) {
                if(messageFieldManager.getField(index) instanceof MessageSeparatorField) {
                    messageFieldManager.setFocus();
                    messageFieldManager.getField(index).setFocus();
                    break;
                }
                index++;
            }
        }
    }

    private void scrollNextUnopened() {
        int index = messageFieldManager.getFieldWithFocusIndex();
        int count = messageFieldManager.getFieldCount();
        
        if(displayOrder) {
            // Ascending
            index--;
            while(index >= 0) {
                if(messageFieldManager.getField(index) instanceof MailboxMessageField) {
                    MailboxMessageField field = (MailboxMessageField)messageFieldManager.getField(index);
                    if((field.getMessageNode().getFlags() & MessageNode.Flag.SEEN) == 0) {
                        messageFieldManager.setFocus();
                        field.setFocus();
                        break;
                    }
                }
                index--;
            }
        }
        else {
            // Descending
            index++;
            while(index < count) {
                if(messageFieldManager.getField(index) instanceof MailboxMessageField) {
                    MailboxMessageField field = (MailboxMessageField)messageFieldManager.getField(index);
                    if((field.getMessageNode().getFlags() & MessageNode.Flag.SEEN) == 0) {
                        messageFieldManager.setFocus();
                        field.setFocus();
                        break;
                    }
                }
                index++;
            }
        }
    }

    private void handleMessageGapAction(final MailboxActionField gapField) {
        AnalyticsDataCollector.getInstance().onButtonClick(getScreenPath(), getScreenName(), "requestMoreMessages");
        
        // This method inserts a short delay, where the gap action field is
        // disabled, prior to starting the request.  This is done to provide
        // feedback that the user has triggered an action before the field
        // disappears from the screen.
        
        final MessageNode[] gap = (MessageNode[])gapField.getTagObject();
        gapField.setEditable(false);
        (new Thread() { public void run() {
            try { Thread.sleep(200); } catch (InterruptedException e) { }
            invokeLater(new Runnable() { public void run() {
                messageFieldManager.delete(gapField);
                mailboxNode.requestMoreMessages(gap[1]);
            }});
        }}).start();
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.ui.AbstractScreenProvider#shortcutAction(org.logicprobe.LogicMail.ui.ScreenProvider.ShortcutItem)
     */
    public void shortcutAction(ShortcutItem item) {
        MessageNode messageNode;
        
    	switch(item.getId()) {
    	case SHORTCUT_COMPOSE:
    		compositionItem.run();
    		break;
    	case SHORTCUT_OPEN:
            messageNode = getSelectedMessage();
            if(messageNode != null) {
                messageActions.openMessage(messageNode);
            }
    	    break;
    	case SHORTCUT_DELETE:
            messageNode = getSelectedMessage();
            if(messageNode != null) {
                messageActions.deleteMessage(messageNode);
            }
    	    break;
    	case SHORTCUT_UP:
    		screen.scroll(Manager.UPWARD);
    		break;
    	case SHORTCUT_DOWN:
    		screen.scroll(Manager.DOWNWARD);
    		break;
    	}
    }
    
    protected static class MessageSeparatorField extends LabeledSeparatorField {
        private final Date date;
        
        public MessageSeparatorField(Date date) {
            super(Field.FOCUSABLE | LabeledSeparatorField.BOTTOM_BORDER);
   
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            
            StringBuffer buffer = new StringBuffer();
            DateFormat.getInstance(DateFormat.DATE_LONG).format(cal, buffer, null);
            setText(buffer.toString());
            
            this.date = cal.getTime();
        }
        
        public Date getDate() {
            return date;
        }
    }
}
