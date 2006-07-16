/*
 * MailClient.java
 *
 * Portions of this code may have been inspired by and/or
 * copied from the following classes of the Mail4ME project:
 * de.trantor.mail.InboxClient
 * These portions are:
 *   Copyright (c) 2000-2002 Jorg Pleumann <joerg@pleumann.de>
 * 
 */

package org.logicprobe.LogicMail.mail;

import java.io.IOException;
import java.util.Date;
import java.util.Vector;
import org.logicprobe.LogicMail.conf.AccountConfig;

/**
 * Create a generic interface to different mail protocols.
 * This class allows most of the UI code to be protocol-agnostic.
 */
public abstract class MailClient {
    /**
     * Relevant information describing a folder.
     */
    public static class FolderItem {
        public String name;
        public String path;
        public String delim;
        public int msgCount;
    }

    protected AccountConfig acctCfg;
    protected Connection connection;
    
    protected MailClient(AccountConfig acctCfg) {
        this.acctCfg = acctCfg;
        connection = new Connection(acctCfg);
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
     * Queries the number of messages currently available in the mailbox.
     *
     * @see #getMessage
     * @see #getHeaders
     * @see #removeMessage
     *
     */
    public abstract int getMessageCount() throws IOException, MailException;
    
    /**
     * Return a list of MessageEnvelope objects for the specified
     * range of messages in the active mailbox
     * @param firstIndex Index of the first message
     * @param lastIndex Index of the last message
     * @return Vector of MessageEnvelope objects
     */
    public abstract Vector getMessageEnvelopes(int firstIndex, int lastIndex) throws IOException, MailException;
    
    /**
     * Retrieves a message from the mailbox. The method retrieves the
     * message with the given index from the server. Message numbering
     * follows the usual Java conventions for vectors. Thus the index must
     * be a number ranging from 0 to getMessageCount() - 1.
     *
     * @see #getMessageCount
     * @see #getHeaders
     */
    public abstract Message getMessage(int index) throws IOException, MailException;
    
    /**
     * Retrieves the structure of a message from the mailbox.
     * This allows for more intelligent retrieval of the
     * message body, but might only work correctly with IMAP.
     * @param index Message index in the active mailbox
     */
    public abstract Message.Structure getMessageStructure(int index) throws IOException, MailException;

    /**
     * Retrieve the body of a message.
     * This allows for more intelligent retrieval of the
     * message body, but might only work correctly with IMAP.
     * @param index Message index in the active mailbox
     * @param bindex Index of the body within the message
     */
    public abstract String getMessageBody(int index, int bindex) throws IOException, MailException;
}
