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
package org.logicprobe.LogicMail.model;

import java.util.Vector;

import org.logicprobe.LogicMail.conf.AccountConfig;
import org.logicprobe.LogicMail.conf.GlobalConfig;
import org.logicprobe.LogicMail.conf.MailSettings;
import org.logicprobe.LogicMail.conf.MailSettingsEvent;
import org.logicprobe.LogicMail.conf.MailSettingsListener;
import org.logicprobe.LogicMail.conf.OutgoingConfig;
import org.logicprobe.LogicMail.mail.AbstractMailSender;
import org.logicprobe.LogicMail.mail.MailConnectionListener;
import org.logicprobe.LogicMail.mail.MailConnectionLoginEvent;
import org.logicprobe.LogicMail.mail.MailConnectionManager;
import org.logicprobe.LogicMail.mail.MailConnectionStateEvent;
import org.logicprobe.LogicMail.mail.MailConnectionStatusEvent;
import org.logicprobe.LogicMail.mail.MailFactory;
import org.logicprobe.LogicMail.mail.NetworkMailSender;
import org.logicprobe.LogicMail.mail.NetworkMailStore;
import org.logicprobe.LogicMail.util.EventListenerList;

/**
 * Singleton that provides external access to the mail data model.
 * Ensures that clients have a fully controlled point from which
 * to get their instances of data and utility classes.
 */
public class MailManager {
	private static MailManager instance = null;
	private boolean startupComplete;
	private final EventListenerList listenerList = new EventListenerList();
	private final MailRootNode mailRootNode;
	private final MailSettings mailSettings;
	private final FolderMessageCache folderMessageCache;
    private OutboxMailboxNode outboxMailboxNode;
	
	/**
	 * Constructor.
	 */
	private MailManager() {
		mailSettings = MailSettings.getInstance();
		mailRootNode = new MailRootNode();
		folderMessageCache = new FolderMessageCache();
		folderMessageCache.restore();

		// Make sure the initial configuration is loaded
		updateMailModelAccountList();
        fireMailConfigurationChanged();
		
		// Register a listener for configuration changes
		MailSettings.getInstance().addMailSettingsListener(new MailSettingsListener() {
			public void mailSettingsSaved(MailSettingsEvent e) {
			    mailSettings_MailSettingsSaved(e);
			}
		});
		
		MailConnectionManager.getInstance().addMailConnectionListener(new MailConnectionListener() {
			public void mailConnectionStateChanged(MailConnectionStateEvent e) {
				mailConnectionManager_MailConnectionStateChanged(e);
			}
			public void mailConnectionStatus(MailConnectionStatusEvent e) { }
			public void mailConnectionError(MailConnectionStatusEvent e) { }
			public void mailConnectionLogin(MailConnectionLoginEvent e) { }
		});
		
		// Refresh data from local account node
		LocalAccountNode localAccount = mailRootNode.getLocalAccount();
		localAccount.refreshMailboxes();
		localAccount.refreshMailboxStatus();

		MailboxNode[] localMailboxes = mailRootNode.getLocalAccount().getRootMailbox().getMailboxes();
		for(int i=0; i<localMailboxes.length; i++) {
	        // Find the outbox node, and save a reference to it for easy access
			if(localMailboxes[i] instanceof OutboxMailboxNode) {
				outboxMailboxNode = (OutboxMailboxNode)localMailboxes[i];
			}
			
			// Refresh messages for local mailboxes
			localMailboxes[i].refreshMessages(true);
		}
	}
	
	private void refreshMailboxTypes() {
        AccountNode[] accounts = mailRootNode.getAccounts();
        for(int i=0; i<accounts.length; i++) {
            if(accounts[i] instanceof NetworkAccountNode) {
                clearMailboxTypes(accounts[i].getRootMailbox());
            }
        }
	    
		int num = mailSettings.getNumAccounts();
		for(int i=0; i<num; i++) {
			AccountConfig accountConfig = mailSettings.getAccountConfig(i);
			MailboxNode mailbox;
			mailbox = accountConfig.getDraftMailbox();
			if(mailbox != null) { mailbox.setType(MailboxNode.TYPE_DRAFTS); }
			mailbox = accountConfig.getSentMailbox();
			if(mailbox != null) { mailbox.setType(MailboxNode.TYPE_SENT); }
		}

        for(int i=0; i<accounts.length; i++) {
            reSortMailboxes(accounts[i].getRootMailbox());
        }
	}

