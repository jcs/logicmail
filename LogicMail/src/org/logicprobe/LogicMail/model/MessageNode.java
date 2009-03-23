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

import java.util.Date;

import net.rim.device.api.util.Arrays;
import net.rim.device.api.util.Comparator;

import org.logicprobe.LogicMail.AppInfo;
import org.logicprobe.LogicMail.mail.AbstractMailStore;
import org.logicprobe.LogicMail.mail.MessageToken;
import org.logicprobe.LogicMail.message.FolderMessage;
import org.logicprobe.LogicMail.message.MessageEnvelope;
import org.logicprobe.LogicMail.message.MessageFlags;
import org.logicprobe.LogicMail.message.MessageForwardConverter;
import org.logicprobe.LogicMail.message.MessageMimeConverter;
import org.logicprobe.LogicMail.message.MessagePart;
import org.logicprobe.LogicMail.message.MessageReplyConverter;
import org.logicprobe.LogicMail.util.EventListenerList;
import org.logicprobe.LogicMail.util.StringParser;

/**
 * Message node for the mail data model.
 * This node represents a mail message, and does
 * not contain any other nodes as children.
 */
public class MessageNode implements Node {
	/**
	 * Defines the flags supported by the {@link MessageNode} class.
	 */
	public static interface Flag {
		public static final int SEEN = 1;
		public static final int ANSWERED = 2;
		public static final int FLAGGED = 4;
		public static final int DELETED = 8;
		public static final int DRAFT = 16;
		public static final int RECENT = 32;
		public static final int JUNK = 64;
	}
	
	private static class MessageNodeComparator implements Comparator {
		public int compare(Object o1, Object o2) {
			if(o1 instanceof MessageNode && o2 instanceof MessageNode) {
				MessageNode message1 = (MessageNode)o1;
				MessageNode message2 = (MessageNode)o2;
				int result;
				//TODO: Add comparator for MessageToken objects
				/*
				if(message1.messageTag instanceof FolderMessage && message2.messageTag instanceof FolderMessage) {
					// First try folder index comparison
					int index1 = ((FolderMessage)message1.messageTag).getIndex();
					int index2 = ((FolderMessage)message2.messageTag).getIndex();
					if(index1 < index2) { result = -1; }
					else if(index1 > index2) { result = 1; }
					else { result = 0; }
				}
				*/
				if(message1.date != null && message2.date != null) {
					// Then try date comparison
					long time1 = message1.date.getTime();
					long time2 = message2.date.getTime();
					if(time1 < time2) { result = -1; }
					else if(time1 > time2) { result = 1; }
					else { result = 0; }
				}
				else {
					// Worst case, return equal
					result = 0;
				}
				return result;
			}
			else {
				throw new ClassCastException("Cannot compare types");
			}
		}
	}
	
	private static String strCRLF = "\r\n";
	
	/** Static comparator used to compare message nodes for insertion ordering */
	private static MessageNodeComparator comparator = new MessageNodeComparator();

	/** The token object used to identify the message to the protocol layer */
	private MessageToken messageToken;
	/** Hash code used to verify uniqueness of message nodes */
	private int hashCode = -1;
	/** Bit-field set of message flags. */
	private int flags;
	/** Date that the message was sent. */
	private Date date;
	/** Message subject. */
	private String subject;
	/** Addresses the message is from. */
	private Address[] from;
	/** Addresses of the message sender. */
	private Address[] sender;
	/** Reply-To addresses the message is from. */
	private Address[] replyTo;
	/** "To" recipients for the message. */
	private Address[] to;
	/** "CC" recipients for the message. */
	private Address[] cc;
	/** "BCC" recipients for the message. */
	private Address[] bcc;
	/** Message ID string of the message this may be a reply to. */
	private String inReplyTo;
	/** Message ID string from the message headers. */
	private String messageId;
	
	private MailboxNode parent;
	private MessagePart messageBody;
	private String messageSource;
	private EventListenerList listenerList = new EventListenerList();
	private boolean refreshInProgress;

