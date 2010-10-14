/*-
 * Copyright (c) 2010, Derek Konigsberg
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

import net.rim.device.api.util.ToIntHashtable;

import org.logicprobe.LogicMail.mail.AbstractMailStore;
import org.logicprobe.LogicMail.mail.FolderEvent;
import org.logicprobe.LogicMail.mail.FolderExpungedEvent;
import org.logicprobe.LogicMail.mail.FolderListener;
import org.logicprobe.LogicMail.mail.FolderMessageIndexMapEvent;
import org.logicprobe.LogicMail.mail.FolderMessagesEvent;
import org.logicprobe.LogicMail.mail.FolderTreeItem;
import org.logicprobe.LogicMail.mail.MailStoreListener;
import org.logicprobe.LogicMail.mail.MailStoreRequestCallback;
import org.logicprobe.LogicMail.mail.MessageEvent;
import org.logicprobe.LogicMail.mail.MessageListener;
import org.logicprobe.LogicMail.mail.MessageToken;
import org.logicprobe.LogicMail.message.FolderMessage;
import org.logicprobe.LogicMail.message.Message;
import org.logicprobe.LogicMail.message.MessageEnvelope;
import org.logicprobe.LogicMail.message.MessageFlags;
import org.logicprobe.LogicMail.message.MessageMimeConverter;
import org.logicprobe.LogicMail.message.MimeMessageContent;
import org.logicprobe.LogicMail.message.MimeMessagePart;
import org.logicprobe.LogicMail.util.EventListenerList;
import org.logicprobe.LogicMail.util.MailMessageParser;

/**
 * Provides a facade to the mail store infrastructure, supplying an interface
 * more directly useful to the needs of the model layer.
 */
public abstract class MailStoreServices {
    private final AbstractMailStore mailStore;
    private final EventListenerList listenerList = new EventListenerList();
    
    protected MailStoreServices(AbstractMailStore mailStore) {
        this.mailStore = mailStore;
        mailStore.addMailStoreListener(new MailStoreListener() {
            public void folderTreeUpdated(FolderEvent e) {
                fireFolderTreeUpdated(e.getFolder());
            }
        });
        mailStore.addFolderListener(new FolderListener() {
            public void folderMessagesAvailable(FolderMessagesEvent e) {
                fireFolderMessagesAvailable(e.getFolder(), e.getMessages(), e.isFlagsOnly());
            }
            public void folderMessageIndexMapAvailable(FolderMessageIndexMapEvent e) {
                fireFolderMessageIndexMapAvailable(e.getFolder(), e.getUidIndexMap());
            }
            public void folderStatusChanged(FolderEvent e) {
                fireFolderStatusChanged(e.getFolder());
            }
            public void folderExpunged(FolderExpungedEvent e) {
                fireFolderExpunged(e.getFolder(), e.getExpungedIndices());
            }
        });
        mailStore.addMessageListener(new MessageListener() {
            public void messageAvailable(MessageEvent e) {
                switch(e.getType()) {
                case MessageEvent.TYPE_FULLY_LOADED:
                    fireMessageAvailable(
                            e.getMessageToken(),
                            e.getMessageStructure(),
                            e.getMessageContent(),
                            e.getMessageSource());
                    break;
                case MessageEvent.TYPE_CONTENT_LOADED:
                    fireMessageContentAvailable(
                            e.getMessageToken(),
                            e.getMessageContent());
                    break;
                }
            }
            public void messageFlagsChanged(MessageEvent e) {
                fireMessageFlagsChanged(e.getMessageToken(), e.getMessageFlags());
            }
            public void messageDeleted(MessageEvent e) {
                fireMessageDeleted(e.getMessageToken(), e.getMessageFlags());
            }
            public void messageUndeleted(MessageEvent e) {
                fireMessageUndeleted(e.getMessageToken(), e.getMessageFlags());
            }
        });
    }

    public void shutdown(boolean wait) {
        mailStore.shutdown(wait);
    }

    public boolean isLocal() {
        return mailStore.isLocal();
    }

