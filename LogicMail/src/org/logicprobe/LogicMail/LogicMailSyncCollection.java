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
package org.logicprobe.LogicMail;

import java.io.EOFException;
import java.util.Hashtable;

import org.logicprobe.LogicMail.conf.AccountConfig;
import org.logicprobe.LogicMail.conf.MailSettings;
import org.logicprobe.LogicMail.util.DataStore;
import org.logicprobe.LogicMail.util.DataStoreFactory;
import org.logicprobe.LogicMail.util.DataStoreSyncObject;

import net.rim.device.api.i18n.Locale;
import net.rim.device.api.notification.NotificationsConstants;
import net.rim.device.api.notification.NotificationsManager;
import net.rim.device.api.synchronization.SyncCollection;
import net.rim.device.api.synchronization.SyncConverter;
import net.rim.device.api.synchronization.SyncObject;
import net.rim.device.api.system.RuntimeStore;
import net.rim.device.api.util.DataBuffer;

public class LogicMailSyncCollection implements SyncCollection, SyncConverter {
    private static LogicMailSyncCollection instance;
    private DataStore[] dataStores;
    
    private LogicMailSyncCollection() {
        dataStores = new DataStore[3];
        dataStores[0] = DataStoreFactory.getConfigurationStore();
        dataStores[1] = DataStoreFactory.getConnectionCacheStore();
        dataStores[2] = DataStoreFactory.getMetadataStore();
    }
    
    public static synchronized LogicMailSyncCollection getInstance() {
        if(instance == null) {
            instance = new LogicMailSyncCollection();
        }
        return instance;
    }
    
    public void beginTransaction() {
        // Nothing needs to be done here
    }
    
    public void endTransaction() {
        // Commit is done during add
        
        // Re-run all the startup code, which should be refactored into a
        // common place to avoid duplication
        RuntimeStore.getRuntimeStore().remove(AppInfo.GUID);
        MailSettings mailSettings = MailSettings.getInstance();
        mailSettings.loadSettings();
        int numAccounts = mailSettings.getNumAccounts();
        Hashtable eventSourceMap = new Hashtable(numAccounts);
        for(int i=0; i<numAccounts; i++) {
            AccountConfig accountConfig = mailSettings.getAccountConfig(i);
            LogicMailEventSource eventSource =
                new LogicMailEventSource(accountConfig.getAcctName(), accountConfig.getUniqueId());
            NotificationsManager.registerSource(
                    eventSource.getEventSourceId(),
                    eventSource,
                    NotificationsConstants.CASUAL);
            eventSourceMap.put(new Long(accountConfig.getUniqueId()), eventSource);
        }
        RuntimeStore.getRuntimeStore().put(AppInfo.GUID, eventSourceMap);
    }
    
    public SyncConverter getSyncConverter() {
        return this;
    }
    
    public String getSyncName() {
        return "LogicMail";
    }

    public String getSyncName(Locale locale) {
        return null;
    }
    
    public int getSyncObjectCount() {
        return dataStores.length;
    }
    
    public SyncObject[] getSyncObjects() {
        SyncObject[] result = new SyncObject[dataStores.length];
        for(int i=0; i<dataStores.length; i++) {
            result[i] = dataStores[i].getSyncObject();
        }
        return result;
    }
    
    public SyncObject getSyncObject(int uid) {
        for(int i=0; i<dataStores.length; i++) {
            if(dataStores[i].getSyncObjectUID() == uid) {
                return dataStores[i].getSyncObject();
            }
        }
        return null;
    }
    
    public int getSyncVersion() {
        return 1;
    }
    
    public boolean addSyncObject(SyncObject object) {
        if(!(object instanceof DataStoreSyncObject)) { return false; }

        for(int i=0; i<dataStores.length; i++) {
            if(dataStores[i].getSyncObjectUID() == object.getUID()) {
                return dataStores[i].setSyncObject((DataStoreSyncObject)object);
            }
        }        

        return false;
    }

    public boolean removeSyncObject(SyncObject object) {
        return false;
    }
    
    public boolean removeAllSyncObjects() {
        return false;
    }

    public void clearSyncObjectDirty(SyncObject object) {
    }

    public void setSyncObjectDirty(SyncObject object) {
    }
    
    public boolean isSyncObjectDirty(SyncObject object) {
        return false;
    }

    public boolean updateSyncObject(SyncObject oldObject, SyncObject newObject) {
        return false;
    }

    public SyncObject convert(DataBuffer data, int version, int UID) {
        // Reading in a synchronization object
        
        if(version != getSyncVersion()) { return null; }

        try {
            DataStoreSyncObject syncObject = new DataStoreSyncObject(UID);
            syncObject.load(data);
            return syncObject;
        } catch (EOFException e) {
            return null;
        }
    }

    public boolean convert(SyncObject object, DataBuffer buffer, int version) {
        // Writing out a synchronization object
        
        if(version != getSyncVersion()) { return false; }
        
        if(object instanceof DataStoreSyncObject) {
            DataStoreSyncObject syncObject = (DataStoreSyncObject)object;
            syncObject.save(buffer);
            return true;
        }
        else {
            return false;
        }
    }
}