	/**
	 * Instantiates a new message node.
	 * 
	 * @param folderMessage the folder message
	 */
	MessageNode(FolderMessage folderMessage) {
		// Populate fields corresponding to FolderMessage members
		this.messageToken = folderMessage.getMessageToken();
		this.flags = convertMessageFlags(folderMessage.getFlags());
		
		// Populate fields corresponding to MessageEnvelope members
		MessageEnvelope envelope = folderMessage.getEnvelope();
		this.date = envelope.date;
		this.subject = envelope.subject;
		this.from = createAddressArray(envelope.from);
		this.sender = createAddressArray(envelope.sender);
		this.replyTo = createAddressArray(envelope.replyTo);
		this.to = createAddressArray(envelope.to);
		this.cc = createAddressArray(envelope.cc);
		this.bcc = createAddressArray(envelope.bcc);
		this.inReplyTo = envelope.inReplyTo;
		this.messageId = envelope.messageId;
	}

	/**
	 * Instantiates a new message node.
	 * This constructor is only intended for internal use.
	 */
	private MessageNode() {
	}
	
	private static Address[] createAddressArray(String[] recipients) {
		Address[] result;
		if(recipients != null && recipients.length > 0) {
			result = new Address[recipients.length];
			for(int i=0; i<recipients.length; i++) {
				result[i] = new Address(recipients[i]);
			}
		}
		else {
			result = null;
		}
		return result;
	}
	
	/**
	 * Gets the comparator used to compare message nodes for insertion ordering.
	 * 
	 * @return the comparator
	 */
	public static Comparator getComparator() {
		return MessageNode.comparator;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		return MessageNode.comparator.compare(this, obj) == 0;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		if(hashCode == -1) {
			if(messageToken != null) {
				hashCode = 31 * 7 + messageToken.hashCode();
			}
			else if(messageId != null) {
				hashCode = messageId.hashCode();
			}
			else {
				hashCode = super.hashCode();
			}
		}
		return hashCode;
	}
	
	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.model.Node#accept(org.logicprobe.LogicMail.model.NodeVisitor)
	 */
	public void accept(NodeVisitor visitor) {
		visitor.visit(this);
	}

	/**
	 * Sets the mailbox which is the parent of this node.
	 * 
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
	 * Gets the token object used to identify the message to the protocol layer.
	 * 
	 * @return the message token.
	 */
	public MessageToken getMessageToken() {
		return this.messageToken;
	}
	
	/**
	 * Gets the bit-field set of message flags, as specified by {@link Flag}.
	 * 
	 * @return the flags
	 */
	public int getFlags() {
		return flags;
	}

	/**
	 * Sets the bit-field set of message flags, as specified by {@link Flag}.
	 * 
	 * @param flags the flags to set
	 */
	public void setFlags(int flags) {
		this.flags = flags;
	}

	/**
	 * Gets the date that the message was sent.
	 * 
	 * @return the date
	 */
	public Date getDate() {
		return date;
	}

	/**
	 * Sets the date that the message was sent.
	 * 
	 * @param date the date to set
	 */
	public void setDate(Date date) {
		this.date = date;
	}

	/**
	 * Gets the message subject.
	 * 
	 * @return the subject
	 */
	public String getSubject() {
		return subject;
	}

	/**
	 * Sets the message subject.
	 * 
	 * @param subject the subject to set
	 */
	public void setSubject(String subject) {
		this.subject = subject;
	}

	/**
	 * Gets the address the message is from.
	 * 
	 * @return the from address
	 */
	public Address[] getFrom() {
		return from;
	}

	/**
	 * Sets the address the message is from.
	 * 
	 * @param from the from address to set
	 */
	public void setFrom(Address[] from) {
		this.from = from;
	}

	/**
	 * Gets the address of the sender.
	 * 
	 * @return the sender address
	 */
	public Address[] getSender() {
		return sender;
	}

	/**
	 * Sets the address of the sender.
	 * 
	 * @param sender the sender address to set
	 */
	public void setSender(Address[] sender) {
		this.sender = sender;
	}

	/**
	 * Gets the Reply-To address of the message.
	 * 
	 * @return the Reply-To address
	 */
	public Address[] getReplyTo() {
		return replyTo;
	}

