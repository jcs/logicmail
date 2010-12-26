/*-
 * Copyright (c) 2006, Derek Konigsberg
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

package org.logicprobe.LogicMail.message;

import net.rim.device.api.util.Comparator;

import org.logicprobe.LogicMail.mail.MessageToken;
import org.logicprobe.LogicMail.util.SerializationUtils;
import org.logicprobe.LogicMail.util.UniqueIdGenerator;

/**
 * This class provides a message in the context of a folder.
 * It contains the message's envelope, along with other information
 * only relevant when looking at a view of the folder.
 * Also optionally contains the message structure tree, for
 * use with protocols that support independent retrieval
 * of the structure from the content.
 */
public class FolderMessage {
    private long uniqueId;
    private MessageToken messageToken;
    private MessageEnvelope envelope;
    private int index;
    private int uid;
    private int size;
    private MessageFlags messageFlags;
    private MimeMessagePart structure;
    private byte[] serializedStructure;

    private static final FolderMessageComparator comparator = new FolderMessageComparator();
    
    /**
     * Creates a new empty instance of FolderMessage.
     */
    public FolderMessage() {
        this.uniqueId = UniqueIdGenerator.getInstance().getUniqueId();
        this.envelope = new MessageEnvelope();
        this.messageFlags = new MessageFlags();
    }
    
    /**
     * Creates a new instance of FolderMessage.
     * @param messageToken the token representing the message within the store
     * @param envelope the message's envelope
     * @param index the index of the message within the folder
     * @param uid the unique ID of the message
     * @param size the size of the message, in bytes, or <code>-1</code> if unavailable
     */
    public FolderMessage(MessageToken messageToken, MessageEnvelope envelope, int index, int uid, int size) {
    	this();
    	this.messageToken = messageToken;
        this.envelope = envelope;
        this.index = index;
        this.uid = uid;
        this.size = size;
    }

    /**
     * Creates a new instance of FolderMessage, setting the contents from a
     * persistable container.
     *
     * @param persistable the persistable container
     */
    public FolderMessage(PersistableFolderMessage persistable) {
        setFromPersistable(persistable);
    }
    
    /**
     * Gets the comparator used to compare messages for insertion ordering.
     *
     * @return the comparator
     */
    public static Comparator getComparator() {
        return comparator;
    }
    
    /**
     * Gets the token for referencing this message.
     * 
     * @return The message token.
     */
    public MessageToken getMessageToken() {
    	return messageToken;
    }
    
    /**
     * Get the envelope associated with this message.
     * @return Message envelope
     */
    public MessageEnvelope getEnvelope() {
        return envelope;
    }

    /**
     * Get the mailbox index of this message.
     * @return index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Sets the mailbox index of this message.
     * @param index the new index
     */
    public void setIndex(int index) {
        this.index = index;
    }
    
    /**
     * Get the unique ID of this message
     * @return unique ID
     */
    public int getUid() {
    	return uid;
    }
    
    /**
     * Gets the size of this message.
     * This is intended to be the total size of the RFC822 source from which
     * the message is parsed. 
     * @return the size, in bytes, or <code>-1</code> if not available
     */
    public int getSize() {
        return size;
    }
    
    /**
     * Gets the flags associated with this message.
     * @return Message flags
     */
    public MessageFlags getFlags() {
    	return messageFlags;
    }
    
    /**
     * Sets the flags associated with this message.
     * @param messageFlags the new message flags
     */
    public void setFlags(MessageFlags messageFlags) {
        this.messageFlags = messageFlags;
    }
    
    /**
     * Find out whether this message has been previously viewed
     */
    public boolean isSeen() {
        return messageFlags.isSeen();
    }

    /**
     * Set the flag indicating whether this message has been previously viewed
     */
    public void setSeen(boolean seen) {
    	messageFlags.setSeen(seen);
    }

    /**
     * Find out whether this message has been replied to
     */
    public boolean isAnswered() {
        return messageFlags.isAnswered();
    }

    /**
     * Set the flag indicating whether this message has been replied to
     */
    public void setAnswered(boolean answered) {
    	messageFlags.setAnswered(answered);
    }

    /**
     * Find out whether this message has been flagged
     */
    public boolean isFlagged() {
        return messageFlags.isFlagged();
    }

    /**
     * Set the flag indicating whether this message has been flagged
     */
    public void setFlagged(boolean flagged) {
    	messageFlags.setFlagged(flagged);
    }

    /**
     * Find out whether this message has been marked as deleted
     */
    public boolean isDeleted() {
        return messageFlags.isDeleted();
    }

    /**
     * Set the flag indicating whether this message has been marked as deleted
     */
    public void setDeleted(boolean deleted) {
    	messageFlags.setDeleted(deleted);
    }

    /**
     * Find out whether this message is a draft
     */
    public boolean isDraft() {
        return messageFlags.isDraft();
    }

    /**
     * Set the flag indicating whether this message is a draft
     */
    public void setDraft(boolean draft) {
    	messageFlags.setDraft(draft);
    }

    /**
     * Find out whether this message has recently arrived
     */
    public boolean isRecent() {
        return messageFlags.isRecent();
    }

