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
import net.rim.device.api.ui.text.TextFilter;
import org.logicprobe.LogicMail.conf.AccountConfig;

/**
 * Configuration screen
 */
public class AcctCfgScreen extends BaseCfgScreen implements FieldChangeListener {
    private BasicEditField fldAcctName;
    private BasicEditField fldServerName;
    private ObjectChoiceField fldServerType;
    private CheckboxField fldServerSSL;
    private BasicEditField fldServerPort;
    private BasicEditField fldServerUser;
    private PasswordEditField fldServerPass;
    private CheckboxField fldUseMDS;
    private ButtonField btSave;

    private boolean acctSaved;
    private AccountConfig acctConfig;
    
    public AcctCfgScreen(AccountConfig acctConfig) {
        super("LogicMail - Account");
        
        this.acctConfig = acctConfig;
        acctSaved = false;
        
        add(fldAcctName = new BasicEditField("Account name: ",
                                             acctConfig.getAcctName()));
        add(fldServerName = new BasicEditField("Server: ",
                                               acctConfig.getServerName()));
        String[] serverTypes = { "POP", "IMAP" };
        add(fldServerType = new ObjectChoiceField("Protocol: ",
                                                  serverTypes,
                                                  acctConfig.getServerType()));
        fldServerType.setChangeListener(this);
        fldServerSSL = new CheckboxField("SSL",
                                         acctConfig.getServerSSL());
        fldServerSSL.setChangeListener(this);
        add(fldServerSSL);
        fldServerPort = new BasicEditField("Port: ",
                                           Integer.toString(acctConfig.getServerPort()));
        fldServerPort.setFilter(TextFilter.get(TextFilter.NUMERIC));
        add(fldServerPort);
        add(fldServerUser = new BasicEditField("Username: ",
                                               acctConfig.getServerUser()));
        add(fldServerPass = new PasswordEditField("Password: ",
                                                  acctConfig.getServerPass()));

        fldUseMDS = new CheckboxField("Use MDS proxy",
                                      !acctConfig.getDeviceSide());
        add(fldUseMDS);

        add(new LabelField(null, Field.NON_FOCUSABLE));
        
        btSave = new ButtonField("Save", Field.FIELD_HCENTER);
        btSave.setChangeListener(this);
        add(btSave);
    }

    public void fieldChanged(Field field, int context) {
        if(field == btSave) {
            onClose();
        }
        else if(field == fldServerType || field == fldServerSSL) {
            if(fldServerType.getSelectedIndex() == 0) {
                if(fldServerSSL.getChecked())
                    fldServerPort.setText("995");
                else
                    fldServerPort.setText("110");
            }
            else if(fldServerType.getSelectedIndex() == 1) {
                if(fldServerSSL.getChecked())
                    fldServerPort.setText("993");
                else
                    fldServerPort.setText("143");
            }
        }
    }

    public void save() {
        this.acctConfig.setAcctName(fldAcctName.getText());
        this.acctConfig.setServerName(fldServerName.getText());
        this.acctConfig.setServerType(fldServerType.getSelectedIndex());
        this.acctConfig.setServerSSL(fldServerSSL.getChecked());
        this.acctConfig.setServerPort(Integer.parseInt(fldServerPort.getText()));
        this.acctConfig.setServerUser(fldServerUser.getText());
        this.acctConfig.setServerPass(fldServerPass.getText());
        this.acctConfig.setDeviceSide(!fldUseMDS.getChecked());
        acctSaved = true;
    }
    
    public boolean acctSaved() {
        return acctSaved;
    }
}
