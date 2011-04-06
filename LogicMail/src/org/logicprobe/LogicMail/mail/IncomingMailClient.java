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

import net.rim.device.api.util.ToIntHashtable;

import org.logicprobe.LogicMail.conf.AccountConfig;
import org.logicprobe.LogicMail.message.Message;
import org.logicprobe.LogicMail.message.MessageFlags;
import org.logicprobe.LogicMail.message.MimeMessageContent;
import org.logicprobe.LogicMail.message.MimeMessagePart;

/**
 * Provides a generic interface to different incoming mail protocols.
 * This class allows most of the UI code to be protocol-agnostic.
 * <p>
 * Since a number of features may not be supported by all protocols,
 * a variety of <tt>hasXXXX()</tt> methods are provided by this interface to
 * determine whether those features exist.
 * </p>
 * <p>
 * Methods that involve a noticeable amount of network traffic and/or
 * parsing overhead take an instance of {@link MailProgressHandler} as
 * a parameter.  This allows more granular progress monitoring than would
 * otherwise be available.
 * </p>
 */
public interface IncomingMailClient extends MailClient {
    /**
     * Set the listener used to handle asynchronous events from the mail client.
     * 
     * @param listener the listener to set
     */
    void setListener(IncomingMailClientListener listener);
    
    /**
     * Get the account configuration.
     * Should probably find a way to remove the need for this.
     */
    AccountConfig getAcctConfig();
    
    /**
     * Return whether the underlying protocol supports mail folders.
     * <p>
     * This enables actions to be taken without having to unnecessarily
     * execute a potentially costly getFolderTree() operation.
     * </p>
     *
     * @return True if folders supported, false otherwise
     * @see #getFolderTree(MailProgressHandler)
     */
    boolean hasFolders();
    
    /**
     * Return whether the underlying protocol supports retrieval of individual
     * message parts.
     * 
     * @return True if retrieval of individual message parts is supported
     * @see #getMessagePart(MessageToken, MimeMessagePart, MailProgressHandler)
     */
    boolean hasMessageParts();
    
    /**
     * Returns whether the underlying protocol supports appending messages to
     * mailboxes.
     * 
     * @return True of append is supported
     * @see #appendMessage(FolderTreeItem, String, MessageFlags)
     */
    boolean hasAppend();
    
    /**
     * Returns whether the underlying protocol supports copying messages
     * between its mailboxes.
     * 
     * @return True if copy is supported
     * @see #copyMessage(MessageToken, FolderTreeItem)
     */
    boolean hasCopy();
    
    /**
     * Return whether the underlying protocol supports undeletion of messages.
     *
     * @return True if undelete supported, false otherwise
     * @see #undeleteMessage(MessageToken, MessageFlags)
     */
    boolean hasUndelete();
    
    /**
     * Return whether the underlying protocol supports expunging of deleted
     * messages.
     *
     * @return True if expunge supported, false otherwise
     * @see #expungeActiveFolder()
     */
    boolean hasExpunge();
    
    /**
     * Return whether the underlying protocol supports message flags.
     * 
     * @return True if message flags are supported, false otherwise
     * 
     * @see #messageAnswered(MessageToken, MessageFlags)
     * @see #messageForwarded(MessageToken, MessageFlags)
     */
    boolean hasFlags();
    
    /**
     * Return whether the underlying protocol supports an operation to retrieve
     * the complete message UID to index map.  This feature should only be
     * provided for protocols that do not support UID-based message references
     * for other commands.
     * 
     * @return True if UID-to-index map retrieval is supported, false otherwise
     * @see #getFolderMessageIndexMap(MailProgressHandler)
     */
    boolean hasFolderMessageIndexMap();
    
    /**
     * Return whether the underlying protocol supports an idle connection mode.
     * 
     * @return True if an idle mode is supported, false otherwise
     * @see #idleModeBegin()
     * @see #idleModeEnd()
     * @see #idleModePoll()
     */
    boolean hasIdle();
    
    /**
     * Return whether this protocol locks the client's view of its folders
     * while the client is connected.  This is intended to control idle
     * behavior when {@link #hasIdle()} returns false.
     * 
     * @return True if folder contents are locked while connected, false
     *         otherwise
     */
    boolean hasLockedFolders();
    
    /**
     * Get the mail folder tree.
     * <p>
     * This should return null if folders are not supported
     * by the underlying protocol.
     * If the tree has no singular root node, then this should
     * reference an invisible root node.  Otherwise, this should
     * reference the visible root folder.
     * </p>
     * 
     * @param progressHandler the progress handler
     * @return Root folder of the tree, and all children below it
     * 
     * @throws IOException on I/O errors
     * @throws MailException on protocol errors
     * @see #hasFolders()
     */
    FolderTreeItem getFolderTree(MailProgressHandler progressHandler)
        throws IOException, MailException;
    
