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

import java.io.IOException;
import java.util.Vector;
import net.rim.device.api.system.Application;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Keypad;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.component.TreeField;
import net.rim.device.api.ui.component.TreeFieldCallback;
import org.logicprobe.LogicMail.mail.FolderTreeItem;
import org.logicprobe.LogicMail.mail.IncomingMailClient;
import org.logicprobe.LogicMail.cache.AccountCache;
import org.logicprobe.LogicMail.mail.MailException;
import org.logicprobe.LogicMail.util.SerializableHashtable;

/**
 * Mail folder listing
 * (may only be available with IMAP)
 */
public class FolderScreen extends BaseScreen implements TreeFieldCallback, MailClientHandlerListener {
    private TreeField treeField;
    private IncomingMailClient client;
    private RefreshFolderTreeHandler refreshFolderTreeHandler;
    private RefreshFolderStatusHandler refreshFolderStatusHandler;
    private AccountCache acctCache;
    private FolderTreeItem folderTreeRoot;
    
    public FolderScreen(IncomingMailClient client) {
        super("Folders");
        this.client = client;

        treeField = new TreeField(this, Field.FOCUSABLE);
        treeField.setEmptyString("No folders", 0);
        treeField.setDefaultExpanded(true);
        treeField.setIndentWidth(20);
        
        add(treeField);
        
        acctCache = new AccountCache(this.client.getAcctConfig());

        folderTreeRoot = acctCache.loadFolderTree();
        if(folderTreeRoot != null && folderTreeRoot.hasChildren()) {
            generateFolderTree(folderTreeRoot);
            loadFolderMetadata();
        }
        else {
            refreshFolderTree();
        }
    }

    protected boolean onSavePrompt() {
        return true;
    }

    public boolean onClose() {
        if(checkClose()) {
            saveFolderMetadata();
            close();
            return true;
        }
        else
            return false;
    }

    private MenuItem folderItem = new MenuItem("Select", 100, 10) {
        public void run() {
            openSelectedFolder();
        }
    };
    private MenuItem refreshStatusItem = new MenuItem("Refresh status", 110, 10) {
        public void run() {
            refreshFolderStatus();
        }
    };
    private MenuItem refreshItem = new MenuItem("Refresh folders", 111, 10) {
        public void run() {
            refreshFolderTree();
        }
    };
    private MenuItem compositionItem = new MenuItem("Compose E-Mail", 120, 10) {
        public void run() {
            UiApplication.getUiApplication().pushScreen(new CompositionScreen(client.getAcctConfig()));
        }
    };

    protected void makeMenu(Menu menu, int instance) {
        menu.add(folderItem);
        menu.add(refreshStatusItem);
        menu.add(refreshItem);
        menu.add(compositionItem);
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
        Font origFont = graphics.getFont();
        if( cookie instanceof FolderTreeItem ) {
            FolderTreeItem item = (FolderTreeItem)cookie;
            StringBuffer buf = new StringBuffer();
            buf.append(item.getName());
            if(item.getUnseenCount() > 0) {
                buf.append(" (");
                buf.append(Integer.toString(item.getUnseenCount()));
                buf.append(")");
                graphics.setFont(origFont.derive(Font.BOLD));
            }
            else {
                graphics.setFont(origFont.derive(Font.PLAIN));
            }
            graphics.drawText( buf.toString(), indent, y, Graphics.ELLIPSIS, width );
        }
    }
    
    public boolean keyChar(char key,
                           int status,
                           int time)
    {
        boolean retval = false;
        switch(key) {
            case Keypad.KEY_SPACE:
                toggleSelectedFolder();
                retval = true;
                break;
            case Keypad.KEY_ENTER:
                openSelectedFolder();
                retval = true;
                break;
        }
        return retval;
    }
    private boolean checkClose() {
        // Prompt before closing the connection
        if(client.isConnected()) {
            if(Dialog.ask(Dialog.D_YES_NO, "Disconnect from server?") == Dialog.YES) {
                try { client.close(); } catch (Exception exp) { }
                return true;
            }
            else
                return false;
        }
        else {
            return true;
        }
    }

    /**
     * Load metadata on folder state, and update the folder tree
     */
    public void loadFolderMetadata() {
        SerializableHashtable folderMetadata;
        folderMetadata = acctCache.loadAccountMetadata("ui_folder");

        if(folderMetadata != null) {
            int curNode = treeField.nextNode(0, 0, true);
            Object cookie;
            Object value;
            Vector actions = new Vector();
            
            // Walk the folder tree and build a depth-first list of
            // nodes we need to set the expansion state for
            while(curNode > 0) {
                if(treeField.getFirstChild(curNode) != -1) {
                    cookie = treeField.getCookie(curNode);
                    if(cookie instanceof FolderTreeItem) {
                        value = folderMetadata.get(((FolderTreeItem)cookie).getPath());
                        if(value instanceof Boolean) {
                            actions.addElement(new Object[] { new Integer(curNode), value } );
                        }
                    }
                }
                curNode = treeField.nextNode(curNode, 0, true);
            }
            
            // Iterate backwards across the results from above, and set
            // the appropriate node expansion states.  This approach is
            // necessary so complex trees will be configured properly.
            Object[] action;
            for(int i = actions.size() - 1; i >= 0; i--) {
                action = (Object[])actions.elementAt(i);
                treeField.setExpanded(((Integer)action[0]).intValue(), ((Boolean)action[1]).booleanValue());
            }
        }
        treeField.setCurrentNode(treeField.getFirstChild(0));
    }
    
