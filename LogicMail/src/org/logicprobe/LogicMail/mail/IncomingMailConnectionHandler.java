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

import net.rim.device.api.system.Backlight;
import net.rim.device.api.system.DeviceInfo;
import net.rim.device.api.ui.UiApplication;

import org.logicprobe.LogicMail.conf.AccountConfig;
import org.logicprobe.LogicMail.message.MessageFlags;
import org.logicprobe.LogicMail.util.Queue;

public class IncomingMailConnectionHandler extends AbstractMailConnectionHandler {
    private final NetworkMailStore mailStore;
    private final IncomingMailClient incomingClient;
    private FolderTreeItem previousActiveFolder;
    private AccountConfig accountConfig;
    
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
    
    /**
     * Maximum amount of time to keep a connection open on a mail store that
     * uses locked folders.  Currently set to 4 minutes, so it is slightly
     * shorter than the smallest configurable polling frequency.
     */
    private static final int LOCKED_TIMEOUT = 240000;
    
    /**
     * Rate at which to check device status while an idle connection is open
     * on a mail store with locked folders.
     */
    private static final int LOCKED_INTERVAL = 15000;
    
    private final Timer idleTimer = new Timer();
    private TimerTask idleTimerTask;
    private boolean idleTimeout;
    private boolean idleRecentMessagesRequested;
    private long idleStartTime;
    private boolean idleEnabledAtBegin;
    
    private volatile long accumulatedIdleTime;
    private final Timer pollingTimer = new Timer();
    private TimerTask pollingTimerTask;
    
    private static final int MS_PER_MIN = 60000;
    private static final int REFRESH_TOLERANCE = 60000;
    
