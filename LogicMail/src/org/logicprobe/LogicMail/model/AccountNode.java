package org.logicprobe.LogicMail.model;

import java.util.Vector;

import org.logicprobe.LogicMail.conf.AccountConfig;
import org.logicprobe.LogicMail.util.EventListenerList;

/**
 * Account node for the mail data model.
 * This node contains only <tt>MailboxNode</tt> instances
 * as its children.
 */
public class AccountNode implements Node {
	private MailRootNode parent;
	private Vector mailboxes;
	private EventListenerList listenerList = new EventListenerList();
	private AccountConfig accountConfig;
	private int status;
	
	public final static int STATUS_LOCAL   = 0;
	public final static int STATUS_OFFLINE = 1;
	public final static int STATUS_ONLINE  = 2;
	
	AccountNode(AccountConfig accountConfig) {
		this.accountConfig = accountConfig;
		this.mailboxes = new Vector();
		this.status = STATUS_OFFLINE; //quick for now
	}
	
	public void accept(NodeVisitor visitor) {
		visitor.visit(this);
	}

	/**
	 * Sets the root node which is the parent of this account.
	 * 
	 * @param parent The root node.
	 */
	void setParent(MailRootNode parent) {
		this.parent = parent;
	}
	
	/**
	 * Gets the root node which is the parent of this account.
	 * 
	 * @return The root node.
	 */
	public MailRootNode getParent() {
		return this.parent;
	}
	
	/**
	 * Get the top-level mailboxes contained within this account.
	 * This method returns an array that is a shallow copy of the
	 * live mailbox list.  It is primarily intended for use
	 * during initialization.
	 *  
	 * @return Mailbox nodes.
	 */
	public MailboxNode[] getMailboxes() {
		MailboxNode[] result;
		synchronized(mailboxes) {
			int size = mailboxes.size();
			result = new MailboxNode[size];
			for(int i=0; i<size; i++) {
				result[i] = (MailboxNode)mailboxes.elementAt(i);
			}
		}
		return result;
	}
	
	/**
	 * Adds a mailbox to this account.
	 * 
	 * @param mailbox The mailbox to add.
	 */
	void addMailbox(MailboxNode mailbox) {
		synchronized(mailboxes) {
			if(!mailboxes.contains(mailbox)) {
				mailboxes.addElement(mailbox);
			}
		}
	}
	
	/**
	 * Removes a mailbox from this account.
	 * 
	 * @param mailbox The mailbox to remove.
	 */
	void removeMailbox(MailboxNode mailbox) {
		synchronized(mailboxes) {
			if(mailboxes.contains(mailbox)) {
				mailboxes.removeElement(mailbox);
			}
		}
	}
	
	/**
	 * Gets the name of this account.
	 * 
	 * @return The name.
	 */
	public String getName() {
		return this.accountConfig.toString();
	}

	/**
	 * Gets the account configuration.
	 * 
	 * @return The account configuration.
	 */
	AccountConfig getAccountConfig() {
		return this.accountConfig;
	}
	
	/**
	 * Sets the status of this account.
	 * 
	 * @param status The status.
	 */
	void setStatus(int status) {
		if(this.status != status) {
			this.status = status;
			fireAccountStatusChanged();
		}
	}
	
	/**
	 * Gets the status of this account.
	 * 
	 * @return The status.
	 */
	public int getStatus() {
		return this.status;
	}
	
	/**
     * Adds a <tt>AccountNodeListener</tt> to the account node.
     * 
     * @param l The <tt>AccountNodeListener</tt> to be added.
     */
    public void addAccountNodeListener(AccountNodeListener l) {
        listenerList.add(AccountNodeListener.class, l);
    }

    /**
     * Removes a <tt>AccountNodeListener</tt> from the account node.
     * 
     * @param l The <tt>AccountNodeListener</tt> to be removed.
     */
    public void removeAccountNodeListener(AccountNodeListener l) {
        listenerList.remove(AccountNodeListener.class, l);
    }
    
    /**
     * Returns an array of all <tt>AccountNodeListener</tt>s
     * that have been added to this account node.
     * 
     * @return All the <tt>AccountNodeListener</tt>s that have been added,
     * or an empty array if no listeners have been added.
     */
    public AccountNodeListener[] getAccountNodeListeners() {
        return (AccountNodeListener[])listenerList.getListeners(AccountNodeListener.class);
    }
    
    /**
     * Notifies all registered <tt>AccountNodeListener</tt>s that
     * the account status has changed. 
     */
    protected void fireAccountStatusChanged() {
        Object[] listeners = listenerList.getListeners(AccountNodeListener.class);
        AccountNodeEvent e = null;
        for(int i=0; i<listeners.length; i++) {
            if(e == null) {
                e = new AccountNodeEvent(this);
            }
            ((AccountNodeListener)listeners[i]).accountStatusChanged(e);
        }
    }
}
