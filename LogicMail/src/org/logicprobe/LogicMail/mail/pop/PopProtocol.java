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

package org.logicprobe.LogicMail.mail.pop;

import java.io.IOException;
import java.util.Hashtable;

import net.rim.device.api.system.EventLogger;
import net.rim.device.api.util.Arrays;
import org.logicprobe.LogicMail.AppInfo;
import org.logicprobe.LogicMail.mail.MailException;
import org.logicprobe.LogicMail.mail.MailProgressHandler;
import org.logicprobe.LogicMail.util.Connection;

/**
 * This class implements the commands for the POP3 protocol
 */
public class PopProtocol {
    private Connection connection;
    
    /** Creates a new instance of PopProtocol */
    public PopProtocol(Connection connection) {
        this.connection = connection;
    }
    
    /**
     * Execute the "CAPA" command
     * <p>
     * This is an optional command, defined in RFC 2449.
     * </p>
     * @return Table of server capabilities, or <code>null</code> if the command
     *         is not supported by the server
     */
	public Hashtable executeCapa() throws IOException, MailException {
        if (EventLogger.getMinimumLevel() >= EventLogger.DEBUG_INFO) {
            EventLogger.logEvent(AppInfo.GUID,
                ("PopProtocol.executeCapa()").getBytes(),
                EventLogger.DEBUG_INFO);
        }
        String[] replyText = executeFollow("CAPA", false, null);
        
        if ((replyText == null) || (replyText.length < 1)) {
            return null;
        }

        Hashtable table = new Hashtable();

        for(int i=0; i<replyText.length; i++) {
        	int p = replyText[i].indexOf(' ');
        	int len = replyText[i].length();
        	if(p != -1 && p + 1 < len) {
        		table.put(replyText[i].substring(0, p), replyText[i].substring(p + 1, len));
        	}
        	else {
        		table.put(replyText[i], Boolean.TRUE);
        	}
        }
        
		return table;
	}
	
    /**
     * Execute the "STARTTLS" command.
     * The underlying connection mode must be switched after the
     * successful execution of this command.
     * 
     * @return true, if successful
     */
	public boolean executeStartTLS() throws IOException, MailException {
        if (EventLogger.getMinimumLevel() >= EventLogger.DEBUG_INFO) {
            EventLogger.logEvent(AppInfo.GUID,
                ("PopProtocol.executeStartTLS()").getBytes(),
                EventLogger.DEBUG_INFO);
        }
        return execute("STARTTLS", false) != null;
	}
	
    /**
     * Execute the "USER" command
     * @param username Username to login with
     */
    public void executeUser(String username) throws IOException, MailException {
        if(EventLogger.getMinimumLevel() >= EventLogger.DEBUG_INFO) {
            EventLogger.logEvent(
            AppInfo.GUID,
            ("PopProtocol.executeUser(\""+username+"\")").getBytes(),
            EventLogger.DEBUG_INFO);
        }
        execute("USER " + username);
        // Invalid users are caught by execute()
        // and a MailException is thrown
    }
    
    /**
     * Execute the "PASS" command
     * @param password Password to login with
     */
    public void executePass(String password) throws IOException, MailException {
        if(EventLogger.getMinimumLevel() >= EventLogger.DEBUG_INFO) {
            EventLogger.logEvent(
            AppInfo.GUID,
            ("PopProtocol.executePass(\""+password+"\")").getBytes(),
            EventLogger.DEBUG_INFO);
        }
        execute("PASS " + password);
        // Invalid users are caught by execute()
        // and a MailException is thrown
    }
    
    /**
     * Execute the "QUIT" command
     */
    public void executeQuit() throws IOException, MailException {
        if(EventLogger.getMinimumLevel() >= EventLogger.DEBUG_INFO) {
            EventLogger.logEvent(
            AppInfo.GUID,
            ("PopProtocol.executeQuit()").getBytes(),
            EventLogger.DEBUG_INFO);
        }
        execute("QUIT");
    }
    
