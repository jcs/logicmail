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

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

import net.rim.device.api.collection.util.BigVector;
import net.rim.device.api.system.EventLogger;
import net.rim.device.api.util.Comparator;
import net.rim.device.api.util.InvertedOrderComparator;

import org.logicprobe.LogicMail.AnalyticsDataCollector;
import org.logicprobe.LogicMail.AppInfo;
import org.logicprobe.LogicMail.conf.GlobalConfig;
import org.logicprobe.LogicMail.conf.MailSettings;
import org.logicprobe.LogicMail.conf.MailSettingsEvent;
import org.logicprobe.LogicMail.conf.MailSettingsListener;
import org.logicprobe.LogicMail.mail.FolderTreeItem;
import org.logicprobe.LogicMail.mail.MessageToken;
import org.logicprobe.LogicMail.message.MimeMessageContent;
import org.logicprobe.LogicMail.message.MimeMessagePart;
import org.logicprobe.LogicMail.util.FilenameEncoder;
import org.logicprobe.LogicMail.util.StringParser;

/**
 * Front-end for reading and writing message content from local file storage.
 */
public class MessageContentFileManager {
    private static MessageContentFileManager instance;
    private final MailSettings mailSettings;
    private String cacheUrl;
    
    private static String CACHE_PREFIX = "cache/";
    private static String MSG_SUFFIX = ".msg";
    private static String MSG_FILTER = "*.msg";
    private static String ALL_FILTER = "*";
    
    /** Map of FolderTreeItems to sets of known message UIDs within them. */
    private final Hashtable folderMessageUidCache = new Hashtable();
    
    /**
     * Instantiates a new mail file manager.
     */
    private MessageContentFileManager() {
        mailSettings = MailSettings.getInstance();

        // Register a listener for configuration changes
        mailSettings.addMailSettingsListener(new MailSettingsListener() {
            public void mailSettingsSaved(MailSettingsEvent e) {
                if((e.getGlobalChange() & GlobalConfig.CHANGE_TYPE_DATA) != 0) {
                    refreshConfiguration();
                }
            }
        });

        refreshConfiguration();
    }
    
    /**
     * Gets the single instance of the message content file manager.
     *
     * @return single instance of the message content file manager
     */
    public static synchronized MessageContentFileManager getInstance() {
        if(instance == null) {
            instance = new MessageContentFileManager();
        }
        return instance;
    }
    
    /**
     * Refreshes the configuration based on any system configuration changes.
     */
    private synchronized void refreshConfiguration() {
        String localDataLocation = mailSettings.getGlobalConfig().getLocalDataLocation();
        if(localDataLocation == null) {
            cacheUrl = null;
            return;
        }
        String newCacheUrl = localDataLocation + CACHE_PREFIX;
        if(!newCacheUrl.equals(cacheUrl)) {
            FileConnection fileConnection;
            try {
                fileConnection = (FileConnection)Connector.open(localDataLocation);
                if(!fileConnection.exists()) {
                    fileConnection.mkdir();
                }
                fileConnection.close();
                
                fileConnection = (FileConnection)Connector.open(newCacheUrl);
                if(!fileConnection.exists()) {
                    fileConnection.mkdir();
                }
                fileConnection.close();
                cacheUrl = newCacheUrl;
            } catch (IOException e) {
                EventLogger.logEvent(AppInfo.GUID,
                        ("Unable to open cache: " + newCacheUrl
                            + "\r\n" + e.getMessage()).getBytes(),
                        EventLogger.ERROR);
                AnalyticsDataCollector.getInstance().onApplicationError(
                        "Unable to open cache: " + e.getMessage());
                cacheUrl = null;
            }
        }
        if(cacheUrl == null) {
            folderMessageUidCache.clear();
        }
    }
    
    /**
     * Check whether cached message content exists.
     *
     * @param folder the folder that the message is stored within
     * @param messageToken the token for the message to check for
     * @return true, if the message exists in the cache
     */
    public synchronized boolean messageContentExists(FolderTreeItem folder, MessageToken messageToken) {
        if(cacheUrl == null) { return false; }
        Hashtable messageUidSet = (Hashtable)folderMessageUidCache.get(folder);
        if(messageUidSet == null) {
            try {
                String[] fileUrls = getMessageFiles(folder);
                messageUidSet = new Hashtable(fileUrls.length);
                for(int i=0; i<fileUrls.length; i++) {
                    String messageUid = getMessageUidFromFileUrl(fileUrls[i]);
                    if(messageUid != null) {
                        messageUidSet.put(messageUid, Boolean.TRUE);
                    }
                }
                folderMessageUidCache.put(folder, messageUidSet);
            } catch (IOException e) { return false; }
        }
        return messageUidSet.containsKey(messageToken.getMessageUid());
    }
    
