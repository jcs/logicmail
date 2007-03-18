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

package org.logicprobe.LogicMail.conf;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import org.logicprobe.LogicMail.util.Serializable;

/**
 * Store the global configuration for LogicMail.
 */
public class GlobalConfig implements Serializable {
    /** full name of the user */
    private String fullname;
    /** number of message headers to retrieve */
    private int retMsgCount;
    /** true for ascending, false for decending */
    private boolean dispOrder;
    /** IMAP: maximum message size */
    private int imapMaxMsgSize;
    /** IMAP: maximum folder depth */
    private int imapMaxFolderDepth;
    /** POP: maximum message lines */
    private int popMaxLines;
    /** Message signature */
    private String msgSignature;

    public GlobalConfig() {
        this.fullname = "";
        this.retMsgCount = 30;
        this.dispOrder = false;
        this.imapMaxMsgSize = 32768;
        this.imapMaxFolderDepth = 4;
        this.popMaxLines = 400;
        this.msgSignature = "";
    }
    
    public GlobalConfig(DataInputStream input) {
        try {
            deserialize(input);
        } catch (IOException ex) {
            this.fullname = "";
            this.retMsgCount = 30;
            this.dispOrder = false;
            this.imapMaxMsgSize = 32768;
            this.imapMaxFolderDepth = 4;
            this.popMaxLines = 400;
            this.msgSignature = "";
        }
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }
    
    public String getFullname() {
        return fullname;
    }

    public void setRetMsgCount(int retMsgCount) {
        this.retMsgCount = retMsgCount;
    }

    public int getRetMsgCount() {
        return retMsgCount;
    }
    
    public void setDispOrder(boolean dispOrder) {
        this.dispOrder = dispOrder;
    }
    
    public boolean getDispOrder() {
        return dispOrder;
    }

    public int getImapMaxMsgSize() {
        return imapMaxMsgSize;
    }

    public void setImapMaxMsgSize(int imapMaxMsgSize) {
        this.imapMaxMsgSize = imapMaxMsgSize;
    }
    
    public int getImapMaxFolderDepth() {
        return imapMaxFolderDepth;
    }

    public void setImapMaxFolderDepth(int imapMaxFolderDepth) {
        this.imapMaxFolderDepth = imapMaxFolderDepth;
    }

    public int getPopMaxLines() {
        return popMaxLines;
    }

    public void setPopMaxLines(int popMaxLines) {
        this.popMaxLines = popMaxLines;
    }
    
    public String getMsgSignature() {
        return msgSignature;
    }

    public void setMsgSignature(String msgSignature) {
        this.msgSignature = msgSignature;
    }

    public void serialize(DataOutputStream output) throws IOException {
        output.writeUTF(fullname);
        output.writeInt(retMsgCount);
        output.writeBoolean(dispOrder);
        output.writeInt(imapMaxMsgSize);
        output.writeInt(imapMaxFolderDepth);
        output.writeInt(popMaxLines);
        output.writeUTF(msgSignature);
    }

    public void deserialize(DataInputStream input) throws IOException {
        fullname = input.readUTF();
        retMsgCount = input.readInt();
        dispOrder = input.readBoolean();
        imapMaxMsgSize = input.readInt();
        imapMaxFolderDepth = input.readInt();
        popMaxLines = input.readInt();
        msgSignature = input.readUTF();
    }
}