    /**
     * Execute the "STAT" command
     * @return The number of messages in the inbox
     */
    public int executeStat() throws IOException, MailException {
        if(EventLogger.getMinimumLevel() >= EventLogger.DEBUG_INFO) {
            EventLogger.logEvent(
            AppInfo.GUID,
            ("PopProtocol.executeStat()").getBytes(),
            EventLogger.DEBUG_INFO);
        }
        String result = execute("STAT");
        int p = result.indexOf(' ');
        int q = result.indexOf(' ', p+1);
        try {
            return Integer.parseInt(result.substring(p+1, q));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /**
     * Execute the "TOP" command
     * @param index Message index
     * @param lines Number of lines to retrieve
     */
    public String[] executeTop(int index, int lines) throws IOException, MailException {
    	return executeTop(index, lines, null);
    }
    
    /**
     * Execute the "TOP" command
     * @param index Message index
     * @param lines Number of lines to retrieve
     * @param progressHandler progress handler
     */
    public String[] executeTop(int index, int lines, MailProgressHandler progressHandler) throws IOException, MailException {
        if(EventLogger.getMinimumLevel() >= EventLogger.DEBUG_INFO) {
            EventLogger.logEvent(
            AppInfo.GUID,
            ("PopProtocol.executeTop("+index+", "+lines+")").getBytes(),
            EventLogger.DEBUG_INFO);
        }
        return executeFollow("TOP " + index + " " + lines, true, progressHandler);
    }
    
    /**
     * Execute the "UIDL" command
     * @param index Message index
     */
    public String executeUidl(int index) throws IOException, MailException {
        if(EventLogger.getMinimumLevel() >= EventLogger.DEBUG_INFO) {
            EventLogger.logEvent(
            AppInfo.GUID,
            ("PopProtocol.executeUidl("+index+")").getBytes(),
            EventLogger.DEBUG_INFO);
        }
        String result = execute("UIDL " + index);
        int p = result.lastIndexOf(' ');
        if(p < result.length() - 2) {
        	return result.substring(p+1);
        }
        else {
        	return null;
        }
    }
    
    /**
     * Execute the "DELE" command.
     * This will mark the message at the specified index as deleted, but will
     * not reindex the messages immediately.  Therefore, it is safe to keep
     * existing message indicies for the rest of the session.
     * @param index Message index to delete
     */
    public void executeDele(int index) throws IOException, MailException {
        if(EventLogger.getMinimumLevel() >= EventLogger.DEBUG_INFO) {
            EventLogger.logEvent(
            AppInfo.GUID,
            ("PopProtocol.executeDele("+index+")").getBytes(),
            EventLogger.DEBUG_INFO);
        }
        execute("DELE " + index);
    }

    /**
     * Executes the "NOOP" command.
     */
    public void executeNoop() throws IOException, MailException {
        if(EventLogger.getMinimumLevel() >= EventLogger.DEBUG_INFO) {
            EventLogger.logEvent(
            AppInfo.GUID,
            ("PopProtocol.executeNoop()").getBytes(),
            EventLogger.DEBUG_INFO);
        }
        execute("NOOP");
    }
    
    /**
     * Execute a POP3 command that returns multiple lines.
     * This works by running the normal execute() and then
     * receiving every new line until a lone "." is encountered.
     *
     * @param command The command to execute
     * @param errorFatal If true, then an "-ERR" response to the command will
     *                   generate an exception.
     * @param progressHandler progress handler
     * @return An array of lines containing the response, or <code>null</code>
     *         if <code>errorFatal</code> is <code>false</code> and the
     *         response was an error.
     */
    private String[] executeFollow(String command, boolean errorFatal, MailProgressHandler progressHandler) throws IOException, MailException {
    	int preCount = connection.getBytesReceived();
        if(execute(command, errorFatal) == null) { return null; }
        
        String buffer = new String(connection.receive());
        int postCount = connection.getBytesReceived();
        if(progressHandler != null) { progressHandler.mailProgress(MailProgressHandler.TYPE_NETWORK, (postCount - preCount), -1); }

        String[] lines = new String[0];
        while(buffer != null && !buffer.equals(".")) {
            Arrays.add(lines, buffer);
            preCount = postCount;
            buffer = new String(connection.receive());
            postCount = connection.getBytesReceived();
            if(progressHandler != null) { progressHandler.mailProgress(MailProgressHandler.TYPE_NETWORK, (postCount - preCount), -1); }
        }
        return lines;
    }
    
    /**
     * Execute a POP3 command, and return the result.
     * If the command is null, we still wait for a result
     * so we can receive a multi-line response.
     *
     * @param command The command
     * @return The result
     */
    private String execute(String command) throws IOException, MailException {
        return execute(command, false);
    }
    
    /**
     * Execute a POP3 command, and return the result.
     * If the command is null, we still wait for a result
     * so we can receive a multi-line response.
     *
     * @param command The command
     * @param errorFatal If true, then an "-ERR" response to the command will
     *                   generate an exception.
     * @return The result, or <code>null</code> if <code>errorFatal</code> is
     *         <code>false</code> and the response was an error.
     */
    private String execute(String command, boolean errorFatal) throws IOException, MailException {
        if(command != null) {
            connection.sendCommand(command);
        }
        
        String result = new String(connection.receive());
        
        if((result.length() > 1) && (result.charAt(0) == '-')) {
            if(errorFatal) {
                throw new MailException(result);
            }
            else {
                return null;
            }
        }
        
        return result;
    }
}
