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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import net.rim.device.api.util.Comparator;

import org.logicprobe.LogicMail.util.StringParser;

/**
 * Special message token for outgoing messages.
 * 
 * This is necessary because outgoing messages do not
 * normally exist within any real mail store unless
 * they fail to transmit.
 */
public class OutgoingMessageToken implements MessageToken {
    private int hashCode = -1;
    private long messageUid;
    private String folderPath;

    public OutgoingMessageToken() {
        this.messageUid = System.currentTimeMillis();
        this.folderPath = "";
    }
    
    public OutgoingMessageToken(FolderTreeItem folderTreeItem, long messageId) {
        this.folderPath = folderTreeItem.getPath();
        this.messageUid = messageId;
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.mail.MessageToken#getComparator()
     */
    public Comparator getComparator() {
        throw new UnsupportedOperationException();
    }

    public boolean containedWithin(FolderTreeItem folderTreeItem) {
        return folderTreeItem.getPath().equals(folderPath);
    }

    public String getMessageUid() {
        return StringParser.toHexString(messageUid).toLowerCase();
    }

    public long getUniqueId() {
        return messageUid;
    }

    public void updateToken(MessageToken messageToken) {
        // Empty because this special token is not intended to be synchronized
    }

    public void updateMessageIndex(int index) {
        // Empty because it is not applicable to this token type
    }
    
    public boolean isLoadable() {
        return true;
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.util.Serializable#serialize(java.io.DataOutput)
     */
    public void serialize(DataOutput output) throws IOException {
        output.writeLong(messageUid);
        output.writeUTF(folderPath);
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.util.Serializable#deserialize(java.io.DataInput)
     */
    public void deserialize(DataInput input) throws IOException {
        messageUid = input.readLong();
        folderPath = input.readUTF();
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if(obj instanceof OutgoingMessageToken) {
            OutgoingMessageToken rhs = (OutgoingMessageToken)obj;
            return (this.folderPath.equals(rhs.folderPath) &&
                    this.messageUid == rhs.messageUid);
        }
        else {
            return false;
        }
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        if(hashCode == -1) {
            int hash = 1;
            hash = 31 * hash
                    + ((folderPath == null) ? 0 : folderPath.hashCode());
            hash = 31 * hash + (int) (messageUid ^ (messageUid >>> 32));
            hashCode = hash;
        }
        return hashCode;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "OutgoingMessageToken [folderPath=" + folderPath
                + ", messageUid=" + messageUid + "]";
    }
    
    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.mail.MessageToken#clone()
     */
    public MessageToken clone() {
        OutgoingMessageToken result = new OutgoingMessageToken();
        result.messageUid = messageUid;
        result.folderPath = folderPath;
        return result;
    }
}
