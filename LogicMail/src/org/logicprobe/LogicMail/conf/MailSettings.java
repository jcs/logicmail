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

package org.logicprobe.LogicMail.conf;

import java.util.Vector;
import org.logicprobe.LogicMail.util.DataStore;
import org.logicprobe.LogicMail.util.DataStoreFactory;
import org.logicprobe.LogicMail.util.SerializableVector;

/**
 * Provide an interface to global and account-specific
 * configuration, along with a front-end to the Record Store
 */
public class MailSettings {
    private static MailSettings instance;
    private GlobalConfig globalConfig;
    private Vector accountConfigs;
    private Vector outgoingConfigs;
    private DataStore configStore;
    
    private static String GLOBAL_CONFIG = "global_config";
    private static String ACCOUNT_CONFIGS = "account_configs";
    private static String OUTGOING_CONFIGS = "outgoing_configs";

    private MailSettings() {
        globalConfig = new GlobalConfig();
        accountConfigs = new Vector();
        outgoingConfigs = new Vector();
        configStore = DataStoreFactory.getConfigurationStore();
    }
    
    /**
     * Gets the mail settings instance.
     * @return Instance
     */
    public static synchronized MailSettings getInstance() {
        if(instance == null) {
            instance = new MailSettings();
        }
        return instance;
    }

    /**
     * Gets the global configuration
     */
    public GlobalConfig getGlobalConfig() {
        return globalConfig;
    }

    /**
     * Gets the number of account configurations
     */
    public int getNumAccounts() {
        return accountConfigs.size();
    }
    
    /**
     * Gets the account configuration at the specified index
     */
    public AccountConfig getAccountConfig(int index) {
        return (AccountConfig)accountConfigs.elementAt(index);
    }
    
    /**
     * Add a new account configuration
     */
    public void addAccountConfig(AccountConfig accountConfig) {
        accountConfigs.addElement(accountConfig);
    }
    
    /**
     * Remove the account configuration at the specified index
     */
    public void removeAccountConfig(int index) {
        accountConfigs.removeElementAt(index);
    }

    /**
     * Gets the number of outgoing server configurations
     */
    public int getNumOutgoing() {
        return outgoingConfigs.size();
    }
    
    /**
     * Gets the outgoing server configuration at the specified index
     */
    public OutgoingConfig getOutgoingConfig(int index) {
        return (OutgoingConfig)outgoingConfigs.elementAt(index);
    }
    
    /**
     * Add a new outgoing server configuration
     */
    public void addOutgoingConfig(OutgoingConfig outgoingConfig) {
        outgoingConfigs.addElement(outgoingConfig);
    }
    
    /**
     * Remove the outgoing server configuration at the specified index
     */
    public void removeOutgoingConfig(int index) {
        outgoingConfigs.removeElementAt(index);
    }
    
    /**
     * Save the configurations to persistent storage.
     */
    public void saveSettings() {
        SerializableVector accountConfigIds = new SerializableVector();
        SerializableVector outgoingConfigIds = new SerializableVector();

        configStore.putNamedObject(GLOBAL_CONFIG, globalConfig);
        configStore.putNamedObject(ACCOUNT_CONFIGS, accountConfigIds);
        configStore.putNamedObject(OUTGOING_CONFIGS, outgoingConfigIds);
        
        int i;
        int size = accountConfigs.size();
        for(i=0; i<size; ++i) {
            AccountConfig config = (AccountConfig)accountConfigs.elementAt(i);
            accountConfigIds.addElement(new Long(config.getUniqueId()));
            configStore.putObject(config);
        }

        size = outgoingConfigs.size();
        for(i=0; i<size; ++i) {
            OutgoingConfig config = (OutgoingConfig)outgoingConfigs.elementAt(i);
            outgoingConfigIds.addElement(new Long(config.getUniqueId()));
            configStore.putObject(config);
        }

        configStore.save();
    }
        
    /**
     * Load the configurations from persistent storage.
     * This method should only be called once in the lifetime of
     * the application, preferably at the very start.
     */
    public void loadSettings() {
        configStore.load();
        
        Object loadedObj;
        loadedObj = configStore.getNamedObject(GLOBAL_CONFIG);
        if(loadedObj instanceof GlobalConfig) {
            globalConfig = (GlobalConfig)loadedObj;
        }
        else {
            globalConfig = new GlobalConfig();
        }
        
        accountConfigs.removeAllElements();
        outgoingConfigs.removeAllElements();
        SerializableVector configIds;
        int size;
        
        loadedObj = configStore.getNamedObject(ACCOUNT_CONFIGS);
        if(loadedObj instanceof SerializableVector) {
            configIds = (SerializableVector)loadedObj;
            size = configIds.size();
            for(int i=0; i<size; ++i) {
                loadedObj = configStore.getObject(((Long)configIds.elementAt(i)).longValue());
                if(loadedObj instanceof AccountConfig) {
                    accountConfigs.addElement(loadedObj);
                }
            }
        }

        loadedObj = configStore.getNamedObject(OUTGOING_CONFIGS);
        if(loadedObj instanceof SerializableVector) {
            configIds = (SerializableVector)loadedObj;
            size = configIds.size();
            for(int i=0; i<size; ++i) {
                loadedObj = configStore.getObject(((Long)configIds.elementAt(i)).longValue());
                if(loadedObj instanceof OutgoingConfig) {
                    outgoingConfigs.addElement(loadedObj);
                }
            }
        }
    }
}
