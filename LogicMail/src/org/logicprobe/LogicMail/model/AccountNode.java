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

import org.logicprobe.LogicMail.mail.AbstractMailStore;
import org.logicprobe.LogicMail.mail.FolderEvent;
import org.logicprobe.LogicMail.mail.FolderListener;
import org.logicprobe.LogicMail.mail.FolderMessagesEvent;
import org.logicprobe.LogicMail.mail.FolderTreeItem;
import org.logicprobe.LogicMail.mail.MailStoreListener;
import org.logicprobe.LogicMail.mail.MessageEvent;
import org.logicprobe.LogicMail.mail.MessageListener;
import org.logicprobe.LogicMail.mail.MessageToken;
import org.logicprobe.LogicMail.util.EventListenerList;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Account node for the mail data model.
 * This node contains the root <code>MailboxNode</code> instance and delegates
 * events from the underlying mail store to the appropriate
 * <code>MailboxNode</code> or <code>MessageNode</code>.
 */
public abstract class AccountNode implements Node {
    public final static int STATUS_LOCAL = 0;
    public final static int STATUS_OFFLINE = 1;
    public final static int STATUS_ONLINE = 2;
    
    private final AbstractMailStore mailStore;
    private MailRootNode parent;
    private MailboxNode rootMailbox;
    private final Hashtable pathMailboxMap;
    private final Object rootMailboxLock = new Object();
    private final EventListenerList listenerList = new EventListenerList();
    
    protected int status;
    
    /**
     * Construct a new node for a network account.
     *
     * @param accountConfig Account configuration.
     */
    protected AccountNode(AbstractMailStore mailStore) {
        this.rootMailbox = null;
        this.pathMailboxMap = new Hashtable();

        this.mailStore = mailStore;

        addMailStoreListeners();
    }

    private void addMailStoreListeners() {
        this.mailStore.addMailStoreListener(new MailStoreListener() {
            public void folderTreeUpdated(FolderEvent e) {
                mailStoreFolderTreeUpdated(e);
            }
        });

        this.mailStore.addFolderListener(new FolderListener() {
            public void folderStatusChanged(FolderEvent e) {
                MailboxNode mailboxNode = getMailboxNodeForEvent(e);
                mailboxNode.mailStoreFolderStatusChanged(e);
            }

            public void folderMessagesAvailable(FolderMessagesEvent e) {
                MailboxNode mailboxNode = getMailboxNodeForEvent(e);
                mailboxNode.mailStoreFolderMessagesAvailable(e);
            }

            public void folderExpunged(FolderEvent e) {
                MailboxNode mailboxNode = getMailboxNodeForEvent(e);
                mailboxNode.mailStoreFolderExpunged(e);
            }
        });

        this.mailStore.addMessageListener(new MessageListener() {
            public void messageAvailable(MessageEvent e) {
                MailboxNode mailboxNode = getMailboxNodeForEvent(e);
                if(mailboxNode != null) {
                    mailboxNode.mailStoreMessageAvailable(e);
                }
            }

            public void messageFlagsChanged(MessageEvent e) {
                MailboxNode mailboxNode = getMailboxNodeForEvent(e);
                if(mailboxNode != null) {
                    mailboxNode.mailStoreMessageFlagsChanged(e);
                }
            }

            public void messageDeleted(MessageEvent e) {
                MailboxNode mailboxNode = getMailboxNodeForEvent(e);
                if(mailboxNode != null) {
                    mailboxNode.mailStoreMessageFlagsChanged(e);
                }
            }

            public void messageUndeleted(MessageEvent e) {
                MailboxNode mailboxNode = getMailboxNodeForEvent(e);
                if(mailboxNode != null) {
                    mailboxNode.mailStoreMessageFlagsChanged(e);
                }
            }
        });
    }
    
    private MailboxNode getMailboxNodeForEvent(FolderEvent e) {
        MailboxNode mailboxNode = (MailboxNode) pathMailboxMap.get(e.getFolder().getPath());
        return mailboxNode;
    }
    
