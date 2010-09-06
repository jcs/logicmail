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

import org.logicprobe.LogicMail.conf.AccountConfig;
import org.logicprobe.LogicMail.mail.AbstractMailSender;
import org.logicprobe.LogicMail.mail.FolderTreeItem;
import org.logicprobe.LogicMail.mail.NetworkMailStore;
import org.logicprobe.LogicMail.message.FolderMessage;
import org.logicprobe.LogicMail.message.Message;
import org.logicprobe.LogicMail.message.MessageEnvelope;
import org.logicprobe.LogicMail.util.DataStore;
import org.logicprobe.LogicMail.util.DataStoreFactory;

public class NetworkAccountNode extends AccountNode {
    private AccountConfig accountConfig;
    private NetworkMailStore networkMailStore;
    private AbstractMailSender mailSender;
    private boolean shutdown = false;

    NetworkAccountNode(NetworkMailStore mailStore) {
        super(mailStore);
        
        this.networkMailStore = mailStore;
        this.accountConfig = networkMailStore.getAccountConfig();
        this.status = STATUS_OFFLINE;

        // While this is generic behavior, we place it here because the only
        // mail stores without folders are network stores and because some
        // mailbox node initialization behavior requires a fully initialized
        // account node.
        if (!mailStore.hasFolders()) {
            // Create the fake INBOX node for non-folder-capable mail stores
            MailboxNode rootMailbox = new MailboxNode(new FolderTreeItem("", "", ""),
                    false,
                    -1);

            MailboxNode inboxNode = new MailboxNode(new FolderTreeItem(
                    "INBOX", "INBOX", "", true), false, MailboxNode.TYPE_INBOX);
            rootMailbox.addMailbox(inboxNode);
            
            setRootMailbox(rootMailbox);
        }
    }
    
    /**
     * Gets the name of this account.
     *
     * @return The name.
     */
    public String toString() {
        return this.accountConfig.toString();
    }
    
    /**
     * Gets the account configuration.
     *
     * @return The account configuration
     */
    public AccountConfig getAccountConfig() {
        return this.accountConfig;
    }
    
    void setStatus(int status) {
        if (this.status != status) {
            if ((this.status == STATUS_OFFLINE) && !shutdown) {
                networkMailStore.restart();
            }
        }
        super.setStatus(status);
    }

    /**
     * Gets the mail sender associated with this account.
     *
     * @return The mail sender.
     */
    AbstractMailSender getMailSender() {
        return this.mailSender;
    }

    /**
     * Sets the mail sender associated with this account.
     * This is not set in the constructor since it can change
     * whenever account configuration changes.
     *
     * @param mailSender The mail sender.
     */
    void setMailSender(AbstractMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Returns whether this account has a mail sender associated with it.
     *
     * @return True if mail can be sent, false otherwise.
     */
    public boolean hasMailSender() {
        return (this.mailSender != null);
    }
    
    /**
     * Returns whether this account has an identity associated with it.
     * 
     * @return True if an identity is configured, false otherwise.
     */
    public boolean hasIdentity() {
        return this.accountConfig.getIdentityConfig() != null;
    }
    
    /**
     * Sends a message from this account.
     *
     * @param envelope Envelope of the message to send
     * @param message Message to send.
     */
    public void sendMessage(MessageEnvelope envelope, Message message) {
        if (mailSender != null) {
            // Construct an outgoing message node
            FolderMessage outgoingFolderMessage = new FolderMessage(null, envelope, -1, -1);
            outgoingFolderMessage.setSeen(false);
            outgoingFolderMessage.setRecent(true);
            OutgoingMessageNode outgoingMessage =
                new OutgoingMessageNode(
                        outgoingFolderMessage,
                        this, mailSender);
            
            outgoingMessage.setMessageStructure(message.getStructure());
            outgoingMessage.putMessageContent(message.getAllContent());
            MailManager.getInstance().getOutboxMailboxNode().addMessage(outgoingMessage);
        }
    }

    /**
     * Sends a reply message from this account.
     *
     * @param envelope Envelope of the message to send
     * @param message Message to send.
     * @param originalMessageNode Message node this was in reply to.
     */
    public void sendMessageReply(MessageEnvelope envelope, Message message,
            MessageNode originalMessageNode) {
        sendMessageReplyImpl(envelope, message, originalMessageNode,
                OutgoingMessageNode.REPLY_ANSWERED);
    }
    
    /**
     * Sends a forwarded message from this account.
     *
     * @param envelope Envelope of the message to send
     * @param message Message to send.
     * @param originalMessageNode Message node this was in reply to.
     */
    public void sendMessageForwarded(MessageEnvelope envelope, Message message,
            MessageNode originalMessageNode) {
        sendMessageReplyImpl(envelope, message, originalMessageNode,
                OutgoingMessageNode.REPLY_FORWARDED);
    }
    
    private void sendMessageReplyImpl(MessageEnvelope envelope, Message message,
        MessageNode originalMessageNode, int replyType) {
        if (mailSender != null) {
            // Construct an outgoing message node
            FolderMessage outgoingFolderMessage = new FolderMessage(null, envelope, -1, -1);
            outgoingFolderMessage.setSeen(false);
            outgoingFolderMessage.setRecent(true);
            OutgoingMessageNode outgoingMessage =
                new OutgoingMessageNode(
                        outgoingFolderMessage,
                        this, mailSender, originalMessageNode, replyType);
            outgoingMessage.setMessageStructure(message.getStructure());
            outgoingMessage.putMessageContent(message.getAllContent());
            MailManager.getInstance().getOutboxMailboxNode().addMessage(outgoingMessage);
        }
    }
    
    public void requestDisconnect(boolean shutdown) {
        if (status == STATUS_ONLINE) {
            networkMailStore.shutdown(false);
            this.shutdown = shutdown;
        }
    }
    
    protected void save() {
        DataStore connectionCache = DataStoreFactory.getConnectionCacheStore();
        connectionCache.putNamedObject(Long.toString(accountConfig.getUniqueId()), getRootMailbox());
        connectionCache.save();
    }
    
    protected void load() {
        DataStore connectionCache = DataStoreFactory.getConnectionCacheStore();
        
        Object loadedObject = connectionCache.getNamedObject(Long.toString(accountConfig.getUniqueId()));
        
        if (loadedObject instanceof MailboxNode) {
            setRootMailbox((MailboxNode)loadedObject);
        }
    }
    
    protected void removeSavedData() {
        DataStore connectionCache = DataStoreFactory.getConnectionCacheStore();
        connectionCache.removeNamedObject(Long.toString(accountConfig.getUniqueId()));
        
        // This will only exist on certain account types, but this is the
        // easiest place from which to clean it up.
        connectionCache.removeNamedObject(Long.toString(accountConfig.getUniqueId()) + "_INBOX");
        
        connectionCache.save();
    }
}
