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
import org.logicprobe.LogicMail.util.EventListenerList;
import org.logicprobe.LogicMail.util.SerializableVector;

/**
 * Provide an interface to global and account-specific
 * configuration, along with a front-end to the Record Store
 */
public class MailSettings {
    private static MailSettings instance;
	private EventListenerList listenerList = new EventListenerList();
    private GlobalConfig globalConfig;
    private Vector identityConfigs;
    private Vector accountConfigs;
    private Vector outgoingConfigs;
    private DataStore configStore;
    private boolean isIdentityListDirty;
    private boolean isAccountListDirty;
    private boolean isOutgoingListDirty;
    
    private static String GLOBAL_CONFIG = "global_config";
    private static String IDENTITY_CONFIGS = "identity_configs";
    private static String ACCOUNT_CONFIGS = "account_configs";
    private static String OUTGOING_CONFIGS = "outgoing_configs";

    private MailSettings() {
        globalConfig = new GlobalConfig();
        identityConfigs = new Vector();
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
     * Gets the number of identity configurations
     */
    public int getNumIdentities() {
        return identityConfigs.size();
    }
    
    /**
     * Gets the identity configuration at the specified index
     */
    public IdentityConfig getIdentityConfig(int index) {
        return (IdentityConfig)identityConfigs.elementAt(index);
    }

    /**
     * Gets the identity configuration by unique ID.
     * Will return null if no identity is found matching that ID.
     */
    public IdentityConfig getIdentityConfigByUniqueId(long uniqueId) {
        int size = getNumIdentities();
        for(int i=0; i<size; i++) {
            IdentityConfig tmpConfig = getIdentityConfig(i);
            if(tmpConfig.getUniqueId() == uniqueId) {
                return tmpConfig;
            }
        }
        return null;
    }
    
    /**
     * Add a new identity configuration
     */
    public void addIdentityConfig(IdentityConfig identityConfig) {
        isIdentityListDirty = true;
        identityConfigs.addElement(identityConfig);
    }
    
    /**
     * Insert an identity configuration at a particular index.
     * 
     * @param identityConfig the identity configuration
     * @param index where to insert the identity configuration
     */
    public void insertIdentityConfigAt(IdentityConfig identityConfig, int index) {
        isIdentityListDirty = true;
    	identityConfigs.insertElementAt(identityConfig, index);
    }

    /**
     * Searches for the index of the identity configuration.
     * 
     * @param identityConfig the identity configuration
     * @return index of the configuration, or -1 if it is not found
     */
    public int indexOfIdentityConfig(IdentityConfig identityConfig) {
    	return identityConfigs.indexOf(identityConfig);
    }
    
    /**
     * Remove the identity configuration at the specified index
     */
    public void removeIdentityConfig(int index) {
        isIdentityListDirty = true;
        identityConfigs.removeElementAt(index);
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
        isAccountListDirty = true;
        accountConfigs.addElement(accountConfig);
    }
    
    /**
     * Insert an account configuration at a particular index.
     * 
     * @param accountConfig the account configuration
     * @param index where to insert the account configuration
     */
    public void insertAccountConfigAt(AccountConfig accountConfig, int index) {
        isAccountListDirty = true;
    	accountConfigs.insertElementAt(accountConfig, index);
    }
    
    /**
     * Searches for the index of the account configuration.
     * 
     * @param accountConfig the account configuration
     * @return index of the configuration, or -1 if it is not found
     */
    public int indexOfAccountConfig(AccountConfig accountConfig) {
    	return accountConfigs.indexOf(accountConfig);
    }
    
    /**
     * Remove the account configuration at the specified index
     */
    public void removeAccountConfig(int index) {
        isAccountListDirty = true;
        accountConfigs.removeElementAt(index);
    }

    /**
     * Gets whether the provided account configuration exists
     * within the mail settings.
     */
    public boolean containsAccountConfig(AccountConfig accountConfig) {
    	return accountConfigs.contains(accountConfig);
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
     * Gets the outgoing server configuration by unique ID.
     * Will return null if no config is found matching that ID.
     */
    public OutgoingConfig getOutgoingConfigByUniqueId(long uniqueId) {
        int size = getNumOutgoing();
        for(int i=0; i<size; i++) {
            OutgoingConfig tmpConfig = getOutgoingConfig(i);
            if(tmpConfig.getUniqueId() == uniqueId) {
                return tmpConfig;
            }
        }
        return null;
    }
    
    /**
     * Add a new outgoing server configuration
     */
    public void addOutgoingConfig(OutgoingConfig outgoingConfig) {
        isOutgoingListDirty = true;
        outgoingConfigs.addElement(outgoingConfig);
    }
    
    /**
     * Insert an outgoing server configuration at a particular index.
     * 
     * @param outgoingConfig the outgoing server configuration
     * @param index where to insert the outgoing server configuration
     */
    public void insertOutgoingConfigAt(OutgoingConfig outgoingConfig, int index) {
        isOutgoingListDirty = true;
    	outgoingConfigs.insertElementAt(outgoingConfig, index);
    }
    
    /**
     * Searches for the index of the outgoing server configuration.
     * 
     * @param outgoingConfig the outgoing server configuration
     * @return index of the configuration, or -1 if it is not found
     */
    public int indexOfOutgoingConfig(OutgoingConfig outgoingConfig) {
    	return outgoingConfigs.indexOf(outgoingConfig);
    }
    
    /**
     * Remove the outgoing server configuration at the specified index
     */
    public void removeOutgoingConfig(int index) {
        isOutgoingListDirty = true;
        outgoingConfigs.removeElementAt(index);
    }

    /**
     * Gets whether the provided outgoing configuration exists
     * within the mail settings.
     */
    public boolean containsOutgoingConfig(OutgoingConfig outgoingConfig) {
    	return outgoingConfigs.contains(outgoingConfig);
    }
    
    /**
     * Save the configurations to persistent storage.
     */
    public void saveSettings() {
        MailSettingsEvent e = new MailSettingsEvent(this);
        e.setGlobalChange(globalConfig.getChangeType());
        int listChangeType = 0;
        if(isIdentityListDirty) { listChangeType |= MailSettingsEvent.LIST_CHANGED_IDENTITY; }
        if(isAccountListDirty) { listChangeType |= MailSettingsEvent.LIST_CHANGED_ACCOUNT; }
        if(isOutgoingListDirty) { listChangeType |= MailSettingsEvent.LIST_CHANGED_OUTGOING; }
        e.setListChange(listChangeType);
        
        // Clean out any old configuration objects from the store
        removeExistingSettings(IDENTITY_CONFIGS);
        removeExistingSettings(ACCOUNT_CONFIGS);
        removeExistingSettings(OUTGOING_CONFIGS);
        
        SerializableVector identityConfigIds = new SerializableVector();
        SerializableVector accountConfigIds = new SerializableVector();
        SerializableVector outgoingConfigIds = new SerializableVector();
        
        configStore.putNamedObject(GLOBAL_CONFIG, globalConfig);
        configStore.putNamedObject(IDENTITY_CONFIGS, identityConfigIds);
        configStore.putNamedObject(ACCOUNT_CONFIGS, accountConfigIds);
        configStore.putNamedObject(OUTGOING_CONFIGS, outgoingConfigIds);
        
        int i;
        int size = identityConfigs.size();
        for(i=0; i<size; ++i) {
            IdentityConfig config = (IdentityConfig)identityConfigs.elementAt(i);
            identityConfigIds.addElement(new Long(config.getUniqueId()));
            configStore.putObject(config);
            e.setConfigChange(config, config.getChangeType());
        }

        size = accountConfigs.size();
        for(i=0; i<size; ++i) {
            AccountConfig config = (AccountConfig)accountConfigs.elementAt(i);
            accountConfigIds.addElement(new Long(config.getUniqueId()));
            configStore.putObject(config);
            e.setConfigChange(config, config.getChangeType());
        }

        size = outgoingConfigs.size();
        for(i=0; i<size; ++i) {
            OutgoingConfig config = (OutgoingConfig)outgoingConfigs.elementAt(i);
            outgoingConfigIds.addElement(new Long(config.getUniqueId()));
            configStore.putObject(config);
            e.setConfigChange(config, config.getChangeType());
        }

        configStore.save();
        fireMailSettingsSaved(e);
        
        isIdentityListDirty = false;
        isAccountListDirty = false;
        isOutgoingListDirty = false;
    }
        
    private void removeExistingSettings(String key) {
        Object object = configStore.getNamedObject(key);
        if(object instanceof SerializableVector) {
            SerializableVector existingItems = (SerializableVector)object;
            for(int i = existingItems.size() - 1; i >= 0; --i) {
                configStore.removeObject(((Long)existingItems.elementAt(i)).longValue());
            }
        }
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
        
        identityConfigs.removeAllElements();
        accountConfigs.removeAllElements();
        outgoingConfigs.removeAllElements();
        SerializableVector configIds;
        int size;
        
        loadedObj = configStore.getNamedObject(IDENTITY_CONFIGS);
        if(loadedObj instanceof SerializableVector) {
            configIds = (SerializableVector)loadedObj;
            size = configIds.size();
            for(int i=0; i<size; ++i) {
            	loadedObj = configIds.elementAt(i);
            	if(loadedObj instanceof Long) {
            		loadedObj = configStore.getObject(((Long)loadedObj).longValue());
	                if(loadedObj instanceof IdentityConfig) {
	                    identityConfigs.addElement(loadedObj);
	                }
            	}
            }
        }

        loadedObj = configStore.getNamedObject(ACCOUNT_CONFIGS);
        if(loadedObj instanceof SerializableVector) {
            configIds = (SerializableVector)loadedObj;
            size = configIds.size();
            for(int i=0; i<size; ++i) {
            	loadedObj = configIds.elementAt(i);
            	if(loadedObj instanceof Long) {
	                loadedObj = configStore.getObject(((Long)loadedObj).longValue());
	                if(loadedObj instanceof AccountConfig) {
	                    accountConfigs.addElement(loadedObj);
	                }
            	}
            }
        }

        loadedObj = configStore.getNamedObject(OUTGOING_CONFIGS);
        if(loadedObj instanceof SerializableVector) {
            configIds = (SerializableVector)loadedObj;
            size = configIds.size();
            for(int i=0; i<size; ++i) {
            	loadedObj = configIds.elementAt(i);
            	if(loadedObj instanceof Long) {
	                loadedObj = configStore.getObject(((Long)loadedObj).longValue());
	                if(loadedObj instanceof OutgoingConfig) {
	                    outgoingConfigs.addElement(loadedObj);
	                }
            	}
            }
        }
        isIdentityListDirty = false;
        isAccountListDirty = false;
        isOutgoingListDirty = false;
    }
    
	/**
     * Adds a <tt>MailSettingsListener</tt> to the mail settings container.
     * 
     * @param l The <tt>MailSettingsListener</tt> to be added.
     */
    public void addMailSettingsListener(MailSettingsListener l) {
        listenerList.add(MailSettingsListener.class, l);
    }

    /**
     * Removes a <tt>MailSettingsListener</tt> from the mail settings container.
     * 
     * @param l The <tt>MailSettingsListener</tt> to be removed.
     */
    public void removeMailSettingsListener(MailSettingsListener l) {
        listenerList.remove(MailSettingsListener.class, l);
    }
    
    /**
     * Returns an array of all <tt>MailSettingsListener</tt>s
     * that have been added to the mail settings container.
     * 
     * @return All the <tt>MailSettingsListener</tt>s that have been added,
     * or an empty array if no listeners have been added.
     */
    public MailSettingsListener[] getMailSettingsListener() {
        return (MailSettingsListener[])listenerList.getListeners(MailSettingsListener.class);
    }
    
    /**
     * Notifies all registered <tt>MailSettingsListener</tt>s that
     * the mail settings have been saved. 
     */
    protected void fireMailSettingsSaved(MailSettingsEvent e) {
        Object[] listeners = listenerList.getListeners(MailSettingsListener.class);
        for(int i=0; i<listeners.length; i++) {
            ((MailSettingsListener)listeners[i]).mailSettingsSaved(e);
        }
    }
}
