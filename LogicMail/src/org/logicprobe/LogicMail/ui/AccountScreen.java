/*
 * AccountScreen.java
 *
 * © <your company here>, 2003-2005
 * Confidential and proprietary.
 */

package org.logicprobe.LogicMail.ui;

import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.ui.container.*;
import java.util.Vector;
import org.logicprobe.LogicMail.conf.*;
import org.logicprobe.LogicMail.mail.MailClient;
import org.logicprobe.LogicMail.mail.ImapClient;

/**
 * Provide a list of accounts for the user
 * to select from.  Ultimately, this should
 * be the main screen for the program.
 */
public class AccountScreen extends BaseScreen implements ListFieldCallback {
    private MailSettings _mailSettings;
    private ListField _accountList;
    
    public AccountScreen(MailSettings mailSettings) {
        super("Accounts");
        
        _mailSettings = mailSettings;
        
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
            editAccount(_accountList.getSelectedIndex());
        }
    };

    private MenuItem addAcctItem = new MenuItem("Add account", 120, 10) {
        public void run() {
            editAccount(-1);
        }
    };

    private MenuItem deleteAcctItem = new MenuItem("Delete account", 130, 10) {
        public void run() {
            deleteAccount(_accountList.getSelectedIndex());
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

    private void editAccount(int index) {
        AcctCfgScreen acctCfgScreen = new AcctCfgScreen();
        if(index == -1) {
            // new account
            UiApplication.getUiApplication().pushModalScreen(acctCfgScreen);
            
            // now save the results
            if(acctCfgScreen.getSaveData()) {
                AccountConfig acctConfig = new AccountConfig();
                acctConfig.setAcctName(acctCfgScreen.getAcctName());
                acctConfig.setServerName(acctCfgScreen.getServerName());
                acctConfig.setServerType(acctCfgScreen.getServerType());
                acctConfig.setServerSSL(acctCfgScreen.getServerSSL());
                acctConfig.setServerUser(acctCfgScreen.getServerUser());
                acctConfig.setServerPass(acctCfgScreen.getServerPass());
                _mailSettings.addAccountConfig(acctConfig);
                _mailSettings.saveSettings();
            }
        } else {
            // edit existing account
            AccountConfig acctConfig = _mailSettings.getAccountConfig(index);
            acctCfgScreen.setAcctName(acctConfig.getAcctName());
            acctCfgScreen.setServerName(acctConfig.getServerName());
            acctCfgScreen.setServerType(acctConfig.getServerType());
            acctCfgScreen.setServerSSL(acctConfig.getServerSSL());
            acctCfgScreen.setServerUser(acctConfig.getServerUser());
            acctCfgScreen.setServerPass(acctConfig.getServerPass());
            
            UiApplication.getUiApplication().pushModalScreen(acctCfgScreen);
            
            // wait for screen to die, then save results
            if(acctCfgScreen.getSaveData()) {
                acctConfig.setAcctName(acctCfgScreen.getAcctName());
                acctConfig.setServerName(acctCfgScreen.getServerName());
                acctConfig.setServerType(acctCfgScreen.getServerType());
                acctConfig.setServerSSL(acctCfgScreen.getServerSSL());
                acctConfig.setServerUser(acctCfgScreen.getServerUser());
                acctConfig.setServerPass(acctCfgScreen.getServerPass());
                _mailSettings.saveSettings();
            }
        }
        for(int i=0;i<_accountList.getSize();i++)
            _accountList.delete(i);
        for(int i=0;i<_mailSettings.getNumAccounts();i++)
            _accountList.insert(i);
    }
    
    private void deleteAccount(int index) {
        int response = Dialog.ask(Dialog.D_DELETE);
        if(response == Dialog.DELETE) {
            _mailSettings.removeAccountConfig(index);
            _accountList.delete(index);
            _mailSettings.saveSettings();
        }
    }
    
    /**
     * Open the selected account.
     * If the account is IMAP, then we go to the folder tree.
     * If the account is POP, then go straight to the inbox.
     */
    private void selectAccount(int index) {
        AccountConfig acctConfig = _mailSettings.getAccountConfig(index);
        if(acctConfig.getServerType() == AccountConfig.TYPE_POP) {
            Dialog.inform("POP is not currently supported");
            //MailClient.FolderItem item = new MailClient.FolderItem();
            //item.name = "INBOX";
            //item.msgCount = 0;
            //UiApplication.getUiApplication().pushScreen(new MailboxScreen(_mailSettings, null, item));
        }
        else if(acctConfig.getServerType() == AccountConfig.TYPE_IMAP) {
            MailClient client = new ImapClient(acctConfig);
            UiApplication.getUiApplication().pushScreen(new FolderScreen(_mailSettings, client, acctConfig));
        }
    }
}

