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
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import net.rim.device.api.util.ToIntHashtable;

import org.logicprobe.LogicMail.LogicMailResource;
import org.logicprobe.LogicMail.message.FolderMessage;
import org.logicprobe.LogicMail.message.Message;
import org.logicprobe.LogicMail.message.MimeMessageContent;
import org.logicprobe.LogicMail.message.MessageFlags;
import org.logicprobe.LogicMail.message.MimeMessagePart;
import org.logicprobe.LogicMail.util.Queue;

public class IncomingMailConnectionHandler extends AbstractMailConnectionHandler {
    private IncomingMailClient incomingClient;
    private FolderTreeItem previousActiveFolder;

    // The various mail store requests, mirroring the
    // "requestXXXX()" methods from AbstractMailStore
    public static final int REQUEST_FOLDER_TREE              = 10;
    public static final int REQUEST_FOLDER_EXPUNGE           = 11;
    public static final int REQUEST_FOLDER_STATUS            = 12;
    public static final int REQUEST_FOLDER_MESSAGES_RANGE    = 13;
    public static final int REQUEST_FOLDER_MESSAGES_SET      = 14;
    public static final int REQUEST_FOLDER_MESSAGES_RECENT   = 15;
    public static final int REQUEST_FOLDER_MESSAGE_INDEX_MAP = 16;
    public static final int REQUEST_FOLDER_REFRESH_REQUIRED  = 17;
    public static final int REQUEST_MESSAGE                  = 20;
    public static final int REQUEST_MESSAGE_PARTS            = 21;
    public static final int REQUEST_MESSAGE_DELETE           = 22;
    public static final int REQUEST_MESSAGE_UNDELETE         = 23;
    public static final int REQUEST_MESSAGE_ANSWERED         = 24;
    public static final int REQUEST_MESSAGE_FORWARDED        = 25;
    public static final int REQUEST_MESSAGE_SEEN             = 26;
    public static final int REQUEST_MESSAGE_UNSEEN           = 27;
    public static final int REQUEST_MESSAGE_APPEND           = 28;
    public static final int REQUEST_MESSAGE_COPY             = 29;

    /**
     * Maximum amount of time to spend in the idle state.
     * Currently set to 5 minutes. (1000 ms/sec * 60 sec/min * 5 min)
     */
    private static final int IDLE_TIMEOUT = 300000;

    /**
     * Interval to do explicit NOOP-based polling when the idle state is
     * not available.  Currently set to 5 minutes.
     */
    private static final int NOOP_TIMEOUT = 300000;
    
    private final Timer idleTimer = new Timer();
    private TimerTask idleTimerTask;
    private boolean idleTimeout;
    private boolean idleRecentMessagesRequested;
    private long idleStartTime;
    
    /**
     * Listener to handle asynchronous notifications from the mail client.
     */
    private IncomingMailClientListener mailClientListener = new IncomingMailClientListener() {
        public void recentFolderMessagesAvailable() {
            handleRecentFolderMessagesAvailable();
        }
        public void folderMessageFlagsChanged(MessageToken token, MessageFlags messageFlags) {
            handleFolderMessageFlagsChanged(token, messageFlags);
        }
        public void folderMessageExpunged(MessageToken expungedToken, MessageToken[] updatedTokens) {
            handleFolderMessageExpunged(expungedToken, updatedTokens);
        }
        public void idleModeError() {
            handleIdleModeError();
        }
    };
    
    public IncomingMailConnectionHandler(IncomingMailClient client) {
        super(client);
        this.incomingClient = client;
        this.incomingClient.setListener(mailClientListener);
    }