    private static void clearMailboxTypes(MailboxNode currentNode) {
        if(currentNode == null) { return; }
        if(currentNode.getType() != MailboxNode.TYPE_INBOX) {
            currentNode.setType(MailboxNode.TYPE_NORMAL);
        }
        MailboxNode[] children = currentNode.getMailboxes();
        if(children != null && children.length > 0) {
            for(int i=0; i<children.length; i++) {
                clearMailboxTypes(children[i]);
            }
        }
    }
    
    private static void reSortMailboxes(MailboxNode currentNode) {
        if(currentNode == null) { return; }
        currentNode.reSort();
        MailboxNode[] children = currentNode.getMailboxes();
        if(children != null && children.length > 0) {
            for(int i=0; i<children.length; i++) {
                reSortMailboxes(children[i]);
            }
        }
    }
	
	/**
	 * First time initialization sequence, should only be invoked on
	 * application startup.
	 */
	public static void initialize() {
		MailManager mailManager = MailManager.getInstance();
		mailManager.refreshMailboxTypes();
	}
	
	/**
	 * Gets the mail manager instance.
	 * 
	 * @return The instance.
	 */
	public static synchronized MailManager getInstance() {
		if(instance == null) {
			instance = new MailManager();
		}
		return instance;
	}

    /**
     * This method should be invoked as soon as the application is completely
     * initialized, and the home screen has been displayed.  It is used to
     * trigger any operations that happen automatically after startup.
     */
    public synchronized void startupComplete() {
        // Make sure this method can only be called once
        if(startupComplete) { return; }
        else { startupComplete = true; }
        
        NetworkAccountNode[] networkAccounts = mailRootNode.getNetworkAccounts();
        for(int i=0; i<networkAccounts.length; i++) {
            AccountConfig accountConfig = networkAccounts[i].getAccountConfig();
            int setting = accountConfig.getRefreshOnStartup();

            boolean refreshTriggered = false;
            if(accountConfig.getRefreshOnStartup() >= AccountConfig.REFRESH_ON_STARTUP_STATUS) {
                //TODO: Consider handling folder refresh on missing-folder errors
                //TODO: Making the GUI always show a server-side value for message counts will clear up side-effects
                networkAccounts[i].refreshMailboxStatus();
                refreshTriggered = true;
            }
            if(setting == AccountConfig.REFRESH_ON_STARTUP_HEADERS) {
                networkAccounts[i].triggerAutomaticRefresh(true);
                refreshTriggered = true;
            }
            
            // If a refresh was not triggered on startup, but polling is
            // enabled, then make sure the polling thread is still started.
            if(!refreshTriggered && accountConfig.getRefreshFrequency() > 0) {
                networkAccounts[i].scheduleAutomaticRefresh();
            }
        }
    }
	
	/**
	 * Gets the mail root node.
	 * Also performs any necessary initialization on
	 * the first call.
	 * 
	 * @return Mail root node.
	 */
	public synchronized MailRootNode getMailRootNode() {
		return mailRootNode;
	}

	/**
	 * Gets the outbox node within the local account.
	 * 
	 * @return Outbox node.
	 */
	public OutboxMailboxNode getOutboxMailboxNode() {
		return outboxMailboxNode;
	}

	/**
	 * Clear the contents of the folder message cache.
	 */
	public void clearFolderMessageCache() {
	    folderMessageCache.clear();
	    if(outboxMailboxNode != null) {
	        outboxMailboxNode.clearMessages();
	    }
	    System.gc();
	}
	
