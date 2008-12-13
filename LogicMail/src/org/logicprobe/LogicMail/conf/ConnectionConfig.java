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

import java.io.DataInputStream;
import java.io.DataOutputStream;
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
    private String acctName;
    private String serverName;
    private boolean serverSSL;
    private int serverPort;
    private boolean deviceSide;
    
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
    public ConnectionConfig(DataInputStream input) {
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
        serverSSL = false;
        serverPort = 110;
        deviceSide = false;
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
        this.acctName = acctName;
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
        this.serverName = serverName;
    }

    /**
     * Gets whether the server connection should use SSL.
     * 
     * @return The SSL mode
     */
    public boolean getServerSSL() {
        return serverSSL;
    }
    
    /**
     * Sets whether the server connection should use SSL.
     * 
     * @param serverSSL The new SSL mode
     */
    public void setServerSSL(boolean serverSSL) {
        this.serverSSL = serverSSL;
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
        this.serverPort = serverPort;
    }

    /**
     * Gets whether the connection is device side.
     * 
     * @return The device side mode
     */
    public boolean getDeviceSide() {
        return deviceSide;
    }
    
    /**
     * Sets whether the connection is device side.
     * 
     * @param deviceSide The new device side mode
     */
    public void setDeviceSide(boolean deviceSide) {
        this.deviceSide = deviceSide;
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.util.Serializable#serialize(java.io.DataOutputStream)
     */
    final public void serialize(DataOutputStream output) throws IOException {
        output.writeLong(uniqueId);
        SerializableHashtable table = new SerializableHashtable();
        writeConfigItems(table);
        table.serialize(output);
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.util.Serializable#deserialize(java.io.DataInputStream)
     */
    final public void deserialize(DataInputStream input) throws IOException {
        setDefaults();
        uniqueId = input.readLong();
        SerializableHashtable table = new SerializableHashtable();
        table.deserialize(input);
        readConfigItems(table);
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
        table.put("account_serverSSL", new Boolean(serverSSL));
        table.put("account_serverPort", new Integer(serverPort));
        table.put("account_deviceSide", new Boolean(deviceSide));
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
        if(value != null && value instanceof String) {
            acctName = (String)value;
        }
        value = table.get("account_serverName");
        if(value != null && value instanceof String) {
            serverName = (String)value;
        }
        value = table.get("account_serverSSL");
        if(value != null && value instanceof Boolean) {
            serverSSL = ((Boolean)value).booleanValue();
        }
        value = table.get("account_serverPort");
        if(value != null && value instanceof Integer) {
            serverPort = ((Integer)value).intValue();
        }
        value = table.get("account_deviceSide");
        if(value != null && value instanceof Boolean) {
            deviceSide = ((Boolean)value).booleanValue();
        }
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.util.Serializable#getUniqueId()
     */
    public long getUniqueId() {
        return uniqueId;
    }
}