    /**
     * Handles a specific request during the REQUESTS state.
     * 
     * @param type Type identifier for the request.
     * @param params Parameters for the request.
     * @param tag Tag reference to pass along with the request.
     * @throws IOException on I/O errors
     * @throws MailException on protocol errors
     */
    protected void handleRequest(int type, Object[] params, Object tag) throws IOException, MailException {
        switch(type) {
        case REQUEST_DISCONNECT:
            handleRequestDisconnect(tag);
            break;
        case REQUEST_FOLDER_TREE:
            handleRequestFolderTree(tag);
            break;
        case REQUEST_FOLDER_EXPUNGE:
            handleRequestFolderExpunge((FolderTreeItem)params[0], tag);
            break;
        case REQUEST_FOLDER_STATUS:
            handleRequestFolderStatus((FolderTreeItem[])params[0], tag);
            break;
        case REQUEST_FOLDER_MESSAGES_RANGE:
            handleRequestFolderMessagesRange(
                    (FolderTreeItem)params[0],
                    (MessageToken)params[1],
                    ((Integer)params[2]).intValue(),
                    tag);
            break;
        case REQUEST_FOLDER_MESSAGES_SET:
            if(params[1] instanceof MessageToken[]) {
                handleRequestFolderMessagesSet(
                        (FolderTreeItem)params[0],
                        (MessageToken[])params[1],
                        ((Boolean)params[2]).booleanValue(),
                        tag);
            }
            else if(params[1] instanceof int[]) {
                handleRequestFolderMessagesSet(
                        (FolderTreeItem)params[0],
                        (int[])params[1],
                        tag);
            }
            break;
        case REQUEST_FOLDER_MESSAGES_RECENT:
            handleRequestFolderMessagesRecent(
                    (FolderTreeItem)params[0], ((Boolean)params[1]).booleanValue(), tag);
            break;
        case REQUEST_FOLDER_MESSAGE_INDEX_MAP:
            handleRequestFolderMessagesIndexMap((FolderTreeItem)params[0], tag);
            break;
        case REQUEST_MESSAGE:
            handleRequestMessage((MessageToken)params[0], ((Boolean)params[1]).booleanValue(), tag);
            break;
        case REQUEST_MESSAGE_PARTS:
            handleRequestMessageParts((MessageToken)params[0], (MimeMessagePart[])params[1], tag);
            break;
        case REQUEST_MESSAGE_DELETE:
            handleRequestMessageDelete((MessageToken)params[0], tag);
            break;
        case REQUEST_MESSAGE_UNDELETE:
            handleRequestMessageUndelete((MessageToken)params[0], tag);
            break;
        case REQUEST_MESSAGE_ANSWERED:
            handleRequestMessageAnswered((MessageToken)params[0], tag);
            break;
        case REQUEST_MESSAGE_FORWARDED:
            handleRequestMessageForwarded((MessageToken)params[0], tag);
            break;
        case REQUEST_MESSAGE_SEEN:
            handleRequestMessageSeen((MessageToken)params[0], tag);
            break;
        case REQUEST_MESSAGE_UNSEEN:
            handleRequestMessageUnseen((MessageToken)params[0], tag);
            break;
        case REQUEST_MESSAGE_APPEND:
            handleRequestMessageAppend((FolderTreeItem)params[0], (String)params[1], (MessageFlags)params[2], tag);
            break;
        case REQUEST_MESSAGE_COPY:
            handleRequestMessageCopy((MessageToken)params[0], (FolderTreeItem)params[1], tag);
        }
    }

    private void handleRecentFolderMessagesAvailable() {
        if(getConnectionState() == STATE_IDLE) {
            if(idleRecentMessagesRequested) { return; }
            idleRecentMessagesRequested = true;
        }
        
        // Make sure we ignore this event if it occurs during the setup portion
        // of the command normally enqueued as a result of this notification.
        if(getRequestInProgress() == REQUEST_FOLDER_MESSAGES_RECENT) {
            return;
        }
        
        addRequest(
                IncomingMailConnectionHandler.REQUEST_FOLDER_MESSAGES_RECENT,
                new Object[] { incomingClient.getActiveFolder(), Boolean.FALSE },
                null);
    }

    private void handleFolderMessageFlagsChanged(MessageToken token, MessageFlags messageFlags) {
        // This notification just updates local data, so it does not need to
        // break out of the idle state.
        
        MailConnectionHandlerListener listener = getListener();
        if(listener != null) {
            listener.mailConnectionRequestComplete(
                    REQUEST_MESSAGE_SEEN,
                    new Object[] { token, messageFlags },
                    null, true);
        }
    }

    private void handleFolderMessageExpunged(MessageToken expungedToken, MessageToken[] updatedTokens) {
        // This notification just updates local data, so it does not need to
        // break out of the idle state.
        MailConnectionHandlerListener listener = getListener();
        if(listener != null) {
            listener.mailConnectionRequestComplete(
                    REQUEST_FOLDER_EXPUNGE,
                    new Object[] {
                            incomingClient.getActiveFolder(),
                            new MessageToken[] { expungedToken },
                            updatedTokens
                    }, null, true);
        }
    }

    private void handleIdleModeError() {
        idleTimerTask.cancel();
        Queue requestQueue = getRequestQueue();
        synchronized(requestQueue) {
            requestQueue.notifyAll();
        }
    }

