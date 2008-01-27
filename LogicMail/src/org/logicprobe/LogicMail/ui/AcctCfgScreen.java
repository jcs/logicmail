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

import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.FieldChangeListener;
import net.rim.device.api.ui.component.BasicEditField;
import net.rim.device.api.ui.component.ButtonField;
import net.rim.device.api.ui.component.CheckboxField;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.EmailAddressEditField;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.component.ObjectChoiceField;
import net.rim.device.api.ui.component.PasswordEditField;
import net.rim.device.api.ui.component.RichTextField;
import net.rim.device.api.ui.component.SeparatorField;
import net.rim.device.api.ui.text.TextFilter;
import org.logicprobe.LogicMail.conf.AccountConfig;
import org.logicprobe.LogicMail.conf.ImapConfig;
import org.logicprobe.LogicMail.conf.MailSettings;
import org.logicprobe.LogicMail.conf.OutgoingConfig;
import org.logicprobe.LogicMail.conf.PopConfig;

/**
 * Configuration screen
 */
public class AcctCfgScreen extends BaseCfgScreen {
    private BasicEditField acctNameField;
    private BasicEditField serverNameField;
    private CheckboxField serverSslField;
    private BasicEditField serverPortField;
    private BasicEditField serverUserField;
    private PasswordEditField serverPassField;
    private CheckboxField useMdsField;
    private ObjectChoiceField outgoingServerField;
    private ButtonField saveButton;
    
    private boolean acctSaved;
    private AccountConfig acctConfig;
    private OutgoingConfig[] outgoingConfigs;
    private FieldChangeListener fieldChangeListener;
    
    private class NullOutgoingConfig extends OutgoingConfig {
        public String toString() {
            return "None";
        }
        public long getUniqueId() {
            return -1;
        }
    }
    
    public AcctCfgScreen(AccountConfig acctConfig) {
        super("LogicMail - Account");
        
        this.acctConfig = acctConfig;
        acctSaved = false;
        
        MailSettings mailSettings = MailSettings.getInstance();
        int numOutgoing = mailSettings.getNumOutgoing();
        outgoingConfigs = new OutgoingConfig[numOutgoing+1];
        outgoingConfigs[0] = new NullOutgoingConfig();
        for(int i=0; i<numOutgoing; ++i) {
            outgoingConfigs[i+1] = mailSettings.getOutgoingConfig(i);
        }
        
        fieldChangeListener = new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                AcctCfgScreen_fieldChanged(field, context);
            }};

        initFields();

        OutgoingConfig selectedOutgoingConfig = acctConfig.getOutgoingConfig();
        if(selectedOutgoingConfig != null) {
            outgoingServerField.setSelectedIndex(selectedOutgoingConfig);
        }
    }

    private void initFields() {
        acctNameField = new BasicEditField("Account name: ", acctConfig.getAcctName());
        serverNameField = new BasicEditField("Server: ", acctConfig.getServerName());
        
        serverSslField = new CheckboxField("SSL", acctConfig.getServerSSL());
        serverSslField.setChangeListener(fieldChangeListener);
        serverPortField = new BasicEditField("Port: ", Integer.toString(acctConfig.getServerPort()));
        serverPortField.setFilter(TextFilter.get(TextFilter.NUMERIC));
        serverUserField = new BasicEditField("Username: ", acctConfig.getServerUser());
        serverPassField = new PasswordEditField("Password: ", acctConfig.getServerPass());
        
        useMdsField = new CheckboxField("Use MDS proxy", !acctConfig.getDeviceSide());
        outgoingServerField = new ObjectChoiceField("Outgoing server: ", outgoingConfigs, 0);
        
        saveButton = new ButtonField("Save", Field.FIELD_HCENTER);
        saveButton.setChangeListener(fieldChangeListener);
        
        add(acctNameField);
        add(new SeparatorField());
        add(new RichTextField("Incoming server:", Field.NON_FOCUSABLE));
        add(serverNameField);
        
        if(acctConfig instanceof ImapConfig) {
            add(new RichTextField("Protocol: IMAP", Field.NON_FOCUSABLE));
        }
        else if(acctConfig instanceof PopConfig) {
            add(new RichTextField("Protocol: POP", Field.NON_FOCUSABLE));
        }
        
        add(serverSslField);
        add(serverPortField);
        add(serverUserField);
        add(serverPassField);
        add(useMdsField);
        add(new LabelField());
        add(outgoingServerField);
        add(new LabelField());
        add(new SeparatorField());
        add(new LabelField(null, Field.NON_FOCUSABLE));
        add(saveButton);
    }
    
    public void AcctCfgScreen_fieldChanged(Field field, int context) {
        if(field == saveButton) {
            field.setDirty(false);
            onClose();
        }
        else if(field == serverSslField) {
            if(acctConfig instanceof PopConfig) {
                if(serverSslField.getChecked()) {
                    serverPortField.setText("995");
                }
                else {
                    serverPortField.setText("110");
                }
            }
            else if(acctConfig instanceof ImapConfig) {
                if(serverSslField.getChecked()) {
                    serverPortField.setText("993");
                }
                else {
                    serverPortField.setText("143");
                }
            }
        }
    }

    protected boolean onSavePrompt() {
        if(acctNameField.getText().length() > 0 &&
           serverNameField.getText().length() > 0 &&
           serverPortField.getText().length() > 0) {
            return super.onSavePrompt();
        }
        else {
            int result = Dialog.ask("Configuration incomplete!", new String[] { "Discard", "Cancel" }, 0);
            if(result == 0) {
                return true;
            }
            else {
                return false;
            }
        }
    }

    public void save() {
        this.acctConfig.setAcctName(acctNameField.getText());
        this.acctConfig.setServerName(serverNameField.getText());
        this.acctConfig.setServerSSL(serverSslField.getChecked());
        this.acctConfig.setServerPort(Integer.parseInt(serverPortField.getText()));
        this.acctConfig.setServerUser(serverUserField.getText());
        this.acctConfig.setServerPass(serverPassField.getText());
        this.acctConfig.setDeviceSide(!useMdsField.getChecked());
        
        OutgoingConfig selectedOutgoingConfig = (OutgoingConfig)outgoingServerField.getChoice(outgoingServerField.getSelectedIndex());
        if(selectedOutgoingConfig.getUniqueId() == -1) {
            this.acctConfig.setOutgoingConfig(null);
        }
        else {
            this.acctConfig.setOutgoingConfig(selectedOutgoingConfig);
        }
        acctSaved = true;
    }
    
    public boolean acctSaved() {
        return acctSaved;
    }
}
