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
package org.logicprobe.LogicMail.model;

import java.util.Enumeration;
import java.util.Hashtable;

import net.rim.device.api.system.PersistentObject;
import net.rim.device.api.system.PersistentStore;
import net.rim.device.api.collection.util.BigVector;

import org.logicprobe.LogicMail.mail.FolderTreeItem;
import org.logicprobe.LogicMail.mail.MessageToken;
import org.logicprobe.LogicMail.message.FolderMessage;

/**
 * Manager for controlling local persistence of <code>FolderMessage</code>
 * objects representing message headers for a mail store.
 */
public class FolderMessageCache {
    private final long cacheObjectKey;
    
    /** Map of FolderTreeItem -> CacheEntry */
    private final Hashtable cachedFolderMap = new Hashtable();
    
    private final Object lockObj = new Object();
    private final PersistentObject persistentObject;
    private FolderMessageCacheObject cacheObject;
    
    /**
     * Instantiates a new folder message cache.
     */
    public FolderMessageCache() {
        //"org.logicprobe.LogicMail.model.FolderMessageCacheObject"
        this(0x77ecbda07bb2fe8fL);
    }

    /**
     * Instantiates a new folder message cache, with a specific key for the
     * persistent object.  This constructor should only be called directly
     * from test-specific subclasses that need to avoid conflicts with normal
     * persisted application data.
     *
     * @param cacheObjectKey the cache object key
     */
    protected FolderMessageCache(long cacheObjectKey) {
        this.cacheObjectKey = cacheObjectKey;
        persistentObject = PersistentStore.getPersistentObject(cacheObjectKey);
        Object persisted = persistentObject.getContents();
        if(persisted instanceof FolderMessageCacheObject) {
            cacheObject = (FolderMessageCacheObject)persisted;
        }
        else {
            cacheObject = new FolderMessageCacheObject();
            persistentObject.setContents(cacheObject);
            persistentObject.commit();
        }
    }
    
    public FolderTreeItem[] getFolders() {
        synchronized(lockObj) {
            int size = cachedFolderMap.size();
            FolderTreeItem[] folders = new FolderTreeItem[size];
            Enumeration e = cachedFolderMap.keys();
            for (int i = 0; i < size; i++) {
                folders[i] = (FolderTreeItem)e.nextElement();
            }
            return folders;
        }
    }
    
    public FolderMessage[] getFolderMessages(FolderTreeItem folder) {
        FolderMessage[] result;
        synchronized(lockObj) {
            CacheEntry cacheEntry = checkAndLoadFolderCache(folder);
            result = cacheEntry.getFolderMessages();
        }
        return result;
    }
    
    public void addFolderMessage(FolderTreeItem folder, FolderMessage message) {
        synchronized(lockObj) {
            CacheEntry cacheEntry = checkAndLoadFolderCache(folder);
            if(cacheEntry.addFolderMessage(message)) {
                cacheObject.addFolderMessage(folder, message);
            }
            else {
                // If the item could not be added, then try to update its
                // existing cache entry.
                FolderMessage updatedMessage = cacheEntry.updateFolderMessage(message);
                if(updatedMessage != null) {
                    cacheObject.updateFolderMessage(folder, updatedMessage);
                }
            }
        }
    }

    public FolderMessage getFolderMessage(FolderTreeItem folder, MessageToken messageToken) {
        FolderMessage folderMessage = null;
        synchronized(lockObj) {
            CacheEntry cacheEntry = checkAndLoadFolderCache(folder);
            folderMessage = cacheEntry.getFolderMessage(messageToken);
        }
        return folderMessage;
    }
    
    public void removeFolderMessage(FolderTreeItem folder, FolderMessage message) {
        synchronized(lockObj) {
            CacheEntry cacheEntry = checkAndLoadFolderCache(folder);
            if(cacheEntry.removeFolderMessage(message)) {
                if(cacheEntry.isEmpty()) {
                    removeFolder(folder);
                    cacheObject.removeFolder(folder);
                }
                else {
                    cacheObject.removeFolderMessage(folder, message);
                }
            }
        }
    }
    
    public boolean updateFolderMessage(FolderTreeItem folder, FolderMessage message) {
        synchronized(lockObj) {
            CacheEntry cacheEntry = checkAndLoadFolderCache(folder);
            FolderMessage updatedMessage = cacheEntry.updateFolderMessage(message);
            if(updatedMessage != null) {
                cacheObject.updateFolderMessage(folder, updatedMessage);
                return true;
            }
            else {
                return false;
            }
        }
    }
    
