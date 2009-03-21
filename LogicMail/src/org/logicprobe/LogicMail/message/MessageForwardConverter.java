/*-
 * Copyright (c) 2007, Derek Konigsberg
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

import java.util.Date;

import org.logicprobe.LogicMail.util.StringParser;

/**
 * Converts a message into a forwarding of that message.
 * This class works by finding the first text/plain part of the message,
 * and generating a new text/plain part that has the same contents,
 * prefixed by some header fields and wrapped in forwarding indicators.
 */
public class MessageForwardConverter implements MessagePartVisitor {
    private TextPart originalTextPart;
    private String subject;
    private Date date;
    private String fromString;
    private String toString;
    private String ccString;
    
    /**
     * Creates a new instance of MessageForwardConverter.
     * @param subject Subject of the message
     * @param date Date the message was sent
     * @param from "From" field of the message
     * @param to "To" field of the message
     * @param cc "CC" field of the message
     */
    public MessageForwardConverter(String subject, Date date, String[] from, String[] to, String[] cc) {
    	this.subject = subject;
    	this.date = date;
    	this.fromString = StringParser.makeCsvString(from);
    	this.toString = StringParser.makeCsvString(to);
    	this.ccString = StringParser.makeCsvString(cc);
    }
    
    /**
     * Creates a new instance of MessageForwardConverter.
     * @param envelope Envelope of the message to convert
     */
    public MessageForwardConverter(MessageEnvelope envelope) {
    	this(envelope.subject, envelope.date, envelope.from, envelope.to, envelope.cc);
    }

    /**
     * Returns a reply message body based on the visited message structure.
     * @return Message body
     */
    public MessagePart toForwardBody() {
        StringBuffer buf = new StringBuffer();

        // Create the first line of the reply text
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
        if(originalTextPart != null) {
            buf.append(originalTextPart.getText());
            buf.append("\r\n");
        }
        
        // Add the footer
        buf.append("------------------------");
        
        // Return the final result of the buffer
        return new TextPart("plain", buf.toString());
    }
    
    public void visitMultiPart(MultiPart part) {
    }

    public void visitTextPart(TextPart part) {
        // If this is the first text part, then use it
        // for the reply message body
        if(originalTextPart == null) {
            originalTextPart = part;
        }
    }

    public void visitImagePart(ImagePart part) {
    }

    public void visitUnsupportedPart(UnsupportedPart part) {
    }
    
}
