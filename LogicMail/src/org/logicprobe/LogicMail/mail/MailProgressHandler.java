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
package org.logicprobe.LogicMail.mail;

/**
 * Interface for mail progress handlers.
 * <p>
 * This provides a lightweight method to be notified when low-level
 * activity occurs within mail protocol implementations.  This activity
 * can be interpreted to provide more useful progress notification to
 * users.
 * </p>
 */
public interface MailProgressHandler {
	/** Progress relates to network or other I/O activities */
	public final static int TYPE_NETWORK = 0;
	
	/** Progress relates to internal processing */
	public final static int TYPE_PROCESSING = 1;
	
	
	/**
	 * Mail progress has occurred.
	 * <p>
	 * Implementers should make all efforts to minimize the processing
	 * that occurs within this method, and/or use count thresholds
	 * to reduce the probability of spending time in the implementation.
	 * This is important because this method may occasionally be called
	 * from within a network data receive loop.
	 * </p>
	 * 
	 * @param type the type of progress
	 * @param count the progress count or index
	 * @param max the maximum value the count may reach, or -1 if not known
	 */
	void mailProgress(int type, int count, int max);
}
