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

package org.logicprobe.LogicMail.mail.imap;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;
import org.logicprobe.LogicMail.conf.AccountConfig;
import org.logicprobe.LogicMail.conf.ConnectionConfig;
import org.logicprobe.LogicMail.conf.GlobalConfig;
import org.logicprobe.LogicMail.conf.ImapConfig;
import org.logicprobe.LogicMail.conf.MailSettings;
import org.logicprobe.LogicMail.conf.MailSettingsEvent;
import org.logicprobe.LogicMail.conf.MailSettingsListener;
import org.logicprobe.LogicMail.mail.FolderTreeItem;
import org.logicprobe.LogicMail.mail.IncomingMailClient;
import org.logicprobe.LogicMail.mail.MailException;
import org.logicprobe.LogicMail.message.FolderMessage;
import org.logicprobe.LogicMail.message.Message;
import org.logicprobe.LogicMail.message.MessagePart;
import org.logicprobe.LogicMail.message.MessagePartFactory;
import org.logicprobe.LogicMail.message.MultiPart;
import org.logicprobe.LogicMail.util.Connection;

/**
 * 
 * Implements the IMAP client
 * 
 */
public class ImapClient implements IncomingMailClient {
    private GlobalConfig globalConfig;
    private ImapConfig accountConfig;
    private Connection connection;
    private ImapProtocol imapProtocol;
    private String username;
    private String password;
    private boolean openStarted;
    private boolean configChanged;

    /**
     * Table of supported server capabilities
     */
    private Hashtable capabilities;
    
    /**
     * Delimiter between folder names in the hierarchy
     */
    private String folderDelim = "";

    /**
     * Personal namespace from the IMAP server
     */
    private ImapProtocol.Namespace nsPersonal;
    
    /**
     * Active mailbox path, so most commands do not need
     * to take a mailbox path parameter.  This makes it easier
     * to provide a common front-end that still works for
     * protocols that do not support a mailbox hierarchy.
     */
    private FolderTreeItem activeMailbox = null;

    public ImapClient(GlobalConfig globalConfig, ImapConfig accountConfig) {
        this.accountConfig = accountConfig;
        this.globalConfig = globalConfig;
        connection = new Connection(
                accountConfig.getServerName(),
                accountConfig.getServerPort(),
                accountConfig.getServerSSL(),
                accountConfig.getDeviceSide());
        imapProtocol = new ImapProtocol(connection);
        username = accountConfig.getServerUser();
        password = accountConfig.getServerPass();
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
	                    accountConfig.getServerSSL(),
	                    accountConfig.getDeviceSide());
	            imapProtocol = new ImapProtocol(connection);
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
    
    public boolean open() throws IOException, MailException {
        try {
            if(!openStarted) {
                connection.open();
                activeMailbox = null;
                
                // Swallow the initial "* OK" line from the server
                connection.receive();

                // Find out server capabilities
                capabilities = imapProtocol.executeCapability();
                openStarted = true;
            }
            // Authenticate with the server
            if(!imapProtocol.executeLogin(username, password)) {
                return false;
            }

            // Get the namespaces, if supported
            if(capabilities.containsKey("NAMESPACE")) {
                ImapProtocol.NamespaceResponse nsResponse = imapProtocol.executeNamespace();

                if(nsResponse.personal != null &&
                   nsResponse.personal.length > 0 &&
                   nsResponse.personal[0] != null &&
                   nsResponse.personal[0].delimiter != null &&
                   nsResponse.personal[0].prefix != null) {
                    // We got a valid personal namespace, so proceed
                    nsPersonal = nsResponse.personal[0];
                    folderDelim = nsPersonal.delimiter;
                }
            }
            // We could not get valid personal namespace information,
            // so the folder delimiter will be acquired differently.
            if(nsPersonal == null) {
                // Discover folder delimiter
                Vector resp = imapProtocol.executeList("", "");
                if(resp.size() > 0) {
                    folderDelim = ((ImapProtocol.ListResponse)resp.elementAt(0)).delim;
                }
            }
            
            openStarted = false;
        } catch (MailException exp) {
            close();
            String msg = exp.getMessage();
            if(msg.startsWith("NO")) {
                msg = msg.substring(msg.indexOf(' ')+1);
            }
            throw new MailException(msg);
        }
        return true;
    }
    
