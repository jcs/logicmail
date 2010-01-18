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
import java.util.Vector;

import org.logicprobe.LogicMail.LogicMailResource;
import org.logicprobe.LogicMail.message.FolderMessage;
import org.logicprobe.LogicMail.message.Message;
import org.logicprobe.LogicMail.message.MimeMessageContent;
import org.logicprobe.LogicMail.message.MessageFlags;
import org.logicprobe.LogicMail.message.MimeMessagePart;
import org.logicprobe.LogicMail.util.Queue;

public class IncomingMailConnectionHandler extends AbstractMailConnectionHandler {
    private IncomingMailClient incomingClient;

    // The various mail store requests, mirroring the
    // "requestXXXX()" methods from AbstractMailStore
    public static final int REQUEST_FOLDER_TREE             = 10;
    public static final int REQUEST_FOLDER_EXPUNGE          = 11;
    public static final int REQUEST_FOLDER_STATUS           = 12;
    public static final int REQUEST_FOLDER_MESSAGES_RANGE   = 13;
    public static final int REQUEST_FOLDER_MESSAGES_SET     = 14;
    public static final int REQUEST_FOLDER_MESSAGES_RECENT  = 15;
    public static final int REQUEST_MESSAGE                 = 20;
    public static final int REQUEST_MESSAGE_PARTS           = 21;
    public static final int REQUEST_MESSAGE_DELETE          = 22;
    public static final int REQUEST_MESSAGE_UNDELETE        = 23;
    public static final int REQUEST_MESSAGE_ANSWERED        = 24;
    public static final int REQUEST_MESSAGE_APPEND          = 25;
    public static final int REQUEST_MESSAGE_COPY            = 26;

    /**
     * Maximum amount of time to spend in the idle state.
     * Currently set to 5 minutes. (1000 ms/sec * 60 sec/min * 5 min)
     */
    private static final int IDLE_TIMEOUT = 300000;

    /**
     * Interval to poll the connection in the idle state.
     * Currently set to 500ms.
     */
    private static final int IDLE_POLL_INTERVAL = 500;

    public IncomingMailConnectionHandler(IncomingMailClient client) {
        super(client);
        this.incomingClient = client;
    }

