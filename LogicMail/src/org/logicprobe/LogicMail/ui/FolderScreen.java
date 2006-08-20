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

import java.util.Vector;
import java.io.IOException;
import net.rim.device.api.system.Application;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Keypad;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.component.TreeField;
import net.rim.device.api.ui.component.TreeFieldCallback;
import org.logicprobe.LogicMail.mail.MailClient;
import org.logicprobe.LogicMail.mail.ImapClient;
import org.logicprobe.LogicMail.mail.MailException;
import org.logicprobe.LogicMail.cache.AccountCache;
import org.logicprobe.LogicMail.controller.MailboxController;
import org.logicprobe.LogicMail.util.Observable;

/**
 * Mail folder listing
 * (may only be available with IMAP)
 */
public class FolderScreen extends BaseScreen implements TreeFieldCallback {
    private TreeField treeField;
    private MailClient _client;
    private MailboxController _mailboxController;
    
    public FolderScreen(MailClient client) {
        super("Folders");
        _client = client;
        _mailboxController = MailboxController.getInstance();
        _mailboxController.addObserver(this);

        treeField = new TreeField(this, Field.FOCUSABLE);
        treeField.setEmptyString("No folders", 0);
        treeField.setDefaultExpanded(true);
        treeField.setIndentWidth(20);
        
        add(treeField);
        
        AccountCache acctCache =
                new AccountCache(_client.getAcctConfig().getAcctName());
        Vector folderItemList = acctCache.loadFolderList();
        if(folderItemList != null && folderItemList.size() > 0) {
            generateFolderTree(folderItemList);
            acctCache.saveFolderList(folderItemList);
        }
        else
            _mailboxController.refreshFolderTree();
    }

    protected boolean onSavePrompt() {
        return true;
    }

    public boolean onClose() {
        if(_client.isConnected()) {
            if(Dialog.ask(Dialog.D_YES_NO, "Disconnect from server?") == Dialog.YES) {
                try { _client.close(); } catch (Exception exp) { }
                close();
                return true;
            }
            else
                return false;
        }
        else {
            close();
            return true;
        }
    }

    private void openSelectedFolder() {
        if(treeField == null) return;
        int curNode = treeField.getCurrentNode();
        if(curNode == -1) return;
        Object cookie = treeField.getCookie(curNode);
        if(cookie instanceof MailClient.FolderItem) {
            UiApplication.getUiApplication().pushScreen(new MailboxScreen(_client, (MailClient.FolderItem)cookie));
        }
    }
    
    private MenuItem folderItem = new MenuItem("Select", 100, 10) {
        public void run() {
            openSelectedFolder();
        }
    };

    private MenuItem refreshItem = new MenuItem("Refresh", 110, 10) {
        public void run() {
            _mailboxController.refreshFolderTree();
        }
    };

    protected void makeMenu(Menu menu, int instance) {
        menu.add(folderItem);
        menu.add(refreshItem);
        super.makeMenu(menu, instance);
    }

    public void drawTreeItem(TreeField treeField,
                             Graphics graphics,
                             int node,
                             int y,
                             int width,
                             int indent)
    {
        Object cookie = treeField.getCookie( node );
        if( cookie instanceof MailClient.FolderItem ) {
            MailClient.FolderItem item = (MailClient.FolderItem)cookie;
            graphics.drawText( item.name, indent, y, Graphics.ELLIPSIS, width );
        }
    }
    
    public boolean keyChar(char key,
                           int status,
                           int time)
    {
        boolean retval = false;
        switch(key) {
            case Keypad.KEY_ENTER:
                openSelectedFolder();
                retval = true;
                break;
        }
        return retval;
    }
    
    private synchronized void generateFolderTree(Vector folderItemList) {
        treeField.deleteAll();
        int parent = 0;
        int node = 0;
        int depth = 0;
        int olddepth = 0;
        Vector path = new Vector();
        for(int i=0;i<folderItemList.size();i++) {
            MailClient.FolderItem folderItem =
                    (MailClient.FolderItem)folderItemList.elementAt(i);
            // Find the tree depth of this folder
            depth = 0;
            int pos = 0;
            while(pos < folderItem.path.length()) {
                if(folderItem.path.indexOf(folderItem.delim, pos) != -1) { depth++; }
                pos++;
            }
            if(depth > olddepth) {
                path.addElement(new Integer(node));
                parent = ((Integer)path.lastElement()).intValue();
            }
            else if(depth < olddepth) {
                for(int j=path.size()-1;j>depth;j--)
                    path.removeElementAt(j);
                parent = ((Integer)path.elementAt(path.size()-2)).intValue();
            }
            node = treeField.addChildNode(parent, folderItem);
            if(depth != olddepth)
                olddepth = depth;
        }
        treeField.setDirty(true);
    }
    
    public void update(Observable subject, Object arg) {
        super.update(subject, arg);
        if(subject.equals(_mailboxController)) {
            if(((String)arg).equals("folders")) {
                synchronized(Application.getEventLock()) {
                    generateFolderTree(_mailboxController.getFolderItemList());
                }
            }
        }
    }
}
