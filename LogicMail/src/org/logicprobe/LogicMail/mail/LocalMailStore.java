/*-
 * Copyright (c) 2008, Derek Konigsberg
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
package org.logicprobe.LogicMail.mail;

import java.io.IOException;
import java.util.Date;
import java.util.Hashtable;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

import net.rim.device.api.system.EventLogger;
import net.rim.device.api.system.UnsupportedOperationException;

import org.logicprobe.LogicMail.AppInfo;
import org.logicprobe.LogicMail.conf.GlobalConfig;
import org.logicprobe.LogicMail.conf.MailSettings;
import org.logicprobe.LogicMail.message.MessageFlags;
import org.logicprobe.LogicMail.message.MimeMessagePart;
import org.logicprobe.LogicMail.util.ThreadQueue;

/**
 * This class manages local mail folders on the device.
 * It should only have a single instance, and does not
 * currently have any user configuration.
 */
public class LocalMailStore extends AbstractMailStore {
    private GlobalConfig globalConfig;
    private FolderTreeItem rootFolder;
    private FolderTreeItem outboxFolder;
    private FolderTreeItem[] localFolders;
    private ThreadQueue threadQueue;
    private Hashtable folderMaildirMap;
    
    public LocalMailStore() {
        super();
        globalConfig = MailSettings.getInstance().getGlobalConfig();
        threadQueue = new ThreadQueue();
        folderMaildirMap = new Hashtable();
        
        // Build the local folder tree, which matches a fixed layout for now.
        // Eventually it should be partially editable by the user.
        // This is also the only folder tree that should lack an INBOX.
        // The "Outbox" folder is marked to prevent the user from being able
        // to append messages to it.  However, it is a special mailbox, and the
        // object model can and does append messages to it as a spool.
        
        outboxFolder = new FolderTreeItem(rootFolder, "Outbox", "Outbox", "/", true, false);
        localFolders = new FolderTreeItem[] {
                new FolderTreeItem(rootFolder, "Drafts", "Drafts", "/", true, true),
                new FolderTreeItem(rootFolder, "Sent", "Sent", "/", true, true),
                new FolderTreeItem(rootFolder, "Trash", "Trash", "/", true, true)
        };
        
        rootFolder = new FolderTreeItem("", "", "");
    }

    public void shutdown(boolean wait) {
    	threadQueue.shutdown(wait);
    }

    public boolean isLocal() {
        return true;
    }

    public boolean hasFolders() {
        return true;
    }

    public boolean hasMessageParts() {
    	return false;
    }
    
    public boolean hasFlags() {
        return true;
    }

    public boolean hasAppend() {
        return true;
    }

    public boolean hasCopy() {
    	return true;
    }
    
    public boolean hasUndelete() {
        return true;
    }

    public boolean hasExpunge() {
        return true;
    }
    
    public FolderTreeRequest createFolderTreeRequest() {
        LocalFolderTreeRequest request = new LocalFolderTreeRequest(this);
        return request;
    }

    public FolderExpungeRequest createFolderExpungeRequest(FolderTreeItem folder) {
        LocalFolderExpungeRequest request = new LocalFolderExpungeRequest(this, folder);
        return request;
    }
    
    public FolderStatusRequest createFolderStatusRequest(FolderTreeItem[] folders) {
        LocalFolderStatusRequest request = new LocalFolderStatusRequest(this, folders);
        return request;
    }
    
    public FolderMessagesRequest createFolderMessagesRangeRequest(FolderTreeItem folder, MessageToken firstToken, int increment) {
    	throw new UnsupportedOperationException("Not yet implemented");
	}

	public FolderMessagesRequest createFolderMessagesSetRequest(FolderTreeItem folder, MessageToken[] messageTokens, boolean flagsOnly) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	public FolderMessagesRequest createFolderMessagesSetByIndexRequest(FolderTreeItem folder, int[] messageIndices) {
        throw new UnsupportedOperationException("Not yet implemented");
	}
	
    public FolderMessagesRequest createFolderMessagesRecentRequest(FolderTreeItem folder, boolean flagsOnly) {
        LocalFolderMessagesRequest request = new LocalFolderMessagesRequest(this, folder, flagsOnly);
        return request;
    }
    
    public MessageRequest createMessageRequest(MessageToken messageToken, boolean useLimits) {
        LocalMessageRequest request = new LocalMessageRequest(this, messageToken, useLimits);
        return request;
    }

    public MessageRequest createMessagePartsRequest(MessageToken messageToken, MimeMessagePart[] messageParts) {
    	throw new UnsupportedOperationException();
    }
    