    /**
     * Handles the start of the IDLE state.
     */
    protected void handleBeginIdle() throws IOException, MailException {
        idleStartTime = System.currentTimeMillis();
        FolderTreeItem inboxFolder = incomingClient.getInboxFolder();
        FolderTreeItem activeFolder = incomingClient.getActiveFolder();
        
        // This case will happen if the connection died while idling, was
        // restored, and we need to re-select the correct active folder.
        if(activeFolder == null && previousActiveFolder != null) {
            try {
                handleSetActiveFolder(previousActiveFolder);
            } catch (MailException e) {
                handleSetActiveFolder(inboxFolder);
            }
        }
        this.previousActiveFolder = activeFolder;
        
        if(incomingClient.hasIdle()) {
            idleRecentMessagesRequested = false;
            idleTimeout = false;
            idleTimerTask = new TimerTask() {
                public void run() {
                    handleIdleModeTimeout();
                }
            };
            idleTimer.schedule(idleTimerTask, IDLE_TIMEOUT);
            
            incomingClient.idleModeBegin();

        }
        else if(!incomingClient.hasLockedFolders()) {
            // In this case, we do a NOOP-based polling
            idleRecentMessagesRequested = false;
            idleTimeout = false;
            idleTimerTask = new TimerTask() {
                public void run() {
                    handleIdleModeTimeout();
                }
            };
            idleTimer.schedule(idleTimerTask, NOOP_TIMEOUT);
        }
    }

    protected void handleIdleModeTimeout() {
        idleTimeout = true;
        Queue requestQueue = getRequestQueue();
        synchronized(requestQueue) {
            requestQueue.notifyAll();
        }
    }

    protected void handleEndIdle() throws IOException, MailException {
        if(idleTimerTask != null) {
            idleTimerTask.cancel();
        }
        
        if(incomingClient.hasIdle()) {
            incomingClient.idleModeEnd();
        }
        
        if(idleTimeout) {
            incomingClient.noop();
            if(!idleRecentMessagesRequested) {
                // If we had a non-INBOX folder selected, then an idle timeout
                // should switch the active folder back to the INBOX.
                FolderTreeItem inboxFolder = incomingClient.getInboxFolder();
                FolderTreeItem activeFolder = incomingClient.getActiveFolder();
                if(inboxFolder != null && activeFolder != null
                        && !inboxFolder.getPath().equalsIgnoreCase(activeFolder.getPath())) {
                    handleSetActiveFolder(inboxFolder);
                }
            }
            MailConnectionHandlerListener listener = getListener();
            if(listener != null) {
                listener.mailConnectionIdleTimeout(System.currentTimeMillis() - idleStartTime);
            }
        }
    }
    
    private void handleRequestDisconnect(Object tag) throws IOException, MailException {
        this.previousActiveFolder = null;
        throw new MailException("", true, REQUEST_DISCONNECT);
    }

    private void handleRequestFolderTree(Object tag) throws IOException, MailException {
        String message = resources.getString(LogicMailResource.MAILCONNECTION_REQUEST_FOLDER_TREE);
        showStatus(message);
        FolderTreeItem root = incomingClient.getFolderTree(getProgressHandler(message));

        MailConnectionHandlerListener listener = getListener();
        if(root != null && listener != null) {
            listener.mailConnectionRequestComplete(REQUEST_FOLDER_TREE, root, tag, true);
        }
    }

    private void handleRequestFolderExpunge(FolderTreeItem folder, Object tag) throws IOException, MailException {
        String message = resources.getString(LogicMailResource.MAILCONNECTION_REQUEST_FOLDER_EXPUNGE);
        showStatus(message);
        checkActiveFolder(folder);
        incomingClient.expungeActiveFolder();
        
        // Notification of expunged messages is received through the client listener
        
        MailConnectionHandlerListener listener = getListener();
        if(listener != null) {
            listener.mailConnectionRequestComplete(REQUEST_FOLDER_EXPUNGE, folder, tag, true);
        }
    }

    private void handleRequestFolderStatus(FolderTreeItem[] folders, Object tag) throws IOException, MailException {
        String message = resources.getString(LogicMailResource.MAILCONNECTION_REQUEST_FOLDER_STATUS);
        showStatus(message);
        incomingClient.refreshFolderStatus(folders, getProgressHandler(message));

        MailConnectionHandlerListener listener = getListener();
        if(listener != null) {
            listener.mailConnectionRequestComplete(REQUEST_FOLDER_STATUS, folders, tag, true);
        }
    }

