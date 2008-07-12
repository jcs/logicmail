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

package org.logicprobe.LogicMail.ui;

import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.FieldChangeListener;
import net.rim.device.api.ui.component.BasicEditField;
import net.rim.device.api.ui.component.ButtonField;
import net.rim.device.api.ui.component.CheckboxField;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.component.ObjectChoiceField;
import net.rim.device.api.ui.component.PasswordEditField;
import net.rim.device.api.ui.component.SeparatorField;
import net.rim.device.api.ui.text.TextFilter;

import org.logicprobe.LogicMail.LogicMailResource;
import org.logicprobe.LogicMail.conf.OutgoingConfig;

/**
 * Outgoing account configuration screen
 */
public class OutgoingConfigScreen extends BaseCfgScreen {
    private BasicEditField acctNameField;
    private BasicEditField serverNameField;
    private CheckboxField serverSslField;
    private BasicEditField serverPortField;
    private ObjectChoiceField useAuthField;
    private BasicEditField serverUserField;
    private PasswordEditField serverPassField;
    private CheckboxField useMdsField;
    private ButtonField saveButton;
    private LabelField statusLabel;
    
    private boolean acctSaved;
    private OutgoingConfig outgoingConfig;

    /**
     * Creates a new instance of OutgoingConfigScreen
     */
    public OutgoingConfigScreen(OutgoingConfig outgoingConfig) {
        super("LogicMail - " + resources.getString(LogicMailResource.CONFIG_OUTGOING_TITLE));
        
        this.outgoingConfig = outgoingConfig;
        acctSaved = false;
        
        initFields();

        if(outgoingConfig.getUseAuth() == 0) {
            serverUserField.setEditable(false);
            serverPassField.setEditable(false);
            serverUserField.setText("");
            serverPassField.setText("");
        }
    }

    private void initFields() {
        acctNameField =
            new BasicEditField(resources.getString(LogicMailResource.CONFIG_ACCOUNT_NAME) + " ", outgoingConfig.getAcctName());
        
        serverNameField =
            new BasicEditField(resources.getString(LogicMailResource.CONFIG_ACCOUNT_SERVER) + " ", outgoingConfig.getServerName());
        
        serverSslField =
            new CheckboxField("SSL", outgoingConfig.getServerSSL());
        serverSslField.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                serverSslField_FieldChanged(field, context);
            }});
        
        serverPortField =
            new BasicEditField(resources.getString(LogicMailResource.CONFIG_ACCOUNT_PORT) + " ", Integer.toString(outgoingConfig.getServerPort()));
        serverPortField.setFilter(TextFilter.get(TextFilter.NUMERIC));
        
        String authTypes[] = {
        		resources.getString(LogicMailResource.MENUITEM_NONE),
        		"PLAIN", "LOGIN", "CRAM-MD5"/*, "DIGEST-MD5"*/ };
        useAuthField =
            new ObjectChoiceField(resources.getString(LogicMailResource.CONFIG_OUTGOING_AUTHENTICATION) + " ", authTypes, outgoingConfig.getUseAuth());
        useAuthField.setChangeListener(new FieldChangeListener(){
            public void fieldChanged(Field field, int context) {
                useAuthField_FieldChanged(field, context);
            }});

        serverUserField =
            new BasicEditField(resources.getString(LogicMailResource.CONFIG_ACCOUNT_USERNAME) + " ", outgoingConfig.getServerUser());
        
        serverPassField =
            new PasswordEditField(resources.getString(LogicMailResource.CONFIG_ACCOUNT_PASSWORD) + " ", outgoingConfig.getServerPass());
        
        useMdsField =
            new CheckboxField(resources.getString(LogicMailResource.CONFIG_ACCOUNT_USEMDSPROXY), !outgoingConfig.getDeviceSide());
        
        saveButton =
            new ButtonField(resources.getString(LogicMailResource.MENUITEM_SAVE), Field.FIELD_HCENTER);
        saveButton.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                saveButton_FieldChanged(field, context);
            }});
        
        statusLabel = new LabelField();

        add(acctNameField);
        add(new SeparatorField());
        add(serverNameField);
        add(serverSslField);
        add(serverPortField);
        add(useAuthField);
        add(serverUserField);
        add(serverPassField);
        add(useMdsField);
        add(new LabelField(null, Field.NON_FOCUSABLE));
        add(saveButton);
        setStatus(statusLabel);
    }
    
    private void serverSslField_FieldChanged(Field field, int context) {
        if(serverSslField.getChecked()) {
            serverPortField.setText("465");
        }
        else {
            serverPortField.setText("25");
        }
    }
    
    private void useAuthField_FieldChanged(Field field, int context) {
        if(useAuthField.getSelectedIndex() > 0) {
            serverUserField.setEditable(true);
            serverPassField.setEditable(true);
            serverUserField.setText(outgoingConfig.getServerUser());
            serverPassField.setText(outgoingConfig.getServerPass());
        }
        else {
            serverUserField.setEditable(false);
            serverPassField.setEditable(false);
            serverUserField.setText("");
            serverPassField.setText("");
        }
    }

    private void saveButton_FieldChanged(Field field, int context) {
        field.setDirty(false);
        onClose();
    }
    
    protected boolean onSavePrompt() {
        if(acctNameField.getText().length() > 0 &&
           serverNameField.getText().length() > 0 &&
           serverPortField.getText().length() > 0) {
            return super.onSavePrompt();
        }
        else {
            int result =
            	Dialog.ask(resources.getString(LogicMailResource.CONFIG_PROMPT_INCOMPLETE),
            			new String[] {
            				resources.getString(LogicMailResource.MENUITEM_DISCARD),
            				resources.getString(LogicMailResource.MENUITEM_CANCEL) }, 0);
            if(result == 0) {
                return true;
            }
            else {
                return false;
            }
        }
    }
    
    public void save() {
        this.outgoingConfig.setAcctName(acctNameField.getText());
        this.outgoingConfig.setServerName(serverNameField.getText());
        this.outgoingConfig.setServerSSL(serverSslField.getChecked());
        this.outgoingConfig.setServerPort(Integer.parseInt(serverPortField.getText()));
        this.outgoingConfig.setServerUser(serverUserField.getText());
        this.outgoingConfig.setServerPass(serverPassField.getText());
        this.outgoingConfig.setDeviceSide(!useMdsField.getChecked());
        this.outgoingConfig.setUseAuth(useAuthField.getSelectedIndex());
        if(useAuthField.getSelectedIndex() > 0) {
            this.outgoingConfig.setServerUser(serverUserField.getText());
            this.outgoingConfig.setServerPass(serverPassField.getText());
        }
        acctSaved = true;
    }
    
    public boolean acctSaved() {
        return acctSaved;
    }
}
