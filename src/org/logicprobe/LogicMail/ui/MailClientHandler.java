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

package org.logicprobe.LogicMail.ui;

import java.io.IOException;
import org.logicprobe.LogicMail.mail.MailException;
import org.logicprobe.LogicMail.mail.MailClient;
import net.rim.device.api.ui.component.Status;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.Screen;

/**
 * Handle network interaction, wrapping with the appropriate
 * UI popup elements for user feedback and interaction.
 */
public abstract class MailClientHandler {
    protected MailClient client;
    private String taskText;
    private MailClientListener clientListener;
    
    /**
     * Creates a new MailClientHandler
     * @param client MailClient instance to use
     * @param taskText UI popup text describing the task
     */
    public MailClientHandler(MailClient client, String taskText) {
        this.client = client;
        this.taskText = taskText;
        this.clientListener = null;
    }
    
    /**
     * Set the listener for this handler.
     * This listener will be notified of completion of
     * the MailClient task.
     * @param clientListener Listener
     */
    public void setMailClientListener(MailClientListener clientListener) {
        this.clientListener = clientListener;
    }
    
    private static class StatusRunnable implements Runnable {
        String message;
        int time;
        StatusRunnable(String message, int time) {
            this.message = message;
            this.time = time;
        }
        public void run() {
            // Remove any previous status screen
            try {
                Screen activeScreen =
                        UiApplication.getUiApplication().getActiveScreen();
                if(activeScreen instanceof Status)
                    UiApplication.getUiApplication().popScreen(activeScreen);
            } catch (Exception e) { }
            if(this.message != null)
                Status.show(this.message, this.time);
        }
    }

    private void showStatus(String message, int time) {
        UiApplication.getUiApplication().invokeLater(new StatusRunnable(message, time));
    }

    /**
     * Change the status message.
     * Commonly used if this is a multi-step task.
     * @param message New status message to display
     */
    public void changeStatusMessage(String message) {
        showStatus(message, 100000);
    }
    
    /**
     * Start the background task
     */
    public void start() {
        Thread thread = new Thread() {
            public void run() {
                try {
                    if(!MailClientHandler.this.client.isConnected()) {
                        // Connect if necessary
                        showStatus("Connecting to server", 100000);
                        MailClientHandler.this.client.open();
                    }
                    // Run the server interaction session
                    showStatus(MailClientHandler.this.taskText, 100000);
                    runSession();
                    showStatus(null, 0); // remove any status screens
                    // Notify the listener of completion
                    if(MailClientHandler.this.clientListener != null)
                        MailClientHandler.this.clientListener.mailActionComplete(MailClientHandler.this, true);
                } catch (IOException exp) {
                    System.out.println(exp);
                    showStatus("I/O Error", 2000);
                    try { MailClientHandler.this.client.close(); } catch (Exception exp2) { }
                    // Notify the listener of failure
                    if(MailClientHandler.this.clientListener != null)
                        MailClientHandler.this.clientListener.mailActionComplete(MailClientHandler.this, false);
                } catch (MailException exp) {
                    System.out.println("Protocol error: " + exp);
                    showStatus(exp.getMessage(), 10000);
                    // Notify the listener of failure
                    if(MailClientHandler.this.clientListener != null)
                        MailClientHandler.this.clientListener.mailActionComplete(MailClientHandler.this, false);
                } catch (Exception exp) {
                    System.out.println("Unknown error: " + exp);
                    showStatus("Unknown error", 2000);
                    try { MailClientHandler.this.client.close(); } catch (Exception exp2) { }
                    // Notify the listener of failure
                    if(MailClientHandler.this.clientListener != null)
                        MailClientHandler.this.clientListener.mailActionComplete(MailClientHandler.this, false);
                }
            }
        };
        thread.start();
    }

    /**
     * Override this method to define the particular MailClient
     * task to be executed.
     */
    public abstract void runSession() throws IOException, MailException;
}