    public void close() throws IOException, MailException {
        if(connection.isConnected()) {
            // Not closing to avoid expunging deleted messages
            //if(activeMailbox != null && !activeMailbox.equals("")) {
            //    imapProtocol.executeClose();
            //}
            try {
                imapProtocol.executeLogout();
            } catch (Exception exp) { }
        }
        activeMailbox = null;
        connection.close();
        
        if(configChanged) {
        	// Rebuild the connection to include new settings
            connection = new Connection(
                    accountConfig.getServerName(),
                    accountConfig.getServerPort(),
                    accountConfig.getServerSSL(),
                    accountConfig.getDeviceSide());
            imapProtocol = new ImapProtocol(connection);
        	configChanged = false;
        }
    }

    public boolean isConnected() {
        return connection.isConnected();
    }

    public AccountConfig getAcctConfig() {
        return accountConfig;
    }

    public ConnectionConfig getConnectionConfig() {
		return getAcctConfig();
	}

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean hasFolders() {
        return true;
    }

    public boolean hasUndelete() {
        return true;
    }
    
    public FolderTreeItem getFolderTree() throws IOException, MailException {
        FolderTreeItem rootItem = new FolderTreeItem("", "", folderDelim);
        
        boolean childrenExtension = capabilities.containsKey("CHILDREN");
        
        // Special logic to handle a user-specified folder prefix
        String folderPrefix = accountConfig.getFolderPrefix();
        if(folderPrefix != null && folderPrefix.length() > 0) {
            FolderTreeItem fakeRootItem = new FolderTreeItem("", folderPrefix, folderDelim);
            getFolderTreeImpl(fakeRootItem, 0, childrenExtension);
            
            // Since we have no way to find the inbox with a hard-coded prefix,
            // assume it to be there and then do a STATUS to verify.
            // If the STATUS fails, a MailException will be thrown, which we can
            // safely ignore.  Otherwise, create a folder item and add it.
            try {
                imapProtocol.executeStatus(new String[] { "INBOX" });
                FolderTreeItem inboxItem = new FolderTreeItem(rootItem, "INBOX", "INBOX", folderDelim, true);
                rootItem.addChild(inboxItem);
            } catch (MailException exp) { }
            
            if(fakeRootItem.hasChildren()) {
                FolderTreeItem[] children = fakeRootItem.children();
                for(int i=0; i<children.length; i++) {
                    if(children[i].getName().trim().length() > 0) {
                        rootItem.addChild(children[i]);
                    }
                }
            }
        }
        else {
            getFolderTreeImpl(rootItem, 0, childrenExtension);
        }

        return rootItem;
    }

    private void getFolderTreeImpl(FolderTreeItem baseFolder, int depth, boolean childrenExtension) throws IOException, MailException {
        Vector respList;
        if(depth == 0) {
            respList = imapProtocol.executeList(baseFolder.getPath(), "%");
        }
        else {
            respList = imapProtocol.executeList(baseFolder.getPath() + baseFolder.getDelim(), "%");
        }

        int size = respList.size();
        for(int i=0;i<size;++i) {
            ImapProtocol.ListResponse resp = (ImapProtocol.ListResponse)respList.elementAt(i);
            FolderTreeItem childItem = getFolderItem(baseFolder, resp.name, resp.canSelect);
                baseFolder.addChild(childItem);
                if(resp.hasChildren || !childrenExtension) {
                    // The folder has children, so lets go and list them
                    if(depth+1 < globalConfig.getImapMaxFolderDepth()) {
                        getFolderTreeImpl(childItem, depth+1, childrenExtension);
                    }
                }
                else if(depth == 0 &&
                        nsPersonal != null &&
                        (resp.name + nsPersonal.delimiter).equals(nsPersonal.prefix) &&
                        globalConfig.getImapMaxFolderDepth() > 1) {
                    // The folder claims to have no children, but it is a root
                    // folder that matches the personal namespace prefix, so
                    // look for children anyways.
                    getFolderTreeImpl(childItem, depth+1, childrenExtension);
                }
            }
        }

