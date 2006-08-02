/*
 * FolderScreen.java
 *
 * © <your company here>, 2003-2005
 * Confidential and proprietary.
 */

package org.logicprobe.LogicMail.ui;

import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.ui.container.*;
import net.rim.device.api.system.KeyListener;
import net.rim.device.api.system.Application;
import org.logicprobe.LogicMail.conf.*;
import org.logicprobe.LogicMail.mail.MailClient;
import org.logicprobe.LogicMail.mail.ImapClient;
import org.logicprobe.LogicMail.mail.MailException;
import org.logicprobe.LogicMail.cache.AccountCache;
import java.util.Vector;
import java.io.IOException;

/**
 * Mail folder listing
 * (may only be available with IMAP)
 */
public class FolderScreen extends BaseScreen implements TreeFieldCallback {
    private TreeField treeField;
    private MailClient _client;

    public FolderScreen(MailClient client) {
        super("Folders");
        _client = client;

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
            refreshFolderTree();
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
            refreshFolderTree();
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
    
    /**
     * Refresh the mail folder tree
     */
    private void refreshFolderTree() {
        MailClientHandler clientHandler = new MailClientHandler(_client, "Refreshing folder tree") {
            public void runSession() throws IOException, MailException {
                // Open a connection to the IMAP server, and retrieve
                // the folder tree as a list of delimited items
                String delim = "";
                Vector folderList = null;
                Vector folderItemList = new Vector();
                ImapClient iclient = (ImapClient)_client;
                try {
                    delim = iclient.getFolderDelim();
                    folderList = iclient.getFolderList("");
                    for(int i=0;i<folderList.size();i++)
                        folderItemList.addElement(iclient.getFolderItem((String)folderList.elementAt(i)));
                } catch (MailException exp) {
                    folderList = null;
                    throw exp;
                }
                if(folderList == null) return;
        
                // Update the folder tree
                synchronized(Application.getEventLock()) {
                    generateFolderTree(folderItemList);
                }

                // Save the results to the cache
                AccountCache acctCache =
                    new AccountCache(_client.getAcctConfig().getAcctName());
                acctCache.saveFolderList(folderItemList);
            }
        };
        clientHandler.start();
    }
}

