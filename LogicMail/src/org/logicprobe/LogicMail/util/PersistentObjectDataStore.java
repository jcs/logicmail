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
import java.util.Hashtable;
import java.util.Vector;

import net.rim.device.api.system.PersistentObject;
import net.rim.device.api.system.PersistentStore;

public class PersistentObjectDataStore implements DataStore {
	/** Unique id for the root persistent object */
	private long storeUid;
	/** Root persistent object store */
    private PersistentObject store;
    /** Name to UID mappings */
    private Hashtable nameMap;
    /** UID to Object mappings */
    private Hashtable objectMap;
    
    /**
     * Creates a new instance of RmsDataStore.
     * @param storeUid Unique ID of the store to use.
     */
    public PersistentObjectDataStore(long storeUid) {
    	this.storeUid = storeUid;
        this.store = PersistentStore.getPersistentObject(storeUid);
        this.objectMap = new Hashtable();
        this.nameMap = new Hashtable();
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
		Hashtable newNameMap = null;
		Vector newObjectMap = null;
		synchronized(store) {
			Object[] storeData = (Object[])store.getContents();
			if(storeData != null) {
				newNameMap = (Hashtable)storeData[0];
				newObjectMap = (Vector)storeData[1];
			}
		}
		if(newNameMap != null && newObjectMap != null) {
			nameMap = newNameMap;
			Object deserializedObject;
			int size = newObjectMap.size();
			for(int i=0; i<size; i++) {
	        	try {
					deserializedObject = SerializationUtils.deserializeClass((byte[])newObjectMap.elementAt(i));
	                if(deserializedObject instanceof Serializable) {
	                    objectMap.put(new Long(((Serializable)deserializedObject).getUniqueId()), deserializedObject);
	                }
	        	} catch (Exception exp) { }
			}
		}
	}

	public void delete() {
		PersistentStore.destroyPersistentObject(storeUid);
	}
}
