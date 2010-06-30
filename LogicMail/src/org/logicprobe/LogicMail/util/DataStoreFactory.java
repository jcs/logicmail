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
 * Factory for creating DataStore instances.
 * The DataStore instances are intended to be used by classes that
 * provide a less general interface to the types of data being
 * stored.
 */
public final class DataStoreFactory {
    private static DataStore configurationStore;
    private static DataStore metadataStore;
    private static DataStore connectionCacheStore;
    
    /** Creates a new instance of DataStoreFactory */
    private DataStoreFactory() {
    }
    
    /**
     * Gets the data store for configuration objects.
     *
     * @return Configuration store
     */
    public static synchronized DataStore getConfigurationStore() {
        if(configurationStore == null) {
        	// "org.logicprobe.LogicMail.store.configuration"
        	configurationStore = new PersistentObjectDataStore(0xbb981925fd9130abL);
        }
        return configurationStore;
    }
    
    /**
     * Gets the data store for metadata objects, such as runtime
     * state information that should be saved for ease of use.
     *
     * @return Metadata store
     */
    public static synchronized DataStore getMetadataStore() {
        if(metadataStore == null) {
        	// "org.logicprobe.LogicMail.store.metadata"
            metadataStore = new PersistentObjectDataStore(0xf630df16ea30eb49L);
        }
        return metadataStore;
    }
    
    /**
     * Gets the cache store for connections.  This is one global store
     * shared by all connections, and used to maintain lightweight
     * data like folder tree structures.  All users are expected to
     * use unique names to store their data.
     *
     * @return Cache store
     */
    public static synchronized DataStore getConnectionCacheStore() {
        if(connectionCacheStore == null) {
        	// "org.logicprobe.LogicMail.store.connection"
            connectionCacheStore = new PersistentObjectDataStore(0xe53945d6e054c98cL);
        }
        return connectionCacheStore;
    }
}