    private static class GetFolderMessageCallback implements FolderMessageCallback {
        private int type;
        private FolderTreeItem folder;
        private MailConnectionHandlerListener listener;
        private Object tag;
        private Object param;

        public GetFolderMessageCallback(int type, FolderTreeItem folder, MailConnectionHandlerListener listener, Object tag) {
            this(type, folder, listener, null, tag);
        }

        public GetFolderMessageCallback(int type, FolderTreeItem folder, MailConnectionHandlerListener listener, Object param, Object tag) {
            this.type = type;
            this.folder = folder;
            this.listener = listener;
            this.param = param;
            this.tag = tag;
        }

        public void folderMessageUpdate(FolderMessage folderMessage) {
            if(listener != null) {
                if(folderMessage != null) {
                    if(param == null) {
                        listener.mailConnectionRequestComplete(type, new Object[] { folder, new FolderMessage[] { folderMessage } }, tag, false);
                    }
                    else {
                        listener.mailConnectionRequestComplete(type, new Object[] { folder, new FolderMessage[] { folderMessage }, param }, tag, false);
                    }
                }
                else {
                    // If this is the last update of the sequence, make sure we
                    // notify the listener with a null array so it knows we are done.
                    if(param == null) {
                        listener.mailConnectionRequestComplete(type, new Object[] { folder, null }, tag, true);
                    }
                    else {
                        listener.mailConnectionRequestComplete(type, new Object[] { folder, null, param }, tag, true);
                    }
                }
            }
        }
    };

    private void handleRequestFolderMessagesRange(FolderTreeItem folder, MessageToken firstToken, int increment, Object tag) throws IOException, MailException {
        String message = resources.getString(LogicMailResource.MAILCONNECTION_REQUEST_FOLDER_MESSAGES);
        showStatus(message + "...");
        checkActiveFolder(folder);

        incomingClient.getFolderMessages(
                firstToken, increment,
                new GetFolderMessageCallback(
                        REQUEST_FOLDER_MESSAGES_RANGE,
                        folder,
                        getListener(), tag),
                        getProgressHandler(message));
    }
    
    private void handleRequestFolderMessagesSet(FolderTreeItem folder, MessageToken[] messageTokens, boolean flagsOnly, Object tag) throws IOException, MailException {
        String message = resources.getString(LogicMailResource.MAILCONNECTION_REQUEST_FOLDER_MESSAGES);
        showStatus(message + "...");
        checkActiveFolder(folder);

        incomingClient.getFolderMessages(
                messageTokens,
                flagsOnly,
                new GetFolderMessageCallback(
                        REQUEST_FOLDER_MESSAGES_SET,
                        folder,
                        getListener(),
                        new Boolean(flagsOnly), tag),
                        getProgressHandler(message));
    }
    
    private void handleRequestFolderMessagesSet(FolderTreeItem folder, int[] messageIndices, Object tag) throws IOException, MailException {
        String message = resources.getString(LogicMailResource.MAILCONNECTION_REQUEST_FOLDER_MESSAGES);
        showStatus(message + "...");
        checkActiveFolder(folder);

        incomingClient.getFolderMessages(
                messageIndices,
                new GetFolderMessageCallback(
                        REQUEST_FOLDER_MESSAGES_SET,
                        folder,
                        getListener(),
                        Boolean.FALSE, tag),
                        getProgressHandler(message));
    }
    
    private void handleRequestFolderMessagesRecent(FolderTreeItem folder, boolean flagsOnly, Object tag) throws IOException, MailException {
        String message = resources.getString(LogicMailResource.MAILCONNECTION_REQUEST_FOLDER_MESSAGES);
        showStatus(message + "...");
        checkActiveFolder(folder);

        incomingClient.getNewFolderMessages(
                flagsOnly,
                new GetFolderMessageCallback(
                        REQUEST_FOLDER_MESSAGES_RECENT,
                        folder,
                        getListener(),
                        new Boolean(flagsOnly), tag),
                        getProgressHandler(message));
    }