	/**
	 * Sets the Reply-To address of the message.
	 * 
	 * @param replyTo the Reply-To address to set
	 */
	public void setReplyTo(Address[] replyTo) {
		this.replyTo = replyTo;
	}

	/**
	 * Gets the "To" recipients of the message.
	 * 
	 * @return the "To" recipients
	 */
	public Address[] getTo() {
		return to;
	}

	/**
	 * Sets the "To" recipients of the message.
	 * 
	 * @param the "To" recipients to set
	 */
	public void setTo(Address[] to) {
		this.to = to;
	}

	/**
	 * Gets the "CC" recipients of the message.
	 * 
	 * @return the "CC" recipients
	 */
	public Address[] getCc() {
		return cc;
	}

	/**
	 * Sets the "CC" recipients of the message.
	 * 
	 * @param cc the "CC" recipients to set
	 */
	public void setCc(Address[] cc) {
		this.cc = cc;
	}

	/**
	 * Gets the "BCC" recipients of the message.
	 * 
	 * @return the "BCC" recipients
	 */
	public Address[] getBcc() {
		return bcc;
	}

	/**
	 * Sets the "BCC" recipients of the message.
	 * 
	 * @param bcc the "BCC" recipients to set
	 */
	public void setBcc(Address[] bcc) {
		this.bcc = bcc;
	}

	/**
	 * Gets the message ID string of the message this may be a reply to.
	 * 
	 * @return the "In-Reply-To" message ID string
	 */
	public String getInReplyTo() {
		return inReplyTo;
	}

	/**
	 * Sets the message ID string of the message this may be a reply to.
	 * 
	 * @param inReplyTo the "In-Reply-To" message ID string to set
	 */
	public void setInReplyTo(String inReplyTo) {
		this.inReplyTo = inReplyTo;
	}

	/**
	 * Gets the message ID string from the message headers.
	 * 
	 * @return the "Message-Id" message ID string
	 */
	public String getMessageId() {
		return messageId;
	}

	/**
	 * Sets the message ID string from the message headers.
	 * 
	 * @param messageId the "Message-Id" message ID string to set
	 */
	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	/**
	 * Sets the message data for this node.
	 * 
	 * @param message The message.
	 */
	void setMessageBody(MessagePart messageBody) {
		this.messageBody = messageBody;
		if(this.messageBody != null) {
			refreshInProgress = false;
			this.flags &= ~Flag.RECENT; // RECENT = false
			fireMessageStatusChanged(MessageNodeEvent.TYPE_LOADED);
		}
	}
	
	/**
	 * Gets the message body for this node.
	 * The message body will be null unless it has been explicitly loaded.
	 * 
	 * @return The message.
	 */
	public MessagePart getMessageBody() {
		if(this.messageBody != null) {
			this.flags |= Flag.SEEN; // SEEN = true
		}
		return this.messageBody;
	}

	/**
	 * Sets the raw message source.
	 * 
	 * @param messageSource The raw message source
	 */
	void setMessageSource(String messageSource) {
		this.messageSource = messageSource;
	}
	
	/**
	 * Gets the raw message source.
	 * 
	 * @return The raw message source
	 */
	public String getMessageSource() {
		return this.messageSource;
	}
	
	/**
	 * Gets the name of this message, which should
	 * be set to the subject text.
	 * 
	 * @return The name.
	 */
	public String toString() {
		return this.subject;
	}

