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
import org.logicprobe.LogicMail.conf.AccountConfig;
import org.logicprobe.LogicMail.message.FolderMessage;
import org.logicprobe.LogicMail.message.Message;

/**
 * Create a generic interface to different incoming mail protocols.
 * This class allows most of the UI code to be protocol-agnostic.
 */
public interface IncomingMailClient extends MailClient {
    /**
     * Get the account configuration.
     * Should probably find a way to remove the need for this.
     */
    public abstract AccountConfig getAcctConfig();
    
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
    public abstract FolderMessage[] getFolderMessages(int firstIndex, int lastIndex)
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
    public abstract Message getMessage(FolderMessage folderMessage) throws IOException, MailException;
}
