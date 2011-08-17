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

import java.util.Vector;

import net.rim.device.api.i18n.ResourceBundle;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.Screen;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.Menu;

import org.logicprobe.LogicMail.AnalyticsDataCollector;
import org.logicprobe.LogicMail.LogicMailResource;
import org.logicprobe.LogicMail.conf.GlobalConfig;
import org.logicprobe.LogicMail.conf.MailSettings;
import org.logicprobe.LogicMail.conf.OutgoingConfig;
import org.logicprobe.LogicMail.mail.AbstractMailSender;
import org.logicprobe.LogicMail.mail.MailFactory;
import org.logicprobe.LogicMail.model.AccountNode;
import org.logicprobe.LogicMail.model.MailManager;
import org.logicprobe.LogicMail.model.MailboxNode;
import org.logicprobe.LogicMail.model.MessageNode;
import org.logicprobe.LogicMail.model.MessageNodeEvent;
import org.logicprobe.LogicMail.model.MessageNodeListener;
import org.logicprobe.LogicMail.model.NetworkAccountNode;
import org.logicprobe.LogicMail.model.OutgoingMessageNode;

/**
 * Delegate class to handle actions on message nodes.
 */
public class MessageActions {
	protected static ResourceBundle resources = ResourceBundle.getBundle(LogicMailResource.BUNDLE_ID, LogicMailResource.BUNDLE_NAME);
	private NavigationController navigationController;
	
    private MenuItem selectItem;
    private MenuItem propertiesItem;
    private MenuItem replyItem;
    private MenuItem replyAllItem;
    private MenuItem forwardItem;
    private MenuItem copyToItem;
    private MenuItem moveToItem;
    private MenuItem deleteItem;
    private MenuItem undeleteItem;
    private MenuItem sendOutgoingItem;
    private MenuItem markOpenedItem;
    private MenuItem markUnopenedItem;
	
    /** Current message node from the last call to makeMenu() */
    protected MessageNode activeMessageNode;
    
    protected static final int ITEM_SELECT        = 0x0001;
    protected static final int ITEM_PROPERTIES    = 0x0002;
    protected static final int ITEM_REPLY         = 0x0004;
    protected static final int ITEM_REPLY_ALL     = 0x0008;
    protected static final int ITEM_FORWARD       = 0x0010;
    protected static final int ITEM_COPY_TO       = 0x0020;
    protected static final int ITEM_MOVE_TO       = 0x0040;
    protected static final int ITEM_DELETE        = 0x0080;
    protected static final int ITEM_UNDELETE      = 0x0100;
    protected static final int ITEM_SEND_OUTGOING = 0x0200;
    protected static final int ITEM_MARK_OPENED   = 0x0400;
    protected static final int ITEM_MARK_UNOPENED = 0x0800;
    
	/**
	 * Instantiates a new message actions handler delegate.
	 * 
	 * @param navigationController the navigation controller
	 */
	public MessageActions(NavigationController navigationController) {
		this.navigationController = navigationController;
		initMenuItems();
	}
	
