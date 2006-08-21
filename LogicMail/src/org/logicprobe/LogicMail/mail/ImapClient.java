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

/*
 * Portions of this code may have been inspired by and/or
 * copied from the following classes of the Mail4ME project:
 * de.trantor.mail.ImapClient
 * These portions are:
 *   Copyright (c) 2000-2002 Jorg Pleumann <joerg@pleumann.de>
 *
 * At this point, the only portions still from Mail4ME are
 * fragments of the "execute" methods.
 */

package org.logicprobe.LogicMail.mail;

import java.io.IOException;
import java.util.Vector;
import net.rim.device.api.util.Arrays;
import org.logicprobe.LogicMail.mail.MailClient.FolderItem;
import org.logicprobe.LogicMail.conf.AccountConfig;

/**
 * 
 * Implements the IMAP client
 * 
 */
public class ImapClient extends MailClient {
    /**
     * Counts the commands executed so far in this session. Every command of an
     * IMAP session needs a unique ID that is prepended to the command line.
     */
    private int commandCount = 0;
    
    /**
     * Delmiter between folder names in the hierarchy
     */
    private String folderDelim = "";

    /**
     * Active mailbox path, so most commands do not need
     * to take a mailbox path parameter.  This makes it easier
     * to provide a common front-end that still works for
     * protocols that do not support a mailbox hierarchy.
     */
    private FolderItem activeMailbox = null;

    /**
     * Container for LIST replies
     */
    private static class ListResponse {
        public boolean hasChildren;
        public boolean canSelect;
        public String delim;
        public String name;
    };

    public ImapClient(AccountConfig acctCfg) {
        super(acctCfg);
    }
    
    public boolean hasFolders() {
        return true;
    }
    
    public void open() throws IOException, MailException {
        super.open();
        activeMailbox = null;
        try {
            // Authenticate with the server
            execute("LOGIN", acctCfg.getServerUser() + " " + acctCfg.getServerPass());

            // Retrieve server settings and capabilities
            Vector resp = executeList("", "");
            if(resp.size() > 0)
                folderDelim = ((ListResponse)resp.elementAt(0)).delim;
        } catch (MailException exp) {
            close();
            String msg = exp.getMessage();
            if(msg.startsWith("NO")) msg = msg.substring(msg.indexOf(' ')+1);
            throw new MailException(msg);
        }
    }
    
    public void close() throws IOException, MailException {
        if(connection.isConnected()) {
            if(activeMailbox != null && !activeMailbox.equals(""))
                execute("CLOSE", null, null);
            execute("LOGOUT", null, null);
        }
        activeMailbox = null;
        super.close();
    }

    public void setActiveMailbox(FolderItem mailbox) throws IOException, MailException {
        this.activeMailbox = mailbox;
        // change active mailbox
        String[] selVec = execute("SELECT", "\""+activeMailbox.path+"\"");
        // ideally, this should parse out the message counts
        // and populate the appropriate fields of the activeMailbox FolderItem
        int p, q;
        for(int i=0;i<selVec.length;i++) {
            String rowText = selVec[i];
            if(rowText.endsWith("EXISTS")) {
                p = rowText.indexOf(' ');
                q = rowText.indexOf(' ', p+1);
                if(q != -1 && p != -1 && q > p) {
                    try {
                        activeMailbox.msgCount = Integer.parseInt(rowText.substring(p+1, q));
                    } catch (Exception e) {
                        activeMailbox.msgCount = 0;
                    }
                }
            }
        }
    }
    
    public FolderItem getActiveMailbox() {
        return activeMailbox;
    }

    /**
     * Get the folder listing
     * @param baseFolder Base folder to search under, or "" for the root
     * @return List of folder items
     */
    public Vector getFolderList(String baseFolder) throws IOException, MailException {
        Vector folders = new Vector();
        Vector respList = executeList(baseFolder, "%");
        for(int i=0;i<respList.size();i++) {
            ListResponse resp = (ListResponse)respList.elementAt(i);
            if(resp.canSelect) {
                folders.addElement(resp.name);
                if(resp.hasChildren) {
                    Vector childList = getFolderList(resp.name + resp.delim);
                    for(int j=0;j<childList.size();j++)
                        folders.addElement(childList.elementAt(j));
                }
            }
        }
        return folders;
    }

