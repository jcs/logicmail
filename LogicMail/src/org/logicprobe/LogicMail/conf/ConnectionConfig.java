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
 * Store connection configuration for LogicMail
 */
public class ConnectionConfig implements Serializable {
    private long uniqueId;
    private String acctName;
    private String serverName;
    private boolean serverSSL;
    private int serverPort;
    private boolean deviceSide;
    
    public ConnectionConfig() {
        setDefaults();
    }
    
    public ConnectionConfig(DataInputStream input) {
        try {
            deserialize(input);
        } catch (IOException ex) {
            setDefaults();
        }
    }

    protected void setDefaults() {
        uniqueId = UniqueIdGenerator.getInstance().getUniqueId();
        acctName = "";
        serverName = "";
        serverSSL = false;
        serverPort = 110;
        deviceSide = false;
    }

    public String getAcctName() {
        return acctName;
    }
    
    public void setAcctName(String acctName) {
        this.acctName = acctName;
    }
    
    public String getServerName() {
        return serverName;
    }
    
    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public boolean getServerSSL() {
        return serverSSL;
    }
    
    public void setServerSSL(boolean serverSSL) {
        this.serverSSL = serverSSL;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public boolean getDeviceSide() {
        return deviceSide;
    }
    
    public void setDeviceSide(boolean deviceSide) {
        this.deviceSide = deviceSide;
    }

    final public void serialize(DataOutputStream output) throws IOException {
        output.writeLong(uniqueId);
        SerializableHashtable table = new SerializableHashtable();
        writeConfigItems(table);
        table.serialize(output);
    }

    final public void deserialize(DataInputStream input) throws IOException {
        setDefaults();
        uniqueId = input.readLong();
        SerializableHashtable table = new SerializableHashtable();
        table.deserialize(input);
        readConfigItems(table);
    }
    
    /**
     * Write configuration items to the hash table that
     * will be serialized.  This method should be overriden
     * by subclasses to support additional configuration items.
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
     * has been deserialized.  This method should be overriden
     * by subclasses to support additional configuration items.
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

    public long getUniqueId() {
        return uniqueId;
    }
}
