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

package org.logicprobe.LogicMail.ui;

import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.ui.container.*;

/**
 * Configuration screen
 */
public class AcctCfgScreen extends MainScreen implements FieldChangeListener {
    private BasicEditField fldAcctName;
    private BasicEditField fldServerName;
    private ObjectChoiceField fldServerType;
    private CheckboxField fldServerSSL;
    private BasicEditField fldServerUser;
    private PasswordEditField fldServerPass;
    private boolean saveData;

    private ButtonField btSave;

    public AcctCfgScreen() {
        super();
        LabelField titleField = new LabelField
         ("LogicMail - Account", LabelField.ELLIPSIS | LabelField.USE_ALL_WIDTH);
        setTitle(titleField);

        add(fldAcctName = new BasicEditField("Account name: ", ""));
        add(fldServerName = new BasicEditField("Server: ", ""));
        String[] serverTypes = { "POP", "IMAP" };
        add(fldServerType = new ObjectChoiceField("Type: ", serverTypes));
        add(fldServerSSL = new CheckboxField("SSL", false));
        add(fldServerUser = new BasicEditField("Username: ", ""));
        add(fldServerPass = new PasswordEditField("Password: ", ""));
        
        btSave = new ButtonField("Save", Field.FIELD_HCENTER);
        btSave.setChangeListener(this);
        add(btSave);
        
        saveData = false;
    }

    public void fieldChanged(Field field, int context) {
        if(field == btSave) {
            onClose();
        }
    }

    public void save() {
        saveData = true;
    }

    public boolean getSaveData() {
        return saveData;
    }

    public String getAcctName() {
        return fldAcctName.getText();
    }
    
    public void setAcctName(String acctName) {
        fldAcctName.setText(acctName);
    }
    
    public String getServerName() {
        return fldServerName.getText();
    }
    
    public void setServerName(String serverName) {
        fldServerName.setText(serverName);
    }

    public int getServerType() {
        return fldServerType.getSelectedIndex();
    }
    
    public void setServerType(int serverType) {
        fldServerType.setSelectedIndex(serverType);
    }
    public boolean getServerSSL() {
        return fldServerSSL.getChecked();
    }
    
    public void setServerSSL(boolean serverSSL) {
        fldServerSSL.setChecked(serverSSL);
    }
    public String getServerUser() {
        return fldServerUser.getText();
    }
    
    public void setServerUser(String serverUser) {
        fldServerUser.setText(serverUser);
    }
    
    public String getServerPass() {
        return fldServerPass.getText();
    }
    
    public void setServerPass(String serverPass) {
        fldServerPass.setText(serverPass);
    }
}

