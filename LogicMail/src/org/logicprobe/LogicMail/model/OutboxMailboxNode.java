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

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

import net.rim.device.api.system.EventLogger;
import net.rim.device.api.system.PersistentObject;
import net.rim.device.api.system.PersistentStore;
import net.rim.device.api.util.ToIntHashtable;

import org.logicprobe.LogicMail.AppInfo;
import org.logicprobe.LogicMail.mail.AbstractMailSender;
import org.logicprobe.LogicMail.mail.FolderTreeItem;
import org.logicprobe.LogicMail.mail.MailSenderListener;
import org.logicprobe.LogicMail.mail.MessageSentEvent;
import org.logicprobe.LogicMail.mail.MessageToken;
import org.logicprobe.LogicMail.mail.OutgoingMessageToken;
import org.logicprobe.LogicMail.mail.RecipientException;
import org.logicprobe.LogicMail.message.Message;
import org.logicprobe.LogicMail.message.MimeMessageContent;
import org.logicprobe.LogicMail.message.MessageEnvelope;
import org.logicprobe.LogicMail.message.MessageFlags;
import org.logicprobe.LogicMail.util.AtomicBoolean;
import org.logicprobe.LogicMail.util.StringParser;

public class OutboxMailboxNode extends MailboxNode {
    //"org.logicprobe.LogicMail.model.PersistableOutboxMailboxNode"
    private static final long persistentObjectKey = 0x1f5b6187e941e162L;
    
    private final PersistentObject persistentObject;
    private PersistableOutboxMailboxNode persistentContainer;
    
    /** Track the refresh so it only happens once */
    private boolean hasRefreshed;
    /** Set of loaded messages, to prevent redundant saving. */
    private Hashtable savedMessageSet = new Hashtable();

    private ToIntHashtable mailSenderTable = new ToIntHashtable();
    private Hashtable outboundMessageMap = new Hashtable();
    private Hashtable outboundMessageNodeMap = new Hashtable();

    private final AtomicBoolean refreshInProgress = new AtomicBoolean();
    private Thread fetchThread;
    
    private MailSenderListener mailSenderListener = new MailSenderListener() {
        public void messageSent(MessageSentEvent e) {
            mailSender_MessageSent(e);
        }

        public void messageSendFailed(MessageSentEvent e) {
            mailSender_MessageSendFailed(e);
        }
    };

    OutboxMailboxNode(FolderTreeItem folderTreeItem) {
        super(folderTreeItem, false, MailboxNode.TYPE_OUTBOX);
        persistentObject = PersistentStore.getPersistentObject(persistentObjectKey);
        synchronized(persistentObject) {
            Object persisted = persistentObject.getContents();
            if(persisted instanceof PersistableOutboxMailboxNode) {
                persistentContainer = (PersistableOutboxMailboxNode)persisted;
            }
            else {
                persistentContainer = new PersistableOutboxMailboxNode();
                persistentObject.setContents(persistentContainer);
                persistentObject.commit();
            }
        }
    }

    void addMessage(MessageNode message) {
        if(message instanceof OutgoingMessageNode) {
            addOutgoingMessageImpl((OutgoingMessageNode)message);
        }
        super.addMessage(message);
    }

    void addMessages(MessageNode[] messages) {
        for(int i=0; i<messages.length; i++) {
            if(messages[i] instanceof OutgoingMessageNode) {
                addOutgoingMessageImpl((OutgoingMessageNode)messages[i]);
            }
        }
        super.addMessages(messages);
    }

    /**
     * Gets the unseen message count for this folder.
     * <p>
     * For the outbox, this is a special case.  Instead of returning
     * the number of unseen messages, we return the total number of
     * messages.  This is because the outbox is transitory in nature,
     * and any messages contained within it are ones the user should
     * know about.
     * </p>
     * 
     * @return Unseen message count.
     */
    public int getUnseenMessageCount() {
        return this.getMessageCount();
    }

    /**
     * Checks whether this mailbox has unsent messages.
     * <p>
     * This method works by comparing the number of messages queued in
     * mail senders to the number of messages in the mailbox itself.
     * Therefore, it will return false if all messages are in the process
     * of being sent.
     * </p>
     * 
     * @return true if unsent messages exist
     */
    public boolean hasUnsentMessages() {
        return outboundMessageNodeMap.size() < getMessageCount();
    }

    protected void fireMailboxStatusChanged(int type, MessageNode[] affectedMessages) {
        super.fireMailboxStatusChanged(type, affectedMessages);
        if(type == MailboxNodeEvent.TYPE_NEW_MESSAGES) {
            (new HandleNewMessagesThread(affectedMessages)).start();
        }
    }

