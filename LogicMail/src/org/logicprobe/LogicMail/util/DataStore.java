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

/**
 * Provides a common interface for persistent data storage implementations.
 * The interface provides the ability to store objects by name or unique id.
 * Since all serializable objects provide a unique id, everything should
 * actually be stored by unique id.  A mapping table should be internally
 * maintained to allow name looups.  Named storage allows retrieval of
 * top-level objects for which the unique id is unknown.
 */
public interface DataStore {
    /**
     * Gets an object by name lookup.
     * @param name Name for the object
     * @return Matching object
     */
    public abstract Serializable getNamedObject(String name);
    
    /**
     * Gets an object by unique id lookup.
     * @param id Unique id for the object
     * @return Matching object
     */
    public abstract Serializable getObject(long id);
    
    /**
     * Puts an object into the store with a name mapping.
     * This should also create the necessary unique id mapping.
     * If the object matching the name already exists in the store,
     * it will be overwritten.
     * @param name Name for the object
     * @param object Object to store
     */
    public abstract void putNamedObject(String name, Serializable object);
    
    /**
     * Puts an object into the store.
     * If the object matching the same unique id already exists in the store,
     * it will be overwritten.
     * @param object Object to store
     */
    public abstract void putObject(Serializable object);
    
    /**
     * Removes an object with a name mapping from the store.
     * This will also remove the unique id mapping.
     * @param name Name of object to remove
     */
    public abstract void removeNamedObject(String name);
    
    /**
     * Removes an object from the store.
     * @param object Object to remove
     */
    public abstract void removeObject(Serializable object);
    
    /**
     * Save the contents of the store to persistent memory.
     */
    public abstract void save();
    
    /**
     * Load the contents of the store from persistent memory.
     */
    public abstract void load();
}
