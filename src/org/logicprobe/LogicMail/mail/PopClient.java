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
import net.rim.device.api.io.SharedInputStream;
import net.rim.device.api.mime.MIMEInputStream;
import net.rim.device.api.mime.MIMEParsingException;
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
import org.logicprobe.LogicMail.util.StringParser;

/**
 * 
 * Implements the POP3 client
 * 
 */
public class PopClient implements MailClient {
    protected GlobalConfig globalConfig;
    protected AccountConfig acctCfg;
    protected Connection connection;

    /**
     * Active mailbox.  Since POP3 does not support multiple
     * mailboxes for a user, it is used to contain some
     * relevant information for the user's single mailbox.
     */
    private FolderTreeItem activeMailbox = null;
    
    /** Creates a new instance of PopClient */
    public PopClient(AccountConfig acctCfg) {
        this.acctCfg = acctCfg;
        globalConfig = MailSettings.getInstance().getGlobalConfig();
        connection = new Connection(acctCfg);
        
        // Create our dummy folder item for the inbox
        activeMailbox = new FolderTreeItem("INBOX", "INBOX", "");
        activeMailbox.setMsgCount(0);
    }

    public AccountConfig getAcctConfig() {
        return acctCfg;
    }

    public void open() throws IOException, MailException {
        connection.open();
        
        try {
            execute(null);
            execute("USER " + acctCfg.getServerUser());
            execute("PASS " + acctCfg.getServerPass());
        } catch (MailException exp) {
            close();
            throw exp;
        }
    }

    public void close() throws IOException, MailException {
        if(connection.isConnected()) {
            execute("QUIT");
        }
        activeMailbox = null;
        connection.close();
    }

    public boolean isConnected() {
        return connection.isConnected();
    }

    public boolean hasFolders() {
        return false;
    }

    public FolderTreeItem getFolderTree() throws IOException, MailException {
        return null;
    }

    public FolderTreeItem getActiveFolder() {
        return activeMailbox;
    }

    public void setActiveFolder(FolderTreeItem mailbox) throws IOException, MailException {
        // Mailbox cannot be changed, so we just pull the message counts
        String result = execute("STAT");
        int p = result.indexOf(' ');
        int q = result.indexOf(' ', p+1);
        activeMailbox.setMsgCount(Integer.parseInt(result.substring(p+1, q)));
    }

    public FolderMessage[] getFolderMessages(int firstIndex, int lastIndex) throws IOException, MailException {
        FolderMessage[] folderMessages = new FolderMessage[lastIndex - firstIndex];
        int index = 0;
        String[] headerText;
        MessageEnvelope env;
        for(int i=firstIndex; i<lastIndex; i++) {
            headerText = executeFollow("TOP " + (i+1) + " 0");
            env = PopParser.parseMessageEnvelope(headerText);
            folderMessages[index++] = new FolderMessage(env, i);
        }
        return folderMessages;
    }

    public Message getMessage(FolderMessage folderMessage) throws IOException, MailException {
        // Figure out the max number of lines
        int maxLines = MailSettings.getInstance().getGlobalConfig().getPopMaxLines();

        // Download the message text
        String[] message = executeFollow("TOP " + (folderMessage.getIndex()+1) + " " + maxLines);
        
        MIMEInputStream mimeInputStream = null;
        try {
            mimeInputStream = new MIMEInputStream(StringParser.createInputStream(message));
        } catch (MIMEParsingException e) {
            return null;
        }

        MessagePart rootPart = getMessagePart(mimeInputStream);
        Message msg = new Message(folderMessage.getEnvelope(), rootPart);
        return msg;
    }

    /**
     * Recursively walk the provided MIMEInputStream, building a message
     * tree in the process.
     *
     * @param mimeInputStream MIMEInputStream of the downloaded message data
     * @return Root MessagePart element for this portion of the message tree
     */
    private MessagePart getMessagePart(MIMEInputStream mimeInputStream) throws IOException {
        // Parse out the MIME type and relevant header fields
        String mimeType = mimeInputStream.getContentType();
        String type = mimeType.substring(0, mimeType.indexOf('/'));
        String subtype = mimeType.substring(mimeType.indexOf('/') + 1);
        String encoding = mimeInputStream.getHeader("Content-Transfer-Encoding");
        String charset = mimeInputStream.getContentTypeParameter("charset");
        
        // Handle the multi-part case
        if(mimeInputStream.isMultiPart() && type.equalsIgnoreCase("multipart")) {
            MessagePart part = MessagePartFactory.createMessagePart(type, subtype, null, null, null);
            MIMEInputStream[] mimeSubparts = mimeInputStream.getParts();
            for(int i=0;i<mimeSubparts.length;i++) {
                MessagePart subPart = getMessagePart(mimeSubparts[i]);
                if(subPart != null)
                    ((MultiPart)part).addPart(subPart);
            }
            return part;
        }
        // Handle the single-part case
        else {
            byte[] buffer;
            // Handle encoded binary data (should be more encoding-agnostic)
            if(encoding.equalsIgnoreCase("base64") && mimeInputStream.isPartComplete()!=0) {
                SharedInputStream sis = mimeInputStream.getRawMIMEInputStream();
                buffer = StringParser.readWholeStream(sis);

                int offset = 0;
                while((offset+3 < buffer.length) &&
                        !(buffer[offset]=='\r' && buffer[offset+1]=='\n' &&
                        buffer[offset+2]=='\r' && buffer[offset+3]=='\n'))
                    offset++;
                int size = buffer.length - offset;
                return MessagePartFactory.createMessagePart(type, subtype, encoding, charset, new String(buffer, offset, size));
            }
            else {
                int size = mimeInputStream.available();
                buffer = new byte[mimeInputStream.available()];
                buffer = StringParser.readWholeStream(mimeInputStream);
                return MessagePartFactory.createMessagePart(type, subtype, encoding, charset, new String(buffer));
            }
        }
    }
    
    /**
     * Execute a POP3 command that returns multiple lines.
     * This works by running the normal execute() and then
     * receiving every new line until a lone "." is encountered.
     *
     * @param command The command to execute
     * @return An array of lines containing the response
     */
    private String[] executeFollow(String command) throws IOException, MailException {
        execute(command);
            
        String buffer = connection.receive();
        String[] lines = new String[0];
        while(buffer != null && !buffer.equals(".")) {
            Arrays.add(lines, buffer);
            buffer = connection.receive();
        }
        return lines;
    }
    
    /**
     * Execute a POP3 command, and return the result.
     * If the command is null, we still wait for a result
     * so we can receive a multi-line response.
     * @param command The command
     * @return The result
     */
    private String execute(String command) throws IOException, MailException {
        if(command != null) connection.send(command);
        
        String result = connection.receive();
        
        if((result.length() > 1) && (result.charAt(0) == '-')) {
            throw new MailException(result);
        }
        
        return result;
    }
}
