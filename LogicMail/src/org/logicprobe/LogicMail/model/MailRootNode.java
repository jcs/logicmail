package org.logicprobe.LogicMail.model;

import java.util.Vector;

/**
 * Root node for the mail data model, implemented as a singleton.
 * This node contains only <tt>AccountNode</tt> instances
 * as its children.
 */
public class MailRootNode implements Node {
	Vector accounts;
	
	public MailRootNode() {
		this.accounts = new Vector();
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
