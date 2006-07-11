/*
 * AccountConfig.java
 *
 * © <your company here>, 2003-2005
 * Confidential and proprietary.
 */

package org.logicprobe.LogicMail.conf;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 * Store account configuration for LogicMail
 */
public class AccountConfig {
    public static int TYPE_POP = 0;
    public static int TYPE_IMAP = 1;
    
    private String _acctName;
    private String _serverName;
    private int _serverType;
    private boolean _serverSSL;
    private String _serverUser;
    private String _serverPass;

    public AccountConfig() {
        _acctName = "";
        _serverName = "";
        _serverType = TYPE_POP;
        _serverSSL = false;
        _serverUser = "";
        _serverPass = "";
    }
    
    public AccountConfig(byte[] byteArray) {
        deserialize(byteArray);
    }

    public String getAcctName() {
        return _acctName;
    }
    
    public void setAcctName(String acctName) {
        _acctName = acctName;
    }
    
    public String getServerName() {
        return _serverName;
    }
    
    public void setServerName(String serverName) {
        _serverName = serverName;
    }
    
    public int getServerType() {
        return _serverType;
    }
    
    public void setServerType(int serverType) {
        _serverType = serverType;
    }
    
    public boolean getServerSSL() {
        return _serverSSL;
    }
    
    public void setServerSSL(boolean serverSSL) {
        _serverSSL = serverSSL;
    }

    /**
     * Get the server port.
     * Eventually this parameter might be exposed to
     * the user.  Right now it just returns the default
     * port based on the protocol and SSL settings.
     * @return TCP port for this account
     */
    public int getServerPort() {
        if(_serverType == TYPE_POP) {
            if(_serverSSL)
                return 995;
            else
                return 110;
        }
        else if(_serverType == TYPE_IMAP) {
            if(_serverSSL)
                return 993;
            else
                return 143;
        }
        else
            return 0;
    }

    public String getServerUser() {
        return _serverUser;
    }
    
    public void setServerUser(String serverUser) {
        _serverUser = serverUser;
    }
    
    public String getServerPass() {
        return _serverPass;
    }
    
    public void setServerPass(String serverPass) {
        _serverPass = serverPass;
    }

    public byte[] serialize() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(buffer);
        
        try {
            output.writeUTF(_acctName);
            output.writeUTF(_serverName);
            output.writeInt(_serverType);
            output.writeBoolean(_serverSSL);
            output.writeUTF(_serverUser);
            output.writeUTF(_serverPass);
            return buffer.toByteArray();
        } catch (IOException exp) {
            return null;
        }
    }

    public void deserialize(byte[] byteArray) {
        ByteArrayInputStream buffer = new ByteArrayInputStream(byteArray);
        DataInputStream input = new DataInputStream(buffer);
        
        try {
            _acctName = input.readUTF();
            _serverName = input.readUTF();
            _serverType = input.readInt();
            _serverSSL = input.readBoolean();
            _serverUser = input.readUTF();
            _serverPass = input.readUTF();
        } catch (IOException exp) {
            _acctName = "";
            _serverName = "";
            _serverType = TYPE_POP;
            _serverSSL = false;
            _serverUser = "";
            _serverPass = "";
        }
    }
}

