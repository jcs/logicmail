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

import org.logicprobe.LogicMail.mail.MailFactory;

/**
 * Root node for the mail data model, implemented as a singleton.
 * This node contains only <tt>AccountNode</tt> instances
 * as its children.
 */
public class MailRootNode implements Node {
	private Vector accounts;
	private AccountNode localAccountNode;
	
	public MailRootNode() {
		this.accounts = new Vector();

		// Add the local mail store
		localAccountNode = new AccountNode(MailFactory.createLocalMailStore());
		accounts.addElement(localAccountNode);
	}
	
	public void accept(NodeVisitor visitor) {
		visitor.visit(this);
	}

	/**
	 * Get the accounts contained within the mail data model.
	 * This method returns an array that is a shallow copy of the
	 * live accounts list.  It is primarily intended for use
	 * during initialization.
	 *  
	 * @return Account nodes.
	 */
	public AccountNode[] getAccounts() {
		AccountNode[] result;
		synchronized(accounts) {
			int size = accounts.size();
			result = new AccountNode[size];
			for(int i=0; i<size; i++) {
				result[i] = (AccountNode)accounts.elementAt(i);
			}
		}
		return result;
	}
	
	/**
	 * Gets the account node representing the local mail folders.
	 * 
	 * @return Local account node.
	 */
	public AccountNode getLocalAccount() {
		return localAccountNode;
	}
	
	/**
	 * Adds an account to the mail data model.
	 * The account is appended to the end of the
	 * live accounts list.
	 * 
	 * @param account The account to add.
	 */
	void addAccount(AccountNode account) {
		synchronized(accounts) {
			if(!accounts.contains(account)) {
				accounts.addElement(account);
			}
		}
	}
	
	/**
	 * Removes an account from the mail data mode.
	 * 
	 * @param account The account to remove
	 */
	void removeAccount(AccountNode account) {
		synchronized(accounts) {
			if(accounts.contains(account)) {
				accounts.removeElement(account);
			}
		}
	}
	
	/**
	 * Gets the name of the root node.
	 * Since the root node is a special case and is
	 * not intended to ever be displayed by a user
	 * interface, this method returns null.
	 * 
	 * @return Null
	 */
	public String getName() {
		return null;
	}
}