    /**
     * Called to inform the mail store services of a change in connection
     * status.  This is only necessary because of the roundabout way connection
     * status notifications are currently implemented.
     *
     * @param connected true if connected, false if disconnected
     */
    public void setConnected(boolean connected) {
        // Default empty implementation
    }

    // ---- Capabilities checking properties, should be eventually removed
    
    public boolean hasFolders() {
        return mailStore.hasFolders();
    }

    public boolean hasMessageParts() {
        return mailStore.hasMessageParts();
    }

    public boolean hasFlags() {
        return mailStore.hasFlags();
    }

    public boolean hasCopy() {
        return mailStore.hasCopy();
    }

    public boolean hasUndelete() {
        return mailStore.hasUndelete();
    }

    public boolean hasExpunge() {
        return mailStore.hasExpunge();
    }

    // ---- Account level request methods
    
    public void requestFolderTree() {
        mailStore.requestFolderTree();
    }
    
    public void requestFolderStatus(FolderTreeItem[] folders) {
        mailStore.requestFolderStatus(folders);
    }
    
    // ---- Folder level request methods
    
    /**
     * Requests a folder refresh, which is the first operation that should be
     * performed to populate the contents of a mailbox node.  The result of
     * this operation will be a series of events that indicate the availability
     * or state change of messages within the mailbox node's folder.
     *
     * @param folderTreeItem the mail store folder to be refreshed
     */
    public abstract void requestFolderRefresh(FolderTreeItem folderTreeItem);
    
    /**
     * Requests a folder fetch for more messages that may be available within
     * the provided folder.
     * 
     * @param folderTreeItem the mail store folder to fetch messages for
     * @param firstToken the token for the message preceeding the range to fetch
     */
    public abstract void requestMoreFolderMessages(FolderTreeItem folderTreeItem, MessageToken firstToken);
    
    /**
     * Appends a message node to the provided mail store folder.
     * This method will request that the underlying mail store add the provided
     * message to its contents for the folder.
     * <p>
     * If the mail store does not support this operation, then this method will
     * have no effect.
     * </p>
     *
     * @param folderTreeItem the mail store folder to append to
     * @param message the message to be appended
     */
    public void requestMessageAppend(FolderTreeItem folderTreeItem, MessageNode message) {
        // Sanity check
        if(!mailStore.hasAppend()) {
            return;
        }
        String rawMessage = message.getMessageSource();
        if(rawMessage == null) {
            // Generate the message source
            rawMessage = message.toMimeMessage(false);
        }

        // Generate the protocol-layer-compatible flag object
        int flags = message.getFlags();
        MessageFlags messageFlags = new MessageFlags();
        messageFlags.setSeen((flags & MessageNode.Flag.SEEN) != 0);
        messageFlags.setAnswered((flags & MessageNode.Flag.ANSWERED) != 0);
        messageFlags.setFlagged((flags & MessageNode.Flag.FLAGGED) != 0);
        messageFlags.setDeleted((flags & MessageNode.Flag.DELETED) != 0);
        messageFlags.setDraft((flags & MessageNode.Flag.DRAFT) != 0);
        messageFlags.setRecent((flags & MessageNode.Flag.RECENT) != 0);
        messageFlags.setForwarded((flags & MessageNode.Flag.FORWARDED) != 0);
        messageFlags.setJunk((flags & MessageNode.Flag.JUNK) != 0);
        
        // Append the message to the folder
        mailStore.requestMessageAppend(
                folderTreeItem,
                rawMessage,
                messageFlags);
    }
    
    /**
     * Appends a message to the provided mail store folder.
     * This method will request that the underlying mail store add the provided
     * message to its contents for the folder.
     * <p>
     * This method is used for drafts, where a MessageNode has not yet been
     * created.
     * </p>
     * <p>
     * If the mail store does not support this operation, then this method will
     * have no effect.
     * </p>
     * 
     * @param folderTreeItem the mail store folder to append to
     * @param envelope Envelope of the message to append
     * @param message Message to append
     * @param messageFlags Flags for the message
     */
    public void requestMessageAppend(FolderTreeItem folderTreeItem, MessageEnvelope envelope, Message message, MessageFlags messageFlags) {
        // Sanity check
        if(!mailStore.hasAppend()) {
            return;
        }
        // Generate the message source
        String rawMessage = generateRawMessage(envelope, message);

        // Append the message to the folder
        mailStore.requestMessageAppend(
                folderTreeItem,
                rawMessage,
                messageFlags);
    }

