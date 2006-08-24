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
import org.logicprobe.LogicMail.conf.AccountConfig;

/**
 * Configuration screen
 */
public class AcctCfgScreen extends BaseCfgScreen implements FieldChangeListener {
    private BasicEditField fldAcctName;
    private BasicEditField fldServerName;
    private ObjectChoiceField fldServerType;
    private CheckboxField fldServerSSL;
    private BasicEditField fldServerUser;
    private PasswordEditField fldServerPass;
    private ButtonField btSave;

    private boolean _acctSaved;
    private AccountConfig _acctConfig;
    
    public AcctCfgScreen(AccountConfig acctConfig) {
        super("LogicMail - Account");
        
        _acctConfig = acctConfig;
        _acctSaved = false;
        
        add(fldAcctName = new BasicEditField("Account name: ",
                                             acctConfig.getAcctName()));
        add(fldServerName = new BasicEditField("Server: ",
                                               acctConfig.getServerName()));
        String[] serverTypes = { "POP", "IMAP" };
        add(fldServerType = new ObjectChoiceField("Type: ",
                                                  serverTypes,
                                                  acctConfig.getServerType()));
        add(fldServerSSL = new CheckboxField("SSL",
                                             acctConfig.getServerSSL()));
        add(fldServerUser = new BasicEditField("Username: ",
                                               acctConfig.getServerUser()));
        add(fldServerPass = new PasswordEditField("Password: ",
                                                  acctConfig.getServerPass()));
        
        btSave = new ButtonField("Save", Field.FIELD_HCENTER);
        btSave.setChangeListener(this);
        add(btSave);
    }

    public void fieldChanged(Field field, int context) {
        if(field == btSave) {
            onClose();
        }
    }

    public void save() {
        _acctConfig.setAcctName(fldAcctName.getText());
        _acctConfig.setServerName(fldServerName.getText());
        _acctConfig.setServerType(fldServerType.getSelectedIndex());
        _acctConfig.setServerSSL(fldServerSSL.getChecked());
        _acctConfig.setServerUser(fldServerUser.getText());
        _acctConfig.setServerPass(fldServerPass.getText());
        _acctSaved = true;
    }
    
    public boolean acctSaved() {
        return _acctSaved;
    }
}
