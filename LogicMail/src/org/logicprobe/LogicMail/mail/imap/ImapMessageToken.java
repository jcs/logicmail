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

package org.logicprobe.LogicMail.mail.imap;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.logicprobe.LogicMail.mail.FolderTreeItem;
import org.logicprobe.LogicMail.mail.MessageToken;
import org.logicprobe.LogicMail.util.UniqueIdGenerator;

/**
 * Token for messages stored on IMAP servers.
 */
public class ImapMessageToken implements MessageToken {
    private long uniqueId;
    private int hashCode = -1;
    
    /** Complete path of the folder this message is stored within */
    private String folderPath;
    /** IMAP unique ID for referencing the message */
    private int messageUid;
    
    /**
     * Instantiates a new IMAP message token for deserialization.
     */
    public ImapMessageToken() {
    	this.uniqueId = UniqueIdGenerator.getInstance().getUniqueId();
	}
	
    /**
     * Instantiates a new IMAP message token.
     * 
     * @param folderPath the complete path of the folder this message is stored within.
     * @param messageUid the IMAP unique ID for referencing the message.
     */
    ImapMessageToken(String folderPath, int messageUid) {
    	this();
    	this.folderPath = folderPath;
    	this.messageUid = messageUid;
    }
    
    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.mail.MessageToken#containedWithin(org.logicprobe.LogicMail.mail.FolderTreeItem)
     */
    public boolean containedWithin(FolderTreeItem folderTreeItem) {
		return (folderPath != null) && folderPath.equals(folderTreeItem.getPath());
	}
    
    /**
     * Gets the complete path of the folder this message is stored within.
     * 
     * @return Complete folder path.
     */
    public String getFolderPath() {
    	return this.folderPath;
    }

    /**
     * IMAP unique ID for referencing the message.
     * 
     * @return IMAP unique ID.
     */
    int getImapMessageUid() {
    	return this.messageUid;
    }
    
    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.mail.MessageToken#getMessageUid()
     */
    public String getMessageUid() {
    	return Integer.toHexString(messageUid).toLowerCase();
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
		output.writeUTF(folderPath);
		output.writeInt(messageUid);
	}
	
	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.util.Serializable#deserialize(java.io.DataInput)
	 */
	public void deserialize(DataInput input) throws IOException {
		uniqueId = input.readLong();
		folderPath = input.readUTF();
		messageUid = input.readInt();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if(obj instanceof ImapMessageToken) {
			ImapMessageToken rhs = (ImapMessageToken)obj;
			return (this.folderPath.equals(rhs.folderPath) &&
					this.messageUid == rhs.messageUid);
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
			hash = 31 * hash + (folderPath == null ? 0 : folderPath.hashCode());
			hash = 31 * hash + messageUid;
			hashCode = hash;
		}
		return hashCode;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("ImapMessageToken [");
		buf.append("uniqueId="); buf.append(uniqueId);
		buf.append(", folderPath=\""); buf.append(folderPath); buf.append("\"");
		buf.append(", messageUid="); buf.append(messageUid);
		buf.append("]");
		return buf.toString();
	}
	
	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.mail.MessageToken#updateToken(org.logicprobe.LogicMail.mail.MessageToken)
	 */
	public void updateToken(MessageToken messageToken) {
	    // IMAP tokens are complete and only have immutable data
	}

	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.mail.MessageToken#isLoadable()
	 */
	public boolean isLoadable() {
	    return true;
	}
}
