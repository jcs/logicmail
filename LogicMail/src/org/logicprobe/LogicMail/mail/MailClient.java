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

import org.logicprobe.LogicMail.conf.ConnectionConfig;

/**
 * This is a generic interface to all mail protocols.
 * This class allows most of the background event handling
 * code to be protocol agnostic.
 */
public interface MailClient {
    /**
     * Open a new connection.
     * This method should typically establish a socket connection and
     * then send the protocol-specific login commands.
     * It should also be possible to invoke this method multiple times
     * to correct for authentication failures, without having to reopen
     * the underlying connection.
     *
     * @return True if successful, false on authentication failure
     * @throws IOException on I/O errors
     * @throws MailException on protocol errors
     */
    boolean open() throws IOException, MailException;
    
    /**
     * Close an existing connection.
     * This method should sent protocol-specific logout commands and
     * then terminate the connection.
     *
     * @throws IOException on I/O errors
     * @throws MailException on protocol errors
     */
    void close() throws IOException, MailException;

    /**
     * Gets the connection configuration associated with this client.
     * @return Connection configuration.
     */
    ConnectionConfig getConnectionConfig();
    
    /**
     * Find out if the connection is active.
     * @return True if the connection is active, false otherwise
     */
    boolean isConnected();

    /**
     * Gets the type of connection that was opened.
     *
     * @return the connection type, based on the
     *     <code>ConnectionConfig.TRANSPORT_XXXX</code> constants.
     */
    int getConnectionType();
    
    /**
     * Checks if a valid username and password are required for this client.
     * @return true, if login is required
     */
    boolean isLoginRequired();

    /**
     * Get the configured username for this client.
     * @return Configured username, empty if none, null if n/a
     */
    String getUsername();
    
    /**
     * Set the username for this client to use
     * @param username The username
     */
    void setUsername(String username);

    /**
     * Get the configured password for this client.
     * @return Configured password, empty if none, null if n/a
     */
    String getPassword();
    
    /**
     * Set the password for this client to use
     * @param password The password
     */
    void setPassword(String password);
}
