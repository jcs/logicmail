/*-
 * Copyright (c) 2008, Derek Konigsberg
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
import net.rim.device.api.ui.component.AutoTextEditField;
import net.rim.device.api.ui.component.BasicEditField;
import net.rim.device.api.ui.component.ButtonField;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.EmailAddressEditField;
import net.rim.device.api.ui.component.RichTextField;
import net.rim.device.api.ui.component.SeparatorField;
import org.logicprobe.LogicMail.conf.IdentityConfig;

/**
 * Identity configuration screen
 */
public class IdentityConfigScreen extends BaseCfgScreen {
    private IdentityConfig identityConfig;
    private boolean configSaved;

    private BasicEditField identityNameField;
    private BasicEditField fullNameField;
    private EmailAddressEditField emailAddressField;
    private EmailAddressEditField replyToAddressField;
    private AutoTextEditField msgSignatureField;
    private ButtonField saveButton;
    
    /**
     * Creates a new instance of IdentityConfigScreen
     */
    public IdentityConfigScreen(IdentityConfig identityConfig) {
        super("LogicMail - Identity");
        this.identityConfig = identityConfig;
        this.configSaved = false;
        initFields();
    }

    private void initFields() {
        identityNameField = new BasicEditField("Identity: ", identityConfig.getIdentityName());
        fullNameField = new BasicEditField("Full name: ", identityConfig.getFullName());
        emailAddressField = new EmailAddressEditField("E-Mail address: ", identityConfig.getEmailAddress());
        replyToAddressField = new EmailAddressEditField("Reply-To address: ", identityConfig.getReplyToAddress());
        msgSignatureField = new AutoTextEditField();
        msgSignatureField.setText(identityConfig.getMsgSignature());

        saveButton = new ButtonField("Save", Field.FIELD_HCENTER);
        saveButton.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                saveButton_FieldChanged(field, context);
            }});

        add(identityNameField);
        add(new SeparatorField());
        add(fullNameField);
        add(emailAddressField);
        add(replyToAddressField);
        add(new SeparatorField());
        add(new RichTextField("Signature:", Field.NON_FOCUSABLE));
        add(msgSignatureField);
        add(new SeparatorField());
        add(saveButton);
    }

    private void saveButton_FieldChanged(Field field, int context) {
        field.setDirty(false);
        onClose();
    }

    protected boolean onSavePrompt() {
        if(identityNameField.getText().length() > 0 &&
           emailAddressField.getText().length() > 0) {
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
        identityConfig.setIdentityName(identityNameField.getText());
        identityConfig.setFullName(fullNameField.getText());
        identityConfig.setEmailAddress(emailAddressField.getText());
        identityConfig.setReplyToAddress(replyToAddressField.getText());
        identityConfig.setMsgSignature(msgSignatureField.getText());
        configSaved = true;
    }
    
    public boolean configSaved() {
        return configSaved;
    }
}
