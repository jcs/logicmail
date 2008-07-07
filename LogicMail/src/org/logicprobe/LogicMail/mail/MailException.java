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

package org.logicprobe.LogicMail.mail;

/**
 * Exception class for protocol errors
 */
public class MailException extends Exception {
	private boolean fatal;
	private int cause;

	/**
	 * Initialize a new MailException.
	 * 
	 * @param message Error message.
	 * @param fatal True if fatal, false if recoverable.
	 * @param cause Error cause, useful for recovery.
	 */
    public MailException(String message, boolean fatal, int cause) {
        super(message);
        this.fatal = fatal;
        this.cause = cause;
    }
    
    /**
     * Initialize a new MailException for a fatal error with an unspecified cause.
     * 
     * @param message Error message.
     */
    public MailException(String message) {
        this(message, true, -1);
    }
    
    /**
     * Gets whether this exception is fatal.
     * 
     * @return True if fatal, false if recoverable.
     */
    public boolean isFatal() {
    	return this.fatal;
    }
    
    /**
     * Gets the cause of this exception.
     * 
     * @return Cause identifier, or -1 if unspecified.
     */
    public int getCause() {
    	return this.cause;
    }
}

