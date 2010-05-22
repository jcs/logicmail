/*-
 * Copyright (c) 2008, Derek Konigsberg
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

package org.logicprobe.LogicMail.mail;

import org.logicprobe.LogicMail.message.Message;
import org.logicprobe.LogicMail.message.MessageEnvelope;

/**
 * Object for sent message events. 
 */
public class MessageSentEvent extends MailSenderEvent {
	private MessageEnvelope envelope;
	private Message message;
	private String messageSource;
	private Throwable exception;
	
	/** Creates a new instance of MessageSentEvent */
	public MessageSentEvent(Object source, MessageEnvelope envelope, Message message, String messageSource) {
		this(source, envelope, message, messageSource, null);
	}
	
    /** Creates a new instance of MessageSentEvent */
    public MessageSentEvent(Object source, MessageEnvelope envelope, Message message, Throwable exception) {
        this(source, envelope, message, null, exception);
    }
	
    private MessageSentEvent(Object source, MessageEnvelope envelope, Message message, String messageSource, Throwable exception) {
        super(source);
        this.envelope = envelope;
        this.message = message;
        this.messageSource = messageSource;
        this.exception = exception;
    }
	
	/**
	 * Gets the envelope for the message that was sent.
	 * 
	 * @return Sent message envelope.
	 */
	public MessageEnvelope getEnvelope() {
		return this.envelope;
	}
	
	/**
	 * Gets the message that was sent.
	 * 
	 * @return Sent message.
	 */
	public Message getMessage() {
		return this.message;
	}
	
	/**
	 * Gets the raw source for the message that was sent, if the sending
	 * operation was successful.
	 * This is the data that was actually sent to the mail server,
	 * and is intended to be used for saving copies of sent messages.
	 * 
	 * @return Message source, or null if the operation failed
	 */
	public String getMessageSource() {
		return this.messageSource;
	}
	
	/**
	 * If the message sending failed, this gets the exception describing
	 * the failure.
	 *
	 * @return the exception, or null if the operation succeeded
	 */
	public Throwable getException() {
        return exception;
    }
}
