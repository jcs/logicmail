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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;

/**
 * DataStore implementation for the J2ME Record Management System.
 * This implementation saves and loads everything at once, using
 * the corresponding save() and load() methods.  It is intended
 * for use with relatively lightweight data, so on-demand I/O has
 * not been implemented.
 */
public class RmsDataStore implements DataStore {
    private String storeName;
    private SerializableHashtable objectMap;
    private Vector storeObjects;
    
    /**
     * Creates a new instance of RmsDataStore.
     * @param name Name of the RMS store to use.
     */
    public RmsDataStore(String storeName) {
        this.storeName = storeName;
        this.objectMap = new SerializableHashtable();
        this.storeObjects = new Vector();
    }

    public Object getNamedObject(String name) {
        // Get the ID that matches the name
        Object value = objectMap.get(name);
        if(value instanceof Long) {
            // Now get the index that matches the ID
            value = objectMap.get(value);
            if(value instanceof Integer) {
                return storeObjects.elementAt(((Integer)value).intValue());
            }
            else {
                return null;
            }
        }
        else {
            return null;
        }
    }

    public Object getObject(long id) {
        Object value = objectMap.get(new Long(id));
        if(value instanceof Integer) {
            return storeObjects.elementAt(((Integer)value).intValue());
        }
        else {
            return null;
        }
    }

    public void putNamedObject(String name, Serializable object) {
        objectMap.put(name, new Long(object.getUniqueId()));
        putObject(object);
    }

    public void putObject(Serializable object) {
        Long keyObj = new Long(object.getUniqueId());
        if(objectMap.containsKey(keyObj)) {
            storeObjects.setElementAt(object, ((Integer)objectMap.get(keyObj)).intValue());
        }
        else {
            storeObjects.addElement(object);
            objectMap.put(keyObj, new Integer(storeObjects.size() - 1));
        }
    }

    public void removeNamedObject(String name) {
    }

    public void removeObject(Serializable object) {
    }

    public void save() {
        // Delete the store if it already exists,
        // as we are going to completely rewrite
        // its contents.
        try {
            RecordStore.deleteRecordStore(storeName);
        } catch (RecordStoreException exp) {
            // do nothing
        }
        
        try {
            RecordStore store = RecordStore.openRecordStore(storeName, true);
            byte[] byteArray;
            
            // Serialize the object map, and store
            // it at the first index.
            byteArray = serializeClass(objectMap);
            store.addRecord(byteArray, 0, byteArray.length);
            
            // Store all the objects in the vector
            Enumeration e = storeObjects.elements();
            while (e.hasMoreElements()) {
                byteArray = serializeClass((Serializable)e.nextElement());
                store.addRecord(byteArray, 0, byteArray.length);
            }
            store.closeRecordStore();
        } catch (RecordStoreException exp) {
            // do nothing
        }
    }

    public void load() {
        Object deserializedObject;
        try {
            RecordStore store = RecordStore.openRecordStore(storeName, false);
            
            int records = store.getNumRecords();
            
            if(records >= 1) {
                // Deserialize the object map
                deserializedObject = deserializeClass(store.getRecord(1));
                if(deserializedObject instanceof SerializableHashtable) {
                    objectMap = (SerializableHashtable)deserializedObject;
                }
                else {
                    return;
                }
            
                // Populate the vector
                if(records > 1) {
                    for(int i=2;i<=records;i++) {
                        deserializedObject = deserializeClass(store.getRecord(i));
                        storeObjects.addElement(deserializedObject);
                    }
                }
            }
            
            store.closeRecordStore();
        } catch (RecordStoreException exp) {
            // do nothing
        }
    }

    /**
     * Utility method to serialize any serializable class.
     * The returned buffer consists of the fully qualified class name,
     * followed by the serialized contents of the class.
     */
    private byte[] serializeClass(Serializable input) {
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
     * Utility method to deserialize any class.
     * First, the fully qualified class name is read from the
     * input stream.  Then, if a class matching that name exists,
     * it is instantiated.  Finally, if that class implements the
     * Serializable interface, the input stream is passed on
     * to its deserialize method.
     */
    private Object deserializeClass(byte[] data) {
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(data));
        Object result = null;
        try {
            String classType = input.readUTF();
            result = Class.forName(classType).newInstance();
            if(result instanceof Serializable) {
                ((Serializable)result).deserialize(input);
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
}
