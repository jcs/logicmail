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
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.component.BasicEditField;
import net.rim.device.api.ui.component.CheckboxField;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.ObjectChoiceField;
import net.rim.device.api.ui.component.PasswordEditField;
import net.rim.device.api.ui.text.TextFilter;

import org.logicprobe.LogicMail.LogicMailResource;
import org.logicprobe.LogicMail.conf.ConnectionConfig;
import org.logicprobe.LogicMail.conf.OutgoingConfig;

/**
 * Outgoing account configuration screen
 */
public class OutgoingConfigScreen extends AbstractConfigScreen {
    private BorderedFieldManager headerFieldManager;
    private BorderedFieldManager contentFieldManager;
    
    private BasicEditField acctNameField;
    private BasicEditField serverNameField;
    private ObjectChoiceField serverSecurityField;
    private BasicEditField serverPortField;
    private ObjectChoiceField useAuthField;
    private BasicEditField serverUserField;
    private PasswordEditField serverPassField;
    private CheckboxField useMdsField;
    
    private boolean acctSaved;
    private OutgoingConfig outgoingConfig;

    /**
     * Creates a new instance of OutgoingConfigScreen
     */
    public OutgoingConfigScreen(OutgoingConfig outgoingConfig) {
        super(resources.getString(LogicMailResource.APPNAME) + " - " + resources.getString(LogicMailResource.CONFIG_OUTGOING_TITLE));
        
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
        Font boldFont = getFont().derive(Font.BOLD);
        
        acctNameField = new BasicEditField(
                resources.getString(LogicMailResource.CONFIG_ACCOUNT_NAME) + ' ',
                outgoingConfig.getAcctName());
        acctNameField.setFont(boldFont);
        
        serverNameField = new HostnameEditField(
                resources.getString(LogicMailResource.CONFIG_ACCOUNT_SERVER) + ' ',
                outgoingConfig.getServerName());
        
        serverSecurityField = new ObjectChoiceField(
        		resources.getString(LogicMailResource.CONFIG_ACCOUNT_SECURITY),
        		new Object[] {
        			resources.getString(LogicMailResource.CONFIG_ACCOUNT_SECURITY_NEVER),
        			resources.getString(LogicMailResource.CONFIG_ACCOUNT_SECURITY_TLS_IF_AVAILABLE),
        			resources.getString(LogicMailResource.CONFIG_ACCOUNT_SECURITY_TLS),
        			resources.getString(LogicMailResource.CONFIG_ACCOUNT_SECURITY_SSL)},
        			outgoingConfig.getServerSecurity());
        serverSecurityField.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
            	serverSecurityField_FieldChanged(field, context);
            }});
        
        serverPortField = new BasicEditField(
                resources.getString(LogicMailResource.CONFIG_ACCOUNT_PORT) + ' ',
                Integer.toString(outgoingConfig.getServerPort()));
        serverPortField.setFilter(TextFilter.get(TextFilter.NUMERIC));
        
        String authTypes[] = {
        		resources.getString(LogicMailResource.MENUITEM_NONE),
        		"PLAIN", "LOGIN", "CRAM-MD5"/*, "DIGEST-MD5"*/ };
        useAuthField = new ObjectChoiceField(
                resources.getString(LogicMailResource.CONFIG_OUTGOING_AUTHENTICATION) + ' ',
                authTypes,
                outgoingConfig.getUseAuth());
        useAuthField.setChangeListener(new FieldChangeListener(){
            public void fieldChanged(Field field, int context) {
                useAuthField_FieldChanged(field, context);
            }});

        serverUserField = new BasicEditField(
                resources.getString(LogicMailResource.CONFIG_ACCOUNT_USERNAME) + ' ',
                outgoingConfig.getServerUser());
        
        serverPassField = new PasswordEditField(
                resources.getString(LogicMailResource.CONFIG_ACCOUNT_PASSWORD) + ' ',
                outgoingConfig.getServerPass());
        
        useMdsField = new CheckboxField(
                resources.getString(LogicMailResource.CONFIG_ACCOUNT_USEMDSPROXY),
                !outgoingConfig.getDeviceSide());
        
        headerFieldManager = new BorderedFieldManager(BorderedFieldManager.BOTTOM_BORDER_NONE);
        headerFieldManager.add(acctNameField);
        
        contentFieldManager = new BorderedFieldManager(BorderedFieldManager.BOTTOM_BORDER_NORMAL | Field.USE_ALL_HEIGHT);
        contentFieldManager.add(serverNameField);
        contentFieldManager.add(serverSecurityField);
        contentFieldManager.add(serverPortField);
        contentFieldManager.add(useAuthField);
        contentFieldManager.add(serverUserField);
        contentFieldManager.add(serverPassField);
        contentFieldManager.add(useMdsField);
        
        add(headerFieldManager);
        add(contentFieldManager);
    }
    
    private void serverSecurityField_FieldChanged(Field field, int context) {
        if(serverSecurityField.getSelectedIndex() == ConnectionConfig.SECURITY_SSL) {
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
        this.outgoingConfig.setServerSecurity(serverSecurityField.getSelectedIndex());
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