	/**
	 * Initialize the common menu items provided by this class.
	 */
    protected void initMenuItems() {
        selectItem = new MenuItem(resources, LogicMailResource.MENUITEM_SELECT, 300100, 1010) {
            public void run() {
                openMessage(activeMessageNode);
            }
        };
        sendOutgoingItem = new MenuItem(resources, LogicMailResource.MENUITEM_SEND, 300110, 1020) {
            public void run() {
                sendMessage(activeMessageNode);
            }
        };
        propertiesItem = new MenuItem(resources, LogicMailResource.MENUITEM_PROPERTIES, 300120, 2000) {
            public void run() {
                openMessageProperties(activeMessageNode);
            }
        };
        markOpenedItem = new MenuItem(resources, LogicMailResource.MENUITEM_MARK_OPENED, 400100, 2000) {
            public void run() {
                markMessageOpened(activeMessageNode);
            }
        };
        markUnopenedItem = new MenuItem(resources, LogicMailResource.MENUITEM_MARK_UNOPENED, 400110, 2000) {
            public void run() {
                markMessageUnopened(activeMessageNode);
            }
        };
        replyItem = new MenuItem(resources, LogicMailResource.MENUITEM_REPLY, 400120, 1030) {
            public void run() {
                replyMessage(activeMessageNode);
            }
        };
        replyAllItem = new MenuItem(resources, LogicMailResource.MENUITEM_REPLYTOALL, 400130, 2000) {
            public void run() {
                replyAllMessage(activeMessageNode);
            }
        };
        forwardItem = new MenuItem(resources, LogicMailResource.MENUITEM_FORWARD, 400140, 2000) {
            public void run() {
                forwardMessage(activeMessageNode);
            }
        };
        copyToItem = new MenuItem(resources, LogicMailResource.MENUITEM_COPY_TO, 400150, 2000) {
            public void run() {
                copyToMailbox(activeMessageNode);
            }
        };
        moveToItem = new MenuItem(resources, LogicMailResource.MENUITEM_MOVE_TO, 400160, 2000) {
            public void run() {
                moveToMailbox(activeMessageNode);
            }
        };
        deleteItem = new MenuItem(resources, LogicMailResource.MENUITEM_DELETE, 400170, 2000) {
            public void run() {
                deleteMessage(activeMessageNode);
            }
        };
        undeleteItem = new MenuItem(resources, LogicMailResource.MENUITEM_UNDELETE, 400180, 2000) {
            public void run() {
                undeleteMessage(activeMessageNode);
            }
        };
    }

    protected static void trackButtonClick(String eventType) {
        Screen screen = UiApplication.getUiApplication().getActiveScreen();
        if(screen instanceof StandardScreen) {
            StandardScreen standardScreen = (StandardScreen)screen;
            AnalyticsDataCollector.getInstance().onButtonClick(standardScreen.getScreenPath(), standardScreen.getScreenName(), eventType);
        }
    }

    /**
     * Inspects the properties of the provided message node, and returns the
     * set of valid menu items.  This allows the complex logic for determining
     * valid menu items to be reused in several different contexts.
     * 
     * @param messageNode The message node for which menu items should be offered.
     * @param isOpen True if the message node is currently open, false if it is
     *     displayed along with other message nodes.
     * @return bit field set of valid menu items
     */
    protected int getValidMenuItems(MessageNode messageNode, boolean isOpen) {
        int items = 0;
        
        if(messageNode == null) { return items; }
        
        // Get all the message properties necessary to determine whether or
        // not to add the various menu items.
        MailboxNode mailboxNode = messageNode.getParent();
        AccountNode accountNode;
        if(mailboxNode != null) {
            accountNode = mailboxNode.getParentAccount();
        }
        else { accountNode = null; }
        boolean unloaded =
            !messageNode.hasCachedContent()
            && !messageNode.hasMessageContent()
            && messageNode.isCachable();

        boolean failedOutgoingMessage = false;
        if(messageNode instanceof OutgoingMessageNode) {
            OutgoingMessageNode outgoingMessage = (OutgoingMessageNode)messageNode;
            if(outgoingMessage.isSendAttempted() && !outgoingMessage.isSending()) {
                failedOutgoingMessage = true;
            }
        }
        
        if(!isOpen && (messageNode.existsOnServer() || messageNode.hasCachedContent())) {
            items |= ITEM_SELECT;
        }
        
        items |= ITEM_PROPERTIES;
        
        if(!unloaded) {
            if(accountNode instanceof NetworkAccountNode) {
                NetworkAccountNode networkAccount = (NetworkAccountNode)accountNode;
                if(networkAccount.hasMailSender()) {
                    items |= ITEM_REPLY;
                    if(networkAccount.hasIdentity()) {
                        items |= ITEM_REPLY_ALL;
                    }
                    items |= ITEM_FORWARD;
                }
            }
        }
        
        // Copy-To is supported if the message has been loaded, or if the
        // underlying mail store supports protocol-level copy.  Since we
        // are only determining whether to offer the menu item here, we just
        // check for protocol-level copy support on the source mailbox.
        // When the actual action is invoked, the destination list will
        // be appropriately filtered to only allow valid destination
        // mailboxes for the current state of the message node.
        if(!unloaded || mailboxNode.hasCopy() || failedOutgoingMessage) {
            items |= ITEM_COPY_TO;
        }
        
        // Move-To is currently only supported if the underlying mail store
        // supports protocol-level copy, and then only between folders on that
        // mail store.  These limitations have been chosen because it is the
        // simplest way to be absolutely sure that a user cannot mess up their
        // data with a message move.
        if(mailboxNode.hasCopy()) {
            items |= ITEM_MOVE_TO;
        }
        
        if((messageNode.getFlags() & MessageNode.Flag.DELETED) != 0) {
            if(mailboxNode != null && mailboxNode.getParentAccount().hasUndelete()) {
                items |= ITEM_UNDELETE;
            }
        }
        else {
            items |= ITEM_DELETE;
        }
        
        if(!(messageNode instanceof OutgoingMessageNode)) {
            if((messageNode.getFlags() & MessageNode.Flag.SEEN) != 0) {
                items |= ITEM_MARK_UNOPENED;
            }
            else {
                items |= ITEM_MARK_OPENED;
            }
        }
        
        if(failedOutgoingMessage) {
            items |= ITEM_SEND_OUTGOING;
        }
        
        return items;
    }
    
