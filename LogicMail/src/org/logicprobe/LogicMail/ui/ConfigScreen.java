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

import org.logicprobe.LogicMail.LogicMailResource;
import org.logicprobe.LogicMail.conf.AccountConfig;
import org.logicprobe.LogicMail.conf.ConnectionConfig;
import org.logicprobe.LogicMail.conf.GlobalConfig;
import org.logicprobe.LogicMail.conf.IdentityConfig;
import org.logicprobe.LogicMail.conf.ImapConfig;
import org.logicprobe.LogicMail.conf.MailSettings;
import org.logicprobe.LogicMail.conf.OutgoingConfig;
import org.logicprobe.LogicMail.conf.PopConfig;

/**
 * This screen is the main entry point to all the
 * other configuration screens.
 */
public class ConfigScreen extends AbstractConfigScreen {
    private MailSettings mailSettings;
    private TreeField configTreeField;
    private int globalId;
    private int identitiesId;
    private int accountsId;
    private int outgoingId;
    private Hashtable identityIndexMap;
    private Hashtable accountIndexMap;
    private Hashtable outgoingIndexMap;
    private boolean configurationChanged;

    /**
     * Instantiates a new configuration screen.
     */
    public ConfigScreen() {
        super(resources.getString(LogicMailResource.APPNAME) + " - " + resources.getString(LogicMailResource.CONFIG_TITLE));
        mailSettings = MailSettings.getInstance();
        identityIndexMap = new Hashtable();
        accountIndexMap = new Hashtable();
        outgoingIndexMap = new Hashtable();

        initFields();

        buildAccountsList();
    }

    /**
     * Initializes the fields.
     */
    private void initFields() {
        configTreeField = FieldFactory.getInstance().getScreenTreeField(
                new TreeFieldCallback() {
                    public void drawTreeItem(TreeField treeField, Graphics graphics, int node, int y, int width, int indent) {
                        configTreeFieldDrawTreeItem(treeField, graphics, node, y, width, indent);
                    }
                },
                Field.FOCUSABLE);
        configTreeField.setDefaultExpanded(true);
        configTreeField.setIndentWidth(20);

        globalId = configTreeField.addChildNode(0, new ConfigTreeNode(resources.getString(LogicMailResource.CONFIG_GLOBAL_SETTINGS)));
        identitiesId = configTreeField.addSiblingNode(globalId, new ConfigTreeNode(resources.getString(LogicMailResource.CONFIG_IDENTITIES)));
        accountsId = configTreeField.addSiblingNode(identitiesId, new ConfigTreeNode(resources.getString(LogicMailResource.CONFIG_ACCOUNTS)));
        outgoingId = configTreeField.addSiblingNode(accountsId, new ConfigTreeNode(resources.getString(LogicMailResource.CONFIG_OUTGOING_SERVERS)));

        add(configTreeField);
    }

    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Screen#onDisplay()
     */
    protected void onDisplay() {
        configurationChanged = false;
        super.onDisplay();
    }

    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Screen#onUndisplay()
     */
    protected void onUndisplay() {
        if(configurationChanged) {
            configurationChanged = false;
        }
        super.onUndisplay();
    }

    /**
     * Draws tree items in the TreeField
     * 
     * @param treeField the tree field
     * @param graphics the graphics
     * @param node the node
     * @param y the y
     * @param width the width
     * @param indent the indent
     */
    public void configTreeFieldDrawTreeItem(TreeField treeField, Graphics graphics, int node, int y, int width, int indent) {
        Object cookie = treeField.getCookie(node);
        graphics.drawText(cookie.toString(), indent + 2, y, Graphics.ELLIPSIS, width);
    }

    private MenuItem selectItem = new MenuItem(resources, LogicMailResource.MENUITEM_EDIT, 100, 10) {
        public void run() {
            openSelectedNode();
        }
    };

    private MenuItem moveUpItem = new MenuItem(resources, LogicMailResource.MENUITEM_MOVE_UP, 100, 10) {
        public void run() {
            moveSelectedNodeUp();
        }
    };

    private MenuItem moveDownItem = new MenuItem(resources, LogicMailResource.MENUITEM_MOVE_DOWN, 100, 10) {
        public void run() {
            moveSelectedNodeDown();
        }
    };