    private class HandleNewMessagesThread extends Thread {
        private MessageNode[] newMessages;

        public HandleNewMessagesThread(MessageNode[] newMessages) {
            this.newMessages = newMessages;
        }

        public void run() {
            yield();
            for(int i=0; i<newMessages.length; i++) {
                if(newMessages[i] instanceof OutgoingMessageNode) {
                    OutgoingMessageNode outgoingMessage = (OutgoingMessageNode)newMessages[i];
                    if(!outgoingMessage.isSendAttempted()) {
                        handleNewMessage((OutgoingMessageNode)newMessages[i]);
                    }
                }
            }
        }
    }

    /**
     * For an outgoing message, this method runs before it is added
     * to the mailbox.  It fixes the ID and makes sure we have a
     * listener subscribed to its mail sender.
     * 
     * @param message The outgoing message.
     */
    private void addOutgoingMessageImpl(OutgoingMessageNode message) {
        // Create and set a message token, if necessary
        if(message.getMessageToken() == null) {
            OutgoingMessageToken messageToken =
                new OutgoingMessageToken(getFolderTreeItem(), System.currentTimeMillis());
            message.setMessageToken(messageToken);
        }
    }

    public void refreshMessages() {
        // Fetch messages stored in the cache
        if(refreshInProgress.compareAndSet(false, true)) {
            if(!hasRefreshed && (fetchThread == null || !fetchThread.isAlive())) {
                hasRefreshed = true;
                fetchThread = new RefreshMessagesThread();
                fetchThread.start();
            }
            else {
                refreshInProgress.set(false);
            }
        }
    }

    private class RefreshMessagesThread extends Thread {
        public void run() {
            try {
                synchronized(persistentObject) {
                    Object value = persistentContainer.getElement(PersistableOutboxMailboxNode.FIELD_OUTGOING_MESSAGE_MAP);
                    if(value instanceof Hashtable) {
                        Hashtable outgoingNodeMap = (Hashtable)value;
                        Enumeration e = outgoingNodeMap.elements();
                        while(e.hasMoreElements()) {
                            Object element = e.nextElement();
                            if(element instanceof PersistableOutgoingMessageNode) {
                                addMessageNode((PersistableOutgoingMessageNode)element);
                            }
                        }
                    }
                }
            } catch (Throwable exp) {
                EventLogger.logEvent(AppInfo.GUID,
                        ("Unable to read outgoing messages\r\n"
                                + exp.getMessage()).getBytes(),
                                EventLogger.ERROR);
            }
            refreshInProgress.set(false);
        }

        private void addMessageNode(PersistableOutgoingMessageNode persistableElement) {
            OutgoingMessageNode outgoingMessage = new OutgoingMessageNode(persistableElement);
            
            // Loaded messages were always send-attempted at least once
            outgoingMessage.setSendAttempted(true);
            
            savedMessageSet.put(outgoingMessage, Boolean.TRUE);
            OutboxMailboxNode.this.addMessage(outgoingMessage);
        }
    }

    public boolean hasAppend() {
        // Feature not supported because the outbox has no backing mail store
        return false;
    }

    public void appendMessage(MessageEnvelope envelope, Message message, MessageFlags messageFlags) {
        // Feature not supported because the outbox has no backing mail store
    }

    public void appendMessage(MessageNode message) {
        // Feature not supported because the outbox has no backing mail store
    }

    public void appendRawMessage(String rawMessage, MessageFlags initialFlags) {
        // Feature not supported because the outbox has no backing mail store
    }

    public boolean hasCopy() {
        // Feature not supported because the outbox has no backing mail store
        return false;
    }

    public void copyMessageInto(MessageNode messageNode) {
        // Feature not supported because the outbox has no backing mail store
    }

    public void expungeDeletedMessages() {
        // Feature not supported because the outbox has no backing mail store
    }

