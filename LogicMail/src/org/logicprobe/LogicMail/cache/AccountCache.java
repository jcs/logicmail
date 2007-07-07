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

import org.logicprobe.LogicMail.mail.FolderTreeItem;
import org.logicprobe.LogicMail.util.SerializableHashtable;

public class AccountCache {
    private String _acctName;
    
    public AccountCache(String acctName) {
        _acctName = acctName;
    }
    
    public String getAcctName() {
        return _acctName;
    }
    
    public void setAcctName(String acctName) {
        _acctName = acctName;
    }

    public void saveFolderTree(FolderTreeItem folderRoot) {
        String key = "acct_" + _acctName + "_folders";
        CacheWriter writer = new CacheWriter("LogicMail_acct_" + Integer.toString(key.hashCode()));
        writer.addItem(folderRoot);
        writer.store();
    }

    public FolderTreeItem loadFolderTree() {
        String key = "acct_" + _acctName + "_folders";
        CacheReader reader = new CacheReader("LogicMail_acct_" + Integer.toString(key.hashCode()));
        reader.load();
        if(reader.getNumItems() < 1) {
            return null;
        }
        FolderTreeItem folderRoot = new FolderTreeItem();
        reader.getItem(0, folderRoot);
        return folderRoot;
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
        String writerKey = "acct_" + _acctName + "_metadata_" + key;
        CacheWriter writer = new CacheWriter("LogicMail_acct_" + Integer.toString(writerKey.hashCode()));
        writer.addItem(metadata);
        writer.store();
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
        String readerKey = "acct_" + _acctName + "_metadata_" + key;
        CacheReader reader = new CacheReader("LogicMail_acct_" + Integer.toString(readerKey.hashCode()));
        reader.load();
        if(reader.getNumItems() < 1) {
            return null;
        }
        SerializableHashtable hashtable = new SerializableHashtable();
        reader.getItem(0, hashtable);
        return hashtable;
    }
}
