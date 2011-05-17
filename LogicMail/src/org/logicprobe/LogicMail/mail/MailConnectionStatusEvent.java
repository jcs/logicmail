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

import org.logicprobe.LogicMail.conf.ConnectionConfig;

/**
 * Object for mail connection status events.
 */
public class MailConnectionStatusEvent extends MailConnectionEvent {
	private final String message;
	private final int progressType;
	private final int progressPercentage;
	private final ConnectionHandlerRequest request;
	
	/** The progress is indeterminate. */
	public static final int PROGRESS_INDETERMINATE = 0;
	/** The progress is measured incrementally. */
	public static final int PROGRESS_MEASURED = 0;
	
	/**
	 * Creates a new instance of MailConnectionStatusEvent.
	 * 
	 * @param source the source
	 * @param connectionConfig the connection configuration instance
	 * @param request the request-in-progress that status is being provided for
	 * @param message the status message
	 * @param progressType the progress type
	 * @param progressPercentage the progress percentage
	 */
	public MailConnectionStatusEvent(Object source, ConnectionConfig connectionConfig, ConnectionHandlerRequest request, String message, int progressType, int progressPercentage) {
		super(source, connectionConfig);
		this.request = request;
		this.message = message;
		this.progressType = progressType;
		this.progressPercentage = progressPercentage;
	}
	
	/**
	 * Creates a new instance of MailConnectionStatusEvent
	 * with indeterminate progress.
	 * 
	 * @param source the source
	 * @param connectionConfig the connection configuration instance
	 * @param request the request-in-progress that status is being provided for
	 * @param message the status message
	 */
	public MailConnectionStatusEvent(Object source, ConnectionConfig connectionConfig, ConnectionHandlerRequest request, String message) {
		this(source, connectionConfig, request, message, PROGRESS_INDETERMINATE, 0);
	}

	/**
	 * Gets the request-in-progress that status is being provided for.
	 *
	 * @return the request-in-progress, or <code>null</code> if not applicable
	 */
	public ConnectionHandlerRequest getRequest() {
        return request;
    }
	
	/**
	 * Gets the status message.
	 * 
	 * @return Status message
	 */
	public String getMessage() {
		return message;
	}
	
	/**
	 * Gets the progress type.
	 * 
	 * @return The progress type.
	 */
	public int getProgressType() {
		return progressType;
	}

	/**
	 * Gets the progress percentage.
	 * Only valid if the progress type is <tt>PROGRESS_MEASURED</tt>.
	 * 
	 * @return The progress percentage.
	 */
	public int getProgressPercentage() {
		return progressPercentage;
	}
}
