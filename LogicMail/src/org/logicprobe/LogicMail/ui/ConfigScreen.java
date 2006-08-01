/*
 * ConfigScreen.java
 *
 * © <your company here>, 2003-2005
 * Confidential and proprietary.
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