    /**
     * Save metadata on folder state
     */
    public void saveFolderMetadata() {
        SerializableHashtable folderMetadata = new SerializableHashtable();
        int curNode = treeField.nextNode(0, 0, true);
        Object cookie;
        while(curNode > 0) {
            if(treeField.getFirstChild(curNode) != -1) {
                cookie = treeField.getCookie(curNode);
                if(cookie instanceof FolderTreeItem) {
                    folderMetadata.put(
                            ((FolderTreeItem)cookie).getPath(),
                            new Boolean(treeField.getExpanded(curNode)));
                }
            }
            curNode = treeField.nextNode(curNode, 0, true);
        }
        acctCache.saveAccountMetadata("ui_folder", folderMetadata);
    }
    
    /**
     * Kick off the folder tree refresh process
     */
    public void refreshFolderTree() {
        // Initialize the handler on demand
        if(refreshFolderTreeHandler == null) {
            refreshFolderTreeHandler = new RefreshFolderTreeHandler();
            refreshFolderTreeHandler.setListener(this);
        }
        
        // Start the background process
        refreshFolderTreeHandler.start();
    }

    /**
     * Kick off the folder status refresh process
     */
    public void refreshFolderStatus() {
        // Initialize the handler on demand
        if(refreshFolderStatusHandler == null) {
            refreshFolderStatusHandler = new RefreshFolderStatusHandler(folderTreeRoot);
            refreshFolderStatusHandler.setListener(this);
        }
        
        // Start the background process
        refreshFolderStatusHandler.start();
    }

    private void openSelectedFolder() {
    	if(treeField == null) {
            return;
        }
        int curNode = treeField.getCurrentNode();
        if(curNode == -1) {
            return;
        }
        Object cookie = treeField.getCookie(curNode);
        if(cookie instanceof FolderTreeItem) {
            UiApplication.getUiApplication().pushScreen(new MailboxScreen(client, (FolderTreeItem)cookie));
        }
    }

    private void toggleSelectedFolder() {
        // Make sure the tree is valid
    	if(treeField == null) {
            return;
        }
        
        int curNode = treeField.getCurrentNode();
        
        // Make sure a node is selected
        if(curNode == -1) {
            return;
        }
        
        // Make sure the selected node has children
        if(treeField.getFirstChild(curNode) == -1) {
            return;
        }

        // Toggle the expansion state of the current node
        treeField.setExpanded(curNode, !treeField.getExpanded(curNode));
    }

    private synchronized void generateFolderTree(FolderTreeItem folderRoot) {
        treeField.deleteAll();
        generateFolderTreeHelper(treeField, 0, folderRoot);
        treeField.setDirty(true);
    }
    
    public void generateFolderTreeHelper(TreeField tree, int parent, FolderTreeItem item) {
        int id = (item.getParent() == null) ? 0 : tree.addChildNode(parent, item);
        treeField.invalidateNode(id);

        if(item.hasChildren()) {
            FolderTreeItem[] children = item.children();
            for(int i=children.length-1; i >= 0; i--) {
                generateFolderTreeHelper(tree, id, children[i]);
            }
        }
    }
    
    public void mailActionComplete(MailClientHandler source, boolean result) {
        if(source.equals(refreshFolderTreeHandler)) {
            if(refreshFolderTreeHandler.getFolderRoot() != null) {
                synchronized(Application.getEventLock()) {
                    folderTreeRoot = refreshFolderTreeHandler.getFolderRoot();
                    generateFolderTree(folderTreeRoot);
                }
            }
        }
        else if(source.equals(refreshFolderStatusHandler)) {
            synchronized(Application.getEventLock()) {
                if(folderTreeRoot != null) {
                    generateFolderTree(folderTreeRoot);
                }
            }
        }
    }

    /**
     * Implements the folder tree refresh action
     */
    private class RefreshFolderTreeHandler extends MailClientHandler {
        private FolderTreeItem folderRoot;
        public RefreshFolderTreeHandler() {
            super(FolderScreen.this.client, "Refreshing folder tree");
        }
        public void runSession() throws IOException, MailException {
            // Open a connection to the IMAP server, and retrieve
            // the folder tree as a list of delimited items
            FolderTreeItem folderItem;
            try {
                folderItem = ((IncomingMailClient)client).getFolderTree();
            } catch (MailException exp) {
                folderItem = null;
                throw exp;
            }

            // Save the results to the cache
            AccountCache acctCache =
                new AccountCache(((IncomingMailClient)client).getAcctConfig());
            acctCache.saveFolderTree(folderItem);

            this.folderRoot = folderItem;
        }

        public FolderTreeItem getFolderRoot() {
            return folderRoot;
        }
    }

    /**
     * Implements the folder status refresh action
     */
    private class RefreshFolderStatusHandler extends MailClientHandler {
        private FolderTreeItem folderRoot;
        public RefreshFolderStatusHandler(FolderTreeItem folderRoot) {
            super(FolderScreen.this.client, "Refreshing folder status");
            this.folderRoot = folderRoot;
        }
        public void runSession() throws IOException, MailException {
            // Open a connection to the IMAP server, and update
            // the folder status
            try {
                ((IncomingMailClient)client).refreshFolderStatus(folderRoot);
            } catch (MailException exp) {
                throw exp;
            }
        }
    }
}
