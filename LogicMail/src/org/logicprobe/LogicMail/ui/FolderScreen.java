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
import java.util.Vector;
import java.io.IOException;

/**
 * Mail folder listing
 * (may only be available with IMAP)
 */
public class FolderScreen extends BaseScreen implements TreeFieldCallback {
    private TreeField treeField;
    private AccountConfig acctCfg;
    private MailClient client;
    private MailSettings mailSettings;

    public FolderScreen(MailSettings mailSettings, MailClient client, AccountConfig acctCfg) {
        super("Folders");
        this.mailSettings = mailSettings;
        this.client = client;
        this.acctCfg = acctCfg;

        treeField = new TreeField(this, Field.FOCUSABLE);
        treeField.setEmptyString("No folders", 0);
        treeField.setDefaultExpanded(true);
        treeField.setIndentWidth(20);
        
        add(treeField);

        refreshFolderTree();
    }

    protected boolean onSavePrompt() {
        return true;
    }

    public boolean onClose() {
        if(client.isConnected()) {
            if(Dialog.ask(Dialog.D_YES_NO, "Disconnect from server?") == Dialog.YES) {
                try { client.close(); } catch (Exception exp) { }
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
            UiApplication.getUiApplication().pushScreen(new MailboxScreen(mailSettings, client, (MailClient.FolderItem)cookie));
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
            String text = item.name + " (" + Integer.toString(item.msgCount) + ")";
            graphics.drawText( text, indent, y, Graphics.ELLIPSIS, width );
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
    
    /**
     * Current a test method for server I/O,
     * this will eventually hook into some
     * app-wide connection instance and just
     * refresh either the folder tree, or
     * message counts for mailboxes within
     * the tree.
     */
    private void refreshFolderTree() {
        Thread thread = new Thread() {
            public void run() {
        // Open a connection to the IMAP server, and retrieve
        // the folder tree as a list of delimited items
        String delim = "";
        Vector folderList = null;
        Vector folderItemList = new Vector();
        ImapClient iclient = (ImapClient)client;
        try {
            if(!iclient.isConnected()) iclient.open();
            delim = iclient.getFolderDelim();
            folderList = iclient.getFolderList("");
            for(int i=0;i<folderList.size();i++)
                folderItemList.addElement(iclient.getFolderItem((String)folderList.elementAt(i)));
        } catch (IOException exp) {
            System.out.println(exp);
            try { iclient.close(); } catch (Exception exp2) { }
        } catch (MailException exp) {
            System.out.println("Protocol error: " + exp);
            folderList = null;
        } catch (Exception exp) {
            System.out.println("Unknown error: " + exp);
            try { iclient.close(); } catch (Exception exp2) { }
        }
        if(folderList == null) return;
        
        // Update the folder tree
        synchronized(Application.getEventLock()) {
            treeField.deleteAll();
            int parent = 0;
            int node = 0;
            int depth = 0;
            int olddepth = 0;
            Vector path = new Vector();
            for(int i=0;i<folderList.size();i++) {
                String name = (String)folderList.elementAt(i);
                // Find the tree depth of this folder
                depth = 0;
                int pos = 0;
                while(pos < name.length()) {
                    if(name.indexOf(delim, pos) != -1) { depth++; }
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
                MailClient.FolderItem item = (MailClient.FolderItem)folderItemList.elementAt(i);
                node = treeField.addChildNode(parent, item);
                if(depth != olddepth)
                    olddepth = depth;
            }
            treeField.setDirty(true);
        }
        }
    };
    thread.start();
    }
}

