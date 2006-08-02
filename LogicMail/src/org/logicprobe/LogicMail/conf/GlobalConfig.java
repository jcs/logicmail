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

/**
 * Store the global configuration for LogicMail.
 * Right now there is no user interface to set
 * any of these fields, and many of them are always
 * set to default values.  However, this prepares the
 * rest of the code to consult these parameters so
 * they ultimately can be made configurable.
 */
public class GlobalConfig {
    /** full name of the user */
    private String _fullname;
    /** number of message headers to retrieve */
    private int _retMsgCount;
    /** true for ascending, false for decending */
    private boolean _dispOrder;
    /** maximum size of an e-mail section to download */
    private int _maxSectionSize = 16384;


    public GlobalConfig() {
        _fullname = "";
        _retMsgCount = 30;
        _dispOrder = false;
        _maxSectionSize = 16384;
    }
    
    public GlobalConfig(byte[] byteArray) {
        deserialize(byteArray);
    }

    public void setFullname(String fullname) {
        _fullname = fullname;
    }
    
    public String getFullname() {
        return _fullname;
    }

    public void setRetMsgCount(int retMsgCount) {
        _retMsgCount = retMsgCount;
    }

    public int getRetMsgCount() {
        return _retMsgCount;
    }
    
    public void setDispOrder(boolean dispOrder) {
        _dispOrder = dispOrder;
    }
    
    public boolean getDispOrder() {
        return _dispOrder;
    }

    public void setMaxSectionSize(int maxSectionSize) {
        _maxSectionSize = maxSectionSize;
    }
    
    public int getMaxSectionSize() {
        return _maxSectionSize;
    }
    
    public byte[] serialize() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(buffer);
        
        try {
            output.writeUTF(_fullname);
            output.writeInt(_retMsgCount);
            output.writeBoolean(_dispOrder);
            output.writeInt(_maxSectionSize);
            return buffer.toByteArray();
        } catch (IOException exp) {
            return null;
        }
    }

    public void deserialize(byte[] byteArray) {
        ByteArrayInputStream buffer = new ByteArrayInputStream(byteArray);
        DataInputStream input = new DataInputStream(buffer);
        
        try {
            _fullname = input.readUTF();
            _retMsgCount = input.readInt();
            _dispOrder = input.readBoolean();
            _maxSectionSize = input.readInt();
        } catch (IOException exp) {
            _fullname = "";
            _retMsgCount = 30;
            _dispOrder = false;
            _maxSectionSize = 16384;
        }
    }
}

