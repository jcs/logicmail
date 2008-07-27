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

import java.util.Hashtable;
import java.util.Vector;

import org.logicprobe.LogicMail.conf.MailSettings;
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
	private Hashtable messageMap;
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
		this.messageMap = new Hashtable();
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
	 * Sets the <tt>FolderTreeItem</tt> associated with this mailbox.
	 * This method exists to allow regenerating the folder tree
	 * without having to recreate the <tt>MailboxNode</tt> instances
	 * that maintain cache data for it.
	 * 
	 * @param folderTreeItem Folder tree item.
	 */
	void setFolderTreeItem(FolderTreeItem folderTreeItem) {
		this.folderTreeItem = folderTreeItem;
	}
	
	/**
	 * Gets the <tt>FolderTreeItem</tt> associated with this mailbox.
	 * This method exists to support regeneration of the folder tree,
	 * and the more specific public methods should be used for
	 * all other purposes.
	 * 
	 * @return Folder tree item.
	 */
	FolderTreeItem getFolderTreeItem() {
		return this.folderTreeItem;
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
				mailbox.setParentMailbox(this);
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
				mailbox.setParentMailbox(null);
			}
		}
	}
	
	/**
	 * Removes all mailboxes from this mailbox.
	 */
	void clearMailboxes() {
		synchronized(mailboxes) {
			mailboxes.removeAllElements();
		}
	}
	
	/**
	 * Adds a message to this mailbox.
	 * 
	 * @param message The message to add.
	 */
	void addMessage(MessageNode message) {
		synchronized(messages) {
			addMessageImpl(message);
		}
		fireMailboxStatusChanged(MailboxNodeEvent.TYPE_NEW_MESSAGES, new MessageNode[] { message });
	}
	
	/**
	 * Adds messages to this mailbox.
	 * 
	 * @param messages The messages to add.
	 */
	void addMessages(MessageNode[] messages) {
		synchronized(messages) {
			for(int i=0; i<messages.length; i++) {
				addMessageImpl(messages[i]);
			}
		}
		fireMailboxStatusChanged(MailboxNodeEvent.TYPE_NEW_MESSAGES, messages);
	}
	
	/**
	 * Adds a message to this mailbox using a sorted insertion
	 * based on message id.  Since this is intended to be used
	 * from within other visible methods, it should only be
	 * called from within a "synchronized(messages)" block.
	 *  
	 * @param message The message to add.
	 */
	private void addMessageImpl(MessageNode message) {
		if(!messageMap.containsKey(new Integer(message.getId()))) {
			if(messages.size() > 0) {
				int msgId = message.getId();
				int index = messages.size();
				MessageNode lastMessage = (MessageNode)messages.lastElement();
				while(index > 0 && lastMessage.getId() > msgId) {
					index--;
					if(index > 0) { lastMessage = (MessageNode)messages.elementAt(index - 1); }
				}
				
				messages.insertElementAt(message, index);
			}
			else {
				messages.addElement(message);
			}
			message.setParent(this);
			messageMap.put(new Integer(message.getId()), message);
		}
	}
	
	/**
	 * Removes a message from this mailbox.
	 * 
	 * @param message The message to remove
	 */
	void removeMessage(MessageNode message) {
		synchronized(messages) {
			if(messageMap.containsKey(new Integer(message.getId()))) {
				messages.removeElement(message);
				messageMap.remove(new Integer(message.getId()));
			}
		}
	}

	/**
	 * Removes all messages from this mailbox.
	 */
	void clearMessages() {
		synchronized(messages) {
			messages.removeAllElements();
			messageMap.clear();
		}
	}
	
	/**
	 * Gets whether this mailbox contains a particular message.
	 * 
	 * @param id The message ID to look for.
	 * @return True if it exists, false otherwise.
	 */
	boolean containsMessage(int id) {
		synchronized(messages) {
			return messageMap.containsKey(new Integer(id));
		}
	}
	
	/**
	 * Gets a particular message.
	 * 
	 * @param id The message ID to look for.
	 * @return The message if it exists, null otherwise.
	 */
	MessageNode getMessage(int id) {
		synchronized(messages) {
			MessageNode message = (MessageNode)messageMap.get(new Integer(id));
			return message;
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
	 * Gets the path of this mailbox.
	 * <p>Once serialization and unique IDs are fully implemented, this should
	 * be removed.  It exposes too much implementation detail to the upper
	 * layers of the system.
	 * 
	 * @return The path.
	 */
	public String getPath() {
		return this.folderTreeItem.getPath();
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
     * Called to refresh the message list contained within this mailbox.
     * This currently replaces the existing message list with the N most
     * recent messages, as defined in the configuration options.
     * 
     * Eventually, this needs to replaced or supplemented with a more
     * sophisticated version that can selectively load additional
     * messages without replacing the existing ones. 
     */
    public void refreshMessages() {
    	parentAccount.getMailStore().requestFolderMessagesRecent(
    			this.folderTreeItem,
    			MailSettings.getInstance().getGlobalConfig().getRetMsgCount());
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
     * 
     * @param type The type of this event.
     * @param affectedMessages The affected messages. 
     */
    void fireMailboxStatusChanged(int type, MessageNode[] affectedMessages) {
        Object[] listeners = listenerList.getListeners(MailboxNodeListener.class);
        MailboxNodeEvent e = null;
        for(int i=0; i<listeners.length; i++) {
            if(e == null) {
                e = new MailboxNodeEvent(this, type, affectedMessages);
            }
            ((MailboxNodeListener)listeners[i]).mailboxStatusChanged(e);
        }
    }
}
