/*
 * LogicMail.java
 *
 * © <your company here>, 2003-2005
 * Confidential and proprietary.
 */

package org.logicprobe.LogicMail;

import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.ui.container.*;
import net.rim.device.api.system.*;

import org.logicprobe.LogicMail.ui.AccountScreen;
import org.logicprobe.LogicMail.conf.MailSettings;

/**
 * Main class for the application
 */
public class LogicMail extends UiApplication {
    public static void main(String argv[]) {
        LogicMail app = new LogicMail();
        app.enterEventDispatcher();
    }

    public LogicMail() {
        // Create the configuration object instance
        MailSettings mailSettings = new MailSettings();
        
        // Load the configuration
        mailSettings.loadSettings();
        pushScreen(new AccountScreen(mailSettings));
    }
} 