    private void handleRequestFolderMessagesIndexMap(FolderTreeItem folder, Object tag) throws IOException, MailException {
        String message = resources.getString(LogicMailResource.MAILCONNECTION_REQUEST_FOLDER_MESSAGES);
        showStatus(message + "...");
        checkActiveFolder(folder);
        
        ToIntHashtable uidIndexMap = incomingClient.getFolderMessageIndexMap(getProgressHandler(message));
        
        MailConnectionHandlerListener listener = getListener();
        if(uidIndexMap != null && listener != null) {
            listener.mailConnectionRequestComplete(
                    REQUEST_FOLDER_MESSAGE_INDEX_MAP,
                    new Object[] { folder, uidIndexMap }, tag, true);
        }
    }

    private void handleRequestMessage(MessageToken messageToken, boolean useLimits, Object tag) throws IOException, MailException {
        String statusMessage = resources.getString(LogicMailResource.MAILCONNECTION_REQUEST_MESSAGE);
        showStatus(statusMessage);
        checkActiveFolder(messageToken);

        Message message = incomingClient.getMessage(messageToken, useLimits, getProgressHandler(statusMessage));

        MailConnectionHandlerListener listener = getListener();
        if(message != null && listener != null) {
            listener.mailConnectionRequestComplete(REQUEST_MESSAGE, new Object[] {
                    messageToken,
                    new Boolean(message.isComplete()),
                    message.getStructure(),
                    message.getAllContent() },
                    tag, true);
        }
    }
    
    private void handleRequestMessageParts(MessageToken messageToken, MimeMessagePart[] messageParts, Object tag) throws IOException, MailException {
        String statusMessage = resources.getString(LogicMailResource.MAILCONNECTION_REQUEST_MESSAGE);
        showStatus(statusMessage);
        checkActiveFolder(messageToken);

        MimeMessageContent[] messageContent;

        // Replace this with a more general method:
        if(incomingClient.hasMessageParts()) {
            Vector messageContentVector = new Vector();
            for(int i=0; i<messageParts.length; i++) {
                MimeMessageContent content =
                    incomingClient.getMessagePart(messageToken, messageParts[i], getProgressHandler(statusMessage));
                if(content != null) {
                    messageContentVector.addElement(content);
                }
            }
            messageContent = new MimeMessageContent[messageContentVector.size()];
            messageContentVector.copyInto(messageContent);
        }
        else {
            messageContent = null;
        }

        MailConnectionHandlerListener listener = getListener();
        if(messageContent != null && listener != null) {
            listener.mailConnectionRequestComplete(REQUEST_MESSAGE_PARTS, new Object[] { messageToken, messageContent }, tag, true);
        }
    }

    private void handleRequestMessageDelete(MessageToken messageToken, Object tag) throws IOException, MailException {
        showStatus(resources.getString(LogicMailResource.MAILCONNECTION_REQUEST_MESSAGE_DELETE));
        checkActiveFolder(messageToken);

        incomingClient.deleteMessage(messageToken);

        MailConnectionHandlerListener listener = getListener();
        if(listener != null) {
            listener.mailConnectionRequestComplete(REQUEST_MESSAGE_DELETE, messageToken, tag, true);
        }
        
        // Notification of actual flag changes is received through the client listener
    }
    
    private void handleRequestMessageUndelete(MessageToken messageToken, Object tag) throws IOException, MailException {
        showStatus(resources.getString(LogicMailResource.MAILCONNECTION_REQUEST_MESSAGE_UNDELETE));
        checkActiveFolder(messageToken);

        incomingClient.undeleteMessage(messageToken);

        MailConnectionHandlerListener listener = getListener();
        if(listener != null) {
            listener.mailConnectionRequestComplete(REQUEST_MESSAGE_UNDELETE, messageToken, tag, true);
        }
        
        // Notification of actual flag changes is received through the client listener
    }
    
    private void handleRequestMessageAnswered(MessageToken messageToken, Object tag) throws IOException, MailException {
        showStatus(resources.getString(LogicMailResource.MAILCONNECTION_REQUEST_MESSAGE_UPDATING_FLAGS));
        if(incomingClient.hasFlags()) {
            checkActiveFolder(messageToken);
            incomingClient.messageAnswered(messageToken);
        }

        MailConnectionHandlerListener listener = getListener();
        if(listener != null) {
            listener.mailConnectionRequestComplete(REQUEST_MESSAGE_ANSWERED, messageToken, tag, true);
        }
        
        // Notification of actual flag changes is received through the client listener
    }
    
