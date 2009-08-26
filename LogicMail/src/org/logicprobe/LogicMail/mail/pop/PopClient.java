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

package org.logicprobe.LogicMail.mail.pop;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

import org.logicprobe.LogicMail.conf.AccountConfig;
import org.logicprobe.LogicMail.conf.ConnectionConfig;
import org.logicprobe.LogicMail.conf.GlobalConfig;
import org.logicprobe.LogicMail.conf.MailSettings;
import org.logicprobe.LogicMail.conf.MailSettingsEvent;
import org.logicprobe.LogicMail.conf.MailSettingsListener;
import org.logicprobe.LogicMail.conf.PopConfig;
import org.logicprobe.LogicMail.mail.FolderTreeItem;
import org.logicprobe.LogicMail.mail.IncomingMailClient;
import org.logicprobe.LogicMail.mail.MailException;
import org.logicprobe.LogicMail.mail.MailProgressHandler;
import org.logicprobe.LogicMail.mail.MessageToken;
import org.logicprobe.LogicMail.message.FolderMessage;
import org.logicprobe.LogicMail.message.Message;
import org.logicprobe.LogicMail.message.MimeMessageContent;
import org.logicprobe.LogicMail.message.MessageEnvelope;
import org.logicprobe.LogicMail.message.MessageFlags;
import org.logicprobe.LogicMail.message.MimeMessagePart;
import org.logicprobe.LogicMail.util.Connection;
import org.logicprobe.LogicMail.util.MailMessageParser;
import org.logicprobe.LogicMail.util.StringParser;

/**
 * 
 * Implements the POP3 client
 * 
 */
public class PopClient implements IncomingMailClient {
    private PopConfig accountConfig;
    private Connection connection;
    private PopProtocol popProtocol;
    private String username;
    private String password;
    private boolean openStarted;
    private boolean configChanged;
    
    /**
     * Table of supported server capabilities
     */
    private Hashtable capabilities;
    
    /**
     * Active mailbox.  Since POP3 does not support multiple
     * mailboxes for a user, it is used to contain some
     * relevant information for the user's single mailbox.
     */
    private FolderTreeItem activeMailbox = null;
    
    /** Creates a new instance of PopClient */
    public PopClient(GlobalConfig globalConfig, PopConfig accountConfig) {
        this.accountConfig = accountConfig;
        connection = new Connection(
                accountConfig.getServerName(),
                accountConfig.getServerPort(),
                accountConfig.getServerSecurity() == ConnectionConfig.SECURITY_SSL,
                accountConfig.getDeviceSide());
        popProtocol = new PopProtocol(connection);
        username = accountConfig.getServerUser();
        password = accountConfig.getServerPass();
        
        // Create our dummy folder item for the inbox
        activeMailbox = new FolderTreeItem("INBOX", "INBOX", "");
        activeMailbox.setMsgCount(0);
        openStarted = false;
        configChanged = false;
        MailSettings.getInstance().addMailSettingsListener(mailSettingsListener);
    }

    private MailSettingsListener mailSettingsListener = new MailSettingsListener() {
		public void mailSettingsSaved(MailSettingsEvent e) {
			mailSettings_MailSettingsSaved(e);
		}
    };
    