    /**
     * Generate raw message source for an object-structured message.
     * 
     * @param envelope The envelope of the message to transform
     * @param message The message to transform
     * @return The raw source
     */
    private static String generateRawMessage(MessageEnvelope envelope, Message message) {
        StringBuffer buf = new StringBuffer();
        buf.append(MailMessageParser.generateMessageHeaders(envelope, false));
        
        MessageMimeConverter messageMime = new MessageMimeConverter(message);
        buf.append(messageMime.toMimeString());

        String rawMessage = buf.toString();
        return rawMessage;
    }
    
    /**
     * Appends a message to the provided mail store folder.
     * This method will request that the underlying mail store add the provided
     * message to its contents for the folder.
     * <p>
     * If the mail store does not support this operation, then this method will
     * have no effect.
     * </p>
     * 
     * @param folderTreeItem the mail store folder to append to
     * @param rawMessage Message to append
     * @param initialFlags Initial flags for the message
     */
    public void requestMessageAppend(FolderTreeItem folderTreeItem, String rawMessage, MessageFlags initialFlags) {
        // Sanity check
        if(!mailStore.hasAppend()) {
            return;
        }
        mailStore.requestMessageAppend(folderTreeItem, rawMessage, initialFlags);
    }
    
    /**
     * Copies a message into the provided mail store folder from another folder
     * on the same mail store.
     * This method will request the underlying mail store to copy the message
     * on the server side.
     * <p>
     * If the mail store does not support this operation, then this method will
     * have no effect.
     * </p>
     * 
     * @param folderTreeItem the mail store folder to copy into
     * @param messageNode Message to copy into this mailbox
     */
    public void requestMessageCopy(FolderTreeItem folderTreeItem, MessageNode messageNode) {
        // Sanity check
        if(!(mailStore.hasAppend()
                && mailStore.hasCopy()
                && messageNode.getParent().getParentAccount().getMailStoreServices() == this)) {
            return;
        }
        mailStore.requestMessageCopy(messageNode.getMessageToken(), folderTreeItem);
    }
    
    /**
     * Moves a message into the provided mail store folder from another folder
     * on the same mail store.
     * This method will request the underlying mail store to copy the message
     * on the server side.  If and only if the copy is successful, the source
     * message will be marked as deleted.
     * <p>
     * If the mail store does not support this operation, then this method will
     * have no effect.
     * </p>
     * 
     * @param folderTreeItem the mail store folder to move into
     * @param messageNode Message to move into this mailbox
     */
    public void requestMessageMove(FolderTreeItem folderTreeItem, MessageNode messageNode) {
        // Sanity check
        if(!(mailStore.hasAppend()
                && mailStore.hasCopy()
                && messageNode.getParent().getParentAccount().getMailStoreServices() == this)) {
            return;
        }
        mailStore.requestMessageCopy(
                messageNode.getMessageToken(),
                folderTreeItem,
                new MoveMessageIntoCallback(messageNode));
    }
    
    /**
     * Callback to handle the result of the copy operation dispatched
     * from <code>moveMessageInto(messageNode)</code>.  If that operation
     * succeeds, then a request is dispatched to mark the original message
     * as deleted.  If it fails, then nothing happens.
     */
    private class MoveMessageIntoCallback implements MailStoreRequestCallback {
        private MessageNode messageNode;
        
        /**
         * Instantiates a new callback for handling the result
         * of the copy operation involved in a message move.
         * 
         * @param messageNode the source message node
         */
        public MoveMessageIntoCallback(MessageNode messageNode) {
            this.messageNode = messageNode;
        }
        
        public void mailStoreRequestComplete() {
            // If the move request succeeded, then dispatch a request
            // to have the original message marked as deleted.
            mailStore.requestMessageDelete(
                    messageNode.getMessageToken(),
                    MessageNode.createMessageFlags(messageNode.getFlags()));
        }

