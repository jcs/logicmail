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

import org.logicprobe.LogicMail.util.StringParser;

/**
 * Converts a message into a reply to that message.
 * This class works by finding the first text/plain part of the message,
 * and generating a new text/plain part that has the same contents, with
 * each line prefixed by a "> ".
 */
public class MessageReplyConverter implements MessagePartVisitor {
    private TextPart originalTextPart;
    private MessageEnvelope envelope;
    
    /**
     * Creates a new instance of MessageReplyConverter.
     * @param envelope Envelope of the message to convert
     */
    public MessageReplyConverter(MessageEnvelope envelope) {
        this.envelope = envelope;
    }

    /**
     * Returns a reply message body based on the visited message structure.
     * @return Message body
     */
    public MessagePart toReplyBody() {
        StringBuffer buf = new StringBuffer();
        int p;
        
        // Create the first line of the reply text
        buf.append("On ");
        buf.append(StringParser.createDateString(envelope.date));
        buf.append(", ");
        if(envelope.sender != null && envelope.sender.length > 0) {
            p = envelope.sender[0].indexOf('<');
            if(p > 0) {
                buf.append(envelope.sender[0].substring(0, p-1));
            }
            else {
                buf.append(envelope.sender[0]);
            }
        }
        buf.append(" wrote:\r\n");
        
        // Generate the quoted message text
        buf.append("> ");
        if(originalTextPart != null) {
            String originalText = originalTextPart.getText();
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
