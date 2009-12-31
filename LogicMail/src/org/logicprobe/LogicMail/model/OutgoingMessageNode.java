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
import org.logicprobe.LogicMail.mail.MessageToken;
import org.logicprobe.LogicMail.message.FolderMessage;

/**
 * Provides a MessageNode object with the necessary data to support
 * outgoing messages.  This keeps all the information involved
 * in sending a message in one place, and provides the ability
 * to serialize unsent messages.
 */
public class OutgoingMessageNode extends MessageNode {
	private AccountNode sendingAccount;
	private AbstractMailSender mailSender;
	private AccountNode replyToAccount;
	private MessageToken replyToToken;

	/**
	 * Creates a new instance of OutgoingMessageNode.
	 * This constructor is only intended for use in {@link MessageNodeReader}.
	 * 
	 * @param messageToken Token for the message
	 */
	OutgoingMessageNode(MessageToken messageToken) {
	    super(messageToken);
	}
	
	/**
	 * Creates a new instance of OutgoingMessageNode.
	 * 
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
    	if(replyToMessageNode != null) {
        	this.replyToAccount = replyToMessageNode.getParent().getParentAccount();
        	this.replyToToken = replyToMessageNode.getMessageToken();
    	}
    }

	/**
	 * Creates a new instance of OutgoingMessageNode.
	 * 
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

    public boolean refreshMessage() {
        // Refresh is a non-applicable operation for outgoing messages,
        // since they should be completely loaded when the outbox
        // is loaded.
        return false;
    }
    
    /**
     * Set the account that this message was sent from.
     * 
     * @param sendingAccount sending account
     */
    void setSendingAccount(AccountNode sendingAccount) {
        this.sendingAccount = sendingAccount;
    }
    
    /**
     * Get the account that this message was sent from.
     * 
     * @return sending account
     */
    public AccountNode getSendingAccount() {
		return sendingAccount;
	}

    /**
     * Set the mail sender that should be used to send this message.
     * 
     * @param mailSender mail sender
     */
	public void setMailSender(AbstractMailSender mailSender) {
		this.mailSender = mailSender;
	}

	/**
	 * Get the mail sender that should be used to send this message.
	 * 
	 * @return mail sender
	 */
	public AbstractMailSender getMailSender() {
		return mailSender;
	}

	/**
	 * Sets the Reply-To message account.
	 * 
	 * @param replyToAccount the new Reply-To message account
	 */
	void setReplyToAccount(AccountNode replyToAccount) {
	    this.replyToAccount = replyToAccount;
	}
	
	/**
	 * Sets the Reply-To message token.
	 * 
	 * @param replyToToken the new Reply-To message token
	 */
	void setReplyToToken(MessageToken replyToToken) {
	    this.replyToToken = replyToToken;
	}
	
	/**
	 * Gets the Reply-To message account.
	 * 
	 * @return the Reply-To message account
	 */
	public AccountNode getReplyToAccount() {
	    return replyToAccount;
	}
	
	/**
	 * Gets the Reply-To message token.
	 * 
	 * @return the Reply-To message token
	 */
	public MessageToken getReplyToToken() {
	    return replyToToken;
	}
}