    /**
     * Populate the provided menu with items appropriate for the
     * provided message node.
     * <p>
     * This method sets a class field for the active message node,
     * so it is not thread-safe.  It normal usage, it should only
     * be called for one screen at a time, so it should not cause
     * any issues.
     * </p>
     * 
     * @param menu Menu to which items should be added.
     * @param instance The instance of the desired menu. If your screen
     *     supports only one menu, this may be ignored. By default, it is 0.
     * @param messageNode The message node for which menu items should be offered.
     * @param isOpen True if the message node is currently open, false if it is
     *     displayed along with other message nodes.
     */
    public void makeMenu(Menu menu, int instance, MessageNode messageNode, boolean isOpen) {
        int items = getValidMenuItems(messageNode, isOpen);

        if((items & ITEM_SELECT) != 0) { menu.add(selectItem); }
        if((items & ITEM_PROPERTIES) != 0) { menu.add(propertiesItem); }
        if((items & ITEM_REPLY) != 0) { menu.add(replyItem); }
        if((items & ITEM_REPLY_ALL) != 0) { menu.add(replyAllItem); }
        if((items & ITEM_FORWARD) != 0) { menu.add(forwardItem); }
        if((items & ITEM_COPY_TO) != 0) { menu.add(copyToItem); }
        if((items & ITEM_MOVE_TO) != 0) { menu.add(moveToItem); }
        if((items & ITEM_UNDELETE) != 0) { menu.add(undeleteItem); }
        if((items & ITEM_DELETE) != 0) { menu.add(deleteItem); }
        if((items & ITEM_MARK_UNOPENED) != 0) { menu.add(markUnopenedItem); }
        if((items & ITEM_MARK_OPENED) != 0) { menu.add(markOpenedItem); }
        if((items & ITEM_SEND_OUTGOING) != 0) { menu.add(sendOutgoingItem); }

        this.activeMessageNode = messageNode;
    }

    public void makeContextMenu(Menu menu, int instance, MessageNode messageNode, boolean isOpen) {
        makeMenu(menu, instance, messageNode, isOpen);
    }
    
