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

package org.logicprobe.LogicMail.mail;

import org.logicprobe.LogicMail.message.FolderMessage;
import org.logicprobe.LogicMail.message.Message;
import org.logicprobe.LogicMail.util.EventListenerList;

/**
 * Provides the base implementation for the asynchronous
 * mail store infrastructure.
 * 
 * <p>Most methods of this class that could talk to a server
 * are designed to return immediately.  The results should
 * be delivered later via an event sent to the appropriate
 * listener.  Since servers can give notifications independent
 * from specific requests, and since commands can fail,
 * clients should not expect an exact match between requests
 * and events.
 */
public abstract class AbstractMailStore {
    private EventListenerList listenerList = new EventListenerList();
	
	protected AbstractMailStore() {
	}

	//TODO: requestFolderMessages() needs a better way to specify a range 
	//TODO: FolderTreeItem needs to reference a FolderTreeNode
	//TODO: Mechanism for changing message flags for Reply/Forward/etc.
	
	public abstract void shutdown(boolean wait);
	
	/**
	 * Returns whether the mail store is local or remote.
	 * 
	 * @return True if local, false if remote.
	 */
	public abstract boolean isLocal();
	
    /**
     * Returns whether the mail store supports mail folders.
     *
     * @return True if folders supported, false otherwise
     */
    public abstract boolean hasFolders();

    /**
     * Returns whether the mail store supports message flags.
     * 
     * @return True if message flags supported, false otherwise
     */
    public abstract boolean hasFlags();
    
    /**
     * Returns whether the mail store supports undeletion of messages.
     *
     * @return True if undelete supported, false otherwise
     */
    public abstract boolean hasUndelete();

    /**
     * Requests the regeneration of the mail folder tree.
     * 
     * <p>Successful completion is indicated by a call to
     * {@link MailStoreListener#folderTreeUpdated(MailStoreEvent)}.
     * 
     * <p>This can be an expensive operation, so it should
     * be called sparingly on non-local mail stores.
     */
    public abstract void requestFolderTree();
    
    /**
     * Requests the current message counts of a folder or
     * group of folders.
     * 
     * <p>Successful completion is indicated by a call to
     * {@link FolderListener#folderStatusChanged(FolderEvent)}.
     * 
     * @param root The root node of the folder tree to refresh.
     */
    public abstract void requestFolderStatus(FolderTreeItem root);
    
    /**
     * Requests the message listing from a particular folder.
     * 
     * <p>Successful completion is indicated by a call to
     * {@link FolderListener#folderMessagesAvailable(FolderEvent)}.
     * 
     * @param folder The folder to request a message listing for.
     * @param firstIndex The index of the first message to get headers for
     * @param lastIndex The index of the last message to get headers for
     */
    public abstract void requestFolderMessages(FolderTreeItem folder, int firstIndex, int lastIndex);
    
    /**
     * Requests a particular message to be loaded.
     * 
     * <p>Successful completion is indicated by a call to
     * {@link MessageListener#messageAvailable(MessageEvent)}.
     * 
     * @param folder The folder that the message is located in
     * @param message The envelope information for the message to request
     */
    public abstract void requestMessage(FolderTreeItem folder, FolderMessage folderMessage);
    
    /**
     * Requests a particular message to be deleted.
     * 
     * <p>Successful completion is indicated by a call to
     * {@link MessageListener#messageDeleted(MessageEvent)}.
     * 
     * @param folder The folder that the message is located in
     * @param message The envelope information for the message to request
     */
    public abstract void requestMessageDelete(FolderTreeItem folder, FolderMessage folderMessage);
    
    /**
     * Requests a particular message to be undeleted.
     * 
     * <p>Successful completion is indicated by a call to
     * {@link MessageListener#messageUndeleted(MessageEvent)}.
     * 
     * <p>If <tt>hasUndelete()</tt> returns <tt>False</tt>,
     * then this method should throw an
     * <tt>UnsupportedOperationException</tt>.
     * 
     * @param folder The folder that the message is located in
     * @param message The envelope information for the message to request
     */
    public abstract void requestMessageUndelete(FolderTreeItem folder, FolderMessage folderMessage);
    
    /**
     * Adds a <tt>MailStoreListener</tt> to the mail store.
     * 
     * @param l The <tt>MailStoreListener</tt> to be added.
     */
    public void addMailStoreListener(MailStoreListener l) {
        listenerList.add(MailStoreListener.class, l);
    }

    /**
     * Removes a <tt>MailStoreListener</tt> from the mail store.
     * @param l The <tt>MailStoreListener</tt> to be removed.
     */
    public void removeMailStoreListener(MailStoreListener l) {
        listenerList.remove(MailStoreListener.class, l);
    }
    
    /**
     * Returns an array of all <tt>MailStoreListener</tt>s
     * that have been added to this mail store.
     * 
     * @return All the <tt>MailStoreListener</tt>s that have been added,
     * or an empty array if no listeners have been added.
     */
    public MailStoreListener[] getMailStoreListeners() {
        return (MailStoreListener[])listenerList.getListeners(MailStoreListener.class);
    }

    /**
     * Adds a <tt>FolderListener</tt> to the mail store.
     * 
     * @param l The <tt>FolderListener</tt> to be added.
     */
    public void addFolderListener(FolderListener l) {
        listenerList.add(FolderListener.class, l);
    }

    /**
     * Removes a <tt>FolderListener</tt> from the mail store.
     * @param l The <tt>FolderListener</tt> to be removed.
     */
    public void removeFolderListener(FolderListener l) {
        listenerList.remove(FolderListener.class, l);
    }
    