    private MenuItem newAccountWizardItem = new MenuItem(resources, LogicMailResource.MENUITEM_NEW_ACCOUNT_WIZARD, 110, 10) {
        public void run() {
            newAccountWizard();
        }
    };

    private MenuItem addIdentityItem = new MenuItem(resources, LogicMailResource.MENUITEM_ADD_IDENTITY, 120, 10) {
        public void run() {
            addIdentity();
        }
    };

    private MenuItem deleteIdentityItem = new MenuItem(resources, LogicMailResource.MENUITEM_DELETE_IDENTITY, 130, 10) {
        public void run() {
            deleteSelectedIdentity();
        }
    };

    private MenuItem addAccountItem = new MenuItem(resources, LogicMailResource.MENUITEM_ADD_ACCOUNT, 120, 10) {
        public void run() {
            addAccount();
        }
    };

    private MenuItem deleteAccountItem = new MenuItem(resources, LogicMailResource.MENUITEM_DELETE_ACCOUNT, 130, 10) {
        public void run() {
            deleteSelectedAccount();
        }
    };

    private MenuItem addOutgoingItem = new MenuItem(resources, LogicMailResource.MENUITEM_ADD_OUTGOING_SERVER, 120, 10) {
        public void run() {
            addOutgoingServer();
        }
    };

    private MenuItem deleteOutgoingItem = new MenuItem(resources, LogicMailResource.MENUITEM_DELETE_OUTGOING_SERVER, 130, 10) {
        public void run() {
            deleteSelectedOutgoingServer();
        }
    };

