/*-
 * Copyright (c) 2006, Derek Konigsberg
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

package org.logicprobe.LogicMail.cache;

import org.logicprobe.LogicMail.conf.AccountConfig;
import org.logicprobe.LogicMail.mail.FolderTreeItem;
import org.logicprobe.LogicMail.util.DataStore;
import org.logicprobe.LogicMail.util.DataStoreFactory;
import org.logicprobe.LogicMail.util.SerializableHashtable;

public class AccountCache {
    /** The global metadata store */
    private DataStore metadataStore;
    /** The per-account cache store */
    private DataStore cacheStore;
    /** Unique id for the account */
    private long accountId;
    /** Flag to prevent redundant loading */
    private boolean metadataLoaded;
    /** Flag to prevent redundant loading */
    private boolean cacheLoaded;
    
    private static String FOLDER_TREE = "folder_tree";
    
    public AccountCache(AccountConfig accountConfig) {
        accountId = accountConfig.getUniqueId();
        metadataStore = DataStoreFactory.getMetadataStore();
        cacheStore = DataStoreFactory.getConnectionCacheStore(accountId);
    }
    
    /**
     * Save the folder tree for the account.
     *
     * @param folderRoot Root item of the tree.
     */
    public void saveFolderTree(FolderTreeItem folderRoot) {
        if(!cacheLoaded) {
            cacheStore.load();
        }

        cacheStore.putNamedObject(FOLDER_TREE, folderRoot);
        cacheStore.save();
    }

    /**
     * Load the folder tree for the account.
     *
     * @return Root item of the tree.
     */
    public FolderTreeItem loadFolderTree() {
        if(!cacheLoaded) {
            cacheStore.load();
        }
        
        Object loadedObj = cacheStore.getNamedObject(FOLDER_TREE);
        
        if(loadedObj instanceof FolderTreeItem) {
            return (FolderTreeItem)loadedObj;
        }
        else {
            return null;
        }
    }
    
    /**
     * Save metadata associated with an account.
     * This is intended to be used for saving things not relating
     * to the mail protocols specifically, such as UI state.
     *
     * @param key The key to save metadata under
     * @param metadata A serializable hashtable containing the metadata to save
     */
    public void saveAccountMetadata(String key, SerializableHashtable metadata) {
        if(!metadataLoaded) {
            metadataStore.load();
        }
        
        metadataStore.putNamedObject(Long.toString(accountId)+"_"+key, metadata);

        metadataStore.save();
    }
    
    /**
     * Load metadata associated with an account.
     * This is intended to be used for loading things not relating
     * to the mail protocols specifically, such as UI state.
     *
     * @param key The key that the metadata was saved under
     * @return A serializable hashtable containing the saved metadata
     */
    public SerializableHashtable loadAccountMetadata(String key) {
        if(!metadataLoaded) {
            metadataStore.load();
        }

        Object loadedObj = metadataStore.getNamedObject(Long.toString(accountId)+"_"+key);
        if(loadedObj instanceof SerializableHashtable) {
            return (SerializableHashtable)loadedObj;
        }
        else {
            return null;
        }
    }
}