    /**
     * Returns an array of all <tt>FolderListener</tt>s
     * that have been added to this mail store.
     * 
     * @return All the <tt>FolderListener</tt>s that have been added,
     * or an empty array if no listeners have been added.
     */
    public FolderListener[] getFolderListener() {
        return (FolderListener[])listenerList.getListeners(FolderListener.class);
    }

    /**
     * Adds a <tt>MessageListener</tt> to the mail store.
     * 
     * @param l The <tt>MessageListener</tt> to be added.
     */
    public void addMessageListener(MessageListener l) {
        listenerList.add(MessageListener.class, l);
    }

    /**
     * Removes a <tt>MessageListener</tt> from the mail store.
     * @param l The <tt>MessageListener</tt> to be removed.
     */
    public void removeMessageListener(MessageListener l) {
        listenerList.remove(MessageListener.class, l);
    }
    
    /**
     * Returns an array of all <tt>MessageListener</tt>s
     * that have been added to this mail store.
     * 
     * @return All the <tt>MessageListener</tt>s that have been added,
     * or an empty array if no listeners have been added.
     */
    public MessageListener[] getMessageListener() {
        return (MessageListener[])listenerList.getListeners(MessageListener.class);
    }
    
    /**
     * Notifies all registered <tt>MailStoreListener</tt>s that
     * the folder tree has been updated. 
     * 
     * @param root The root node of the updated folder tree
     */
    protected void fireFolderTreeUpdated(FolderTreeItem root) {
        Object[] listeners = listenerList.getListeners(MailStoreListener.class);
        FolderEvent e = null;
        for(int i=0; i<listeners.length; i++) {
            if(e == null) {
                e = new FolderEvent(this, root);
            }
            ((MailStoreListener)listeners[i]).folderTreeUpdated(e);
        }
    }
    
    /**
     * Notifies all registered <tt>FolderListener</tt>s that
     * the the status of a folder has changed.
     * 
     * @param root The root node of the updated folder tree
     */
    protected void fireFolderStatusChanged(FolderTreeItem root) {
        Object[] listeners = listenerList.getListeners(FolderListener.class);
        FolderEvent e = null;
        for(int i=0; i<listeners.length; i++) {
            if(e == null) {
                e = new FolderEvent(this, root);
            }
            ((FolderListener)listeners[i]).folderStatusChanged(e);
        }
    }

    /**
     * Notifies all registered <tt>FolderListener</tt>s that
     * the message list for a folder has been loaded.
     * 
     * @param folder The folder which has available messages
     * @param messages The messages that are now available
     */
    protected void fireFolderMessagesAvailable(FolderTreeItem folder, FolderMessage[] messages) {
        Object[] listeners = listenerList.getListeners(FolderListener.class);
        FolderMessagesEvent e = null;
        for(int i=0; i<listeners.length; i++) {
            if(e == null) {
                e = new FolderMessagesEvent(this, folder, messages);
            }
            ((FolderListener)listeners[i]).folderMessagesAvailable(e);
        }
    }

    /**
     * Notifies all registered <tt>MessageListener</tt>s that
     * a message has been loaded.
     * 
     * @param folder The folder in which a message has become available
     * @param folderMessage The folder data for the message
     * @param message The message itself
     */
    protected void fireMessageAvailable(FolderTreeItem folder, FolderMessage folderMessage, Message message) {
        Object[] listeners = listenerList.getListeners(MessageListener.class);
        MessageEvent e = null;
        for(int i=0; i<listeners.length; i++) {
            if(e == null) {
                e = new MessageEvent(this, folder, folderMessage, message);
            }
            ((MessageListener)listeners[i]).messageAvailable(e);
        }
    }
    
    /**
     * Notifies all registered <tt>MessageListener</tt>s that
     * a message's flags have changed.
     * 
     * @param folder The folder in which a message's flags have changed
     * @param folderMessage The updated folder data for the message
     */
    protected void fireMessageFlagsChanged(FolderTreeItem folder, FolderMessage folderMessage) {
        Object[] listeners = listenerList.getListeners(MessageListener.class);
        MessageEvent e = null;
        for(int i=0; i<listeners.length; i++) {
            if(e == null) {
                e = new MessageEvent(this, folder, folderMessage);
            }
            ((MessageListener)listeners[i]).messageFlagsChanged(e);
        }
    }
    
    /**
     * Notifies all registered <tt>MessageListener</tt>s that
     * a message has been deleted.
     * 
     * @param folder The folder in which a message has been undeleted
     * @param folderMessage The folder data for the message
     */
    protected void fireMessageDeleted(FolderTreeItem folder, FolderMessage folderMessage) {
        Object[] listeners = listenerList.getListeners(MessageListener.class);
        MessageEvent e = null;
        for(int i=0; i<listeners.length; i++) {
            if(e == null) {
                e = new MessageEvent(this, folder, folderMessage);
            }
            ((MessageListener)listeners[i]).messageDeleted(e);
        }
    }
    
    /**
     * Notifies all registered <tt>MessageListener</tt>s that
     * a message has been undeleted.
     * 
     * @param folder The folder in which a message has been undeleted
     * @param folderMessage The folder data for the message
     */
    protected void fireMessageUndeleted(FolderTreeItem folder, FolderMessage folderMessage) {
        Object[] listeners = listenerList.getListeners(MessageListener.class);
        MessageEvent e = null;
        for(int i=0; i<listeners.length; i++) {
            if(e == null) {
                e = new MessageEvent(this, folder, folderMessage);
            }
            ((MessageListener)listeners[i]).messageUndeleted(e);
        }
    }
}