    public void refreshFolderStatus(FolderTreeItem root) throws IOException, MailException {
        // Flatten the tree for easy batching of the status refresh
        Vector folders = new Vector();
        flattenFolderTree(folders, root);
        
        // Construct an array of mailbox paths to match the folder vector
        int size = folders.size();
        Vector mboxPaths = new Vector();
        Hashtable mboxMap = new Hashtable();
        int i;
        for(i=0; i<size; i++) {
            FolderTreeItem item = (FolderTreeItem)folders.elementAt(i);
            if(item.isSelectable()) {
                mboxPaths.addElement(item.getPath());
                mboxMap.put(item.getPath(), item);
            }
        }
        String[] mboxPathsArray = new String[mboxPaths.size()];
        mboxPaths.copyInto(mboxPathsArray);
        
        // Execute the STATUS command on the folders
        ImapProtocol.StatusResponse[] response = imapProtocol.executeStatus(mboxPathsArray);
        
        // Iterate through the results and update the FolderTreeItem objects
        for(i=0; i<mboxPathsArray.length; i++) {
            FolderTreeItem item = (FolderTreeItem)mboxMap.get(mboxPathsArray[i]);
            item.setMsgCount(response[i].exists);
            item.setUnseenCount(response[i].unseen);
        }
    }
    
    /**
     * Recursively walk the folder tree, populating a flat vector of
     * FolderTreeItem objects.
     *
     * @param folders An initialized Vector to populate
     * @param node The FolderTreeItem to start from
     */
    private void flattenFolderTree(Vector folders, FolderTreeItem node) {
        // Avoid adding the invisible root node
        if(node.getPath().length() > 0) {
            folders.addElement(node);
        }
        
        if(node.hasChildren()) {
            FolderTreeItem[] children = node.children();
            for(int i = 0; i < children.length; i++) {
                flattenFolderTree(folders, children[i]);
            }
        }
    }
    
    
    public FolderTreeItem getActiveFolder() {
        return activeMailbox;
    }

    public void setActiveFolder(FolderTreeItem mailbox) throws IOException, MailException {
        // change active mailbox
        ImapProtocol.SelectResponse response = imapProtocol.executeSelect(mailbox.getPath());

        this.activeMailbox = mailbox;
        activeMailbox.setMsgCount(response.exists);
        
        // ideally, this should parse out the message counts
        // and populate the appropriate fields of the activeMailbox FolderItem
    }

    public FolderMessage[] getFolderMessages(int firstIndex, int lastIndex) throws IOException, MailException {
        // Make sure we do not FETCH an empty folder
        if(firstIndex > lastIndex) {
            return new FolderMessage[0];
        }
        
        ImapProtocol.FetchEnvelopeResponse[] response =
                imapProtocol.executeFetchEnvelope(firstIndex, lastIndex);
        
        FolderMessage[] folderMessages = new FolderMessage[response.length];
        for(int i=0;i<response.length;i++) {
            folderMessages[i] = new FolderMessage(response[i].envelope, response[i].index);
            folderMessages[i].setSeen(response[i].flags.seen);
            folderMessages[i].setAnswered(response[i].flags.answered);
            folderMessages[i].setDeleted(response[i].flags.deleted);
            folderMessages[i].setRecent(response[i].flags.recent);
            folderMessages[i].setFlagged(response[i].flags.flagged);
            folderMessages[i].setDraft(response[i].flags.draft);
            folderMessages[i].setJunk(response[i].flags.junk);
        }
        
        return folderMessages;
    }

    public Message getMessage(FolderMessage folderMessage) throws IOException, MailException {
        ImapParser.MessageSection structure = getMessageStructure(folderMessage.getIndex());
        MessagePart rootPart =
            getMessagePart(folderMessage.getIndex(),
                           structure, globalConfig.getImapMaxMsgSize());
        Message msg = new Message(folderMessage.getEnvelope(), rootPart);
        return msg;
    }

