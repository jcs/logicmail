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

/**
 * This class provides a message in the context of a folder.
 * It contains the message's envelope, along with other information
 * only relevant when looking at a view of the folder.
 */
public class FolderMessage {
    private MessageEnvelope envelope;
    private int index;
    private int uid;
    private MessageFlags messageFlags;
    
    /**
     * Creates a new instance of FolderMessage.
     * @param envelope The message's envelope
     * @param index The index of the message within the folder
     */
    public FolderMessage(MessageEnvelope envelope, int index, int uid) {
        this.envelope = envelope;
        this.index = index;
        this.uid = uid;
        this.messageFlags = new MessageFlags();
    }

    /**
     * Get the envelope associated with this message.
     * @return Message envelope
     */
    public MessageEnvelope getEnvelope() {
        return envelope;
    }

    /**
     * Get the mailbox index of this message
     * @return index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Get the unique ID of this message
     * @return unique ID
     */
    public int getUid() {
    	return uid;
    }
    
    /**
     * Gets the flags associated with this message.
     * @return Message flags
     */
    public MessageFlags getFlags() {
    	return messageFlags;
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
}
