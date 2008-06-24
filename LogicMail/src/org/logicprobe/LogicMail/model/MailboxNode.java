package org.logicprobe.LogicMail.model;

import java.util.Vector;

import org.logicprobe.LogicMail.mail.FolderTreeItem;
import org.logicprobe.LogicMail.util.EventListenerList;

/**
 * Mailbox node for the mail data model.
 * This node contains both <tt>MailboxNode</tt> and
 * <tt>MessageNode</tt> instances as its children.
 */
public class MailboxNode implements Node {
	private AccountNode parentAccount;
	private MailboxNode parentMailbox;
	private Vector mailboxes;
	private Vector messages;
	private EventListenerList listenerList = new EventListenerList();
	private int type;
	private FolderTreeItem folderTreeItem;
	
	public final static int TYPE_NORMAL = 0;
	public final static int TYPE_INBOX  = 1;
	public final static int TYPE_DRAFTS = 2;
	public final static int TYPE_SENT   = 3;
	public final static int TYPE_TRASH  = 4;

	MailboxNode(FolderTreeItem folderTreeItem, int type) {
		this.mailboxes = new Vector();
		this.messages = new Vector();
		this.folderTreeItem = folderTreeItem;
		this.type = type;
	}
	
	MailboxNode(FolderTreeItem folderTreeItem) {
		this(folderTreeItem, TYPE_NORMAL);
	}
	
	MailboxNode() {
		this(null, TYPE_NORMAL);
	}
	
	public void accept(NodeVisitor visitor) {
		visitor.visit(this);
	}

	/**
	 * Sets the account which this mailbox is contained within.
	 * 
	 * @param parentAccount The account.
	 */
	void setParentAccount(AccountNode parentAccount) {
		this.parentAccount = parentAccount;
	}
	
	/**
	 * Gets the account which contains this mailbox.
	 * 
	 * @return The account.
	 */
	public AccountNode getParentAccount() {
		return this.parentAccount;
	}
	
	/**
	 * Sets the mailbox which contains this mailbox.
	 * 
	 * @param parentMailbox The mailbox, or null if top-level.
	 */
	void setParentMailbox(MailboxNode parentMailbox) {
		this.parentMailbox = parentMailbox;
	}
	
	/**
	 * Gets the mailbox which contains this mailbox.
	 * 
	 * @return The mailbox, or null if this is a top-level mailbox.
	 */
	public MailboxNode getParentMailbox() {
		return this.parentMailbox;
	}
	
	/**
	 * Get the sub-level mailboxes contained under this mailbox.
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
	 * Get the messages contained within this mailbox.
	 * This method returns an array that is a shallow copy of the
	 * live message list.  It is primarily intended for use
	 * during initialization.
	 *  
	 * @return Mailbox nodes.
	 */
	public MessageNode[] getMessages() {
		MessageNode[] result;
		synchronized(messages) {
			int size = messages.size();
			result = new MessageNode[size];
			for(int i=0; i<size; i++) {
				result[i] = (MessageNode)messages.elementAt(i);
			}
		}
		return result;
	}
	
	/**
	 * Adds a mailbox to this mailbox.
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
	 * Removes a mailbox from this mailbox.
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
	 * Adds a message to this mailbox.
	 * 
	 * @param message The message to add.
	 */
	void addMessage(MessageNode message) {
		synchronized(messages) {
			if(!messages.contains(message)) {
				messages.addElement(message);
			}
		}
	}
	
	/**
	 * Removes a message from this mailbox.
	 * 
	 * @param message The message to remove
	 */
	void removeMessage(MessageNode message) {
		synchronized(messages) {
			if(messages.contains(message)) {
				messages.removeElement(message);
			}
		}
	}
	
	/**
	 * Gets the name of this mailbox.
	 * 
	 * @return The name.
	 */
	public String getName() {
		return this.folderTreeItem.getName();
	}

	/**
	 * Sets the type of this mailbox.
	 * 
	 * @param type The type.
	 */
	void setType(int type) {
		this.type = type;
	}
	
	/**
	 * Gets the type of this mailbox.
	 * 
	 * @return The type.
	 */
	public int getType() {
		return this.type;
	}

	/**
	 * Gets whether this folder can be selected.
	 * If a folder cannot be selected, then it cannot
	 * contain any messages.  However, it can contain
	 * other folders.
	 * 
	 * @return True if this folder can be selected.
	 */
    public boolean isSelectable() {
        return this.folderTreeItem.isSelectable();
    }
    
    /**
     * Gets the message count for this folder.
     * This should match the length of the array returned
     * by getMessages(), and is provided for convenience.
     * 
     * @return Message count.
     */
    public int getMessageCount() {
        return this.folderTreeItem.getMsgCount();
    }

    /**
     * Gets the unseen message count for this folder.
     * 
     * @return Unseen message count.
     */
    public int getUnseenMessageCount() {
        return this.folderTreeItem.getUnseenCount();
    }
	
	/**
     * Adds a <tt>MailboxNodeListener</tt> to the mailbox node.
     * 
     * @param l The <tt>MailboxNodeListener</tt> to be added.
     */
    public void addMailboxNodeListener(MailboxNodeListener l) {
        listenerList.add(MailboxNodeListener.class, l);
    }

    /**
     * Removes a <tt>MailboxNodeListener</tt> from the mailbox node.
     * 
     * @param l The <tt>MailboxNodeListener</tt> to be removed.
     */
    public void removeMailboxNodeListener(MailboxNodeListener l) {
        listenerList.remove(MailboxNodeListener.class, l);
    }
    
    /**
     * Returns an array of all <tt>MailboxNodeListener</tt>s
     * that have been added to this mailbox node.
     * 
     * @return All the <tt>MailboxNodeListener</tt>s that have been added,
     * or an empty array if no listeners have been added.
     */
    public MailboxNodeListener[] getMailboxNodeListeners() {
        return (MailboxNodeListener[])listenerList.getListeners(MailboxNodeListener.class);
    }
    
    /**
     * Notifies all registered <tt>MailboxNodeListener</tt>s that
     * the mailbox status has changed. 
     */
    void fireMailboxStatusChanged() {
        Object[] listeners = listenerList.getListeners(MailboxNodeListener.class);
        MailboxNodeEvent e = null;
        for(int i=0; i<listeners.length; i++) {
            if(e == null) {
                e = new MailboxNodeEvent(this);
            }
            ((MailboxNodeListener)listeners[i]).mailboxStatusChanged(e);
        }
    }
}
