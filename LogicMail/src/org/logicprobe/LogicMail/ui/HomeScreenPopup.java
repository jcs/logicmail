/*-
 * Copyright (c) 2010, Derek Konigsberg
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

import net.rim.device.api.i18n.ResourceBundle;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.FieldChangeListener;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.Ui;
import net.rim.device.api.ui.component.ButtonField;
import net.rim.device.api.ui.component.CheckboxField;
import net.rim.device.api.ui.component.RichTextField;
import net.rim.device.api.ui.component.SeparatorField;
import net.rim.device.api.ui.container.PopupScreen;
import net.rim.device.api.ui.container.VerticalFieldManager;

/**
 * This is the popup screen shown the first time the application is started.
 * It contains the end-user license, and any related text.
 */
public class HomeScreenPopup extends PopupScreen {
    protected static ResourceBundle resources = ResourceBundle.getBundle(LogicMailResource.BUNDLE_ID, LogicMailResource.BUNDLE_NAME);
    private RichTextField licenseTextField;
    private CheckboxField analyticsCheckboxField;
    private RichTextField acceptTextField;
    private RichTextField spacerField1;
    private RichTextField spacerField2;
    private ButtonField okayButton;
    private ButtonField cancelButton;
    private boolean accepted;
    
    public HomeScreenPopup() {
        super(new VerticalFieldManager(Manager.VERTICAL_SCROLL | Manager.VERTICAL_SCROLLBAR));
        
        Font font = Font.getDefault().derive(Font.PLAIN, 6, Ui.UNITS_pt);
        
        licenseTextField = new RichTextField(RichTextField.READONLY);
        String[] licenseText = resources.getStringArray(LogicMailResource.ABOUT_LICENSE);
        for(int i=0; i<licenseText.length; i++) {
            licenseTextField.insert(licenseText[i]);
            if(i < licenseText.length - 1) {
                licenseTextField.insert("\n");
            }
        }
        licenseTextField.setFont(font);
        licenseTextField.setEditable(false);

        spacerField1 = new RichTextField("", RichTextField.READONLY);
        spacerField1.setFont(font);
        spacerField1.setEditable(false);
        
        analyticsCheckboxField = new CheckboxField(
                resources.getString(LogicMailResource.ABOUT_ANALYTICS_ENABLE),
                true);
        analyticsCheckboxField.setFont(font);
        
        acceptTextField = new RichTextField(
                resources.getString(LogicMailResource.ABOUT_LICENSE_ACCEPT),
                RichTextField.READONLY);
        acceptTextField.setFont(font);
        acceptTextField.setEditable(false);
        
        spacerField2 = new RichTextField("", RichTextField.READONLY);
        spacerField2.setFont(font);
        spacerField2.setEditable(false);
        
        okayButton = new ButtonField(resources.getString(LogicMailResource.MENUITEM_ACCEPT), ButtonField.CONSUME_CLICK | Field.FIELD_HCENTER);
        okayButton.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                okayButtonClicked();
            }});
        cancelButton = new ButtonField(resources.getString(LogicMailResource.MENUITEM_DECLINE), ButtonField.CONSUME_CLICK | Field.FIELD_HCENTER);
        cancelButton.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                cancelButtonClicked();
            }});
        
        add(licenseTextField);
        add(spacerField1);
        add(analyticsCheckboxField);
        add(spacerField2);
        add(acceptTextField);
        add(new SeparatorField());
        add(okayButton);
        add(cancelButton);
    }
    
    private void okayButtonClicked() {
        accepted = true;
        this.close();
    }

    private void cancelButtonClicked() {
        accepted = false;
        this.close();
    }
    
    public boolean isLicenseAccepted() {
        return accepted;
    }
    
    public boolean isAnalyticsEnabled() {
        return analyticsCheckboxField.getChecked();
    }
}
