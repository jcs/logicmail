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

import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Keypad;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.component.ListField;
import net.rim.device.api.ui.component.ListFieldCallback;
import net.rim.device.api.ui.component.Menu;
import org.logicprobe.LogicMail.conf.AccountConfig;
import org.logicprobe.LogicMail.conf.MailSettings;
import org.logicprobe.LogicMail.controller.MailboxController;
import org.logicprobe.LogicMail.util.Observable;

/**
 * Provide a list of accounts for the user
 * to select from.  Ultimately, this should
 * be the main screen for the program.
 */
public class AccountScreen extends BaseScreen implements ListFieldCallback {
    private MailSettings _mailSettings;
    private ListField _accountList;

    public AccountScreen() {
        super("Accounts");
        
        _mailSettings = MailSettings.getInstance();
        
        _accountList = new ListField();
        _accountList.setEmptyString("No accounts", 0);
        _accountList.setCallback(this);
        for(int i=0;i<_mailSettings.getNumAccounts();i++)
            _accountList.insert(i);
        add(_accountList);
        
    }

    protected boolean onSavePrompt() {
        return true;
    }
    
    // menu items
    private MenuItem selectAcctItem = new MenuItem("Select account", 100, 10) {
        public void run() {
            selectAccount(_accountList.getSelectedIndex());
        }
    };

    private MenuItem editAcctItem = new MenuItem("Edit account", 110, 10) {
        public void run() {
            _configController.editAccount(_accountList.getSelectedIndex());
        }
    };

    private MenuItem addAcctItem = new MenuItem("Add account", 120, 10) {
        public void run() {
            _configController.addAccount();
        }
    };

    private MenuItem deleteAcctItem = new MenuItem("Delete account", 130, 10) {
        public void run() {
            _configController.deleteAccount(_accountList.getSelectedIndex());
        }
    };

    protected void makeMenu(Menu menu, int instance) {
        if(_mailSettings.getNumAccounts() > 0) {
            menu.add(selectAcctItem);
            menu.addSeparator();
            menu.add(editAcctItem);
        }
        menu.add(addAcctItem);
        if(_mailSettings.getNumAccounts() > 0) {
            menu.add(deleteAcctItem);
        }
        super.makeMenu(menu, instance);
    }
    
    public void drawListRow(ListField list,
                            Graphics g,
                            int index,
                            int y, int w)
    {
        String text = _mailSettings.getAccountConfig(index).getAcctName();
        if(_mailSettings.getAccountConfig(index).getServerType() == AccountConfig.TYPE_POP)
            text = text.concat(" (POP)");
        else if(_mailSettings.getAccountConfig(index).getServerType() == AccountConfig.TYPE_IMAP)
            text = text.concat(" (IMAP)");
            
        // In the future, consider icons or font size changes
        // to indicate the runtime status of an account on
        // the listing screen.
        g.drawText(text, 0, y, 0, w);
    }

    public int getPreferredWidth(ListField listField) {
        return Graphics.getScreenWidth();
    }
    
    public Object get(ListField listField, int index) {
        return (Object)_mailSettings.getAccountConfig(index);
    }
    
    public int indexOfList(ListField listField,
                           String prefix,
                           int start)
    {
        return 0;
    }

    public boolean keyChar(char key,
                           int status,
                           int time)
    {
        boolean retval = false;
        switch(key) {
            case Keypad.KEY_ENTER:
                selectAccount(_accountList.getSelectedIndex());
                retval = true;
                break;
        }
        return retval;
    }

    public void update(Observable subject, Object arg) {
        super.update(subject, arg);
        if(subject.equals(_configController)) {
            if(((String)arg).equals("accounts")) {
                int numAcct = _mailSettings.getNumAccounts();
                int selItem = _accountList.getSelectedIndex();

                while(_accountList.getSize() > 0)
                    _accountList.delete(0);
                
                for(int i=0;i<numAcct;i++)
                    _accountList.insert(i);
                
                if(selItem > numAcct)
                    selItem = numAcct;
                _accountList.setSelectedIndex(selItem);
            }
        }
    }
    
    /**
     * Open the selected account.
     * If the account is IMAP, then we go to the folder tree.
     * If the account is POP, then go straight to the inbox.
     */
    private void selectAccount(int index) {
        AccountConfig acctConfig = _mailSettings.getAccountConfig(index);
        if(acctConfig == null) return;
        MailboxController.getInstance().openAccount(acctConfig);
    }
}
