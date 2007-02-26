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

import java.io.IOException;
import org.logicprobe.LogicMail.message.Message;

/**
 * Create a generic interface to outgoing mail protocols.
 */
public interface OutgoingMailClient {
    /**
     * Open a new connection.
     * This method should typically establish a socket connection and
     * then send the protocol-specific login commands
     *
     * @throw IOException on I/O errors
     * @throw MailException on protocol errors
     */
    public abstract void open() throws IOException, MailException;
    
    /**
     * Close an existing connection.
     * This method should sent protocol-specific logout commands and
     * then terminate the connection.
     *
     * @throw IOException on I/O errors
     * @throw MailException on protocol errors
     */
    public abstract void close() throws IOException, MailException;

    /**
     * Find out if the connection is active.
     * @return True if the connection is active, false otherwise
     */
    public abstract boolean isConnected();

    /**
     * Send a message
     *
     * @throw IOException on I/O errors
     * @throw MailException on protocol errors
     */
    public abstract void sendMessage(Message message) throws IOException, MailException;
}