        public void mailStoreRequestFailed(Throwable exception) {
            // Do nothing if the request failed
        }
    }
    
    public void requestFolderExpunge(FolderTreeItem folder) {
        mailStore.requestFolderExpunge(folder);
    }
    
    // ---- Message level request methods
    
    public void requestMessage(MessageToken messageToken) {
        mailStore.requestMessage(messageToken);
    }
    
    public void requestMessageParts(MessageToken messageToken, MimeMessagePart[] messageParts) {
        mailStore.requestMessageParts(messageToken, messageParts);
    }
    
    public void requestMessageDelete(MessageToken messageToken, MessageFlags messageFlags) {
        mailStore.requestMessageDelete(messageToken, messageFlags);
    }
    
    public void requestMessageUndelete(MessageToken messageToken, MessageFlags messageFlags) {
        mailStore.requestMessageUndelete(messageToken, messageFlags);
    }
    
    public void requestMessageAnswered(MessageToken messageToken, MessageFlags messageFlags) {
        mailStore.requestMessageAnswered(messageToken, messageFlags);
    }
    
    public void requestMessageForwarded(MessageToken messageToken, MessageFlags messageFlags) {
        mailStore.requestMessageForwarded(messageToken, messageFlags);
    }

    /**
     * Removes the saved data for the mail store.
     *
     * @param folderTreeItems the folder tree items for the mail store
     */
    public void removeSavedData(FolderTreeItem[] folderTreeItems) {
        // Default empty implementation
    }
    
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
    public FolderListener[] getFolderListeners() {
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
     * 
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
    public MessageListener[] getMessageListeners() {
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
     * the status of a folder has changed.
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
     * @param flagsOnly True if the message data only includes flags
     */
    protected void fireFolderMessagesAvailable(FolderTreeItem folder, FolderMessage[] messages, boolean flagsOnly) {
        fireFolderMessagesAvailable(folder, messages, flagsOnly, true);
    }
    
    /**
     * Notifies all registered <tt>FolderListener</tt>s that
     * the message list for a folder has been loaded.
     * 
     * @param folder The folder which has available messages
     * @param messages The messages that are now available
     * @param flagsOnly True if the message data only includes flags
     * @param server True if the message data came from the mail store
     */
    protected void fireFolderMessagesAvailable(FolderTreeItem folder, FolderMessage[] messages, boolean flagsOnly, boolean server) {
        Object[] listeners = listenerList.getListeners(FolderListener.class);
        FolderMessagesEvent e = null;
        for(int i=0; i<listeners.length; i++) {
            if(e == null) {
                e = new FolderMessagesEvent(this, folder, messages, flagsOnly, server);
            }
            ((FolderListener)listeners[i]).folderMessagesAvailable(e);
        }
    }
    
    /**
     * Notifies all registered <tt>FolderListener</tt>s that
     * the UID-to-index map for a folder has been loaded.
     * 
     * @param folder The folder which has available messages
     * @param uidIndexMap The UID-to-index map for the folder's messages
     */
    protected void fireFolderMessageIndexMapAvailable(FolderTreeItem folder, ToIntHashtable uidIndexMap) {
        Object[] listeners = listenerList.getListeners(FolderListener.class);
        FolderMessageIndexMapEvent e = null;
        for(int i=0; i<listeners.length; i++) {
            if(e == null) {
                e = new FolderMessageIndexMapEvent(this, folder, uidIndexMap);
            }
            ((FolderListener)listeners[i]).folderMessageIndexMapAvailable(e);
        }
    }

    /**
     * Notifies all registered <tt>FolderListener</tt>s that
     * the folder has been expunged.
     *
     * @param indices an array of the indices of all expunged messages, or null if not provided
     * @param folder The folder which has had its deleted messages expunged
     */
    protected void fireFolderExpunged(FolderTreeItem folder, int[] indices) {
        Object[] listeners = listenerList.getListeners(FolderListener.class);
        FolderExpungedEvent e = null;
        for(int i=0; i<listeners.length; i++) {
            if(e == null) {
                e = new FolderExpungedEvent(this, folder, indices);
            }
            ((FolderListener)listeners[i]).folderExpunged(e);
        }
    }

    /**
     * Notifies all registered <tt>FolderListener</tt>s that
     * the folder has been expunged.
     *
     * @param tokens an array of the tokens of all expunged messages, or null if not provided
     * @param folder The folder which has had its deleted messages expunged
     */
    protected void fireFolderExpunged(FolderTreeItem folder, MessageToken[] tokens) {
        Object[] listeners = listenerList.getListeners(FolderListener.class);
        FolderExpungedEvent e = null;
        for(int i=0; i<listeners.length; i++) {
            if(e == null) {
                e = new FolderExpungedEvent(this, folder, tokens);
            }
            ((FolderListener)listeners[i]).folderExpunged(e);
        }
    }

    /**
     * Notifies all registered <tt>MessageListener</tt>s that
     * a message has been loaded.
     * 
     * @param messageToken The token identifying the message
     * @param messageStructure The message structure
     * @param messageContent The message content
     * @param messageSource The raw message source, if available
     */
    protected void fireMessageAvailable(MessageToken messageToken, MimeMessagePart messageStructure, MimeMessageContent[] messageContent, String messageSource) {
        Object[] listeners = listenerList.getListeners(MessageListener.class);
        MessageEvent e = null;
        for(int i=0; i<listeners.length; i++) {
            if(e == null) {
                e = new MessageEvent(this, messageToken, messageStructure, messageContent, messageSource);
            }
            ((MessageListener)listeners[i]).messageAvailable(e);
        }
    }

    /**
     * Notifies all registered <tt>MessageListener</tt>s that
     * a message has been loaded.
     * 
     * @param messageToken The token identifying the message
     * @param messageContent The message content
     */
    protected void fireMessageContentAvailable(MessageToken messageToken, MimeMessageContent[] messageContent) {
        Object[] listeners = listenerList.getListeners(MessageListener.class);
        MessageEvent e = null;
        for(int i=0; i<listeners.length; i++) {
            if(e == null) {
                e = new MessageEvent(this, messageToken, messageContent);
            }
            ((MessageListener)listeners[i]).messageAvailable(e);
        }
    }

    /**
     * Notifies all registered <tt>MessageListener</tt>s that
     * a message's flags have changed.
     * 
     * @param messageToken The token identifying the message
     * @param messageFlags The updated message flags
     */
    protected void fireMessageFlagsChanged(MessageToken messageToken, MessageFlags messageFlags) {
        Object[] listeners = listenerList.getListeners(MessageListener.class);
        MessageEvent e = null;
        for(int i=0; i<listeners.length; i++) {
            if(e == null) {
                e = new MessageEvent(this, messageToken, messageFlags);
            }
            ((MessageListener)listeners[i]).messageFlagsChanged(e);
        }
    }

    /**
     * Notifies all registered <tt>MessageListener</tt>s that
     * a message has been deleted.
     * 
     * @param messageToken The token identifying the message
     * @param messageFlags The updated message flags
     */
    protected void fireMessageDeleted(MessageToken messageToken, MessageFlags messageFlags) {
        Object[] listeners = listenerList.getListeners(MessageListener.class);
        MessageEvent e = null;
        for(int i=0; i<listeners.length; i++) {
            if(e == null) {
                e = new MessageEvent(this, messageToken, messageFlags);
            }
            ((MessageListener)listeners[i]).messageDeleted(e);
        }
    }

    /**
     * Notifies all registered <tt>MessageListener</tt>s that
     * a message has been undeleted.
     * 
     * @param messageToken The token identifying the message
     * @param messageFlags The updated message flags
     */
    protected void fireMessageUndeleted(MessageToken messageToken, MessageFlags messageFlags) {
        Object[] listeners = listenerList.getListeners(MessageListener.class);
        MessageEvent e = null;
        for(int i=0; i<listeners.length; i++) {
            if(e == null) {
                e = new MessageEvent(this, messageToken, messageFlags);
            }
            ((MessageListener)listeners[i]).messageUndeleted(e);
        }
    }
}