    /**
     * Get relevant stats for the folder.
     * This method parses out the folder path, determines the
     * message counts, and provides generally useful information
     * for the UI to display.
     * @param folderPath Folder path string
     * @return Folder item object
     */
    public FolderItem getFolderItem(String folderPath) throws IOException, MailException {
        FolderItem item = new FolderItem();
        item.path = folderPath;
        item.delim = folderDelim;
        item.msgCount = 0;
        int pos = 0;
        int i = 0;
        while((i = folderPath.indexOf(folderDelim, i)) != -1)
            if(i != -1) { pos = i+1; i++; }
        item.name = folderPath.substring(pos);
        
        return item;
    }

    public Vector getMessageEnvelopes(int firstIndex, int lastIndex) throws IOException, MailException {
        Vector envList = new Vector();

        String[] rawList = execute("FETCH",
                                   Integer.toString(firstIndex) +
                                   ":" +
                                   Integer.toString(lastIndex) +
                                   " (ENVELOPE)");
        
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

        for(int i=0;i<rawList2.size();i++) {
            try {
                Message.Envelope env = ImapParser.parseMessageEnvelope((String)rawList2.elementAt(i));
                envList.addElement(env);
            } catch (Exception exp) {
                System.out.println("Parse error: " + exp);
            }
        }
        return envList;
    }
        
    public String getMessageBody(int index, int bindex) throws IOException, MailException {
        if(activeMailbox.equals("")) throw new MailException("Mailbox not selected");
        
        String[] rawList = execute("FETCH", index + " (BODY["+ (bindex+1) +"])");

        if(rawList.length <= 1) return "";
        
        StringBuffer msgBuf = new StringBuffer();
        for(int i=1;i<rawList.length-1;i++) {
            msgBuf.append(rawList[i] + "\n");
        }
        String lastLine = rawList[rawList.length-1];
        msgBuf.append(lastLine.substring(0, lastLine.lastIndexOf(')')));

        return msgBuf.toString();
    }

    public Message.Structure getMessageStructure(int index) throws IOException, MailException {
        if(activeMailbox.equals("")) throw new MailException("Mailbox not selected");

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

        Message.Structure msgStructure = null;
        for(int i=0;i<rawList2.size();i++) {
            try {
                msgStructure = ImapParser.parseMessageStructure((String)rawList2.elementAt(i));
            } catch (Exception exp) {
                System.out.println("Parse error: " + exp);
            }
        }
        
        return msgStructure;
    }

    /**
     * Execute a LIST command, and return a fully parsed response
     * @param refName Reference name
     * @param mboxName Mailbox name or wildcards (i.e. "%")
     * @return Vector of ListResponse objects
     */
    private Vector executeList(String refName, String mboxName) throws IOException, MailException {
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
     * Handles a request/response pair. This is a convenience method used
     * internally to handle sending a request to the IMAP server as well as
     * receiving the response. If the response starts with a "-" sign, and thus
     * denotes a protocol error, an exception is raised to reflect it. Note that
     * the request is only sent if it doesn't equal null, while the response is
     * always being waited for.
     *
     * The message parameter is ignored, but kept for now to maintain this
     * interface and not break other code.
     *
     * @see MailException
     */
    private String execute(String command, String arguments, Object message) throws IOException, MailException {
        String result = null;

        String tag = "A" + commandCount++ + " ";
        connection.send(tag + command + (arguments == null ? "" : " " + arguments));

        String temp = connection.receive();
        while (!temp.startsWith(tag)) {
            if (temp.indexOf(" " + command + " ") != -1) {
                int p = temp.indexOf('(');
                int q = temp.indexOf(')', p + 1);

                if (p != -1) {
                    if (q > p) {
                        result = temp.substring(p + 1, q);
                    }
                    else if (message != null) {
                        int left = temp.indexOf('{');
                        int right = temp.indexOf('}', left);
                        message = null;
                        //receiveMessage(message, Integer.parseInt(temp.substring(left + 1, right)));
                    }
                }
            }
            temp = connection.receive();
        }

        temp = temp.substring(tag.length());
        if (temp.startsWith("BAD ") || temp.startsWith("NO ")) {
            throw new MailException(temp);
        }

        return result;
    }    

    /**
     * Variation on the normal execute command that does not
     * attempt to parse the reply text
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