	private void mailSettings_MailSettingsSaved(MailSettingsEvent e) {
        // This logic is rather crude, and will trigger a full refresh
        // under a wide variety of circumstances.  Its major intent is
        // to avoid a refresh in a few situations where a minor
        // configuration change affects future behavior and not
        // current state.
        
        boolean shouldRefreshAccounts =
            connectionListChanged(e) || accountChangeRequiringRefresh(e);
        
        // Trigger a refresh if appropriate
        if(shouldRefreshAccounts) {
            updateMailModelAccountList();
            refreshMailboxTypes();
            
            // Notify any listeners
            fireMailConfigurationChanged();
        }
        
        if((e.getGlobalChange() & GlobalConfig.CHANGE_TYPE_DATA) != 0) {
            mailRootNode.getLocalAccount().refreshMailboxes();
        }
	}

    private boolean connectionListChanged(MailSettingsEvent e) {
        int listChange = e.getListChange();
        return (listChange & MailSettingsEvent.LIST_CHANGED_ACCOUNT) != 0
                || (listChange & MailSettingsEvent.LIST_CHANGED_OUTGOING) != 0;
    }
	
    private boolean accountChangeRequiringRefresh(MailSettingsEvent e) {
        boolean refreshRequired = false;
        int num = mailSettings.getNumAccounts();
        for(int i=0; i<num; i++) {
            AccountConfig accountConfig = mailSettings.getAccountConfig(i);
            int accountChange = e.getConfigChange(accountConfig);
            if((accountChange & AccountConfig.CHANGE_TYPE_NAME) != 0
                    || (accountChange & AccountConfig.CHANGE_TYPE_MAILBOXES) != 0
                    || (accountChange & AccountConfig.CHANGE_TYPE_OUTGOING) != 0) {
                refreshRequired = true;
                break;
            }
            OutgoingConfig outgoingConfig = accountConfig.getOutgoingConfig();
            if(outgoingConfig != null
                    && (e.getConfigChange(outgoingConfig) & OutgoingConfig.CHANGE_TYPE_CONNECTION) != 0) {
                refreshRequired = true;
                break;
            }
        }
        return refreshRequired;
    }
    
    /**
     * Called when the account configuration has changed,
     * to cause the mail model to update its account list.
     */
    private synchronized void updateMailModelAccountList() {
        // Build the new account list from the configuration and
        // the existing account nodes.  This works by checking to see
        // if an account already exists, and only creating new nodes
        // for new accounts.
        NetworkAccountNode[] existingAccounts = mailRootNode.getNetworkAccounts();
        NetworkAccountNode[] newAccounts = getNewNetworkAccountNodes();

        // Remove and replace all account nodes from the root node.
        // This approach is taken to preserve any ordering which may
        // have been changed in the configuration.
        mailRootNode.removeAccounts(existingAccounts);
        mailRootNode.addAccounts(newAccounts);

        // Clear deleted accounts from the MailFactory and persistent storage
        clearDeletedAccounts(existingAccounts, newAccounts);

        // Get the newly updated account list, and determine whether
        // we need to update any mail senders.
        NetworkAccountNode[] updatedAccounts = mailRootNode.getNetworkAccounts();
        updateAccountMailSenders(updatedAccounts);

        //TODO: Clear deleted senders from the MailFactory
    }

    private NetworkAccountNode[] getNewNetworkAccountNodes() {
	    Vector newAccounts = new Vector();

	    int num = mailSettings.getNumAccounts();
	    for(int i=0; i<num; i++) {
	        AccountConfig accountConfig = mailSettings.getAccountConfig(i);
	        NetworkAccountNode existingAccountNode = mailRootNode.findAccountForConfig(accountConfig);

	        if(existingAccountNode != null) {
	            newAccounts.addElement(existingAccountNode);
	        }
	        else {
	            AccountNode newAccountNode = new NetworkAccountNode(
	                    new NetworkMailStoreServices(
	                            (NetworkMailStore) MailFactory.createMailStore(accountConfig),
	                            folderMessageCache));
	            newAccountNode.load();
	            newAccounts.addElement(newAccountNode);
	        }
	    }
	    NetworkAccountNode[] newAccountsArray = new NetworkAccountNode[newAccounts.size()];
	    newAccounts.copyInto(newAccountsArray);
	    return newAccountsArray;
	}
	
