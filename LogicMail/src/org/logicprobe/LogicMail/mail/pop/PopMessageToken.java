/*-
 * Copyright (c) 2009, Derek Konigsberg
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

package org.logicprobe.LogicMail.mail.pop;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.logicprobe.LogicMail.mail.FolderTreeItem;
import org.logicprobe.LogicMail.mail.MessageToken;
import org.logicprobe.LogicMail.util.UniqueIdGenerator;

/**
 * Token for messages stored on POP servers.
 * <p>
 * The longevity of the data in a POP message token is somewhat
 * uncertain, since POP message UIDs are only useful for identification,
 * not as a reference for commands.  Therefore, the message index values
 * across known tokens may need to be updated every time a server connection
 * is opened.
 * </p>
 */
public class PopMessageToken implements MessageToken {
    private long uniqueId;
    private int hashCode = -1;
    
    /** Index of the message within the mailbox */
    private int messageIndex;
    /** POP unique ID that identifies the message */
    private String messageUid;
    
    /**
     * Instantiates a new POP message token.
     */
    public PopMessageToken() {
    	this.uniqueId = UniqueIdGenerator.getInstance().getUniqueId();
	}
	
    /**
     * Instantiates a new POP message token.
     * 
     * @param messageIndex the index of the message within the mailbox
     * @param messageUid the POP unique ID that identifies the message
     */
    PopMessageToken(int messageIndex, String messageUid) {
    	this();
    	this.messageIndex = messageIndex;
    	this.messageUid = messageUid;
    }
    
    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.mail.MessageToken#containedWithin(org.logicprobe.LogicMail.mail.FolderTreeItem)
     */
    public boolean containedWithin(FolderTreeItem folderTreeItem) {
    	// POP does not have folders, so we always return true
		return true;
	}
    
    /**
     * Gets the index of the message within the mailbox.
     * 
     * @return the message index
     */
    int getMessageIndex() {
    	return this.messageIndex;
    }

    /**
     * Sets the index of the message within the mailbox.
     * 
     * @param messageIndex the new message index
     */
    void setMessageIndex(int messageIndex) {
    	this.messageIndex = messageIndex;
    }
    
    /**
     * Gets the POP unique ID that identifies the message.
     * 
     * @return POP unique ID
     */
    public String getMessageUid() {
    	return this.messageUid;
    }
    
	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.util.Serializable#getUniqueId()
	 */
	public long getUniqueId() {
		return this.uniqueId;
	}
	
	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.util.Serializable#serialize(java.io.DataOutput)
	 */
	public void serialize(DataOutput output) throws IOException {
		output.writeLong(uniqueId);
	}
	
	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.util.Serializable#deserialize(java.io.DataInput)
	 */
	public void deserialize(DataInput input) throws IOException {
		uniqueId = input.readLong();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if(obj instanceof PopMessageToken) {
			PopMessageToken rhs = (PopMessageToken)obj;
			return (this.messageIndex == rhs.messageIndex &&
					this.messageUid.equals(rhs.messageUid));
		}
		else {
			return false;
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		if(hashCode == -1) {
			int hash = 7;
			hash = 31 * hash + messageIndex;
			hash = 31 * hash + (messageUid == null ? 0 : messageUid.hashCode());
			hashCode = hash;
		}
		return hashCode;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("PopMessageToken [");
		buf.append("uniqueId="); buf.append(uniqueId);
		buf.append(", messageIndex="); buf.append(messageIndex);
		buf.append(", messageUid=\""); buf.append(messageUid); buf.append("\"");
		buf.append("]");
		return buf.toString();
	}
}