    private MailboxNode getMailboxNodeForEvent(MessageEvent e) {
        MessageToken messageToken = e.getMessageToken();
        Enumeration en = pathMailboxMap.elements();
        while(en.hasMoreElements()) {
            MailboxNode currentMailbox = (MailboxNode)en.nextElement();
            if(messageToken.containedWithin(currentMailbox.getFolderTreeItem())) {
                return currentMailbox;
            }
        }
        return null;
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
        synchronized (rootMailboxLock) {
            return this.rootMailbox;
        }
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
        if (this.status != status) {
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
     * Gets whether this account supports folders.
     * If folders are not supported, then this account will automatically
     * present a single "INBOX" folder.  However, no other folder-related
     * operations will have any relevance.
     *
     * @return True if supported, false otherwise.
     */
    public boolean hasFolders() {
        return this.mailStore.hasFolders();
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
     * Gets whether this account supports expunging deleted messages.
     *
     * @return True if supported, false otherwise.
     */
    public boolean hasExpunge() {
        return this.mailStore.hasExpunge();
    }

    /**
     * Called to trigger a refresh of the mailboxes under
     * this account.  Completion is signaled by an
     * AccountStatusChanged event.
     */
    public void refreshMailboxes() {
        if (mailStore.hasFolders()) {
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
        int size = pathMailboxMap.size();
        FolderTreeItem[] folders = new FolderTreeItem[size];
        Enumeration e = pathMailboxMap.keys();

        for (int i = 0; i < size; i++) {
            folders[i] = ((MailboxNode) pathMailboxMap.get(e.nextElement())).getFolderTreeItem();
        }

        mailStore.requestFolderStatus(folders);
    }

    /**
     * Handles folder tree updates.
     *
     * @param e Event data.
     */
    private void mailStoreFolderTreeUpdated(FolderEvent e) {
        FolderTreeItem rootFolder = e.getFolder();

        synchronized (rootMailboxLock) {
            Hashtable remainingMailboxMap = new Hashtable();

            if (rootMailbox != null) {
                // Disassemble the model tree into a flat collection of nodes
                Vector flatMailboxes = new Vector();
                populateFlatMailboxes(flatMailboxes, rootMailbox);
                rootMailbox = null;

                // Prune the collection to only include nodes that are still valid,
                // and make them reference the new FolderTreeItem objects.
                Hashtable folderPathMap = new Hashtable();
                populateFolderPathMap(folderPathMap, rootFolder);

                int size = flatMailboxes.size();

                for (int i = 0; i < size; i++) {
                    MailboxNode mailboxNode = (MailboxNode) flatMailboxes.elementAt(i);
                    String path = mailboxNode.getFolderTreeItem().getPath();

                    if (folderPathMap.containsKey(path)) {
                        mailboxNode.setFolderTreeItem((FolderTreeItem) folderPathMap.get(path));
                        remainingMailboxMap.put(path, mailboxNode);
                    }
                }
            }

            // Build a new tree from the FolderTreeItem, using the collected
            // nodes where possible, and new nodes when necessary.
            this.pathMailboxMap.clear();
            this.rootMailbox = new MailboxNode(rootFolder, false, -1);
            populateMailboxNodes(rootFolder, rootMailbox, remainingMailboxMap);
        }

        save();
        fireAccountStatusChanged(AccountNodeEvent.TYPE_MAILBOX_TREE);
    }

    private void populateFlatMailboxes(Vector flatMailboxes,
        MailboxNode currentMailbox) {
        flatMailboxes.addElement(currentMailbox);

        MailboxNode[] childNodes = currentMailbox.getMailboxes();

        for (int i = 0; i < childNodes.length; i++) {
            populateFlatMailboxes(flatMailboxes, childNodes[i]);
        }

        currentMailbox.clearMailboxes();
    }

    private void populateFolderPathMap(Hashtable folderPathMap,
        FolderTreeItem folderTreeItem) {
        if (folderTreeItem != null) {
            folderPathMap.put(folderTreeItem.getPath(), folderTreeItem);

            if (folderTreeItem.hasChildren()) {
                FolderTreeItem[] children = folderTreeItem.children();

                for (int i = 0; i < children.length; i++) {
                    populateFolderPathMap(folderPathMap, children[i]);
                }
            }
        }
    }

    private void populateMailboxNodes(FolderTreeItem folderTreeItem,
        MailboxNode currentMailbox, Hashtable remainingMailboxMap) {
        pathMailboxMap.put(folderTreeItem.getPath(), currentMailbox);

        if (folderTreeItem.hasChildren()) {
            FolderTreeItem[] folderTreeItemChildren = folderTreeItem.children();

            for (int i = 0; i < folderTreeItemChildren.length; i++) {
                MailboxNode childMailbox;

                if (remainingMailboxMap.containsKey(
                            folderTreeItemChildren[i].getPath())) {
                    childMailbox = (MailboxNode) remainingMailboxMap.get(folderTreeItemChildren[i].getPath());
                } else {
                	int mailboxType = getMailboxType(folderTreeItemChildren[i]);
                	if(mailboxType == MailboxNode.TYPE_OUTBOX) {
	                    childMailbox = new OutboxMailboxNode(folderTreeItemChildren[i]);
                	}
                	else {
	                    childMailbox = new MailboxNode(folderTreeItemChildren[i],
	                    		folderTreeItemChildren[i].isAppendable(),
	                    		mailboxType);
                	}
                    childMailbox.setParentAccount(this);
                }

                populateMailboxNodes(folderTreeItemChildren[i], childMailbox,
                    remainingMailboxMap);
                currentMailbox.addMailbox(childMailbox);
            }
        }
    }

    /**
     * Attempts to determine the folder type based on its name,
     * and any configuration options.
     * <p>
     * This approach is only necessary to for local folders and general
     * defaults.  Explicitly configured special folders have their type set
     * later in the account loading process.
     * </p>
     * @param folderTreeItem Source folder tree item.
     * @return Mailbox type
     */
    protected int getMailboxType(FolderTreeItem folderTreeItem) {
    	int mailboxType;
        if (folderTreeItem.getPath().equalsIgnoreCase("INBOX")) {
        	mailboxType = MailboxNode.TYPE_INBOX;
        }
        else {
            mailboxType = MailboxNode.TYPE_NORMAL;
        }
        return mailboxType;
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
        return (AccountNodeListener[]) listenerList.getListeners(AccountNodeListener.class);
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

        for (int i = 0; i < listeners.length; i++) {
            if (e == null) {
                e = new AccountNodeEvent(this, type);
            }

            ((AccountNodeListener) listeners[i]).accountStatusChanged(e);
        }
    }

    /**
     * Saves the mailbox tree to persistent storage.
     */
    abstract void save();

    /**
     * Loads the mailbox tree from persistent storage.
     */
    abstract void load();

    /**
     * Clear any persistent data associated with this account node.
     *
     * <p>
     * When this account node removed from the model tree because the
     * underlying account has been deleted, this method needs to be called to
     * ensure that persistent data does not linger on the device.
     * </p>
     */
    protected void removeSavedData() {
        // Default empty implementation
    }

    /**
     * Sets the top-level mailbox contained within this account.
     * This method should only be called by subclasses when loading saved
     * account data.
     */
    protected void setRootMailbox(MailboxNode mailboxNode) {
        synchronized (rootMailboxLock) {
            this.rootMailbox = mailboxNode;
            prepareDeserializedMailboxNode(rootMailbox);
        }
    }
    
    /**
     * Traverses the deserialized mailbox nodes, populates any necessary
     * data structures in the account node, and sets the mailbox parent
     * account references.
     *
     * @param mailboxNode The mailbox node.
     */
    private void prepareDeserializedMailboxNode(MailboxNode mailboxNode) {
        mailboxNode.setParentAccount(this);

        FolderTreeItem item = mailboxNode.getFolderTreeItem();

        if ((item != null) && (item.getPath().length() > 0)) {
            this.pathMailboxMap.put(item.getPath(), mailboxNode);
        }

        MailboxNode[] children = mailboxNode.getMailboxes();

        for (int i = 0; i < children.length; i++) {
            prepareDeserializedMailboxNode(children[i]);
        }
    }
}