	/**
	 * Converts the contents of the MessageNode into a standard
	 * MIME formatted message, headers included.  Should be similar
	 * to the results of {@link MessageNode#getMessageSource()}, however
	 * the contents are generated dynamically and not are from the
	 * mail server.
	 * 
	 * @param includeUserAgent True to include the User-Agent line.
	 * @return MIME-formatted message
	 */
	public String toMimeMessage(boolean includeUserAgent) {
		StringBuffer buffer = new StringBuffer();

		// Generate the headers
		// TODO: Handle internationalization-friendly encoding of recipient headers
        buffer.append("From: ");
        buffer.append(StringParser.makeCsvString(from));
        buffer.append(strCRLF);

        buffer.append("To: ");
        buffer.append(StringParser.makeCsvString(to));
        buffer.append(strCRLF);

        if ((cc != null) && (cc.length > 0)) {
            buffer.append("Cc: ");
            buffer.append(StringParser.makeCsvString(cc));
            buffer.append(strCRLF);
        }

        if ((replyTo != null) && (replyTo.length > 0)) {
            buffer.append("Reply-To: ");
            buffer.append(StringParser.makeCsvString(replyTo));
            buffer.append(strCRLF);
        }

        buffer.append("Date: ");
        buffer.append(StringParser.createDateString(date));
        buffer.append(strCRLF);

        if (includeUserAgent) {
            buffer.append("User-Agent: ");
            buffer.append(AppInfo.getName());
            buffer.append('/');
            buffer.append(AppInfo.getVersion());
            buffer.append(strCRLF);
        }

        buffer.append("Subject: ");
        buffer.append(subject);
        buffer.append(strCRLF);

        if (inReplyTo != null) {
            buffer.append("In-Reply-To: ");
            buffer.append(inReplyTo);
            buffer.append(strCRLF);
        }
		
		// Generate the body
        MessageMimeConverter messageMime = new MessageMimeConverter();
        messageBody.accept(messageMime);
        buffer.append(messageMime.toMimeString());

		// Return the result
		return buffer.toString();
	}
	
	//TODO: Weed out duplicates from reply headers
	
    /**
     * Get a message that represents a reply to the original message.
     * @return Reply message
     */
	public MessageNode toReplyMessage() {
        // Generate the reply message body
		String senderName;
		if(sender != null && sender.length > 0) {
			senderName = sender[0].getName();
			if(senderName == null || senderName.length() == 0) {
				senderName = sender[0].getAddress();
			}
		}
		else {
			senderName = "";
		}
        MessageReplyConverter replyConverter = new MessageReplyConverter(this.date, senderName);
        if(this.messageBody != null) {
            this.messageBody.accept(replyConverter);
        }
        
        MessageNode replyNode = new MessageNode();
        replyNode.messageBody = replyConverter.toReplyBody();
        populateReplyEnvelope(replyNode);
		
		return replyNode;
	}

	/**
     * Get a message that represents a reply to all the recipients
     * of the original message.
     * @param myAddress Address of the person doing the reply-all, to avoid
     *                  being sent a copy of the outgoing message.
     * @return Reply-All message
     */
    public MessageNode toReplyAllMessage(String myAddress) {
    	MessageNode replyNode = this.toReplyMessage();

        // Handle the additional fields for the reply-all case
        // How do we get myAddress here?
        int i;
        if(to != null) {
            for(i=0; i<to.length; i++) {
                if(to[i].getAddress().toLowerCase().indexOf(myAddress) == -1) {
                    if(replyNode.to == null) {
                    	replyNode.to = new Address[1];
                    	replyNode.to[0] = to[i];
                    }
                    else {
                        Arrays.add(replyNode.to, to[i]);
                    }
                }
            }
        }
        if(cc != null) {
            for(i=0; i<cc.length; i++) {
                if(cc[i].getAddress().toLowerCase().indexOf(myAddress) == -1) {
                    if(replyNode.cc == null) {
                    	replyNode.cc = new Address[1];
                    	replyNode.cc[0] = cc[i];
                    }
                    else {
                        Arrays.add(replyNode.cc, cc[i]);
                    }
                }
            }
        }
    	return replyNode;
    }
    
    /**
     * Get a message that represents a forwarding of the original message.
     * The resulting header will be clean, aside from the subject.
     * The resulting body will be the same as a reply message (single TextPart),
     * aside from some of the original header fields being prepended.
     *
     * @return Forwarded message
     */
    public MessageNode toForwardMessage() {
        // Generate the forward message body
        MessageForwardConverter forwardConverter = new MessageForwardConverter(
        		subject, date,
        		StringParser.toStringArray(from),
        		StringParser.toStringArray(to),
        		StringParser.toStringArray(cc));
        
        if(this.messageBody != null) {
            this.messageBody.accept(forwardConverter);
        }

        MessageNode forwardNode = new MessageNode();
        forwardNode.messageBody = forwardConverter.toForwardBody();

        // Set the forward subject
        if(subject.toLowerCase().startsWith("fwd:")) {
        	forwardNode.subject = subject;
        }
        else {
        	forwardNode.subject = "Fwd: " + subject;
        }
        
    	return forwardNode;
    }

