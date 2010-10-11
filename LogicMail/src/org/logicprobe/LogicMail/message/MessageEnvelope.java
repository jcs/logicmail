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
import java.util.Calendar;
import java.util.Date;

import org.logicprobe.LogicMail.util.Serializable;
import org.logicprobe.LogicMail.util.UniqueIdGenerator;

/**
 * The "envelope" of a mail message.
 * Essentially, this is a usable parsed form
 * of some parts of the message headers
 */
public class MessageEnvelope implements Serializable {
    private long uniqueId;
    public Date date;
    public String subject;
    public String[] from;
    public String[] sender;
    public String[] replyTo;
    public String[] to;
    public String[] cc;
    public String[] bcc;
    public String inReplyTo;
    public String messageId;

    /** Creates a new instance of MessageEnvelope */
    public MessageEnvelope() {
        uniqueId = UniqueIdGenerator.getInstance().getUniqueId();
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.util.Serializable#getUniqueId()
     */
    public long getUniqueId() {
        return uniqueId;
	}

	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.util.Serializable#serialize(java.io.DataOutput)
	 */
	public void serialize(DataOutput output) throws IOException {
		output.writeLong(uniqueId);
        output.writeLong(date.getTime());
        output.writeUTF((subject != null) ? subject : "");
        serializeArray(output, from);
        serializeArray(output, sender);
        serializeArray(output, replyTo);
        serializeArray(output, to);
        serializeArray(output, cc);
        serializeArray(output, bcc);
        output.writeUTF((inReplyTo != null) ? inReplyTo : "");
        output.writeUTF((messageId != null) ? messageId : "");
	}

	private static void serializeArray(DataOutput output, String[] array) throws IOException {
	    if(array == null) {
	        output.writeInt(0);
	    }
	    else {
    		output.writeInt(array.length);
    		for(int i=0; i<array.length; i++) {
    			output.writeUTF(array[i]);
    		}
	    }
	}
	
	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.util.Serializable#deserialize(java.io.DataInput)
	 */
	public void deserialize(DataInput input) throws IOException {
		uniqueId = input.readLong();
        long dateValue = input.readLong();
        date = Calendar.getInstance().getTime();
        date.setTime(dateValue);
        
        subject = input.readUTF();
        from = deserializeArray(input);
        sender = deserializeArray(input);
        replyTo = deserializeArray(input);
        to = deserializeArray(input);
        cc = deserializeArray(input);
        bcc = deserializeArray(input);
        inReplyTo = input.readUTF();
        messageId = input.readUTF();
	}

	private static String[] deserializeArray(DataInput input) throws IOException {
		int length = input.readInt();
		String[] result = new String[length];
		for(int i=0; i<length; i++) {
			result[i] = input.readUTF();
		}
		return result;
	}
}
