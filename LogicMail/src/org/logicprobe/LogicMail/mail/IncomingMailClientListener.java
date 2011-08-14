/*-
 * Copyright (c) 2011, Derek Konigsberg
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

import org.logicprobe.LogicMail.message.MessageFlags;

/**
 * Provides the ability to listen for asynchronous events from the mail client.
 * While most of the commands are synchronous in design, some can also receive
 * unsolicited responses.  This interface provides a way to handle those
 * responses.
 */
public interface IncomingMailClientListener {
    /**
     * Called to indicate the presence of recently arrived messages in the
     * active mailbox.
     * The expected behavior is to follow this with a call to
     * <code>getNewFolderMessages()</code> at the earliest convenience of the
     * connection handler.
     * 
     * @param folder The folder that was selected at the time of the event
     */
    void recentFolderMessagesAvailable(FolderTreeItem folder);
    
    /**
     * Called to indicate that a message's flags have changed.
     * 
     * @param token the token of the message, or null if unavailable
     * @param messageFlags the updated message flags
     */
    void folderMessageFlagsChanged(MessageToken token, MessageFlags messageFlags);
    
    /**
     * Called to indicate that a message has been expunged from the active
     * mailbox.
     *  
     * @param expungedToken the token of the expunged message, or null if unavailable
     * @param updatedTokens the tokens with updated data as a result of the expunge operation
     */
    void folderMessageExpunged(MessageToken expungedToken, MessageToken[] updatedTokens);
    
    /**
     * Called to indicate that idle processing has prematurely terminated
     * due to an error condition.  This should be handled by calling
     * <code>idleModeEnd()</code> which will perform any necessary cleanup and
     * throw any pending exceptions.
     */
    void idleModeError();
}
