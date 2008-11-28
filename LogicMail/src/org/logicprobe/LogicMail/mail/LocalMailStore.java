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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Hashtable;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

import org.logicprobe.LogicMail.conf.MailSettings;
import org.logicprobe.LogicMail.message.FolderMessage;
import org.logicprobe.LogicMail.message.Message;
import org.logicprobe.LogicMail.message.MessageFlags;
import org.logicprobe.LogicMail.message.MessagePart;
import org.logicprobe.LogicMail.util.MailMessageParser;
import org.logicprobe.LogicMail.util.ThreadQueue;


/**
 * This class manages local mail folders on the device.
 * It should only have a single instance, and does not
 * currently have any user configuration.
 */
public class LocalMailStore extends AbstractMailStore {
	private static String urlRoot = "file:///";
    private FolderTreeItem rootFolder;
    private ThreadQueue threadQueue;
    private Hashtable folderMaildirMap;
    
    public LocalMailStore() {
        super();

        threadQueue = new ThreadQueue();
        folderMaildirMap = new Hashtable();
        
        // Build the local folder tree, which matches a fixed layout for now.
        // Eventually it should be partially editable by the user.
        // This is also the only folder tree that should lack an INBOX.
        // The "Outbox" folder is marked to prevent the user from being able
        // to append messages to it.  However, it is a special mailbox, and the
        // object model can and does append messages to it as a spool.
        rootFolder = new FolderTreeItem("", "", "");
        rootFolder.addChild(new FolderTreeItem(rootFolder, "Outbox", "Outbox", "/", true, false));
        rootFolder.addChild(new FolderTreeItem(rootFolder, "Drafts", "Drafts", "/", true, true));
        rootFolder.addChild(new FolderTreeItem(rootFolder, "Sent", "Sent", "/", true, true));
        rootFolder.addChild(new FolderTreeItem(rootFolder, "Trash", "Trash", "/", true, true));
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

    public boolean hasFlags() {
        return true;
    }

    public boolean hasAppend() {
        return true;
    }

    public boolean hasUndelete() {
        return false;
    }

    public void requestFolderTree() {
        fireFolderTreeUpdated(rootFolder);
    }

    public void requestFolderStatus(FolderTreeItem[] folders) {
        // Make every entry in the provided array match the local folder
        // objects just in case they do not.  Then, fire change events
        // for all those folders.  The actual data to answer this
        // request should already be available.
        FolderTreeItem[] localFolders = rootFolder.children();

        for (int i = 0; i < folders.length; i++) {
            for (int j = 0; j < localFolders.length; j++) {
                if (folders[i].getPath().equals(localFolders[j].getPath())) {
                    folders[i] = localFolders[j];

                    break;
                }
            }
        }

        for (int i = 0; i < folders.length; i++) {
            fireFolderStatusChanged(folders[i]);
        }
    }

    public void requestFolderMessagesRange(FolderTreeItem folder, int firstIndex, int lastIndex) {
        // TODO Auto-generated method stub
    }

    public void requestFolderMessagesSet(FolderTreeItem folder, int[] indices) {
        // TODO Auto-generated method stub
    }

    public void requestFolderMessagesRecent(FolderTreeItem folder) {
        FolderTreeItem requestFolder = getMatchingFolderTreeItem(folder);
        
        if(requestFolder != null) {
        	threadQueue.invokeLater(new RequestFolderMessagesRecentRunnable(requestFolder));
        }
    }

    private class RequestFolderMessagesRecentRunnable extends MaildirRunnable {
    	public RequestFolderMessagesRecentRunnable(FolderTreeItem requestFolder) {
    		super(requestFolder);
    	}
    	
		public void run() {
        	FolderMessage[] folderMessages = null;
        	try {
        		maildirFolder.open();
        		folderMessages = maildirFolder.getFolderMessages();
        		maildirFolder.close();
        	} catch (IOException e) {
        		System.err.println("Unable to read folder: " + e.toString());
        	}
        	
        	if(folderMessages != null) {
        		fireFolderMessagesAvailable(requestFolder, folderMessages);
        	}
		}
    }
    
    public void requestMessage(FolderTreeItem folder, FolderMessage folderMessage) {
        FolderTreeItem requestFolder = getMatchingFolderTreeItem(folder);
        
        if(requestFolder != null && folderMessage != null) {
        	threadQueue.invokeLater(new RequestMessageRunnable(requestFolder, folderMessage));
        }
    }

    private class RequestMessageRunnable extends MaildirRunnable {
    	private FolderMessage folderMessage;
    	
    	public RequestMessageRunnable(FolderTreeItem requestFolder, FolderMessage folderMessage) {
    		super(requestFolder);
    		this.folderMessage = folderMessage;
    	}
    	
		public void run() {
			String messageSource = null;
			Message message = null;
        	try {
        		maildirFolder.open();
        		messageSource = maildirFolder.getMessageSource(folderMessage);
        		maildirFolder.close();
        		
        		// Parse the message source
                MessagePart rootPart = MailMessageParser.parseRawMessage(new ByteArrayInputStream(messageSource.getBytes()));
                message = new Message(folderMessage.getEnvelope(), rootPart);
        	} catch (IOException e) {
        		System.err.println("Unable to read message: " + e.toString());
        	}
        	
        	if(message != null && messageSource != null) {
        		fireMessageAvailable(requestFolder, folderMessage, message, messageSource);
        	}
		}
    }

    public void requestMessageDelete(FolderTreeItem folder, FolderMessage folderMessage) {
        // TODO Auto-generated method stub
    }

    public void requestMessageUndelete(FolderTreeItem folder, FolderMessage folderMessage) {
        // TODO Auto-generated method stub
    }

    public void requestMessageAnswered(FolderTreeItem folder, FolderMessage folderMessage) {
        // TODO Auto-generated method stub
    }

    public void requestMessageAppend(FolderTreeItem folder, String rawMessage, MessageFlags initialFlags) {
        FolderTreeItem requestFolder = getMatchingFolderTreeItem(folder);
        
        if(requestFolder != null && rawMessage != null && rawMessage.length() > 0 && initialFlags != null) {
        	threadQueue.invokeLater(new RequestMessageAppendRunnable(requestFolder, rawMessage, initialFlags));
        }
    }

    private class RequestMessageAppendRunnable extends MaildirRunnable {
    	private String rawMessage;
    	private MessageFlags initialFlags;
    	
    	public RequestMessageAppendRunnable(FolderTreeItem requestFolder, String rawMessage, MessageFlags initialFlags) {
    		super(requestFolder);
    		this.rawMessage = rawMessage;
    		this.initialFlags = initialFlags;
    	}
    	
		public void run() {
        	FolderMessage folderMessage = null;
        	try {
        		maildirFolder.open();
        		folderMessage = maildirFolder.appendMessage(rawMessage, initialFlags);
        		maildirFolder.close();
        	} catch (IOException e) {
        		System.err.println("Unable to read folder: " + e.toString());
        	}
        	
        	if(folderMessage != null) {
        		fireFolderMessagesAvailable(requestFolder, new FolderMessage[] { folderMessage });
        	}
		}
    }
    
    /**
     * Gets the matching folder tree item for the parameter.
     * This method ensures that we are working with a FolderTreeItem object
     * owned by this mail store, even if the provided parameter is a
     * separately created object with similar properties.
     * 
     * @param folder The folder parameter.
     * @return The matching folder tree item.
     */
    private FolderTreeItem getMatchingFolderTreeItem(FolderTreeItem folder) {
        FolderTreeItem[] localFolders = rootFolder.children();
        FolderTreeItem requestFolder = null;
        for (int i = 0; i < localFolders.length; i++) {
        	if(localFolders[i].getPath().equals(folder.getPath())) {
        		requestFolder = localFolders[i];
        		break;
        	}
        }
    	return requestFolder;
    }
    
    private abstract class MaildirRunnable implements Runnable {
    	protected FolderTreeItem requestFolder;
    	protected MaildirFolder maildirFolder;
    	
    	public MaildirRunnable(FolderTreeItem requestFolder) {
    		this.requestFolder = requestFolder;

    		if(folderMaildirMap.containsKey(requestFolder)) {
        		maildirFolder = (MaildirFolder)folderMaildirMap.get(requestFolder);
        	}
        	else {
        		String folderUrl = urlRoot + MailSettings.getInstance().getGlobalConfig().getLocalDataLocation() + "/LogicMail";
        		try {
	        		FileConnection fileConnection = (FileConnection)Connector.open(folderUrl + '/');
	        		if(!fileConnection.exists()) {
	        			fileConnection.mkdir();
	        		}
	        		fileConnection.close();
        		} catch (IOException e) {
        			System.err.println("Error preparing root path: " + e.toString());
        		}
        		
            	StringBuffer buf = new StringBuffer();
            	buf.append(folderUrl);
            	buf.append('/');
            	buf.append(requestFolder.getPath());
        		maildirFolder = new MaildirFolder(buf.toString());
        		folderMaildirMap.put(requestFolder, maildirFolder);
        	}
    	}
    }
}
