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

import org.logicprobe.LogicMail.message.MimeMessageContent;
import org.logicprobe.LogicMail.message.MessageFlags;
import org.logicprobe.LogicMail.message.MimeMessagePart;

/**
 * Object for message events.
 */
public class MessageEvent extends MailStoreEvent {
	private int type;
	private MessageToken messageToken;
	private MessageFlags messageFlags;
	private MimeMessagePart messageStructure;
	private MimeMessageContent[] mimeMessageContent;
	private String messageSource;
	
	/** The entire message has been loaded. */
	public final static int TYPE_FULLY_LOADED = 0;
	/** Only message content has been loaded. */
	public final static int TYPE_CONTENT_LOADED = 1;
	/** Message flags have changed. */
	public final static int TYPE_FLAGS_CHANGED = 3;
	
	/**
	 * Creates a new instance of MessageEvent.
	 * 
	 * @param source the source of the event
	 * @param type the type of the event
	 * @param messageToken the message token
	 * @param messageFlags the message flags
	 * @param messageStructure the message structure
	 * @param messageContent the message content
	 * @param messageSource the message source
	 */
	public MessageEvent(Object source, int type, MessageToken messageToken, MessageFlags messageFlags, MimeMessagePart messageStructure, MimeMessageContent[] messageContent, String messageSource) {
		super(source);
		this.type = type;
		this.messageToken = messageToken;
		this.messageFlags = messageFlags;
		this.messageStructure = messageStructure;
		this.mimeMessageContent = messageContent;
		this.messageSource = messageSource;
	}

	/**
	 * Creates a new instance of MessageEvent to indicate that some of
	 * the message content has been loaded.
	 * 
	 * @param source the source of the event
	 * @param messageToken the message token
	 * @param messageContent the loaded message content
	 */
	public MessageEvent(Object source, MessageToken messageToken, MimeMessageContent[] messageContent) {
		this(source, TYPE_CONTENT_LOADED, messageToken, null, null, messageContent, null);
	}

	/**
	 * Creates a new instance of MessageEvent to indicate that all possible
	 * message content has been loaded, as well as the raw source for the message.
	 * 
	 * @param source the source of the event
	 * @param messageToken the message token
	 * @param messageStructure the message structure
	 * @param messageContent the message content
	 * @param messageSource the message source
	 */
	public MessageEvent(Object source, MessageToken messageToken, MimeMessagePart messageStructure, MimeMessageContent[] messageContent, String messageSource) {
		this(source, TYPE_FULLY_LOADED, messageToken, null, messageStructure, messageContent, messageSource);
	}

	/**
	 * Creates a new instance of MessageEvent to indicate that the
	 * message flags have changed.
	 * 
	 * @param source the source of the event
	 * @param messageToken the message token
	 * @param messageFlags the message flags
	 */
	public MessageEvent(Object source, MessageToken messageToken, MessageFlags messageFlags) {
		this(source, TYPE_FLAGS_CHANGED, messageToken, messageFlags, null, null, null);
	}
	
	/**
	 * Gets the type of this message event.
	 * 
	 * @return Event type.
	 */
	public int getType() {
		return this.type;
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
	 * Gets the message structure, if it is available for this particular event.
	 */
	public MimeMessagePart getMessageStructure() {
		return messageStructure;
	}

	/**
	 * Get the message content, if it is available for this particular event.
	 * This should be all the content that was just loaded, and may not necessarily
	 * be all the content that could be loaded for the message.
	 */
	public MimeMessageContent[] getMessageContent() {
		return mimeMessageContent;
	}
	
	/**
	 * Gets the message source, if it is available for this particular event.
	 */
	public String getMessageSource() {
		return messageSource;
	}
}
