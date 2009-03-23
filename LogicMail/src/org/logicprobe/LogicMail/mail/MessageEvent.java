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
import org.logicprobe.LogicMail.message.MessageFlags;

/**
 * Object for message events.
 */
public class MessageEvent extends MailStoreEvent {
	private MessageToken messageToken;
	private MessageFlags messageFlags;
	private Message message;
	private String messageSource;
	
	/** Creates a new instance of MessageEvent */
	public MessageEvent(Object source, MessageToken messageToken, MessageFlags messageFlags, Message message, String messageSource) {
		super(source);
		this.messageToken = messageToken;
		this.messageFlags = messageFlags;
		this.message = message;
		this.messageSource = messageSource;
	}

	/** Creates a new instance of MessageEvent */
	public MessageEvent(Object source, MessageToken messageToken, Message message, String messageSource) {
		this(source, messageToken, null, message, messageSource);
	}

	/** Creates a new instance of MessageEvent */
	public MessageEvent(Object source, MessageToken messageToken, Message message) {
		this(source, messageToken, null, message, null);
	}

	/** Creates a new instance of MessageEvent */
	public MessageEvent(Object source, MessageToken messageToken) {
		this(source, messageToken, null, null, null);
	}

	/** Creates a new instance of MessageEvent */
	public MessageEvent(Object source, MessageToken messageToken, MessageFlags messageFlags) {
		this(source, messageToken, messageFlags, null, null);
	}
	
	/**
	 * Gets the token for the message this event applies to.
	 */
	public MessageToken getMessageToken() {
		return messageToken;
	}

	/**
	 * Gets the updated message flags, if they are available for this particular event.
	 */
	public MessageFlags getMessageFlags() {
		return messageFlags;
	}
	
	/**
	 * Gets the message itself, if it is available for this particular event.
	 */
	public Message getMessage() {
		return message;
	}

	/**
	 * Gets the message source, if it is available for this particular event.
	 */
	public String getMessageSource() {
		return messageSource;
	}
}