    public MessageFlagChangeRequest createMessageFlagChangeRequest(
            MessageToken messageToken,
            MessageFlags messageFlags,
            boolean addOrRemove) {
        
        LocalMessageFlagChangeRequest request = new LocalMessageFlagChangeRequest(
                this, messageToken, messageFlags.clone(), addOrRemove);
        return request;
    }
    
    public MessageFlagChangeRequest createMessageFlagChangeRequest(
            MessageToken[] messageTokens,
            MessageFlags messageFlags,
            boolean addOrRemove) {
        
        LocalMessageFlagChangeRequest request = new LocalMessageFlagChangeRequest(
                this, messageTokens, messageFlags.clone(), addOrRemove);
        return request;
    }
    
    public MessageRangeFlagChangeRequest createMessageRangeFlagChangeRequest(
            FolderTreeItem folder,
            Date startDate,
            MessageFlags messageFlags,
            boolean addOrRemove) {
        
        LocalMessageRangeFlagChangeRequest request = new LocalMessageRangeFlagChangeRequest(
                this, folder, startDate, messageFlags.clone(), addOrRemove);
        return request;
    }
    
    public MessageAppendRequest createMessageAppendRequest(FolderTreeItem folder, String rawMessage, MessageFlags initialFlags) {
        LocalMessageAppendRequest request = new LocalMessageAppendRequest(
                this, folder, rawMessage, initialFlags);
        return request;
    }
    
    public MessageCopyRequest createMessageCopyRequest(MessageToken messageToken, FolderTreeItem destinationFolder) {
        LocalMessageCopyRequest request = new LocalMessageCopyRequest(this, messageToken, destinationFolder);
        return request;
    }
    
    public void processRequest(MailStoreRequest request) {
        if(!(request instanceof LocalMailStoreRequest)) {
            throw new IllegalArgumentException();
        }
        LocalMailStoreRequest localRequest = (LocalMailStoreRequest)request;
        
        if(localRequest instanceof LocalFolderTreeRequest) {
            // This is a bad special-case for startup, where this initializes
            // the folder tree within the local mail store.  It is only called
            // once during the lifetime of the application, and it simply is
            // not worth the effort to implement a generic solution to the
            // problem.
            localRequest.run();
        }
        else {
            threadQueue.invokeLater((LocalMailStoreRequest)request);
        }
    }
    
    GlobalConfig getGlobalConfig() {
        return globalConfig;
    }
    
    FolderTreeItem getRootFolder() {
        return rootFolder;
    }
    
    FolderTreeItem getOutboxFolder() {
        return outboxFolder;
    }
    
    FolderTreeItem[] getLocalFolders() {
        return localFolders;
    }
    
    /**
     * Gets the matching folder tree item for the parameter.
     * This method ensures that we are working with a FolderTreeItem object
     * owned by this mail store, even if the provided parameter is a
     * separately created object with similar properties.
     * 
     * @param folderPath The folder path.
     * @return The matching folder tree item.
     */
    FolderTreeItem getMatchingFolderTreeItem(String folderPath) {
        FolderTreeItem[] localFolders = rootFolder.children();
        FolderTreeItem requestFolder = null;
        for (int i = 0; i < localFolders.length; i++) {
        	if(localFolders[i].getPath().equals(folderPath)) {
        		requestFolder = localFolders[i];
        		break;
        	}
        }
    	return requestFolder;
    }
    
    MaildirFolder getMaildirFolder(FolderTreeItem requestFolder) {
        MaildirFolder maildirFolder;
        if(folderMaildirMap.containsKey(requestFolder)) {
            maildirFolder = (MaildirFolder)folderMaildirMap.get(requestFolder);
        }
        else {
            String folderUrl = globalConfig.getLocalDataLocation() + "local/";
            try {
                FileConnection fileConnection = (FileConnection)Connector.open(folderUrl);
                if(!fileConnection.exists()) {
                    fileConnection.mkdir();
                }
                fileConnection.close();
            } catch (IOException e) {
                EventLogger.logEvent(AppInfo.GUID, ("Error preparing root path: " + e.toString()).getBytes(), EventLogger.ERROR);
            }
            
            StringBuffer buf = new StringBuffer();
            buf.append(folderUrl);
            buf.append(requestFolder.getPath());
            maildirFolder = new MaildirFolder(requestFolder.getPath(), buf.toString());
            folderMaildirMap.put(requestFolder, maildirFolder);
        }
        return maildirFolder;
    }
}
