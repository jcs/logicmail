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
package org.logicprobe.LogicMail.util;

import java.io.EOFException;
import java.util.Enumeration;
import java.util.Vector;

import net.rim.device.api.synchronization.ConverterUtilities;
import net.rim.device.api.synchronization.SyncObject;
import net.rim.device.api.util.DataBuffer;
import net.rim.device.api.util.ToLongHashtable;

/**
 * This synchronization object is implemented specifically for handling the
 * internal data of <code>DataStore</code> implementations.  For the sake of
 * efficiency, it exposes a package-scoped interface that is rather low-level
 * and DataStore-specific.
 */
public class DataStoreSyncObject implements SyncObject {
    private final int uid;
    private ToLongHashtable nameMap;
    private Vector objectData;
    
    private static final int TYPE_STORE_UID = 1;
    private static final int TYPE_NAMEMAP_LENGTH = 10;
    private static final int TYPE_NAMEMAP_ENTRY_KEY = 11;
    private static final int TYPE_NAMEMAP_ENTRY_VALUE = 21;
    private static final int TYPE_DATA_LENGTH = 20;
    private static final int TYPE_DATA_ELEMENT = 21;
    
    /**
     * Instantiates a new data store sync object.
     *
     * @param uid the UID for the sync object, which should be the UID of
     * the store, cast to <code>int</code>.
     */
    public DataStoreSyncObject(int uid) {
        this.uid = uid;
    }
    
    /* (non-Javadoc)
     * @see net.rim.device.api.synchronization.SyncObject#getUID()
     */
    public int getUID() {
        return uid;
    }

    /**
     * Sets the name map to be used for synchronization, which uses
     * <code>String</code> keys and <code>long</code> values.
     *
     * @param nameMap the name map to be used for synchronization
     */
    void setNameMap(ToLongHashtable nameMap) {
        this.nameMap = nameMap;
    }
    
    /**
     * Gets the name map that has been read via synchronization, which uses
     * <code>String</code> keys and <code>long</code> values.
     *
     * @return the name map read via synchronization
     */
    ToLongHashtable getNameMap() {
        return nameMap;
    }
    
    /**
     * Sets the object data to be used for synchronization, which contains
     * objects serialized into <code>byte[]</code> arrays as its elements.
     *
     * @param objectData the object data to be used for synchronization
     */
    void setObjectData(Vector objectData) {
        this.objectData = objectData;
    }
    
    /**
     * Gets the object data that has been read via synchronization, which
     * contains objects serialized into <code>byte[]</code> arrays as its
     * elements. 
     *
     * @return the object data read via synchronization
     */
    Vector getObjectData() {
        return objectData;
    }
    
    /**
     * Save the contents of this sync object to the provided buffer, using the
     * <code>ConverterUtilities</code> format.
     * 
     * @param buffer the buffer into which the data will be saved.
     */
    public void save(DataBuffer buffer) {
        if(nameMap == null || objectData == null) { return; }
        
        // Write the UID of the sync object
        ConverterUtilities.writeLong(buffer, TYPE_STORE_UID, uid);

        // Create an array of the keys in the name map
        String[] names = new String[nameMap.size()];
        Enumeration en = nameMap.keys();
        int i = 0;
        while(en.hasMoreElements()) {
            names[i++] = (String)en.nextElement();
        }

        // Write the length of the name map
        ConverterUtilities.writeInt(buffer, TYPE_NAMEMAP_LENGTH, names.length);
        
        // Write the key/value pairs contained within the name map
        for(i=0; i<names.length; i++) {
            ConverterUtilities.writeString(buffer, TYPE_NAMEMAP_ENTRY_KEY, names[i]);
            ConverterUtilities.writeLong(buffer, TYPE_NAMEMAP_ENTRY_VALUE,
                    nameMap.get(names[i]));
        }
        
        // Write the length of the object data vector
        int objectDataSize = objectData.size();
        ConverterUtilities.writeInt(buffer, TYPE_DATA_LENGTH, objectDataSize);
        
        // Write the elements within the object data vector
        for(i=0; i<objectDataSize; i++) {
            ConverterUtilities.writeByteArray(buffer, TYPE_DATA_ELEMENT,
                    (byte[])objectData.elementAt(i));
            
        }
    }
    
    /**
     * Load the contents of this sync object from the provided buffer, using
     * the <code>ConverterUtilities</code> format.
     * 
     * @param buffer the buffer from which the data will be loaded
     */
    public void load(DataBuffer buffer) throws EOFException {
        ToLongHashtable tempNameMap;
        Vector tempObjectData;
        long readStoreUid = ConverterUtilities.readLong(buffer);
        if(readStoreUid != uid) {
            // UID mismatch means we cannot load this object
            return;
        }
        
        // Read the length of the name map
        int nameMapLength = ConverterUtilities.readInt(buffer);
        if(nameMapLength < 0) { return; }
        
        // Read the key/value pairs of the name map
        tempNameMap = new ToLongHashtable(nameMapLength);
        for(int i=0; i<nameMapLength; i++) {
            String key = ConverterUtilities.readString(buffer);
            long value = ConverterUtilities.readLong(buffer);
            tempNameMap.put(key, value);
        }
        
        // Read the length of the object data vector
        int dataMapLength = ConverterUtilities.readInt(buffer);
        if(dataMapLength < 0) { return; }
        
        // Read the elements of the object data vector
        tempObjectData = new Vector(dataMapLength);
        for(int i=0; i<dataMapLength; i++) {
            byte[] data = ConverterUtilities.readByteArray(buffer);
            if(data == null || data.length == 0) { continue; }
            tempObjectData.addElement(data);
        }
        
        this.nameMap = tempNameMap;
        this.objectData = tempObjectData;
    }
}
