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
import org.logicprobe.LogicMail.mail.LocalMailStore;
import org.logicprobe.LogicMail.mail.MailFactory;

/**
 * Root node for the mail data model, implemented as a singleton.
 * This node contains only <tt>AccountNode</tt> instances
 * as its children.
 */
public class MailRootNode implements Node {
    private LocalAccountNode localAccountNode;
    private final Vector networkAccounts;
    private AccountNode[] accountsArray;
    private NetworkAccountNode[] networkAccountsArray;
    private final Object accountsLock = new Object();

    public MailRootNode() {
        this.networkAccounts = new Vector();

        // Add the local mail store account
        localAccountNode = new LocalAccountNode(new LocalMailStoreServices(
                (LocalMailStore) MailFactory.createLocalMailStore()));
        localAccountNode.load();
    }

    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    /**
     * Get the accounts contained within the mail data model.
     * This method returns an array that is a shallow copy of the
     * live accounts list.  Since multiple calls to this method
     * may return the same instance of that array, it should
     * not be modified by callers.
     *  
     * @return Account nodes.
     */
    public AccountNode[] getAccounts() {
        // Since this method is used quite frequently, a reference to the
        // temporary snapshot array is kept.  It is only recreated if the
        // accounts vector is modified.
        synchronized(accountsLock) {
            if(accountsArray == null) {
                int size = networkAccounts.size();
                accountsArray = new AccountNode[size + 1];
                accountsArray[0] = localAccountNode;
                for(int i=0; i<size; i++) {
                    accountsArray[i + 1] = (AccountNode)networkAccounts.elementAt(i);
                }
            }
        }
        return accountsArray;
    }

    /**
     * Get the network accounts contained within the mail data model.
     * This method returns an array that is a shallow copy of the
     * live accounts list.  Since multiple calls to this method
     * may return the same instance of that array, it should
     * not be modified by callers.
     *  
     * @return Network account nodes.
     */
    public NetworkAccountNode[] getNetworkAccounts() {
        // Since this method is used quite frequently, a reference to the
        // temporary snapshot array is kept.  It is only recreated if the
        // accounts vector is modified.
        synchronized(accountsLock) {
            if(networkAccountsArray == null) {
                int size = networkAccounts.size();
                networkAccountsArray = new NetworkAccountNode[size];
                networkAccounts.copyInto(networkAccountsArray);
            }
        }
        return networkAccountsArray;
    }

    /**
     * Find the account node matching the provided account configuration.
     * This is a convenience method for a relatively common operation.
     *
     * @param accountConfig the account configuration
     * @return the network account node, or null if none found
     */
    public NetworkAccountNode findAccountForConfig(AccountConfig accountConfig) {
        NetworkAccountNode[] networkAccounts = getNetworkAccounts();
        for(int i=0; i<networkAccounts.length; i++) {
            if(accountConfig.equals(networkAccounts[i].getAccountConfig())) {
                return networkAccounts[i];
            }
        }
        return null;
    }

    /**
     * Gets the account node representing the local mail folders.
     * 
     * @return Local account node.
     */
    public LocalAccountNode getLocalAccount() {
        return localAccountNode;
    }

    /**
     * Adds a network account to the mail data model.
     * The account is appended to the end of the
     * live accounts list.
     * 
     * @param account The account to add.
     */
    void addAccount(NetworkAccountNode account) {
        synchronized(accountsLock) {
            if(!networkAccounts.contains(account)) {
                networkAccounts.addElement(account);
                accountsArray = null;
                networkAccountsArray = null;
            }
        }
    }

    /**
     * Adds a collection of network account to the mail data model.
     *
     * @param accounts The accounts to add.
     */
    void addAccounts(NetworkAccountNode[] accounts) {
        synchronized(accountsLock) {
            for(int i=0; i<accounts.length; i++) {
                addAccount(accounts[i]);
            }
        }
    }

    /**
     * Removes a network account from the mail data model.
     * 
     * @param account The account to remove.
     */
    void removeAccount(NetworkAccountNode account) {
        synchronized(accountsLock) {
            if(networkAccounts.contains(account)) {
                networkAccounts.removeElement(account);
                accountsArray = null;
                networkAccountsArray = null;
            }
        }
    }

    /**
     * Removes a collection of network accounts from the mail data model.
     *
     * @param accounts The accounts to remove.
     */
    void removeAccounts(NetworkAccountNode[] accounts) {
        synchronized(accountsLock) {
            for(int i=0; i<accounts.length; i++) {
                removeAccount(accounts[i]);
            }
        }
    }
}