    private MessagePart getMessagePart(int index,
                                       ImapParser.MessageSection structure,
                                       int maxSize)
        throws IOException, MailException
    {
        MessagePart part;
        if(MessagePartFactory.isMessagePartSupported(structure.type, structure.subtype)) {
            String data;
            if(structure.type.equalsIgnoreCase("multipart"))
                data = null;
            else {
                if(structure.size < maxSize) {
                    data = getMessageBody(index, structure.address);
                    maxSize -= structure.size;
                }
                else {
                    // We hit the size limit, so stop processing
                    return null;
                }
            }
            part = MessagePartFactory.createMessagePart(structure.type, structure.subtype, structure.encoding, structure.charset, data);
        }
        else
            part = null;

        if((part instanceof MultiPart)&&(structure.subsections != null)&&(structure.subsections.length > 0)) {
            for(int i=0;i<structure.subsections.length;i++) {
                MessagePart subPart = getMessagePart(index, structure.subsections[i], maxSize);
                if(subPart != null) {
                    ((MultiPart)part).addPart(subPart);
                }
            }
        }
        return part;
    }
    
    /**
     * Returns the message structure tree independent of message content.
     * This tree is used to build the final message tree.
     */
    private ImapParser.MessageSection getMessageStructure(int msgIndex) throws IOException, MailException {
        if(activeMailbox.equals("")) {
            throw new MailException("Mailbox not selected");
        }

        return imapProtocol.executeFetchBodystructure(msgIndex);
    }

    /**
     * Create a new folder item, doing the relevant parsing on the path.
     * @param parent Parent folder
     * @param folderPath Folder path string
     * @param canSelect Whether the folder is selectable
     * @return Folder item object
     */
    private FolderTreeItem getFolderItem(FolderTreeItem parent, String folderPath, boolean canSelect) throws IOException, MailException {
        int pos = 0;
        int i = 0;
        while((i = folderPath.indexOf(folderDelim, i)) != -1) {
            if(i != -1) { pos = i+1; i++; }
        }
        String decodedName = ImapParser.parseFolderName(folderPath.substring(pos));
        FolderTreeItem item = new FolderTreeItem(parent, decodedName, folderPath, folderDelim, canSelect);
        item.setMsgCount(0);
        return item;
    }

    private String getMessageBody(int index, String address) throws IOException, MailException {
        if(activeMailbox.equals("")) {
            throw new MailException("Mailbox not selected");
        }
        
        return imapProtocol.executeFetchBody(index, address);
    }
    
    public void deleteMessage(FolderMessage folderMessage) throws IOException, MailException {
        ImapProtocol.MessageFlags updatedFlags =
            imapProtocol.executeStore(folderMessage.getIndex(), true, new String[] { "\\Deleted" });
        refreshMessageFlags(updatedFlags, folderMessage);
    }


    public void undeleteMessage(FolderMessage folderMessage) throws IOException, MailException {
        ImapProtocol.MessageFlags updatedFlags =
            imapProtocol.executeStore(folderMessage.getIndex(), false, new String[] { "\\Deleted" });
        refreshMessageFlags(updatedFlags, folderMessage);
    }
    
    /**
     * Sets the flags on a message so the server knows it was answered.
     *
     * @throw IOException on I/O errors
     * @throw MailException on protocol errors
     */
    public void messageAnswered(FolderMessage folderMessage) throws IOException, MailException {
        ImapProtocol.MessageFlags updatedFlags =
            imapProtocol.executeStore(folderMessage.getIndex(), true, new String[] { "\\Answered" });
        refreshMessageFlags(updatedFlags, folderMessage);
    }
    
    private void refreshMessageFlags(ImapProtocol.MessageFlags updatedFlags, FolderMessage folderMessage) {
        if(updatedFlags != null) {
            folderMessage.setAnswered(updatedFlags.answered);
            folderMessage.setDeleted(updatedFlags.deleted);
            folderMessage.setDraft(updatedFlags.draft);
            folderMessage.setFlagged(updatedFlags.draft);
            folderMessage.setRecent(updatedFlags.recent);
            folderMessage.setSeen(updatedFlags.seen);
        }
    }
    
    /**
     * Appends a message to the specified folder, and flags it as seen.
     * This is intended for use when saving sent or draft messages.
     *
     * @throw IOException on I/O errors
     * @throw MailException on protocol errors
     */
    public void appendMessage(FolderTreeItem folder, String rawMessage, boolean isSeen, boolean isDraft) throws IOException, MailException {
        ImapProtocol.MessageFlags flags = new ImapProtocol.MessageFlags();
        flags.seen = isSeen;
        flags.draft = isDraft;
        imapProtocol.executeAppend(folder.getPath(), rawMessage, flags);
    }
}