    /**
     * Gets the cached content for a particular message.
     *
     * @param folder the folder that the message is stored within
     * @param messageToken the token for the message
     * @param messageParts the message parts to try loading content for
     * @return the loaded message content
     */
    public synchronized MimeMessageContent[] getMessageContent(FolderTreeItem folder, MessageToken messageToken, MimeMessagePart[] messageParts) {
        return getMessageContent(folder, messageToken, messageParts, new int[4]);
    }
    
    /**
     * Gets the cached content for a particular message.
     *
     * @param folder the folder that the message is stored within
     * @param messageToken the token for the message
     * @param messageParts the message parts to try loading content for
     * @param customValues an existing 4-element array to be populated with the
     *     custom values from the message header
     * @return the loaded message content
     */
    public synchronized MimeMessageContent[] getMessageContent(FolderTreeItem folder, MessageToken messageToken, MimeMessagePart[] messageParts, int[] customValues) {
        if(customValues == null || customValues.length != 4) {
            throw new IllegalArgumentException();
        }
        if(cacheUrl == null) { return new MimeMessageContent[0]; }

        FileConnection fileConnection = null;
        String fileUrl = null;
        Vector loadedContent = new Vector(messageParts.length);
        try {
            fileConnection = getFolderFileConnection(folder);
            if(!fileConnection.exists()) { return new MimeMessageContent[0]; }
            
            String folderUrl = fileConnection.getURL();
            fileConnection.close();
            fileUrl = getMessageFileUrl(folderUrl, messageToken);
            
            fileConnection = (FileConnection)Connector.open(fileUrl);
            if(fileConnection.exists()) {
                MessageContentFileReader reader = new MessageContentFileReader(fileConnection, messageToken.getMessageUid());
                reader.open();
                
                // Read out the custom values from the header
                int[] fileCustomValues = reader.getCustomValues();
                for(int i=0; i<4; i++) {
                    customValues[i] = fileCustomValues[i];
                }
                
                for(int i=0; i<messageParts.length; i++) {
                    MimeMessageContent content = reader.getContent(messageParts[i]);
                    if(content != null) {
                        loadedContent.addElement(content);
                    }
                }
                
                reader.close();
            }
        } catch (Exception e) {
            if(fileUrl != null) {
                EventLogger.logEvent(AppInfo.GUID,
                        ("Unable to read token: " + fileUrl
                            + "\r\n" + e.getMessage()).getBytes(),
                        EventLogger.ERROR);
                AnalyticsDataCollector.getInstance().onApplicationError(
                        "Unable to read token: " + e.getMessage());
            }
            else {
                EventLogger.logEvent(AppInfo.GUID,
                        ("Unable to access cache for folder: " + folder.getPath()
                            + "\r\n" + e.getMessage()).getBytes(),
                        EventLogger.ERROR);
                AnalyticsDataCollector.getInstance().onApplicationError(
                        "Unable to access cache for folder: " + e.getMessage());
            }
        } finally {
            if(fileConnection != null) {
                try { fileConnection.close(); } catch (Exception e) { }
            }
        }
        
        MimeMessageContent[] result = new MimeMessageContent[loadedContent.size()];
        loadedContent.copyInto(result);
        return result;
    }
    
    /**
     * Put complete message content into the cache.
     * This method will create a new file if one does not exist, or replace an
     * existing file if it does.  This method also allows the caller to set
     * header fields on file creation.
     *
     * @param folder the folder that the message is stored within
     * @param messageToken the token for the message
     * @param content the content to be added to the cache
     * @param customValues array of 4 <code>int</code> values for the file header
     */
    public synchronized void putCompleteMessageContent(
            FolderTreeItem folder,
            MessageToken messageToken,
            MimeMessageContent[] content,
            int[] customValues) {
        if(customValues == null || customValues.length != 4) {
            throw new IllegalArgumentException();
        }
        putMessageContentImpl(folder, messageToken, content, false, customValues);
    }
    
