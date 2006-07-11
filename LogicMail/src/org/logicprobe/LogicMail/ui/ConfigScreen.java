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

/**
 * Configuration screen
 */
public class ConfigScreen extends BaseScreen {
    private ButtonField btSave;
    public ConfigScreen() {
        super("Config");

        add(new BasicEditField("Account name: ", ""));
        add(new BasicEditField("Server: ", ""));
        String[] serverTypes = { "IMAP", "POP" };
        add(new ObjectChoiceField("Type: ", serverTypes));
        add(new CheckboxField("SSL", false));
        add(new BasicEditField("Username: ", ""));
        add(new PasswordEditField("Password: ", ""));
        
        btSave = new ButtonField("Save", Field.FIELD_HCENTER);
        add(btSave);
    }
}

