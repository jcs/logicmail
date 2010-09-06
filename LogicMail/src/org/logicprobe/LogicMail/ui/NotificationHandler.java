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
import org.logicprobe.LogicMail.LogicMailEventSource;
import org.logicprobe.LogicMail.conf.AccountConfig;
import org.logicprobe.LogicMail.model.AccountNode;
import org.logicprobe.LogicMail.model.AccountNodeEvent;
import org.logicprobe.LogicMail.model.AccountNodeListener;
import org.logicprobe.LogicMail.model.MailManager;
import org.logicprobe.LogicMail.model.MailManagerEvent;
import org.logicprobe.LogicMail.model.MailManagerListener;
import org.logicprobe.LogicMail.model.MailboxNode;
import org.logicprobe.LogicMail.model.MailboxNodeEvent;
import org.logicprobe.LogicMail.model.MailboxNodeListener;
import org.logicprobe.LogicMail.model.MessageNode;
import org.logicprobe.LogicMail.model.NetworkAccountNode;

import net.rim.blackberry.api.homescreen.HomeScreen;
import net.rim.device.api.notification.NotificationsConstants;
import net.rim.device.api.notification.NotificationsManager;
import net.rim.device.api.system.Application;
import net.rim.device.api.system.HolsterListener;
import net.rim.device.api.system.RuntimeStore;
import net.rim.device.api.util.LongHashtable;

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

	private AccountNodeListener accountNodeListener = new AccountNodeListener() {
		public void accountStatusChanged(AccountNodeEvent e) {
			if(e.getType() == AccountNodeEvent.TYPE_MAILBOX_TREE) {
				updateAccountMap((AccountNode)e.getSource());
			}
		}
	};
	
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
					if((messages[i].getFlags() & MessageNode.Flag.RECENT) != 0) {
						raiseNotification = true;
						break;
					}
				}
				if(raiseNotification) {
					notifyNewMessages(mailboxNode);
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
	
	/**
	 * Update the account subscriptions.
	 */
	private void updateAccountSubscriptions() {
		// Get the registered event sources from the runtime store
		LongHashtable eventSourceMap = (LongHashtable)RuntimeStore.getRuntimeStore().get(AppInfo.GUID);
		if(eventSourceMap == null) {
			eventSourceMap = new LongHashtable();
			RuntimeStore.getRuntimeStore().put(AppInfo.GUID, eventSourceMap);
		}
		
		// Subscribe to any new accounts
		NetworkAccountNode[] accountNodes = MailManager.getInstance().getMailRootNode().getNetworkAccounts();
		for(int i=0; i<accountNodes.length; i++) {
			updateAccountMap(accountNodes[i]);
			
			accountNodes[i].addAccountNodeListener(accountNodeListener);
			
			// Register the notification source, if necessary
			AccountConfig accountConfig = ((NetworkAccountNode)accountNodes[i]).getAccountConfig();
			LogicMailEventSource eventSource = (LogicMailEventSource)eventSourceMap.get(accountConfig.getUniqueId());
			if(eventSource == null || !eventSource.getAccountName().equals(accountConfig.getAcctName())) {
				eventSource =
					new LogicMailEventSource(accountConfig.getAcctName(), accountConfig.getUniqueId());
            	NotificationsManager.registerSource(
        			eventSource.getEventSourceId(),
        			eventSource,
        			NotificationsConstants.CASUAL);
            	eventSourceMap.put(accountConfig.getUniqueId(), eventSource);
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
			accountNode.removeAccountNodeListener(accountNodeListener);

			// Unregister the notification source
			long eventSourceKey = ((NetworkAccountNode)accountNode).getAccountConfig().getUniqueId();
			LogicMailEventSource eventSource = (LogicMailEventSource)eventSourceMap.get(eventSourceKey);
			if(eventSource != null) {
				NotificationsManager.deregisterSource(eventSource.getEventSourceId());
				eventSourceMap.remove(eventSourceKey);
			}
		}
	}
	
	/**
	 * Update the INBOX subscription for the provided account node.
	 * 
	 * @param accountNode The account node
	 */
	private void updateAccountMap(AccountNode accountNode) {
		MailboxNode rootMailbox = accountNode.getRootMailbox();
		if(rootMailbox != null) {
			MailboxNode[] mailboxNodes = rootMailbox.getMailboxes();
			MailboxNode inboxNode = null;
			for(int j=0; j<mailboxNodes.length; j++) {
				if(mailboxNodes[j].toString().equalsIgnoreCase("INBOX")) {
					inboxNode = mailboxNodes[j];
					break;
				}
			}
			
			if(inboxNode != null) {
				if(accountMap.containsKey(accountNode) && accountMap.get(accountNode) != inboxNode) {
					((MailboxNode)accountMap.get(accountNode)).removeMailboxNodeListener(mailboxNodeListener);
				}
				else if(!accountMap.containsKey(accountNode)) {
					inboxNode.addMailboxNodeListener(mailboxNodeListener);
					accountMap.put(accountNode, inboxNode);
				}
			}
		}
	}
	
	/**
	 * Notify the user of new messages.
	 * 
	 * @param mailboxNode The mailbox node containing the new messages
	 */
	private void notifyNewMessages(MailboxNode mailboxNode) {
		long sourceId = AppInfo.GUID + ((NetworkAccountNode)mailboxNode.getParentAccount()).getAccountConfig().getUniqueId();
		NotificationsManager.triggerImmediateEvent(sourceId, 0, this, null);
		setAppIcon(true);
	}

	/**
	 * Cancel all existing notifications.
	 */
	public void cancelNotification() {
		Enumeration e = accountMap.keys();
		while(e.hasMoreElements()) {
			AccountNode accountNode = (AccountNode)e.nextElement();
			long sourceId = AppInfo.GUID + ((NetworkAccountNode)accountNode).getAccountConfig().getUniqueId();
			NotificationsManager.cancelImmediateEvent(sourceId, 0, this, null);
		}
		
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