    /**
     * Put message content into the cache.
     * This method will create a new file if one does not exist, or append to
     * an existing file if it does.
     *
     * @param folder the folder that the message is stored within
     * @param messageToken the token for the message
     * @param content the content to be added to the cache
     */
    public synchronized void putMessageContent(
            FolderTreeItem folder,
            MessageToken messageToken,
            MimeMessageContent[] content) {
        putMessageContentImpl(folder, messageToken, content, true, null);
    }
    
    private void putMessageContentImpl(
            FolderTreeItem folder,
            MessageToken messageToken,
            MimeMessageContent[] content,
            boolean append,
            int[] customValues) {
        
        if(cacheUrl == null) { return; }

        FileConnection fileConnection = null;
        String fileUrl = null;
        try {
            fileConnection = getFolderFileConnection(folder);
            if(!fileConnection.exists()) { return; }
            
            String folderUrl = fileConnection.getURL();
            fileConnection.close();
            fileUrl = getMessageFileUrl(folderUrl, messageToken);
            
            fileConnection = (FileConnection)Connector.open(fileUrl);
            
            // Delete existing file if append behavior is undesired
            if(!append && fileConnection.exists()) { fileConnection.delete(); }
            
            MessageContentFileWriter writer = new MessageContentFileWriter(fileConnection, messageToken.getMessageUid());
            if(customValues != null) {
                writer.setCustomValues(customValues);
            }
            writer.open();
            
            //TODO: Make sure we're not appending content that already exists
            
            for(int i=0; i<content.length; i++) {
                writer.appendContent(content[i]);
            }
            
            writer.close();

            // Make sure we add the existence of this message to the UID cache
            Hashtable messageUidSet = (Hashtable)folderMessageUidCache.get(folder);
            if(messageUidSet == null) {
                messageUidSet = new Hashtable();
                folderMessageUidCache.put(folder, messageUidSet);
            }
            messageUidSet.put(messageToken.getMessageUid(), Boolean.TRUE);
        } catch (Exception e) {
            if(fileUrl != null) {
                EventLogger.logEvent(AppInfo.GUID,
                        ("Unable to read token: " + fileUrl
                            + "\r\n" + e.getMessage()).getBytes(),
                        EventLogger.ERROR);
            }
            else {
                EventLogger.logEvent(AppInfo.GUID,
                        ("Unable to access cache for folder: " + folder.getPath()
                            + "\r\n" + e.getMessage()).getBytes(),
                        EventLogger.ERROR);
            }
        } finally {
            if(fileConnection != null) {
                try { fileConnection.close(); } catch (Exception e) { }
            }
        }
    }
    
    private String[] getMessageFiles(FolderTreeItem folder) throws IOException {
        BigVector fileVector = new BigVector();
        Comparator comparator;
        if(mailSettings.getGlobalConfig().getDispOrder()) {
            comparator = new MailFileComparator();
        }
        else {
            comparator = new InvertedOrderComparator(new MailFileComparator());
        }
        
        FileConnection fileConnection = getFolderFileConnection(folder);
        String folderUrl = fileConnection.getURL();
        Enumeration e = fileConnection.list(MSG_FILTER, false);
        while(e.hasMoreElements()) {
            fileVector.insertElement(comparator, e.nextElement());
        }
        fileConnection.close();

        int size = fileVector.size();
        String[] result = new String[size];
        for(int i=0; i<size; i++) {
            result[i] = folderUrl + fileVector.elementAt(i);
        }
        return result;
    }
    
    private FileConnection getFolderFileConnection(FolderTreeItem folder) throws IOException {
        String folderUid = StringParser.toHexString(
                folder.getUniqueId()).toLowerCase();
        
        StringBuffer buf = new StringBuffer(cacheUrl);
        
        // Open the folder directory, creating if necessary
        buf.append(folderUid); buf.append('/');
        FileConnection fileConnection = (FileConnection)Connector.open(buf.toString());
        if(!fileConnection.exists()) {
            fileConnection.mkdir();
        }
        
        return fileConnection;
    }
    