    public void removeFolder(FolderTreeItem folder) {
        synchronized(lockObj) {
            cacheObject.removeFolder(folder);
            cachedFolderMap.remove(folder);
        }
    }
    
    /**
     * Check to see if the cache for a folder is available, and load if
     * necessary.
     *
     * @param folder the folder to check the cache for
     * @return the cached data for the folder
     */
    private CacheEntry checkAndLoadFolderCache(FolderTreeItem folder) {
        CacheEntry cacheEntry = (CacheEntry)cachedFolderMap.get(folder);
        if(cacheEntry == null) {
            cacheEntry = new CacheEntry();
            cacheEntry.setLoaded(true);
            cachedFolderMap.put(folder, cacheEntry);
        }
        else if(!cacheEntry.isLoaded()) {
            FolderMessage[] messages = cacheObject.getFolderMessages(folder);
            for(int i=0; i<messages.length; i++) {
                cacheEntry.addFolderMessage(messages[i]);
            }
            cacheEntry.setLoaded(true);
        }
        return cacheEntry;
    }
    
    /**
     * Restore the persisted contents of the folder message cache.
     * This method should only be called once.
     */
    public void restore() {
        synchronized(lockObj) {
            FolderTreeItem[] folders = cacheObject.getFolders();
            for(int i=0; i<folders.length; i++) {
                cachedFolderMap.put(folders[i], new CacheEntry());
            }
        }
    }
    
    /**
     * Commit the contents of the folder message cache to the persistent store.
     * This method should be called at the end of any batch of operations.
     */
    public void commit() {
        synchronized(lockObj) {
            // Resort any message lists containing updated messages
            Enumeration e = cachedFolderMap.elements();
            while(e.hasMoreElements()) {
                ((CacheEntry)e.nextElement()).sortIfNecessary();
            }
            
            persistentObject.commit();
        }
    }

    /**
     * Clear the contents of the folder message cache, without actually deleting
     * the persistent store object.  This method is intended to be called in
     * response to user interaction.
     */
    public void clear() {
        synchronized(lockObj) {
            cachedFolderMap.clear();
            cacheObject.clear();
            persistentObject.commit();
        }
    }
    
    /**
     * Remove the persisted data from the persistent store.
     * This method should only be called as part of a test cleanup.  Following
     * the invocation of this method, this cache will be in an invalid and
     * unusable state.  To use it again, it must be recreated.
     */
    public void destroy() {
        synchronized(lockObj) {
            cachedFolderMap.clear();
            PersistentStore.destroyPersistentObject(cacheObjectKey);
        }
    }
    
    private static class CacheEntry {
        private boolean loaded;
        private boolean messagesUpdated;
        private final BigVector messageList = new BigVector();
        private final Hashtable messageMap = new Hashtable();
        
        public void setLoaded(boolean loaded) {
            this.loaded = loaded;
        }

        public boolean isLoaded() {
            return loaded;
        }
        
        public FolderMessage[] getFolderMessages() {
            int size = messageList.size();
            FolderMessage[] result = new FolderMessage[size];
            messageList.copyInto(0, size, result, 0);
            return result;
        }
        
        public boolean addFolderMessage(FolderMessage message) {
            String messageUid = message.getMessageToken().getMessageUid();
            if(!messageMap.containsKey(messageUid)) {
                messageList.insertElement(FolderMessage.getComparator(), message);
                messageMap.put(messageUid, message);
                return true;
            }
            else {
                return false;
            }
        }

        public FolderMessage getFolderMessage(MessageToken messageToken) {
            String messageUid = messageToken.getMessageUid();
            return (FolderMessage)messageMap.get(messageUid);
        }
        
        public boolean removeFolderMessage(FolderMessage message) {
            if(messageList.removeElement(FolderMessage.getComparator(), message)) {
                messageMap.remove(message.getMessageToken().getMessageUid());
                return true;
            }
            else {
                return false;
            }
        }
        
        public FolderMessage updateFolderMessage(FolderMessage message) {
            FolderMessage existingMessage = (FolderMessage)messageMap.get(message.getMessageToken().getMessageUid());
            if(existingMessage != null) {
                existingMessage.setIndex(message.getIndex());
                existingMessage.setFlags(message.getFlags());
                messagesUpdated = true;
                return existingMessage;
            }
            else {
                return null;
            }
        }
        
        public void sortIfNecessary() {
            if(messagesUpdated) {
                messageList.sort(FolderMessage.getComparator());
                messagesUpdated = false;
            }
        }
        
        public boolean isEmpty() {
            return messageList.isEmpty();
        }
    }
}
