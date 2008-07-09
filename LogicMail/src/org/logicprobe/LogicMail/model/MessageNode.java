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
import org.logicprobe.LogicMail.message.FolderMessage;
import org.logicprobe.LogicMail.message.Message;
import org.logicprobe.LogicMail.util.EventListenerList;

/**
 * Message node for the mail data model.
 * This node represents a mail message, and does
 * not contain any other nodes as children.
 */
public class MessageNode implements Node {
	private MailboxNode parent;
	private FolderMessage folderMessage;
	private Message message;
	private EventListenerList listenerList = new EventListenerList();

	MessageNode(FolderMessage folderMessage) {
		this.folderMessage = folderMessage;
	}
	
	public void accept(NodeVisitor visitor) {
		visitor.visit(this);
	}

	/**
	 * Sets the mailbox which is the parent of this node.
	 * @param parent The parent mailbox.
	 */
	void setParent(MailboxNode parent) {
		this.parent = parent;
	}
	
	/**
	 * Gets the mailbox which is the parent of this node.
	 * 
	 * @return The mailbox.
	 */
	public MailboxNode getParent() {
		return this.parent;
	}
	
	/**
	 * Gets the folder message data associated with this node.
	 * This data should always be available when this node is visible.
	 * 
	 * @return Folder message data.
	 */
	public FolderMessage getFolderMessage() {
		return this.folderMessage;
	}
	
	/**
	 * Sets the message data for this node.
	 * 
	 * @param message The message.
	 */
	void setMessage(Message message) {
		this.message = message;
		if(this.message != null) {
			folderMessage.setRecent(false);
			fireMessageStatusChanged(MessageNodeEvent.TYPE_LOADED);
		}
	}
	
	/**
	 * Gets the message data for this node.
	 * The message data will be null unless it has been explicitly loaded.
	 * 
	 * @return The message.
	 */
	public Message getMessage() {
		if(this.message != null) {
			folderMessage.setSeen(true);
		}
		return this.message;
	}
	
	/**
	 * Gets the name of this message, which should
	 * be set to the subject text.
	 * 
	 * @return The name.
	 */
	public String getName() {
		return this.folderMessage.getEnvelope().subject;
	}

	/**
	 * Gets the ID of this message.
	 * 
	 * This is currently the index within the mailbox, but it really
	 * should be changed to be the UID of the message.
	 * 
	 * @return Message ID.
	 */
	public int getId() {
		return this.folderMessage.getIndex();
	}
	
    /**
     * Called to load the message data for this node.
     * This loads as much of the message as allowed within
     * the limits defined in the configuration options.
     */
	public void refreshMessage() {
		parent.getParentAccount().getMailStore().requestMessage(
				parent.getFolderTreeItem(),
				folderMessage);
	}
	
	/**
	 * Called to request that the message state be changed to deleted.
	 * Completion of this request will be indicated by a status
	 * change event for the message flags.
	 */
	public void deleteMessage() {
		parent.getParentAccount().getMailStore().requestMessageDelete(
				parent.getFolderTreeItem(),
				folderMessage);
	}
	
	/**
	 * Called to request that the state of a deleted message be changed
	 * back to normal.
	 * Completion of this request will be indicated by a status
	 * change event for the message flags.
	 * <p>
	 * If the mail store does not support undelete, then this method
	 * will do nothing.
	 * </p>
	 */
	public void undeleteMessage() {
		AbstractMailStore mailStore = parent.getParentAccount().getMailStore();
		if(mailStore.hasUndelete()) {
			mailStore.requestMessageUndelete(
					parent.getFolderTreeItem(),
					folderMessage);
		}
	}
	
	/**
     * Adds a <tt>MessageNodeListener</tt> to the message node.
     * 
     * @param l The <tt>MessageNodeListener</tt> to be added.
     */
    public void addMessageNodeListener(MessageNodeListener l) {
        listenerList.add(MessageNodeListener.class, l);
    }

    /**
     * Removes a <tt>MessageNodeListener</tt> from the message node.
     * 
     * @param l The <tt>MessageNodeListener</tt> to be removed.
     */
    public void removeMessageNodeListener(MessageNodeListener l) {
        listenerList.remove(MessageNodeListener.class, l);
    }
    
    /**
     * Returns an array of all <tt>MessageNodeListener</tt>s
     * that have been added to this message node.
     * 
     * @return All the <tt>MessageNodeListener</tt>s that have been added,
     * or an empty array if no listeners have been added.
     */
    public MessageNodeListener[] getMessageNodeListeners() {
        return (MessageNodeListener[])listenerList.getListeners(MessageNodeListener.class);
    }
    
    /**
     * Notifies all registered <tt>MessageNodeListener</tt>s that
     * the message status has changed.
     * 
     * @param type The type of the status change.
     */
    protected void fireMessageStatusChanged(int type) {
        Object[] listeners = listenerList.getListeners(MessageNodeListener.class);
        MessageNodeEvent e = null;
        for(int i=0; i<listeners.length; i++) {
            if(e == null) {
                e = new MessageNodeEvent(this, type);
            }
            ((MessageNodeListener)listeners[i]).messageStatusChanged(e);
        }
    }
}
