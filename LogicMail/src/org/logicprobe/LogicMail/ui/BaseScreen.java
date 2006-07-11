/*
 * BaseScreen.java
 *
 * © <your company here>, 2003-2005
 * Confidential and proprietary.
 */

package org.logicprobe.LogicMail.ui;

import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.ui.container.*;
import net.rim.device.api.system.KeyListener;

/**
 * This class is the base for all screens in LogicMail.
 * Its purpose is to provide uniform menu and event
 * handler interfaces across the application.
 */
public class BaseScreen extends MainScreen implements KeyListener {
    public BaseScreen(String title) {
        super();
        // Create screen elements
        LabelField titleField = new LabelField
         ("LogicMail - "+title, LabelField.ELLIPSIS | LabelField.USE_ALL_WIDTH);
        setTitle(titleField);
    }

    // Create menu items
    private MenuItem mailboxItem = new MenuItem("Mailbox", 110, 10) {
        public void run() {
            //UiApplication.getUiApplication().pushScreen(new FolderScreen());
        }
    };
    private MenuItem configItem = new MenuItem("Config", 120, 10) {
        public void run() {
            UiApplication.getUiApplication().pushScreen(new AcctCfgScreen());
        }
    };
    private MenuItem aboutItem = new MenuItem("About", 150, 10) {
        public void run() {
            // Show the about dialog
            String aboutMsg = "LogicMail\nVersion 0.1";
            Dialog.inform(aboutMsg);
        }
    };
    private MenuItem closeItem = new MenuItem("Close", 200000, 10) {
        public void run() {
            onClose();
        }
    };
    private MenuItem exitItem = new MenuItem("Exit", 200001, 10) {
        public void run() {
            Dialog.alert("Visit Again!");
            System.exit(0);
        }
    };

    protected void makeMenu(Menu menu, int instance) {
        menu.addSeparator();
        menu.add(aboutItem);
        menu.add(closeItem);
        menu.add(exitItem);
    }

    /*
    public boolean onClose() {
       return true;
    }
    */
    
    // KeyListener methods
    public boolean keyChar(char key, int status, int time) {
        return false;
    }
    public boolean keyDown(int keycode, int time) {
        return false;
    }
    public boolean keyUp(int keycode, int time) {
        return false;
    }
    public boolean keyRepeat(int keycode, int time) {
        return false;
    }
    public boolean keyStatus(int keycode, int time) {
        return false;
    }
}

