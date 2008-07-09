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

import net.rim.device.api.util.Arrays;

/**
 * This class encapsulates all the data to represent an E-Mail message.
 */
public class Message {
    private MessageEnvelope envelope;
    private MessagePart body;
    
    /**
     * Creates a new instance of Message
     * @param envelope The envelope for the message
     * @param body The structured message body tree
     */
    public Message(MessageEnvelope envelope, MessagePart body) {
        this.envelope = envelope;
        this.body = body;
    }
    
    /**
     * Get the message envelope
     * @return Envelope
     */
    public MessageEnvelope getEnvelope() {
        return envelope;
    }

    /**
     * Get the message body
     * @return Body
     */
    public MessagePart getBody() {
        return body;
    }

    /**
     * Get a message that represents a reply to the original message.
     * @return Reply message
     */
    public Message toReplyMessage() {
        // Generate the reply message body
        MessageReplyConverter replyConverter = new MessageReplyConverter(this.envelope);
        if(this.body != null) {
            this.body.accept(replyConverter);
        }
        MessagePart replyBody = replyConverter.toReplyBody();
        MessageEnvelope replyEnvelope = createReplyEnvelope();

        Message result = new Message(replyEnvelope, replyBody);
        return result;
    }

    /**
     * Create the envelope for a reply to this message
     * @return Envelope
     */
    private MessageEnvelope createReplyEnvelope() {
        // Generate the reply message envelope
        MessageEnvelope replyEnvelope = new MessageEnvelope();

        // Set the reply subject
        if(envelope.subject.startsWith("Re:") || envelope.subject.startsWith("re:")) {
            replyEnvelope.subject = envelope.subject;
        }
        else {
            replyEnvelope.subject = "Re: " + envelope.subject;
        }
        
        // Set the message recipient
        int i;
        if(envelope.replyTo == null || envelope.replyTo.length == 0) {
            if(envelope.sender == null || envelope.sender.length == 0) {
                replyEnvelope.to = new String[envelope.from.length];
                for(i=0; i<envelope.from.length; i++) {
                    replyEnvelope.to[i] = envelope.from[i];
                }
            }
            else {
                replyEnvelope.to = new String[envelope.sender.length];
                for(i=0; i<envelope.sender.length; i++) {
                    replyEnvelope.to[i] = envelope.sender[i];
                }
            }
        }
        else {
            replyEnvelope.to = new String[envelope.replyTo.length];
            for(i=0; i<envelope.replyTo.length; i++) {
                replyEnvelope.to[i] = envelope.replyTo[i];
            }
        }

        // Finally, set the message in-reply-to ID
        replyEnvelope.inReplyTo = envelope.messageId;
        return replyEnvelope;
    }
    
    /**
     * Get a message that represents a reply to all the recipients
     * of the original message.
     * @param myAddress Address of the person doing the reply-all, to avoid
     *                  being sent a copy of the outgoing message.
     * @return Reply-All message
     */
    public Message toReplyAllMessage(String myAddress) {
        // Generate the reply message body
        MessageReplyConverter replyConverter = new MessageReplyConverter(this.envelope);
        if(this.body != null) {
            this.body.accept(replyConverter);
        }
        MessagePart replyBody = replyConverter.toReplyBody();
        MessageEnvelope replyEnvelope = createReplyEnvelope();
        
        // Then handle the additional fields for the reply-all case
        // How do we get myAddress here?
        int i;
        if(envelope.to != null) {
            for(i=0; i<envelope.to.length; i++) {
                if(envelope.to[i].toLowerCase().indexOf(myAddress) == -1) {
                    if(replyEnvelope.to == null) {
                        replyEnvelope.to = new String[1];
                        replyEnvelope.to[0] = envelope.to[i];
                    }
                    else {
                        Arrays.add(replyEnvelope.to, envelope.to[i]);
                    }
                }
            }
        }
        if(envelope.cc != null) {
            for(i=0; i<envelope.cc.length; i++) {
                if(envelope.cc[i].toLowerCase().indexOf(myAddress) == -1) {
                    if(replyEnvelope.cc == null) {
                        replyEnvelope.cc = new String[1];
                        replyEnvelope.cc[0] = envelope.cc[i];
                    }
                    else {
                        Arrays.add(replyEnvelope.cc, envelope.cc[i]);
                    }
                }
            }
        }
        
        Message result = new Message(replyEnvelope, replyBody);
        return result;
    }

    /**
     * Get a message that represents a forwarding of the original message.
     * The resulting header will be clean, aside from the subject.
     * The resulting body will be the same as a reply message (single TextPart),
     * aside from some of the original header fields being prepended.
     *
     * @return Forwarded message
     */
    public Message toForwardMessage() {
        // Generate the forward message body
        MessageForwardConverter forwardConverter = new MessageForwardConverter(this.envelope);
        if(this.body != null) {
            this.body.accept(forwardConverter);
        }
        MessagePart forwardBody = forwardConverter.toForwardBody();

        MessageEnvelope forwardEnvelope = new MessageEnvelope();
        
        // Set the forward subject
        if(envelope.subject.toLowerCase().startsWith("fwd:")) {
            forwardEnvelope.subject = envelope.subject;
        }
        else {
            forwardEnvelope.subject = "Fwd: " + envelope.subject;
        }
        
        Message result = new Message(forwardEnvelope, forwardBody);
        return result;
    }
}