    /**
     * Refresh the folder status.
     * 
     * @param folders The folders to refresh
     * @param progressHandler the progress handler
     * 
     * @throws IOException on I/O errors
     * @throws MailException on protocol errors
     */
    void refreshFolderStatus(FolderTreeItem[] folders, MailProgressHandler progressHandler)
        throws IOException, MailException;
    
    /**
     * Gets the INBOX folder.
     * <p>
     * This method returns the special folder that is considered the
     * primary mailbox for the connected user on the server.
     * Even protocols that do not support folders must provide a fake
     * INBOX folder through this method.
     * </p>
     * 
     * @return The FolderTreeItem object describing the INBOX mailbox
     */
    FolderTreeItem getInboxFolder();
    
    /**
     * Get the active mail folder.
     * <p>
     * This method is only useful if the protocol supports
     * folders, and should otherwise return null or
     * a single INBOX folder
     * </p>
     *
     * @return The FolderTreeItem object describing the active mailbox
     */
    FolderTreeItem getActiveFolder();
    
    /**
     * Select a new active mail folder.
     * If new messages have arrived in this folder since it was previously
     * selected, and the client's state information for the folder is still
     * valid, a {@link IncomingMailClientListener#recentFolderMessagesAvailable()}
     * event will be fired.  
     * <p>
     * This method is only useful if the protocol supports
     * folders, and should otherwise be ignored or limited
     * to a single INBOX folder
     * </p>
     * 
     * @param folderItem The FolderTreeItem object describing the new active
     *     folderItem
     * @param notifyAvailable Whether a notification should be generated if the
     *     client's state information for the folder is still valid and news
     *     messages have arrived
     * @return True, if the mail client's state information for the folder is
     *         still valid.  False if the folder needs to be refreshed.
     *
     * @throws IOException on I/O errors
     * @throws MailException on protocol errors
     */
    boolean setActiveFolder(FolderTreeItem folderItem, boolean notifyAvailable)
        throws IOException, MailException;

    /**
     * Select a new active mail folder based on where a particular message is
     * stored.
     * If new messages have arrived in this folder since it was previously
     * selected, and the client's state information for the folder is still
     * valid, a {@link IncomingMailClientListener#recentFolderMessagesAvailable()}
     * event will be fired.  
     * <p>
     * This method is only useful if the protocol supports
     * folders, and should otherwise be ignored or limited
     * to a single INBOX folder
     * </p>
     * 
     * @param messageToken The message token object for the message whose
     *     folder we want to switch to
     * @param notifyAvailable Whether a notification should be generated if the
     *     client's state information for the folder is still valid and news
     *     messages have arrived
     * @return <code>null</code>, if the mail client's state information for the
     *     message's folder is still valid.  If the folder needs to be
     *     refreshed, then the <code>FolderTreeItem</code> for the folder is
     *     returned.  
     *
     * @throws IOException on I/O errors
     * @throws MailException on protocol errors
     */
    FolderTreeItem setActiveFolder(MessageToken messageToken, boolean notifyAvailable)
        throws IOException, MailException;
    
    /**
     * Expunges deleted messages from the currently active mail folder.
     * This should do nothing if the underlying protocol does not support
     * expunge.
     * <p>
     * The list of expunged messages will be provided asynchronously via
     * {@link IncomingMailClientListener#folderMessageExpunged(MessageToken)}.
     * </p>
     * 
     * @throws IOException on I/O errors
     * @throws MailException on protocol errors
     */
    void expungeActiveFolder() throws IOException, MailException;
    
    /**
     * Get a list of the messages in the selected folder that appear after the
     * provided message token.
     * 
     * @param firstToken Token for the first message in the range, which exists
     * @param increment Maximum number of messages to load
     * @param callback The callback to provide notifications as individual
     *                 folder messages are loaded
     * @param progressHandler the progress handler
     * 
     * @throws IOException on I/O errors
     * @throws MailException on protocol errors
     */
    void getFolderMessages(MessageToken firstToken, int increment, FolderMessageCallback callback, MailProgressHandler progressHandler)
        throws IOException, MailException;
    
    /**
     * Get a list of the messages in the selected folder.
     * 
     * @param firstIndex Index of the first message
     * @param lastIndex Index of the last message
     * @param callback The callback to provide notifications as individual
     *                 folder messages are loaded
     * @param progressHandler the progress handler
     * 
     * @throws IOException on I/O errors
     * @throws MailException on protocol errors
     */
    void getFolderMessages(int firstIndex, int lastIndex, FolderMessageCallback callback, MailProgressHandler progressHandler)
        throws IOException, MailException;
    
