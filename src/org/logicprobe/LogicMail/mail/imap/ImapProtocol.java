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

package org.logicprobe.LogicMail.mail.imap;

import java.io.IOException;
import java.util.Vector;
import net.rim.device.api.util.Arrays;
import org.logicprobe.LogicMail.mail.MailException;
import org.logicprobe.LogicMail.util.Connection;

/**
 * This class implements the commands for the IMAP protocol
 */
public class ImapProtocol {
    private Connection connection;

    /**
     * Counts the commands executed so far in this session. Every command of an
     * IMAP session needs a unique ID that is prepended to the command line.
     */
    private int commandCount = 0;
    
    /** Creates a new instance of ImapProtocol */
    public ImapProtocol(Connection connection) {
        this.connection = connection;
    }
    
    /**
     * Execute the "LOGIN" command
     * @param username The username to login with
     * @param password The password to login with
     */
    public void executeLogin(String username, String password) throws IOException, MailException {
        // Authenticate with the server
        execute("LOGIN", "\""+ username + "\" \"" + password + "\"");
        
        // This really needs error checking
    }
    
    /**
     * Execute the "LOGOUT" command
     */
    public void executeLogout() throws IOException, MailException {
        execute("LOGOUT", null);
    }
    
    /**
     * Execute the "CLOSE" command
     */
    public void executeClose() throws IOException, MailException {
        execute("CLOSE", null);
    }
    
    /**
     * Execute the "SELECT" command
     * @param mboxpath The mailbox path to select
     */
    public String[] executeSelect(String mboxpath) throws IOException, MailException {
        return execute("SELECT", "\""+mboxpath+"\"");
        // Ideally, this should parse the reply.
        // Right now, however, it is just a thin wrapper.
    }
    
    /**
     * Execute the "FETCH (ENVELOPE)" command
     * @param firstIndex Index of the first message
     * @param lastIndex Index of the last message
     */
    public String[] executeFetchEnvelope(int firstIndex, int lastIndex) throws IOException, MailException {
        String[] rawList = execute("FETCH",
                                   Integer.toString(firstIndex) + ":" +
                                   Integer.toString(lastIndex) +
                                   " (ENVELOPE)");
        return rawList;
        // Ideally, this should parse the reply.
        // Right now, however, it is just a thin wrapper.
    }

    /**
     * Execute the "FETCH (BODYSTRUCTURE)" command
     * @param index Index of the message
     */
    public String[] executeFetchBodystructure(int index) throws IOException, MailException {
        String[] rawList = execute("FETCH", index + " (BODYSTRUCTURE)");
        return rawList;
        // Ideally, this should parse the reply.
        // Right now, however, it is just a thin wrapper.
    }

    /**
     * Execute the "FETCH (BODY)" command
     * @param index Index of the message
     * @param address Address of the body section (i.e. "1", "1.2")
     */
    public String[] executeFetchBody(int index, String address) throws IOException, MailException {
        String[] rawList = execute("FETCH", index + " (BODY["+ address +"])");
        return rawList;
        // Ideally, this should parse the reply.
        // Right now, however, it is just a thin wrapper.
    }

    /**
     * Container for LIST replies
     */
    public static class ListResponse {
        public boolean hasChildren;
        public boolean canSelect;
        public String delim;
        public String name;
    };
    
    /**
     * Execute the "LIST" command, and return a fully parsed response
     * @param refName Reference name
     * @param mboxName Mailbox name or wildcards (i.e. "%")
     * @return Vector of ListResponse objects
     */
    public Vector executeList(String refName, String mboxName) throws IOException, MailException {
        String[] results;
        results = execute("LIST", "\""+refName+"\" \""+mboxName+"\"");
        
        Vector retVec = new Vector(results.length);
        ListResponse response;
        String temp;
        String flagStr;
        String argStr;
        int p;
        int q;
        for(int i=0;i<results.length;i++) {
            // Separate out the flag and argument strings
            flagStr = null;
            argStr = null;
            temp = results[i];
            p = temp.indexOf('(');
            q = temp.indexOf(')', p + 1);
            if((p != -1) && (q > p))
                flagStr = temp.substring(p + 1, q);
            if(temp.length() > q+2)
                argStr = temp.substring(q+2);
            
            response = new ListResponse();
            response.hasChildren = false;
            response.canSelect = true;
            response.delim = "";
            response.name = "";
            
            // Should eventually tokenize and parse properly.
            // Right now just looking at the first flag, for
            // simplicity, since most responses only have one
            if(flagStr.startsWith("\\Noselect"))
                response.canSelect = false;
            else if(flagStr.startsWith("\\HasChildren"))
                response.hasChildren = true;
            
            p = argStr.indexOf('\"');
            q = argStr.indexOf('\"', p + 1);
            
            // Store the delimiter
            if((p != -1) && (q > p))
                response.delim = argStr.substring(p+1, q);
            
            // Store the name, strip off quotes if necessary
            if(argStr.length() > q+2) {
                response.name = argStr.substring(q+2);
                p = response.name.indexOf('\"');
                q = response.name.indexOf('\"', p + 1);
                if((p != -1) && (q > p))
                    response.name = response.name.substring(p+1, q);
            }
            
            retVec.addElement(response);
        }
        
        return retVec;
    }

    /**
     * Executes an IMAP command, and returns the reply as an
     * array of strings.
     * @param command IMAP command
     * @param arguments Arguments for the command
     * @return List of returned strings
     */
    private String[] execute(String command, String arguments)
        throws IOException, MailException
    {
        String[] result = new String[0];

        String tag = "A" + commandCount++ + " ";
        connection.send(tag + command + (arguments == null ? "" : " " + arguments));

        String temp = connection.receive();
        while (!temp.startsWith(tag)) {
            Arrays.add(result, temp);
            temp = connection.receive();
        }

        temp = temp.substring(tag.length());
        if (temp.startsWith("BAD ") || temp.startsWith("NO ")) {
            throw new MailException(temp);
        }
        return result;
    }
}
