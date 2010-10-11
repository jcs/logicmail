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
import net.rim.device.api.util.IntHashtable;
import net.rim.device.api.util.SimpleSortingVector;

import org.logicprobe.LogicMail.mail.FolderTreeItem;
import org.logicprobe.LogicMail.message.FolderMessage;

/**
 * Manager for controlling local persistence of <code>FolderMessage</code>
 * objects representing message headers for a mail store.
 */
public class FolderMessageCache {
    private final long cacheObjectKey;
    private final Hashtable cachedFolderSet = new Hashtable();
    private final Hashtable folderMessageListTable = new Hashtable();
    private final Hashtable folderMessageMapTable = new Hashtable();
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
        //System.err.println("-->getFolders()");
        
        synchronized(lockObj) {
            int size = cachedFolderSet.size();
            FolderTreeItem[] folders = new FolderTreeItem[size];
            Enumeration e = cachedFolderSet.keys();
            for (int i = 0; i < size; i++) {
                folders[i] = (FolderTreeItem)e.nextElement();
            }
            return folders;
        }
    }
    
    public FolderMessage[] getFolderMessages(FolderTreeItem folder) {
        //System.err.println("-->getFolderMessages()");
        
        FolderMessage[] result;
        synchronized(lockObj) {
            checkAndLoadFolderCache(folder);
            SimpleSortingVector messageList = (SimpleSortingVector)folderMessageListTable.get(folder);
            if(messageList != null) {
                result = new FolderMessage[messageList.size()];
                messageList.copyInto(result);
            }
            else {
                result = new FolderMessage[0];
            }
        }
        return result;
    }
    
    public void addFolderMessage(FolderTreeItem folder, FolderMessage message) {
        //System.err.println("-->addFolderMessage(\"" + folder.getPath() + "\", \"" + message.getMessageToken().getMessageUid() + "\")");
        
        synchronized(lockObj) {
            checkAndLoadFolderCache(folder);
            SimpleSortingVector messageList;
            IntHashtable messageMap;
            boolean newFolder;
            if(folderMessageListTable.containsKey(folder)) {
                messageList = (SimpleSortingVector)folderMessageListTable.get(folder);
                messageMap = (IntHashtable)folderMessageMapTable.get(folder);
                newFolder = false;
            }
            else {
                messageList = new SimpleSortingVector();
                messageList.setSortComparator(FolderMessage.getComparator());
                messageList.setSort(true);
                folderMessageListTable.put(folder, messageList);
                messageMap = new IntHashtable();
                folderMessageMapTable.put(folder, messageMap);
                newFolder = true;
            }
            
            if(!messageMap.containsKey(message.getUid())) {
                messageList.addElement(message);
                messageMap.put(message.getUid(), message);
                
                cacheObject.addFolderMessage(folder, message);
                if(newFolder) {
                    cachedFolderSet.put(folder, Boolean.TRUE);
                }
            }
        }
    }
    
    public void removeFolderMessage(FolderTreeItem folder, FolderMessage message) {
        //System.err.println("-->removeFolderMessage(\"" + folder.getPath() + "\", \"" + message.getMessageToken().getMessageUid() + "\")");
        
        synchronized(lockObj) {
            checkAndLoadFolderCache(folder);
            if(folderMessageListTable.containsKey(folder)) {
                SimpleSortingVector messageList = (SimpleSortingVector)folderMessageListTable.get(folder);
                IntHashtable messageMap = (IntHashtable)folderMessageMapTable.get(folder);
                messageList.removeElement(message);
                messageMap.remove(message.getUid());
                
                if(messageList.isEmpty()) {
                    removeFolder(folder);
                    cacheObject.removeFolder(folder);
                    cachedFolderSet.remove(folder);
                }
                else {
                    cacheObject.removeFolderMessage(folder, message);
                }
            }
        }
    }
    
    public boolean updateFolderMessage(FolderTreeItem folder, FolderMessage message) {
        //System.err.println("-->updateFolderMessage(\"" + folder.getPath() + "\", \"" + message.getMessageToken().getMessageUid() + "\")");

        boolean updated = false;
        synchronized(lockObj) {
            checkAndLoadFolderCache(folder);
            if(folderMessageListTable.containsKey(folder)) {
                IntHashtable messageMap = (IntHashtable)folderMessageMapTable.get(folder);
                FolderMessage existingMessage = (FolderMessage)messageMap.get(message.getUid());
                if(existingMessage != null) {
                    existingMessage.setIndex(message.getIndex());
                    existingMessage.setFlags(message.getFlags());
                    
                    cacheObject.updateFolderMessage(folder, existingMessage);
                    updated = true;
                }
            }
        }
        return updated;
    }
    
    public void removeFolder(FolderTreeItem folder) {
        //System.err.println("-->removeFolder(\"" + folder.getPath() + "\")");
        
        synchronized(lockObj) {
            folderMessageListTable.remove(folder);
            folderMessageMapTable.remove(folder);
            cacheObject.removeFolder(folder);
            cachedFolderSet.remove(folder);
        }
    }
    
    
    /**
     * Check to see if the cache for a folder is available, and load if
     * necessary.
     *
     * @param folder the folder to check the cache for
     */
    private void checkAndLoadFolderCache(FolderTreeItem folder) {
        if(!folderMessageListTable.containsKey(folder) && cachedFolderSet.containsKey(folder)) {
            SimpleSortingVector messageList = new SimpleSortingVector();
            messageList.setSortComparator(FolderMessage.getComparator());
            messageList.setSort(true);
            folderMessageListTable.put(folder, messageList);
            IntHashtable messageMap = new IntHashtable();
            folderMessageMapTable.put(folder, messageMap);
            
            FolderMessage[] messages = cacheObject.getFolderMessages(folder);
            for(int i=0; i<messages.length; i++) {
                messageList.addElement(messages[i]);
                messageMap.put(messages[i].getUid(), messages[i]);
            }
        }
    }
    
    /**
     * Restore the persisted contents of the folder message cache.
     * This method should only be called once.
     */
    public void restore() {
        //System.err.println("-->restore()");
        
        synchronized(lockObj) {
            FolderTreeItem[] folders = cacheObject.getFolders();
            for(int i=0; i<folders.length; i++) {
                cachedFolderSet.put(folders[i], Boolean.TRUE);
            }
        }
    }
    
    /**
     * Commit the contents of the folder message cache to the persistent store.
     * This method should be called at the end of any batch of operations.
     */
    public void commit() {
        //System.err.println("-->commit()");
        
        synchronized(lockObj) {
            persistentObject.commit();
        }
    }

    /**
     * Remove the persisted data from the persistent store.
     * This method should only be called as part of a test cleanup.  Following
     * the invocation of this method, this object will be in an invalid and
     * unusable state.  To use it again, it must be recreated.
     */
    public void destroy() {
        //System.err.println("-->destroy()");
        
        synchronized(lockObj) {
            PersistentStore.destroyPersistentObject(cacheObjectKey);
        }
    }

}
