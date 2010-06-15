/*-
 * Copyright (c) 2007, Derek Konigsberg
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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.logicprobe.LogicMail.util.Serializable;
import org.logicprobe.LogicMail.util.SerializableHashtable;
import org.logicprobe.LogicMail.util.UniqueIdGenerator;


/**
 * Abstract parent class for all configuration objects that represent
 * network connections.
 */
public abstract class ConnectionConfig implements Serializable {
    private long uniqueId;
    protected int changeType;
    private String acctName;
    private String serverName;
    private int serverSecurity;
    private int serverPort;
    private int transportType;
    private boolean enableWiFi;

    /** Configuration name change. */
    public static final int CHANGE_TYPE_NAME = 0x01;
    /** Server and network settings. */
    public static final int CHANGE_TYPE_CONNECTION = 0x02;
    
    /** Use the transport specified in the global configuration */
    public static final int TRANSPORT_GLOBAL = 0;
    /** Use the Direct TCP transport */
    public static final int TRANSPORT_DIRECT_TCP = 10;
    /** Use the MDS transport */
    public static final int TRANSPORT_MDS = 20;
    /** Use the WAP 2.0 transport */
    public static final int TRANSPORT_WAP2 = 30;
    /** Use only WiFi, disabling all other transport types */
    public static final int TRANSPORT_WIFI_ONLY = 99;
    /** Automatically select the transport type */
    public static final int TRANSPORT_AUTO = 9999;

    /** Connection is not encrypted */
    public static final int SECURITY_NONE = 0;
    /** Connection should use TLS if available */
    public static final int SECURITY_TLS_IF_AVAILABLE = 1;
    /** Connection should always use TLS */
    public static final int SECURITY_TLS = 2;
    /** Connection should use SSL */
    public static final int SECURITY_SSL = 3;
    
    /**
     * Instantiates a new connection configuration with defaults.
     */
    public ConnectionConfig() {
        setDefaults();
    }
    
    /**
     * Instantiates a new connection configuration from serialized data.
     * 
     * @param input The input stream to deserialize from
     */
    public ConnectionConfig(DataInput input) {
        try {
            deserialize(input);
        } catch (IOException ex) {
            setDefaults();
        }
    }

    /**
     * Sets the default values for all fields.
     * Subclasses should override this method to add default
     * values for all of their fields.
     */
    protected void setDefaults() {
        uniqueId = UniqueIdGenerator.getInstance().getUniqueId();
        acctName = "";
        serverName = "";
        serverSecurity = SECURITY_NONE;
        serverPort = 110;
        transportType = TRANSPORT_GLOBAL;
        enableWiFi = false;
        changeType = 0;
    }

    /**
     * Gets the account name.
     * 
     * @return The account name
     */
    public String getAcctName() {
        return acctName;
    }
    
    /**
     * Sets the account name.
     * 
     * @param acctName The new account name
     */
    public void setAcctName(String acctName) {
        if(!this.acctName.equals(acctName)) {
            this.acctName = acctName;
            changeType |= CHANGE_TYPE_NAME;
        }
    }
    
    /**
     * Gets the server name.
     * 
     * @return The server name
     */
    public String getServerName() {
        return serverName;
    }
    
    /**
     * Sets the server name.
     * 
     * @param serverName The new server name
     */
    public void setServerName(String serverName) {
        if(!this.serverName.equals(serverName)) {
            this.serverName = serverName;
            changeType |= CHANGE_TYPE_CONNECTION;
        }
    }

    /**
     * Gets the server connection security mode.
     * 
     * @return the server connection security mode
     */
    public int getServerSecurity() {
    	return serverSecurity;
    }
    
    /**
     * Sets the server connection security mode.
     * 
     * @param serverSecurity the new server connection security mode
     */
    public void setServerSecurity(int serverSecurity) {
        if(this.serverSecurity != serverSecurity) {
            this.serverSecurity = serverSecurity;
            changeType |= CHANGE_TYPE_CONNECTION;
        }
    }
    