    /**
     * Get a list of the messages in the selected folder
     * which match the provided tokens.
     * 
     * @param messageTokens Tokens of the messages
     * @param flagsOnly If true, only tokens and flags will be fetched
     * @param callback The callback to provide notifications as individual
     *                 folder messages are loaded
     * @param progressHandler the progress handler
     * 
     * @throws IOException on I/O errors
     * @throws MailException on protocol errors
     */
    void getFolderMessages(MessageToken[] messageTokens, boolean flagsOnly, FolderMessageCallback callback, MailProgressHandler progressHandler)
        throws IOException, MailException;

    /**
     * Get a list of the messages in the selected folder for the provided
     * index values.
     * 
     * @param messageIndices Index values for the messages to fetch
     * @param callback The callback to provide notifications as individual
     *                 folder messages are loaded
     * @param progressHandler the progress handler
     * 
     * @throws IOException on I/O errors
     * @throws MailException on protocol errors
     */
    void getFolderMessages(int[] messageIndices, FolderMessageCallback callback, MailProgressHandler progressHandler)
        throws IOException, MailException;
    
    /**
     * Get a list of new messages in the selected folder.
     * <p>
     * On the first invocation, it will return the most recent messages
     * up to the limit configured in the global configuration.
     * On subsequent invocations, if the underlying protocol supports it,
     * this method should return all new messages that have entered the
     * mailbox since.  Otherwise, it will behave the same as it
     * did on the first invocation.
     * </p>
     * 
     * @param flagsOnly If true, only tokens and flags will be fetched
     * @param callback The callback to provide notifications as individual
     *                 folder messages are loaded
     * @param progressHandler the progress handler
     * 
     * @throws IOException on I/O errors
     * @throws MailException on protocol errors
     */
    void getNewFolderMessages(boolean flagsOnly, FolderMessageCallback callback, MailProgressHandler progressHandler) throws IOException, MailException;
    
    /**
     * Get a map of message UIDs to indices for the selected folder.
     * 
     * @param progressHandler the progress handler
     * @return map of message UIDs, in <code>String</code> form, to index values
     * 
     * @throws IOException on I/O errors
     * @throws MailException on protocol errors
     */
    ToIntHashtable getFolderMessageIndexMap(MailProgressHandler progressHandler) throws IOException, MailException;
    
    /**
     * Get a particular message from the selected folder.
     * <p>
     * The details of message retrieval should be constrained by
     * protocol-specific capabilities, application-wide data
     * format capabilities, and user configuration options.
     * </p>
     * 
     * @param messageToken the message token
     * @param useLimits true, if user-configured download limits should be used
     * @param progressHandler the progress handler
     * @return the message
     * 
     * @throws IOException on I/O errors
     * @throws MailException on protocol errors
     */
    Message getMessage(MessageToken messageToken, boolean useLimits, MailProgressHandler progressHandler) throws IOException, MailException;

    /**
     * Get a particular message part from a message in the selected folder.
     * If the underlying protocol does not support retrieval of individual
     * message parts, this method should return null.
     * <p>
     * The details of message retrieval should be constrained by
     * protocol-specific capabilities, application-wide data
     * format capabilities, and user configuration options.
     * </p>
     *
     * @param messageToken the message token
     * @param mimeMessagePart the mime message part to retrieve
     * @param progressHandler the progress handler
     * @return the content for the message part
     * 
     * @throws IOException on I/O errors
     * @throws MailException on protocol errors
     * @see #hasMessageParts()
     */
    MimeMessageContent getMessagePart(MessageToken messageToken, MimeMessagePart mimeMessagePart, MailProgressHandler progressHandler) throws IOException, MailException;
    
    /**
     * Deletes a particular message from the selected folder.
     * <p>
     * If the protocol supports a two-stage (mark and expunge)
     * deletion process, then this command should only mark the
     * message as deleted.
     * </p>
     *
     * @throws IOException on I/O errors
     * @throws MailException on protocol errors
     */
    void deleteMessage(MessageToken messageToken) throws IOException, MailException;
    
    /**
     * Undeletes a particular message from the selected folder.
     * This should do nothing if the underlying protocol does not support
     * undeletion.
     *
     * @throws IOException on I/O errors
     * @throws MailException on protocol errors
     * @see #hasUndelete()
     */
    void undeleteMessage(MessageToken messageToken) throws IOException, MailException;