    private void clearDeletedAccounts(NetworkAccountNode[] existingAccounts, NetworkAccountNode[] newAccounts) {
        for(int i=0; i<existingAccounts.length; i++) {
            boolean accountDeleted = true;
            for(int j=0; j<newAccounts.length; j++) {
                NetworkAccountNode newAccount = newAccounts[j];
                if(newAccount == existingAccounts[i]) {
                    accountDeleted = false;
                    break;
                }
            }
            if(accountDeleted) {
                existingAccounts[i].removeSavedData();
                MailFactory.clearMailStore(existingAccounts[i].getAccountConfig());
            }
        }
    }

    private void updateAccountMailSenders(NetworkAccountNode[] updatedAccounts) {
        for(int i=0; i<updatedAccounts.length; i++) {
            NetworkAccountNode networkAccount = updatedAccounts[i];
            AbstractMailSender mailSender = networkAccount.getMailSender();
            OutgoingConfig outgoingConfig = networkAccount.getAccountConfig().getOutgoingConfig();
            if(outgoingConfig == null) {
                if(mailSender != null) {
                    mailSender.shutdown(false);
                }
                networkAccount.setMailSender(null);
            }
            else if((mailSender instanceof NetworkMailSender
                    &&((NetworkMailSender)mailSender).getOutgoingConfig() != outgoingConfig)) {
                mailSender.shutdown(false);
                networkAccount.setMailSender(MailFactory.createMailSender(networkAccount.getAccountConfig().getOutgoingConfig()));
            }
            else if(mailSender == null) {
                networkAccount.setMailSender(MailFactory.createMailSender(networkAccount.getAccountConfig().getOutgoingConfig()));
            }
        }
    }

	/**
	 * Handle connection state changes by updating the
	 * appropriate account nodes.
	 * 
	 * @param e Event data.
	 */
	private void mailConnectionManager_MailConnectionStateChanged(MailConnectionStateEvent e) {
	    if(e.getConnectionConfig() instanceof AccountConfig) {
    		// Find the account node associated with this event
    		AccountNode matchingAccount = mailRootNode.findAccountForConfig(
    		        (AccountConfig)e.getConnectionConfig());
    		
    		// Update account state
    		if(matchingAccount != null) {
    			int state = e.getState();
    			if(state == MailConnectionStateEvent.STATE_CONNECTED) {
    				matchingAccount.setStatus(AccountNode.STATUS_ONLINE);
    			}
    			else if(state == MailConnectionStateEvent.STATE_DISCONNECTED) {
    				matchingAccount.setStatus(AccountNode.STATUS_OFFLINE);
    			}
    		}
	    }
	}
	
	/**
     * Adds a <tt>MailManagerListener</tt> to the mail manager.
     * 
     * @param l The <tt>MailManagerListener</tt> to be added.
     */
    public void addMailManagerListener(MailManagerListener l) {
        listenerList.add(MailManagerListener.class, l);
    }

    /**
     * Removes a <tt>MailManagerListener</tt> from the mail manager.
     * 
     * @param l The <tt>MailManagerListener</tt> to be removed.
     */
    public void removeMailManagerListener(MailManagerListener l) {
        listenerList.remove(MailManagerListener.class, l);
    }
    
    /**
     * Returns an array of all <tt>MailManagerListener</tt>s
     * that have been added to this mail manager.
     * 
     * @return All the <tt>MailManagerListener</tt>s that have been added,
     * or an empty array if no listeners have been added.
     */
    public MailManagerListener[] getMailManagerListeners() {
        return (MailManagerListener[])listenerList.getListeners(MailManagerListener.class);
    }
    
    /**
     * Notifies all registered <tt>MailManagerListener</tt>s that
     * the mail system configuration has changed.
     */
    private void fireMailConfigurationChanged() {
        Object[] listeners = listenerList.getListeners(MailManagerListener.class);
        MailManagerEvent e = null;
        for(int i=0; i<listeners.length; i++) {
            if(e == null) {
                e = new MailManagerEvent(this);
            }
            ((MailManagerListener)listeners[i]).mailConfigurationChanged(e);
        }
    }
}
