/*
 * MailSettings.java
 *
 * © <your company here>, 2003-2005
 * Confidential and proprietary.
 */

package org.logicprobe.LogicMail.conf;

import java.util.Vector;
import javax.microedition.rms.*;

/**
 * Provide an interface to global and account-specific
 * configuration, along with a front-end to the Record Store
 */
public class MailSettings {
    private static MailSettings _instance;
    private GlobalConfig _globalConfig;
    private Vector _accountConfigs;
    
    private MailSettings() {
        _globalConfig = new GlobalConfig();
        _accountConfigs = new Vector();
    }
    
    public static synchronized MailSettings getInstance() {
        if(_instance == null)
            _instance = new MailSettings();
        return _instance;
    }
    
    public GlobalConfig getGlobalConfig() {
        return _globalConfig;
    }
    
    public int getNumAccounts() {
        return _accountConfigs.size();
    }
    
    public AccountConfig getAccountConfig(int index) {
        return (AccountConfig)_accountConfigs.elementAt(index);
    }
    
    public void addAccountConfig(AccountConfig accountConfig) {
        _accountConfigs.addElement(accountConfig);
    }
    
    public void removeAccountConfig(int index) {
        _accountConfigs.removeElementAt(index);
    }
    
    public void saveSettings() {
        try {
            RecordStore.deleteRecordStore("LogicMail_config");
        } catch (RecordStoreException exp) {
            // do nothing
        }
        
        RecordStore store = null;
        try {
            store = RecordStore.openRecordStore("LogicMail_config", true);
            byte[] buffer;
            buffer = _globalConfig.serialize();
            store.addRecord(buffer, 0, buffer.length);
        
            for(int i=0;i<_accountConfigs.size();i++) {
                buffer = ((AccountConfig)_accountConfigs.elementAt(i)).serialize();
                store.addRecord(buffer, 0, buffer.length);
            }
        } catch (RecordStoreException exp) {
            // do nothing
        }
        
        try {
            if(store != null) store.closeRecordStore();
        } catch (RecordStoreException exp) {
            // do nothing
        }
    }
    
    public void loadSettings() {
        _accountConfigs.removeAllElements();
        RecordStore store = null;
        try {
            store = RecordStore.openRecordStore("LogicMail_config", false);
            byte[] buffer;
            
            int records = store.getNumRecords();
            
            if(records >= 1) {
                buffer = store.getRecord(1);
                _globalConfig.deserialize(buffer);
            
                if(records > 1) {
                    for(int i=2;i<=store.getNumRecords();i++) {
                        buffer = store.getRecord(i);
                        _accountConfigs.addElement(new AccountConfig(buffer));
                    }
                }
            }
        } catch (RecordStoreException exp) {
            // do nothing
        }
        
        try {
            if(store != null) store.closeRecordStore();
        } catch (RecordStoreException exp) {
            // do nothing
        }
    }
}

