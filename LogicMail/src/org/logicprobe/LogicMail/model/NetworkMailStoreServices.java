/*-
 * Copyright (c) 2010, Derek Konigsberg
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
package org.logicprobe.LogicMail.model;

import java.util.Enumeration;
import java.util.Hashtable;

import net.rim.device.api.util.ToIntHashtable;

import org.logicprobe.LogicMail.conf.AccountConfig;
import org.logicprobe.LogicMail.mail.FolderTreeItem;
import org.logicprobe.LogicMail.mail.MessageToken;
import org.logicprobe.LogicMail.mail.NetworkMailStore;
import org.logicprobe.LogicMail.message.FolderMessage;

public class NetworkMailStoreServices extends MailStoreServices {
    private final NetworkMailStore mailStore;
    private final FolderMessageCache folderMessageCache;
    
    /** Map of FolderTreeItem objects to corresponding request handlers */
    private final Hashtable folderRequestHandlerMap = new Hashtable();
    
    public NetworkMailStoreServices(NetworkMailStore mailStore, FolderMessageCache folderMessageCache) {
        super(mailStore);
        this.mailStore = mailStore;
        this.folderMessageCache = folderMessageCache;
    }

    AccountConfig getAccountConfig() {
        return mailStore.getAccountConfig();
    }
    
    public void restart() {
        mailStore.restart();
    }

    public void setConnected(boolean connected) {
        // Only handle disconnects
        if(connected) { return; }
        
        synchronized(folderRequestHandlerMap) {
            Enumeration e = folderRequestHandlerMap.elements();
            while(e.hasMoreElements()) {
                FolderRequestHandler handler = (FolderRequestHandler)e.nextElement();
                handler.handleDisconnect();
            }
        }
    }
    
    public void requestFolderRefresh(final FolderTreeItem folderTreeItem) {
        FolderRequestHandler handler = getFolderRequestHandler(folderTreeItem);
        handler.requestFolderRefresh();
    }
    
    public void requestMoreFolderMessages(FolderTreeItem folderTreeItem, MessageToken firstToken) {
        FolderRequestHandler handler = getFolderRequestHandler(folderTreeItem);
        int increment = mailStore.getAccountConfig().getFolderMessageIncrement();
        handler.requestMoreFolderMessages(firstToken, increment);
    }
    
    public void removeSavedData(FolderTreeItem[] folderTreeItems) {
        for(int i=0; i<folderTreeItems.length; i++) {
            folderMessageCache.removeFolder(folderTreeItems[i]);
        }
        folderMessageCache.commit();
        super.removeSavedData(folderTreeItems);
    }
    
    protected void handleFolderMessagesAvailable(FolderTreeItem folder, FolderMessage[] messages, boolean flagsOnly) {
        FolderRequestHandler handler = getFolderRequestHandler(folder);
        
        if(flagsOnly) {
            handler.handleFolderMessageFlagsAvailable(messages);
        }
        else {
            handler.handleFolderMessagesAvailable(messages);
        }
    }
    
    protected void handleFolderMessageIndexMapAvailable(FolderTreeItem folder, ToIntHashtable uidIndexMap) {
        FolderRequestHandler handler = getFolderRequestHandler(folder);
        
        handler.handleFolderMessageIndexMapAvailable(uidIndexMap);
    }

    protected void handleFolderExpunged(FolderTreeItem folder, int[] indices) {
        FolderRequestHandler handler = getFolderRequestHandler(folder);
        
        handler.handleFolderExpunged(indices);
    }

    private FolderRequestHandler getFolderRequestHandler(FolderTreeItem folder) {
        FolderRequestHandler handler;
        synchronized(folderRequestHandlerMap) {
            handler = (FolderRequestHandler)folderRequestHandlerMap.get(folder);
            if(handler == null) {
                handler = new FolderRequestHandler(this, mailStore, folderMessageCache, folder);
                folderRequestHandlerMap.put(folder, handler);
            }
        }
        return handler;
    }
}
