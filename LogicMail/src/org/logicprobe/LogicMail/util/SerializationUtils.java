/*-
 * Copyright (c) 2008, Derek Konigsberg
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Utility class providing common methods used in serialization.
 */
public final class SerializationUtils {
	private SerializationUtils() { }
	
    /**
     * Utility method to serialize any serializable class.
     * The returned buffer consists of the fully qualified class name,
     * followed by the serialized contents of the class.
     * 
     * @param input The object to serialize
     * @return The resulting byte array
     */
    public static byte[] serializeClass(Serializable input) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(buffer);
        try {
            output.writeUTF(input.getClass().getName());
            input.serialize(output);
        } catch (IOException ex) {
            // do nothing
        }
        return buffer.toByteArray();
    }

    /**
     * Utility method to serialize any serializable class and then
     * write it to a {@link DataOutputStream}.
     * This combines a call to {@link #serializeClass(Serializable)}
     * with the calls to actually write the length and contents of
     * the resulting byte array to the stream.
     * 
     * @param input The object to serialize
     * @param output The stream to write the serialized object onto
     * @throws IOException if an I/O error occurs
     */
    public static void serializeClass(Serializable input, DataOutput output) throws IOException {
		byte[] classBytes = SerializationUtils.serializeClass(input);
		output.writeInt(classBytes.length);
		output.write(classBytes);
    }
    
    /**
     * Utility method to deserialize any class.
     * First, the fully qualified class name is read from the
     * input stream.  Then, if a class matching that name exists,
     * it is instantiated.  Finally, if that class implements the
     * Serializable interface, the input stream is passed on
     * to its deserialize method.
     * 
     * @param data The byte array to deserialize
     * @return The resulting object
     */
    public static Serializable deserializeClass(byte[] data) {
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(data));
        Object deserializedObject;
        Serializable result = null;
        try {
            String classType = input.readUTF();
            deserializedObject = Class.forName(classType).newInstance();
            if(deserializedObject instanceof Serializable) {
                result = (Serializable)deserializedObject;
                result.deserialize(input);
            }
        } catch (IOException ex) {
            result = null;
        } catch (ClassNotFoundException ex) {
            result = null;
        } catch (InstantiationException ex) {
            result = null;
        } catch (IllegalAccessException ex) {
            result = null;
        }
        return result;
    }
    
    /**
     * Utility method to deserialize any class from the provided
     * {@link DataInputStream}.
     * This combines a call to {@link #deserializeClass(byte[])}
     * with the calls to actually read the length and contents of
     * the serialized byte array from the stream.
     * 
     * @param input The stream to read the serialized object from
     * @return The deserialized object
     * @throws IOException if an I/O error occurs
     */
    public static Serializable deserializeClass(DataInput input) throws IOException {
		int classLength = input.readInt();
		byte[] classBytes = new byte[classLength];
		input.readFully(classBytes);
		Serializable result = SerializationUtils.deserializeClass(classBytes);
    	return result;
    }
}
