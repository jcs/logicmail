/*-
 * Copyright (c) 2009, Derek Konigsberg
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

import net.rim.device.api.util.Comparator;

import org.logicprobe.LogicMail.util.Serializable;

/**
 * Common tagging interface for all message token objects.
 * <p>
 * A message token is intended to be a serializable object
 * that provides a memento for tracking a mail message across
 * the application.  It is designed to prevent the need for
 * protocol-specific message properties to be exposed beyond
 * the protocol layer, while still allowing other layers to
 * perform message operations.
 * </p>
 */
public interface MessageToken extends Serializable {
	/**
	 * Gets the message UID, which is a protocol-specific way of uniquely
	 * identifying a message within its mailbox.  This is different from
	 * the {@link Serializable#getUniqueId()}, as it is a <tt>String</tt>
	 * identifier known to the mail protocol and does not have to be
	 * unique beyond the mailbox.
	 * 
	 * @return the message UID
	 */
	String getMessageUid(); 
	
	/**
	 * Returns whether this token represents a message contained within
	 * the specified mail folder.
	 * 
	 * @param folderTreeItem Folder item to check
	 * @return True if contained, false otherwise
	 */
	boolean containedWithin(FolderTreeItem folderTreeItem);

	/**
	 * Update this message token with information from the provided
	 * message token.  This is meant to update volatile token fields
	 * that are not serialized or checked with {@link #equals(Object)}.
	 * 
	 * @param messageToken other token to update from
	 */
    void updateToken(MessageToken messageToken);
    
    /**
     * Update this message token with the provided index value, if applicable.
     * Message index values are never persisted, but they are still needed
     * at run time.  This method allows them to be updated from outside the
     * mail store layer, as a necessary part of the folder refresh process.
     * 
     * @param index updated message index
     */
    void updateMessageIndex(int index);
    
    /**
     * Get the message index contained within this token object.  Message index
     * values are never persisted, and not required, so this method should
     * return </code>-1</code> if an index value is not available.
     * 
     * @return message index value, of <code>-1</code> if not available
     */
    int getMessageIndex();
    
    /**
     * Gets whether this token is sufficiently complete to load a message
     * from a mail store.  Messages not loadable from a mail store should
     * still be loadable from local cache.
     * 
     * @return True if loadable, false otherwise
     */
    boolean isLoadable();
    
    /**
     * Gets a comparator that can be used to sort, in ascending order, a
     * collection of message tokens of the same type.
     * 
     * @return comparator for two message tokens
     */
    Comparator getComparator();
    
    /**
     * Creates and returns a copy of this token.  While the implementation may
     * vary depending on the specific token type, the intent is that the clone
     * be fully substitutable for the original when making mail store calls.
     *
     * @return a clone of this message token
     * @throws UnsupportedOperationException if cloning is not supported
     */
    MessageToken clone();
}