    /**
     * For an outgoing message, this method runs after everyone else
     * has been notified of the message being added to the mailbox.
     * Its purpose is to request that the message be sent using its
     * mail sender.
     * 
     * @param outgoingMessageNode the outgoing message node
     */
    private void handleNewMessage(OutgoingMessageNode outgoingMessageNode) {
        outgoingMessageNode.setSendAttempted(true);

        // Persist the message node
        if(!savedMessageSet.containsKey(outgoingMessageNode)) {
            try {
                synchronized(persistentObject) {
                    Object value = persistentContainer.getElement(PersistableOutboxMailboxNode.FIELD_OUTGOING_MESSAGE_MAP);
                    if(value instanceof Hashtable) {
                        ((Hashtable)value).put(
                                outgoingMessageNode.getMessageToken().clone(),
                                outgoingMessageNode.getPersistable());
                    }
                    else {
                        Hashtable outgoingNodeMap = new Hashtable();
                        outgoingNodeMap.put(
                                outgoingMessageNode.getMessageToken().clone(),
                                outgoingMessageNode.getPersistable());
                        persistentContainer.setElement(PersistableOutboxMailboxNode.FIELD_OUTGOING_MESSAGE_MAP, outgoingNodeMap);
                    }
                    persistentObject.commit();
                }
                
                outgoingMessageNode.setCached(true);
                savedMessageSet.put(outgoingMessageNode, Boolean.TRUE);
            } catch (Throwable exp) {
                EventLogger.logEvent(AppInfo.GUID,
                        ("Unable to store outgoing message: " + exp.toString()).getBytes(),
                        EventLogger.ERROR);
            }
        }

        requestSendMessage(outgoingMessageNode);
    }

    /**
     * Send a message contained within this mailbox.
     * <p>
     * Message sending normally happens automatically when a message is added
     * to the outbox.  This method is intended to only be called for deliberate
     * send attempts as triggered by the user.
     * </p>
     * @param outgoingMessageNode the message to send
     */
    void sendMessage(OutgoingMessageNode outgoingMessageNode) {
        if(this.containsMessage(outgoingMessageNode) && !outboundMessageNodeMap.containsKey(outgoingMessageNode)) {
            requestSendMessage(outgoingMessageNode);
        }
    }

    private void requestSendMessage(OutgoingMessageNode outgoingMessageNode) {
        AbstractMailSender mailSender = outgoingMessageNode.getMailSender();
        if(mailSender == null) { return; }

        addMailSenderListener(mailSender);
        outgoingMessageNode.setSending(true);

        // Build the envelope object
        MessageEnvelope envelope = new MessageEnvelope();
        envelope.date = outgoingMessageNode.getDate();
        envelope.subject = outgoingMessageNode.getSubject();
        envelope.from = StringParser.toStringArray(outgoingMessageNode.getFrom());
        envelope.sender = StringParser.toStringArray(outgoingMessageNode.getSender());
        envelope.replyTo = StringParser.toStringArray(outgoingMessageNode.getReplyTo());
        envelope.to = StringParser.toStringArray(outgoingMessageNode.getTo());
        envelope.cc = StringParser.toStringArray(outgoingMessageNode.getCc());
        envelope.bcc = StringParser.toStringArray(outgoingMessageNode.getBcc());
        envelope.inReplyTo = outgoingMessageNode.getInReplyTo();
        envelope.messageId = outgoingMessageNode.getMessageId();

        // Create a protocol-compatible message object
        Message message = new Message(outgoingMessageNode.getMessageStructure());

        // Populate the message content
        MimeMessageContent[] content = outgoingMessageNode.getAllMessageContent();
        for(int i=0; i<content.length; i++) {
            message.putContent(content[i].getMessagePart(), content[i]);
        }

        // Update the outbound map and request the message to be sent
        outboundMessageMap.put(message, outgoingMessageNode);
        outboundMessageNodeMap.put(outgoingMessageNode, message);
        mailSender.requestSendMessage(envelope, message);
    }

    private void mailSender_MessageSent(MessageSentEvent e) {
        // Find out whether we know about this message
        Message message = e.getMessage();
        if(outboundMessageMap.get(message) instanceof OutgoingMessageNode) {
            OutgoingMessageNode outgoingMessageNode = (OutgoingMessageNode)outboundMessageMap.get(message);
            removeMailSenderListener(outgoingMessageNode.getMailSender());
            outboundMessageMap.remove(message);
            outboundMessageNodeMap.remove(outgoingMessageNode);
            outgoingMessageNode.setSending(false);

            // Remove the persisted version of this message
            removePersistedMessage(outgoingMessageNode);

            // Store to the Sent folder
            if(outgoingMessageNode.getSendingAccount() != null) {
                NetworkAccountNode sendingAccount = (NetworkAccountNode)outgoingMessageNode.getSendingAccount();
                
                // Append to the Sent message folder, if available
                MailboxNode sentMailbox = sendingAccount.getSentMailbox();
                if(sentMailbox != null && sentMailbox.hasAppend()) {
                    MessageFlags initialFlags = new MessageFlags();
                    initialFlags.setSeen(true);
                    sentMailbox.appendRawMessage(e.getMessageSource(), initialFlags);
                }
            }

            // Update replied-to message flags
            AccountNode replyToMessageAccount = outgoingMessageNode.getReplyToAccount();
            MessageToken replyToMessageToken = outgoingMessageNode.getReplyToToken();
            if(replyToMessageAccount != null && replyToMessageToken != null) {
                MailStoreServices sendingMailStore = replyToMessageAccount.getMailStoreServices();
                if(sendingMailStore.hasFlags()) {
                    if(outgoingMessageNode.getReplyType() == OutgoingMessageNode.REPLY_FORWARDED) {
                        sendingMailStore.requestMessageForwarded(replyToMessageToken);
                    }
                    else {
                        sendingMailStore.requestMessageAnswered(replyToMessageToken);
                    }
                }
            }

            // Remove from this folder
            removeMessage(outgoingMessageNode);
        }
    }

