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

import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import org.logicprobe.LogicMail.util.Serializable;
import org.logicprobe.LogicMail.util.SerializableHashtable;

/**
 * Store account configuration for LogicMail
 */
public class AccountConfig implements Serializable {
    final public static int TYPE_POP = 0;
    final public static int TYPE_IMAP = 1;
    
    private String acctName;
    private String serverName;
    private int serverType;
    private boolean serverSSL;
    private String serverUser;
    private String serverPass;
    private int serverPort;
    private boolean deviceSide;
    private String smtpServerName;
    private int smtpServerPort;
    private boolean smtpServerSSL;
    private String smtpFromAddress;
    private int smtpUseAuth;
    private String smtpUser;
    private String smtpPass;

    public AccountConfig() {
            setDefaults();
    }
    
    public AccountConfig(DataInputStream input) {
        try {
            deserialize(input);
        } catch (IOException ex) {
            setDefaults();
        }
    }

    private void setDefaults() {
        acctName = "";
        serverName = "";
        serverType = TYPE_POP;
        serverSSL = false;
        serverUser = "";
        serverPass = "";
        serverPort = 110;
        deviceSide = false;
        smtpServerName = "";
        smtpServerPort = 25;
        smtpServerSSL = false;
        smtpUseAuth = 0;
        smtpUser = "";
        smtpPass = "";
        smtpFromAddress = "";
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
    
    public int getServerType() {
        return serverType;
    }
    
    public void setServerType(int serverType) {
        this.serverType = serverType;
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
    
    public String getServerUser() {
        return serverUser;
    }
    
    public void setServerUser(String serverUser) {
        this.serverUser = serverUser;
    }
    
    public String getServerPass() {
        return serverPass;
    }
    
    public void setServerPass(String serverPass) {
        this.serverPass = serverPass;
    }

    public boolean getDeviceSide() {
        return deviceSide;
    }
    
    public void setDeviceSide(boolean deviceSide) {
        this.deviceSide = deviceSide;
    }

    public String getSmtpServerName() {
        return smtpServerName;
    }

    public void setSmtpServerName(String smtpServerName) {
        this.smtpServerName = smtpServerName;
    }

    public int getSmtpUseAuth() {
        return smtpUseAuth;
    }

    public void setSmtpUseAuth(int smtpUseAuth) {
        this.smtpUseAuth = smtpUseAuth;
    }

    public String getSmtpUser() {
        return smtpUser;
    }

    public void setSmtpUser(String smtpUser) {
        this.smtpUser = smtpUser;
    }

    public String getSmtpPass() {
        return smtpPass;
    }

    public void setSmtpPass(String smtpPass) {
        this.smtpPass = smtpPass;
    }

    public int getSmtpServerPort() {
        return smtpServerPort;
    }

    public void setSmtpServerPort(int smtpServerPort) {
        this.smtpServerPort = smtpServerPort;
    }

    public boolean getSmtpServerSSL() {
        return smtpServerSSL;
    }

    public void setSmtpServerSSL(boolean smtpServerSSL) {
        this.smtpServerSSL = smtpServerSSL;
    }

    public String getSmtpFromAddress() {
        return smtpFromAddress;
    }

    public void setSmtpFromAddress(String smtpFromAddress) {
        this.smtpFromAddress = smtpFromAddress;
    }

    public void serialize(DataOutputStream output) throws IOException {
        SerializableHashtable table = new SerializableHashtable();
        
        table.put("account_acctName", acctName);
        table.put("account_serverName", serverName);
        table.put("account_serverType", new Integer(serverType));
        table.put("account_serverSSL", new Boolean(serverSSL));
        table.put("account_serverUser", serverUser);
        table.put("account_serverPass", serverPass);
        table.put("account_serverPort", new Integer(serverPort));
        table.put("account_deviceSide", new Boolean(deviceSide));
        table.put("account_smtpServerName", smtpServerName);
        table.put("account_smtpServerPort", new Integer(smtpServerPort));
        table.put("account_smtpServerSSL", new Boolean(smtpServerSSL));
        table.put("account_smtpUseAuth", new Integer(smtpUseAuth));
        table.put("account_smtpUser", smtpUser);
        table.put("account_smtpPass", smtpPass);
        table.put("account_smtpFromAddress", smtpFromAddress);
        
        table.serialize(output);        
    }

    public void deserialize(DataInputStream input) throws IOException {
        setDefaults();
        SerializableHashtable table = new SerializableHashtable();
        table.deserialize(input);
        Object value;

        value = table.get("account_acctName");
        if(value != null && value instanceof String) {
            acctName = (String)value;
        }
        value = table.get("account_serverName");
        if(value != null && value instanceof String) {
            serverName = (String)value;
        }
        value = table.get("account_serverType");
        if(value != null && value instanceof Integer) {
            serverType = ((Integer)value).intValue();
        }
        value = table.get("account_serverSSL");
        if(value != null && value instanceof Boolean) {
            serverSSL = ((Boolean)value).booleanValue();
        }
        value = table.get("account_serverUser");
        if(value != null && value instanceof String) {
            serverUser = (String)value;
        }
        value = table.get("account_serverPass");
        if(value != null && value instanceof String) {
            serverPass = (String)value;
        }
        value = table.get("account_serverPort");
        if(value != null && value instanceof Integer) {
            serverPort = ((Integer)value).intValue();
        }
        value = table.get("account_deviceSide");
        if(value != null && value instanceof Boolean) {
            deviceSide = ((Boolean)value).booleanValue();
        }
        value = table.get("account_smtpServerName");
        if(value != null && value instanceof String) {
            smtpServerName = (String)value;
        }
        value = table.get("account_smtpServerPort");
        if(value != null && value instanceof Integer) {
            smtpServerPort = ((Integer)value).intValue();
        }
        value = table.get("account_smtpServerSSL");
        if(value != null && value instanceof Boolean) {
            smtpServerSSL = ((Boolean)value).booleanValue();
        }
        value = table.get("account_smtpUseAuth");
        if(value != null && value instanceof Integer) {
            smtpUseAuth = ((Integer)value).intValue();
        }
        value = table.get("account_smtpUser");
        if(value != null && value instanceof String) {
            smtpUser = (String)value;
        }
        value = table.get("account_smtpPass");
        if(value != null && value instanceof String) {
            smtpPass = (String)value;
        }
        value = table.get("account_smtpFromAddress");
        if(value != null && value instanceof String) {
            smtpFromAddress = (String)value;
        }
    }
}

