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

import org.logicprobe.LogicMail.conf.OutgoingConfig;
import org.logicprobe.LogicMail.message.Message;
import org.logicprobe.LogicMail.message.MessageEnvelope;

public class NetworkMailSender extends AbstractMailSender {
	private OutgoingMailClient client;
	private OutgoingMailConnectionHandler connectionHandler;
	private OutgoingConfig outgoingConfig;

	public NetworkMailSender(OutgoingConfig outgoingConfig) {
		super();
		this.client = MailClientFactory.createOutgoingMailClient(outgoingConfig);
		this.outgoingConfig = outgoingConfig;
		this.connectionHandler = new OutgoingMailConnectionHandler(client);
		this.connectionHandler.start();
	}

    /**
	 * Gets the outgoing account configuration associated with this network mail sender.
	 * 
	 * @return Outgoing account configuration.
	 */
	public OutgoingConfig getOutgoingConfig() {
		return this.outgoingConfig;
	}
	
	public void shutdown(boolean wait) {
		connectionHandler.shutdown(wait);
	}

	/**
	 * Restarts the mail connection handler thread.
	 */
	public void restart() {
		if(!connectionHandler.isRunning()) {
			connectionHandler.start();
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
	    return this.outgoingConfig.toString();
	}
	
	public NetworkSendMessageRequest createSendMessageRequest(MessageEnvelope envelope, Message message) {
		NetworkSendMessageRequest request = new NetworkSendMessageRequest(this, envelope, message);
		return request;
	}
	
	public void processRequest(ConnectionHandlerRequest request) {
	    if(request instanceof NetworkSendMessageRequest) {
	        connectionHandler.addRequest(request);
	    }
	    else {
	        throw new IllegalArgumentException();
	    }
	}
}