    /**
     * Populate the envelope for a reply to this message
     * 
     * @param replyNode The MessageNode for the reply
     */
    private void populateReplyEnvelope(MessageNode replyNode) {
        // Set the reply subject
        if(subject.startsWith("Re:") || subject.startsWith("re:")) {
        	replyNode.subject = subject;
        }
        else {
        	replyNode.subject = "Re: " + subject;
        }
        
        // Set the message recipient
        int i;
        if(replyTo == null || replyTo.length == 0) {
            if(sender == null || sender.length == 0) {
            	replyNode.to = new Address[from.length];
                for(i=0; i<from.length; i++) {
                	replyNode.to[i] = from[i];
                }
            }
            else {
            	replyNode.to = new Address[sender.length];
                for(i=0; i<sender.length; i++) {
                	replyNode.to[i] = sender[i];
                }
            }
        }
        else {
        	replyNode.to = new Address[replyTo.length];
            for(i=0; i<replyTo.length; i++) {
            	replyNode.to[i] = replyTo[i];
            }
        }

        // Finally, set the message in-reply-to ID
        replyNode.inReplyTo = messageId;
    }
    
    /**
     * Called to load the message data for this node.
     * This loads as much of the message as allowed within
     * the limits defined in the configuration options.
     */
	public void refreshMessage() {
		if(!refreshInProgress) {
			refreshInProgress = true;
			parent.getParentAccount().getMailStore().requestMessage(
					messageToken);
		}
	}
	
	/**
	 * Called to request that the message state be changed to deleted.
	 * Completion of this request will be indicated by a status
	 * change event for the message flags.
	 */
	public void deleteMessage() {
		parent.getParentAccount().getMailStore().requestMessageDelete(
				messageToken,
				createMessageFlags(this.flags));
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
				messageToken,
				createMessageFlags(this.flags));
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

    /**
     * Convert a protocol-later message flags object into the bit-field
     * representation needed for the object model.
     * 
     * @param messageFlags Message flags object.
     * @return Bit-field message flags.
     */
    static int convertMessageFlags(MessageFlags messageFlags) {
		int flags = 0;
		if(messageFlags.isSeen()) { flags |= Flag.SEEN; }
		if(messageFlags.isAnswered()) { flags |= Flag.ANSWERED; }
		if(messageFlags.isFlagged()) { flags |= Flag.FLAGGED; }
		if(messageFlags.isDeleted()) { flags |= Flag.DELETED; }
		if(messageFlags.isDraft()) { flags |= Flag.DRAFT; }
		if(messageFlags.isRecent()) { flags |= Flag.RECENT; }
		if(messageFlags.isJunk()) { flags |= Flag.JUNK; }
		return flags;
	}

    /**
     * Convert a bit-field message flag representation from the
     * object model into a message flags object needed by the
     * protocol-later.
     * 
     * @param flags Bit-field message flags.
     * @return Message flags object.
     */
	static MessageFlags createMessageFlags(int flags) {
		MessageFlags messageFlags = new MessageFlags();
		messageFlags.setSeen((flags & Flag.SEEN) != 0);
		messageFlags.setAnswered((flags & Flag.ANSWERED) != 0);
		messageFlags.setFlagged((flags & Flag.FLAGGED) != 0);
		messageFlags.setDeleted((flags & Flag.DELETED) != 0);
		messageFlags.setDraft((flags & Flag.DRAFT) != 0);
		messageFlags.setRecent((flags & Flag.RECENT) != 0);
		messageFlags.setJunk((flags & Flag.JUNK) != 0);
		return messageFlags;
	}
}