    /**
     * Handles a specific request during the REQUESTS state.
     * 
     * @param type Type identifier for the request.
     * @param params Parameters for the request.
     * @throws IOException on I/O errors
     * @throws MailException on protocol errors
     */
    protected void handleRequest(int type, Object[] params) throws IOException, MailException {
        switch(type) {
        case REQUEST_FOLDER_TREE:
            handleRequestFolderTree();
            break;
        case REQUEST_FOLDER_EXPUNGE:
            handleRequestFolderExpunge((FolderTreeItem)params[0]);
            break;
        case REQUEST_FOLDER_STATUS:
            handleRequestFolderStatus((FolderTreeItem[])params[0]);
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
                    (MessageToken[])params[1]);
            break;
        case REQUEST_FOLDER_MESSAGES_RECENT:
            handleRequestFolderMessagesRecent(
                    (FolderTreeItem)params[0], ((Boolean)params[1]).booleanValue());
            break;
        case REQUEST_MESSAGE:
            handleRequestMessage((MessageToken)params[0]);
            break;
        case REQUEST_MESSAGE_PARTS:
            handleRequestMessageParts((MessageToken)params[0], (MimeMessagePart[])params[1]);
            break;
        case REQUEST_MESSAGE_DELETE:
            handleRequestMessageDelete((MessageToken)params[0], (MessageFlags)params[1]);
            break;
        case REQUEST_MESSAGE_UNDELETE:
            handleRequestMessageUndelete((MessageToken)params[0], (MessageFlags)params[1]);
            break;
        case REQUEST_MESSAGE_ANSWERED:
            handleRequestMessageAnswered((MessageToken)params[0], (MessageFlags)params[1]);
            break;
        case REQUEST_MESSAGE_APPEND:
            handleRequestMessageAppend((FolderTreeItem)params[0], (String)params[1], (MessageFlags)params[2]);
            break;
        case REQUEST_MESSAGE_COPY:
            handleRequestMessageCopy((MessageToken)params[0], (FolderTreeItem)params[1]);
        }
    }

    /**
     * Handles the start of the IDLE state.
     */
    protected void handleBeginIdle() throws IOException, MailException {
        if(incomingClient.hasIdle()) {
            incomingClient.idleModeBegin();
            boolean endIdle = false;
            int idleTime = 0;
            while(!endIdle) {
                sleepConnectionThread(IDLE_POLL_INTERVAL);
                idleTime += IDLE_POLL_INTERVAL;
                if(incomingClient.idleModePoll()) {
                    addRequest(
                            IncomingMailConnectionHandler.REQUEST_FOLDER_MESSAGES_RECENT,
                            new Object[] { incomingClient.getActiveFolder() });
                    endIdle = true;
                }
                else if(getShutdownInProgress()) {
                    endIdle = true;
                }
                else if(idleTime >= IDLE_TIMEOUT) {
                    endIdle = true;
                }
                else
                {
                    Queue requestQueue = getRequestQueue();
                    synchronized(requestQueue) {
                        if(requestQueue.element() != null) {
                            endIdle = true;
                        }
                    }
                }
            }
            incomingClient.idleModeEnd();

            // If the idle state was ended due to a timeout, perform a no-operation
            // command on the mail server as a final explicit check for new messages.
            if(idleTime >= IDLE_TIMEOUT) {
                if(incomingClient.noop()) {
                    addRequest(
                            IncomingMailConnectionHandler.REQUEST_FOLDER_MESSAGES_RECENT,
                            new Object[] { incomingClient.getActiveFolder() });
                }
                else {
                    // If we had a non-INBOX folder selected, then an idle timeout
                    // should switch the active folder back to the INBOX.
                    FolderTreeItem inboxMailbox = incomingClient.getInboxFolder();
                    FolderTreeItem activeMailbox = incomingClient.getActiveFolder();
                    if(inboxMailbox != null && !inboxMailbox.getPath().equalsIgnoreCase(activeMailbox.getPath())) {
                        incomingClient.setActiveFolder(inboxMailbox);
                    }
                }
            }
        }
        else {
            Queue requestQueue = getRequestQueue();
            synchronized(requestQueue) {
                if(requestQueue.element() != null) {
                    return;
                }
                else {
                    try {
                        requestQueue.wait();
                    } catch (InterruptedException e) { }
                }
            }
        }
    }

    private void handleRequestFolderTree() throws IOException, MailException {
        String message = resources.getString(LogicMailResource.MAILCONNECTION_REQUEST_FOLDER_TREE);
        showStatus(message);
        FolderTreeItem root = incomingClient.getFolderTree(getProgressHandler(message));

        MailConnectionHandlerListener listener = getListener();
        if(root != null && listener != null) {
            listener.mailConnectionRequestComplete(REQUEST_FOLDER_TREE, root);
        }
    }

    private void handleRequestFolderExpunge(FolderTreeItem folder) throws IOException, MailException {
        String message = resources.getString(LogicMailResource.MAILCONNECTION_REQUEST_FOLDER_EXPUNGE);
        showStatus(message);
        checkActiveFolder(folder);
        incomingClient.expungeActiveFolder();

        MailConnectionHandlerListener listener = getListener();
        if(listener != null) {
            listener.mailConnectionRequestComplete(REQUEST_FOLDER_EXPUNGE, folder);
        }
    }

    private void handleRequestFolderStatus(FolderTreeItem[] folders) throws IOException, MailException {
        String message = resources.getString(LogicMailResource.MAILCONNECTION_REQUEST_FOLDER_STATUS);
        showStatus(message);
        incomingClient.refreshFolderStatus(folders, getProgressHandler(message));

        MailConnectionHandlerListener listener = getListener();
        if(listener != null) {
            listener.mailConnectionRequestComplete(REQUEST_FOLDER_STATUS, folders);
        }
    }

    private static class GetFolderMessageCallback implements FolderMessageCallback {
        private int maxCount;
        private int type;
        private FolderTreeItem folder;
        private MailConnectionHandlerListener listener;
        private Object param;
        private FolderMessage[] folderMessages;
        private int count;

        public GetFolderMessageCallback(int maxCount, int type, FolderTreeItem folder, MailConnectionHandlerListener listener) {
            this(maxCount, type, folder, listener, null);
        }

        public GetFolderMessageCallback(int maxCount, int type, FolderTreeItem folder, MailConnectionHandlerListener listener, Object param) {
            this.maxCount = maxCount;
            this.type = type;
            this.folder = folder;
            this.listener = listener;
            this.param = param;
            this.count = 0;
        }

        public void folderMessageUpdate(FolderMessage folderMessage) {
            FolderMessage[] resultMessages = null;
            synchronized(this) {
                if(folderMessage != null) {
                    if(count == 0) {
                        folderMessages = new FolderMessage[maxCount];
                    }
                    folderMessages[count++] = folderMessage;
                }

                if(count == maxCount) {
                    resultMessages = folderMessages;
                    folderMessages = null;
                    count = 0;
                }
                else if(folderMessage == null) {
                    resultMessages = new FolderMessage[count];
                    for(int i=0; i<count; i++) {
                        resultMessages[i] = folderMessages[i];
                    }
                    folderMessages = null;
                    count = 0;
                }
            }

            if(listener != null) {
                if(resultMessages != null) {
                    if(param == null) {
                        listener.mailConnectionRequestComplete(type, new Object[] { folder, resultMessages });
                    }
                    else {
                        listener.mailConnectionRequestComplete(type, new Object[] { folder, resultMessages, param });
                    }
                }

                // If this is the last update of the sequence, make sure we notify
                // the listener with a null array so it knows we are done.
                if(folderMessage == null) {
                    if(param == null) {
                        listener.mailConnectionRequestComplete(type, new Object[] { folder, null });
                    }
                    else {
                        listener.mailConnectionRequestComplete(type, new Object[] { folder, null, param });
                    }
                }
            }
        }
    };

    private void handleRequestFolderMessagesRange(FolderTreeItem folder, int firstIndex, int lastIndex) throws IOException, MailException {
        String message = resources.getString(LogicMailResource.MAILCONNECTION_REQUEST_FOLDER_MESSAGES);
        showStatus(message + "...");
        checkActiveFolder(folder);

        incomingClient.getFolderMessages(
                firstIndex, lastIndex,
                new GetFolderMessageCallback(
                        getFolderMessageUpdateFrequency(),
                        REQUEST_FOLDER_MESSAGES_RANGE,
                        folder,
                        getListener()),
                        getProgressHandler(message));
    }
    
    private void handleRequestFolderMessagesSet(FolderTreeItem folder, MessageToken[] messageTokens) throws IOException, MailException {
        String message = resources.getString(LogicMailResource.MAILCONNECTION_REQUEST_FOLDER_MESSAGES);
        showStatus(message + "...");
        checkActiveFolder(folder);

        incomingClient.getFolderMessages(
                messageTokens,
                new GetFolderMessageCallback(
                        getFolderMessageUpdateFrequency(),
                        REQUEST_FOLDER_MESSAGES_SET,
                        folder,
                        getListener()),
                        getProgressHandler(message));
    }
    
    private void handleRequestFolderMessagesRecent(FolderTreeItem folder, boolean flagsOnly) throws IOException, MailException {
        String message = resources.getString(LogicMailResource.MAILCONNECTION_REQUEST_FOLDER_MESSAGES);
        showStatus(message + "...");
        checkActiveFolder(folder);

        incomingClient.getNewFolderMessages(
                flagsOnly,
                new GetFolderMessageCallback(
                        getFolderMessageUpdateFrequency(),
                        REQUEST_FOLDER_MESSAGES_RECENT,
                        folder,
                        getListener(),
                        new Boolean(flagsOnly)),
                        getProgressHandler(message));
    }

    private int getFolderMessageUpdateFrequency() {
        // Replace this with a more general method:
        int frequency;
        if(incomingClient instanceof org.logicprobe.LogicMail.mail.imap.ImapClient) {
            frequency = 5;
        }
        else if(incomingClient instanceof org.logicprobe.LogicMail.mail.pop.PopClient) {
            frequency = 2;
        }
        else {
            frequency = 10;
        }
        return frequency;
    }
    
    private void handleRequestMessage(MessageToken messageToken) throws IOException, MailException {
        String statusMessage = resources.getString(LogicMailResource.MAILCONNECTION_REQUEST_MESSAGE);
        showStatus(statusMessage);
        checkActiveFolder(messageToken);

        Message message = incomingClient.getMessage(messageToken, getProgressHandler(statusMessage));

        MailConnectionHandlerListener listener = getListener();
        if(message != null && listener != null) {
            listener.mailConnectionRequestComplete(REQUEST_MESSAGE, new Object[] { messageToken, message.getStructure(), message.getAllContent() });
        }
    }
    
    private void handleRequestMessageParts(MessageToken messageToken, MimeMessagePart[] messageParts) throws IOException, MailException {
        String statusMessage = resources.getString(LogicMailResource.MAILCONNECTION_REQUEST_MESSAGE);
        showStatus(statusMessage);
        checkActiveFolder(messageToken);

        MimeMessageContent[] messageContent;

        // Replace this with a more general method:
        if(incomingClient instanceof org.logicprobe.LogicMail.mail.imap.ImapClient) {
            Vector messageContentVector = new Vector();
            for(int i=0; i<messageParts.length; i++) {
                MimeMessageContent content =
                    ((org.logicprobe.LogicMail.mail.imap.ImapClient)incomingClient).getMessagePart(messageToken, messageParts[i], getProgressHandler(statusMessage));
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
            listener.mailConnectionRequestComplete(REQUEST_MESSAGE_PARTS, new Object[] { messageToken, messageContent });
        }
    }

    private void handleRequestMessageDelete(MessageToken messageToken, MessageFlags messageFlags) throws IOException, MailException {
        showStatus(resources.getString(LogicMailResource.MAILCONNECTION_REQUEST_MESSAGE_DELETE));
        checkActiveFolder(messageToken);

        incomingClient.deleteMessage(messageToken, messageFlags);

        MailConnectionHandlerListener listener = getListener();
        if(listener != null) {
            listener.mailConnectionRequestComplete(REQUEST_MESSAGE_DELETE, new Object[] { messageToken, messageFlags });
        }
    }
    
    private void handleRequestMessageUndelete(MessageToken messageToken, MessageFlags messageFlags) throws IOException, MailException {
        showStatus(resources.getString(LogicMailResource.MAILCONNECTION_REQUEST_MESSAGE_UNDELETE));
        checkActiveFolder(messageToken);

        incomingClient.undeleteMessage(messageToken, messageFlags);

        MailConnectionHandlerListener listener = getListener();
        if(listener != null) {
            listener.mailConnectionRequestComplete(REQUEST_MESSAGE_UNDELETE, new Object[] { messageToken, messageFlags });
        }
    }
    
    private void handleRequestMessageAnswered(MessageToken messageToken, MessageFlags messageFlags) throws IOException, MailException {
        showStatus(resources.getString(LogicMailResource.MAILCONNECTION_REQUEST_MESSAGE_ANSWERED));
        // Replace this with a more general method:
        if(incomingClient instanceof org.logicprobe.LogicMail.mail.imap.ImapClient) {
            ((org.logicprobe.LogicMail.mail.imap.ImapClient)incomingClient).messageAnswered(messageToken, messageFlags);
        }
        messageFlags.setAnswered(true);

        MailConnectionHandlerListener listener = getListener();
        if(listener != null) {
            listener.mailConnectionRequestComplete(REQUEST_MESSAGE_ANSWERED, new Object[] { messageToken, messageFlags });
        }
    }
    
    private void handleRequestMessageAppend(FolderTreeItem folder, String rawMessage, MessageFlags initialFlags) throws IOException, MailException {
        showStatus(resources.getString(LogicMailResource.MAILCONNECTION_REQUEST_MESSAGE_APPEND));
        // Clean up this interface:
        if(incomingClient.hasAppend()) {
            incomingClient.appendMessage(folder, rawMessage, initialFlags);
        }

        MailConnectionHandlerListener listener = getListener();
        if(listener != null) {
            // Using a null FolderMessage since no information is returned on the appended message:
            listener.mailConnectionRequestComplete(REQUEST_MESSAGE_APPEND, new Object[] { folder, null });
        }
    }
    
    private void handleRequestMessageCopy(MessageToken messageToken, FolderTreeItem destinationFolder) throws IOException, MailException {
        showStatus(resources.getString(LogicMailResource.MAILCONNECTION_REQUEST_MESSAGE_COPY));
        if(incomingClient.hasCopy()) {
            checkActiveFolder(messageToken);
            incomingClient.copyMessage(messageToken, destinationFolder);
        }

        MailConnectionHandlerListener listener = getListener();
        if(listener != null) {
            // Using a null FolderMessage since no information is returned on the appended message:
            listener.mailConnectionRequestComplete(REQUEST_MESSAGE_COPY, new Object[] { messageToken, destinationFolder });
        }
    }

    private void checkActiveFolder(FolderTreeItem requestFolder) throws IOException, MailException {
        if(incomingClient.getActiveFolder() == null || !incomingClient.getActiveFolder().getPath().equals(requestFolder.getPath())) {
            incomingClient.setActiveFolder(requestFolder);
        }
    }

    private void checkActiveFolder(MessageToken messageToken) throws IOException, MailException {
        incomingClient.setActiveFolder(messageToken);
    }
}
