/*-
 * Copyright (c) 2009, octo
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

/**
 * Abstract representation of a message part representing content.
 */
public abstract class ContentPart extends MimeMessagePart {
	private String name;
	private String encoding;
	private String disposition;
	private String contentId;
	
	public ContentPart(String mimeType, String mimeSubtype, String name, String encoding, String disposition, String contentId, int size, String tag) {
        super(mimeType, mimeSubtype, size, tag);
        this.name = name;
        this.encoding = encoding;
        this.disposition = disposition;
        this.contentId = contentId;
    }

	public ContentPart(String mimeType, String mimeSubtype, String name, String encoding, String disposition, String contentId, int size) {
        this(mimeType, mimeSubtype, name, encoding, disposition, contentId, size, "");
    }

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
    public String getEncoding() {
    	return encoding;
    }
    
    public void setEncoding(String encoding) {
    	this.encoding = encoding;
    }
    
    public String getDisposition() {
    	return disposition;
    }
    
    public void setDisposition(String disposition) {
    	this.disposition = disposition;
    }

    public String getContentId() {
    	return contentId;
    }
    
    public void setContentId(String contentId) {
    	this.contentId = contentId;
    }
    
	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.message.MimeMessagePart#serialize(java.io.DataOutput)
	 */
	public void serialize(DataOutput output) throws IOException {
		super.serialize(output);
		output.writeUTF(name);
		output.writeUTF(encoding);
		output.writeUTF(disposition);
		output.writeUTF(contentId);
	}
	
	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.message.MimeMessagePart#deserialize(java.io.DataInput)
	 */
	public void deserialize(DataInput input) throws IOException {
		super.deserialize(input);
		name = input.readUTF();
		encoding = input.readUTF();
		disposition = input.readUTF();
		contentId = input.readUTF();
	}
}
