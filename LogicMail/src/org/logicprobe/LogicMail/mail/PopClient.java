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
import org.logicprobe.LogicMail.conf.MailSettings;
import org.logicprobe.LogicMail.mail.MailClient.FolderItem;
import org.logicprobe.LogicMail.util.StringParser;

/**
 * 
 * Implements the POP3 client
 * 
 */
public class PopClient extends MailClient {
    /**
     * Active mailbox.  Since POP3 does not support multiple
     * mailboxes for a user, it is used to contain some
     * relevant information for the user's single mailbox.
     */
    private FolderItem activeMailbox = null;
    
    /** Body sections for the active message */
    private String[] bodySections = null;
    
    /** The active message */
    private Message.Envelope activeMessage = null;
    
    /** Creates a new instance of PopClient */
    public PopClient(AccountConfig acctCfg) {
        super(acctCfg);
        
        // Create our dummy folder item for the inbox
        activeMailbox = new FolderItem();
        activeMailbox.name = "INBOX";
        activeMailbox.path = "INBOX";
        activeMailbox.delim = "";
        activeMailbox.msgCount = 0;
    }

    public void open() throws IOException, MailException {
        super.open();
        
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
        super.close();
    }

    public boolean hasFolders() {
        return false;
    }

    public void setActiveMailbox(MailClient.FolderItem mailbox) throws IOException, MailException {
        // Mailbox cannot be changed, so we just pull the message counts
        String result = execute("STAT");
        int p = result.indexOf(' ');
        int q = result.indexOf(' ', p+1);
        activeMailbox.msgCount = Integer.parseInt(result.substring(p+1, q));
    }

    public MailClient.FolderItem getActiveMailbox() {
        return activeMailbox;
    }

    public Vector getMessageEnvelopes(int firstIndex, int lastIndex) throws IOException, MailException {
        Vector envList = new Vector();
        String[] headerText;
        Message.Envelope env;
        for(int i=firstIndex; i<lastIndex; i++) {
            headerText = executeFollow("TOP " + (i+1) + " 0");
            env = PopParser.parseMessageEnvelope(headerText);
            env.index = i;
            envList.addElement(env);
        }
        return envList;
    }

    /**
     * Return the structure of the message.
     * Since POP does not give us the ability to do this independently of
     * retrieving the whole message itself, we will just download the whole
     * message here and cache it for use in separate calls to getMessageBody().
     */
    public Message.Structure getMessageStructure(Message.Envelope env) throws IOException, MailException {
        // Figure out the max number of lines, using the byte-count
        // specified in the user preferences, and assuming a line
        // is 80 characters wide.
        int maxLines = MailSettings.getInstance().getGlobalConfig().getPopMaxLines();

        // Set the requested message to the active one.
        activeMessage = env;
        
        // Now download the message text
        String[] message = executeFollow("TOP " + (env.index+1) + " " + maxLines);
        int i = 0;
        // Find the end of the headers
        while(i < message.length) {
            if(message[i].equals("")) {
                i++;
                break;
            }
            i++;
        }
        if(i == message.length) {
            bodySections = null;
            return env.structure;
        }

        // First handle the simple case of a non-multipart message
        if(env.structure.boundary == null) {
            // Now turn the message body into one big string,
            // and return it.
            StringBuffer buf = new StringBuffer();
            while(i < message.length) {
                buf.append(message[i] + "\r\n");
                i++;
            }
            bodySections = new String[1];
            try {
                bodySections[0] = new String(buf.toString().getBytes(), env.structure.sections[0].charset);
            } catch (Exception e) {
                bodySections[0] = buf.toString();
            }
            env.structure.sections[0].size = bodySections[0].length();
        }
        // Now handle multi-part messages
        else {
            bodySections = PopParser.parseMultipartMessage(env, message, i);
        }
        return env.structure;
    }

    public String getMessageBody(Message.Envelope env, int bindex) throws IOException, MailException {
        // Handle a case of mis-ordering of use, to make sure we have
        // the right message cached for retrieval
        if(activeMessage != env) getMessageStructure(env);
        
        // Return the requested section of the message
        if(bodySections != null &&
           bindex < bodySections.length &&
           bodySections[bindex] != null)
            return bodySections[bindex];
        else
            return null;
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
        String result = execute(command);
        if(result.indexOf("follows") == -1)
            throw new MailException("Command lacks a multi-line response");
            
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
