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
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.Menu;

import org.logicprobe.LogicMail.LogicMailResource;
import org.logicprobe.LogicMail.conf.AccountConfig;
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
	
    /** Current message node from the last call to makeMenu() */
    private MessageNode activeMessageNode;
    
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
    private void initMenuItems() {
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
        propertiesItem = new MenuItem(resources, LogicMailResource.MENUITEM_PROPERTIES, 300120, 1030) {
            public void run() {
                openMessageProperties(activeMessageNode);
            }
        };
        replyItem = new MenuItem(resources, LogicMailResource.MENUITEM_REPLY, 300130, 2000) {
            public void run() {
                replyMessage(activeMessageNode);
            }
        };
        replyAllItem = new MenuItem(resources, LogicMailResource.MENUITEM_REPLYTOALL, 300140, 2000) {
            public void run() {
                replyAllMessage(activeMessageNode);
            }
        };
        forwardItem = new MenuItem(resources, LogicMailResource.MENUITEM_FORWARD, 300150, 2000) {
            public void run() {
                forwardMessage(activeMessageNode);
            }
        };
        copyToItem = new MenuItem(resources, LogicMailResource.MENUITEM_COPY_TO, 300160, 2000) {
            public void run() {
                copyToMailbox(activeMessageNode);
            }
        };
        moveToItem = new MenuItem(resources, LogicMailResource.MENUITEM_MOVE_TO, 300170, 2000) {
            public void run() {
                moveToMailbox(activeMessageNode);
            }
        };
        
        deleteItem = new MenuItem(resources, LogicMailResource.MENUITEM_DELETE, 300180, 2000) {
            public void run() {
                deleteMessage(activeMessageNode);
            }
        };
        undeleteItem = new MenuItem(resources, LogicMailResource.MENUITEM_UNDELETE, 300190, 2000) {
            public void run() {
                undeleteMessage(activeMessageNode);
            }
        };
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
     * supports only one menu, this may be ignored. By default, it is 0.
     * @param messageNode The message node for which menu items should be offered.
     * @param isOpen True if the message node is currently open, false if it is
     * displayed along with other message nodes.
     */
    public void makeMenu(Menu menu, int instance, MessageNode messageNode, boolean isOpen) {
        if(messageNode == null) { return; }
        
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
        
        
        if(!isOpen) {
            menu.add(selectItem);
        }
        
        menu.add(propertiesItem);
        
        if(!unloaded) {
            if(accountNode.hasMailSender()) {
                menu.add(replyItem);
                if(accountNode.hasIdentity()) {
                    menu.add(replyAllItem);
                }
                menu.add(forwardItem);
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
            menu.add(copyToItem);
        }
        
        // Move-To is currently only supported if the underlying mail store
        // supports protocol-level copy, and then only between folders on that
        // mail store.  These limitations have been chosen because it is the
        // simplest way to be absolutely sure that a user cannot mess up their
        // data with a message move.
        if(mailboxNode.hasCopy()) {
            menu.add(moveToItem);
        }
        
        if((messageNode.getFlags() & MessageNode.Flag.DELETED) != 0) {
            if(mailboxNode != null && mailboxNode.getParentAccount().hasUndelete()) {
                menu.add(undeleteItem);
            }
        }
        else {
            menu.add(deleteItem);
        }
        if(failedOutgoingMessage) {
            menu.add(sendOutgoingItem);
        }
        
        this.activeMessageNode = messageNode;
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
    	AccountNode[] accounts = MailManager.getInstance().getMailRootNode().getAccounts();

    	for(int i=0; i<accounts.length; i++) {
    	    if(accounts[i].hasMailSender()) {
        		AccountConfig accountConfig = accounts[i].getAccountConfig();
        		if(accountConfig != null) {
        			if(accountConfig.getDraftMailbox() == messageNode.getParent()) {
        				matchingAccounts.addElement(accounts[i]);
        			}
        		}
    	    }
    	}

    	// If no matching accounts were found, then add all
    	// non-local accounts so we have something for the
    	// user to select from.
    	if(matchingAccounts.size() == 0) {
        	for(int i=0; i<accounts.length; i++) {
        		if(accounts[i].hasMailSender() && accounts[i].getAccountConfig() != null) {
    				matchingAccounts.addElement(accounts[i]);
        		}
        	}
    	}
    	
    	// Select the account node that matches this mailbox, prompting the
    	// user if necessary.
    	AccountNode account;
    	int size = matchingAccounts.size();
    	if(size > 1) {
    		AccountNode[] choices = new AccountNode[size];
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
    		account = (AccountNode)matchingAccounts.elementAt(0);
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
    	MessagePropertiesScreen propertiesScreen =
    		new MessagePropertiesScreen(messageNode);
        UiApplication.getUiApplication().pushModalScreen(propertiesScreen);
    }
    
    /**
     * Delete message.
     * 
     * @param messageNode the message node
     */
    public void deleteMessage(MessageNode messageNode) {
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
            }
        }
        else {
            messageNode.deleteMessage();
        }
    }
    
    /**
     * Undelete message.
     * 
     * @param messageNode the message node
     */
    public void undeleteMessage(MessageNode messageNode) {
		messageNode.undeleteMessage();
    }
    
    /**
     * Compose a reply to the message.
     * 
     * @param messageNode the message node
     */
    public void replyMessage(MessageNode messageNode) {
        if(messageNode.hasMessageContent()) {
            navigationController.displayCompositionReply(messageNode.getParent().getParentAccount(), messageNode, false);
        }
    }
    
    /**
     * Compose a reply to all recipients of the message.
     * 
     * @param messageNode the message node
     */
    public void replyAllMessage(MessageNode messageNode) {
        if(messageNode.hasMessageContent()) {
            navigationController.displayCompositionReply(messageNode.getParent().getParentAccount(), messageNode, true);
        }
    }
    
    /**
     * Forward the message.
     * 
     * @param messageNode the message node
     */
    public void forwardMessage(MessageNode messageNode) {
        if(messageNode.hasMessageContent()) {
            navigationController.displayCompositionForward(messageNode.getParent().getParentAccount(), messageNode);
        }
    }
    
    /**
     * Copy the message to another mailbox.
     * 
     * @param messageNode the message node
     */
    public void copyToMailbox(MessageNode messageNode) {
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
        		    messageNode.refreshMessage();
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
