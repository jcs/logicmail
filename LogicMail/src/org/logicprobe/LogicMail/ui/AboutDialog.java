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

import org.logicprobe.LogicMail.AppInfo;
import org.logicprobe.LogicMail.LogicMailResource;

import net.rim.device.api.i18n.ResourceBundle;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.Ui;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.component.SeparatorField;

public class AboutDialog extends Dialog {
    private static ResourceBundle resources = ResourceBundle.getBundle(LogicMailResource.BUNDLE_ID, LogicMailResource.BUNDLE_NAME);
    private static final int BACKDOOR_RST = ('R' << 16) | ('S' << 8) | ('T');

    public AboutDialog() {
        super(
                resources.getString(LogicMailResource.ABOUT_TITLE) + ' ' + AppInfo.getName(),
                new Object[] { resources.getString(LogicMailResource.MENUITEM_CLOSE) },
                new int[] { Dialog.OK },
                Dialog.OK,
                Bitmap.getBitmapResource("logicmail.png"));
        setEscapeEnabled(true);
        initFields();
    }

    private void initFields() {
        StringBuffer buf = new StringBuffer();
        buf.append(AppInfo.getName());
        buf.append(' ');
        buf.append(AppInfo.getVersion());
        if(AppInfo.isRelease()) {
            String moniker = AppInfo.getVersionMoniker();
            if(moniker != null && moniker.length() > 0) {
                buf.append(" (");
                buf.append(moniker);
                buf.append(')');
            }
        }
        else {
            buf.append(" (dev)");
        }
        
        LabelField nameLabelField = new LabelField(buf.toString(), Field.FIELD_HCENTER);
        LabelField urlLabelField = new LabelField(resources.getString(LogicMailResource.ABOUT_URL), Field.FIELD_HCENTER);

        LabelField licenseLabelField = new LabelField(resources.getStringArray(LogicMailResource.ABOUT_LICENSE)[0]);
        licenseLabelField.setFont(Font.getDefault().derive(Font.PLAIN, 6, Ui.UNITS_pt));
        
        add(new SeparatorField());
        add(nameLabelField);
        add(urlLabelField);
        add(licenseLabelField);
        add(new SeparatorField());
    }
    
    protected boolean openDevelopmentBackdoor(int backdoorCode) {
        switch( backdoorCode ) {
        case BACKDOOR_RST:
            backdoorRST();
            return true;
        }
        return super.openDevelopmentBackdoor(backdoorCode);
    }

    protected boolean openProductionBackdoor(int backdoorCode) {
        switch( backdoorCode ) {
        case BACKDOOR_RST:
            backdoorRST();
            return true;
        }
        return super.openProductionBackdoor(backdoorCode);
    }
    
    private void backdoorRST() {
        AppInfo.resetPersistableInfo();
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                Dialog.inform("Persisted application startup properties have been reset");
            }
        });
    }
}
