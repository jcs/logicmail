/*
 * ImapClient.java
 *
 * Portions of this code may have been inspired by and/or
 * copied from the following classes of the Mail4ME project:
 * de.trantor.mail.ImapClient
 * These portions are:
 *   Copyright (c) 2000-2002 Jorg Pleumann <joerg@pleumann.de>
 */

package org.logicprobe.LogicMail.mail;

import org.logicprobe.LogicMail.conf.AccountConfig;
import org.logicprobe.LogicMail.util.StringParser;
import java.io.IOException;
import java.util.Vector;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Implements the IMAP client
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

    public String getFolderDelim() {
        return folderDelim;
    }

    public void setActiveMailbox(FolderItem mailbox) throws IOException, MailException {
        this.activeMailbox = mailbox;
        // change active mailbox
        Vector selVec = execute("SELECT", "\""+activeMailbox.path+"\"");
        // ideally, this should parse out the message counts
        // and populate the appropriate fields of the activeMailbox FolderItem
        int p, q;
        for(int i=0;i<selVec.size();i++) {
            String rowText = (String)selVec.elementAt(i);
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
        
        //String bakMailbox = activeMailbox;
        //activeMailbox = folderPath;
        // Message counts should be disabled for performance
        // reasons, but cannot be until SELECT parsing
        // is implemented.
        //item.msgCount = getMessageCount();
        //activeMailbox = bakMailbox;
        
        return item;
    }

    /**
     * Receives a message. This method receives a whole message from the server
     * and stores the header and body parts in the according vectors. It is able
     * to undo any byte stuffing produced by the server. It also undoes header
     * folding in a way, putting multiple header lines that belong to one
     * field into a single line of the header vector.
     * <p>
     * The method assumes that either a "RETR" or a "TOP" command has already
     * been issued, so that it can only be called from the getMessage() and
     * getHeader() methods (whom it serves as an internal helper method).
     *
     * @see #getMessage
     * @see #getHeaders
     */
    private void receiveMessage(Message message, int size) throws IOException, MailException {
        // Rewrite this one
        //int count = 0;

        /**
         * First we read the header lines. The end of the header is denoted by
         * an empty line.
         */
        String buffer = connection.receive();
        int octets = buffer.length() + 2;

        while (!(buffer.equals(""))) {
            /**
             * Undo header folding, that is, put logical header lines that span
             * multiple physical ones into one vector entry. This eases dealing
             * with header fields a lot.
             */
            if (buffer.startsWith(" ") || buffer.startsWith("\t")) {
                //message.setHeaderLine(count - 1, message.getHeaderLine(count - 1) + "\r\n" + buffer);
            }
            else {
                //message.addHeaderLine(buffer);
                //count++;
            }

            buffer = connection.receive();
            octets = octets + buffer.length() + 2;
        }

        /**
         * Next we read the body lines. The end of the body is denoted by a line
         * consisting only of a dot (which is the usual end of multiline respones).
         */
        while (octets < size) {
            buffer = connection.receive();
            octets = octets + buffer.length() + 2;
            //message.addBodyLine(buffer);
        }
    }

    public int getMessageCount() throws IOException, MailException {
        String buffer = execute("STATUS", "\""+activeMailbox+"\" (MESSAGES)", null);

        /**
         * The result of the "STAT" request should always be "+OK <#msgs> <#bytes>",
         * so we simply fetch the number between the first and the second space
         * (and keep our fingers crossed that every POP3 implementation follows the
         * RFC).
         */
        int space = buffer.indexOf(' ');

        return Integer.parseInt(buffer.substring(space + 1));
    }


    public Vector getMessageEnvelopes(int firstIndex, int lastIndex) throws IOException, MailException {
        Vector envList = new Vector();
        Vector rawList = execute("FETCH",
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
        for(int i=0;i<rawList.size();i++) {
            line = (String)rawList.elementAt(i);
            if(line.length() > 0 &&
                    lineBuf.toString().startsWith("* ") &&
                    line.startsWith("* ")) {
                rawList2.addElement(lineBuf.toString());
                lineBuf = new StringBuffer();
            }
            lineBuf.append(line);
            if(i == rawList.size()-1 && lineBuf.toString().startsWith("* "))
                rawList2.addElement(lineBuf.toString());
        }

        for(int i=0;i<rawList2.size();i++) {
            try {
                Message.Envelope env = parseMessageEnvelope((String)rawList2.elementAt(i));
                envList.addElement(env);
            } catch (Exception exp) {
                System.out.println("Parse error: " + exp);
            }
        }
        return envList;
    }

    private Message.Envelope generateDummyEnvelope() {
        Message.Envelope env = new Message.Envelope();
        env.date = Calendar.getInstance().getTime();
        env.from = new String[1];
        env.from[0] = "<sender>";
        env.subject = "<subject>";
        return env;
    }
    
    private Message.Envelope parseMessageEnvelope(String rawText) {
        Vector parsedText = null;
        try {
            parsedText = StringParser.parseNestedParenString(rawText.substring(rawText.indexOf('(')));
        } catch (Exception exp) {
            return generateDummyEnvelope();
        }

        // Sanity checking
        if(parsedText.size() < 2 ||
           !(parsedText.elementAt(1) instanceof Vector))
           return generateDummyEnvelope();
        
        Vector parsedEnv = (Vector)parsedText.elementAt(1);

        // More sanity checking
        if(parsedEnv.size() < 10)
           return generateDummyEnvelope();
            
        Message.Envelope env = new Message.Envelope();

        if(parsedEnv.elementAt(0) instanceof String) {
            env.date = StringParser.parseDateString((String)parsedEnv.elementAt(0));
        }
        
        if(parsedEnv.elementAt(1) instanceof String) {
            env.subject = (String)parsedEnv.elementAt(1);
        }

        if(parsedEnv.elementAt(2) instanceof Vector) {
            env.from = parseAddressList((Vector)parsedEnv.elementAt(2));
        }
        
        if(parsedEnv.elementAt(3) instanceof Vector) {
            env.sender = parseAddressList((Vector)parsedEnv.elementAt(3));
        }

        if(parsedEnv.elementAt(4) instanceof Vector) {
            env.replyTo = parseAddressList((Vector)parsedEnv.elementAt(4));
        }

        if(parsedEnv.elementAt(5) instanceof Vector) {
            env.to = parseAddressList((Vector)parsedEnv.elementAt(5));
        }

        if(parsedEnv.elementAt(6) instanceof Vector) {
            env.cc = parseAddressList((Vector)parsedEnv.elementAt(6));
        }

        if(parsedEnv.elementAt(7) instanceof Vector) {
            env.bcc = parseAddressList((Vector)parsedEnv.elementAt(7));
        }

        if(parsedEnv.elementAt(8) instanceof String) {
            env.inReplyTo = (String)parsedEnv.elementAt(8);
            if(env.inReplyTo.equals("NIL")) env.inReplyTo = "";
        }

        if(parsedEnv.elementAt(9) instanceof String) {
            env.messageId = (String)parsedEnv.elementAt(9);
            if(env.messageId.equals("NIL")) env.messageId = "";
        }
        
        env.isOpened = false;
        
        env.index = Integer.parseInt(rawText.substring(rawText.indexOf(' '), rawText.indexOf("FETCH")-1).trim());
        return env;
    }

    private String[] parseAddressList(Vector addrVec) {
        // Find the number of addresses, and allocate the array
        String[] addrList = new String[addrVec.size()];
        int index = 0;
        
        for(int i=0;i<addrVec.size();i++) {
            if((addrVec.elementAt(i) instanceof Vector) &&
               ((Vector)addrVec.elementAt(i)).size() >= 4) {
                
                Vector entry = (Vector)addrVec.elementAt(i);

                String realName = "NIL";
                if(entry.elementAt(0) instanceof String)
                    realName = (String)entry.elementAt(0);

                String mbName = "NIL";
                if(entry.elementAt(2) instanceof String)
                    mbName = (String)entry.elementAt(2);

                String hostName = "NIL";
                if(entry.elementAt(3) instanceof String)
                    hostName = (String)entry.elementAt(3);

                // Now assemble these into a single address entry
                // (possibly eventually storing them separately)
                if(realName.length() > 0 && !realName.equals("NIL"))
                    addrList[index] = realName + " <" + mbName + "@" + hostName + ">";
                else
                    addrList[index] = mbName + "@" + hostName;
                index++;
            }
        }
        return addrList;
    }
    
    public Message getMessage(int index) throws IOException, MailException {
        if(activeMailbox.equals("")) throw new MailException("Mailbox not selected");
        Message message = new Message();
        return message;
    }

    public String getMessageBody(int index, int bindex) throws IOException, MailException {
        if(activeMailbox.equals("")) throw new MailException("Mailbox not selected");
        
        Vector rawList = execute("FETCH", index + " (BODY["+ (bindex+1) +"])");

        if(rawList.size() <= 1) return "";
        
        StringBuffer msgBuf = new StringBuffer();
        for(int i=1;i<rawList.size()-1;i++) {
            msgBuf.append((String)rawList.elementAt(i) + "\n");
        }
        String lastLine = (String)rawList.elementAt(rawList.size()-1);
        msgBuf.append(lastLine.substring(0, lastLine.lastIndexOf(')')));

        return msgBuf.toString();
    }

    public Message.Structure getMessageStructure(int index) throws IOException, MailException {
        if(activeMailbox.equals("")) throw new MailException("Mailbox not selected");

        Vector rawList = execute("FETCH", index + " (BODYSTRUCTURE)");

        // Pre-process the returned text to clean up mid-field line breaks
        // This should all become unnecessary once execute()
        // becomes more intelligent in how it handles replies
        String line;
        StringBuffer lineBuf = new StringBuffer();
        Vector rawList2 = new Vector();
        for(int i=0;i<rawList.size();i++) {
            line = (String)rawList.elementAt(i);
            if(line.length() > 0 &&
                    lineBuf.toString().startsWith("* ") &&
                    line.startsWith("* ")) {
                rawList2.addElement(lineBuf.toString());
                lineBuf = new StringBuffer();
            }
            lineBuf.append(line);
            if(i == rawList.size()-1 && lineBuf.toString().startsWith("* "))
                rawList2.addElement(lineBuf.toString());
        }

        Message.Structure msgStructure = null;
        for(int i=0;i<rawList2.size();i++) {
            try {
                msgStructure = parseMessageStructure((String)rawList2.elementAt(i));
            } catch (Exception exp) {
                System.out.println("Parse error: " + exp);
            }
        }
        
        return msgStructure;
    }

        private Message.Structure parseMessageStructure(String rawText) {
        Vector parsedText = null;
        try {
            parsedText = StringParser.parseNestedParenString(rawText.substring(rawText.indexOf('(')));
        } catch (Exception exp) {
            return null;
        }

        // Sanity checking
        if(parsedText.size() < 2 ||
           !(parsedText.elementAt(1) instanceof Vector))
           return null;
        
        Vector parsedStruct = (Vector)parsedText.elementAt(1);

        Message.Structure msgStructure = new Message.Structure();

        // Determine the number of body parts and parse
        if(parsedStruct.elementAt(0) instanceof String) {
            msgStructure.sections = new Message.Section[1];
            msgStructure.sections[0] = parseMessageStructureSection(parsedStruct);
        }
        else {
            int count = 0;
            int i;
            for(i=0;i<parsedStruct.size();i++)
                if(parsedStruct.elementAt(i) instanceof Vector)
                    count++;
                else
                    break;
            msgStructure.sections = new Message.Section[count];
            for(i=0;i<count;i++)
                msgStructure.sections[i] = parseMessageStructureSection((Vector)parsedStruct.elementAt(i));
        }
        
        return msgStructure;
    }

    private Message.Section parseMessageStructureSection(Vector sectionList) {
        Message.Section sec = new Message.Section();
        
        if(sectionList.elementAt(0) instanceof String) {
            sec.type = ((String)sectionList.elementAt(0)).toLowerCase();
        }

        if(sectionList.elementAt(1) instanceof String) {
            sec.subtype = ((String)sectionList.elementAt(1)).toLowerCase();
        }

        if(sectionList.elementAt(5) instanceof String) {
            sec.encoding = ((String)sectionList.elementAt(5)).toLowerCase();
        }

        if(sectionList.elementAt(6) instanceof String) {
            try {
                sec.size = Integer.parseInt((String)sectionList.elementAt(6));
            } catch (Exception exp) {
                sec.size = -1;
            }
        }

        return sec;
    }

    /**
     * Execute a LIST command, and return a fully parsed response
     * @param refName Reference name
     * @param mboxName Mailbox name or wildcards (i.e. "%")
     * @return Vector of ListResponse objects
     */
    private Vector executeList(String refName, String mboxName) throws IOException, MailException {
        Vector results;
        results = execute("LIST", "\""+refName+"\" \""+mboxName+"\"");
        
        Vector retVec = new Vector(results.size());
        ListResponse response;
        String temp;
        String flagStr;
        String argStr;
        int p;
        int q;
        for(int i=0;i<results.size();i++) {
            // Separate out the flag and argument strings
            flagStr = null;
            argStr = null;
            temp = (String)results.elementAt(i);
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
     * @see MailException
     */
    private String execute(String command, String arguments, Message message) throws IOException, MailException {
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

                        receiveMessage(message, Integer.parseInt(temp.substring(left + 1, right)));
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
    private Vector execute(String command, String arguments)
        throws IOException, MailException
    {
        Vector result = new Vector();

        String tag = "A" + commandCount++ + " ";
        connection.send(tag + command + (arguments == null ? "" : " " + arguments));

        String temp = connection.receive();
        while (!temp.startsWith(tag)) {
            result.addElement(temp);
            temp = connection.receive();
        }

        temp = temp.substring(tag.length());
        if (temp.startsWith("BAD ") || temp.startsWith("NO ")) {
            throw new MailException(temp);
        }
        return result;
    }
}

