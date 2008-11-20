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
package org.logicprobe.LogicMail.model;

import org.logicprobe.LogicMail.mail.AbstractMailSender;
import org.logicprobe.LogicMail.message.FolderMessage;

/**
 * Provides a MessageNode object with the necessary data to support
 * outgoing messages.  This keeps all the information involved
 * in sending a message in one place, and provides the ability
 * to serialize unsent messages.
 */
public class OutgoingMessageNode extends MessageNode {
	private int messageId;
	private AccountNode sendingAccount;
	private AbstractMailSender mailSender;
	private MessageNode replyToMessageNode;

	/**
	 * Creates a new instance of OutgoingMessageNode.
	 * @param The FolderMessage being represented.
     * @param sendingAccount The account the message is being sent from.
     * @param mailSender The mail sender to be used for sending the message.
     * @param replyToMessageNode The message this is a reply to, if applicable.
     */
    OutgoingMessageNode(
    		FolderMessage folderMessage,
    		AccountNode sendingAccount,
    		AbstractMailSender mailSender,
    		MessageNode replyToMessageNode) {
    	super(folderMessage);
    	this.sendingAccount = sendingAccount;
    	this.mailSender = mailSender;
    	this.replyToMessageNode = replyToMessageNode;
    }

	/**
	 * Creates a new instance of OutgoingMessageNode.
	 * @param The FolderMessage being represented.
     * @param sendingAccount The account the message is being sent from.
     * @param mailSender The mail sender to be used for sending the message.
     */
    OutgoingMessageNode(
    		FolderMessage folderMessage,
    		AccountNode sendingAccount,
    		AbstractMailSender mailSender) {
    	this(folderMessage, sendingAccount, mailSender, null);
    }

    public int getId() {
    	return messageId;
    }

    public void setId(int messageId) {
    	this.messageId = messageId;
    }
    
    public AccountNode getSendingAccount() {
		return sendingAccount;
	}

	public void setMailSender(AbstractMailSender mailSender) {
		this.mailSender = mailSender;
	}

	public AbstractMailSender getMailSender() {
		return mailSender;
	}

	public MessageNode getReplyToMessageNode() {
		return replyToMessageNode;
	}
}
