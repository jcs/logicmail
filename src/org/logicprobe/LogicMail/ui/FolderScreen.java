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
import net.rim.device.api.system.Application;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Keypad;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.component.TreeField;
import net.rim.device.api.ui.component.TreeFieldCallback;
import org.logicprobe.LogicMail.mail.FolderTreeItem;
import org.logicprobe.LogicMail.mail.MailClient;
import org.logicprobe.LogicMail.cache.AccountCache;
import org.logicprobe.LogicMail.controller.MailboxController;
import org.logicprobe.LogicMail.util.Observable;

/**
 * Mail folder listing
 * (may only be available with IMAP)
 */
public class FolderScreen extends BaseScreen implements TreeFieldCallback {
    private TreeField treeField;
    private MailClient client;
    private MailboxController mailboxController;
    
    public FolderScreen(MailClient client) {
        super("Folders");
        this.client = client;
        mailboxController = MailboxController.getInstance();
        mailboxController.addObserver(this);

        treeField = new TreeField(this, Field.FOCUSABLE);
        treeField.setEmptyString("No folders", 0);
        treeField.setDefaultExpanded(true);
        treeField.setIndentWidth(20);
        
        add(treeField);
        
        AccountCache acctCache =
                new AccountCache(this.client.getAcctConfig().getAcctName());

        FolderTreeItem treeRoot = acctCache.loadFolderTree();
        if(treeRoot != null && treeRoot.hasChildren())
            generateFolderTree(treeRoot);
        else
            mailboxController.refreshFolderTree();
    }

    protected boolean onSavePrompt() {
        return true;
    }

    public boolean onClose() {
        if(mailboxController.checkClose()) {
            close();
            return true;
        }
        else
            return false;
    }

    private void openSelectedFolder() {
    	if(treeField == null) return;
        int curNode = treeField.getCurrentNode();
        if(curNode == -1) return;
        Object cookie = treeField.getCookie(curNode);
        if(cookie instanceof FolderTreeItem) {
            mailboxController.openFolder((FolderTreeItem)cookie);
        }
    }
    
    private MenuItem folderItem = new MenuItem("Select", 100, 10) {
        public void run() {
            openSelectedFolder();
        }
    };

    private MenuItem refreshItem = new MenuItem("Refresh", 110, 10) {
        public void run() {
            mailboxController.refreshFolderTree();
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
        if( cookie instanceof FolderTreeItem ) {
            FolderTreeItem item = (FolderTreeItem)cookie;
            graphics.drawText( item.getName(), indent, y, Graphics.ELLIPSIS, width );
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
    
    private synchronized void generateFolderTree(FolderTreeItem folderRoot) {
        treeField.deleteAll();
        generateFolderTreeHelper(treeField, 0, folderRoot);
        treeField.setDirty(true);
    }
    
    public void generateFolderTreeHelper(TreeField tree, int parent, FolderTreeItem item) {
        int id = (item.getParent() == null) ? 0 : tree.addChildNode(parent, item);
        if(item.hasChildren()) {
            FolderTreeItem[] children = item.children();
            for(int i=0;i<children.length;i++)
                generateFolderTreeHelper(tree, id, children[i]);
        }
    }
    
    public void update(Observable subject, Object arg) {
        super.update(subject, arg);
        if(subject.equals(mailboxController)) {
            if(((String)arg).equals("folders")) {
                synchronized(Application.getEventLock()) {
                    generateFolderTree(mailboxController.getFolderRoot());
                }
            }
        }
    }
}
