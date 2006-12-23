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
 * de.trantor.mail.InboxClient
 * These portions are:
 *   Copyright (c) 2000-2002 Jorg Pleumann <joerg@pleumann.de>
 * 
 */

package org.logicprobe.LogicMail.mail;

import java.io.IOException;
import java.util.Vector;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import org.logicprobe.LogicMail.conf.AccountConfig;
import org.logicprobe.LogicMail.cache.Cacheable;
import org.logicprobe.LogicMail.conf.GlobalConfig;
import org.logicprobe.LogicMail.conf.MailSettings;

/**
 * Create a generic interface to different mail protocols.
 * This class allows most of the UI code to be protocol-agnostic.
 */
public abstract class MailClient {
    /**
     * Relevant information describing a folder.
     */
    public static class FolderItem implements Cacheable {
        public String name;
        public String path;
        public String delim;
        public int msgCount;

        public byte[] serialize() {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(buffer);
            try {
                output.writeUTF(name);
                output.writeUTF(path);
                output.writeUTF(delim);
                output.writeInt(msgCount);
                return buffer.toByteArray();
            } catch (IOException exp) {
                return null;
            }
        }
        public void deserialize(byte[] byteArray) {
            ByteArrayInputStream buffer = new ByteArrayInputStream(byteArray);
            DataInputStream input = new DataInputStream(buffer);
            try {
               name = input.readUTF();
               path = input.readUTF();
               delim = input.readUTF();
               msgCount = input.readInt();
            } catch (IOException exp) { }
        }
    }

    protected GlobalConfig _globalConfig;
    protected AccountConfig acctCfg;
    protected Connection connection;
    
    protected MailClient(AccountConfig acctCfg) {
        this.acctCfg = acctCfg;
        _globalConfig = MailSettings.getInstance().getGlobalConfig();
        connection = new Connection(acctCfg);
    }

    public AccountConfig getAcctConfig() {
        return acctCfg;
    }
    
    /**
     * Determine whether the underlying protocol supports multiple
     * mail folders.
     * @return True if folders are supported
     */
    public abstract boolean hasFolders();

    /**
     * Select a new active mailbox.
     * This method is only useful if the protocol supports
     * folders, and should otherwise be ignored or limited
     * to a single inbox folder
     * @param mailbox The FolderItem object describing the new active mailbox
     */
    public abstract void setActiveMailbox(FolderItem mailbox) throws IOException, MailException;
    
    /**
     * Get the active mailbox.
     * This method is only useful if the protocol supports
     * folders, and should otherwise return null or
     * a single inbox folder
     * @return The FolderItem object describing the active mailbox
     */
    public abstract FolderItem getActiveMailbox();

    /**
     * Open a new connection.
     * This method should be overloaded to add protocol-specific
     * login commands after the socket connection is established.
     * @param acctCfg Configuration for the account to open
     */
    public void open() throws IOException, MailException {
        connection.open();
    }
    
    /**
     * Close an existing connection.
     * This method should be overloaded to add protocol-specific
     * logout commands prior to terminating the connection.
     */
    public void close() throws IOException, MailException {
        connection.close();
    }

    public boolean isConnected() {
        return connection.isConnected();
    }

    /**
     * Return a list of MessageEnvelope objects for the specified
     * range of messages in the active mailbox
     * @param firstIndex Index of the first message
     * @param lastIndex Index of the last message
     * @return Vector of MessageEnvelope objects
     */
    public abstract Vector getMessageEnvelopes(int firstIndex, int lastIndex) throws IOException, MailException;
    
    /**
     * Retrieves the structure of a message from the mailbox.
     * This allows for more intelligent retrieval of the
     * message body, but might only work correctly with IMAP.
     * @param env Message index in the active mailbox
     */
    public abstract Message.Structure getMessageStructure(Message.Envelope env) throws IOException, MailException;

    /**
     * Retrieve the body of a message.
     * This allows for more intelligent retrieval of the
     * message body, but might only work correctly with IMAP.
     * @param env Message index in the active mailbox
     * @param bindex Index of the body within the message
     */
    public abstract String getMessageBody(Message.Envelope env, int bindex) throws IOException, MailException;
}
