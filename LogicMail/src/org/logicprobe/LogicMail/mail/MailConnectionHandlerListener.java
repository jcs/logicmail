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

/**
 * Interface between MailConnectionHandler implementations and
 * MailStore implementations.
 */
public interface MailConnectionHandlerListener {
	/**
	 * Indicates that a request has been completed.
	 * 
	 * @param type The type of the request.
	 * @param result The data returned from the request.
     * @param tag Tag reference to pass along with the request
     * @param isFinal true if this is the final or only callback of a request
	 */
	public void mailConnectionRequestComplete(int type, Object result, Object tag, boolean isFinal);
	
    /**
     * Indicates that a request has failed.
     * 
     * @param type The type of the request.
     * @param tag Tag reference to pass along with the request
     * @param exception The exception that caused the request to fail, if applicable.
     * @param isFinal true if the connection will be closed, false if it is being reopened
     */
    public void mailConnectionRequestFailed(int type, Object tag, Throwable exception, boolean isFinal);
    
    /**
     * Indicates that the connection has left the idle state due to a timeout.
     * 
     * @param idleDuration The elapsed time, in milliseconds, that the connection has been idle.
     */
    public void mailConnectionIdleTimeout(long idleDuration);
}
