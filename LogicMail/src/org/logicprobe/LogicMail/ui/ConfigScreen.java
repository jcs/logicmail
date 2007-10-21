/*-
 * Copyright (c) 2007, Derek Konigsberg
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

import java.util.Hashtable;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Keypad;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.component.TreeField;
import net.rim.device.api.ui.component.TreeFieldCallback;
import org.logicprobe.LogicMail.conf.AccountConfig;
import org.logicprobe.LogicMail.conf.ImapConfig;
import org.logicprobe.LogicMail.conf.MailSettings;
import org.logicprobe.LogicMail.conf.OutgoingConfig;
import org.logicprobe.LogicMail.conf.PopConfig;

/**
 * This screen is the main entry point to all the
 * other configuration screens.
 */
public class ConfigScreen extends BaseCfgScreen {
    private MailSettings mailSettings;
    private TreeField configTreeField;
    private int globalId;
    private int accountsId;
    private int outgoingId;
    private Hashtable accountIndexMap;
    private Hashtable outgoingIndexMap;
    
    public ConfigScreen() {
        super("LogicMail - Configuration");
        mailSettings = MailSettings.getInstance();
        accountIndexMap = new Hashtable();
        outgoingIndexMap = new Hashtable();
        
        initFields();
        
        buildAccountsList();
    }
    
    private void initFields() {
        configTreeField = new TreeField(
            new TreeFieldCallback() {
                public void drawTreeItem(TreeField treeField, Graphics graphics, int node, int y, int width, int indent) {
                    configTreeFieldDrawTreeItem(treeField, graphics, node, y, width, indent);
                }
            },
            Field.FOCUSABLE);
        configTreeField.setDefaultExpanded(true);
        configTreeField.setIndentWidth(20);
        
        globalId = configTreeField.addChildNode(0, "Global settings");
        accountsId = configTreeField.addSiblingNode(globalId, "Accounts");        
        outgoingId = configTreeField.addSiblingNode(accountsId, "Outgoing servers");

        add(configTreeField);
    }
    
    public void configTreeFieldDrawTreeItem(TreeField treeField, Graphics graphics, int node, int y, int width, int indent) {
        Object cookie = treeField.getCookie(node);
        graphics.drawText(cookie.toString(), indent, y, Graphics.ELLIPSIS, width);
    }
    
    private MenuItem selectItem = new MenuItem("Edit", 100, 10) {
        public void run() {
            openSelectedNode();
        }
    };

    private MenuItem addAcctItem = new MenuItem("Add account", 120, 10) {
        public void run() {
            addAccount();
        }
    };

    private MenuItem deleteAcctItem = new MenuItem("Delete account", 130, 10) {
        public void run() {
            deleteSelectedAccount();
        }
    };

    private MenuItem addOutgoingItem = new MenuItem("Add outgoing server", 120, 10) {
        public void run() {
            addOutgoingServer();
        }
    };

    private MenuItem deleteOutgoingItem = new MenuItem("Delete outgoing server", 130, 10) {
        public void run() {
            deleteSelectedOutgoingServer();
        }
    };

    protected void makeMenu(Menu menu, int instance) {
        int id = configTreeField.getCurrentNode();
        if(id != accountsId && id != outgoingId) {
            menu.add(selectItem);
        }
        if(id == accountsId) {
            menu.add(addAcctItem);
        }
        else if(configTreeField.getCookie(id) instanceof AccountConfig) {
            menu.add(addAcctItem);
            menu.add(deleteAcctItem);
        }
        else if(id == outgoingId) {
            menu.add(addOutgoingItem);
        }
        else if(configTreeField.getCookie(id) instanceof OutgoingConfig) {
            menu.add(addOutgoingItem);
            menu.add(deleteOutgoingItem);
        }
        super.makeMenu(menu, instance);
    }
    
    public boolean keyChar(char key, int status, int time)
    {
        boolean retval = false;
        switch(key) {
            case Keypad.KEY_SPACE:
                toggleSelectedNode();
                retval = true;
                break;
            case Keypad.KEY_ENTER:
                openSelectedNode();
                retval = true;
                break;
        }
        return retval;
    }
    
    private void toggleSelectedNode() {
        int curNode = configTreeField.getCurrentNode();
        
        // Make sure a node is selected
        if(curNode == -1) {
            return;
        }
        
        // Make sure the selected node has children
        if(configTreeField.getFirstChild(curNode) == -1) {
            return;
        }

        // Toggle the expansion state of the current node
        configTreeField.setExpanded(curNode, !configTreeField.getExpanded(curNode));
    }
    
