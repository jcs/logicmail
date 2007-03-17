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
import java.util.Calendar;
import java.util.Vector;
import org.logicprobe.LogicMail.AppInfo;
import org.logicprobe.LogicMail.conf.AccountConfig;
import org.logicprobe.LogicMail.conf.GlobalConfig;
import org.logicprobe.LogicMail.mail.MailException;
import org.logicprobe.LogicMail.mail.OutgoingMailClient;
import org.logicprobe.LogicMail.message.Message;
import org.logicprobe.LogicMail.message.MessageEnvelope;
import org.logicprobe.LogicMail.message.MessageMimeConverter;
import org.logicprobe.LogicMail.util.Connection;

/**
 * Implements an SMTP client
 */
public class SmtpClient implements OutgoingMailClient {
    private GlobalConfig globalConfig;
    private AccountConfig acctCfg;
    private Connection connection;
    private SmtpProtocol smtpProtocol;
    private Vector helloResult;
    private boolean isFresh;
    
    /** Creates a new instance of SmtpClient */
    public SmtpClient(GlobalConfig globalConfig, AccountConfig acctCfg) {
        this.acctCfg = acctCfg;
        this.globalConfig = globalConfig;
        connection = new Connection(
            acctCfg.getSmtpServerName(),
            acctCfg.getSmtpServerPort(),
            acctCfg.getSmtpServerSSL(),
            acctCfg.getDeviceSide());
        smtpProtocol = new SmtpProtocol(connection);
    }

    public void open() throws IOException, MailException {
        connection.open();
        
        // Eat the initial server response
        connection.receive();
        String hostname = System.getProperty("microedition.hostname");
        if(hostname == null) hostname = "unknown";
        
        helloResult = smtpProtocol.executeExtendedHello(hostname);
        
        if(acctCfg.getSmtpUseAuth() > 0) {
            boolean result = smtpProtocol.executeAuth(acctCfg.getSmtpUseAuth(), acctCfg.getSmtpUser(), acctCfg.getSmtpPass());
            if(!result) {
                connection.close();
                throw new MailException("Unable to authenticate");
            }
        }
        isFresh = true;
    }

    public void close() throws IOException, MailException {
        smtpProtocol.executeQuit();
        connection.close();
    }

    public boolean isConnected() {
        return connection.isConnected();
    }

    public void sendMessage(Message message) throws IOException, MailException {
        if(!isFresh) smtpProtocol.executeReset();
        isFresh = false;
        
        // serialize the message
        MessageMimeConverter messageMime = new MessageMimeConverter();
        message.getBody().accept(messageMime);
        String mimeStr = messageMime.toMimeString();
        
        MessageEnvelope env = message.getEnvelope();
        StringBuffer buffer = new StringBuffer();

        // Create the message headers
        buffer.append("From: " + makeCsvString(env.from) + "\r\n");
        buffer.append("To: " + makeCsvString(env.to) + "\r\n");
        if(env.cc != null && env.cc.length > 0)
            buffer.append("Cc: " + makeCsvString(env.cc) + "\r\n");
        if(env.bcc != null && env.bcc.length > 0)
            buffer.append("Bcc: " + makeCsvString(env.bcc) + "\r\n");
        if(env.replyTo != null && env.replyTo.length > 0)
            buffer.append("Reply-To: " + makeCsvString(env.replyTo) + "\r\n");
        buffer.append("Date: " + Calendar.getInstance().getTime().toString() + "\r\n");
        buffer.append("User-Agent: "+AppInfo.getName()+"/"+AppInfo.getVersion() + "\r\n");
        buffer.append("Subject: " + env.subject + "\r\n");
        
        // Add the body
        buffer.append(mimeStr);

        // Send the message
        if(!smtpProtocol.executeMail(stripEmail(env.from[0])))
            throw new MailException("Error with sender");
        if(!smtpProtocol.executeRecipient(stripEmail(env.to[0])))
            throw new MailException("Error with recipient");
        if(!smtpProtocol.executeData(buffer.toString()))
            throw new MailException("Error sending message");
    }
    
    private static String makeCsvString(String[] input) {
        if(input == null || input.length == 0)
            return "";
        else if(input.length == 1)
            return input[0];
        else {
            StringBuffer buffer = new StringBuffer();
            for(int i=0; i<input.length-1; i++) {
                buffer.append(input[i]);
                buffer.append(", ");
            }
            buffer.append(input[input.length-1]);
            return buffer.toString();
        }
    }
    
    private static String stripEmail(String input) {
        int p = input.indexOf('<');
        int q = input.indexOf('>');
        if(p == -1 || q == -1 || q <= p)
            return input;
        else
            return input.substring(p+1, q);
    }
}