    /**
     * Set the flag indicating whether this message has recently arrived
     */
    public void setRecent(boolean recent) {
    	messageFlags.setRecent(recent);
    }
    
    /**
     * Find out whether this message has been forwarded
     */
    public boolean isForwarded() {
        return messageFlags.isForwarded();
    }
    
    /**
     * Set the flag indicating whether this message has been forwarded
     */
    public void setForwarded(boolean forwarded) {
        messageFlags.setForwarded(forwarded);
    }
    
    /**
     * Find out whether this message has been flagged as junk
     */
    public boolean isJunk() {
        return messageFlags.isJunk();
    }

    /**
     * Set the flag indicating whether this message has been flagged as junk
     */
    public void setJunk(boolean junk) {
    	messageFlags.setJunk(junk);
    }

    /**
     * Gets the structure of the message, if available.
     * @return Root part of the message structure tree
     */
    public MimeMessagePart getStructure() {
        synchronized(this) {
            if(serializedStructure != null) {
                this.structure = (MimeMessagePart)SerializationUtils.deserializeClass(serializedStructure);
                serializedStructure = null;
            }
        }
    	return this.structure;
    }

    /**
     * Sets the structure of the message.
     * @param structure Root part of the message structure tree
     */
    public void setStructure(MimeMessagePart structure) {
    	this.structure = structure;
    }
    
	public long getUniqueId() {
		return this.uniqueId;
	}

    /**
     * Gets a persistable container populated with the contents of this object.
     *
     * @return the persistable container
     */
    public PersistableFolderMessage getPersistable() {
        PersistableFolderMessage result = new PersistableFolderMessage();
        result.setElement(PersistableFolderMessage.FIELD_UNIQUEID, new Long(uniqueId));
        result.setElement(PersistableFolderMessage.FIELD_MESSAGETOKEN, messageToken.clone());
        result.setElement(PersistableFolderMessage.FIELD_ENVELOPE, envelope.getPersistable());
        result.setElement(PersistableFolderMessage.FIELD_INDEX, new Integer(index));
        result.setElement(PersistableFolderMessage.FIELD_UID, new Integer(uid));
        result.setElement(PersistableFolderMessage.FIELD_SIZE, new Integer(size));
        result.setElement(PersistableFolderMessage.FIELD_MESSAGEFLAGS, new Integer(messageFlags.getFlags()));
        
        MimeMessagePart structurePart = getStructure();
        if(structurePart != null) {
            result.setElement(PersistableFolderMessage.FIELD_STRUCTURE, SerializationUtils.serializeClass(structurePart));
        }
        return result;
    }

    private void setFromPersistable(PersistableFolderMessage persistable) {
        Object value;
        value = persistable.getElement(PersistableFolderMessage.FIELD_UNIQUEID);
        if(value instanceof Long) { this.uniqueId = ((Long)value).longValue(); }
        
        value = persistable.getElement(PersistableFolderMessage.FIELD_MESSAGETOKEN);
        if(value instanceof MessageToken) {
            this.messageToken = ((MessageToken)value).clone();
        }
        
        value = persistable.getElement(PersistableFolderMessage.FIELD_ENVELOPE);
        if(value instanceof PersistableMessageEnvelope) {
            this.envelope = new MessageEnvelope((PersistableMessageEnvelope)value);
        }
        else {
            this.envelope = new MessageEnvelope();
        }
        
        value = persistable.getElement(PersistableFolderMessage.FIELD_INDEX);
        if(value instanceof Integer) { this.index = ((Integer)value).intValue(); }
        
        value = persistable.getElement(PersistableFolderMessage.FIELD_UID);
        if(value instanceof Integer) { this.uid = ((Integer)value).intValue(); }
        
        value = persistable.getElement(PersistableFolderMessage.FIELD_SIZE);
        if(value instanceof Integer) { this.size = ((Integer)value).intValue(); }
        
        value = persistable.getElement(PersistableFolderMessage.FIELD_MESSAGEFLAGS);
        if(value instanceof Integer) {
            this.messageFlags = new MessageFlags(((Integer)value).intValue());
        }
        else {
            this.messageFlags = new MessageFlags();
        }
        
        value = persistable.getElement(PersistableFolderMessage.FIELD_STRUCTURE);
        if(value instanceof byte[]) {
            this.serializedStructure = (byte[])value;
        }
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return 31 * 1 + (int) (uniqueId ^ (uniqueId >>> 32));
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        FolderMessage other = (FolderMessage) obj;
        if (uniqueId != other.uniqueId) {
            return false;
        }
        return true;
    }
	
	private static class FolderMessageComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            if(o1 instanceof FolderMessage && o2 instanceof FolderMessage) {
                FolderMessage message1 = (FolderMessage)o1;
                FolderMessage message2 = (FolderMessage)o2;
                if(message1.index < message2.index) {
                    return -1;
                }
                else if(message1.index > message2.index) {
                    return 1;
                }
                else {
                    return 0;
                }
            }
            else {
                throw new ClassCastException("Cannot compare types");
            }
        }
	}
}