    private void openSelectedNode() {
        int curNode = configTreeField.getCurrentNode();
        if(curNode == globalId) {
            UiApplication.getUiApplication().pushScreen(new GlobalConfigScreen());
        }
        else {
            int parentNode = configTreeField.getParent(curNode);
            if(parentNode == accountsId) {
                AccountConfig acctConfig = (AccountConfig)configTreeField.getCookie(curNode);
                AcctCfgScreen acctCfgScreen = new AcctCfgScreen(acctConfig);
                UiApplication.getUiApplication().pushModalScreen(acctCfgScreen);
                if(acctCfgScreen.acctSaved()) {
                    mailSettings.saveSettings();
                }
            }
        }
    }

    private void buildAccountsList() {
        int numAccounts = mailSettings.getNumAccounts();
        accountIndexMap.clear();
        int numOutgoing = mailSettings.getNumOutgoing();
        outgoingIndexMap.clear();
        int id;
        while((id = configTreeField.getFirstChild(accountsId)) != -1) {
            configTreeField.deleteSubtree(id);
        }
        while((id = configTreeField.getFirstChild(outgoingId)) != -1) {
            configTreeField.deleteSubtree(id);
        }
        
        AccountConfig acctConfig;
        for(int i = numAccounts-1; i >= 0; i--) {
            acctConfig = mailSettings.getAccountConfig(i);
            configTreeField.addChildNode(accountsId, acctConfig);
            accountIndexMap.put(acctConfig, new Integer(i));
        }

        OutgoingConfig outgoingConfig;
        for(int i = numOutgoing-1; i >= 0; i--) {
            outgoingConfig = mailSettings.getOutgoingConfig(i);
            configTreeField.addChildNode(outgoingId, outgoingConfig);
            outgoingIndexMap.put(outgoingConfig, new Integer(i));
        }
    }
    
    private void addAccount() {
        int response = Dialog.ask("What type of account?", new String[] { "IMAP", "POP" }, 0);
        if(response != Dialog.CANCEL) {
            AccountConfig acctConfig;
            if(response == 0) {
                acctConfig = new ImapConfig();
            }
            else {
                acctConfig = new PopConfig();
            }
            AcctCfgScreen acctCfgScreen = new AcctCfgScreen(acctConfig);
            UiApplication.getUiApplication().pushModalScreen(acctCfgScreen);
            if(acctCfgScreen.acctSaved()) {
                mailSettings.addAccountConfig(acctConfig);
                mailSettings.saveSettings();
                buildAccountsList();
            }
        }
    }

    private void deleteSelectedAccount() {
        AccountConfig acctConfig =
            (AccountConfig)configTreeField.getCookie(configTreeField.getCurrentNode());
        
        int index = ((Integer)accountIndexMap.get(acctConfig)).intValue();
        int response = Dialog.ask(Dialog.D_DELETE);
        if(response == Dialog.DELETE) {
            mailSettings.removeAccountConfig(index);
            mailSettings.saveSettings();
            configTreeField.deleteSubtree(configTreeField.getCurrentNode());
            accountIndexMap.remove(acctConfig);
        }            
    }

    private void addOutgoingServer() {
        OutgoingConfig outgoingConfig = new OutgoingConfig();
        OutgoingConfigScreen outgoingConfigScreen = new OutgoingConfigScreen(outgoingConfig);
        UiApplication.getUiApplication().pushModalScreen(outgoingConfigScreen);
        if(outgoingConfigScreen.acctSaved()) {
            mailSettings.addOutgoingConfig(outgoingConfig);
            mailSettings.saveSettings();
            buildAccountsList();
        }
    }
    
    private void deleteSelectedOutgoingServer() {
        OutgoingConfig outgoingConfig =
            (OutgoingConfig)configTreeField.getCookie(configTreeField.getCurrentNode());
        
        int index = ((Integer)outgoingIndexMap.get(outgoingConfig)).intValue();
        int response = Dialog.ask(Dialog.D_DELETE);
        if(response == Dialog.DELETE) {
            mailSettings.removeOutgoingConfig(index);
            mailSettings.saveSettings();
            configTreeField.deleteSubtree(configTreeField.getCurrentNode());
            outgoingIndexMap.remove(outgoingConfig);
        }
    }
}
