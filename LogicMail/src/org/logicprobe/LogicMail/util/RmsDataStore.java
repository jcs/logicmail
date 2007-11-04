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
import java.util.Hashtable;
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
    /** Name of the RMS store */
    private String storeName;
    /** Name to UID mappings */
    private SerializableHashtable nameMap;
    /** UID to Object mappings */
    private Hashtable objectMap;
    
    /**
     * Creates a new instance of RmsDataStore.
     * @param name Name of the RMS store to use.
     */
    public RmsDataStore(String storeName) {
        this.storeName = storeName;
        this.objectMap = new Hashtable();
        this.nameMap = new SerializableHashtable();
    }

    public Serializable getNamedObject(String name) {
        // Get the ID that matches the name
        Object value = nameMap.get(name);
        if(value instanceof Long) {
            // Now get the object that matches the ID
            value = objectMap.get(value);
            return (Serializable)value;
        }
        else {
            return null;
        }
    }
    
    public String[] getNamedObjects() {
        String[] result = new String[nameMap.size()];
        Enumeration e = nameMap.keys();
        int i = 0;
        while(e.hasMoreElements()) {
            result[i++] = (String)e.nextElement();
        }
        return result;
    }

    public Serializable getObject(long id) {
        return (Serializable)objectMap.get(new Long(id));
    }

    public void putNamedObject(String name, Serializable object) {
        nameMap.put(name, new Long(object.getUniqueId()));
        putObject(object);
    }

    public void putObject(Serializable object) {
        objectMap.put(new Long(object.getUniqueId()), object);
    }

    public void removeNamedObject(String name) {
        removeObject(getNamedObject(name));
        nameMap.remove(name);
    }

    public void removeObject(Serializable object) {
        objectMap.remove(new Long(object.getUniqueId()));
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
            
            // Serialize the name map, and store
            // it at the first index.
            byteArray = serializeClass(nameMap);
            store.addRecord(byteArray, 0, byteArray.length);
            
            // Store all the objects in the object map
            Enumeration e = objectMap.elements();
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
                // Deserialize the name map
                deserializedObject = deserializeClass(store.getRecord(1));
                if(deserializedObject instanceof SerializableHashtable) {
                    nameMap = (SerializableHashtable)deserializedObject;
                }
                else {
                    return;
                }
            
                // Populate the vector
                objectMap.clear();
                if(records > 1) {
                    for(int i=2;i<=records;i++) {
                        deserializedObject = deserializeClass(store.getRecord(i));
                        if(deserializedObject instanceof Serializable) {
                            objectMap.put(new Long(((Serializable)deserializedObject).getUniqueId()), deserializedObject);
                        }
                    }
                }
            }
            
            store.closeRecordStore();
        } catch (RecordStoreException exp) {
            // do nothing
        }
    }

    public void delete() {
        try {
            RecordStore.deleteRecordStore(storeName);
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
    private Serializable deserializeClass(byte[] data) {
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
}
