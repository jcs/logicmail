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
import org.logicprobe.LogicMail.conf.AccountConfig;
import org.logicprobe.LogicMail.message.Message;

/**
 * Create a generic interface to different mail protocols.
 * This class allows most of the UI code to be protocol-agnostic.
 */
public abstract class MailClient {
    /**
     * Get a new concrete mail client instance.
     * This could be placed in an external factory, but it seemed simpler
     * to put it in here for now.
     *
     * @param acctConfig User account configuration
     * @return Usable mail client instance
     */
    public static MailClient getNewClient(AccountConfig acctConfig) {
        MailClient client;
        if(acctConfig.getServerType() == AccountConfig.TYPE_POP) {
            return new PopClient(acctConfig);
        }
        else if(acctConfig.getServerType() == AccountConfig.TYPE_IMAP) {
            return new ImapClient(acctConfig);
        }
        else {
            return null;
        }
    }
    
    /**
     * Get the account configuration.
     * Should probably find a way to remove the need for this.
     */
    public abstract AccountConfig getAcctConfig();
    
    /**
     * Open a new connection.
     * This method should typically establish a socket connection and
     * then send the protocol-specific login commands
     *
     * @throw IOException on I/O errors
     * @throw MailException on protocol errors
     */
    public abstract void open() throws IOException, MailException;
    
    /**
     * Close an existing connection.
     * This method should sent protocol-specific logout commands and
     * then terminate the connection.
     *
     * @throw IOException on I/O errors
     * @throw MailException on protocol errors
     */
    public abstract void close() throws IOException, MailException;

    /**
     * Find out if the connection is active.
     * @return True if the connection is active, false otherwise
     */
    public abstract boolean isConnected();

    /**
     * Return whether the underlying protocol supports mail folders.
     * This enables actions to be taken without having to unnecessarily
     * execute a potentially costly getFolderTree() operation.
     *
     * @return True if folders supported, false otherwise
     */
    public abstract boolean hasFolders();
    
    /**
     * Get the mail folder tree.
     * This should return null if folders are not supported
     * by the underlying protocol.
     * If the tree has no singular root node, then this should
     * reference an invisible root node.  Otherwise, this should
     * reference the visible root folder.
     *
     * @return Root folder of the tree, and all children below it
     * @throw IOException on I/O errors
     * @throw MailException on protocol errors
     */
    public abstract FolderTreeItem getFolderTree()
        throws IOException, MailException;
    
    /**
     * Get the active mail folder.
     * This method is only useful if the protocol supports
     * folders, and should otherwise return null or
     * a single inbox folder
     *
     * @return The FolderItem object describing the active mailbox
     */
    public abstract FolderTreeItem getActiveFolder();
    
    /**
     * Select a new active mail folder.
     * This method is only useful if the protocol supports
     * folders, and should otherwise be ignored or limited
     * to a single inbox folder
     * 
     * @param folderItem The FolderItem object describing the new active folderItem
     * @throw IOException on I/O errors
     * @throw MailException on protocol errors
     */
    public abstract void setActiveFolder(FolderTreeItem folderItem)
        throws IOException, MailException;
    
    /**
     * Get a list of the messages in the selected folder.
     *
     * @param firstIndex Index of the first message
     * @param lastIndex Index of the last message
     * @return List of message envelopes
     * @throw IOException on I/O errors
     * @throw MailException on protocol errors
     */
    public abstract Message.Envelope[] getMessageList(int firstIndex, int lastIndex)
        throws IOException, MailException;
    
    /**
     * Get a particular message from the selected folder.
     * The details of message retrieval should be constrained by
     * protocol-specific capabilities, application-wide data
     * format capabilities, and user configuration options.
     *
     * @throw IOException on I/O errors
     * @throw MailException on protocol errors
     */
    public abstract Message getMessage(int index) throws IOException, MailException;

    // ----------------------------------------------------------------
    // All remaining methods are deprecated and should be removed once
    // the protocol code is adapted.  Ultimately, getMessage() should
    // provide functionality equivalent to the typical use case of both
    // these methods together.
    
    /**
     * Retrieves the structure of a message from the mailbox.
     * This allows for more intelligent retrieval of the
     * message body, but might only work correctly with IMAP.
     *
     * @param env Message index in the active mailbox
     */
    public abstract Message.Structure getMessageStructure(Message.Envelope env) throws IOException, MailException;

    /**
     * Retrieve the body of a message.
     * This allows for more intelligent retrieval of the
     * message body, but might only work correctly with IMAP.
     *
     * @param env Message index in the active mailbox
     * @param bindex Index of the body within the message
     */
    public abstract String getMessageBody(Message.Envelope env, int bindex) throws IOException, MailException;
}
