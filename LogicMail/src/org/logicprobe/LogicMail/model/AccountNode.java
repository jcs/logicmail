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

import org.logicprobe.LogicMail.conf.AccountConfig;
import org.logicprobe.LogicMail.mail.AbstractMailStore;
import org.logicprobe.LogicMail.mail.FolderEvent;
import org.logicprobe.LogicMail.mail.FolderListener;
import org.logicprobe.LogicMail.mail.FolderMessagesEvent;
import org.logicprobe.LogicMail.mail.FolderTreeItem;
import org.logicprobe.LogicMail.mail.MailStoreListener;
import org.logicprobe.LogicMail.mail.MessageEvent;
import org.logicprobe.LogicMail.mail.MessageListener;
import org.logicprobe.LogicMail.mail.NetworkMailStore;
import org.logicprobe.LogicMail.message.FolderMessage;
import org.logicprobe.LogicMail.util.EventListenerList;

/**
 * Account node for the mail data model.
 * This node contains only the root <tt>MailboxNode</tt> instance.
 * Currently the type of mail store backing this node is
 * determined by the constructor that is called.
 * Eventually a more elegant approach will need to
 * be implemented.
 */
public class AccountNode implements Node {
	private AbstractMailStore mailStore;
	private MailRootNode parent;
	private MailboxNode rootMailbox;
	private Hashtable pathMailboxMap;
	private Object rootMailboxLock = new Object();
	private EventListenerList listenerList = new EventListenerList();
	private AccountConfig accountConfig;
	private int status;
	
	public final static int STATUS_LOCAL   = 0;
	public final static int STATUS_OFFLINE = 1;
	public final static int STATUS_ONLINE  = 2;
	