    /**
     * Gets the server connection port.
     * 
     * @return The server port
     */
    public int getServerPort() {
        return serverPort;
    }

    /**
     * Sets the server connection port.
     * 
     * @param serverPort The new server port
     */
    public void setServerPort(int serverPort) {
        if(this.serverPort != serverPort) {
            this.serverPort = serverPort;
            changeType |= CHANGE_TYPE_CONNECTION;
        }
    }

    /**
     * Gets the preferred network transport type.
     * 
     * @return the preferred network transport type
     */
    public int getTransportType() {
        return transportType;
    }
    
    /**
     * Sets the preferred network transport type.
     * 
     * @param transportType the new preferred network transport type
     */
    public void setTransportType(int transportType) {
        if(this.transportType != transportType) {
            this.transportType = transportType;
            changeType |= CHANGE_TYPE_CONNECTION;
        }
    }
    
    /**
     * Gets whether to use WiFi if available.
     * 
     * @return whether to use WiFi if available
     */
    public boolean getEnableWiFi() {
        return enableWiFi;
    }
    
    /**
     * Sets whether to use WiFi if available.
     * 
     * @param enableWiFi whether to use WiFi if available
     */
    public void setEnableWiFi(boolean enableWiFi) {
        if(this.enableWiFi != enableWiFi) {
            this.enableWiFi = enableWiFi;
            changeType |= CHANGE_TYPE_CONNECTION;
        }
    }
    
    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.util.Serializable#serialize(java.io.DataOutput)
     */
    public final void serialize(DataOutput output) throws IOException {
        output.writeLong(uniqueId);
        SerializableHashtable table = new SerializableHashtable();
        writeConfigItems(table);
        table.serialize(output);
        changeType = 0;
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.util.Serializable#deserialize(java.io.DataInput)
     */
    public final void deserialize(DataInput input) throws IOException {
        setDefaults();
        uniqueId = input.readLong();
        SerializableHashtable table = new SerializableHashtable();
        table.deserialize(input);
        readConfigItems(table);
        changeType = 0;
    }
    
    /**
     * Write configuration items to the hash table that
     * will be serialized.  Subclasses should override this method
     * to support additional configuration items.
     * 
     * @param table The configuration item hash table
     */
    protected void writeConfigItems(SerializableHashtable table) {
        table.put("account_acctName", acctName);
        table.put("account_serverName", serverName);
        table.put("account_serverSecurity", new Integer(serverSecurity));
        table.put("account_serverPort", new Integer(serverPort));
        table.put("account_transportType", new Integer(transportType));
        table.put("account_enableWiFi", new Boolean(enableWiFi));
    }
    
    /**
     * Read configuration items from the hash table that
     * has been deserialized.  Subclasses should override this method
     * to support additional configuration items.
     * 
     * @param table The configuration item hash table
     */
    protected void readConfigItems(SerializableHashtable table) {
        Object value;

        value = table.get("account_acctName");
        if(value instanceof String) {
            acctName = (String)value;
        }
        value = table.get("account_serverName");
        if(value instanceof String) {
            serverName = (String)value;
        }
        value = table.get("account_serverSecurity");
        if(value instanceof Integer) {
        	serverSecurity = ((Integer)value).intValue();
        }
        value = table.get("account_serverPort");
        if(value instanceof Integer) {
            serverPort = ((Integer)value).intValue();
        }
        value = table.get("account_transportType");
        if(value instanceof Integer) {
            transportType = ((Integer)value).intValue();
        }
        value = table.get("account_enableWiFi");
        if(value instanceof Boolean) {
            enableWiFi = ((Boolean)value).booleanValue();
        }
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.util.Serializable#getUniqueId()
     */
    public long getUniqueId() {
        return uniqueId;
    }

    /**
     * Checks if this object has changed since it was last saved.
     * 
     * @return the change type, if applicable
     */
    public int getChangeType() {
        return changeType;
    }
}
