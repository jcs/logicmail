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
import java.util.Hashtable;
import java.util.Vector;
import net.rim.device.api.util.Arrays;
import org.logicprobe.LogicMail.mail.MailException;
import org.logicprobe.LogicMail.message.MessageEnvelope;
import org.logicprobe.LogicMail.util.Connection;
import org.logicprobe.LogicMail.util.StringParser;

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

    /**
     * Flags that can be associated with a message.
     * Used inside reply objects for multiple commands.
     */
    public static class MessageFlags {
        /** Message has been read */
        public boolean seen;
        /** Message has been answered */
        public boolean answered;
        /** Message is flagged for special attention */
        public boolean flagged;
        /** Message is deleted */
        public boolean deleted;
        /** Message has not completed composition */
        public boolean draft;
        /** Message has recently arrived */
        public boolean recent;
    }
    
    /** Creates a new instance of ImapProtocol */
    public ImapProtocol(Connection connection) {
        this.connection = connection;
    }
    
    /**
     * Execute the "LOGIN" command
     * @param username The username to login with
     * @param password The password to login with
     * @return True on success, false on authentication failures
     */
    public boolean executeLogin(String username, String password) throws IOException, MailException {
        // Authenticate with the server
        try {
            execute("LOGIN", "\""+ username + "\" \"" + password + "\"");
        } catch (MailException exp) {
            // Invalid users are caught by execute()
            // and a MailException is thrown
            return false;
        }
        return true;
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
     * Execute the "CAPABILITY" command
     * @return Hashtable containing a mapping from String to the boolean
     * value "true" for every capability that exists in the reply.
     */
    public Hashtable executeCapability() throws IOException, MailException {
        String replyText[] = execute("CAPABILITY", null);
        if(replyText == null || replyText.length < 1) {
            throw new MailException("Unable to query server capabilities");
        }
        
        Hashtable table = new Hashtable();

        String[] tokens = StringParser.parseTokenString(replyText[0], "");
        if(tokens.length > 2 && tokens[1].equals("CAPABILITY")) {
            for(int i=2;i<tokens.length;i++)
                table.put(tokens[i], new Boolean(true));
        }
        
        return table;
    }
    
    /**
     * Container for a SELECT response
     */
    public static class SelectResponse {
        //public MessageFlags availableFlags; // not used
        //public MessageFlags permanentFlags; // not used
        public int exists;
        public int recent;
        public int unseen;
        public int uidNext;
        public int uidValidity;
    }
    
    /**
     * Execute the "SELECT" command
     * @param mboxpath The mailbox path to select
     * @return Parsed response object
     */
    public SelectResponse executeSelect(String mboxpath) throws IOException, MailException {
        String replyText[] = execute("SELECT", "\""+mboxpath+"\"");
        SelectResponse response = new SelectResponse();

        int p, q;
        for(int i=0;i<replyText.length;i++) {
            String rowText = replyText[i];
            if((p = rowText.indexOf(" EXISTS")) != -1) {
                q = p;
                p = rowText.indexOf(' ');
                if(q != -1 && p != -1 && q > p) {
                    try {
                        response.exists = Integer.parseInt(rowText.substring(p+1, q));
                    } catch (NumberFormatException e) {
                        response.exists = 0;
                    }
                }
            }
            else if((p = rowText.indexOf(" RECENT")) != -1) {
                q = p;
                p = rowText.indexOf(' ');
                if(q != -1 && p != -1 && q > p) {
                    try {
                        response.recent = Integer.parseInt(rowText.substring(p+1, q));
                    } catch (NumberFormatException e) {
                        response.recent = 0;
                    }
                }
            }
            else if((p = rowText.indexOf("UNSEEN ")) != -1) {
                p += 6;
                q = rowText.indexOf(']');
                if(q != -1 && p != -1 && q > p) {
                    try {
                        response.unseen = Integer.parseInt(rowText.substring(p+1, q));
                    } catch (NumberFormatException e) {
                        response.unseen = 0;
                    }
                }                
            }
            else if((p = rowText.indexOf("UIDVALIDITY ")) != -1) {
                p += 11;
                q = rowText.indexOf(']');
                if(q != -1 && p != -1 && q > p) {
                    try {
                        response.uidValidity = Integer.parseInt(rowText.substring(p+1, q));
                    } catch (NumberFormatException e) {
                        response.uidValidity = 0;
                    }
                }
            }
            else if((p = rowText.indexOf("UIDNEXT ")) != -1) {
                p += 7;
                q = rowText.indexOf(']');
                if(q != -1 && p != -1 && q > p) {
                    try {
                        response.uidNext = Integer.parseInt(rowText.substring(p+1, q));
                    } catch (NumberFormatException e) {
                        response.uidNext = 0;
                    }
                }
            }
        }
        return response;
    }
    
    /**
     * Container for a FETCH (ENVELOPE) response
     */
    public static class FetchEnvelopeResponse {
        public int index;
        public MessageFlags flags;
        public MessageEnvelope envelope;
    }

    /**
     * Execute the "FETCH (ENVELOPE)" command
     * @param firstIndex Index of the first message
     * @param lastIndex Index of the last message
     * @return Array of FetchEnvelopeResponse objects
     */
    public FetchEnvelopeResponse[] executeFetchEnvelope(int firstIndex, int lastIndex) throws IOException, MailException {
        String[] rawList = execute("FETCH",
                                   Integer.toString(firstIndex) + ":" +
                                   Integer.toString(lastIndex) +
                                   " (FLAGS ENVELOPE)");
        
        FetchEnvelopeResponse[] envResponses = new FetchEnvelopeResponse[(lastIndex - firstIndex)+1];
        
        // Pre-process the returned text to clean up mid-field line breaks
        // This should all become unnecessary once execute()
        // becomes more intelligent in how it handles replies
        String line;
        StringBuffer lineBuf = new StringBuffer();
        Vector rawList2 = new Vector();
        for(int i=0;i<rawList.length;i++) {
            line = rawList[i];
            if(line.length() > 0 &&
                    lineBuf.toString().startsWith("* ") &&
                    line.startsWith("* ")) {
                rawList2.addElement(lineBuf.toString());
                lineBuf = new StringBuffer();
            }
            lineBuf.append(line);
            if(i == rawList.length-1 && lineBuf.toString().startsWith("* "))
                rawList2.addElement(lineBuf.toString());
        }
        
        int index = 0;
        int size = rawList2.size();
        for(int i=0;i<size;i++) {
            try {
                String rawText = (String)rawList2.elementAt(i);

                MessageEnvelope env;
                Vector parsedText = null;
                try {
                    parsedText = StringParser.nestedParenStringLexer(rawText.substring(rawText.indexOf('(')));
                } catch (Exception exp) {
                    parsedText = null;
                }

                FetchEnvelopeResponse envRespItem = new FetchEnvelopeResponse();

                if(parsedText.size() > 3) {
                    if((parsedText.elementAt(0) instanceof String) &&
                            ((String)parsedText.elementAt(0)).equals("FLAGS")) {
                        if(parsedText.elementAt(1) instanceof Vector) {
                            // Violates the token structure, but avoids having
                            // to fix some strange behavior in the lexer
                            String flagText = rawText.substring(rawText.indexOf('('), rawText.indexOf(')'));
                            envRespItem.flags = ImapParser.parseMessageFlags(flagText);
                        }
                        if((parsedText.elementAt(2) instanceof String) &&
                                ((String)parsedText.elementAt(2)).equals("ENVELOPE") &&
                                (parsedText.elementAt(3) instanceof Vector))
                        {
                            env = ImapParser.parseMessageEnvelope((Vector)parsedText.elementAt(3));
                        }
                        else {
                            env = ImapParser.generateDummyEnvelope();
                        }
                    }
                    else {
                        env = ImapParser.generateDummyEnvelope();
                    }
                }
                else {
                    env = ImapParser.generateDummyEnvelope();
                }
                
                int midx = Integer.parseInt(rawText.substring(rawText.indexOf(' '), rawText.indexOf("FETCH")-1).trim());
                
                envRespItem.index = midx;
                envRespItem.envelope = env;
                envResponses[index++] = envRespItem;
            } catch (Exception exp) {
                System.err.println("Parse error: " + exp);
            }
        }

        return envResponses;
    }

    /**
     * Execute the "FETCH (BODYSTRUCTURE)" command
     * @param index Index of the message
     * @return Body structure tree
     */
    public ImapParser.MessageSection executeFetchBodystructure(int index) throws IOException, MailException {
        String[] rawList = execute("FETCH", index + " (BODYSTRUCTURE)");

        // Pre-process the returned text to clean up mid-field line breaks
        // This should all become unnecessary once execute()
        // becomes more intelligent in how it handles replies
        String line;
        StringBuffer lineBuf = new StringBuffer();
        Vector rawList2 = new Vector();
        for(int i=0;i<rawList.length;i++) {
            line = rawList[i];
            if(line.length() > 0 &&
                    lineBuf.toString().startsWith("* ") &&
                    line.startsWith("* ")) {
                rawList2.addElement(lineBuf.toString());
                lineBuf = new StringBuffer();
            }
            lineBuf.append(line);
            if(i == rawList.length-1 && lineBuf.toString().startsWith("* "))
                rawList2.addElement(lineBuf.toString());
        }

        ImapParser.MessageSection msgStructure = null;
        int size = rawList2.size();
        for(int i=0;i<size;i++) {
            try {
                msgStructure = ImapParser.parseMessageStructure((String)rawList2.elementAt(i));
            } catch (Exception exp) {
                System.out.println("Parse error: " + exp);
            }
        }
        
        return msgStructure;
    }

    /**
     * Execute the "FETCH (BODY)" command
     * @param index Index of the message
     * @param address Address of the body section (i.e. "1", "1.2")
     * @return Body text as a string
     */
    public String executeFetchBody(int index, String address) throws IOException, MailException {
        String[] rawList = execute("FETCH", index + " (BODY["+ address +"])");

        if(rawList.length <= 1) return "";
        
        StringBuffer msgBuf = new StringBuffer();
        for(int i=1;i<rawList.length-1;i++) {
            msgBuf.append(rawList[i] + "\n");
        }
        String lastLine = rawList[rawList.length-1];
        msgBuf.append(lastLine.substring(0, lastLine.lastIndexOf(')')));
        return msgBuf.toString();
    }

    /**
     * Container for a LIST response
     */
    public static class ListResponse {
        public boolean hasChildren;
        public boolean canSelect;
        public boolean marked;
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
            response.delim = "";
            response.name = "";
            
            // Look for flags
            response.canSelect = !(flagStr.indexOf("\\Noselect") != -1);
            response.hasChildren = (flagStr.indexOf("\\HasChildren") != -1);
            response.marked = (flagStr.indexOf("\\Marked") != -1);
            
            
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
