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
    private String activeMailbox = "";

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
        activeMailbox = "";
        try {
            // Authenticate with the server
            execute("LOGIN", acctCfg.getServerUser() + " " + acctCfg.getServerPass(), null);
            // Retrieve server settings and capabilities
            Vector resp = executeList("", "");
            if(resp.size() > 0)
                folderDelim = ((ListResponse)resp.elementAt(0)).delim;
        } catch (MailException exp) {
            close();
            throw exp;
        }
    }
    
    public void close() throws IOException, MailException {
        if(connection.isConnected()) {
            if(!activeMailbox.equals(""))
                execute("CLOSE", null, null);
            execute("LOGOUT", null, null);
        }
        activeMailbox = "";
        super.close();
    }

    public String getFolderDelim() {
        return folderDelim;
    }

    public void setActiveMailbox(String mailbox) throws IOException, MailException {
        this.activeMailbox = mailbox;
        // change active mailbox
        execute("SELECT", "\""+activeMailbox+"\"", null);
        // ideally, this should parse out the message counts
        // and populate the appropriate fields of a FolderItem
    }
    
    public String getActiveMailbox() {
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
        int pos = 0;
        int i = 0;
        while((i = folderPath.indexOf(folderDelim, i)) != -1)
            if(i != -1) { pos = i+1; i++; }
        item.name = folderPath.substring(pos);
        String bakMailbox = activeMailbox;
        activeMailbox = folderPath;
        // Message counts should be disabled for performance
        // reasons, but cannot be until SELECT parsing
        // is implemented.
        item.msgCount = getMessageCount();
        
        activeMailbox = bakMailbox;
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
        int count = 0;

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
                message.setHeaderLine(count - 1, message.getHeaderLine(count - 1) + "\r\n" + buffer);
            }
            else {
                message.addHeaderLine(buffer);
                count++;
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
            message.addBodyLine(buffer);
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
        // pre-process the returned text to clean up mid-field line breaks
        String line;
        StringBuffer lineBuf = new StringBuffer();
        Vector rawList2 = new Vector();
        for(int i=0;i<rawList.size();i++) {
            line = (String)rawList.elementAt(i);
            if(line.length() > 0 && line.startsWith("* ")) {
                rawList2.addElement(lineBuf.toString());
                lineBuf = new StringBuffer();
            }
            lineBuf.append(line);
        }
        for(int i=0;i<rawList2.size();i++) {
            //System.out.println((String)rawList2.elementAt(i));
            try {
                MessageEnvelope env = parseMessageEnvelope((String)rawList2.elementAt(i));
                // the index could be parsed, but this is quicker for now
                env.index = firstIndex+i;
                envList.addElement(env);
            } catch (Exception exp) {
                System.out.println("Parse error: " + exp);
            }
        }
        return envList;
    }

    private MessageEnvelope parseMessageEnvelope(String rawText) {
        MessageEnvelope env = new MessageEnvelope();
        
        int p = 0;
        int q = 0;
        
        // Separate and parse date string
        p = rawText.indexOf("\"");
        q = rawText.indexOf("\"", p+1);
        env.date = parseDateString(rawText.substring(p+1, q));
        
        // Parse the subject
        p = rawText.indexOf("\"", q+1);
        q = rawText.indexOf("\"", p+1);
        env.subject = rawText.substring(p+1, q);

        p = rawText.indexOf("(", q+1);
        q = rawText.indexOf("))", p+1)+1;
        try {
            env.from = parseAddressList(rawText.substring(p+1, q));
        } catch (Exception e) {
            env.from = new String[1];
            env.from[0] = "<sender>";
        }
        
        
        env.isOpened = false;
        return env;
    }

    private String[] parseAddressList(String rawText) {
        int index = 0;
        int maxIndex = 0;
        
        // Find the number of addresses, and allocate the array
        for(int i=0;i<rawText.length();i++)
            if(rawText.charAt(i) == '(') maxIndex++;
        String[] addrList = new String[maxIndex];
        
        int p = 0;
        int q = 0;
        while(index < maxIndex) {
            p = rawText.indexOf("(", q)+1;
            if(rawText.charAt(p) == '\"')
                q = rawText.indexOf("\"", p+1)+1;
            else
                q = rawText.indexOf(" ", p+1);
            String realName = parseQuotableString(rawText.substring(p, q));
            
            p = q+1;
            if(rawText.charAt(p+1) == '\"')
                q = rawText.indexOf("\"", p+1)+1;
            else
                q = rawText.indexOf(" ", p+1);
            String srcRoute = parseQuotableString(rawText.substring(p, q));
            
            p = q+1;
            if(rawText.charAt(p+1) == '\"')
                q = rawText.indexOf("\"", p+1)+1;
            else
                q = rawText.indexOf(" ", p+1);
            String mbName = parseQuotableString(rawText.substring(p, q));

            p = q+1;
            if(rawText.charAt(p+1) == '\"')
                q = rawText.indexOf("\"", p+1)+1;
            else
                q = rawText.indexOf(")", p+1);
            String hostName = parseQuotableString(rawText.substring(p, q));

            // Now assemble these into a single address entry
            // (possibly eventually storing them separately)
            if(realName.length() > 0)
                addrList[index] = realName + " <" + mbName + "@" + hostName + ">";
            else
                addrList[index] = mbName + "@" + hostName;
            index++;
        }        
        return addrList;
    }
    
    /**
     * Process a string that could be quoted,
     * unquoted, or "NIL", stripping the quotes
     * and cleaning things up.
     * @param rawText Raw input string
     * @return Cleaned up output string
     */
    private String parseQuotableString(String rawText) {
        if(rawText == null || rawText.length() == 0)
            return "";
        if(rawText.length() < 2)
            return rawText;
        if(rawText.equals("NIL"))
            return "";
        if(rawText.charAt(0) == '\"' && rawText.charAt(rawText.length()-1) == '\"')
            return rawText.substring(1, rawText.length()-1);
        return "";
    }
    
    private Date parseDateString(String rawDate) {
        int p = 0;
        int q = 0;
        
        // Clean up the date string for simple parsing
        p = rawDate.indexOf(",");
        if(p != -1) {
            p++;
            while(rawDate.charAt(p)==' ') p++;
            rawDate = rawDate.substring(p);
        }
        if(rawDate.charAt(rawDate.length()-1) == ')')
            rawDate = rawDate.substring(0, rawDate.lastIndexOf(' '));

        // Set the time zone
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"+rawDate.lastIndexOf(' ')+1));

        p = 0;
        q = rawDate.indexOf(" ", p+1);
        cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(rawDate.substring(p, q)));

        p = q+1;
        q = rawDate.indexOf(" ", p+1);
        String monthStr = rawDate.substring(p, q);
        if(monthStr.equals("Jan")) cal.set(Calendar.MONTH, Calendar.JANUARY);
        else if(monthStr.equals("Feb")) cal.set(Calendar.MONTH, Calendar.FEBRUARY);
        else if(monthStr.equals("Mar")) cal.set(Calendar.MONTH, Calendar.MARCH);
        else if(monthStr.equals("Apr")) cal.set(Calendar.MONTH, Calendar.APRIL);
        else if(monthStr.equals("May")) cal.set(Calendar.MONTH, Calendar.MAY);
        else if(monthStr.equals("Jun")) cal.set(Calendar.MONTH, Calendar.JUNE);
        else if(monthStr.equals("Jul")) cal.set(Calendar.MONTH, Calendar.JULY);
        else if(monthStr.equals("Aug")) cal.set(Calendar.MONTH, Calendar.AUGUST);
        else if(monthStr.equals("Sep")) cal.set(Calendar.MONTH, Calendar.SEPTEMBER);
        else if(monthStr.equals("Oct")) cal.set(Calendar.MONTH, Calendar.OCTOBER);
        else if(monthStr.equals("Nov")) cal.set(Calendar.MONTH, Calendar.NOVEMBER);
        else if(monthStr.equals("Dec")) cal.set(Calendar.MONTH, Calendar.DECEMBER);
        
        p = q+1;
        q = rawDate.indexOf(" ", p+1);
        cal.set(Calendar.YEAR, Integer.parseInt(rawDate.substring(p, q)));
        
        p = q+1;
        q = rawDate.indexOf(":", p+1);
        cal.set(Calendar.HOUR, Integer.parseInt(rawDate.substring(p, q)));
        
        p = q+1;
        q = rawDate.indexOf(":", p+1);
        cal.set(Calendar.MINUTE, Integer.parseInt(rawDate.substring(p, q)));

        p = q+1;
        q = rawDate.indexOf(" ", p+1);
        cal.set(Calendar.SECOND, Integer.parseInt(rawDate.substring(p, q)));
        cal.set(Calendar.MILLISECOND, 0);
        
        return cal.getTime();
    }
    
    public Message getMessage(int index) throws IOException, MailException {
        if(activeMailbox.equals("")) throw new MailException("Mailbox not selected");
        Message message = new Message();
        execute("FETCH", (index + 1)+ " (RFC822)", message);
        return message;
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

