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

import java.util.Enumeration;
import java.util.Vector;

import net.rim.device.api.system.PersistentObject;
import net.rim.device.api.system.PersistentStore;
import net.rim.device.api.util.LongHashtable;
import net.rim.device.api.util.ToLongHashtable;

public class PersistentObjectDataStore implements DataStore {
    /** Unique id for the root persistent object */
    private long storeUid;
    /** Root persistent object store */
    private PersistentObject store;
    /** Name to UID mappings */
    private ToLongHashtable nameMap;
    /** UID to Object mappings */
    private LongHashtable objectMap;
    
    /**
     * Creates a new instance of RmsDataStore.
     * @param storeUid Unique ID of the store to use.
     */
    public PersistentObjectDataStore(long storeUid) {
        this.storeUid = storeUid;
        this.store = PersistentStore.getPersistentObject(storeUid);
        this.nameMap = new ToLongHashtable();
        this.objectMap = new LongHashtable();
    }

    public Serializable getNamedObject(String name) {
        // Get the ID that matches the name
        long id = nameMap.get(name);
        if(id != -1) {
            // Now get the object that matches the ID
            return (Serializable)objectMap.get(id);
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
        return (Serializable)objectMap.get(id);
    }

    public void putNamedObject(String name, Serializable object) {
        // If this replaces an existing named object, we need to make sure to
        // remove the old object from the object map.
        long oldObjectId = nameMap.get(name);
        if(oldObjectId != -1) {
            objectMap.remove(oldObjectId);
        }
        
        nameMap.put(name, object.getUniqueId());
        putObject(object);
    }

    public void putObject(Serializable object) {
        objectMap.put(object.getUniqueId(), object);
    }

    public void removeNamedObject(String name) {
        Serializable object = getNamedObject(name);
        if(object != null) {
            objectMap.remove(object.getUniqueId());
        }
        nameMap.remove(name);
    }

    public void removeObject(Serializable object) {
        objectMap.remove(object.getUniqueId());
    }
    
    public void removeObject(long id) {
        objectMap.remove(id);
    }
    
    public void save() {
        Vector objectData = new Vector();

        byte[] byteArray;
        Enumeration e = objectMap.elements();
        while (e.hasMoreElements()) {
            try {
                byteArray = SerializationUtils.serializeClass((Serializable)e.nextElement());
                objectData.addElement(byteArray);
            } catch (Exception exp) { }
        }

        Object[] storeData = { nameMap, objectData };

        synchronized(store) {
            store.setContents(storeData);
            store.commit();
        }
    }

    public void load() {
        ToLongHashtable newNameMap = null;
        Vector newObjects = null;
        synchronized(store) {
            Object[] storeData = (Object[])store.getContents();
            if(storeData != null && storeData.length == 2
                    && storeData[0] instanceof ToLongHashtable
                    && storeData[1] instanceof Vector) {
                newNameMap = (ToLongHashtable)storeData[0];
                newObjects = (Vector)storeData[1];
            }
        }
        if(newNameMap != null && newObjects != null) {
            nameMap = newNameMap;
            Object deserializedObject;
            int size = newObjects.size();
            for(int i=0; i<size; i++) {
                try {
                    deserializedObject = SerializationUtils.deserializeClass((byte[])newObjects.elementAt(i));
                    if(deserializedObject != null) {
                        objectMap.put(
                                ((Serializable)deserializedObject).getUniqueId(),
                                deserializedObject);
                    }
                } catch (Exception exp) { }
            }
        }
    }

    public void delete() {
        PersistentStore.destroyPersistentObject(storeUid);
    }

    public int getSyncObjectUID() {
        return (int)storeUid;
    }
    
    public DataStoreSyncObject getSyncObject() {
        DataStoreSyncObject syncObject = new DataStoreSyncObject((int)storeUid);
        Object[] storeData = (Object[])store.getContents();
        if(storeData != null && storeData.length == 2
                && storeData[0] instanceof ToLongHashtable
                && storeData[1] instanceof Vector) {
            syncObject.setNameMap((ToLongHashtable)storeData[0]);
            syncObject.setObjectData((Vector)storeData[1]);
        }
        else {
            syncObject.setNameMap(new ToLongHashtable());
            syncObject.setObjectData(new Vector());
        }
        return syncObject;
    }

    public boolean setSyncObject(DataStoreSyncObject syncObject) {
        // Extra sanity checking to avoid populating from invalid data
        if(syncObject == null
                || syncObject.getUID() != (int)storeUid
                || syncObject.getNameMap() == null
                || syncObject.getObjectData() == null) {
            return false;
        }
        
        Object[] storeData = {
                syncObject.getNameMap(),
                syncObject.getObjectData() };
        
        store.setContents(storeData);
        store.commit();
        return true;
    }
}