    /**
     * Appends a message to the specified folder, and flags it as seen.
     * This is intended for use when saving sent or draft messages.
     * This should do nothing if the underlying protocol does not support
     * appending.
     *
     * @throws IOException on I/O errors
     * @throws MailException on protocol errors
     * @see #hasAppend()
     */
    void appendMessage(FolderTreeItem folder, String rawMessage, MessageFlags initialFlags) throws IOException, MailException;
    
    /**
     * Copies a message from its current location to the specified folder.
     * This should do nothing if the underlying protocol does not support
     * copying.
     * <p>
     * This method should only be implemented when the underlying protocol
     * directly supports the copy operation in a way that does not require
     * downloading the message to the client.
     * </p>
     * 
     * @param messageToken Token identifying the message to be copied
     * @param destinationFolder Destination folder to copy the message into
     *
     * @throws IOException on I/O errors
     * @throws MailException on protocol errors
     * @see #hasCopy()
     */
    void copyMessage(MessageToken messageToken, FolderTreeItem destinationFolder) throws IOException, MailException;
    
    /**
     * Sets the flags on a message so the server knows it was answered.
     * This should do nothing if the underlying protocol does not support
     * setting an answered state on a message.
     * <p>
     * The actual flag updates will be provided asynchronously via
     * {@link IncomingMailClientListener#folderMessageFlagsChanged(MessageToken, MessageFlags)}.
     * </p>
     * 
     * @param messageToken Token identifying the message being modified
     * 
     * @throws IOException on I/O errors
     * @throws MailException on protocol errors
     * @see #hasFlags()
     */
    void messageAnswered(MessageToken messageToken) throws IOException, MailException;
    
    /**
     * Sets the flags on a message so the server knows it was forwarded.
     * This should do nothing if the underlying protocol does not support
     * setting a forwarded state on a message.
     * <p>
     * The actual flag updates will be provided asynchronously via
     * {@link IncomingMailClientListener#folderMessageFlagsChanged(MessageToken, MessageFlags)}.
     * </p>
     *
     * @param messageToken Token identifying the message being modified
     * 
     * @throws IOException on I/O errors
     * @throws MailException on protocol errors
     * @see #hasFlags()
     */
    void messageForwarded(MessageToken messageToken) throws IOException, MailException;
    
    /**
     * Sets the flags on a message so that the server marks it as seen.
     * Since this automatically happens during normal message retrieval, this
     * should only be necessary if a state change is needed without a
     * corresponding fetch request.
     * This should do nothing if the underlying protocol does not support
     * changing the seen state of a message.
     * <p>
     * The actual flag updates will be provided asynchronously via
     * {@link IncomingMailClientListener#folderMessageFlagsChanged(MessageToken, MessageFlags)}.
     * </p>
     *
     * @param messageToken Token identifying the message being modified
     * 
     * @throws IOException on I/O errors
     * @throws MailException on protocol errors
     * @see #hasFlags()
     */
    void messageSeen(MessageToken messageToken) throws IOException, MailException;
    
    /**
     * Sets the flags on a message so that the server marks it as unseen.
     * This should do nothing if the underlying protocol does not support
     * changing the seen state of a message.
     * <p>
     * The actual flag updates will be provided asynchronously via
     * {@link IncomingMailClientListener#folderMessageFlagsChanged(MessageToken, MessageFlags)}.
     * </p>
     *
     * @param messageToken Token identifying the message being modified
     * 
     * @throws IOException on I/O errors
     * @throws MailException on protocol errors
     * @see #hasFlags()
     */
    void messageUnseen(MessageToken messageToken) throws IOException, MailException;
    
    /**
     * Sends the underlying protocol's no-operation command.
     * <p>
     * On some protocols, this can be an explicit way of checking for the
     * availability of new messages, so it will be called periodically as part
     * of the idle processing routine.  However, any actual notifications will
     * be sent through the same asynchronous mechanism as idle processing.
     * </p>
     * 
     * @throws IOException on I/O errors
     * @throws MailException on protocol errors
     */
    void noop() throws IOException, MailException;
    
    /**
     * Begins the idle mode for the underlying protocol.
     * This should do nothing if the underlying protocol does not support
     * idling.
     * 
     * @throws IOException on I/O errors
     * @throws MailException on protocol errors
     * @see #hasIdle()
     */
    void idleModeBegin() throws IOException, MailException;
    
    /**
     * Ends the idle mode for the underlying protocol.
     * This should do nothing if the underlying protocol does not support
     * idling.
     * 
     * @throws IOException on I/O errors
     * @throws MailException on protocol errors
     * @see #hasIdle()
     */
    void idleModeEnd() throws IOException, MailException;
}
