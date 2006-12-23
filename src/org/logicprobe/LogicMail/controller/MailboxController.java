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

package org.logicprobe.LogicMail.controller;

import java.io.IOException;
import java.util.Vector;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.Dialog;
import org.logicprobe.LogicMail.cache.AccountCache;
import org.logicprobe.LogicMail.conf.AccountConfig;
import org.logicprobe.LogicMail.conf.MailSettings;
import org.logicprobe.LogicMail.mail.FolderTreeItem;
import org.logicprobe.LogicMail.mail.ImapClient;
import org.logicprobe.LogicMail.mail.MailClient;
import org.logicprobe.LogicMail.mail.MailException;
import org.logicprobe.LogicMail.message.Message;
import org.logicprobe.LogicMail.mail.PopClient;
import org.logicprobe.LogicMail.ui.FolderScreen;
import org.logicprobe.LogicMail.ui.MailClientHandler;
import org.logicprobe.LogicMail.ui.MailboxScreen;
import org.logicprobe.LogicMail.util.Observable;

/**
 * Controller for folder and mailbox screens
 */
public class MailboxController extends Controller implements Observable {
    private static MailboxController instance;
    private MailClient client;
    private MailSettings mailSettings;
    private FolderTreeItem folderRoot;
    private Vector msgEnvList;
    
    /** Creates a new instance of MailboxController */
    private MailboxController() {
        mailSettings = MailSettings.getInstance();
        client = null;
        folderRoot = null;
        msgEnvList = null;
    }
    
    public static synchronized MailboxController getInstance() {
        if(instance == null)
            instance = new MailboxController();
        return instance;
    }

    /**
     * Open an selected account.
     * If the account is IMAP, then we go to the folder tree.
     * If the account is POP, then go straight to the inbox.
     * @param acctConfig Configuration of the account to open
     */
    public void openAccount(AccountConfig acctConfig) {
        client = MailClient.getNewClient(acctConfig);
        if(client.hasFolders()) {
            UiApplication.getUiApplication().pushScreen(new FolderScreen(client));
        }
        else {
            UiApplication.getUiApplication().pushScreen(new MailboxScreen(client, client.getActiveFolder()));
        }
    }
    
    /**
     * Open a selected folder
     * @param folderItem Item describing the folder to be opened
     */
    public void openFolder(FolderTreeItem folderItem) {
    	UiApplication.getUiApplication().pushScreen(new MailboxScreen(client, folderItem));
    }
    
    public void refreshFolderTree() {
        // This only works for IMAP, so check to be sure
        if(client == null || !(client instanceof ImapClient)) return;
        
        // Invoke a thread to update the folder tree
        MailClientHandler clientHandler = new MailClientHandler(client, "Refreshing folder tree") {
            public void runSession() throws IOException, MailException {
                // Open a connection to the IMAP server, and retrieve
                // the folder tree as a list of delimited items
                FolderTreeItem folderItem;
                try {
                    folderItem = client.getFolderTree();
                } catch (MailException exp) {
                    folderItem = null;
                    throw exp;
                }

                // Save the results to the cache
                AccountCache acctCache =
                    new AccountCache(client.getAcctConfig().getAcctName());
                acctCache.saveFolderTree(folderItem);

                if(folderItem != null) {
                    folderRoot = folderItem;
                    notifyObservers("folders");
                }
            }
        };
        clientHandler.start();
    }
    
    public void refreshMessageList(FolderTreeItem folderItem) {
        if(client == null) return;
        final FolderTreeItem _folderItem = folderItem;
        MailClientHandler clientHandler = new MailClientHandler(client, "Retrieving message list") {
            public void runSession() throws IOException, MailException {
                Vector msgEnvList = null;
                try {
                    client.setActiveFolder(_folderItem);
                    int firstIndex = _folderItem.getMsgCount() - mailSettings.getGlobalConfig().getRetMsgCount();
                    if(firstIndex < 0) firstIndex = 0;

                    // Kludge to remove dependency on getMessageEnvelopes
                    Message.Envelope[] msgEnvArray;
                    msgEnvArray = client.getMessageList(firstIndex, _folderItem.getMsgCount());
                    msgEnvList = new Vector();
                    for(int i=0;i<msgEnvArray.length;i++)
                        msgEnvList.addElement(msgEnvArray[i]);
                } catch (MailException exp) {
                    msgEnvList = null;
                    throw exp;
                }
                if(msgEnvList != null) {
                    MailboxController.this.msgEnvList = msgEnvList;
                    notifyObservers("messages");
                }
            }
        };
        clientHandler.start();
    }

    public void openMessage(FolderTreeItem folderItem, Message.Envelope envelope) {
        MessageController messageController =
                new MessageController(client, folderItem, envelope);
        messageController.viewMessage();
    }

    public boolean checkClose() {
        // Immediately close without prompting if we are on a mailbox screen
        // and using a protocol that supports folders.
        if((UiApplication.getUiApplication().getActiveScreen() instanceof MailboxScreen) &&
           client.hasFolders())
            return true;
        
        // Otherwise we are on the main screen for the account, so prompt
        // before closing the connection
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
    
    
    public MailClient getMailClient() {
        return client;
    }
    
    public FolderTreeItem getFolderRoot() {
        return folderRoot;
    }
    
    public Vector getMsgEnvList() {
        return msgEnvList;
    }
}
