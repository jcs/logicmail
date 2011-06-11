/*-
 * Copyright (c) 2011, Derek Konigsberg
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

package org.logicprobe.LogicMail.util;

import net.rim.device.api.system.EventLogger;

import org.logicprobe.LogicMail.AppInfo;
import org.logicprobe.LogicMail.conf.ConnectionConfig;

public class Watchdog {
    private static final long DEFAULT_TIMEOUT = 15000;
    private static final long DEFAULT_TIMEOUT_MOBILE = 30000;
    private static final long DEFAULT_TIMEOUT_WIFI = 15000;
    
    private final WatchdogListener listener;
    private final WatchdogThread watchdogThread;
    private boolean started;
    private long defaultTimeout = DEFAULT_TIMEOUT;

    public Watchdog(WatchdogListener listener) {
        if(listener == null) {
            throw new NullPointerException("Must supply a listener");
        }
        this.listener = listener;
        this.watchdogThread = new WatchdogThread();
    }

    public void shutdown() {
        if(watchdogThread.isAlive()) {
            watchdogThread.shutdown();
            try {
                watchdogThread.join();
            } catch (InterruptedException e) { }
        }
    }
    
    public long getDefaultTimeout() {
        return defaultTimeout;
    }
    
    public void setDefaultTimeout(long defaultTimeout) {
        this.defaultTimeout = defaultTimeout;
    }
    
    public void setDefaultTimeoutForConnection(int connectionType) {
        switch(connectionType) {
        case ConnectionConfig.TRANSPORT_WIFI_ONLY:
            this.defaultTimeout = DEFAULT_TIMEOUT_WIFI;
            break;
        default:
            this.defaultTimeout = DEFAULT_TIMEOUT_MOBILE;
            break;
        }
    }
    
    public void start() {
        start(defaultTimeout);
    }

    public void start(long timeout) {
        if(started) {
            throw new IllegalStateException();
        }
        if(!watchdogThread.isAlive()) {
            watchdogThread.start();
        }
        watchdogThread.startTimeout(timeout);
        started = true;
    }
    
    public void kick() {
        if(!started) {
            throw new IllegalStateException();
        }
        watchdogThread.kickTimeout();
    }

    public void cancel() {
        if(!started) {
            throw new IllegalStateException();
        }
        watchdogThread.cancelTimeout();
        started = false;
    }

    public boolean isStarted() {
        return started;
    }
    
    private void notifyListener() {
        started = false;
        EventLogger.logEvent(AppInfo.GUID,
                "Watchdog timeout".getBytes(), EventLogger.WARNING);
        listener.watchdogTimeout();
    }

    private class WatchdogThread extends Thread {
        private final Object monitor = new Object();
        private final Object controlLock = new Object();
        
        private boolean shutdown;
        private long timeout;
        private boolean kicked;

        public synchronized void start() {
            synchronized(controlLock) {
                super.start();
                try {
                    controlLock.wait();
                } catch (InterruptedException e) { }
            }
        }
        
        public void run() {
            while(true) {
                // Synchronize on the control lock, and collect the values
                // of all the relevant control fields
                boolean localShutdown;
                long localTimeout;
                synchronized(controlLock) {
                    localShutdown = this.shutdown;
                    localTimeout = this.timeout;
                    controlLock.notify();
                }
                if(localShutdown) { break; }

                // Wait for the timeout duration, or indefinitely if disabled
                // (i.e. localTimeout == 0)
                synchronized(monitor) {
                    try {
                        monitor.wait(localTimeout);
                    } catch (InterruptedException e) { }
                }
                
                // Synchronize on the control lock again, and figure out why
                // the wait() call returned
                boolean notifyListener = false;
                synchronized(controlLock) {
                    // If we were waiting, and the timeout was not zeroed,
                    // then the watchdog timer expired. In this case, we
                    // cancel it and send a notification to the listener.
                    if(localTimeout > 0 && this.timeout > 0 && !kicked) {
                        this.timeout = 0;
                        notifyListener = true;
                    }
                    kicked = false;
                    controlLock.notify();
                }
                if(notifyListener) {
                    Watchdog.this.notifyListener();
                }
            }
        }

        public void startTimeout(long timeout) {
            synchronized(controlLock) {
                this.timeout = timeout;
                synchronized(monitor) {
                    monitor.notify();
                }
                try {
                    controlLock.wait();
                } catch (InterruptedException e) { }
            }
        }

        public void kickTimeout() {
            synchronized(controlLock) {
                kicked = true;
                synchronized(monitor) {
                    monitor.notify();
                }
                try {
                    controlLock.wait();
                } catch (InterruptedException e) { }
            }
        }
        
        public void cancelTimeout() {
            synchronized(controlLock) {
                this.timeout = 0;
                synchronized(monitor) {
                    monitor.notify();
                }
                try {
                    controlLock.wait();
                } catch (InterruptedException e) { }
            }
        }
        
        public void shutdown() {
            synchronized(controlLock) {
                this.timeout = 0;
                this.shutdown = true;
                synchronized(monitor) {
                    monitor.notify();
                }
                try {
                    controlLock.wait();
                } catch (InterruptedException e) { }
            }
        }
    }
}