	/**
	 * Construct a new node for a network account.
	 * 
	 * @param accountConfig Account configuration.
	 */
	AccountNode(AbstractMailStore mailStore) {
		this.rootMailbox = null;
		this.pathMailboxMap = new Hashtable();
		
		this.mailStore = mailStore;
		
		// TODO: Have a better way to deal with network vs. local
		if(mailStore instanceof NetworkMailStore) {
			this.accountConfig = ((NetworkMailStore)mailStore).getAccountConfig();
			this.status = STATUS_OFFLINE;
		}
		else {
			this.status = STATUS_LOCAL;
		}
		
		this.mailStore.addMailStoreListener(new MailStoreListener() {
			public void folderTreeUpdated(FolderEvent e) {
				mailStore_FolderTreeUpdated(e);
			}
		});
		
		this.mailStore.addFolderListener(new FolderListener() {
			public void folderStatusChanged(FolderEvent e) {
				mailStore_FolderStatusChanged(e);
			}
			public void folderMessagesAvailable(FolderMessagesEvent e) {
				mailStore_FolderMessagesAvailable(e);
			}
		});
		
		this.mailStore.addMessageListener(new MessageListener() {
			public void messageAvailable(MessageEvent e) {
				mailStore_messageAvailable(e);
			}
			public void messageFlagsChanged(MessageEvent e) {
				mailStore_messageFlagsChanged(e);
			}
			public void messageDeleted(MessageEvent e) {
				mailStore_messageDeleted(e);
			}
			public void messageUndeleted(MessageEvent e) {
				mailStore_messageUndeleted(e);
			}
		});
		
		if(!mailStore.hasFolders()) {
			// Create the fake INBOX node for non-folder-capable mail stores
			this.rootMailbox = new MailboxNode(new FolderTreeItem("", "", ""), -1);
			MailboxNode inboxNode = new MailboxNode(new FolderTreeItem("INBOX", "INBOX", ""), MailboxNode.TYPE_INBOX);
			inboxNode.setParentAccount(this);
			this.rootMailbox.addMailbox(inboxNode);
			pathMailboxMap.put("INBOX", inboxNode);
		}
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
	 * Get the top-level mailbox contained within this account.
	 * This mailbox typically exists only for the purpose of
	 * containing other mailboxes, and is not normally shown
	 * to the user.
	 *  
	 * @return Root mailbox node.
	 */
	public MailboxNode getRootMailbox() {
		synchronized(rootMailboxLock) {
			return this.rootMailbox;
		}
	}
	
	/**
	 * Gets the name of this account.
	 * 
	 * @return The name.
	 */
	public String getName() {
		if(accountConfig != null) {
			return this.accountConfig.toString();
		}
		else {
			return "Local Folders";
		}
	}

	/**
	 * Gets the account configuration.
	 * 
	 * @return The account configuration, or null for local accounts.
	 */
	public AccountConfig getAccountConfig() {
		return this.accountConfig;
	}
	
	/**
	 * Gets the mail store associated with this account.
	 * 
	 * @return The mail store.
	 */
	AbstractMailStore getMailStore() {
		return this.mailStore;
	}
	
	/**
	 * Sets the status of this account.
	 * 
	 * @param status The status.
	 */
	void setStatus(int status) {
		if(this.status != status) {
			this.status = status;
			fireAccountStatusChanged(AccountNodeEvent.TYPE_CONNECTION);
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
	 * Gets whether this account supports undelete.
	 * 
	 * @return True if supported, false otherwise.
	 */
	public boolean hasUndelete() {
		return this.mailStore.hasUndelete();
	}
	
	/**
	 * Called to trigger a refresh of the mailboxes under
	 * this account.  Completion is signaled by an
	 * AccountStatusChanged event.
	 */
	public void refreshMailboxes() {
		if(mailStore.hasFolders()) {
			mailStore.requestFolderTree();
		}
	}
	
	/**
	 * Called to trigger a refresh of message count status
	 * for mailboxes under this account.  Completion is
	 * signaled by MailboxStatusChanged events on the
	 * updated mailboxes.
	 */
	public void refreshMailboxStatus() {
		MailboxNode mailbox;
		synchronized(rootMailboxLock) {
			mailbox = this.rootMailbox;
		}
		mailStore.requestFolderStatus(mailbox.getFolderTreeItem());
	}
	
	/**
	 * Handles folder tree updates.
	 * 
	 * @param e Event data.
	 */
	private void mailStore_FolderTreeUpdated(FolderEvent e) {
		FolderTreeItem rootFolder = e.getFolder();

		synchronized(rootMailboxLock) {
			Hashtable remainingMailboxMap = new Hashtable();
			if(rootMailbox != null) {
				// Disassemble the model tree into a flat collection of nodes
				Vector flatMailboxes = new Vector();
				populateFlatMailboxes(flatMailboxes, rootMailbox);
				rootMailbox = null;
				
				// Prune the collection to only include nodes that are still valid,
				// and make them reference the new FolderTreeItem objects.
				Hashtable folderPathMap = new Hashtable();
				populateFolderPathMap(folderPathMap, rootFolder);
				
				int size = flatMailboxes.size();
				for(int i=0; i<size; i++) {
					MailboxNode mailboxNode = (MailboxNode)flatMailboxes.elementAt(i);
					String path = mailboxNode.getFolderTreeItem().getPath();
					if(folderPathMap.containsKey(path)) {
						mailboxNode.setFolderTreeItem((FolderTreeItem)folderPathMap.get(path));
						remainingMailboxMap.put(path, mailboxNode);
					}
				}
			}
			
			// Build a new tree from the FolderTreeItem, using the collected
			// nodes where possible, and new nodes when necessary.
			this.pathMailboxMap.clear();
			this.rootMailbox = new MailboxNode(rootFolder, -1);
			populateMailboxNodes(rootFolder, rootMailbox, remainingMailboxMap);
		}
		
		fireAccountStatusChanged(AccountNodeEvent.TYPE_MAILBOX_TREE);
	}
	
	private void populateFlatMailboxes(Vector flatMailboxes, MailboxNode currentMailbox) {
		flatMailboxes.addElement(currentMailbox);
		MailboxNode[] childNodes = currentMailbox.getMailboxes();
		for(int i=0; i<childNodes.length; i++) {
			populateFlatMailboxes(flatMailboxes, childNodes[i]);
		}
		currentMailbox.clearMailboxes();
	}
	
	private void populateFolderPathMap(Hashtable folderPathMap, FolderTreeItem folderTreeItem) {
		if(folderTreeItem != null) {
			folderPathMap.put(folderTreeItem.getPath(), folderTreeItem);
			if(folderTreeItem.hasChildren()) {
				FolderTreeItem[] children = folderTreeItem.children();
				for(int i=0; i<children.length; i++) {
					populateFolderPathMap(folderPathMap, children[i]);
				}
			}
		}
	}
	
	private void populateMailboxNodes(FolderTreeItem folderTreeItem, MailboxNode currentMailbox, Hashtable remainingMailboxMap) {
		pathMailboxMap.put(folderTreeItem.getPath(), currentMailbox);
		if(folderTreeItem.hasChildren()) {
			FolderTreeItem[] folderTreeItemChildren = folderTreeItem.children();
			for(int i=0; i<folderTreeItemChildren.length; i++) {
				MailboxNode childMailbox;
				if(remainingMailboxMap.containsKey(folderTreeItemChildren[i].getPath())) {
					childMailbox = (MailboxNode)remainingMailboxMap.get(folderTreeItemChildren[i].getPath());
				}
				else {
					childMailbox = new MailboxNode(folderTreeItemChildren[i], getMailboxType(folderTreeItemChildren[i]));
					childMailbox.setParentAccount(this);
				}
				populateMailboxNodes(folderTreeItemChildren[i], childMailbox, remainingMailboxMap);
				currentMailbox.addMailbox(childMailbox);
			}
		}
	}
	
	/**
	 * Handles folder status changes.
	 * 
	 * @param e Event data.
	 */
	private void mailStore_FolderStatusChanged(FolderEvent e) {
		updateMailboxStatus(e.getFolder());
	}
	
	/**
	 * Recursively update mailbox status.
	 * 
	 * @param currentFolder Folder item to start from.
	 */
	private void updateMailboxStatus(FolderTreeItem currentFolder) {
		MailboxNode mailboxNode = (MailboxNode)pathMailboxMap.get(currentFolder.getPath());
		if(mailboxNode != null) {
			FolderTreeItem mailboxFolder = mailboxNode.getFolderTreeItem();
			mailboxFolder.setMsgCount(currentFolder.getMsgCount());
			mailboxFolder.setUnseenCount(currentFolder.getUnseenCount());
			mailboxNode.fireMailboxStatusChanged(MailboxNodeEvent.TYPE_STATUS, null);
		}
		if(currentFolder.hasChildren()) {
			FolderTreeItem[] children = currentFolder.children();
			for(int i=0; i<children.length; i++) {
				updateMailboxStatus(children[i]);
			}
		}
	}
	
	/**
	 * Handles folder messages becoming available.
	 * 
	 * @param e Event data.
	 */
	private void mailStore_FolderMessagesAvailable(FolderMessagesEvent e) {
		// Find the MailboxNode that this event applies to.
		// If none apply, then shortcut out of here.
		if(!pathMailboxMap.containsKey(e.getFolder().getPath())) {
			return;
		}
		MailboxNode mailboxNode = (MailboxNode)pathMailboxMap.get(e.getFolder().getPath());
		
		// Determine what MessageNodes need to be created, and add them.
		FolderMessage[] folderMessages = e.getMessages();
		Vector addedMessages = new Vector();
		for(int i=0; i<folderMessages.length; i++) {
			if(!mailboxNode.containsMessage(folderMessages[i].getIndex())) {
				addedMessages.addElement(new MessageNode(folderMessages[i]));
			}
		}
		MessageNode[] addedMessagesArray = new MessageNode[addedMessages.size()];
		addedMessages.copyInto(addedMessagesArray);
		mailboxNode.addMessages(addedMessagesArray);
	}

	/**
	 * Handles a message being loaded.
	 * 
	 * @param e Event data.
	 */
	public void mailStore_messageAvailable(MessageEvent e) {
		MessageNode messageNode = findMessageForEvent(e);
		if(messageNode != null) {
			messageNode.setMessage(e.getMessage());
		}
	}
	
	/**
	 * Handles a message flags changing.
	 * 
	 * @param e Event data.
	 */
	public void mailStore_messageFlagsChanged(MessageEvent e) {
		MessageNode messageNode = findMessageForEvent(e);
		if(messageNode != null) {
			messageNode.fireMessageStatusChanged(MessageNodeEvent.TYPE_FLAGS);
		}
	}
	
	/**
	 * Handles a message being deleted.
	 * 
	 * @param e Event data.
	 */
	public void mailStore_messageDeleted(MessageEvent e) {
		MessageNode messageNode = findMessageForEvent(e);
		if(messageNode != null) {
			messageNode.fireMessageStatusChanged(MessageNodeEvent.TYPE_FLAGS);
		}
	}
	
	/**
	 * Handles a message being undeleted.
	 * 
	 * @param e Event data.
	 */
	public void mailStore_messageUndeleted(MessageEvent e) {
		MessageNode messageNode = findMessageForEvent(e);
		if(messageNode != null) {
			messageNode.fireMessageStatusChanged(MessageNodeEvent.TYPE_FLAGS);
		}
	}
	
	/**
	 * Finds the message node matching a particular event.
	 * 
	 * @param e Event data.
	 * @return Message node, or null if none was found.
	 */
	private MessageNode findMessageForEvent(MessageEvent e) {
		MailboxNode mailboxNode = (MailboxNode)pathMailboxMap.get(e.getFolder().getPath());
		if(mailboxNode != null) {
			// Change this to use the UID once implemented
			return mailboxNode.getMessage(e.getFolderMessage().getIndex());
		}
		else {
			return null;
		}
	}
	
	/**
	 * Attempts to determine the folder type based on its name,
	 * and any configuration options.
	 * @param folderTreeItem Source folder tree item.
	 * @return Mailbox type
	 */
	private int getMailboxType(FolderTreeItem folderTreeItem) {
		if(folderTreeItem.getPath().equalsIgnoreCase("INBOX")) {
			return MailboxNode.TYPE_INBOX;
		}
		else {
			return MailboxNode.TYPE_NORMAL;
		}
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
     * 
     * @param type Event type.
     */
    protected void fireAccountStatusChanged(int type) {
        Object[] listeners = listenerList.getListeners(AccountNodeListener.class);
        AccountNodeEvent e = null;
        for(int i=0; i<listeners.length; i++) {
            if(e == null) {
                e = new AccountNodeEvent(this, type);
            }
            ((AccountNodeListener)listeners[i]).accountStatusChanged(e);
        }
    }
}
