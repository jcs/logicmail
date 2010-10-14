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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import net.rim.device.api.util.Comparator;

/**
 * Fake message token for test cases.
 * This is exposed as a public class to support test cases involving
 * object serialization.
 */
public class FakeMessageToken implements MessageToken {
    private long uniqueId;
    public FakeMessageToken() { this.uniqueId = 0; }
    public FakeMessageToken(long uniqueId) { this.uniqueId = uniqueId; }
    public long getUniqueId() { return uniqueId; }
    public void deserialize(DataInput input) throws IOException { uniqueId = input.readLong(); }
    public void serialize(DataOutput output) throws IOException { output.writeLong(uniqueId); }
    public boolean containedWithin(FolderTreeItem folderTreeItem) { return true; }
    public String getMessageUid() { return Long.toString(uniqueId); }
    public void updateToken(MessageToken messageToken) { }
    public boolean isLoadable() { return true; }
    public void updateMessageIndex(int index) { }
    
    private static final Comparator comparator = new Comparator() {
        public int compare(Object o1, Object o2) {
            if(o1 instanceof FakeMessageToken && o2 instanceof FakeMessageToken) {
                FakeMessageToken token1 = (FakeMessageToken)o1;
                FakeMessageToken token2 = (FakeMessageToken)o2;
                
                if(token1.uniqueId < token2.uniqueId) {
                    return -1;
                }
                else if(token1.uniqueId > token2.uniqueId) {
                    return 1;
                }
                else {
                    return 0;
                }
            }
            else {
                throw new ClassCastException("Cannot compare types");
            }
        }
    };
    
    public Comparator getComparator() {
        return comparator;
    }
    
    public int hashCode() {
        return 31 * 1 + (int) (uniqueId ^ (uniqueId >>> 32));
    }
    
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        FakeMessageToken other = (FakeMessageToken) obj;
        if (uniqueId != other.uniqueId) {
            return false;
        }
        return true;
    }
}