    private void mailSettings_MailSettingsSaved(MailSettingsEvent e) {
		if(MailSettings.getInstance().containsAccountConfig(accountConfig)) {
			// Refresh authentication information from the configuration
	        username = accountConfig.getServerUser();
	        password = accountConfig.getServerPass();
	        
	        if(!isConnected()) {
	        	// Rebuild the connection to include new settings
	            connection = new Connection(
	                    accountConfig.getServerName(),
	                    accountConfig.getServerPort(),
	                    accountConfig.getServerSecurity() == ConnectionConfig.SECURITY_SSL,
	                    accountConfig.getDeviceSide());
	            popProtocol = new PopProtocol(connection);
	        }
	        else {
		        // Set a flag to make sure we rebuild the Connection object
		        // the next time we close the connection.
		        configChanged = true;
	        }
		}
		else {
			// We have been deleted, so unregister to make sure we
			// no longer affect the system and can be garbage collected.
			MailSettings.getInstance().removeMailSettingsListener(mailSettingsListener);
		}
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.mail.IncomingMailClient#getAcctConfig()
     */
    public AccountConfig getAcctConfig() {
        return accountConfig;
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.mail.MailClient#getConnectionConfig()
     */
    public ConnectionConfig getConnectionConfig() {
		return getAcctConfig();
	}

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.mail.MailClient#open()
     */
    public boolean open() throws IOException, MailException {
        if(!openStarted) {
            connection.open();
            // Eat the initial server response
            connection.receive();
            openStarted = true;
        }
        
        // Find out server capabilities
        capabilities = popProtocol.executeCapa();
        
        // TLS initialization
        int serverSecurity = accountConfig.getServerSecurity();
        if((serverSecurity == ConnectionConfig.SECURITY_TLS_IF_AVAILABLE && capabilities.containsKey("STARTTLS"))
        		|| (serverSecurity == ConnectionConfig.SECURITY_TLS)) {
        	popProtocol.executeStartTLS();
        	connection.startTLS();
        }
        
        try {
            // Login to the server
            popProtocol.executeUser(username);
            popProtocol.executePass(password);
        } catch (MailException exp) {
            return false;
        }
        // Update message counts
        activeMailbox.setMsgCount(popProtocol.executeStat());
        
        openStarted = false;
        return true;
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.mail.MailClient#close()
     */
    public void close() throws IOException, MailException {
        if(connection.isConnected()) {
            try {
                popProtocol.executeQuit();
            } catch (Exception exp) { }
        }
        connection.close();
        
        if(configChanged) {
        	// Rebuild the connection to include new settings
            connection = new Connection(
                    accountConfig.getServerName(),
                    accountConfig.getServerPort(),
                    accountConfig.getServerSecurity() == ConnectionConfig.SECURITY_SSL,
                    accountConfig.getDeviceSide());
            popProtocol = new PopProtocol(connection);
            configChanged = false;
        }
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.mail.MailClient#isConnected()
     */
    public boolean isConnected() {
        return connection.isConnected();
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.mail.MailClient#getUsername()
     */
    public String getUsername() {
        return username;
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.mail.MailClient#setUsername(java.lang.String)
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.mail.MailClient#getPassword()
     */
    public String getPassword() {
        return password;
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.mail.MailClient#setPassword(java.lang.String)
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.mail.IncomingMailClient#hasFolders()
     */
    public boolean hasFolders() {
        return false;
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.mail.IncomingMailClient#hasUndelete()
     */
    public boolean hasUndelete() {
        return false;
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.mail.IncomingMailClient#hasAppend()
     */
    public boolean hasAppend() {
    	return false;
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.mail.IncomingMailClient#hasCopy()
     */
    public boolean hasCopy() {
		return false;
	}
    
    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.mail.IncomingMailClient#hasIdle()
     */
    public boolean hasIdle() {
		return false;
	}

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.mail.IncomingMailClient#getFolderTree(org.logicprobe.LogicMail.mail.MailProgressHandler)
     */
    public FolderTreeItem getFolderTree(MailProgressHandler progressHandler) throws IOException, MailException {
        return null;
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.mail.IncomingMailClient#refreshFolderStatus(org.logicprobe.LogicMail.mail.FolderTreeItem[], org.logicprobe.LogicMail.mail.MailProgressHandler)
     */
    public void refreshFolderStatus(FolderTreeItem[] folders, MailProgressHandler progressHandler) throws IOException, MailException {
        // Only one mailbox can exist, so we just pull the message counts.
    	// Since this is a single operation with POP, there is no reason to
    	// provide more detailed status through the progress handler.
        activeMailbox.setMsgCount(popProtocol.executeStat());
        if(folders.length == 1 && folders[0] != activeMailbox) {
        	folders[0].setMsgCount(activeMailbox.getMsgCount());
        }
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.mail.IncomingMailClient#getInboxFolder()
     */
    public FolderTreeItem getInboxFolder() {
    	return activeMailbox;
    }
    
    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.mail.IncomingMailClient#getActiveFolder()
     */
    public FolderTreeItem getActiveFolder() {
        return activeMailbox;
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.mail.IncomingMailClient#setActiveFolder(org.logicprobe.LogicMail.mail.FolderTreeItem)
     */
    public void setActiveFolder(FolderTreeItem mailbox) throws IOException, MailException {
        // Mailbox cannot be changed, so we just pull the message counts
        activeMailbox.setMsgCount(popProtocol.executeStat());
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.mail.IncomingMailClient#setActiveFolder(org.logicprobe.LogicMail.mail.MessageToken)
     */
    public void setActiveFolder(MessageToken messageToken) throws IOException, MailException {
        // Mailbox cannot be changed, so we just pull the message counts
        activeMailbox.setMsgCount(popProtocol.executeStat());
    }
    
    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.mail.IncomingMailClient#getFolderMessages(int, int, org.logicprobe.LogicMail.mail.MailProgressHandler)
     */
    public FolderMessage[] getFolderMessages(int firstIndex, int lastIndex, MailProgressHandler progressHandler) throws IOException, MailException {
    	FolderMessage[] folderMessages = new FolderMessage[(lastIndex - firstIndex)+1];
        int index = 0;
        String[] headerText;
        String uid;
        MessageEnvelope env;
        int preCount;
        int postCount = connection.getBytesReceived();
        for(int i=firstIndex; i<=lastIndex; i++) {
        	preCount = postCount;
            headerText = popProtocol.executeTop(i, 0);
            uid = popProtocol.executeUidl(i);
            env = MailMessageParser.parseMessageEnvelope(headerText);
            folderMessages[index++] = new FolderMessage(
            		new PopMessageToken(i, uid),
            		env, i, uid.hashCode());
            postCount = connection.getBytesReceived();
            if(progressHandler != null) { progressHandler.mailProgress(MailProgressHandler.TYPE_NETWORK, (postCount - preCount), -1); }
        }
        return folderMessages;
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.mail.IncomingMailClient#getNewFolderMessages(org.logicprobe.LogicMail.mail.MailProgressHandler)
     */
    public FolderMessage[] getNewFolderMessages(MailProgressHandler progressHandler) throws IOException, MailException {
    	int count = MailSettings.getInstance().getGlobalConfig().getRetMsgCount();
		int msgCount = activeMailbox.getMsgCount();
        int firstIndex = Math.max(1, msgCount - count);
    	return getFolderMessages(firstIndex, activeMailbox.getMsgCount(), progressHandler);
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.mail.IncomingMailClient#getMessage(org.logicprobe.LogicMail.mail.MessageToken, org.logicprobe.LogicMail.mail.MailProgressHandler)
     */
    public Message getMessage(MessageToken messageToken, MailProgressHandler progressHandler) throws IOException, MailException {
    	PopMessageToken popMessageToken = (PopMessageToken)messageToken;
    	
        // Figure out the max number of lines
        int maxLines = accountConfig.getMaxMessageLines();

        // Download the message text
        String[] message = popProtocol.executeTop((popMessageToken.getMessageIndex()), maxLines, progressHandler);
        
        Hashtable contentMap = new Hashtable();
        MimeMessagePart rootPart = MailMessageParser.parseRawMessage(contentMap, StringParser.createInputStream(message));
        Message msg = new Message(rootPart);
        Enumeration e = contentMap.keys();
        while(e.hasMoreElements()) {
        	MimeMessagePart part = (MimeMessagePart)e.nextElement();
        	msg.putContent(part, (MimeMessageContent)contentMap.get(part));
        }
        return msg;
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.mail.IncomingMailClient#deleteMessage(org.logicprobe.LogicMail.mail.MessageToken, org.logicprobe.LogicMail.message.MessageFlags)
     */
    public void deleteMessage(MessageToken messageToken, MessageFlags messageFlags) throws IOException, MailException {
    	PopMessageToken popMessageToken = (PopMessageToken)messageToken;
    	
        popProtocol.executeDele(popMessageToken.getMessageIndex());
        messageFlags.setDeleted(true);
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.mail.IncomingMailClient#undeleteMessage(org.logicprobe.LogicMail.mail.MessageToken, org.logicprobe.LogicMail.message.MessageFlags)
     */
    public void undeleteMessage(MessageToken messageToken, MessageFlags messageFlags) throws IOException, MailException {
        // Undelete is not supported, so we do nothing here.
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.mail.IncomingMailClient#appendMessage(org.logicprobe.LogicMail.mail.FolderTreeItem, java.lang.String, org.logicprobe.LogicMail.message.MessageFlags)
     */
    public void appendMessage(FolderTreeItem folder, String rawMessage, MessageFlags initialFlags) throws IOException, MailException {
    	// Append is not supported, so we do nothing here.
    }
    
    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.mail.IncomingMailClient#copyMessage(org.logicprobe.LogicMail.mail.MessageToken, org.logicprobe.LogicMail.mail.FolderTreeItem)
     */
    public void copyMessage(MessageToken messageToken, FolderTreeItem destinationFolder) throws IOException, MailException {
    	// Copy is not supported, so we do nothing here.
    }
    
    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.mail.IncomingMailClient#noop()
     */
    public boolean noop() throws IOException, MailException {
    	popProtocol.executeNoop();
		return false;
	}

	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.mail.IncomingMailClient#idleModeBegin()
	 */
	public void idleModeBegin() throws IOException, MailException {
		// Idle mode is not supported, so we do nothing here.
	}

	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.mail.IncomingMailClient#idleModeEnd()
	 */
	public void idleModeEnd() throws IOException, MailException {
		// Idle mode is not supported, so we do nothing here.
	}

	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.mail.IncomingMailClient#idleModePoll()
	 */
	public boolean idleModePoll() throws IOException, MailException {
		// Idle mode is not supported, so we do nothing here.
		return false;
	}
}
