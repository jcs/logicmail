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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.logicprobe.LogicMail.util.Serializable;
import org.logicprobe.LogicMail.util.UniqueIdGenerator;

/**
 * Abstract representation of a MIME message part
 */
public abstract class MimeMessagePart implements Serializable {
    private long uniqueId;
    private String tag;
    private String mimeType;
    private String mimeSubtype;
    private int size;
    private MimeMessagePart parent;

    /** Creates a new instance of MessagePart */
    protected MimeMessagePart(String mimeType, String mimeSubtype, int size, String tag) {
        this.uniqueId = UniqueIdGenerator.getInstance().getUniqueId();
        this.tag = tag;
        this.mimeType = mimeType;
        this.mimeSubtype = mimeSubtype;
        this.size = size;
        this.parent = null;
    }

    /**
     * Accept a visitor on this message part.
     * @param visitor The visitor instance
     */
    public abstract void accept(MimeMessagePartVisitor visitor);
    
    /**
     * Gets the tag used to embed protocol-specific address
     * information in the part.
     * 
     * @return The tag
     */
    public String getTag() {
    	return tag;
    }
    
    /**
     * Get the MIME type for this part
     * @return The "type" part of "type/subtype"
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Get the MIME subtype for this part
     * @return The "subtype" part of "type/subtype"
     */
    public String getMimeSubtype() {
        return mimeSubtype;
    }

    /**
     * Get the size of the content this part describes
     * @return Size if available, or -1 otherwise
     */
    public int getSize() {
    	return size;
    }
    
    /**
     * Set the parent of this part
     * @param parent The parent message part, or null if this is the root
     */
    protected void setParent(MimeMessagePart parent) {
        this.parent = parent;
    }
    
    /**
     * Get the parent of this part
     * @return Parent, or null if this is the root
     */
    public MimeMessagePart getParent() {
        return parent;
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
		output.writeUTF(tag);
		output.writeUTF(mimeType);
		output.writeUTF(mimeSubtype);
		output.writeInt(size);
	}
	
	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.util.Serializable#deserialize(java.io.DataInput)
	 */
	public void deserialize(DataInput input) throws IOException {
		uniqueId = input.readLong();
		tag = input.readUTF();
		mimeType = input.readUTF();
		mimeSubtype = input.readUTF();
		size = input.readInt();
	}
}
