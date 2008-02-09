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
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.ListField;
import net.rim.device.api.ui.component.ListFieldCallback;
import net.rim.device.api.ui.component.Menu;
import org.logicprobe.LogicMail.conf.AccountConfig;
import org.logicprobe.LogicMail.conf.MailSettings;
import org.logicprobe.LogicMail.mail.IncomingMailClient;
import org.logicprobe.LogicMail.mail.MailClientFactory;

/**
 * Provide a list of accounts for the user
 * to select from.  Ultimately, this should
 * be the main screen for the program.
 */
public class AccountScreen extends BaseScreen implements ListFieldCallback {
    private MailSettings mailSettings;
    private ListField accountList;

    public AccountScreen() {
        super("Accounts");
        
        mailSettings = MailSettings.getInstance();
        
        accountList = new ListField();
        accountList.setEmptyString("No accounts", 0);
        accountList.setCallback(this);
        for(int i=0;i<mailSettings.getNumAccounts();i++)
            accountList.insert(i);
        add(accountList);
    }
    
    protected boolean onSavePrompt() {
        return true;
    }
    
    // menu items
    private MenuItem selectAcctItem = new MenuItem("Select", 100, 10) {
        public void run() {
            selectAccount(accountList.getSelectedIndex());
        }
    };

    protected void makeMenu(Menu menu, int instance) {
        if(mailSettings.getNumAccounts() > 0) {
            menu.add(selectAcctItem);
        }
        super.makeMenu(menu, instance);
    }
    
    public void drawListRow(ListField list,
                            Graphics g,
                            int index,
                            int y, int w)
    {
        String text = mailSettings.getAccountConfig(index).toString();

        // In the future, consider icons or font size changes
        // to indicate the runtime status of an account on
        // the listing screen.
        g.drawText(text, 0, y, 0, w);
    }

    public int getPreferredWidth(ListField listField) {
        return Graphics.getScreenWidth();
    }
    
    public Object get(ListField listField, int index) {
        return (Object)mailSettings.getAccountConfig(index);
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
                selectAccount(accountList.getSelectedIndex());
                retval = true;
                break;
        }
        return retval;
    }

    protected void showConfigScreen() {
        super.showConfigScreen();
        updateAccountList();
    }
    
    private void updateAccountList() {
        int numAcct = mailSettings.getNumAccounts();
        int selItem = accountList.getSelectedIndex();

        while(accountList.getSize() > 0) {
            accountList.delete(0);
        }

        for(int i=0;i<numAcct;i++) {
            accountList.insert(i);
        }

        if(selItem > numAcct) {
            selItem = numAcct;
        }
        accountList.setSelectedIndex(selItem);
    }
    
    /**
     * Open the selected account.
     * If the account supports folders, then we go to the folder tree.
     * Otherwise, go straight to the inbox.
     */
    private void selectAccount(int index) {
        AccountConfig acctConfig = mailSettings.getAccountConfig(index);
        if(acctConfig == null) {
            return;
        }

        IncomingMailClient client = MailClientFactory.createMailClient(acctConfig);
        if(client.hasFolders()) {
            UiApplication.getUiApplication().pushScreen(new FolderScreen(client));
        }
        else {
            UiApplication.getUiApplication().pushScreen(new MailboxScreen(client, client.getActiveFolder()));
        }
    }
}
