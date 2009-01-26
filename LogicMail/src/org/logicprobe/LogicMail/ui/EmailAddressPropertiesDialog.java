/*-
 * Copyright (c) 2009, Derek Konigsberg
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

import org.logicprobe.LogicMail.LogicMailResource;
import org.logicprobe.LogicMail.util.StringParser;

import net.rim.device.api.i18n.ResourceBundle;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.FieldChangeListener;
import net.rim.device.api.ui.component.BasicEditField;
import net.rim.device.api.ui.component.ButtonField;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.EmailAddressEditField;
import net.rim.device.api.ui.component.LabelField;

public class EmailAddressPropertiesDialog extends Dialog {
	protected static ResourceBundle resources = ResourceBundle.getBundle(LogicMailResource.BUNDLE_ID, LogicMailResource.BUNDLE_NAME);

	private BasicEditField nameEditField;
    private EmailAddressEditField addressEditField;
    private ButtonField okButton;

    private String name;
    private String address;
    
    public EmailAddressPropertiesDialog(String name, String address) {
        super("Address Properties", null, null, 0,
            Bitmap.getPredefinedBitmap(Bitmap.QUESTION),
            Field.FOCUSABLE | Field.FIELD_HCENTER);

        this.name = name;
        this.address = address;
        
        initFields();
    }

    public EmailAddressPropertiesDialog(String recipient) {
        super("Address Properties", null, null, 0,
                Bitmap.getPredefinedBitmap(Bitmap.QUESTION),
                Field.FOCUSABLE | Field.FIELD_HCENTER);
        String[] recipientElements = StringParser.parseRecipient(recipient);
        
        this.name = recipientElements[0];
        this.address = recipientElements[1];
        
        initFields();
    }
    
    private void initFields() {
        nameEditField = new BasicEditField("Name: ", name);
        addressEditField = new EmailAddressEditField("Address: ", address);

        okButton = new ButtonField(
        		resources.getString(LogicMailResource.MENUITEM_OK),
                Field.FOCUSABLE | Field.FIELD_HCENTER);
        okButton.setChangeListener(new FieldChangeListener() {
                public void fieldChanged(Field field, int context) {
                	EmailAddressPropertiesDialog.this.select(Dialog.OK);
                	EmailAddressPropertiesDialog.this.close();
                }
        });

        this.add(nameEditField);
        this.add(addressEditField);
        this.add(new LabelField("", Field.NON_FOCUSABLE));
        this.add(okButton);
    }
    
    public String getName() {
        return nameEditField.getText();
    }

    public String getAddress() {
        return addressEditField.getText();
    }
    
    public String getRecipient() {
    	return StringParser.mergeRecipient(nameEditField.getText(), addressEditField.getText());
    }
}
