/*-
 * Copyright (c) 2007, Derek Konigsberg
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

package org.logicprobe.LogicMail.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Provides a wrapper for java.util.Hashtable that implements the
 * LogicMail.util.Serializable interface.
 * The maximum number of items that can be stored is 1000.
 * This limit exists so that the deserialization code can quickly
 * handle data corruption that could result in a bad size value.
 */
public class SerializableHashtable extends Hashtable implements Serializable {
    final private static int TYPE_NULL    = 0;
    final private static int TYPE_BOOLEAN = 1;
    final private static int TYPE_BYTE    = 2;
    final private static int TYPE_CHAR    = 3;
    final private static int TYPE_STRING  = 4;
    final private static int TYPE_DOUBLE  = 5;
    final private static int TYPE_FLOAT   = 6;
    final private static int TYPE_INT     = 7;
    final private static int TYPE_LONG    = 8;
    final private static int TYPE_SHORT   = 9;

    final private static int MAX_ITEMS = 1000;
    
    private long uniqueId;
    
    /**
     * Creates a new instance of SerializableHashtable.
     * This class only supports hash tables containing objects which
     * wrap the various primitive types supported by DataOutputStream
     * and DataInputStream.
     *
     * @param table The instance of Hashtable to wrap
     */
    public SerializableHashtable() {
        super();
        uniqueId = UniqueIdGenerator.getInstance().getUniqueId();
    }

    /**
     * Creates a new instance of SerializableHashtable.
     * This class only supports hash tables containing objects which
     * wrap the various primitive types supported by DataOutputStream
     * and DataInputStream.
     *
     * @param initialCapacity Initial capacity of the hashtable.
     */
    public SerializableHashtable(int initialCapacity) {
        super(initialCapacity);
        uniqueId = UniqueIdGenerator.getInstance().getUniqueId();
    }
    
    private void writeObject(DataOutputStream output, Object item) throws IOException {
        if(item instanceof Boolean) {
            output.write(TYPE_BOOLEAN);
            output.writeBoolean(((Boolean)item).booleanValue());
        }
        else if(item instanceof Byte) {
            output.write(TYPE_BYTE);
            output.writeByte(((Byte)item).byteValue());
        }
        else if(item instanceof Character) {
            output.write(TYPE_CHAR);
            output.writeChar(((Character)item).charValue());
        }
        else if(item instanceof String) {
            output.write(TYPE_STRING);
            output.writeUTF(((String)item));
        }
        else if(item instanceof Double) {
            output.write(TYPE_DOUBLE);
            output.writeDouble(((Double)item).doubleValue());
        }
        else if(item instanceof Float) {
            output.write(TYPE_FLOAT);
            output.writeFloat(((Float)item).floatValue());
        }
        else if(item instanceof Integer) {
            output.write(TYPE_INT);
            output.writeInt(((Integer)item).intValue());
        }
        else if(item instanceof Long) {
            output.write(TYPE_LONG);
            output.writeLong(((Long)item).longValue());
        }
        else if(item instanceof Short) {
            output.write(TYPE_SHORT);
            output.writeShort(((Short)item).shortValue());
        }
        else {
            output.write(TYPE_NULL);
            output.write(0);
        }
    }

    private Object readObject(DataInputStream input) throws IOException {
        int type = input.read();
        switch(type) {
            case TYPE_BOOLEAN:
                return new Boolean(input.readBoolean());
            case TYPE_BYTE:
                return new Byte(input.readByte());
            case TYPE_CHAR:
                return new Character(input.readChar());
            case TYPE_STRING:
                return input.readUTF();
            case TYPE_DOUBLE:
                return new Double(input.readDouble());
            case TYPE_FLOAT:
                return new Float(input.readFloat());
            case TYPE_INT:
                return new Integer(input.readInt());
            case TYPE_LONG:
                return new Long(input.readLong());
            case TYPE_SHORT:
                return new Short(input.readShort());
            case TYPE_NULL:
                return null;
            default:
                return null;
        }
    }
    
    public void serialize(DataOutputStream output) throws IOException {
        output.writeLong(uniqueId);
        output.writeInt(this.size());
        Enumeration e = this.keys();
        Object key;
        while(e.hasMoreElements()) {
            key = e.nextElement();
            writeObject(output, key);
            writeObject(output, this.get(key));
        }
    }

    public void deserialize(DataInputStream input) throws IOException {
        this.clear();
        uniqueId = input.readLong();
        int size = input.readInt();
        if(size > MAX_ITEMS) {
            throw new IOException();
        }
        Object key;
        Object value;
        for(int i=0; i<size; i++) {
            key = readObject(input);
            value = readObject(input);
            if(key != null && value != null) {
                this.put(key, value);
            }
        }
    }    

    public long getUniqueId() {
        return uniqueId;
    }
}
