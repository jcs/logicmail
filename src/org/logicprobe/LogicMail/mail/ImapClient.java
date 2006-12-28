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
import java.util.Vector;
import net.rim.device.api.util.Arrays;
import org.logicprobe.LogicMail.conf.AccountConfig;
import org.logicprobe.LogicMail.conf.GlobalConfig;
import org.logicprobe.LogicMail.conf.MailSettings;
import org.logicprobe.LogicMail.message.FolderMessage;
import org.logicprobe.LogicMail.message.Message;
import org.logicprobe.LogicMail.message.MessageEnvelope;
import org.logicprobe.LogicMail.message.MessagePart;
import org.logicprobe.LogicMail.message.MessagePartFactory;
import org.logicprobe.LogicMail.message.MultiPart;
import org.logicprobe.LogicMail.util.Connection;

/**
 * 
 * Implements the IMAP client
 * 
 */
public class ImapClient implements MailClient {
    protected GlobalConfig globalConfig;
    protected AccountConfig acctCfg;
    protected Connection connection;

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
    private FolderTreeItem activeMailbox = null;

    /**
     * Container for LIST replies
     */
    private static class ListResponse {
        public boolean hasChildren;
        public boolean canSelect;
        public String delim;
        public String name;
    };

    public ImapClient(GlobalConfig globalConfig, AccountConfig acctCfg) {
        this.acctCfg = acctCfg;
        this.globalConfig = globalConfig;
        connection = new Connection(acctCfg);
    }
    
    public AccountConfig getAcctConfig() {
        return acctCfg;
    }

    public void open() throws IOException, MailException {
        connection.open();
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
                execute("CLOSE", null);
            execute("LOGOUT", null);
        }
        activeMailbox = null;
        connection.close();
    }

    public boolean isConnected() {
        return connection.isConnected();
    }

    public boolean hasFolders() {
        return true;
    }

    public FolderTreeItem getFolderTree() throws IOException, MailException {
        FolderTreeItem rootItem = new FolderTreeItem("", "", folderDelim);
        getFolderTreeImpl(rootItem, 0);
        return rootItem;
    }

    private void getFolderTreeImpl(FolderTreeItem baseFolder, int depth) throws IOException, MailException {
        Vector respList;
        if(depth == 0)
            respList = executeList(baseFolder.getPath(), "%");
        else
            respList = executeList(baseFolder.getPath() + baseFolder.getDelim(), "%");

        int size = respList.size();
        for(int i=0;i<size;++i) {
            ListResponse resp = (ListResponse)respList.elementAt(i);
            if(resp.canSelect) {
                FolderTreeItem childItem = getFolderItem(baseFolder, resp.name);
                baseFolder.addChild(childItem);
                if(resp.hasChildren) {
                    if(depth+1 >= globalConfig.getImapMaxFolderDepth())
                        return;
                    getFolderTreeImpl(childItem, depth+1);
                }
            }
        }
    }

    public FolderTreeItem getActiveFolder() {
        return activeMailbox;
    }

    public void setActiveFolder(FolderTreeItem mailbox) throws IOException, MailException {
        this.activeMailbox = mailbox;
        // change active mailbox
        String[] selVec = execute("SELECT", "\""+activeMailbox.getPath()+"\"");
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
                        activeMailbox.setMsgCount(Integer.parseInt(rowText.substring(p+1, q)));
                    } catch (Exception e) {
                        activeMailbox.setMsgCount(0);
                    }
                }
            }
        }
    }

    public FolderMessage[] getFolderMessages(int firstIndex, int lastIndex) throws IOException, MailException {
        FolderMessage[] folderMessages = new FolderMessage[(lastIndex - firstIndex)+1];
        int index = 0;

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
                String rawText = (String)rawList2.elementAt(i);
                MessageEnvelope env = ImapParser.parseMessageEnvelope(rawText);
                int midx = Integer.parseInt(rawText.substring(rawText.indexOf(' '), rawText.indexOf("FETCH")-1).trim());
                folderMessages[index++] = new FolderMessage(env, midx);
            } catch (Exception exp) {
                System.out.println("Parse error: " + exp);
            }
        }
        return folderMessages;
    }

    public Message getMessage(FolderMessage folderMessage) throws IOException, MailException {
        ImapParser.MessageSection structure = getMessageStructure(folderMessage.getIndex());
        MessagePart rootPart =
            getMessagePart(folderMessage.getIndex(),
                           structure, globalConfig.getImapMaxMsgSize());
        Message msg = new Message(folderMessage.getEnvelope(), rootPart);
        return msg;
    }

    private MessagePart getMessagePart(int index,
                                       ImapParser.MessageSection structure,
                                       int maxSize)
        throws IOException, MailException
    {
        MessagePart part;
        if(MessagePartFactory.isMessagePartSupported(structure.type, structure.subtype)) {
            String data;
            if(structure.type.equalsIgnoreCase("multipart"))
                data = null;
            else {
                if(structure.size < maxSize) {
                    data = getMessageBody(index, structure.address);
                    maxSize -= structure.size;
                }
                else {
                    // We hit the size limit, so stop processing
                    return null;
                }
            }
            part = MessagePartFactory.createMessagePart(structure.type, structure.subtype, structure.encoding, structure.charset, data);
        }
        else
            part = null;

        if((part instanceof MultiPart)&&(structure.subsections != null)&&(structure.subsections.length > 0))
            for(int i=0;i<structure.subsections.length;i++) {
                MessagePart subPart = getMessagePart(index, structure.subsections[i], maxSize);
                if(subPart != null)
                    ((MultiPart)part).addPart(subPart);
            }
        return part;
    }
    
    /**
     * Returns the message structure tree independent of message content.
     * This tree is used to build the final message tree.
     */
    private ImapParser.MessageSection getMessageStructure(int msgIndex) throws IOException, MailException {
        if(activeMailbox.equals("")) throw new MailException("Mailbox not selected");

        String[] rawList = execute("FETCH", msgIndex + " (BODYSTRUCTURE)");

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
     * Create a new folder item, doing the relevant parsing on the path.
     * @param parent Parent folder
     * @param folderPath Folder path string
     * @return Folder item object
     */
    private FolderTreeItem getFolderItem(FolderTreeItem parent, String folderPath) throws IOException, MailException {
        int pos = 0;
        int i = 0;
        while((i = folderPath.indexOf(folderDelim, i)) != -1)
            if(i != -1) { pos = i+1; i++; }
        FolderTreeItem item = new FolderTreeItem(parent, folderPath.substring(pos), folderPath, folderDelim);
        item.setMsgCount(0);
        return item;
    }

    private String getMessageBody(int index, String address) throws IOException, MailException {
        if(activeMailbox.equals("")) throw new MailException("Mailbox not selected");
        
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
