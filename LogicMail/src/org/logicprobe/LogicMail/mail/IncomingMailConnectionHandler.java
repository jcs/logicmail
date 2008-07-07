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

import net.rim.device.api.system.UnsupportedOperationException;

import org.logicprobe.LogicMail.message.FolderMessage;
import org.logicprobe.LogicMail.message.Message;

public class IncomingMailConnectionHandler extends AbstractMailConnectionHandler {
	private IncomingMailClient incomingClient;
	
	// The various mail store requests, mirroring the
	// "requestXXXX()" methods from AbstractMailStore
	public static final int REQUEST_FOLDER_TREE             = 10;
	public static final int REQUEST_FOLDER_STATUS           = 11;
	public static final int REQUEST_FOLDER_MESSAGES_RANGE   = 12;
	public static final int REQUEST_FOLDER_MESSAGES_SET     = 13;
	public static final int REQUEST_FOLDER_MESSAGES_RECENT  = 14;
	public static final int REQUEST_MESSAGE                 = 20;
	public static final int REQUEST_MESSAGE_DELETE          = 21;
	public static final int REQUEST_MESSAGE_UNDELETE        = 22;
	
	public IncomingMailConnectionHandler(IncomingMailClient client) {
		super(client);
		this.incomingClient = client;
	}

	/**
	 * Handles a specific request during the REQUESTS state.
	 * Subclasses should implement this to dispatch requests for all
	 * the request types they know how to handle.
	 * 
	 * @param type Type identifier for the request.
	 * @param params Parameters for the request.
     * @throw IOException on I/O errors
     * @throw MailException on protocol errors
	 */
	protected void handleRequest(int type, Object[] params) throws IOException, MailException {
		switch(type) {
		case REQUEST_FOLDER_TREE:
			handleRequestFolderTree();
			break;
		case REQUEST_FOLDER_STATUS:
			handleRequestFolderStatus((FolderTreeItem)params[0]);
			break;
		case REQUEST_FOLDER_MESSAGES_RANGE:
			handleRequestFolderMessagesRange(
					(FolderTreeItem)params[0],
					((Integer)params[1]).intValue(),
					((Integer)params[2]).intValue());
			break;
		case REQUEST_FOLDER_MESSAGES_SET:
			handleRequestFolderMessagesSet(
					(FolderTreeItem)params[0],
					(int[])params[1]);
			break;
		case REQUEST_FOLDER_MESSAGES_RECENT:
			handleRequestFolderMessagesRecent(
					(FolderTreeItem)params[0],
					((Integer)params[1]).intValue());
			break;
		case REQUEST_MESSAGE:
			handleRequestMessage((FolderTreeItem)params[0], (FolderMessage)params[1]);
			break;
		case REQUEST_MESSAGE_DELETE:
			handleRequestMessageDelete((FolderTreeItem)params[0], (FolderMessage)params[1]);
			break;
		case REQUEST_MESSAGE_UNDELETE:
			handleRequestMessageUndelete((FolderTreeItem)params[0], (FolderMessage)params[1]);
			break;
		}
	}

	private void handleRequestFolderTree() throws IOException, MailException {
		FolderTreeItem root = incomingClient.getFolderTree();

		MailConnectionHandlerListener listener = getListener();
		if(root != null && listener != null) {
			listener.mailConnectionRequestComplete(REQUEST_FOLDER_TREE, root);
		}
	}
	
	private void handleRequestFolderStatus(FolderTreeItem root) throws IOException, MailException {
		incomingClient.refreshFolderStatus(root);
		
		MailConnectionHandlerListener listener = getListener();
		if(listener != null) {
			listener.mailConnectionRequestComplete(REQUEST_FOLDER_STATUS, root);
		}
	}
	
	private void handleRequestFolderMessagesRange(FolderTreeItem folder, int firstIndex, int lastIndex) throws IOException, MailException {
		checkActiveFolder(folder);
		
		FolderMessage[] messages = incomingClient.getFolderMessages(firstIndex, lastIndex);
		
		MailConnectionHandlerListener listener = getListener();
		if(messages != null && messages.length > 0 && listener != null) {
			listener.mailConnectionRequestComplete(REQUEST_FOLDER_MESSAGES_RANGE, new Object[] { folder, messages });
		}
	}
	
	private void handleRequestFolderMessagesSet(FolderTreeItem folder, int[] indices) throws IOException, MailException {
		throw new UnsupportedOperationException("Not yet implemented");
//		checkActiveFolder(folder);
//		
//		FolderMessage[] messages = incomingClient.getFolderMessages(firstIndex, lastIndex);
//		
//		MailConnectionHandlerListener listener = getListener();
//		if(messages != null && messages.length > 0 && listener != null) {
//			listener.mailConnectionRequestComplete(REQUEST_FOLDER_MESSAGES_SET, new Object[] { folder, messages });
//		}
	}
	
	private void handleRequestFolderMessagesRecent(FolderTreeItem folder, int count) throws IOException, MailException {
		checkActiveFolder(folder);
		int msgCount = incomingClient.getActiveFolder().getMsgCount();
        int firstIndex = Math.max(1, msgCount - count);
        
		FolderMessage[] messages = incomingClient.getFolderMessages(firstIndex, msgCount);
		
		MailConnectionHandlerListener listener = getListener();
		if(messages != null && messages.length > 0 && listener != null) {
			listener.mailConnectionRequestComplete(REQUEST_FOLDER_MESSAGES_RECENT, new Object[] { folder, messages });
		}
	}
	
	private void handleRequestMessage(FolderTreeItem folder, FolderMessage folderMessage) throws IOException, MailException {
		checkActiveFolder(folder);
		
		Message message = incomingClient.getMessage(folderMessage);
		
		MailConnectionHandlerListener listener = getListener();
		if(message != null && listener != null) {
			listener.mailConnectionRequestComplete(REQUEST_MESSAGE, new Object[] { folder, folderMessage, message });
		}
	}
	
	private void handleRequestMessageDelete(FolderTreeItem folder, FolderMessage folderMessage) throws IOException, MailException {
		checkActiveFolder(folder);
		
		incomingClient.deleteMessage(folderMessage);
		
		MailConnectionHandlerListener listener = getListener();
		if(listener != null) {
			listener.mailConnectionRequestComplete(REQUEST_MESSAGE_DELETE, new Object[] { folder, folderMessage });
		}
	}
	
	private void handleRequestMessageUndelete(FolderTreeItem folder, FolderMessage folderMessage) throws IOException, MailException {
		checkActiveFolder(folder);
		
		incomingClient.undeleteMessage(folderMessage);

		MailConnectionHandlerListener listener = getListener();
		if(listener != null) {
			listener.mailConnectionRequestComplete(REQUEST_MESSAGE_UNDELETE, new Object[] { folder, folderMessage });
		}
	}
	
	private void checkActiveFolder(FolderTreeItem requestFolder) throws IOException, MailException {
		if(incomingClient.getActiveFolder() == null || !incomingClient.getActiveFolder().getPath().equals(requestFolder.getPath())) {
			incomingClient.setActiveFolder(requestFolder);
		}
	}
}
