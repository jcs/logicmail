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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;
import javax.microedition.rms.*;
import org.logicprobe.LogicMail.util.Serializable;

/**
 * Provide an interface to global and account-specific
 * configuration, along with a front-end to the Record Store
 */
public class MailSettings {
    private static MailSettings instance;
    private GlobalConfig globalConfig;
    private Vector accountConfigs;
    private Vector outgoingConfigs;
    
    private MailSettings() {
        globalConfig = new GlobalConfig();
        accountConfigs = new Vector();
        outgoingConfigs = new Vector();
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
        try {
            RecordStore.deleteRecordStore("LogicMail_config");
        } catch (RecordStoreException exp) {
            // do nothing
        }
        
        RecordStore store = null;
        try {
            store = RecordStore.openRecordStore("LogicMail_config", true);
            byte[] byteArray;
            
            byteArray = serializeClass(globalConfig);
            store.addRecord(byteArray, 0, byteArray.length);
            
            int size = accountConfigs.size();
            for(int i=0;i<size;i++) {
                byteArray = serializeClass((Serializable)accountConfigs.elementAt(i));
                store.addRecord(byteArray, 0, byteArray.length);
            }
            size = outgoingConfigs.size();
            for(int i=0;i<size;i++) {
                byteArray = serializeClass((Serializable)outgoingConfigs.elementAt(i));
                store.addRecord(byteArray, 0, byteArray.length);
            }
        } catch (RecordStoreException exp) {
            // do nothing
        }
        
        try {
            if(store != null) {
                store.closeRecordStore();
            }
        } catch (RecordStoreException exp) {
            // do nothing
        }
    }
    
    /**
     * Utility method to serialize any serializable class.
     * The returned buffer consists of the fully qualified class name,
     * followed by the serialized contents of the class.
     */
    private byte[] serializeClass(Serializable input) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(buffer);
        try {
            output.writeUTF(input.getClass().getName());
            input.serialize(output);
        } catch (IOException ex) {
            // do nothing
        }
        return buffer.toByteArray();
    }
    
    /**
     * Load the configurations from persistent storage.
     * This method should only be called once in the lifetime of
     * the application, preferably at the very start.
     */
    public void loadSettings() {
        accountConfigs.removeAllElements();
        outgoingConfigs.removeAllElements();
        RecordStore store = null;
        Object deserializedObject;
        try {
            store = RecordStore.openRecordStore("LogicMail_config", false);
            
            int records = store.getNumRecords();
            
            if(records >= 1) {
                deserializedObject = deserializeClass(store.getRecord(1));
                if(deserializedObject instanceof GlobalConfig) {
                    globalConfig = (GlobalConfig)deserializedObject;
                }
                else {
                    globalConfig = new GlobalConfig();
                }
            
                if(records > 1) {
                    for(int i=2;i<=store.getNumRecords();i++) {
                        deserializedObject = deserializeClass(store.getRecord(i));
                        if(deserializedObject instanceof AccountConfig)
                        {
                            accountConfigs.addElement(deserializedObject);
                        }
                        else if(deserializedObject instanceof OutgoingConfig)
                        {
                            outgoingConfigs.addElement(deserializedObject);
                        }
                    }
                }
            }
        } catch (RecordStoreException exp) {
            // do nothing
        }
        
        try {
            if(store != null) {
                store.closeRecordStore();
            }
        } catch (RecordStoreException exp) {
            // do nothing
        }
    }
    
    /**
     * Utility method to deserialize any class.
     * First, the fully qualified class name is read from the
     * input stream.  Then, if a class matching that name exists,
     * it is instantiated.  Finally, if that class implements the
     * Serializable interface, the input stream is passed on
     * to its deserialize method.
     */
    private Object deserializeClass(byte[] data) {
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(data));
        Object result = null;
        try {
            String classType = input.readUTF();
            result = Class.forName(classType).newInstance();
            if(result instanceof Serializable) {
                ((Serializable)result).deserialize(input);
            }
        } catch (IOException ex) {
            result = null;
        } catch (ClassNotFoundException ex) {
            result = null;
        } catch (InstantiationException ex) {
            result = null;
        } catch (IllegalAccessException ex) {
            result = null;
        }
        return result;
    }
}
