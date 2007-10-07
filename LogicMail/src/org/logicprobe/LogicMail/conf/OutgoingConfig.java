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
import org.logicprobe.LogicMail.util.SerializableHashtable;

/**
 * Store account configuration for outgoing (SMTP) connections.
 */
public class OutgoingConfig extends ConnectionConfig {
    private String fromAddress;
    private int useAuth;
    private String serverUser;
    private String serverPass;
    
    /** Creates a new instance of OutgoingConfig */
    public OutgoingConfig() {
        super();
    }
    
    public OutgoingConfig(DataInputStream input) {
        super(input);
    }

    protected void setDefaults() {
        super.setDefaults();
        setServerPort(25);
    }

    public String toString() {
        String text = getAcctName().concat(" (SMTP)");
        return text;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public int getUseAuth() {
        return useAuth;
    }

    public void setUseAuth(int useAuth) {
        this.useAuth = useAuth;
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
    
    public void writeConfigItems(SerializableHashtable table) {
        super.writeConfigItems(table);
        table.put("account_smtpUseAuth", new Integer(useAuth));
        table.put("account_smtpUser", serverUser);
        table.put("account_smtpPass", serverPass);
        table.put("account_smtpFromAddress", fromAddress);
    }
    
    public void readConfigItems(SerializableHashtable table) {
        super.readConfigItems(table);
        Object value;
        
        value = table.get("account_smtpUseAuth");
        if(value != null && value instanceof Integer) {
            useAuth = ((Integer)value).intValue();
        }
        value = table.get("account_smtpUser");
        if(value != null && value instanceof String) {
            serverUser = (String)value;
        }
        value = table.get("account_smtpPass");
        if(value != null && value instanceof String) {
            serverPass = (String)value;
        }
        value = table.get("account_smtpFromAddress");
        if(value != null && value instanceof String) {
            fromAddress = (String)value;
        }
    }    
}
