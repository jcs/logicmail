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

package org.logicprobe.LogicMail.controller;

import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.util.Arrays;
import org.logicprobe.LogicMail.conf.AccountConfig;
import org.logicprobe.LogicMail.conf.MailSettings;
import org.logicprobe.LogicMail.ui.AcctCfgScreen;
import org.logicprobe.LogicMail.ui.ConfigScreen;
import org.logicprobe.LogicMail.util.Observer;
import org.logicprobe.LogicMail.util.Observable;

/**
 * Controller for configuration screens
 */
public class ConfigController extends Controller implements Observable {
    private static ConfigController _instance;
    private MailSettings _mailSettings;
    
    /** Creates a new instance of ConfigController */
    private ConfigController() {
        _mailSettings = MailSettings.getInstance();
    }
    
    public static synchronized ConfigController getInstance() {
        if(_instance == null)
            _instance = new ConfigController();
        return _instance;
    }
    
    public void configGlobal() {
        UiApplication.getUiApplication().pushModalScreen(new ConfigScreen());
        notifyObservers("global");
    }
    
    public void addAccount() {
        AccountConfig acctConfig = new AccountConfig();
        AcctCfgScreen acctCfgScreen = new AcctCfgScreen(acctConfig);
        UiApplication.getUiApplication().pushModalScreen(acctCfgScreen);
        if(acctCfgScreen.acctSaved()) {
            _mailSettings.addAccountConfig(acctConfig);
            _mailSettings.saveSettings();
            notifyObservers("accounts");
        }
    }

    public void editAccount(int index) {
        if(index == -1) return;
        AccountConfig acctConfig = _mailSettings.getAccountConfig(index);
        AcctCfgScreen acctCfgScreen = new AcctCfgScreen(acctConfig);
        UiApplication.getUiApplication().pushModalScreen(acctCfgScreen);
        if(acctCfgScreen.acctSaved()) {
            _mailSettings.saveSettings();
            notifyObservers("accounts");
        }
    }
    
    public void deleteAccount(int index) {
        if(index == -1) return;
        int response = Dialog.ask(Dialog.D_DELETE);
        if(response == Dialog.DELETE) {
            _mailSettings.removeAccountConfig(index);
            _mailSettings.saveSettings();
            notifyObservers("accounts");
        }            
    }

}
