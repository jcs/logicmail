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

import net.rim.device.api.system.UnsupportedOperationException;

import org.logicprobe.LogicMail.conf.AccountConfig;
import org.logicprobe.LogicMail.message.FolderMessage;
import org.logicprobe.LogicMail.message.Message;

public class NetworkMailStore extends AbstractMailStore {
	private IncomingMailClient client;
	private IncomingMailConnectionHandler connectionHandler;
	private AccountConfig accountConfig;
	
	public NetworkMailStore(AccountConfig accountConfig) {
		super();
		this.client = MailClientFactory.createMailClient(accountConfig);
		this.accountConfig = accountConfig;
		this.connectionHandler = new IncomingMailConnectionHandler(client);
		this.connectionHandler.setListener(new MailConnectionHandlerListener() {
			public void mailConnectionRequestComplete(int type, Object result) {
				connectionHandler_mailConnectionRequestComplete(type, result);
			}
		});
		this.connectionHandler.start();
	}

	/**
	 * Gets the account configuration associated with this network mail store.
	 * 
	 * @return Account configuration.
	 */
	public AccountConfig getAccountConfig() {
		return this.accountConfig;
	}
	
	public void shutdown(boolean wait) {
		connectionHandler.shutdown(wait);
	}

	/**
	 * Restarts the mail connection handler thread.
	 */
	public void restart() {
		if(!connectionHandler.isRunning()) {
			connectionHandler.start();
		}
	}
	
	public boolean isLocal() {
		return false;
	}

	public boolean hasFolders() {
		return client.hasFolders();
	}

	public boolean hasFlags() {
		// Fix this kludge later
		return (client instanceof org.logicprobe.LogicMail.mail.imap.ImapClient);
	}

	public boolean hasUndelete() {
		return client.hasUndelete();
	}

	public boolean isConnected() {
		return client.isConnected();
	}

	public void requestFolderTree() {
		connectionHandler.addRequest(IncomingMailConnectionHandler.REQUEST_FOLDER_TREE, new Object[] { });
	}

	public void requestFolderStatus(FolderTreeItem root) {
		connectionHandler.addRequest(IncomingMailConnectionHandler.REQUEST_FOLDER_STATUS, new Object[] { root });
	}

	public void requestFolderMessagesRange(FolderTreeItem folder, int firstIndex, int lastIndex) {
		connectionHandler.addRequest(
				IncomingMailConnectionHandler.REQUEST_FOLDER_MESSAGES_RANGE,
				new Object[] { folder, new Integer(firstIndex), new Integer(lastIndex) });
	}

	public void requestFolderMessagesSet(FolderTreeItem folder, int[] indices) {
		throw new UnsupportedOperationException("Not yet implemented");
	}
	
	public void requestFolderMessagesRecent(FolderTreeItem folder, int count) {
		connectionHandler.addRequest(
				IncomingMailConnectionHandler.REQUEST_FOLDER_MESSAGES_RECENT,
				new Object[] { folder, new Integer(count) });
	}
	
	public void requestMessage(FolderTreeItem folder, FolderMessage folderMessage) {
		connectionHandler.addRequest(IncomingMailConnectionHandler.REQUEST_MESSAGE, new Object[] { folder, folderMessage });
	}

	public void requestMessageDelete(FolderTreeItem folder, FolderMessage folderMessage) {
		connectionHandler.addRequest(IncomingMailConnectionHandler.REQUEST_MESSAGE_DELETE, new Object[] { folder, folderMessage });
	}

	public void requestMessageUndelete(FolderTreeItem folder, FolderMessage folderMessage) {
		if(!client.hasUndelete()) {
			throw new UnsupportedOperationException();
		}
		connectionHandler.addRequest(IncomingMailConnectionHandler.REQUEST_MESSAGE_UNDELETE, new Object[] { folder, folderMessage });
	}
	
	private void connectionHandler_mailConnectionRequestComplete(int type, Object result) {
		Object[] results;
		switch(type) {
		case IncomingMailConnectionHandler.REQUEST_FOLDER_TREE:
			fireFolderTreeUpdated((FolderTreeItem)result);
			break;
		case IncomingMailConnectionHandler.REQUEST_FOLDER_STATUS:
			fireFolderStatusChanged((FolderTreeItem)result);
			break;
		case IncomingMailConnectionHandler.REQUEST_FOLDER_MESSAGES_RANGE:
		case IncomingMailConnectionHandler.REQUEST_FOLDER_MESSAGES_SET:
		case IncomingMailConnectionHandler.REQUEST_FOLDER_MESSAGES_RECENT:
			results = (Object[])result;
			fireFolderMessagesAvailable((FolderTreeItem)results[0], (FolderMessage[])results[1]);
			break;
		case IncomingMailConnectionHandler.REQUEST_MESSAGE:
			results = (Object[])result;
			fireMessageAvailable((FolderTreeItem)results[0], (FolderMessage)results[1], (Message)results[2]);
			break;
		case IncomingMailConnectionHandler.REQUEST_MESSAGE_DELETE:
			results = (Object[])result;
			fireMessageDeleted((FolderTreeItem)results[0], (FolderMessage)results[1]);
			break;
		case IncomingMailConnectionHandler.REQUEST_MESSAGE_UNDELETE:
			results = (Object[])result;
			fireMessageUndeleted((FolderTreeItem)results[0], (FolderMessage)results[1]);
			break;
		}
	}
}