    /**
     * Listener to handle asynchronous notifications from the mail client.
     */
    private IncomingMailClientListener mailClientListener = new IncomingMailClientListener() {
        public void recentFolderMessagesAvailable(FolderTreeItem folder) {
            handleRecentFolderMessagesAvailable(folder);
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
    
    public IncomingMailConnectionHandler(NetworkMailStore mailStore, IncomingMailClient client) {
        super(client);
        this.accountConfig = client.getAcctConfig();
        this.mailStore = mailStore;
        this.incomingClient = client;
        this.incomingClient.setListener(mailClientListener);
    }

    public void start() {
        cleanupIdleState();
        super.start();
    }
    
    public void shutdown(boolean wait) {
        cleanupIdleState();
        super.shutdown(wait);
    }
    
    protected void handleRequest(ConnectionHandlerRequest request) throws IOException, MailException {
        cleanupIdleState();
        super.handleRequest(request);
    }
    
    private void handleRecentFolderMessagesAvailable(FolderTreeItem folder) {
        if(getConnectionState() == STATE_IDLE) {
            if(idleRecentMessagesRequested) { return; }
            idleRecentMessagesRequested = true;
        }
        
        ConnectionHandlerRequest currentRequest = getRequestInProgress();
        if(currentRequest instanceof FolderMessagesRequest) {
            // Ignore this event if it occurs during the setup portion of the
            // command normally enqueued as a result of this notification.
            FolderMessagesRequest folderRequest = (FolderMessagesRequest)currentRequest;
            if(folderRequest.getType() == FolderMessagesRequest.TYPE_RECENT
                    && folderRequest.getFolder().getPath().equals(folder.getPath())) {
                return;
            }
        }
        else if(currentRequest instanceof ImapFolderRefreshRequest) {
            // If this event occurs during a folder refresh, then notify the
            // in-progress refresh request instead of creating a new request.
            ImapFolderRefreshRequest refreshRequest = (ImapFolderRefreshRequest)currentRequest;
            if(refreshRequest.getFolder().getPath().equals(folder.getPath())) {
                refreshRequest.notifyRecentMessagesAvailable();
                return;
            }
        }
        FolderMessagesRequest request = mailStore.createFolderMessagesRecentRequest(folder, false);
        ((ConnectionHandlerRequest)request).setDeliberate(false);
        mailStore.processRequest(request);
    }

    private void handleFolderMessageFlagsChanged(MessageToken token, MessageFlags messageFlags) {
        // This notification just updates local data, so it does not need to
        // break out of the idle state.
        
        mailStore.fireMessageFlagsChanged(token, messageFlags);
    }

    private void handleFolderMessageExpunged(MessageToken expungedToken, MessageToken[] updatedTokens) {
        // This notification just updates local data, so it does not need to
        // break out of the idle state.
        
        mailStore.fireFolderExpunged(incomingClient.getActiveFolder(), new MessageToken[] { expungedToken }, updatedTokens);
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
        
        idleEnabledAtBegin = incomingClient.isIdleEnabled();
        
        if(incomingClient.hasIdle() && idleEnabledAtBegin) {
            startIdleTimer(IDLE_TIMEOUT);
            incomingClient.idleModeBegin();
        }
        else if(!incomingClient.hasLockedFolders()) {
            // In this case, we do a NOOP-based polling
            startIdleTimer(NOOP_TIMEOUT);
        }
        else {
            // If we got here, that means we are on a mail store that locks
            // folders while the client is connected. In this case, the only
            // reason to keep the connection open is to improve the user
            // experience.  However, we should still eventually timeout and
            // disconnect.
            startLockedFoldersIdleTimer();
        }
    }

    private void startIdleTimer(int timeout) {
        idleRecentMessagesRequested = false;
        idleTimeout = false;
        idleTimerTask = new TimerTask() {
            public void run() {
                handleIdleModeTimeout();
            }
        };
        idleTimer.schedule(idleTimerTask, timeout);
    }
    
    private void startLockedFoldersIdleTimer() {
        idleRecentMessagesRequested = false;
        idleTimeout = false;
        idleTimerTask = new TimerTask() {
            public void run() {
                boolean handleTimeout = false;
                long timeInIdle = System.currentTimeMillis() - idleStartTime;
                if(timeInIdle > LOCKED_TIMEOUT) {
                    // If we've been idle longer than the timeout constant, then
                    // consider this an idle timeout.
                    handleTimeout = true;
                }
                else if(DeviceInfo.isInHolster()) {
                    // Device is in the holster, and thus assumed to not be
                    // in use.
                    handleTimeout = true;
                }
                else if(!UiApplication.getUiApplication().isForeground()) {
                    // Application is in the background, and not being
                    // interacted with by the user.
                    handleTimeout = true;
                }
                else if(!Backlight.isEnabled() && DeviceInfo.getIdleTime() > 30) {
                    // The screen backlight as shut off, and the user has not
                    // touched the device for at least 30 seconds. In this case,
                    // we are assuming that the device probably isn't in use.
                    handleTimeout = true;
                }
                
                if(handleTimeout) {
                    handleIdleModeTimeout();
                }
            }
        };
        idleTimer.scheduleAtFixedRate(idleTimerTask, LOCKED_INTERVAL, LOCKED_INTERVAL);
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
        
        if(incomingClient.hasIdle() && idleEnabledAtBegin) {
            incomingClient.idleModeEnd();
        }
        
        if(idleTimeout) {
            if(incomingClient.hasLockedFolders()) {
                // An idle timeout on a mail store with locked folders should
                // be handled by promptly disconnecting.  That way, any further
                // data requests will cause a reconnect and get fresh data.
                NetworkDisconnectRequest disconnectRequest = new NetworkDisconnectRequest(mailStore, NetworkDisconnectRequest.REQUEST_DISCONNECT_TIMEOUT);
                disconnectRequest.setDeliberate(false);
                mailStore.processRequest(disconnectRequest);
            }
            else {
                // If idle mode was disabled for long enough that a timeout
                // occurred, then something forgot to re-enable it. Just to be
                // safe, we will re-enable it behind the scenes.
                incomingClient.setIdleEnabled(true);
                
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
                    else if(inboxFolder != null && activeFolder == null) {
                        handleSetActiveFolder(inboxFolder);
                    }
                }
                handleConnectionIdleTimeout(System.currentTimeMillis() - idleStartTime);
            }
        }
    }
    
    void handleRequestDisconnect() throws IOException, MailException {
        this.previousActiveFolder = null;
        throw new MailException("", true, REQUEST_DISCONNECT);
    }
    
    void handleRequestDisconnectTimeout() throws IOException, MailException {
        this.previousActiveFolder = null;
        handleConnectionDisconnectTimeout(System.currentTimeMillis() - idleStartTime);
        throw new MailException("", true, REQUEST_DISCONNECT_TIMEOUT);
    }

    private void handleSetActiveFolder(FolderTreeItem folder) throws IOException, MailException {
        boolean isStateValid = incomingClient.setActiveFolder(folder, true);
        
        if(!isStateValid) {
            mailStore.fireFolderRefreshRequired(folder, false);
        }
    }
    
    FolderTreeItem getActiveFolder() {
        return incomingClient.getActiveFolder();
    }
    
    private void handleConnectionIdleTimeout(long idleDuration) {
        accumulatedIdleTime += idleDuration;
        
        long refreshFrequency = accountConfig.getRefreshFrequency() * MS_PER_MIN;

        if(refreshFrequency == 0) {
            accumulatedIdleTime = 0;
            return;
        }
        
        // If we've been idle for a time period close to the refresh frequency,
        // then we should trigger a refresh.
        if(Math.abs(accumulatedIdleTime - refreshFrequency) < REFRESH_TOLERANCE) {
            accumulatedIdleTime = 0;
            mailStore.fireRefreshRequired(false);
        }
    }
    
    private void handleConnectionDisconnectTimeout(long idleDuration) {
        accumulatedIdleTime += idleDuration;
        
        long refreshFrequency = accountConfig.getRefreshFrequency() * MS_PER_MIN;
        
        if(refreshFrequency == 0) {
            accumulatedIdleTime = 0;
            return;
        }
        
        // If we've been idle for a time period close to the refresh frequency,
        // then we should trigger a refresh.
        if((accumulatedIdleTime + (REFRESH_TOLERANCE >>> 1)) > refreshFrequency) {
            accumulatedIdleTime = 0;
            mailStore.fireRefreshRequired(false);
        }
        else {
            // Otherwise, we should start the polling timer
            long nextRefresh = refreshFrequency - accumulatedIdleTime;
            
            schedulePollingRefresh(nextRefresh);
        }
    }
    
    protected void handleFailedConnection(boolean isSilent) {
        long refreshFrequency = accountConfig.getRefreshFrequency() * MS_PER_MIN;
        if(refreshFrequency == 0) { return; }
        
        schedulePollingRefresh(refreshFrequency);
    }

    public void startPollingThread() {
        long refreshFrequency = 0;
        synchronized(pollingTimer) {
            if(getConnectionState() == STATE_CLOSED && pollingTimerTask == null) {
                refreshFrequency = accountConfig.getRefreshFrequency() * MS_PER_MIN;
            }
        }
        if(refreshFrequency > 0) {
            schedulePollingRefresh(refreshFrequency);
        }
    }

    private void schedulePollingRefresh(long nextRefresh) {
        synchronized(pollingTimer) {
            if(pollingTimerTask != null) {
                pollingTimerTask.cancel();
                pollingTimerTask = null;
            }
            pollingTimerTask = new TimerTask() {
                public void run() {
                    accumulatedIdleTime = 0;
                    mailStore.fireRefreshRequired(false);
                }
            };
            pollingTimer.schedule(pollingTimerTask, nextRefresh);
        }
    }
    
    private void cleanupIdleState() {
        synchronized(pollingTimer) {
            if(pollingTimerTask != null) {
                pollingTimerTask.cancel();
                pollingTimerTask = null;
            }
        }
        accumulatedIdleTime = 0;
    }
}
