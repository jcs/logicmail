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

import org.logicprobe.LogicMail.mail.smtp.SmtpProtocol;
import org.logicprobe.LogicMail.util.SerializableHashtable;

/**
 * Configuration object to store settings for
 * outgoing (SMTP) connections.
 */
public class OutgoingConfig extends ConnectionConfig {
    private int useAuth;
    private String serverUser;
    private String serverPass;
    
    /**
     * Instantiates a new connection configuration with defaults.
     */
    public OutgoingConfig() {
        super();
    }
    
    /**
     * Instantiates a new connection configuration from serialized data.
     * 
     * @param input The input stream to deserialize from
     */
    public OutgoingConfig(DataInput input) {
        super(input);
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.conf.ConnectionConfig#setDefaults()
     */
    protected void setDefaults() {
        super.setDefaults();
        setServerPort(25);
        useAuth = 0;
        serverUser = "";
        serverPass = "";
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        String text = getAcctName().concat(" (SMTP)");
        return text;
    }

    /**
     * Gets which authentication method to use.
     * Valid values are constants defined in {@link SmtpProtocol}.
     * 
     * @return The authentication method to use, or 0 for none.
     */
    public int getUseAuth() {
        return useAuth;
    }

    /**
     * Sets which authentication method to use.
     * Valid values are constants defined in {@link SmtpProtocol}.
     * 
     * @param useAuth The new authentication method to use, or 0 for none.
     */
    public void setUseAuth(int useAuth) {
        if(this.useAuth != useAuth) {
            this.useAuth = useAuth;
            changeType |= CHANGE_TYPE_CONNECTION;
        }
    }

    /**
     * Gets the username to authenticate with.
     * 
     * @return The username
     */
    public String getServerUser() {
        return serverUser;
    }

    /**
     * Sets the username to authenticate with.
     * 
     * @param serverUser The new username
     */
    public void setServerUser(String serverUser) {
        if(!this.serverUser.equals(serverUser)) {
            this.serverUser = serverUser;
            changeType |= CHANGE_TYPE_CONNECTION;
        }
    }

    /**
     * Gets the password to authenticate with.
     * 
     * @return The password
     */
    public String getServerPass() {
        return serverPass;
    }

    /**
     * Sets the password to authenticate with.
     * 
     * @param serverPass The new password
     */
    public void setServerPass(String serverPass) {
        if(!this.serverPass.equals(serverPass)) {
            this.serverPass = serverPass;
            changeType |= CHANGE_TYPE_CONNECTION;
        }
    }
    
    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.conf.ConnectionConfig#writeConfigItems(org.logicprobe.LogicMail.util.SerializableHashtable)
     */
    public void writeConfigItems(SerializableHashtable table) {
        super.writeConfigItems(table);
        table.put("account_smtpUseAuth", new Integer(useAuth));
        table.put("account_smtpUser", serverUser);
        table.put("account_smtpPass", serverPass);
    }
    
    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.conf.ConnectionConfig#readConfigItems(org.logicprobe.LogicMail.util.SerializableHashtable)
     */
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
    }    
}
