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

import java.io.DataInputStream;
import org.logicprobe.LogicMail.util.SerializableHashtable;

/**
 * Store account configuration for LogicMail
 */
public class AccountConfig extends ConnectionConfig {
    final public static int TYPE_POP = 0;
    final public static int TYPE_IMAP = 1;
    
    private int serverType;
    private String serverUser;
    private String serverPass;
    private String smtpServerName;
    private int smtpServerPort;
    private boolean smtpServerSSL;
    private String smtpFromAddress;
    private int smtpUseAuth;
    private String smtpUser;
    private String smtpPass;

    public AccountConfig() {
        super();
    }
    
    public AccountConfig(DataInputStream input) {
        super(input);
    }

    protected void setDefaults() {
        super.setDefaults();
        serverType = TYPE_POP;
        serverUser = "";
        serverPass = "";
        setServerPort(110);
        smtpServerName = "";
        smtpServerPort = 25;
        smtpServerSSL = false;
        smtpUseAuth = 0;
        smtpUser = "";
        smtpPass = "";
        smtpFromAddress = "";
    }

    public String toString() {
        String text = getAcctName();
        if(serverType == TYPE_POP) {
            text = text.concat(" (POP)");
        }
        else if(serverType == TYPE_IMAP) {
            text = text.concat(" (IMAP)");
        }
        
        return text;
    }

    public int getServerType() {
        return serverType;
    }
    
    public void setServerType(int serverType) {
        this.serverType = serverType;
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

    public void writeConfigItems(SerializableHashtable table) {
        super.writeConfigItems(table);
        table.put("account_serverType", new Integer(serverType));
        table.put("account_serverUser", serverUser);
        table.put("account_serverPass", serverPass);
        table.put("account_smtpServerName", smtpServerName);
        table.put("account_smtpServerPort", new Integer(smtpServerPort));
        table.put("account_smtpServerSSL", new Boolean(smtpServerSSL));
        table.put("account_smtpUseAuth", new Integer(smtpUseAuth));
        table.put("account_smtpUser", smtpUser);
        table.put("account_smtpPass", smtpPass);
        table.put("account_smtpFromAddress", smtpFromAddress);
    }

    public void readConfigItems(SerializableHashtable table) {
        super.readConfigItems(table);
        Object value;

        value = table.get("account_serverType");
        if(value != null && value instanceof Integer) {
            serverType = ((Integer)value).intValue();
        }
        value = table.get("account_serverUser");
        if(value != null && value instanceof String) {
            serverUser = (String)value;
        }
        value = table.get("account_serverPass");
        if(value != null && value instanceof String) {
            serverPass = (String)value;
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

