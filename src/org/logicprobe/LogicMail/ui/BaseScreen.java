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

import net.rim.device.api.system.KeyListener;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.container.MainScreen;
import org.logicprobe.LogicMail.AppInfo;

/**
 * This class is the base for all screens in LogicMail.
 * Its purpose is to provide uniform menu and event
 * handler interfaces across the application.
 */
public abstract class BaseScreen extends MainScreen implements KeyListener {
    public BaseScreen() {
        super();
    }

    public BaseScreen(String title) {
        super();
        // Create screen elements
        LabelField titleField = new LabelField
         ("LogicMail - "+title, LabelField.ELLIPSIS | LabelField.USE_ALL_WIDTH);
        setTitle(titleField);
    }

    // Create menu items
    private MenuItem configItem = new MenuItem("Config", 10020, 10) {
        public void run() {
            configGlobal();
        }
    };
    private MenuItem aboutItem = new MenuItem("About", 10050, 10) {
        public void run() {
            // Show the about dialog
            String aboutMsg = AppInfo.getName() + "\nVersion " + AppInfo.getVersion();
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

    private void configGlobal() {
        UiApplication.getUiApplication().pushModalScreen(new ConfigScreen());
    }
    
    protected void makeMenu(Menu menu, int instance) {
        menu.addSeparator();
        menu.add(configItem);
        menu.add(aboutItem);
        menu.add(closeItem);
        menu.add(exitItem);
    }

    /**
     * Disable save-prompting dialog since it
     * isn't applicable to most BaseScreen
     * implementations
     */
    protected boolean onSavePrompt() {
        return true;
    }

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

