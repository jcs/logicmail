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

package org.logicprobe.LogicMail.mail.smtp;

import java.io.IOException;
import java.util.Vector;
import net.rim.device.api.util.Arrays;
import org.logicprobe.LogicMail.mail.MailException;
import org.logicprobe.LogicMail.util.Connection;

/**
 * This class implements the commands for the SMTP protocol
 */
public class SmtpProtocol {
    private Connection connection;
    
    /** Creates a new instance of SmtpProtocol */
    public SmtpProtocol(Connection connection) {
        this.connection = connection;
    }
    
    /**
     * Execute the "EHLO" command.
     * @param domain Domain name of the client
     * @return List of returned strings
     */
    public Vector executeExtendedHello(String domain) throws IOException, MailException {
        String[] result = executeFollow("EHLO " + domain);
        Vector items = new Vector();
        for(int i=0; i<result.length; i++) {
            if(result[i].length() > 4) {
                items.addElement(result[i].substring(4));
            }
        }
        return items;
    }

    /**
     * Execute the "MAIL FROM" command.
     * @param sender Sender of the message, formatted the standard way,
     *               as "foo@bar.com"
     * @return True if successful, false on failure
     */
    public boolean executeMail(String sender) throws IOException, MailException {
        String result = execute("MAIL FROM:<" + sender + ">");
        return result.startsWith("250");
    }

    /**
     * Execute the "RCPT TO" command.
     * @param recipient Recipient of the message, formatted the standard way,
     *                  as "foo@bar.com"
     * @return True if successful, false on failure
     */
    public boolean executeRecipient(String recipient) throws IOException, MailException {
        String result = execute("RCPT TO:<" + recipient + ">");
        return result.startsWith("250");
    }
    
    /**
     * Execute the "DATA" command.
     * @param message Message data fully serialized into a flat ASCII string
     * @return True if successful, false on failure
     */
    public boolean executeData(String message) throws IOException, MailException {
        String result = execute("DATA");
        if(!result.startsWith("354"))
            return false;
        
        connection.send(message);
        result = execute("\r\n.");
        
        return result.startsWith("250");
    }
    
    /**
     * Execute the "RSET" command.
     * @return True if successful, false on failure
     */
    public boolean executeReset() throws IOException, MailException {
        String result = execute("RSET");
        return result.startsWith("250");
    }
    
    /**
     * Execute the "QUIT" command.
     * @return True if successful, false on failure
     */
    public boolean executeQuit() throws IOException, MailException {
        String result = execute("QUIT");
        return result.startsWith("221");
    }

    /**
     * Execute an SMTP command, and return the result.
     * If the command is null, we still wait for a result
     * so we can receive a multi-line response.
     *
     * @param command The command
     * @return The result
     */
    private String execute(String command) throws IOException, MailException {
        if(command != null) connection.send(command);
        
        String result = connection.receive();
        
//        if((result.length() > 1) && (result.charAt(0) == '-')) {
//            throw new MailException(result);
//        }
        
        return result;
    }

    /**
     * Execute an SMTP command that returns multiple lines.
     * This works by running the normal execute() and then
     * receiving every new line until a line with a space
     * between the code and value is encountered.
     *
     * @param command The command to execute
     * @return An array of lines containing the response
     */
    private String[] executeFollow(String command) throws IOException, MailException {
        execute(command);
            
        String buffer = connection.receive();
        String[] lines = new String[0];
        while(buffer != null) {
            buffer = connection.receive();
            Arrays.add(lines, buffer);
            if(buffer.length() >=4 && buffer.charAt(3) == ' ') break;
        }
        return lines;
    }
}