    void removePersistedMessage(OutgoingMessageNode outgoingMessageNode) {
        try {
            synchronized(persistentObject) {
                Object value = persistentContainer.getElement(PersistableOutboxMailboxNode.FIELD_OUTGOING_MESSAGE_MAP);
                if(value instanceof Hashtable) {
                    ((Hashtable)value).remove(outgoingMessageNode.getMessageToken());
                }
                persistentObject.commit();
            }
        } catch (Throwable exp) {
            EventLogger.logEvent(AppInfo.GUID,
                    ("Unable to delete sent message: " + exp.toString()).getBytes(),
                    EventLogger.ERROR);
        }
    }
    
    void clearMessages() {
        super.clearMessages();
        savedMessageSet.clear();
        outboundMessageMap.clear();
        outboundMessageNodeMap.clear();
        synchronized(persistentObject) {
            Object value = persistentContainer.getElement(PersistableOutboxMailboxNode.FIELD_OUTGOING_MESSAGE_MAP);
            if(value instanceof Hashtable) {
                ((Hashtable)value).clear();
            }
            persistentObject.commit();
        }
    }
    
    private void mailSender_MessageSendFailed(MessageSentEvent e) {
        // Find out whether we know about this message
        Message message = e.getMessage();
        
        if(outboundMessageMap.get(message) instanceof OutgoingMessageNode) {
            OutgoingMessageNode outgoingMessageNode = (OutgoingMessageNode)outboundMessageMap.get(message);

            // Handle the specific case of a failed server connection being retried
            if(!e.isFinal() && e.getException() instanceof IOException) {
                AbstractMailSender mailSender = outgoingMessageNode.getMailSender();
                mailSender.requestSendMessage(e.getEnvelope(), message);
                return;
            }

            removeMailSenderListener(outgoingMessageNode.getMailSender());

            // Remove from the maps that track outbound messages
            outboundMessageMap.remove(message);
            outboundMessageNodeMap.remove(outgoingMessageNode);
            outgoingMessageNode.setSending(false);
            
            if(e.getException() instanceof RecipientException) {
                RecipientException recipientException = (RecipientException)e.getException();
                String address = recipientException.getAddress();
                Address[] recipients;
                switch(recipientException.getCause()) {
                case RecipientException.RECIPIENT_TO:
                    recipients = outgoingMessageNode.getTo();
                    for(int i=0; i<recipients.length; i++) {
                        if(recipients[i].getAddress().equals(address)) {
                            outgoingMessageNode.setToError(i);
                            break;
                        }
                    }
                    break;
                case RecipientException.RECIPIENT_CC:
                    recipients = outgoingMessageNode.getCc();
                    for(int i=0; i<recipients.length; i++) {
                        if(recipients[i].getAddress().equals(address)) {
                            outgoingMessageNode.setCcError(i);
                            break;
                        }
                    }
                    break;
                case RecipientException.RECIPIENT_BCC:
                    recipients = outgoingMessageNode.getBcc();
                    for(int i=0; i<recipients.length; i++) {
                        if(recipients[i].getAddress().equals(address)) {
                            outgoingMessageNode.setBccError(i);
                            break;
                        }
                    }
                    break;
                }
            }
        }
    }

    private void addMailSenderListener(AbstractMailSender mailSender) {
        if(!mailSenderTable.containsKey(mailSender)) {
            mailSender.addMailSenderListener(mailSenderListener);
            mailSenderTable.put(mailSender, 1);
        }
        else {
            int count = mailSenderTable.get(mailSender);
            mailSenderTable.put(mailSender, ++count);
        }
    }

    private void removeMailSenderListener(AbstractMailSender mailSender) {
        if(mailSenderTable.containsKey(mailSender)) {
            int count = mailSenderTable.get(mailSender);
            count--;
            if(count > 0) {
                mailSenderTable.put(mailSender, count);
            }
            else {
                mailSender.removeMailSenderListener(mailSenderListener);
                mailSenderTable.remove(mailSender);
            }
        }
    }
}
