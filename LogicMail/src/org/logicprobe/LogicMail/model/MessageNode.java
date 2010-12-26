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

import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;

import net.rim.device.api.util.Arrays;
import net.rim.device.api.util.Comparator;

import org.logicprobe.LogicMail.AppInfo;
import org.logicprobe.LogicMail.mail.MessageEvent;
import org.logicprobe.LogicMail.mail.MessageToken;
import org.logicprobe.LogicMail.message.AbstractMimeMessagePartVisitor;
import org.logicprobe.LogicMail.message.ContentPart;
import org.logicprobe.LogicMail.message.FolderMessage;
import org.logicprobe.LogicMail.message.Message;
import org.logicprobe.LogicMail.message.MimeMessageContent;
import org.logicprobe.LogicMail.message.MessageEnvelope;
import org.logicprobe.LogicMail.message.MessageFlags;
import org.logicprobe.LogicMail.message.MessageMimeConverter;
import org.logicprobe.LogicMail.message.MimeMessagePart;
import org.logicprobe.LogicMail.message.MimeMessagePartTransformer;
import org.logicprobe.LogicMail.message.TextContent;
import org.logicprobe.LogicMail.message.TextPart;
import org.logicprobe.LogicMail.util.AtomicBoolean;
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
		public static final int SEEN      = 1;
		public static final int ANSWERED  = 2;
		public static final int FLAGGED   = 4;
		public static final int DELETED   = 8;
		public static final int DRAFT     = 16;
		public static final int RECENT    = 32;
		public static final int JUNK      = 64;
        public static final int FORWARDED = 128;
	}
	
	private static class MessageNodeComparator implements Comparator {
		public int compare(Object o1, Object o2) {
			if(o1 instanceof MessageNode && o2 instanceof MessageNode) {
				MessageNode message1 = (MessageNode)o1;
				MessageNode message2 = (MessageNode)o2;
				int result;
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
	/** True if the message is up-to-date in the cache */
	private boolean cached;
	/** True if the message has cached content available for loading */
	private boolean hasCachedContent;
	/** True if the message has been verified to exist on a mail server */
	private boolean existsOnServer;
	/** The RFC822 size of the message, if available. */
    private int messageSize = -1;
	/** True if the mail store lacks parts, and the message was not truncated. */
	private boolean messageComplete = true;
	/** Bit-field set of message flags. */
	private volatile int flags;
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
	private MimeMessagePart messageStructure;
	private final Hashtable messageContent = new Hashtable();
	private MimeMessagePart[] attachmentParts;
	private String messageSource;
	private final EventListenerList listenerList = new EventListenerList();
	private final AtomicBoolean refreshInProgress = new AtomicBoolean();
	
	/**
	 * Instantiates a new message node.
	 * 
	 * @param folderMessage the folder message
	 */
	MessageNode(FolderMessage folderMessage) {
		// Populate fields corresponding to FolderMessage members
		this.messageToken = folderMessage.getMessageToken();
		this.messageSize = folderMessage.getSize();
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
		this.messageStructure = folderMessage.getStructure();
		if(this.messageStructure != null) {
			this.attachmentParts = MimeMessagePartTransformer.getAttachmentParts(this.messageStructure);
		}
	}
	
	/**
	 * Instantiates a new empty message node.
	 * 
	 * @param messageToken the message token
	 */
	MessageNode(MessageToken messageToken) {
		this.messageToken = messageToken;
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
	
	/**
	 * Checks if the message is up-to-date in the cache.
	 * 
	 * @return true, if the message is cached
	 */
	boolean isCached() {
		return this.cached;
	}
	
	/**
	 * Checks if the message is capable of being cached.
	 * 
	 * @return true, if the message is associated with a mailbox from a non-local account
	 */
	public boolean isCachable() {
		if(parent != null
				&& parent.getParentAccount() != null
				&& parent.getParentAccount() instanceof NetworkAccountNode) {
			return true;
		}
		else {
			return false;
		}
	}
	
	/**
	 * Sets whether the message is up-to-date in the cache.
	 * 
	 * @param cached the new cached state
	 */
	void setCached(boolean cached) {
		this.cached = cached;
	}
	
	/**
	 * Sets whether this message has been verified to exist on a server.
	 * 
	 * @param existsOnServer true if the message has been verified to exist on a server
	 */
	void setExistsOnServer(boolean existsOnServer) {
	    if(this.existsOnServer != existsOnServer) {
	        this.existsOnServer = existsOnServer;
            fireMessageStatusChanged(MessageNodeEvent.TYPE_FLAGS);
	    }
	}
	
	/**
	 * Gets whether this message has been verified to exist on a server.
	 * 
	 * @return true if the message has been verified to exist on a server or is on a local account
	 */
	public boolean existsOnServer() {
	    return existsOnServer;
	}
	
	/**
	 * Gets the RFC822 message size, as reported by the server.
	 * This value does not reflect the size of the downloaded content, and is
	 * primarily intended for user notification.
	 *
	 * @return the message size, or <code>-1</code> if not available
	 */
	public int getMessageSize() {
        return messageSize;
    }
	
	/**
	 * Checks if is message data is complete.
	 * This flag is only relevant on mail stores without selective message
	 * part download.  It indicates that the message was completely downloaded
	 * and has not been truncated by a size limit.  On messages from local
	 * mail stores, or mail stores supporting selective part download, this
	 * flag is completely irrelevant and will always return true.
	 *
	 * @return true, if the message is complete
	 */
	public boolean isMessageComplete() {
        return messageComplete;
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
		if(!isCachable()) { existsOnServer = true; }
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
	 * Sets the token object used to identify the message to the protocol later.
	 * 
	 * @param messageToken the message token.
	 */
	protected void setMessageToken(MessageToken messageToken) {
		cached = false;
		this.messageToken = messageToken;
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
		cached = false;
		if(this.flags != flags) {
		    this.flags = flags;
		    if(this.getParent() != null) {
		        this.getParent().updateUnseenMessages(true);
		    }
		    fireMessageStatusChanged(MessageNodeEvent.TYPE_FLAGS);
		}
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
		cached = false;
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
		cached = false;
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
		cached = false;
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
		cached = false;
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
		cached = false;
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
		cached = false;
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
		cached = false;
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
		cached = false;
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
		cached = false;
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
		cached = false;
		this.messageId = messageId;
	}

	/**
	 * Sets the message structure for this node.
	 * 
	 * @param message The message structure.
	 */
	void setMessageStructure(MimeMessagePart messageStructure) {
		boolean fireEvent;
		synchronized(messageContent) {
			cached = false;
			this.messageStructure = messageStructure;
			if(this.messageStructure != null) {
				this.flags &= ~Flag.RECENT; // RECENT = false
				this.attachmentParts = MimeMessagePartTransformer.getAttachmentParts(this.messageStructure);
				fireEvent = true;
			}
			else {
				fireEvent = false;
			}
		}
		if(fireEvent) {
			fireMessageStatusChanged(MessageNodeEvent.TYPE_STRUCTURE_LOADED);
		}
	}

	/**
	 * Adds content to this message node.
	 * 
	 * @param mimeMessageContent The content to add.
	 */
	void putMessageContent(MimeMessageContent mimeMessageContent) {
		synchronized(messageContent) {
			cached = false;
			this.messageContent.put(mimeMessageContent.getMessagePart(), mimeMessageContent);
		}
		fireMessageStatusChanged(MessageNodeEvent.TYPE_CONTENT_LOADED);
	}

	/**
	 * Adds content to this message node.
	 * <p>
	 * This method provides for a batch addition of content, causing a
	 * single event to be fired afterwards.
	 * </p>
	 * 
	 * @param messageContent The content sections to add.
	 */
	void putMessageContent(MimeMessageContent[] messageContent) {
	    putMessageContent(messageContent, true);
	}
	
    /**
     * Adds content to this message node.
     * <p>
     * This method provides for a batch addition of content, causing a
     * single event to be fired afterwards.
     * </p>
     * 
     * @param messageContent The content sections to add.
     * @param notify True, if a change event should be fired
     */
    void putMessageContent(MimeMessageContent[] messageContent, boolean notify) {
        synchronized(messageContent) {
            cached = false;
            for(int i=0; i<messageContent.length; i++) {
                this.messageContent.put(messageContent[i].getMessagePart(), messageContent[i]);
            }
        }
        if(notify) {
            fireMessageStatusChanged(MessageNodeEvent.TYPE_CONTENT_LOADED);
        }
    }
	
	/**
	 * Gets the message structure for this node.
	 * The message structure will be null unless it has been explicitly loaded.
	 * 
	 * @return The message structure.
	 */
	public MimeMessagePart getMessageStructure() {
        return this.messageStructure;
	}

	/**
	 * Gets message content.
	 * 
	 * @param mimeMessagePart The part that represents the content's structural placement.
	 * @return The content.
	 */
	public MimeMessageContent getMessageContent(MimeMessagePart mimeMessagePart) {
		synchronized(messageContent) {
			return (MimeMessageContent)messageContent.get(mimeMessagePart);
		}
    }
	
	/**
	 * Gets whether this message has any content available.
	 * This is intended to be used as a quick check to determine
	 * whether message content needs to be loaded.
	 * 
	 * @return True if content is available, false otherwise
	 */
	public boolean hasMessageContent() {
		synchronized(messageContent) {
			return (messageStructure != null) && (!messageContent.isEmpty());
		}
	}
	
	/**
	 * Sets whether this message has cached content available.
	 * This method should only be called by code which is creating
	 * a message note instance from local cache data.
	 * 
	 * @param hasCachedContent True if cached content is available for loading
	 */
	public void setCachedContent(boolean hasCachedContent) {
	    this.hasCachedContent = hasCachedContent;
	}
	
	/**
	 * Gets whether this message has cached content available.
	 * This is intended to be used as a quick check to determine whether
	 * message content can be loaded from local cache, without actually
	 * having to load that content first.  It checks a variable that should
	 * be set when message headers are loaded from cache.
	 * 
	 * @return True if cached content is available for loading
	 */
	public boolean hasCachedContent() {
	    return hasCachedContent;
	}
	
	/**
	 * Gets all message content.
	 * 
	 * @return All the content.
	 */
	public MimeMessageContent[] getAllMessageContent() {
		synchronized(messageContent) {
			MimeMessageContent[] result = new MimeMessageContent[messageContent.size()];
			Enumeration e = messageContent.keys();
			int i = 0;
	    	while(e.hasMoreElements()) {
	    		result[i++] = (MimeMessageContent)messageContent.get(e.nextElement());
	    	}
			return result;
		}
	}
	
	/**
	 * Gets the message parts that are considered to be message attachments.
	 * <p>
	 * This is a convenience method, as it returns an array that is populated
	 * from the message structure when {@link #setMessageStructure(MimeMessagePart)}
	 * is called.  This array will contain all message parts that are not of
	 * type multi, text, or unsupported.
	 * </p>
	 * 
	 * @return Message attachments.
	 */
	public MimeMessagePart[] getAttachmentParts() {
		synchronized(messageContent) {
			return this.attachmentParts;
		}
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
        buffer.append(makeRecipientHeader("From:", from));
        buffer.append(strCRLF);

        buffer.append(makeRecipientHeader("To:", to));
        buffer.append(strCRLF);

        if ((cc != null) && (cc.length > 0)) {
            buffer.append(makeRecipientHeader("Cc:", cc));
            buffer.append(strCRLF);
        }

        if ((replyTo != null) && (replyTo.length > 0)) {
            buffer.append(makeRecipientHeader("Reply-To:", replyTo));
            buffer.append(strCRLF);
        }

        buffer.append("Date: ");
        if(date != null) {
            buffer.append(StringParser.createDateString(date));
        }
        else {
            buffer.append(StringParser.createDateString(Calendar.getInstance().getTime()));
        }
        buffer.append(strCRLF);

        if (includeUserAgent) {
            buffer.append("User-Agent: ");
            buffer.append(AppInfo.getName());
            buffer.append('/');
            buffer.append(AppInfo.getVersion());
            buffer.append(strCRLF);
        }

        buffer.append(StringParser.createEncodedHeader("Subject:", subject));
        buffer.append(strCRLF);

        if (inReplyTo != null) {
            buffer.append("In-Reply-To: ");
            buffer.append(inReplyTo);
            buffer.append(strCRLF);
        }
		
        synchronized(messageContent) {
			// Generate the body
	        Message message = new Message(messageStructure);
	        Enumeration en = messageContent.keys();
	        while(en.hasMoreElements()) {
	        	MimeMessagePart part = (MimeMessagePart)en.nextElement();
	        	message.putContent(part, (MimeMessageContent)messageContent.get(part));
	        }
	        
	        MessageMimeConverter messageMime = new MessageMimeConverter(message);
	        buffer.append(messageMime.toMimeString());
	
			// Return the result
			return buffer.toString();
        }
	}
	
	private static String makeRecipientHeader(String key, Address[] recipients) {
	    String[] recipientsStr = new String[recipients.length];
	    for(int i=0; i<recipients.length; i++) {
	        recipientsStr[i] = recipients[i].toString();
	    }
	    return StringParser.createEncodedRecipientHeader(key, recipientsStr);
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
		
		synchronized(messageContent) {
	        FindFirstTextPartVisitor findVisitor = new FindFirstTextPartVisitor();
	        if(this.messageStructure != null) {
	        	this.messageStructure.accept(findVisitor);
	        }
	        TextPart originalTextPart = findVisitor.getFirstTextPart();
	        TextContent originalTextContent = (TextContent)messageContent.get(originalTextPart);
			
	        StringBuffer buf = new StringBuffer();
	        
	        // Create the first line of the reply text
	        buf.append("\r\n");
	        buf.append("On ");
	        buf.append(StringParser.createDateString(date));
	        buf.append(", ");
	        buf.append(senderName);
	        buf.append(" wrote:\r\n");
	        
	        // Generate the quoted message text
	        buf.append("> ");
	        if(originalTextContent != null) {
	            String originalText = originalTextContent.getText();
	            int size = originalText.length();
	            char ch;
	            for(int i=0; i<size; i++) {
	                ch = originalText.charAt(i);
	                buf.append(ch);
	                if(ch == '\n' && i < size - 1) {
	                    buf.append("> ");
	                }
	            }
	        }
	        
	        MessageNode replyNode = new MessageNode();
	        String contentText = buf.toString();
	        TextPart replyPart = new TextPart("plain", "", "", "", "", "", contentText.length());
	        replyNode.messageStructure = replyPart;
	        replyNode.putMessageContent(new TextContent(replyPart, contentText));
	        
	        populateReplyEnvelope(replyNode);
	        
			return replyNode;
		}
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
    	
        String fromString = StringParser.makeCsvString(StringParser.toStringArray(from));
        String toString = StringParser.makeCsvString(StringParser.toStringArray(to));
        String ccString = StringParser.makeCsvString(StringParser.toStringArray(cc));
    	
        synchronized(messageContent) {
	        FindFirstTextPartVisitor findVisitor = new FindFirstTextPartVisitor();
	        if(this.messageStructure != null) {
	        	this.messageStructure.accept(findVisitor);
	        }
	        TextPart originalTextPart = findVisitor.getFirstTextPart();
	        TextContent originalTextContent = (TextContent)messageContent.get(originalTextPart);
	
	        StringBuffer buf = new StringBuffer();
	
	        // Create the first line of the reply text
	        buf.append("\r\n");
	        buf.append("----Original Message----\r\n");
	        
	        // Add the subject
	        buf.append("Subject: ");
	        buf.append(subject);
	        buf.append("\r\n");
	
	        // Add the date
	        buf.append("Date: ");
	        buf.append(StringParser.createDateString(date));
	        buf.append("\r\n");
	        
	        // Add the from field
	        if(fromString != null && fromString.length() > 0) {
		        buf.append("From: ");
		        buf.append(fromString);
		        buf.append("\r\n");
	        }
	        
	        // Add the from field
	        if(toString != null && toString.length() > 0) {
		        buf.append("To: ");
		        buf.append(toString);
		        buf.append("\r\n");
	        }
	        
	        // Add the CC field
	        if(ccString != null && ccString.length() > 0) {
	            buf.append("Cc: ");
	            buf.append(ccString);
	            buf.append("\r\n");
	        }
	
	        // Add a blank like
	        buf.append("\r\n");
	        
	        // Add the original text
	        if(originalTextContent != null) {
	            buf.append(originalTextContent.getText());
	            buf.append("\r\n");
	        }
	        
	        // Add the footer
	        buf.append("------------------------");
	
	        // Build the forward node
	        MessageNode forwardNode = new MessageNode();
	        String contentText = buf.toString();
	        TextPart forwardPart = new TextPart("plain", "", "", "", "", "", contentText.length());
	        forwardNode.messageStructure = forwardPart;
	        forwardNode.putMessageContent(new TextContent(forwardPart, contentText));
	
	        // Set the forward subject
	        if(subject.toLowerCase().startsWith("fwd:")) {
	        	forwardNode.subject = subject;
	        }
	        else {
	        	forwardNode.subject = "Fwd: " + subject;
	        }
	        
	    	return forwardNode;
        }
    }

    private static class FindFirstTextPartVisitor extends AbstractMimeMessagePartVisitor {
        private TextPart firstTextPart;

        public TextPart getFirstTextPart() { return firstTextPart; }
        
		public void visitTextPart(TextPart part) {
	        if(firstTextPart == null) {
	        	firstTextPart = part;
	        }
		}
    };
    
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
     * <p>
     * This loads as much of the displayable parts of the message
     * as allowed within the limits defined in the configuration options.
     * It is intended to be a convenience request for the UI,
     * as individual parts can be requested for attachment download.
     * Since multiple parts are downloaded in response to this request,
     * multiple events may be fired as the retrieval process completes.
     * </p>
     * 
     * @return True if a refresh was triggered, false otherwise
     */
    public boolean refreshMessage() {
        if(refreshInProgress.compareAndSet(false, true)) {
            // Build a list of all message parts that have already been loaded
            MimeMessagePart[] loadedParts;
            synchronized(messageContent) {
                loadedParts = new MimeMessagePart[messageContent.size()];
                int i=0;
                Enumeration e = messageContent.keys();
                while(e.hasMoreElements()) {
                    loadedParts[i++] = (MimeMessagePart)e.nextElement();
                }
            }
            
            MailStoreServices mailStore = parent.getParentAccount().getMailStoreServices();
            return mailStore.requestMessageRefresh(messageToken, loadedParts);
        }
        else {
            return false;
        }
    }

    /**
     * Called to load the complete message data for this node.
     * <p>
     * This loads the entire message, regardless of size, and is only supported
     * for mail stores that do not support retrieval of individual message
     * parts.  It should be called with extreme caution, and only after checking
     * {@link #getMessageSize()} and prompting the user for confirmation.
     * </p>
     * 
     * @return True if a refresh was triggered, false otherwise
     */
    public boolean refreshEntireMessage() {
        // TODO Auto-generated method stub
        return false;
    }
	
	/**
	 * Called to load a specific message part for this node.
	 * 
	 * @param messagePart Content part to load
	 */
	public void requestContentPart(ContentPart messagePart) {
	    MailStoreServices mailStore = parent.getParentAccount().getMailStoreServices();
		if(mailStore.hasMessageParts()) {
			mailStore.requestMessageParts(messageToken, new MimeMessagePart[] { messagePart });
		}
	}
	
	/**
	 * Called to request that the message state be changed to deleted.
	 * Completion of this request will be indicated by a status
	 * change event for the message flags.
	 */
	public void deleteMessage() {
		parent.getParentAccount().getMailStoreServices().requestMessageDelete(
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
	    MailStoreServices mailStore = parent.getParentAccount().getMailStoreServices();
		if(mailStore.hasUndelete()) {
			mailStore.requestMessageUndelete(
				messageToken,
				createMessageFlags(this.flags));
		}
	}
	
    /**
     * Called when the mail store has new message data available.
     *
     * @param e the event from the mail store
     */
    void mailStoreMessageAvailable(MessageEvent e) {
        // Set the SEEN bit and unset the RECENT bit
        int flags = getFlags();
        flags |= MessageNode.Flag.SEEN;
        flags &= ~MessageNode.Flag.RECENT;

        setFlags(flags);
        
        MimeMessageContent[] content = e.getMessageContent();

        switch(e.getType()) {
        case MessageEvent.TYPE_FULLY_LOADED:
            messageComplete = e.isMessageComplete();
            setMessageStructure(e.getMessageStructure());
            setMessageSource(e.getMessageSource());
            putMessageContent(content, false);
            contentLoadComplete();
            break;
        case MessageEvent.TYPE_CONTENT_LOADED:
            if(content == null) {
                contentLoadComplete();
            }
            else {
                putMessageContent(content, false);
            }
            break;
        }
    }
    
    private void contentLoadComplete() {
        refreshInProgress.set(false);
        fireMessageStatusChanged(MessageNodeEvent.TYPE_CONTENT_LOADED);
    }

    /**
     * Called when the mail store notifies that message flags have changed.
     *
     * @param e the event from the mail store
     */
    void mailStoreMessageFlagsChanged(MessageEvent e) {
        setFlags(MessageNode.convertMessageFlags(e.getMessageFlags()));
    }
    
	/**
	 * Adds a <tt>MessageNodeListener</tt> to the message node.
	 * 
	 * @param l The <tt>MessageNodeListener</tt> to be added.
	 */
    public void addMessageNodeListener(MessageNodeListener l) {
        synchronized(listenerList) {
            listenerList.add(MessageNodeListener.class, l);
        }
    }

    /**
     * Removes a <tt>MessageNodeListener</tt> from the message node.
     * 
     * @param l The <tt>MessageNodeListener</tt> to be removed.
     */
    public void removeMessageNodeListener(MessageNodeListener l) {
        synchronized(listenerList) {
            listenerList.remove(MessageNodeListener.class, l);
        }
    }
    
    /**
     * Returns an array of all <tt>MessageNodeListener</tt>s
     * that have been added to this message node.
     * 
     * @return All the <tt>MessageNodeListener</tt>s that have been added,
     * or an empty array if no listeners have been added.
     */
    public MessageNodeListener[] getMessageNodeListeners() {
        synchronized(listenerList) {
            return (MessageNodeListener[])listenerList.getListeners(MessageNodeListener.class);
        }
    }
    
    /**
     * Notifies all registered <tt>MessageNodeListener</tt>s that
     * the message status has changed.
     * 
     * @param type The type of the status change.
     */
    protected void fireMessageStatusChanged(int type) {
        synchronized(listenerList) {
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

    /**
     * Convert a protocol-later message flags object into the bit-field
     * representation needed for the object model.
     * 
     * @param messageFlags Message flags object.
     * @return Bit-field message flags.
     */
    static int convertMessageFlags(MessageFlags messageFlags) {
        // These two message flag representations are now identical in format.
        // This fake conversion step remains to minimize code impact.
		return messageFlags.getFlags();
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
        // These two message flag representations are now identical in format.
        // This fake conversion step remains to minimize code impact.
		MessageFlags messageFlags = new MessageFlags();
		messageFlags.setFlags(flags);
		return messageFlags;
	}
}