    private void handleRequestMessageForwarded(MessageToken messageToken, Object tag) throws IOException, MailException {
        showStatus(resources.getString(LogicMailResource.MAILCONNECTION_REQUEST_MESSAGE_UPDATING_FLAGS));
        if(incomingClient.hasFlags()) {
            checkActiveFolder(messageToken);
            incomingClient.messageForwarded(messageToken);
        }

        MailConnectionHandlerListener listener = getListener();
        if(listener != null) {
            listener.mailConnectionRequestComplete(REQUEST_MESSAGE_FORWARDED, messageToken, tag, true);
        }
        
        // Notification of actual flag changes is received through the client listener
    }
    
    private void handleRequestMessageSeen(MessageToken messageToken, Object tag) throws IOException, MailException {
        showStatus(resources.getString(LogicMailResource.MAILCONNECTION_REQUEST_MESSAGE_UPDATING_FLAGS));
        if(incomingClient.hasFlags()) {
            checkActiveFolder(messageToken);
            incomingClient.messageSeen(messageToken);
        }

        MailConnectionHandlerListener listener = getListener();
        if(listener != null) {
            listener.mailConnectionRequestComplete(REQUEST_MESSAGE_SEEN, messageToken, tag, true);
        }
        
        // Notification of actual flag changes is received through the client listener
    }
    
    private void handleRequestMessageUnseen(MessageToken messageToken, Object tag) throws IOException, MailException {
        showStatus(resources.getString(LogicMailResource.MAILCONNECTION_REQUEST_MESSAGE_UPDATING_FLAGS));
        if(incomingClient.hasFlags()) {
            checkActiveFolder(messageToken);
            incomingClient.messageUnseen(messageToken);
        }

        MailConnectionHandlerListener listener = getListener();
        if(listener != null) {
            listener.mailConnectionRequestComplete(REQUEST_MESSAGE_UNSEEN, messageToken, tag, true);
        }
        
        // Notification of actual flag changes is received through the client listener
    }
    
    private void handleRequestMessageAppend(FolderTreeItem folder, String rawMessage, MessageFlags initialFlags, Object tag) throws IOException, MailException {
        showStatus(resources.getString(LogicMailResource.MAILCONNECTION_REQUEST_MESSAGE_APPEND));
        if(incomingClient.hasAppend()) {
            incomingClient.appendMessage(folder, rawMessage, initialFlags);
        }

        MailConnectionHandlerListener listener = getListener();
        if(listener != null) {
            // Using a null FolderMessage since no information is returned on the appended message:
            listener.mailConnectionRequestComplete(REQUEST_MESSAGE_APPEND, new Object[] { folder, null }, tag, true);
        }
    }
    
    private void handleRequestMessageCopy(MessageToken messageToken, FolderTreeItem destinationFolder, Object tag) throws IOException, MailException {
        showStatus(resources.getString(LogicMailResource.MAILCONNECTION_REQUEST_MESSAGE_COPY));
        if(incomingClient.hasCopy()) {
            checkActiveFolder(messageToken);
            incomingClient.copyMessage(messageToken, destinationFolder);
        }

        MailConnectionHandlerListener listener = getListener();
        if(listener != null) {
            // Using a null FolderMessage since no information is returned on the appended message:
            listener.mailConnectionRequestComplete(REQUEST_MESSAGE_COPY, new Object[] { messageToken, destinationFolder }, tag, true);
        }
    }

    private void handleSetActiveFolder(FolderTreeItem folder) throws IOException, MailException {
        boolean isStateValid = incomingClient.setActiveFolder(folder, true);
        
        if(!isStateValid) {
            MailConnectionHandlerListener listener = getListener();
            if(listener != null) {
                listener.mailConnectionRequestComplete(REQUEST_FOLDER_REFRESH_REQUIRED, new Object[] { folder }, null, true);
            }
        }
    }
    
    private void checkActiveFolder(FolderTreeItem requestFolder) throws IOException, MailException {
        if(incomingClient.getActiveFolder() == null || !incomingClient.getActiveFolder().getPath().equals(requestFolder.getPath())) {
            handleSetActiveFolder(requestFolder);
        }
    }

    private void checkActiveFolder(MessageToken messageToken) throws IOException, MailException {
        FolderTreeItem invalidFolder = incomingClient.setActiveFolder(messageToken, true);
        
        if(invalidFolder != null) {
            MailConnectionHandlerListener listener = getListener();
            if(listener != null) {
                listener.mailConnectionRequestComplete(REQUEST_FOLDER_REFRESH_REQUIRED, new Object[] { invalidFolder }, null, true);
            }
        }
    }
}