    /* (non-Javadoc)
     * @see net.rim.device.api.ui.container.MainScreen#makeMenu(net.rim.device.api.ui.component.Menu, int)
     */
    protected void makeMenu(Menu menu, int instance) {
        int id = configTreeField.getCurrentNode();

        Object cookie;
        Object rawCookie = configTreeField.getCookie(id);
        if(rawCookie instanceof ConfigTreeNode) {
            cookie = ((ConfigTreeNode)rawCookie).cookie;
        }
        else {
            cookie = null;
        }

        if(id != identitiesId && id != accountsId && id != outgoingId) {
            menu.add(selectItem);
            if(cookie instanceof ConnectionConfig) {
                if(configTreeField.getPreviousSibling(id) != -1) {
                    menu.add(moveUpItem);
                }
                if(configTreeField.getNextSibling(id) != -1) {
                    menu.add(moveDownItem);
                }
            }
            menu.addSeparator();
        }

        if(id == identitiesId) {
            menu.add(addIdentityItem);
        }
        else if(cookie instanceof IdentityConfig) {
            menu.add(addIdentityItem);
            menu.add(deleteIdentityItem);
        }
        else if(id == accountsId) {
            menu.add(newAccountWizardItem);
            menu.add(addAccountItem);
        }
        else if(cookie instanceof AccountConfig) {
            menu.add(newAccountWizardItem);
            menu.add(addAccountItem);
            menu.add(deleteAccountItem);
        }
        else if(id == outgoingId) {
            menu.add(addOutgoingItem);
        }
        else if(cookie instanceof OutgoingConfig) {
            menu.add(addOutgoingItem);
            menu.add(deleteOutgoingItem);
        }
        super.makeMenu(menu, instance);
    }

    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Screen#keyChar(char, int, int)
     */
    public boolean keyChar(char key, int status, int time) {
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

    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Screen#navigationClick(int, int)
     */
    protected boolean navigationClick(int status, int time) {
        return openSelectedNode();
    }

    /**
     * Toggle selected node's expansion state
     */
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

    /**
     * Open selected node.
     * 
     * @return true, if successful
     */
    private boolean openSelectedNode() {
        boolean result = false;
        int curNode = configTreeField.getCurrentNode();
        if(curNode == globalId) {
            UiApplication.getUiApplication().pushScreen(new GlobalConfigScreen());
            result = true;
        }
        else {
            int parentNode = configTreeField.getParent(curNode);
            Object cookie = ((ConfigTreeNode)configTreeField.getCookie(curNode)).cookie;
            if(parentNode == identitiesId) {
                IdentityConfig identityConfig = (IdentityConfig)cookie;
                IdentityConfigScreen identityConfigScreen = new IdentityConfigScreen(identityConfig);
                UiApplication.getUiApplication().pushModalScreen(identityConfigScreen);
                if(identityConfigScreen.configSaved()) {
                    mailSettings.saveSettings();
                    configurationChanged = true;
                }
                result = true;
            }
            else if(parentNode == accountsId) {
                AccountConfig acctConfig = (AccountConfig)cookie;
                AccountConfigScreen accountConfigScreen = new AccountConfigScreen(acctConfig);
                UiApplication.getUiApplication().pushModalScreen(accountConfigScreen);
                if(accountConfigScreen.acctSaved()) {
                    mailSettings.saveSettings();
                    configurationChanged = true;
                }
                result = true;
            }
            else if(parentNode == outgoingId) {
                OutgoingConfig outgoingConfig = (OutgoingConfig)cookie;
                OutgoingConfigScreen outgoingConfigScreen = new OutgoingConfigScreen(outgoingConfig);
                UiApplication.getUiApplication().pushModalScreen(outgoingConfigScreen);
                if(outgoingConfigScreen.acctSaved()) {
                    mailSettings.saveSettings();
                    configurationChanged = true;
                }
                result = true;
            }
        }
        if(result) {
            buildAccountsList();
        }
        return result;
    }

    private void moveSelectedNodeUp() {
        int curNode = configTreeField.getCurrentNode();
        if(curNode == -1) { return; }
        int prevNode = configTreeField.getPreviousSibling(curNode);
        if(prevNode == -1) { return; }

        Object cookie = ((ConfigTreeNode)configTreeField.getCookie(curNode)).cookie;
        Object prevCookie = ((ConfigTreeNode)configTreeField.getCookie(prevNode)).cookie;

        boolean result = false;

        if(cookie instanceof IdentityConfig) {
            IdentityConfig curConfig = (IdentityConfig)cookie;
            IdentityConfig prevConfig = (IdentityConfig)prevCookie;

            int curConfigIndex = mailSettings.indexOfIdentityConfig(curConfig);
            mailSettings.removeAccountConfig(curConfigIndex);

            int prevConfigIndex = mailSettings.indexOfIdentityConfig(prevConfig);
            mailSettings.insertIdentityConfigAt(curConfig, prevConfigIndex);
            result = true;
        }
        else if(cookie instanceof AccountConfig) {
            AccountConfig curConfig = (AccountConfig)cookie;
            AccountConfig prevConfig = (AccountConfig)prevCookie;

            int curConfigIndex = mailSettings.indexOfAccountConfig(curConfig);
            mailSettings.removeAccountConfig(curConfigIndex);

            int prevConfigIndex = mailSettings.indexOfAccountConfig(prevConfig);
            mailSettings.insertAccountConfigAt(curConfig, prevConfigIndex);
            result = true;
        }
        else if(cookie instanceof OutgoingConfig) {
            OutgoingConfig curConfig = (OutgoingConfig)cookie;
            OutgoingConfig prevConfig = (OutgoingConfig)prevCookie;

            int curConfigIndex = mailSettings.indexOfOutgoingConfig(curConfig);
            mailSettings.removeAccountConfig(curConfigIndex);

            int prevConfigIndex = mailSettings.indexOfOutgoingConfig(prevConfig);
            mailSettings.insertOutgoingConfigAt(curConfig, prevConfigIndex);
            result = true;
        }

        if(result) {
            mailSettings.saveSettings();
            configurationChanged = true;
            buildAccountsList();
        }
    }

    private void moveSelectedNodeDown() {
        int curNode = configTreeField.getCurrentNode();
        if(curNode == -1) { return; }
        int nextNode = configTreeField.getNextSibling(curNode);
        if(nextNode == -1) { return; }

        Object cookie = ((ConfigTreeNode)configTreeField.getCookie(curNode)).cookie;
        Object nextCookie = ((ConfigTreeNode)configTreeField.getCookie(nextNode)).cookie;

        boolean result = false;

        if(cookie instanceof IdentityConfig) {
            IdentityConfig curConfig = (IdentityConfig)cookie;
            IdentityConfig nextConfig = (IdentityConfig)nextCookie;

            int curConfigIndex = mailSettings.indexOfIdentityConfig(curConfig);
            mailSettings.removeAccountConfig(curConfigIndex);

            int nextConfigIndex = mailSettings.indexOfIdentityConfig(nextConfig);
            mailSettings.insertIdentityConfigAt(curConfig, nextConfigIndex + 1);
            result = true;
        }
        else if(cookie instanceof AccountConfig) {
            AccountConfig curConfig = (AccountConfig)cookie;
            AccountConfig nextConfig = (AccountConfig)nextCookie;

            int curConfigIndex = mailSettings.indexOfAccountConfig(curConfig);
            mailSettings.removeAccountConfig(curConfigIndex);

            int nextConfigIndex = mailSettings.indexOfAccountConfig(nextConfig);
            mailSettings.insertAccountConfigAt(curConfig, nextConfigIndex + 1);
            result = true;
        }
        else if(cookie instanceof OutgoingConfig) {
            OutgoingConfig curConfig = (OutgoingConfig)cookie;
            OutgoingConfig nextConfig = (OutgoingConfig)nextCookie;

            int curConfigIndex = mailSettings.indexOfOutgoingConfig(curConfig);
            mailSettings.removeAccountConfig(curConfigIndex);

            int nextConfigIndex = mailSettings.indexOfOutgoingConfig(nextConfig);
            mailSettings.insertOutgoingConfigAt(curConfig, nextConfigIndex + 1);
            result = true;
        }

        if(result) {
            mailSettings.saveSettings();
            configurationChanged = true;
            buildAccountsList();
        }
    }

    /**
     * Builds the accounts list.
     */
    private void buildAccountsList() {
        Object curCookie;
        int curNode = configTreeField.getCurrentNode();
        if(curNode != -1) {
            curCookie = ((ConfigTreeNode)configTreeField.getCookie(curNode)).cookie;
        }
        else {
            curCookie = null;
        }

        int numIdentities = mailSettings.getNumIdentities();
        identityIndexMap.clear();
        int numAccounts = mailSettings.getNumAccounts();
        accountIndexMap.clear();
        int numOutgoing = mailSettings.getNumOutgoing();
        outgoingIndexMap.clear();
        int id;
        while((id = configTreeField.getFirstChild(identitiesId)) != -1) {
            configTreeField.deleteSubtree(id);
        }
        while((id = configTreeField.getFirstChild(accountsId)) != -1) {
            configTreeField.deleteSubtree(id);
        }
        while((id = configTreeField.getFirstChild(outgoingId)) != -1) {
            configTreeField.deleteSubtree(id);
        }

        IdentityConfig identityConfig;
        for(int i = numIdentities-1; i >= 0; i--) {
            identityConfig = mailSettings.getIdentityConfig(i);
            configTreeField.addChildNode(identitiesId, new ConfigTreeNode(identityConfig));
            identityIndexMap.put(identityConfig, new Integer(i));
        }

        AccountConfig acctConfig;
        for(int i = numAccounts-1; i >= 0; i--) {
            acctConfig = mailSettings.getAccountConfig(i);
            configTreeField.addChildNode(accountsId, new ConfigTreeNode(acctConfig));
            accountIndexMap.put(acctConfig, new Integer(i));
        }

        OutgoingConfig outgoingConfig;
        for(int i = numOutgoing-1; i >= 0; i--) {
            outgoingConfig = mailSettings.getOutgoingConfig(i);
            configTreeField.addChildNode(outgoingId, new ConfigTreeNode(outgoingConfig));
            outgoingIndexMap.put(outgoingConfig, new Integer(i));
        }

        if(curCookie != null) {
            int node = configTreeField.nextNode(0, 0, true);
            while(node != -1) {
                if(((ConfigTreeNode)configTreeField.getCookie(node)).cookie == curCookie) {
                    configTreeField.setCurrentNode(node);
                    break;
                }
                else {
                    node = configTreeField.nextNode(node, 0, true);
                }
            }
        }
    }

    /**
     * Starts the new account wizard.
     */
    private void newAccountWizard() {
        AccountConfigWizard wizard = new AccountConfigWizard();
        wizard.start();
    }

    /**
     * Adds a new identity.
     */
    private void addIdentity() {
        IdentityConfig identityConfig = new IdentityConfig();
        IdentityConfigScreen identityConfigScreen = new IdentityConfigScreen(identityConfig);
        UiApplication.getUiApplication().pushModalScreen(identityConfigScreen);
        if(identityConfigScreen.configSaved()) {
            mailSettings.addIdentityConfig(identityConfig);
            mailSettings.saveSettings();
            configurationChanged = true;
            buildAccountsList();
        }
    }

    /**
     * Delete the currently selected identity.
     */
    private void deleteSelectedIdentity() {
        IdentityConfig identityConfig =
            (IdentityConfig)((ConfigTreeNode)configTreeField.getCookie(configTreeField.getCurrentNode())).cookie;

        int index = ((Integer)identityIndexMap.get(identityConfig)).intValue();
        int response = Dialog.ask(Dialog.D_DELETE);
        if(response == Dialog.DELETE) {
            mailSettings.removeIdentityConfig(index);
            mailSettings.saveSettings();
            configurationChanged = true;
            configTreeField.deleteSubtree(configTreeField.getCurrentNode());
            identityIndexMap.remove(identityConfig);
            buildAccountsList();
        }
    }

    /**
     * Adds a new account.
     */
    private void addAccount() {
        int response = Dialog.ask(resources.getString(LogicMailResource.CONFIG_WHAT_ACCOUNT_TYPE), new String[] { "IMAP", "POP" }, 0);
        if(response != Dialog.CANCEL) {
            AccountConfig acctConfig;
            if(response == 0) {
                acctConfig = new ImapConfig();
            }
            else {
                acctConfig = new PopConfig();
            }
            AccountConfigScreen accountConfigScreen = new AccountConfigScreen(acctConfig);
            UiApplication.getUiApplication().pushModalScreen(accountConfigScreen);
            if(accountConfigScreen.acctSaved()) {
                mailSettings.addAccountConfig(acctConfig);
                mailSettings.saveSettings();
                configurationChanged = true;
                buildAccountsList();
            }
        }
    }

    /**
     * Delete the currently selected account.
     */
    private void deleteSelectedAccount() {
        AccountConfig acctConfig =
            (AccountConfig)((ConfigTreeNode)configTreeField.getCookie(configTreeField.getCurrentNode())).cookie;

        int index = ((Integer)accountIndexMap.get(acctConfig)).intValue();
        int response = Dialog.ask(Dialog.D_DELETE);
        if(response == Dialog.DELETE) {
            mailSettings.removeAccountConfig(index);
            mailSettings.saveSettings();
            configurationChanged = true;
            configTreeField.deleteSubtree(configTreeField.getCurrentNode());
            accountIndexMap.remove(acctConfig);
            buildAccountsList();
        }
    }

    /**
     * Adds a new outgoing server.
     */
    private void addOutgoingServer() {
        OutgoingConfig outgoingConfig = new OutgoingConfig();
        OutgoingConfigScreen outgoingConfigScreen = new OutgoingConfigScreen(outgoingConfig);
        UiApplication.getUiApplication().pushModalScreen(outgoingConfigScreen);
        if(outgoingConfigScreen.acctSaved()) {
            mailSettings.addOutgoingConfig(outgoingConfig);
            mailSettings.saveSettings();
            configurationChanged = true;
            buildAccountsList();
        }
    }

    /**
     * Delete the currently selected outgoing server.
     */
    private void deleteSelectedOutgoingServer() {
        OutgoingConfig outgoingConfig =
            (OutgoingConfig)((ConfigTreeNode)configTreeField.getCookie(configTreeField.getCurrentNode())).cookie;

        int index = ((Integer)outgoingIndexMap.get(outgoingConfig)).intValue();
        int response = Dialog.ask(Dialog.D_DELETE);
        if(response == Dialog.DELETE) {
            mailSettings.removeOutgoingConfig(index);
            mailSettings.saveSettings();
            configurationChanged = true;
            configTreeField.deleteSubtree(configTreeField.getCurrentNode());
            outgoingIndexMap.remove(outgoingConfig);
            buildAccountsList();
        }
    }

    private static class ConfigTreeNode implements TreeFieldNode {
        public Object cookie;

        public ConfigTreeNode(Object cookie) {
            this.cookie = cookie;
        }

        public String toString() {
            return cookie.toString();
        }

        public boolean isNodeSelectable() {
            if((cookie instanceof GlobalConfig)
                    || (cookie instanceof IdentityConfig)
                    || (cookie instanceof AccountConfig)
                    || (cookie instanceof OutgoingConfig)) {
                return true;
            }
            else {
                return false;
            }
        }
    }
}
