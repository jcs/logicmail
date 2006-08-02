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
import net.rim.device.api.ui.container.*;
import net.rim.device.api.ui.text.TextFilter;
import org.logicprobe.LogicMail.conf.MailSettings;
import org.logicprobe.LogicMail.conf.GlobalConfig;

/**
 * Configuration screen
 */
public class ConfigScreen extends MainScreen implements FieldChangeListener {
    private MailSettings _mailSettings;
    private BasicEditField fldFullname;
    private BasicEditField fldRetMsgCount;
    private ObjectChoiceField fldDispOrder;
    private BasicEditField fldMaxSectionSize;
    
    private ButtonField btSave;

    public ConfigScreen() {
        super();
        LabelField titleField = new LabelField
         ("LogicMail - Config", LabelField.ELLIPSIS | LabelField.USE_ALL_WIDTH);
        setTitle(titleField);

        _mailSettings = MailSettings.getInstance();
        GlobalConfig config = _mailSettings.getGlobalConfig();
        
        fldFullname = new BasicEditField("Full name: ", config.getFullname());
        add(fldFullname);

        fldRetMsgCount = new BasicEditField("Message count: ",
                                            Integer.toString(config.getRetMsgCount()));
        fldRetMsgCount.setFilter(TextFilter.get(TextFilter.NUMERIC));
        add(fldRetMsgCount);

        String[] orderTypes = { "Ascending", "Descending" };
        if(!config.getDispOrder())
            fldDispOrder = new ObjectChoiceField("Message order: ", orderTypes, 0);
        else
            fldDispOrder = new ObjectChoiceField("Message order: ", orderTypes, 1);            
        add(fldDispOrder);
        
        fldMaxSectionSize = new BasicEditField("Max message section size (kb): ",
                                               Integer.toString(config.getMaxSectionSize()/1024));
        fldMaxSectionSize.setFilter(TextFilter.get(TextFilter.NUMERIC));
        add(fldMaxSectionSize);

        btSave = new ButtonField("Save", Field.FIELD_HCENTER);
        btSave.setChangeListener(this);
        add(btSave);
    }

    public void fieldChanged(Field field, int context) {
        if(field == btSave) {
            onClose();
        }
    }

    public void save() {
        GlobalConfig config = _mailSettings.getGlobalConfig();
        config.setFullname(fldFullname.getText());

        try {
            config.setRetMsgCount(Integer.parseInt(fldRetMsgCount.getText()));
        } catch (Exception e) { }

        if(fldDispOrder.getSelectedIndex() == 0)
            config.setDispOrder(false);
        else
            config.setDispOrder(true);

        try {
            config.setMaxSectionSize(Integer.parseInt(fldMaxSectionSize.getText())*1024);
        } catch (Exception e) { }
        
        _mailSettings.saveSettings();
    }
}
