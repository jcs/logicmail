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
import org.logicprobe.LogicMail.conf.MailSettings;
import org.logicprobe.LogicMail.conf.OutgoingConfig;
import org.logicprobe.LogicMail.mail.AbstractMailSender;
import org.logicprobe.LogicMail.mail.MailConnectionListener;
import org.logicprobe.LogicMail.mail.MailConnectionLoginEvent;
import org.logicprobe.LogicMail.mail.MailConnectionManager;
import org.logicprobe.LogicMail.mail.MailConnectionStateEvent;
import org.logicprobe.LogicMail.mail.MailConnectionStatusEvent;
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
	private EventListenerList listenerList = new EventListenerList();
	private MailRootNode mailRootNode;
	private MailSettings mailSettings;
	
	/**
	 * Constructor.
	 */
	private MailManager() {
		mailSettings = MailSettings.getInstance();
		mailRootNode = new MailRootNode();
		// Make sure the initial configuration is loaded
		configurationChanged();
		
		MailConnectionManager.getInstance().addMailConnectionListener(new MailConnectionListener() {
			public void mailConnectionStateChanged(MailConnectionStateEvent e) {
				mailConnectionManager_MailConnectionStateChanged(e);
			}
			public void mailConnectionStatus(MailConnectionStatusEvent e) { }
			public void mailConnectionError(MailConnectionStatusEvent e) { }
			public void mailConnectionLogin(MailConnectionLoginEvent e) { }
		});
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
	 * Called when the account configuration has changed,
	 * to cause the mail model to update its account list.
	 */
	public synchronized void configurationChanged() {
		// Build the new account list from the configuration and
		// the existing account nodes.  This works by checking to see
		// if an account already exists, and only creating new nodes
		// for new accounts.
		AccountNode[] existingAccounts = mailRootNode.getAccounts();
		int num = mailSettings.getNumAccounts();
		Vector newAccounts = new Vector();
		boolean accountExists;
		for(int i=0; i<num; i++) {
			AccountConfig accountConfig = mailSettings.getAccountConfig(i);
			accountExists = false;
			for(int j=0; j<existingAccounts.length; j++) {
				if(accountConfig == existingAccounts[j].getAccountConfig()) {
					accountExists = true;
					newAccounts.addElement(existingAccounts[j]);
					break;
				}
			}
			if(!accountExists) {
				AccountNode newAccountNode = new AccountNode(new NetworkMailStore(accountConfig));
				newAccounts.addElement(newAccountNode);
			}
		}

		// Remove and replace all account nodes from the root node.
		// This approach is taken to preserve any ordering which may
		// have been changed in the configuration.
		for(int i=0; i<existingAccounts.length; i++) {
			mailRootNode.removeAccount(existingAccounts[i]);
		}
		num = newAccounts.size();
		for(int i=0; i<num; i++) {
			mailRootNode.addAccount((AccountNode)newAccounts.elementAt(i));
		}
		
		// Get the newly updated account list, and determine whether
		// we need to update any mail senders.
		existingAccounts = mailRootNode.getAccounts();
		for(int i=0; i<existingAccounts.length; i++) {
			AbstractMailSender mailSender = existingAccounts[i].getMailSender();
			OutgoingConfig outgoingConfig = existingAccounts[i].getAccountConfig().getOutgoingConfig();
			if(outgoingConfig == null) {
				if(mailSender != null) {
					mailSender.shutdown(false);
				}
				existingAccounts[i].setMailSender(null);
			}
			else if(mailSender instanceof NetworkMailSender &&
			   ((NetworkMailSender)mailSender).getOutgoingConfig() != outgoingConfig) {
				mailSender.shutdown(false);
				existingAccounts[i].setMailSender(new NetworkMailSender(existingAccounts[i].getAccountConfig()));
			}
		}
		
		// Notify any listeners
		fireMailConfigurationChanged();
	}

	/**
	 * Handle connection state changes by updating the
	 * appropriate account nodes.
	 * 
	 * @param e Event data.
	 */
	private void mailConnectionManager_MailConnectionStateChanged(MailConnectionStateEvent e) {
		// Find the account node associated with this event
		AccountNode[] accounts = mailRootNode.getAccounts();
		AccountNode matchingAccount = null;
		for(int i=0; i<accounts.length; i++) {
			if(accounts[i].getAccountConfig().equals(e.getConnectionConfig())) {
				matchingAccount = accounts[i];
				break;
			}
		}
		
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
