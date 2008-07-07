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

import net.rim.device.api.system.Bitmap;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.FieldChangeListener;
import net.rim.device.api.ui.component.BasicEditField;
import net.rim.device.api.ui.component.ButtonField;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.component.PasswordEditField;

public class LoginDialog extends Dialog {
    private BasicEditField fldUser;
    private PasswordEditField fldPass;
    private String username;
    private String password;
    
    public LoginDialog(String username, String password) {
        super("Authentication", null, null, 0,
              Bitmap.getPredefinedBitmap(Bitmap.QUESTION),
              Field.FOCUSABLE | Field.FIELD_HCENTER);
        this.username = username;
        this.password = password;
        initFields();
    }
    
    private void initFields() {
        fldUser = new BasicEditField("Username: ", username);
        fldPass = new PasswordEditField("Password: ", password);
        this.add(fldUser);
        this.add(fldPass);
        this.add(new LabelField("", Field.NON_FOCUSABLE));
        
        ButtonField btnOk = new ButtonField("OK", Field.FOCUSABLE | Field.FIELD_HCENTER);
        btnOk.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                if(fldUser.getText().length() > 0 && fldPass.getText().length() > 0) {
                    LoginDialog.this.select(Dialog.OK);
                    LoginDialog.this.close();
                }
            }
        });
        this.add(btnOk);
    }
    
    public String getUsername() {
        return fldUser.getText();
    }
    
    public String getPassword() {
        return fldPass.getText();
    }

}
