/*-
 * Copyright (c) 2009, Derek Konigsberg
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

package org.logicprobe.LogicMail;

/**
 * The notification event source class.
 */
public final class LogicMailEventSource {
	private String eventSourceName;
	private String accountName;
	private long accountUniqueId;
	private long eventSourceId;
	
	/**
	 * Instantiates a new event source.
	 * 
	 * @param accountName The account name
	 * @param accountUniqueId The account configuration's unique id
	 */
	public LogicMailEventSource(String accountName, long accountUniqueId) {
		StringBuffer buf = new StringBuffer();
		buf.append("LogicMail [");
		buf.append(accountName);
		buf.append("]");
		this.eventSourceName = buf.toString();
		this.accountName = accountName;
		this.accountUniqueId = accountUniqueId;
		this.eventSourceId = AppInfo.GUID + this.accountUniqueId;
	}
	
	/**
	 * Gets the account name.
	 * 
	 * @return The account name
	 */
	public String getAccountName() {
		return this.accountName;
	}
	
	/**
	 * Gets the account unique id.
	 * 
	 * @return The account unique id
	 */
	public long getAccountUniqueId() {
		return this.accountUniqueId;
	}
	
	/**
	 * Gets the event source id.
	 * 
	 * @return The event source id
	 */
	public long getEventSourceId() {
		return eventSourceId;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
    	return eventSourceName;
	}
}
