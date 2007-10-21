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
    private ButtonField saveButton;
    
    private BasicEditField fldSmtpServerName;
    private BasicEditField fldSmtpServerPort;
    private CheckboxField fldSmtpServerSSL;
    private EmailAddressEditField fldSmtpFromAddress;
    private ObjectChoiceField fldSmtpUseAuth;
    private BasicEditField fldSmtpUser;
    private PasswordEditField fldSmtpPass;

    private boolean acctSaved;
    private AccountConfig acctConfig;
    private FieldChangeListener fieldChangeListener;
    
    public AcctCfgScreen(AccountConfig acctConfig) {
        super("LogicMail - Account");
        
        this.acctConfig = acctConfig;
        acctSaved = false;
        
        fieldChangeListener = new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                AcctCfgScreen_fieldChanged(field, context);
            }};
        
        initFields();

        if(acctConfig.getSmtpUseAuth() == 0) {
            fldSmtpUser.setEditable(false);
            fldSmtpPass.setEditable(false);
            fldSmtpUser.setText("");
            fldSmtpPass.setText("");
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
        fldSmtpServerName = new BasicEditField("SMTP server: ", acctConfig.getSmtpServerName());
        
        fldSmtpServerPort = new BasicEditField("Port: ", Integer.toString(acctConfig.getSmtpServerPort()));
        fldSmtpServerPort.setFilter(TextFilter.get(TextFilter.NUMERIC));
        
        fldSmtpServerSSL = new CheckboxField("SSL", acctConfig.getSmtpServerSSL());
        fldSmtpServerSSL.setChangeListener(fieldChangeListener);
        
        fldSmtpFromAddress = new EmailAddressEditField("E-Mail address: ", acctConfig.getSmtpFromAddress());
        
        
        String authTypes[] = { "NONE", "PLAIN", "LOGIN", "CRAM-MD5"/*, "DIGEST-MD5"*/ };
        fldSmtpUseAuth = new ObjectChoiceField("Authentication: ", authTypes, acctConfig.getSmtpUseAuth());
        
        fldSmtpUser = new BasicEditField("Username: ", acctConfig.getSmtpUser());
        fldSmtpPass = new PasswordEditField("Password: ", acctConfig.getSmtpPass());
        
        fldSmtpUseAuth.setChangeListener(fieldChangeListener);
        
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
        add(new SeparatorField());
        add(new RichTextField("Outgoing server:", Field.NON_FOCUSABLE));
        add(fldSmtpServerName);
        add(fldSmtpServerPort);
        add(fldSmtpServerSSL);
        add(fldSmtpFromAddress);
        add(fldSmtpUseAuth);
        add(fldSmtpUser);
        add(fldSmtpPass);
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
        else if(field == fldSmtpServerSSL) {
            if(fldSmtpServerSSL.getChecked())
                fldSmtpServerPort.setText("465");
            else
                fldSmtpServerPort.setText("25");
        }
        else if(field == fldSmtpUseAuth) {
            if(fldSmtpUseAuth.getSelectedIndex() > 0) {
                fldSmtpUser.setEditable(true);
                fldSmtpPass.setEditable(true);
                fldSmtpUser.setText(acctConfig.getSmtpUser());
                fldSmtpPass.setText(acctConfig.getSmtpPass());
            }
            else {
                fldSmtpUser.setEditable(false);
                fldSmtpPass.setEditable(false);
                fldSmtpUser.setText("");
                fldSmtpPass.setText("");
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
        this.acctConfig.setSmtpServerName(fldSmtpServerName.getText());
        this.acctConfig.setSmtpServerPort(Integer.parseInt(fldSmtpServerPort.getText()));
        this.acctConfig.setSmtpServerSSL(fldSmtpServerSSL.getChecked());
        this.acctConfig.setSmtpFromAddress(fldSmtpFromAddress.getText());
        this.acctConfig.setSmtpUseAuth(fldSmtpUseAuth.getSelectedIndex());
        if(fldSmtpUseAuth.getSelectedIndex() > 0) {
            this.acctConfig.setSmtpUser(fldSmtpUser.getText());
            this.acctConfig.setSmtpPass(fldSmtpPass.getText());
        }
        acctSaved = true;
    }
    
    public boolean acctSaved() {
        return acctSaved;
    }
}
