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
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.Dialog;

import org.logicprobe.LogicMail.LogicMailResource;
import org.logicprobe.LogicMail.conf.AccountConfig;
import org.logicprobe.LogicMail.model.AccountNode;
import org.logicprobe.LogicMail.model.MailManager;
import org.logicprobe.LogicMail.model.MailboxNode;
import org.logicprobe.LogicMail.model.MessageNode;
import org.logicprobe.LogicMail.model.OutgoingMessageNode;

/**
 * Delegate class to handle actions on message nodes.
 */
public class MessageActions {
	protected static ResourceBundle resources = ResourceBundle.getBundle(LogicMailResource.BUNDLE_ID, LogicMailResource.BUNDLE_NAME);
	private NavigationController navigationController;
	
	public MessageActions(NavigationController navigationController) {
		this.navigationController = navigationController;
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
    	// configured as their drafts folder.
    	Vector matchingAccounts = new Vector();
    	AccountNode[] accounts = MailManager.getInstance().getMailRootNode().getAccounts();

    	for(int i=0; i<accounts.length; i++) {
    		AccountConfig accountConfig = accounts[i].getAccountConfig();
    		if(accountConfig != null) {
    			if(accountConfig.getDraftMailbox() == messageNode.getParent()) {
    				matchingAccounts.addElement(accounts[i]);
    			}
    		}
    	}

    	// If no matching accounts were found, then add all
    	// non-local accounts so we have something for the
    	// user to select from.
    	if(matchingAccounts.size() == 0) {
        	for(int i=0; i<accounts.length; i++) {
        		if(accounts[i].getAccountConfig() != null) {
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
        if(Dialog.ask(Dialog.D_YES_NO, resources.getString(LogicMailResource.MAILBOX_DELETE_PROMPT)) == Dialog.YES) {
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
        		if(selectedMailbox.hasCopy()
        				&& selectedMailbox.getParentAccount() == messageNode.getParent().getParentAccount()) {
        			selectedMailbox.copyMessageInto(messageNode);
        		}
        		else {
        			selectedMailbox.appendMessage(messageNode);
        		}
        	}
        }
    }
    
    /**
     * Move the message to another mailbox.
     * 
     * @param messageNode the message node
     */
    public void moveToMailbox(MessageNode messageNode) {
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
    
    /**
     * Send an outgoing message.
     * 
     * @param messageNode the message node
     */
    public void sendMessage(MessageNode messageNode) {
        if(messageNode instanceof OutgoingMessageNode) {
            OutgoingMessageNode outgoingMessage = (OutgoingMessageNode)messageNode;
            outgoingMessage.sendMessage();
        }
    }
}
