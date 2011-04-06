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
import net.rim.device.api.util.ToIntHashtable;

import org.logicprobe.LogicMail.conf.AccountConfig;
import org.logicprobe.LogicMail.message.FolderMessage;
import org.logicprobe.LogicMail.message.MimeMessageContent;
import org.logicprobe.LogicMail.message.MessageFlags;
import org.logicprobe.LogicMail.message.MimeMessagePart;

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
			public void mailConnectionRequestComplete(int type, Object tag, Object result, boolean isFinal) {
				connectionHandler_mailConnectionRequestComplete(type, tag, result, isFinal);
			}
            public void mailConnectionRequestFailed(int type, Object tag, Throwable exception, boolean isFinal) {
                connectionHandler_mailConnectionRequestFailed(type, tag, exception, isFinal);
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

	public boolean hasMessageParts() {
		return client.hasMessageParts();
	}
	
	public boolean hasFlags() {
		return client.hasFlags();
	}
	
	public boolean hasAppend() {
		return client.hasAppend();
	}

	public boolean hasCopy() {
		return client.hasCopy();
	}
	
	public boolean hasUndelete() {
		return client.hasUndelete();
	}

	public boolean hasExpunge() {
	    return client.hasExpunge();
	}
	
	/**
	 * Returns whether the mail store supports retrieval of a full folder
	 * message index-to-UID map.
	 *
	 * @return True if index-to-UID map retrieval is supported, false otherwise
	 * @see #requestFolderMessageIndexMap(FolderTreeItem, MailStoreRequestCallback)
	 */
	public boolean hasFolderMessageIndexMap() {
	    return client.hasFolderMessageIndexMap();
	}
	
	/**
	 * Returns whether the mail store has a locked folder view while connected.
	 * If this method returns true, then an explicit refresh during an existing
	 * connection will not return new data.
	 * @return True if folder contents are locked while connected, false otherwise.
	 */
	public boolean hasLockedFolders() {
	    return client.hasLockedFolders();
	}
	
	public boolean isConnected() {
		return client.isConnected();
	}

    /**
     * Requests that the mail store disconnect from the mail server.
     * <p>
     * Unlike the <code>shutdown(boolean)</code> method, this does not cause
     * the connection handler thread to terminate.  As such, any subsequent
     * request may cause it to reconnect.
     * </p>
     */
    public void requestDisconnect() {
        connectionHandler.addRequest(IncomingMailConnectionHandler.REQUEST_DISCONNECT, new Object[] { }, null);
    }

	public void requestFolderTree(MailStoreRequestCallback callback) {
		connectionHandler.addRequest(IncomingMailConnectionHandler.REQUEST_FOLDER_TREE, new Object[] { }, callback);
	}

	public void requestFolderExpunge(FolderTreeItem folder, MailStoreRequestCallback callback) {
	    connectionHandler.addRequest(IncomingMailConnectionHandler.REQUEST_FOLDER_EXPUNGE, new Object[] { folder }, callback);
	}
	
	public void requestFolderStatus(FolderTreeItem[] folders, MailStoreRequestCallback callback) {
		connectionHandler.addRequest(IncomingMailConnectionHandler.REQUEST_FOLDER_STATUS, new Object[] { folders }, callback);
	}

	public void requestFolderMessagesRange(FolderTreeItem folder, MessageToken firstToken, int increment, MailStoreRequestCallback callback) {
	    if(firstToken == null || increment <= 0) {
	        throw new IllegalArgumentException();
	    }
		connectionHandler.addRequest(
				IncomingMailConnectionHandler.REQUEST_FOLDER_MESSAGES_RANGE,
				new Object[] { folder, firstToken, new Integer(increment) },
				callback);
	}

	public void requestFolderMessagesSet(FolderTreeItem folder, MessageToken[] messageTokens, boolean flagsOnly, MailStoreRequestCallback callback) {
		connectionHandler.addRequest(
				IncomingMailConnectionHandler.REQUEST_FOLDER_MESSAGES_SET,
				new Object[] { folder, messageTokens, new Boolean(flagsOnly) },
				callback);
	}
	
	public void requestFolderMessagesSet(FolderTreeItem folder, int[] messageIndices, MailStoreRequestCallback callback) {
        connectionHandler.addRequest(
                IncomingMailConnectionHandler.REQUEST_FOLDER_MESSAGES_SET,
                new Object[] { folder, messageIndices },
                callback);
	}
	
	public void requestFolderMessagesRecent(FolderTreeItem folder, boolean flagsOnly, MailStoreRequestCallback callback) {
		connectionHandler.addRequest(
				IncomingMailConnectionHandler.REQUEST_FOLDER_MESSAGES_RECENT,
				new Object[] { folder, new Boolean(flagsOnly) },
				callback);
	}
	
	/**
	 * Requests the message UID-to-index map for a particular folder.
     * <p>
     * Successful completion is indicated by a call to
     * {@link FolderListener#folderMessageIndexMapAvailable(FolderMessageIndexMapEvent)}.
     * </p>
     * 
     * @param folder The folder to request a message listing for.
     * @param callback The callback to receive success or failure notifications about the request
	 */
	public void requestFolderMessageIndexMap(FolderTreeItem folder, MailStoreRequestCallback callback) {
        connectionHandler.addRequest(
                IncomingMailConnectionHandler.REQUEST_FOLDER_MESSAGE_INDEX_MAP,
                new Object[] { folder },
                callback);
	}

    /**
     * Requests the message index-to-UID map for a particular folder.
     * <p>
     * Successful completion is indicated by a call to
     * {@link FolderListener#folderMessageIndexMapAvailable(FolderMessageIndexMapEvent)}.
     * </p>
     * 
     * @param folder The folder to request a message listing for.
     */
    public void requestFolderMessageIndexMap(FolderTreeItem folder) {
        requestFolderMessageIndexMap(folder, null);
    }
	
	public void requestMessage(MessageToken messageToken, boolean useLimits, MailStoreRequestCallback callback) {
		connectionHandler.addRequest(IncomingMailConnectionHandler.REQUEST_MESSAGE, new Object[] { messageToken, new Boolean(useLimits) }, callback);
	}

	public void requestMessageParts(MessageToken messageToken, MimeMessagePart[] messageParts, MailStoreRequestCallback callback) {
		connectionHandler.addRequest(IncomingMailConnectionHandler.REQUEST_MESSAGE_PARTS, new Object[] { messageToken, messageParts }, callback);
	}
	
	public void requestMessageDelete(MessageToken messageToken, MailStoreRequestCallback callback) {
		connectionHandler.addRequest(IncomingMailConnectionHandler.REQUEST_MESSAGE_DELETE, new Object[] { messageToken }, callback);
	}

	public void requestMessageUndelete(MessageToken messageToken, MailStoreRequestCallback callback) {
		if(!client.hasUndelete()) {
			throw new UnsupportedOperationException();
		}
		connectionHandler.addRequest(IncomingMailConnectionHandler.REQUEST_MESSAGE_UNDELETE, new Object[] { messageToken }, callback);
	}

	public void requestMessageAnswered(MessageToken messageToken, MailStoreRequestCallback callback) {
		if(!this.hasFlags()) {
			throw new UnsupportedOperationException();
		}
		connectionHandler.addRequest(IncomingMailConnectionHandler.REQUEST_MESSAGE_ANSWERED, new Object[] { messageToken }, callback);
	}
	
    public void requestMessageForwarded(MessageToken messageToken, MailStoreRequestCallback callback) {
        if(!this.hasFlags()) {
            throw new UnsupportedOperationException();
        }
        connectionHandler.addRequest(IncomingMailConnectionHandler.REQUEST_MESSAGE_FORWARDED, new Object[] { messageToken }, callback);
    }

    public void requestMessageSeen(MessageToken messageToken, MailStoreRequestCallback callback) {
        if(!this.hasFlags()) {
            throw new UnsupportedOperationException();
        }
        connectionHandler.addRequest(IncomingMailConnectionHandler.REQUEST_MESSAGE_SEEN, new Object[] { messageToken }, callback);
    }
    
    public void requestMessageUnseen(MessageToken messageToken, MailStoreRequestCallback callback) {
        if(!this.hasFlags()) {
            throw new UnsupportedOperationException();
        }
        connectionHandler.addRequest(IncomingMailConnectionHandler.REQUEST_MESSAGE_UNSEEN, new Object[] { messageToken }, callback);
    }

	public void requestMessageAppend(FolderTreeItem folder, String rawMessage, MessageFlags initialFlags, MailStoreRequestCallback callback) {
		if(!this.hasAppend()) {
			throw new UnsupportedOperationException();
		}
		connectionHandler.addRequest(IncomingMailConnectionHandler.REQUEST_MESSAGE_APPEND, new Object[] { folder, rawMessage, initialFlags }, callback);
	}
	
	public void requestMessageCopy(MessageToken messageToken, FolderTreeItem destinationFolder, MailStoreRequestCallback callback) {
		if(!this.hasCopy()) {
			throw new UnsupportedOperationException();
		}
		connectionHandler.addRequest(IncomingMailConnectionHandler.REQUEST_MESSAGE_COPY, new Object[] { messageToken, destinationFolder }, callback);
	}
	
	private void connectionHandler_mailConnectionRequestComplete(int type, Object result, Object tag, boolean isFinal) {
        MailStoreRequestCallback callback;
        if(tag instanceof MailStoreRequestCallback) {
            callback = (MailStoreRequestCallback)tag;
        }
        else {
            callback = null;
        }
	    
		Object[] results;
		switch(type) {
		case IncomingMailConnectionHandler.REQUEST_FOLDER_TREE:
		    if(callback != null && isFinal) { callback.mailStoreRequestComplete(); }
			fireFolderTreeUpdated((FolderTreeItem)result);
			break;
		case IncomingMailConnectionHandler.REQUEST_FOLDER_EXPUNGE:
            if(callback != null && isFinal) { callback.mailStoreRequestComplete(); }
            if(result instanceof Object[]) {
                results = (Object[])result;
                if(results[1] instanceof int[]) {
                    fireFolderExpunged((FolderTreeItem)results[0], (int[])results[1], (MessageToken[])results[2]);
                }
                else if(results[1] instanceof MessageToken[]) {
                    fireFolderExpunged((FolderTreeItem)results[0], (MessageToken[])results[1], (MessageToken[])results[2]);
                }
                else {
                    fireFolderExpunged((FolderTreeItem)results[0], new int[0], new MessageToken[0]);
                }
            }
		    break;
		case IncomingMailConnectionHandler.REQUEST_FOLDER_STATUS:
            if(callback != null && isFinal) { callback.mailStoreRequestComplete(); }
			FolderTreeItem[] folders = (FolderTreeItem[])result;
			for(int i=0; i<folders.length; i++) {
				fireFolderStatusChanged(folders[i]);
			}
			break;
		case IncomingMailConnectionHandler.REQUEST_FOLDER_MESSAGES_RANGE:
            if(callback != null && isFinal) { callback.mailStoreRequestComplete(); }
            results = (Object[])result;
            fireFolderMessagesAvailable((FolderTreeItem)results[0], (FolderMessage[])results[1], false);
		    break;
		case IncomingMailConnectionHandler.REQUEST_FOLDER_MESSAGES_SET:
            if(callback != null && isFinal) { callback.mailStoreRequestComplete(); }
			results = (Object[])result;
			fireFolderMessagesAvailable((FolderTreeItem)results[0], (FolderMessage[])results[1], ((Boolean)results[2]).booleanValue());
			break;
		case IncomingMailConnectionHandler.REQUEST_FOLDER_MESSAGES_RECENT:
            if(callback != null && isFinal) { callback.mailStoreRequestComplete(); }
			results = (Object[])result;
			fireFolderMessagesAvailable((FolderTreeItem)results[0], (FolderMessage[])results[1], ((Boolean)results[2]).booleanValue());
			break;
		case IncomingMailConnectionHandler.REQUEST_FOLDER_MESSAGE_INDEX_MAP:
		    if(callback != null && isFinal) { callback.mailStoreRequestComplete(); }
		    results = (Object[])result;
		    fireFolderMessageIndexMapAvailable((FolderTreeItem)results[0], (ToIntHashtable)results[1]);
		    break;
		case IncomingMailConnectionHandler.REQUEST_FOLDER_REFRESH_REQUIRED:
		    results = (Object[])result;
		    fireFolderRefreshRequired((FolderTreeItem)results[0]);
		    break;
		case IncomingMailConnectionHandler.REQUEST_MESSAGE:
            if(callback != null && isFinal) { callback.mailStoreRequestComplete(); }
			results = (Object[])result;
			fireMessageAvailable(
			        (MessageToken)results[0],
			        ((Boolean)results[1]).booleanValue(),
			        (MimeMessagePart)results[2],
			        (MimeMessageContent[])results[3], null);
			break;
		case IncomingMailConnectionHandler.REQUEST_MESSAGE_PARTS:
            if(callback != null && isFinal) { callback.mailStoreRequestComplete(); }
			results = (Object[])result;
			fireMessageContentAvailable((MessageToken)results[0], (MimeMessageContent[])results[1]);
			break;
		case IncomingMailConnectionHandler.REQUEST_MESSAGE_DELETE:
		case IncomingMailConnectionHandler.REQUEST_MESSAGE_UNDELETE:
		case IncomingMailConnectionHandler.REQUEST_MESSAGE_ANSWERED:
        case IncomingMailConnectionHandler.REQUEST_MESSAGE_FORWARDED:
        case IncomingMailConnectionHandler.REQUEST_MESSAGE_SEEN:
        case IncomingMailConnectionHandler.REQUEST_MESSAGE_UNSEEN:
            if(callback != null && isFinal) { callback.mailStoreRequestComplete(); }
            if(result instanceof Object[]) {
    			results = (Object[])result;
    			fireMessageFlagsChanged((MessageToken)results[0], (MessageFlags)results[1]);
            }
			break;
		case IncomingMailConnectionHandler.REQUEST_MESSAGE_APPEND:
            if(callback != null && isFinal) { callback.mailStoreRequestComplete(); }
			results = (Object[])result;
			fireFolderMessagesAvailable((FolderTreeItem)results[0], (FolderMessage[])results[1], false);
			break;
		case IncomingMailConnectionHandler.REQUEST_MESSAGE_COPY:
            if(callback != null && isFinal) { callback.mailStoreRequestComplete(); }
			break;
		}
	}

    private void connectionHandler_mailConnectionRequestFailed(int type, Object tag, Throwable exception, boolean isFinal) {
        MailStoreRequestCallback callback;
        if(tag instanceof MailStoreRequestCallback) {
            callback = (MailStoreRequestCallback)tag;
        }
        else {
            callback = null;
        }
        
        if(callback != null) { callback.mailStoreRequestFailed(exception, isFinal); }
	}
}
