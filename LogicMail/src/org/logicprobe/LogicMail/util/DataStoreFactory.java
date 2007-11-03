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

import java.util.Hashtable;

/**
 * Factory for creating DataStore instances.
 * The DataStore instances are intended to be used by classes that
 * provide a less general interface to the types of data being
 * stored.
 */
public final class DataStoreFactory {
    private static DataStore configurationStore;
    private static DataStore metadataStore;
    private static Hashtable connectionCacheStoreTable;
    
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
            configurationStore = new RmsDataStore("LogicMail_config");
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
        if(configurationStore == null) {
            configurationStore = new RmsDataStore("LogicMail_metadata");
        }
        return configurationStore;
    }
    
    /**
     * Gets the cache store for a particular connection object.
     * This provides separate cache stores for each configured
     * connection that wants to make use of one.
     *
     * @param connectionId Unique id for the connection object
     * @return Cache store
     */
    public static synchronized DataStore getConnectionCacheStore(long connectionId) {
        if(connectionCacheStoreTable == null) {
            connectionCacheStoreTable = new Hashtable();
        }
        Long idObj = new Long(connectionId);
        Object value = connectionCacheStoreTable.get(idObj);
        if(value != null) {
            return (DataStore)value;
        }
        else {
            DataStore cacheStore = new RmsDataStore("LogicMail_c_"+idObj.toString());
            connectionCacheStoreTable.put(idObj, cacheStore);
            return cacheStore;
        }
    }
}