    /**
     * Removes a set of messages from the cache.
     *
     * @param folder the folder that the messages are stored within
     * @param messageTokens the tokens for the messages to remove
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public synchronized void removeMessages(FolderTreeItem folder, MessageToken[] messageTokens) throws IOException {
        if(cacheUrl == null) { return; }

        FileConnection fileConnection = getFolderFileConnection(folder);
        String folderUrl = fileConnection.getURL();
        fileConnection.close();
        
        Hashtable messageUidSet = (Hashtable)folderMessageUidCache.get(folder);
        for(int i=0; i<messageTokens.length; i++) {
            try {
                FileConnection mailFileConnection =
                    (FileConnection)Connector.open(getMessageFileUrl(folderUrl, messageTokens[i]));
                if(mailFileConnection.exists() && !mailFileConnection.isDirectory() && mailFileConnection.canRead()) {
                    mailFileConnection.delete();
                }
                mailFileConnection.close();
                
                if(messageUidSet != null) {
                    messageUidSet.remove(messageTokens[i].getMessageUid());
                }
            } catch (IOException exp) {
                if (EventLogger.getMinimumLevel() >= EventLogger.DEBUG_INFO) {
                    EventLogger.logEvent(AppInfo.GUID,
                            ("Error deleting message from cache: " + exp.toString()).getBytes(),
                            EventLogger.DEBUG_INFO);
                }
            }
        }
    }

    /**
     * Removes an individual message from the cache.
     *
     * @param folder the folder that the message is stored within
     * @param messageToken the token for the message to remove
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public synchronized void removeMessage(FolderTreeItem folder, MessageToken messageToken) throws IOException {
        if(cacheUrl == null) { return; }

        FileConnection fileConnection = getFolderFileConnection(folder);
        String folderUrl = fileConnection.getURL();
        fileConnection.close();
        
        try {
            FileConnection mailFileConnection =
                (FileConnection)Connector.open(getMessageFileUrl(folderUrl, messageToken));
            if(mailFileConnection.exists() && !mailFileConnection.isDirectory() && mailFileConnection.canRead()) {
                mailFileConnection.delete();
            }
            mailFileConnection.close();
            
            // Update directory listing cache, if available
            Hashtable messageUidSet = (Hashtable)folderMessageUidCache.get(folder);
            if(messageUidSet != null) {
                messageUidSet.remove(messageToken.getMessageUid());
            }
        } catch (IOException exp) {
            if (EventLogger.getMinimumLevel() >= EventLogger.DEBUG_INFO) {
                EventLogger.logEvent(AppInfo.GUID,
                        ("Error deleting message from cache: " + exp.toString()).getBytes(),
                        EventLogger.DEBUG_INFO);
            }
        }
    }
    
    /**
     * Removes an entire folder from the cache.
     *
     * @param folder the folder to remove
     * @return true, if successful
     */
    public synchronized boolean removeFolder(FolderTreeItem folder) {
        if(cacheUrl == null) { return false; }

        folderMessageUidCache.remove(folder);
        
        FileConnection fileConnection = null;
        try {
            fileConnection = getFolderFileConnection(folder);
            deleteTree(fileConnection);
            return true;
        } catch (IOException exp) {
            if (EventLogger.getMinimumLevel() >= EventLogger.DEBUG_INFO) {
                EventLogger.logEvent(AppInfo.GUID,
                        ("Error deleting folder from cache: " + exp.toString()).getBytes(),
                        EventLogger.DEBUG_INFO);
            }
            return false;
        } finally {
            if(fileConnection != null) {
                try { fileConnection.close(); } catch (Exception e) { }
            }
        }
    }
    
    private static void deleteTree(FileConnection fileConnection) throws IOException {
        if(!fileConnection.canWrite()) {
            fileConnection.setWritable(true);
        }
        if(fileConnection.isDirectory()) {
            String directoryUrl = fileConnection.getURL();
            Enumeration en = fileConnection.list(ALL_FILTER, true);
            while(en.hasMoreElements()) {
                String fileUrl = directoryUrl + (String)en.nextElement();
                deleteTree((FileConnection)Connector.open(fileUrl));
            }
        }
        fileConnection.delete();
    }
    
    private static String getMessageFileUrl(String folderUrl, MessageToken messageToken) {
        return folderUrl + FilenameEncoder.encode(messageToken.getMessageUid()) + MSG_SUFFIX;
    }
    
    private static String getMessageUidFromFileUrl(String fileUrl) {
        int p = fileUrl.lastIndexOf('/');
        int q = fileUrl.lastIndexOf('.');
        if(p != -1 && q != -1 && p < q) {
            return FilenameEncoder.decode(fileUrl.substring(p + 1, q));
        }
        else {
            return null;
        }
    }
}
