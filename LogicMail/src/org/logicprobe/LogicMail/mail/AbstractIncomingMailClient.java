/*-
 * Copyright (c) 2010, Derek Konigsberg
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

import org.logicprobe.LogicMail.message.MessageFlags;
import org.logicprobe.LogicMail.message.MimeMessageContent;
import org.logicprobe.LogicMail.message.MimeMessagePart;

/**
 * Common base implementation of an {@code IncomingMailClient} that provides
 * default implementations of several optional methods.
 */
public abstract class AbstractIncomingMailClient implements IncomingMailClient {

    public boolean hasFolders() {
        return false;
    }
    
    public FolderTreeItem getFolderTree(MailProgressHandler progressHandler)
    throws IOException, MailException {
        return null;
    }

    public boolean hasMessageParts() {
        return false;
    }

    public MimeMessageContent getMessagePart(
            MessageToken messageToken,
            MimeMessagePart mimeMessagePart,
            MailProgressHandler progressHandler)
    throws IOException, MailException {
        // Default empty implementation for unsupported feature
        return null;
    }
    
    public boolean hasAppend() {
        return false;
    }
    
    public void appendMessage(
            FolderTreeItem folder,
            String rawMessage,
            MessageFlags initialFlags)
    throws IOException, MailException {
        // Default empty implementation for unsupported feature
    }

    public boolean hasCopy() {
        return false;
    }

    public void copyMessage(
            MessageToken messageToken,
            FolderTreeItem destinationFolder)
    throws IOException, MailException {
        // Default empty implementation for unsupported feature
    }
    
    public boolean hasUndelete() {
        return false;
    }
    
    public void undeleteMessage(
            MessageToken messageToken,
            MessageFlags messageFlags)
    throws IOException, MailException {
        // Default empty implementation for unsupported feature
    }
    
    public boolean hasFlags() {
        return false;
    }

    public void messageAnswered(
            MessageToken messageToken,
            MessageFlags messageFlags)
    throws IOException, MailException {
        // Default empty implementation for unsupported feature
    }
    
    public void messageForwarded(
            MessageToken messageToken,
            MessageFlags messageFlags)
    throws IOException, MailException {
        // Default empty implementation for unsupported feature
    }
    
    public boolean hasExpunge() {
        return false;
    }

    public int[] expungeActiveFolder() throws IOException, MailException {
        // Default empty implementation for unsupported feature
        return new int[0];
    }
    
    public boolean hasIdle() {
        return false;
    }

    public void idleModeBegin() throws IOException, MailException {
        // Default empty implementation for unsupported feature
    }

    public void idleModeEnd() throws IOException, MailException {
        // Default empty implementation for unsupported feature
    }

    public boolean idleModePoll() throws IOException, MailException {
        // Default empty implementation for unsupported feature
        return false;
    }
}
