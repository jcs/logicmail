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

package org.logicprobe.LogicMail.conf;

import java.io.DataInput;

import org.logicprobe.LogicMail.model.AccountNode;
import org.logicprobe.LogicMail.model.MailManager;
import org.logicprobe.LogicMail.model.MailRootNode;
import org.logicprobe.LogicMail.model.MailboxNode;
import org.logicprobe.LogicMail.util.SerializableHashtable;

/**
 * Configuration object to store settings for
 * incoming mail accounts.
 */
public abstract class AccountConfig extends ConnectionConfig {
    private String serverUser;
    private String serverPass;
    private IdentityConfig identityConfig;
    private long identityConfigId;
    private OutgoingConfig outgoingConfig;
    private long outgoingConfigId;
    private int refreshOnStartup;
    private int refreshFrequency;
    private MailboxNode sentMailbox;
    private long sentMailboxId;
    private MailboxNode draftMailbox;
    private long draftMailboxId;
    private boolean replySignatureIncluded;
    private boolean forwardSignatureIncluded;
    private boolean signatureAbove;
    private boolean selectableIdentityEnabled;
    
    private int initialFolderMessages;
    private int folderMessageIncrement;
    private int maximumFolderMessages;

    public static final int REFRESH_ON_STARTUP_NEVER = 0;
    public static final int REFRESH_ON_STARTUP_STATUS = 1;
    public static final int REFRESH_ON_STARTUP_HEADERS = 2;
    
    /** Account identity configuration selection. */
    public static final int CHANGE_TYPE_IDENTITY = 0x0100;
    /** Account outgoing configuration selection. */
    public static final int CHANGE_TYPE_OUTGOING = 0x0200;
    /** Account special mailbox settings */
    public static final int CHANGE_TYPE_MAILBOXES = 0x0400;
    /** Account download limit settings. */
    public static final int CHANGE_TYPE_LIMITS = 0x0800;
    /** Account prompts for user actions. */
    public static final int CHANGE_TYPE_PROMPTS = 0x1000;
    /** Account prompts for user actions. */
    public static final int CHANGE_TYPE_ADVANCED = 0x2000;
    /** Account settings for refresh and polling. */
    public static final int CHANGE_TYPE_REFRESH = 0x4000;

    /**
     * Instantiates a new connection configuration with defaults.
     */
    public AccountConfig() {
        super();
    }

