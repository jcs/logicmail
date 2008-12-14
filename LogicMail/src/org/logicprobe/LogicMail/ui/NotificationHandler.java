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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.logicprobe.LogicMail.AppInfo;
import org.logicprobe.LogicMail.model.AccountNode;
import org.logicprobe.LogicMail.model.MailManager;
import org.logicprobe.LogicMail.model.MailManagerEvent;
import org.logicprobe.LogicMail.model.MailManagerListener;
import org.logicprobe.LogicMail.model.MailboxNode;
import org.logicprobe.LogicMail.model.MailboxNodeEvent;
import org.logicprobe.LogicMail.model.MailboxNodeListener;
import org.logicprobe.LogicMail.model.MessageNode;

import net.rim.blackberry.api.homescreen.HomeScreen;
import net.rim.device.api.notification.NotificationsManager;
import net.rim.device.api.system.Application;
import net.rim.device.api.system.HolsterListener;

/**
 * Handles new message notification through the various
 * notification mechanisms of the BlackBerry.
 */
public class NotificationHandler {
	private static NotificationHandler instance = null;
	private Hashtable accountMap;
	private boolean isEnabled;

	private NotificationHandler() {
		accountMap = new Hashtable();
		Application.getApplication().addHolsterListener(holsterListener);
		
		MailManager.getInstance().addMailManagerListener(new MailManagerListener() {
			public void mailConfigurationChanged(MailManagerEvent e) {
				MailManager_mailConfigurationChanged(e);
			}
		});
		updateAccountSubscriptions();
	}

	/**
	 * Gets the NotificationHandler instance.
	 * 
	 * @return Single instance of NotificationHandler
	 */
	public static synchronized NotificationHandler getInstance() {
		if(instance == null) {
			instance = new NotificationHandler();
		}
		return instance;
	}
	
	/**
	 * Sets whether notifications are enabled.
	 * 
	 * @param isEnabled True to enable notifications, false to disable
	 */
	public void setEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
	}

	/**
	 * Shutdown the listener and unsubscribe from any system events.
	 */
	public void shutdown() {
		cancelNotification();
		isEnabled = false;
		Application.getApplication().removeHolsterListener(holsterListener);
	}
	
	private HolsterListener holsterListener = new HolsterListener() {
		public void inHolster() {
		}

		public void outOfHolster() {
		}
	};
	
	private MailboxNodeListener mailboxNodeListener = new MailboxNodeListener() {
		public void mailboxStatusChanged(MailboxNodeEvent e) {
			mailboxNodeListener_mailboxStatusChanged(e);
		}
	};
	
	private void MailManager_mailConfigurationChanged(MailManagerEvent e) {
		updateAccountSubscriptions();
	}
	
	private void mailboxNodeListener_mailboxStatusChanged(MailboxNodeEvent e) {
		MailboxNode mailboxNode = (MailboxNode)e.getSource();
		if(e.getType() == MailboxNodeEvent.TYPE_NEW_MESSAGES) {
			if(isEnabled) {
				boolean raiseNotification = false;
				MessageNode[] messages = e.getAffectedMessages();
				for(int i=0; i<messages.length; i++) {
					if(messages[i].getFolderMessage().getFlags().isRecent()) {
						raiseNotification = true;
						break;
					}
				}
				if(raiseNotification) {
					notifyNewMessages();
				}
			}
		}
		else if(e.getType() == MailboxNodeEvent.TYPE_STATUS) {
			if(mailboxNode.getUnseenMessageCount() > 0) {
				setAppIcon(true);
			}
			else {
				setAppIcon(false);
			}
		}
	}
	
	private void updateAccountSubscriptions() {
		// Subscribe to any new accounts
		AccountNode[] accountNodes = MailManager.getInstance().getMailRootNode().getAccounts();
		for(int i=0; i<accountNodes.length; i++) {
			if(accountNodes[i].getStatus() != AccountNode.STATUS_LOCAL) {
				MailboxNode[] mailboxNodes = accountNodes[i].getRootMailbox().getMailboxes();
				MailboxNode inboxNode = null;
				for(int j=0; j<mailboxNodes.length; j++) {
					if(mailboxNodes[j].getName().equalsIgnoreCase("INBOX")) {
						inboxNode = mailboxNodes[j];
						break;
					}
				}
				
				if(inboxNode != null) {
					if(accountMap.containsKey(accountNodes[i]) && accountMap.get(accountNodes[i]) != inboxNode) {
						((MailboxNode)accountMap.get(accountNodes[i])).removeMailboxNodeListener(mailboxNodeListener);
					}
					else if(!accountMap.containsKey(accountNodes[i])) {
						inboxNode.addMailboxNodeListener(mailboxNodeListener);
						accountMap.put(accountNodes[i], inboxNode);
					}
				}
			}
		}

		// Unsubscribe from any deleted accounts
		Vector deletedAccounts = new Vector();
		Enumeration e = accountMap.keys();
		while(e.hasMoreElements()) {
			AccountNode accountNode = (AccountNode)e.nextElement();
			boolean accountDeleted = true;
			for(int i=0; i<accountNodes.length; i++) {
				if(accountNodes[i] == accountNode) {
					accountDeleted = false;
					break;
				}
			}
			if(accountDeleted) {
				deletedAccounts.addElement(accountNode);
			}
		}
		
		e = deletedAccounts.elements();
		while(e.hasMoreElements()) {
			AccountNode accountNode = (AccountNode)e.nextElement();
			((MailboxNode)accountMap.get(accountNode)).removeMailboxNodeListener(mailboxNodeListener);
			accountMap.remove(accountNode);
		}
	}
	
	
	/**
	 * Notify the user of new messages.
	 * 
	 * @param mailboxNode The mailbox node containing the new messages
	 */
	private void notifyNewMessages() {
		NotificationsManager.triggerImmediateEvent(AppInfo.GUID, 0, this, null);
		setAppIcon(true);
	}

	public void cancelNotification() {
		NotificationsManager.cancelImmediateEvent(AppInfo.GUID, 0, this, null);
		setAppIcon(false);
	}
	
	private void setAppIcon(boolean newMessages) {
		if(newMessages) {
			HomeScreen.updateIcon(AppInfo.getNewMessagesIcon());
			HomeScreen.setRolloverIcon(AppInfo.getNewMessagesRolloverIcon());
		}
		else {
			HomeScreen.updateIcon(AppInfo.getIcon());
			HomeScreen.setRolloverIcon(AppInfo.getRolloverIcon());
		}
	}
}
