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
import java.util.Vector;

import org.logicprobe.LogicMail.mail.FolderTreeItem;
import org.logicprobe.LogicMail.mail.PersistableFolderTreeItem;
import org.logicprobe.LogicMail.message.FolderMessage;
import org.logicprobe.LogicMail.message.PersistableFolderMessage;

import net.rim.device.api.system.ObjectGroup;
import net.rim.device.api.util.IntHashtable;
import net.rim.device.api.util.LongHashtable;
import net.rim.device.api.util.Persistable;

/**
 * Container class for persisting <code>FolderMessage</code> objects
 * representing message headers for a mail store.
 * <p>
 * Special care needs to be taken to ensure that all of the fields of this
 * class are in a raw form for persistence.  
 * </p>
 */
public class FolderMessageCacheObject implements Persistable {
    /**
     * Map of folder Unique IDs to serialized FolderTreeItem objects.
     */
    private LongHashtable cachedFolders;

    /**
     * Map of folder Unique IDs to IntHashtable instances that map message UIDs
     * to serialized FolderMessage objects.
     */
    private LongHashtable cachedMessages;
    
    /**
     * Instantiates a new folder message cache object, when creating for the
     * first time.  This constructor is not expected to be called when this
     * object is loaded from persistent storage.
     */
    public FolderMessageCacheObject() {
        initializeIfNecessary();
    }
    
    private void initializeIfNecessary() {
        if(cachedFolders == null || cachedMessages == null) {
            cachedFolders = new LongHashtable();
            cachedMessages = new LongHashtable();
        }
    }
    
    public FolderTreeItem[] getFolders() {
        initializeIfNecessary();
        FolderTreeItem[] result;
        
        if(cachedFolders != null) {
            int size = cachedFolders.size();
            Vector loadedFolders = new Vector(size);
            
            Enumeration e = cachedFolders.elements();
            while(e.hasMoreElements()) {
                Object element = e.nextElement();
                if(element instanceof PersistableFolderTreeItem) {
                    PersistableFolderTreeItem persistableFolder = (PersistableFolderTreeItem)element;
                    FolderTreeItem folder = new FolderTreeItem(persistableFolder);
                    loadedFolders.addElement(folder);
                }
            }
            result = new FolderTreeItem[loadedFolders.size()];
            loadedFolders.copyInto(result);
        }
        else {
            result = new FolderTreeItem[0];
        }
        return result;
    }
    
    public void removeFolder(FolderTreeItem folder) {
        cachedFolders.remove(folder.getUniqueId());
        cachedMessages.remove(folder.getUniqueId());
    }
    
    public FolderMessage[] getFolderMessages(FolderTreeItem folder) {
        FolderMessage[] result;
        if(cachedMessages != null && cachedMessages.containsKey(folder.getUniqueId())) {
            IntHashtable messageTable = (IntHashtable)cachedMessages.get(folder.getUniqueId());
            Vector loadedMessages = new Vector(messageTable.size());
            Enumeration e = messageTable.elements();
            while(e.hasMoreElements()) {
                Object element = e.nextElement();
                if(element instanceof PersistableFolderMessage) {
                    PersistableFolderMessage persistableMessage = (PersistableFolderMessage)element;
                    FolderMessage message = new FolderMessage(persistableMessage);
                    loadedMessages.addElement(message);
                }
            }
            result = new FolderMessage[loadedMessages.size()];
            loadedMessages.copyInto(result);
        }
        else {
            result = new FolderMessage[0];
        }
        return result;
    }
    
    public void addFolderMessage(FolderTreeItem folder, FolderMessage message) {
        IntHashtable messageTable = (IntHashtable)cachedMessages.get(folder.getUniqueId());
        if(messageTable == null) {
            messageTable = new IntHashtable();
            PersistableFolderTreeItem persistableFolder = folder.getPersistable();
            ObjectGroup.createGroup(persistableFolder);
            cachedFolders.put(folder.getUniqueId(), persistableFolder);
            cachedMessages.put(folder.getUniqueId(), messageTable);
        }
        PersistableFolderMessage persistableMessage = message.getPersistable();
        ObjectGroup.createGroup(persistableMessage);
        messageTable.put(message.getUid(), persistableMessage);
    }

    public void removeFolderMessage(FolderTreeItem folder, FolderMessage message) {
        IntHashtable messageTable = (IntHashtable)cachedMessages.get(folder.getUniqueId());
        if(messageTable != null) {
            messageTable.remove(message.getUid());
            if(messageTable.isEmpty()) {
                cachedMessages.remove(folder.getUniqueId());
            }
        }
    }

    public void updateFolderMessage(FolderTreeItem folder, FolderMessage message) {
        IntHashtable messageTable = (IntHashtable)cachedMessages.get(folder.getUniqueId());
        if(messageTable != null && messageTable.containsKey(message.getUid())) {
            PersistableFolderMessage persistableMessage = message.getPersistable();
            ObjectGroup.createGroup(persistableMessage);
            messageTable.put(message.getUid(), persistableMessage);
        }
    }
    
    public void clear() {
        cachedFolders.clear();
        cachedMessages.clear();
    }
}