    public boolean keyCharShortcut(MessageNode messageNode, int shortcut) {
        // Get all the message properties necessary to determine whether or
        // not to various shortcuts are valid.
        MailboxNode mailboxNode = messageNode.getParent();
        AccountNode accountNode;
        if(mailboxNode != null) {
            accountNode = mailboxNode.getParentAccount();
        }
        else { accountNode = null; }
        boolean unloaded =
            !messageNode.hasCachedContent()
            && !messageNode.hasMessageContent()
            && messageNode.isCachable();

        switch(shortcut) {
        case KeyHandler.MESSAGE_REPLY:
            if(!unloaded && accountNode instanceof NetworkAccountNode) {
                NetworkAccountNode networkAccount = (NetworkAccountNode)accountNode;
                if(networkAccount.hasMailSender()) {
                    this.activeMessageNode = messageNode;
                    replyItem.run();
                    return true;
                }
            }
        case KeyHandler.MESSAGE_REPLY_ALL:
            if(!unloaded && accountNode instanceof NetworkAccountNode) {
                NetworkAccountNode networkAccount = (NetworkAccountNode)accountNode;
                if(networkAccount.hasMailSender() && networkAccount.hasIdentity()) {
                    this.activeMessageNode = messageNode;
                    replyAllItem.run();
                    return true;
                }
            }
        case KeyHandler.MESSAGE_FORWARD:
            if(!unloaded && accountNode instanceof NetworkAccountNode) {
                NetworkAccountNode networkAccount = (NetworkAccountNode)accountNode;
                if(networkAccount.hasMailSender()) {
                    this.activeMessageNode = messageNode;
                    forwardItem.run();
                    return true;
                }
            }
        case KeyHandler.MESSAGE_MARK_OPENED:
            if(!(messageNode instanceof OutgoingMessageNode)) {
                this.activeMessageNode = messageNode;
                if((messageNode.getFlags() & MessageNode.Flag.SEEN) != 0) {
                    markUnopenedItem.run();
                }
                else {
                    markOpenedItem.run();
                }
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Open the message.
     * <p>
     * Normal messages are displayed for viewing, while drafts are
     * opened for resuming composition.
     * </p>
     * 
     * @param messageNode the message node
     */
    public void openMessage(MessageNode messageNode)
    {
        trackButtonClick("openMessage");

        if(!messageNode.existsOnServer() && !messageNode.hasCachedContent()) {
            return;
        }
        
		MailboxNode mailboxNode = messageNode.getParent();
		if(mailboxNode != null && mailboxNode.getType() == MailboxNode.TYPE_DRAFTS) {
			openDraftMessage(messageNode);
		}
		else {
			navigationController.displayMessage(messageNode);
		}
    }

    /**
     * Handles opening a draft message for resuming composition.
     * 
     * @param messageNode the message node
     */
    private void openDraftMessage(MessageNode messageNode) {
    	// Build a list of all the accounts that have this mailbox
    	// configured as their drafts folder, and have a mail sender
    	Vector matchingAccounts = new Vector();
    	NetworkAccountNode[] accounts = MailManager.getInstance().getMailRootNode().getNetworkAccounts();

    	for(int i=0; i<accounts.length; i++) {
    	    if(accounts[i].hasMailSender()) {
    			if(accounts[i].getDraftMailbox() == messageNode.getParent()) {
    				matchingAccounts.addElement(accounts[i]);
    			}
    	    }
    	}

    	// If no matching accounts were found, then add all
    	// non-local accounts so we have something for the
    	// user to select from.
    	if(matchingAccounts.size() == 0) {
        	for(int i=0; i<accounts.length; i++) {
        		if(accounts[i].hasMailSender()) {
    				matchingAccounts.addElement(accounts[i]);
        		}
        	}
    	}
    	
    	// Select the account node that matches this mailbox, prompting the
    	// user if necessary.
    	NetworkAccountNode account;
    	int size = matchingAccounts.size();
    	if(size > 1) {
    	    NetworkAccountNode[] choices = new NetworkAccountNode[size];
    		matchingAccounts.copyInto(choices);
        	int result = Dialog.ask(
    			resources.getString(LogicMailResource.MAILBOX_DRAFT_MULTIPLE_ACCOUNTS),
    			choices, 0);
        	if(result != -1) {
        		account = choices[result];
        	}
        	else {
        		return;
        	}
    	}
    	else if(size == 1) {
    		account = (NetworkAccountNode)matchingAccounts.elementAt(0);
    	}
    	else {
    	    Dialog.alert(resources.getString(LogicMailResource.MESSAGE_NO_ACCOUNTS_HAVE_SENDERS));
    		return;
    	}

    	// Show the message composition screen
    	navigationController.displayComposition(account, messageNode);
    }

    /**
     * Open message properties.
     * 
     * @param messageNode the message node
     */
    public void openMessageProperties(MessageNode messageNode)
    {
        trackButtonClick("openMessageProperties");
    	MessagePropertiesScreen propertiesScreen =
    		new MessagePropertiesScreen(messageNode);
        UiApplication.getUiApplication().pushModalScreen(propertiesScreen);
    }
    
    /**
     * Delete message.
     * 
     * @param messageNode the message node
     * @return true, if the operation proceeded
     */
    public boolean deleteMessage(MessageNode messageNode) {
        trackButtonClick("deleteMessage");
        MailSettings mailSettings = MailSettings.getInstance();
        if(mailSettings.getGlobalConfig().getPromptOnDelete()) {
            Dialog dialog = new Dialog(
                    Dialog.D_YES_NO,
                    resources.getString(LogicMailResource.MAILBOX_DELETE_PROMPT),
                    Dialog.NO,
                    Bitmap.getPredefinedBitmap(Bitmap.QUESTION), 0, true);
            if(dialog.doModal() == Dialog.YES) {
                messageNode.deleteMessage();
                if(dialog.getDontAskAgainValue()) {
                    mailSettings.getGlobalConfig().setPromptOnDelete(false);
                    mailSettings.saveSettings();
                }
                return true;
            }
            else {
                return false;
            }
        }
        else {
            messageNode.deleteMessage();
            return true;
        }
    }
    
    /**
     * Undelete message.
     * 
     * @param messageNode the message node
     */
    public void undeleteMessage(MessageNode messageNode) {
        trackButtonClick("undeleteMessage");
		messageNode.undeleteMessage();
    }
    
    /**
     * Marks a message as opened.
     * 
     * @param messageNode the message node
     */
    public void markMessageOpened(MessageNode messageNode) {
        trackButtonClick("markMessageOpened");
        messageNode.markMessageOpened();
    }
    
    /**
     * Marks a message as unopened.
     * 
     * @param messageNode the message node
     */
    public void markMessageUnopened(MessageNode messageNode) {
        trackButtonClick("markMessageUnopened");
        messageNode.markMessageUnopened();
    }

    /**
     * Make sure the message node has loaded content, then run the provided
     * runnable on the UI thread.
     */
    private static void refreshMessageAndRun(final MessageNode messageNode, final Runnable runnable) {
        if(messageNode.hasMessageContent()) {
            runnable.run();
        }
        else if(messageNode.hasCachedContent()) {
            final MessageNodeListener listener =
                new MessageNodeListener() {
                public void messageStatusChanged(MessageNodeEvent e) {
                    if(e.getType() == MessageNodeEvent.TYPE_CONTENT_LOADED) {
                        messageNode.removeMessageNodeListener(this);
                        UiApplication.getUiApplication().invokeLater(runnable);
                    }
                }};
            messageNode.addMessageNodeListener(listener);
            if(!messageNode.refreshMessageCacheOnly(GlobalConfig.MESSAGE_DISPLAY_PLAIN_TEXT)) {
                messageNode.removeMessageNodeListener(listener);
            }
        }
    }
    
    /**
     * Compose a reply to the message.
     * 
     * @param messageNode the message node
     */
    public void replyMessage(final MessageNode messageNode) {
        trackButtonClick("replyMessage");
        refreshMessageAndRun(messageNode, new Runnable() {
            public void run() {
                navigationController.displayCompositionReply(
                        (NetworkAccountNode)messageNode.getParent().getParentAccount(),
                        messageNode, false);
            }});
    }
    
    /**
     * Compose a reply to all recipients of the message.
     * 
     * @param messageNode the message node
     */
    public void replyAllMessage(final MessageNode messageNode) {
        trackButtonClick("replyAllMessage");
        refreshMessageAndRun(messageNode, new Runnable() {
            public void run() {
                navigationController.displayCompositionReply(
                        (NetworkAccountNode)messageNode.getParent().getParentAccount(),
                        messageNode, true);
            }});
    }
    
    /**
     * Forward the message.
     * 
     * @param messageNode the message node
     */
    public void forwardMessage(final MessageNode messageNode) {
        trackButtonClick("forwardMessage");
        refreshMessageAndRun(messageNode, new Runnable() {
            public void run() {
                navigationController.displayCompositionForward(
                        (NetworkAccountNode)messageNode.getParent().getParentAccount(),
                        messageNode);
            }});
    }
    
    /**
     * Copy the message to another mailbox.
     * 
     * @param messageNode the message node
     */
    public void copyToMailbox(MessageNode messageNode) {
        trackButtonClick("copyToMailbox");
        if(messageNode.hasMessageContent() || messageNode.hasCachedContent()) {
            // Normal case where the message has been loaded within the
            // data model and all copy options should be made available.
        	AccountNode[] accountNodes = MailManager.getInstance().getMailRootNode().getAccounts();
        	MailboxSelectionDialog dialog = new MailboxSelectionDialog(
        			resources.getString(LogicMailResource.MESSAGE_SELECT_FOLDER_COPY_TO),
        			accountNodes);
        	dialog.setSelectedMailboxNode(messageNode.getParent());
        	dialog.addUnselectableNode(messageNode.getParent());
        	dialog.doModal();
        	
        	MailboxNode selectedMailbox = dialog.getSelectedMailboxNode();
        	if(selectedMailbox != null && selectedMailbox != messageNode.getParent()) {
        		if(selectedMailbox.hasCopy()
        				&& selectedMailbox.getParentAccount() == messageNode.getParent().getParentAccount()
        				&& !(messageNode instanceof OutgoingMessageNode)) {
        		    // The source and destination are on the same mail store,
        		    // and that mail store supports protocol-level copy.
        			selectedMailbox.copyMessageInto(messageNode);
        		}
        		else if(messageNode.hasMessageContent()) {
        		    // Protocol-level copy is not possible, so just append
        		    // to the destination mailbox.
        			selectedMailbox.appendMessage(messageNode);
        		}
        		else if(messageNode.hasCachedContent()) {
        		    // Message is available in the cache, but not yet loaded.
        		    // We need to first load from the cache, then proceed with
        		    // an append on the destination mailbox.
        		    
        		    // Add a listener to the message node and then trigger a refresh.
        		    messageNode.addMessageNodeListener(new CopyToMessageNodeListener(selectedMailbox));
        		    messageNode.refreshMessage(GlobalConfig.MESSAGE_DISPLAY_PLAIN_TEXT);
        		}
        	}
        }
        else if(messageNode.getParent().hasCopy()) {
            // Alternate case where the message has not been loaded, but is
            // on a mail store that supports protocol-level copy.  In this
            // situation, only other mailboxes on the same mail store are
            // to be considered valid destinations.
            MailboxSelectionDialog dialog = new MailboxSelectionDialog(
                    resources.getString(LogicMailResource.MESSAGE_SELECT_FOLDER_COPY_TO),
                    new AccountNode[] { messageNode.getParent().getParentAccount() });
            dialog.setSelectedMailboxNode(messageNode.getParent());
            dialog.addUnselectableNode(messageNode.getParent());
            dialog.doModal();
            
            MailboxNode selectedMailbox = dialog.getSelectedMailboxNode();
            if(selectedMailbox != null && selectedMailbox != messageNode.getParent()) {
                selectedMailbox.copyMessageInto(messageNode);
            }
        }
    }
    
    /**
     * Handles the result of a message refresh as triggered by a copy
     * operation when the message needs to be loaded from cache.
     */
    private class CopyToMessageNodeListener implements MessageNodeListener {
        private MailboxNode selectedMailbox;
        public CopyToMessageNodeListener(MailboxNode selectedMailbox) {
            this.selectedMailbox = selectedMailbox;
        }

        public void messageStatusChanged(MessageNodeEvent e) {
            if(e.getType() == MessageNodeEvent.TYPE_CONTENT_LOADED) {
                // Remove this listener from the message node, and request
                // that it be appended to the selected destination mailbox.
                MessageNode messageNode = (MessageNode)e.getSource();
                messageNode.removeMessageNodeListener(this);
                selectedMailbox.appendMessage(messageNode);
            }
        }
    }
    
    /**
     * Move the message to another mailbox.
     * 
     * @param messageNode the message node
     */
    public void moveToMailbox(MessageNode messageNode) {
        trackButtonClick("moveToMailbox");
        if(messageNode.getParent().hasCopy()) {
        	MailboxSelectionDialog dialog = new MailboxSelectionDialog(
        			resources.getString(LogicMailResource.MESSAGE_SELECT_FOLDER_MOVE_TO),
        			new AccountNode[] { messageNode.getParent().getParentAccount() });
        	dialog.setSelectedMailboxNode(messageNode.getParent());
        	dialog.addUnselectableNode(messageNode.getParent());
        	dialog.doModal();
        	
        	MailboxNode selectedMailbox = dialog.getSelectedMailboxNode();
            if(selectedMailbox != null && selectedMailbox != messageNode.getParent()) {
                selectedMailbox.moveMessageInto(messageNode);
            }
        }
    }
    
    /**
     * Send an outgoing message.
     * 
     * @param messageNode the message node
     */
    public void sendMessage(MessageNode messageNode) {
        trackButtonClick("sendMessage");
        if(messageNode instanceof OutgoingMessageNode) {
            OutgoingMessageNode outgoingMessage = (OutgoingMessageNode)messageNode;
            if(outgoingMessage.isSendAttempted() && !outgoingMessage.isSending()) {
                if(validateMessageSender(outgoingMessage)) {
                    outgoingMessage.sendMessage();
                }
            }
        }
    }

    /**
     * Validate that the message has a sender configured for it, and
     * prompt the user appropriately if it does not.
     * 
     * @param outgoingMessage the outgoing message
     * @return true if the message is now sendable, false to cancel
     */
    private boolean validateMessageSender(OutgoingMessageNode outgoingMessage) {
        if(outgoingMessage.getMailSender() != null) {
            return true;
        }
        else {
            AbstractMailSender[] senders = getMailSenders();
            if(senders.length == 0) {
                Dialog.alert(resources.getString(LogicMailResource.MESSAGE_NO_OUTGOING_SERVERS));
                return false;
            }
            else {
                int choice = Dialog.ask(
                        resources.getString(LogicMailResource.MESSAGE_SELECT_OUTGOING_SERVER),
                        senders, 0);
                if(choice < 0 || choice >= senders.length) {
                    return false;
                }
                else {
                    outgoingMessage.setMailSender(senders[choice]);
                    return true;
                }
            }
        }
    }
    
    private AbstractMailSender[] getMailSenders() {
        MailSettings mailSettings = MailSettings.getInstance();
        int num = mailSettings.getNumOutgoing();
        AbstractMailSender[] result = new AbstractMailSender[num];
        for(int i=0; i<num; i++) {
            OutgoingConfig outgoingConfig = mailSettings.getOutgoingConfig(i);
            result[i] = MailFactory.createMailSender(outgoingConfig);
        }
        return result;
    }
}