    /**
     * Instantiates a new connection configuration from serialized data.
     * 
     * @param input The input stream to deserialize from
     */
    public AccountConfig(DataInput input) {
        super(input);
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.conf.ConnectionConfig#setDefaults()
     */
    protected void setDefaults() {
        super.setDefaults();
        serverUser = "";
        serverPass = "";
        setServerPort(110);
        identityConfig = null;
        identityConfigId = -1L;
        outgoingConfig = null;
        outgoingConfigId = -1L;
        refreshOnStartup = REFRESH_ON_STARTUP_NEVER;
        refreshFrequency = 0;
        sentMailbox = null;
        sentMailboxId = -1L;
        draftMailbox = null;
        draftMailboxId = -1L;
        replySignatureIncluded = true;
        forwardSignatureIncluded = false;
        signatureAbove = false;
        selectableIdentityEnabled = false;
        initialFolderMessages = 20;
        folderMessageIncrement = 5;
        maximumFolderMessages = 40;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return getAcctName();
    }

    /**
     * Gets the username to authenticate with.
     * 
     * @return The username
     */
    public String getServerUser() {
        return serverUser;
    }

    /**
     * Sets the username to authenticate with.
     * 
     * @param serverUser The new username
     */
    public void setServerUser(String serverUser) {
        if(!this.serverUser.equals(serverUser)) {
            this.serverUser = serverUser;
            changeType |= CHANGE_TYPE_CONNECTION;
        }
    }

    /**
     * Gets the password to authenticate with.
     * 
     * @return The password
     */
    public String getServerPass() {
        return serverPass;
    }

    /**
     * Sets the password to authenticate with.
     * 
     * @param serverPass The new password
     */
    public void setServerPass(String serverPass) {
        if(!this.serverPass.equals(serverPass)) {
            this.serverPass = serverPass;
            changeType |= CHANGE_TYPE_CONNECTION;
        }
    }

    /**
     * Gets the identity configuration.
     * 
     * @return The identity configuration
     */
    public IdentityConfig getIdentityConfig() {
        if(identityConfig == null && identityConfigId != -1L) {
            identityConfig = MailSettings.getInstance().getIdentityConfigByUniqueId(identityConfigId);
        }
        return identityConfig;
    }

    /**
     * Sets the identity configuration.
     * 
     * @param identityConfig The new identity configuration
     */
    public void setIdentityConfig(IdentityConfig identityConfig) {
        if(this.identityConfig != identityConfig) {
            if(identityConfig == null) {
                this.identityConfig = null;
                this.identityConfigId = -1L;
            }
            else {
                if(this.identityConfig != identityConfig) {
                }
                this.identityConfig = identityConfig;
                this.identityConfigId = identityConfig.getUniqueId();
            }
            changeType |= CHANGE_TYPE_IDENTITY;
        }
    }

    /**
     * Gets the outgoing connection configuration.
     * 
     * @return The outgoing connection configuration
     */
    public OutgoingConfig getOutgoingConfig() {
        if(outgoingConfig == null && outgoingConfigId != -1L) {
            outgoingConfig = MailSettings.getInstance().getOutgoingConfigByUniqueId(outgoingConfigId);
        }
        return outgoingConfig;
    }

    /**
     * Sets the outgoing connection configuration.
     * 
     * @param outgoingConfig The new outgoing connection configuration
     */
    public void setOutgoingConfig(OutgoingConfig outgoingConfig) {
        if(this.outgoingConfig != outgoingConfig) {
            if(outgoingConfig == null) {
                this.outgoingConfig = null;
                this.outgoingConfigId = -1L;
            }
            else {
                this.outgoingConfig = outgoingConfig;
                this.outgoingConfigId = outgoingConfig.getUniqueId();
            }
            changeType |= CHANGE_TYPE_OUTGOING;
        }
    }

    /**
     * Checks how the account should be refreshed on startup.
     */
    public int getRefreshOnStartup() {
        return refreshOnStartup;
    }
    
    /**
     * Sets how the account should be refreshed on startup.
     */
    public void setRefreshOnStartup(int refreshOnStartup) {
        if(this.refreshOnStartup != refreshOnStartup) {
            this.refreshOnStartup = refreshOnStartup;
            changeType |= CHANGE_TYPE_REFRESH;
        }
    }
    
    /**
     * Gets the refresh frequency.
     *
     * @return the refresh frequency in minutes, or <code>0</code> if disabled
     */
    public int getRefreshFrequency() {
        return refreshFrequency;
    }
    
    /**
     * Sets the refresh frequency.
     *
     * @param refreshFrequency the new refresh frequency in minutes, or <code>0</code> if disabled
     */
    public void setRefreshFrequency(int refreshFrequency) {
        if(this.refreshFrequency != refreshFrequency) {
            this.refreshFrequency = refreshFrequency;
            changeType |= CHANGE_TYPE_REFRESH;
        }
    }
    
    /**
     * Gets the sent message mailbox.
     * 
     * @return The sent message mailbox
     */
    public MailboxNode getSentMailbox() {
        if(sentMailbox == null && sentMailboxId != -1L) {
            MailRootNode rootNode = MailManager.getInstance().getMailRootNode();

            AccountNode[] accounts = rootNode.getAccounts();
            for(int i=0; i<accounts.length; i++) {
                if(accounts[i].getRootMailbox() != null) {
                    sentMailbox = findMailboxNode(accounts[i].getRootMailbox(), sentMailboxId);
                    if(sentMailbox != null) {
                        break;
                    }
                }
            }
        }
        return sentMailbox;
    }


    /**
     * Sets the sent message mailbox.
     * 
     * @param sentMailbox The new sent message mailbox
     */
    public void setSentMailbox(MailboxNode sentMailbox) {
        if(this.sentMailbox != sentMailbox) {
            if(sentMailbox == null) {
                this.sentMailbox = null;
                this.sentMailboxId = -1L;
            }
            else {
                this.sentMailbox = sentMailbox;
                this.sentMailboxId = sentMailbox.getUniqueId();
            }
            changeType |= CHANGE_TYPE_MAILBOXES;
        }
    }

    /**
     * Gets the draft message mailbox.
     * 
     * @return The draft message mailbox
     */
    public MailboxNode getDraftMailbox() {
        if(draftMailbox == null && draftMailboxId != -1L) {
            MailRootNode rootNode = MailManager.getInstance().getMailRootNode();

            AccountNode[] accounts = rootNode.getAccounts();
            for(int i=0; i<accounts.length; i++) {
                if(accounts[i].getRootMailbox() != null) {
                    draftMailbox = findMailboxNode(accounts[i].getRootMailbox(), draftMailboxId);
                    if(draftMailbox != null) {
                        break;
                    }
                }
            }
        }
        return draftMailbox;
    }


    /**
     * Sets the draft message mailbox.
     * 
     * @param draftMailbox The new draft message mailbox
     */
    public void setDraftMailbox(MailboxNode draftMailbox) {
        if(this.draftMailbox != draftMailbox) {
            if(draftMailbox == null) {
                this.draftMailbox = null;
                this.draftMailboxId = -1L;
            }
            else {
                this.draftMailbox = draftMailbox;
                this.draftMailboxId = draftMailbox.getUniqueId();
            }
            changeType |= CHANGE_TYPE_MAILBOXES;
        }
    }

    /**
     * Finds a mailbox node recursively in the mail model tree.
     * 
     * @param currentNode The current node
     * @param id The id to look for
     * 
     * @return The mailbox node
     */
    protected static MailboxNode findMailboxNode(MailboxNode currentNode, long id) {
        if(currentNode.getUniqueId() == id) {
            return currentNode;
        }
        else {
            MailboxNode[] nodes = currentNode.getMailboxes();
            for(int i=0; i<nodes.length; i++) {
                MailboxNode result = findMailboxNode(nodes[i], id);
                if(result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    /**
     * Gets whether a signature is included on reply messages.
     */
    public boolean isReplySignatureIncluded() {
        return replySignatureIncluded;
    }
    
    /**
     * Sets whether a signature is included on reply messages.
     */
    public void setReplySignatureIncluded(boolean replySignatureIncluded) {
        this.replySignatureIncluded = replySignatureIncluded;
    }
    
    /**
     * Gets whether a signature is included on forwarded messages.
     */
    public boolean isForwardSignatureIncluded() {
        return forwardSignatureIncluded;
    }
    
    /**
     * Sets whether a signature is included on forwarded messages.
     */
    public void setForwardSignatureIncluded(boolean forwardSignatureIncluded) {
        this.forwardSignatureIncluded = forwardSignatureIncluded;
    }
    
    /**
     * Gets whether the signature is above the quoted content.
     * 
     * @return true if above, false if below
     */
    public boolean isSignatureAbove() {
        return signatureAbove;
    }
    
    /**
     * Sets whether the signature is above the quoted content.
     * 
     * @param signatureAbove true if above, false if below
     */
    public void setSignatureAbove(boolean signatureAbove) {
        this.signatureAbove = signatureAbove;
    }
    
    /**
     * Gets whether selectable identity support is enabled.
     * This option allows the user to select an identity other than the account
     * default when composing a new message.
     *
     * @return true, if selectable identity support is enabled
     */
    public boolean isSelectableIdentityEnabled() {
        return selectableIdentityEnabled;
    }
    
    /**
     * Sets whether selectable identity support is enabled.
     *
     * @param selectableIdentityEnabled the new selectable identity setting
     */
    public void setSelectableIdentityEnabled(boolean selectableIdentityEnabled) {
        this.selectableIdentityEnabled = selectableIdentityEnabled;
    }
    
    /**
     * Gets the number of folder messages to load when freshly selecting a
     * mailbox.  This value should normally affect loading the first time a
     * given mailbox is selected following application startup.
     *
     * @return the initial folder message count
     */
    public int getInitialFolderMessages() {
        return initialFolderMessages;
    }

    /**
     * Sets the number of initial folder messages.
     *
     * @param initialFolderMessages the new initial folder message count
     */
    public void setInitialFolderMessages(int initialFolderMessages) {
        if(this.initialFolderMessages != initialFolderMessages) {
            this.initialFolderMessages = initialFolderMessages;
            changeType |= CHANGE_TYPE_LIMITS;
        }
    }

    /**
     * Gets the folder message load increment.  This value should affect the
     * number of additional folder messages that are loaded for a mailbox,
     * following a user request.
     *
     * @return the folder message load increment
     */
    public int getFolderMessageIncrement() {
        return folderMessageIncrement;
    }

    /**
     * Sets the folder message load increment.
     *
     * @param folderMessageIncrement the new folder message load increment
     */
    public void setFolderMessageIncrement(int folderMessageIncrement) {
        if(this.folderMessageIncrement != folderMessageIncrement) {
            this.folderMessageIncrement = folderMessageIncrement;
            changeType |= CHANGE_TYPE_LIMITS;
        }
    }

    /**
     * Gets the maximum number folder messages to retain for a mailbox.
     *
     * @return the maximum number of folder messages
     */
    public int getMaximumFolderMessages() {
        return maximumFolderMessages;
    }

    /**
     * Sets the maximum number of folder messages.
     *
     * @param maximumFolderMessages the new maximum number of folder messages
     */
    public void setMaximumFolderMessages(int maximumFolderMessages) {
        if(this.maximumFolderMessages != maximumFolderMessages) {
            this.maximumFolderMessages = maximumFolderMessages;
            changeType |= CHANGE_TYPE_LIMITS;
        }
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.conf.ConnectionConfig#writeConfigItems(org.logicprobe.LogicMail.util.SerializableHashtable)
     */
    public void writeConfigItems(SerializableHashtable table) {
        super.writeConfigItems(table);
        table.put("account_serverUser", serverUser);
        table.put("account_serverPass", serverPass);
        table.put("account_identityConfigId", new Long(identityConfigId));
        table.put("account_outgoingConfigId", new Long(outgoingConfigId));
        table.put("account_refreshOnStartup", new Integer(refreshOnStartup));
        table.put("account_refreshFrequency", new Integer(refreshFrequency));
        table.put("account_sentMailboxId", new Long(sentMailboxId));
        table.put("account_draftMailboxId", new Long(draftMailboxId));
        table.put("account_replySignatureIncluded", new Boolean(replySignatureIncluded));
        table.put("account_forwardSignatureIncluded", new Boolean(forwardSignatureIncluded));
        table.put("account_signatureAbove", new Boolean(signatureAbove));
        table.put("account_selectableIdentityEnabled", new Boolean(selectableIdentityEnabled));
        table.put("account_initialFolderMessages", new Integer(initialFolderMessages));
        table.put("account_folderMessageIncrement", new Integer(folderMessageIncrement));
        table.put("account_maximumFolderMessages", new Integer(maximumFolderMessages));
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.conf.ConnectionConfig#readConfigItems(org.logicprobe.LogicMail.util.SerializableHashtable)
     */
    public void readConfigItems(SerializableHashtable table) {
        super.readConfigItems(table);
        Object value;

        value = table.get("account_serverUser");
        if(value instanceof String) { serverUser = (String)value; }
        
        value = table.get("account_serverPass");
        if(value instanceof String) { serverPass = (String)value; }
        
        value = table.get("account_identityConfigId");
        if(value instanceof Long) {
            identityConfigId = ((Long)value).longValue();
        }
        else {
            identityConfigId = -1;
        }
        identityConfig = null;
        value = table.get("account_outgoingConfigId");
        if(value instanceof Long) {
            outgoingConfigId = ((Long)value).longValue();
        }
        else {
            outgoingConfigId = -1;
        }
        outgoingConfig = null;

        value = table.get("account_refreshOnStartup");
        if(value instanceof Integer) { refreshOnStartup = ((Integer)value).intValue(); }
        
        value = table.get("account_refreshFrequency");
        if(value instanceof Integer) { refreshFrequency = ((Integer)value).intValue(); }
        
        value = table.get("account_sentMailboxId");
        if(value instanceof Long) {
            sentMailboxId = ((Long)value).longValue();
        }
        else {
            sentMailboxId = -1L;
        }

        value = table.get("account_draftMailboxId");
        if(value instanceof Long) {
            draftMailboxId = ((Long)value).longValue();
        }
        else {
            draftMailboxId = -1L;
        }

        value = table.get("account_replySignatureIncluded");
        if(value instanceof Boolean) { replySignatureIncluded = ((Boolean)value).booleanValue(); }
        
        value = table.get("account_forwardSignatureIncluded");
        if(value instanceof Boolean) { forwardSignatureIncluded = ((Boolean)value).booleanValue(); }
        
        value = table.get("account_signatureAbove");
        if(value instanceof Boolean) { signatureAbove = ((Boolean)value).booleanValue(); }
        
        value = table.get("account_selectableIdentityEnabled");
        if(value instanceof Boolean) { selectableIdentityEnabled = ((Boolean)value).booleanValue(); }
        
        value = table.get("account_initialFolderMessages");
        if(value instanceof Integer) { initialFolderMessages = ((Integer)value).intValue(); }

        value = table.get("account_folderMessageIncrement");
        if(value instanceof Integer) { folderMessageIncrement = ((Integer)value).intValue(); }

        value = table.get("account_maximumFolderMessages");
        if(value instanceof Integer) { maximumFolderMessages = ((Integer)value).intValue(); }
    }
}
