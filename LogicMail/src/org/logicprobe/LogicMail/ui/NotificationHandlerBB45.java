/*-
 * Copyright (c) 2011, Derek Konigsberg
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

import net.rim.blackberry.api.homescreen.HomeScreen;
import net.rim.device.api.notification.NotificationsConstants;
import net.rim.device.api.notification.NotificationsManager;
import net.rim.device.api.system.Application;
import net.rim.device.api.system.EventLogger;
import net.rim.device.api.system.HolsterListener;

import org.logicprobe.LogicMail.AppInfo;
import org.logicprobe.LogicMail.LogicMailEventSource;
import org.logicprobe.LogicMail.LogicMailRuntimeState;
import org.logicprobe.LogicMail.conf.MailSettings;
import org.logicprobe.LogicMail.conf.MailSettingsEvent;
import org.logicprobe.LogicMail.conf.MailSettingsListener;
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

public class NotificationHandlerBB45 extends NotificationHandler {
    /**
     * Map of <code>AccountNode</code> to <code>MailboxNode[]</code> tracking
     * subscribed mailboxes.  The first mailbox in the array is assumed to be
     * the Inbox.
     */
    private final Hashtable accountMap = new Hashtable();
    
    private boolean isEnabled;
    private boolean notificationTriggered;
    
    public NotificationHandlerBB45() {
        Application.getApplication().addHolsterListener(holsterListener);
        
        MailManager.getInstance().addMailManagerListener(new MailManagerListener() {
            public void mailConfigurationChanged(MailManagerEvent e) {
                handleMailConfigurationChanged(e);
            }
        });
        MailSettings.getInstance().addMailSettingsListener(new MailSettingsListener() {
            public void mailSettingsSaved(MailSettingsEvent e) {
                handleMailSettingsSaved(e);
            }
        });
        
        try {
            updateAccountSubscriptions();
        } catch (Throwable t) {
            EventLogger.logEvent(AppInfo.GUID,
                    ("Unable to update notification sources:\r\n"
                            + t.toString()).toString().getBytes(),
                            EventLogger.ERROR);
        }
    }

    private AccountNodeListener accountNodeListener = new AccountNodeListener() {
        public void accountStatusChanged(AccountNodeEvent e) {
            if(e.getType() == AccountNodeEvent.TYPE_MAILBOX_TREE
                    && e.getSource() instanceof NetworkAccountNode) {
                updateAccountMap((NetworkAccountNode)e.getSource());
                updateMessageIndicator();
            }
        }
    };
    private MailboxNodeListener mailboxNodeListener = new MailboxNodeListener() {
        public void mailboxStatusChanged(MailboxNodeEvent e) {
            mailboxNodeStatusChanged(e);
        }
    };
    private HolsterListener holsterListener = new HolsterListener() {
        public void inHolster() {
        }
        public void outOfHolster() {
        }
    };

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.ui.NotificationHandler#setEnabled(boolean)
     */
    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.ui.NotificationHandler#shutdown()
     */
    public void shutdown() {
        cancelNotification();
        indicateUnseenMessageCount(0, false);
        isEnabled = false;
        Application.getApplication().removeHolsterListener(holsterListener);
    }
    
    protected void handleMailConfigurationChanged(MailManagerEvent e) {
        updateAccountSubscriptions();
        updateMessageIndicator();
    }

    protected void handleMailSettingsSaved(MailSettingsEvent e) {
        updateAccountSubscriptions();
        updateMessageIndicator();
    }
    
    protected void mailboxNodeStatusChanged(MailboxNodeEvent e) {
        if(!isEnabled) { return; }
        
        MailboxNode mailboxNode = (MailboxNode)e.getSource();

        switch(e.getType()) {
        case MailboxNodeEvent.TYPE_NEW_MESSAGES:
            boolean raiseNotification = messagesAreRecent(e.getAffectedMessages());
            if(raiseNotification && !notificationTriggered) {
                notificationTriggered = true;
                triggerNotification(mailboxNode);
            }
            break;
        case MailboxNodeEvent.TYPE_FETCH_COMPLETE:
            notificationTriggered = false;
        case MailboxNodeEvent.TYPE_DELETED_MESSAGES:
        case MailboxNodeEvent.TYPE_STATUS:
            updateMessageIndicator();
            break;
        }
    }

    private boolean messagesAreRecent(MessageNode[] messages) {
        boolean recent = false;
        for(int i=0; i<messages.length; i++) {
            if((messages[i].getFlags() & MessageNode.Flag.RECENT) != 0) {
                recent = true;
                break;
            }
        }
        return recent;
    }

    private void updateMessageIndicator() {
        int count = calculateIndicatorCount();
        boolean recent = checkForRecentMessages();
        indicateUnseenMessageCount(count, recent);
    }

    private int calculateIndicatorCount() {
        int count = 0;
        synchronized(accountMap) {
            Enumeration e = accountMap.elements();
            while(e.hasMoreElements()) {
                MailboxNode[] mailboxNodes = (MailboxNode[])e.nextElement();
                for(int i=0; i<mailboxNodes.length; i++) {
                    count += mailboxNodes[i].getUnseenMessageCount();
                }
            }
        }
        return count;
    }

    private boolean checkForRecentMessages() {
        synchronized(accountMap) {
            Enumeration e = accountMap.elements();
            while(e.hasMoreElements()) {
                MailboxNode[] mailboxNodes = (MailboxNode[])e.nextElement();
                for(int i=0; i<mailboxNodes.length; i++) {
                    if(mailboxNodes[i].getRecentMessageCount() > 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * Called when the message indicator(s) need to be updated.
     *
     * @param count the number of new messages that should be indicated
     * @param recent true, if at least one message is considered recent
     */
    protected void indicateUnseenMessageCount(int count, boolean recent) {
        setAppIcon(count > 0);
    }

    /**
     * Update the account subscriptions.
     */
    private void updateAccountSubscriptions() {
        // Get the runtime state object
        LogicMailRuntimeState runtimeState = LogicMailRuntimeState.getInstance();
        
        // Subscribe to any new accounts
        NetworkAccountNode[] accountNodes = subscribeToNewAccounts(runtimeState);

        // Unsubscribe from any deleted accounts
        unsubscribeFromDeletedAccounts(runtimeState, accountNodes);
    }
    
    /**
     * Check account subscriptions, registering notification sources as necessary.
     * 
     * @param eventSourceMap Map of account IDs to event source objects
     * @return Configured network account nodes
     */
    private NetworkAccountNode[] subscribeToNewAccounts(LogicMailRuntimeState runtimeState) {
        NetworkAccountNode[] accountNodes = MailManager.getInstance().getMailRootNode().getNetworkAccounts();
        for(int i=0; i<accountNodes.length; i++) {
            updateAccountMap(accountNodes[i]);
            
            accountNodes[i].addAccountNodeListener(accountNodeListener);
            
            // Register the notification source, if necessary
            long accountUniqueId = ((NetworkAccountNode)accountNodes[i]).getUniqueId();
            String accountName = accountNodes[i].toString();
            LogicMailEventSource eventSource = runtimeState.getEventSource(accountUniqueId);
            if(eventSource == null || !eventSource.getAccountName().equals(accountName)) {
                eventSource = new LogicMailEventSource(accountName, accountUniqueId);
                NotificationsManager.registerSource(
                    eventSource.getEventSourceId(),
                    eventSource,
                    NotificationsConstants.CASUAL);
                runtimeState.putEventSource(eventSource);
            }
        }
        return accountNodes;
    }

    private void unsubscribeFromDeletedAccounts(LogicMailRuntimeState runtimeState, NetworkAccountNode[] accountNodes) {
        synchronized(accountMap) {
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
                MailboxNode[] mailboxes = (MailboxNode[])accountMap.remove(accountNode);
                for(int i=0; i<mailboxes.length; i++) {
                    mailboxes[i].removeMailboxNodeListener(mailboxNodeListener);
                }
                accountNode.removeAccountNodeListener(accountNodeListener);

                // Unregister the notification source
                long accountUniqueId = ((NetworkAccountNode)accountNode).getUniqueId();
                LogicMailEventSource eventSource = runtimeState.getEventSource(accountUniqueId);
                if(eventSource != null) {
                    NotificationsManager.deregisterSource(eventSource.getEventSourceId());
                    runtimeState.removeEventSource(accountUniqueId);
                }
            }
        }
    }
    
    /**
     * Update the subscriptions for the provided account node.
     * 
     * @param accountNode The account node
     */
    private void updateAccountMap(NetworkAccountNode accountNode) {
        synchronized(accountMap) {
            // Remove any existing listeners for the updated account
            MailboxNode[] mailboxes = (MailboxNode[])accountMap.remove(accountNode);
            if(mailboxes != null) {
                for(int i=0; i<mailboxes.length; i++) {
                    mailboxes[i].removeMailboxNodeListener(mailboxNodeListener);
                }
            }

            // Add listeners for the notification mailboxes on the updated account
            mailboxes = accountNode.getNotificationMailboxes();
            for(int i=0; i<mailboxes.length; i++) {
                mailboxes[i].addMailboxNodeListener(mailboxNodeListener);
            }
            accountMap.put(accountNode, mailboxes);
        }
    }
    
    /**
     * Notify the user of new messages.
     * 
     * @param mailboxNode The mailbox node containing the new messages
     */
    private void triggerNotification(MailboxNode mailboxNode) {
        long accountUniqueId = ((NetworkAccountNode)mailboxNode.getParentAccount()).getUniqueId();
        LogicMailEventSource eventSource = LogicMailRuntimeState.getInstance().getEventSource(accountUniqueId);
        NotificationsManager.triggerImmediateEvent(eventSource.getEventSourceId(), 0, this, null);
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.ui.NotificationHandler#cancelNotification()
     */
    public void cancelNotification() {
        LogicMailRuntimeState runtimeState = LogicMailRuntimeState.getInstance();
        synchronized(accountMap) {
            Enumeration e = accountMap.keys();
            while(e.hasMoreElements()) {
                AccountNode accountNode = (AccountNode)e.nextElement();
                long accountUniqueId = ((NetworkAccountNode)accountNode).getUniqueId();
                LogicMailEventSource eventSource = runtimeState.getEventSource(accountUniqueId);
                NotificationsManager.cancelImmediateEvent(eventSource.getEventSourceId(), 0, this, null);
            }
        }
        updateMessageIndicator();
    }
    
    protected void setAppIcon(boolean newMessages) {
        if(newMessages) {
            HomeScreen.updateIcon(AppInfo.getNewMessagesIcon(), 0);
            HomeScreen.setRolloverIcon(AppInfo.getNewMessagesRolloverIcon(), 0);
        }
        else {
            HomeScreen.updateIcon(AppInfo.getIcon(), 0);
            HomeScreen.setRolloverIcon(AppInfo.getRolloverIcon(), 0);
        }
    }
}
