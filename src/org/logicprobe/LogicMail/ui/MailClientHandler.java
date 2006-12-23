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
    MailClient _client;
    String _taskText;
    
    public MailClientHandler(MailClient client, String taskText) {
        _client = client;
        _taskText = taskText;
    }
    
    private static class StatusRunnable implements Runnable {
        String _message;
        int _time;
        StatusRunnable(String message, int time) {
            _message = message;
            _time = time;
        }
        public void run() {
            // Remove any previous status screen
            try {
                Screen activeScreen =
                        UiApplication.getUiApplication().getActiveScreen();
                if(activeScreen instanceof Status)
                    UiApplication.getUiApplication().popScreen(activeScreen);
            } catch (Exception e) { }
            if(_message != null)
                Status.show(_message, _time);
        }
    }

    private void showStatus(String message, int time) {
        UiApplication.getUiApplication().invokeLater(new StatusRunnable(message, time));
    }
    
    public void changeStatusMessage(String message) {
        showStatus(message, 100000);
    }
    
    public void start() {
        Thread thread = new Thread() {
            public void run() {
                try {
                    if(!_client.isConnected()) {
                        // Connect if necessary
                        showStatus("Connecting to server", 100000);
                        _client.open();
                    }
                    // Run the server interaction session
                    showStatus(_taskText, 100000);
                    runSession();
                    showStatus(null, 0); // remove any status screens
                } catch (IOException exp) {
                    System.out.println(exp);
                    showStatus("I/O Error", 2000);
                    try { _client.close(); } catch (Exception exp2) { }
                } catch (MailException exp) {
                    System.out.println("Protocol error: " + exp);
                    showStatus(exp.getMessage(), 10000);
                } catch (Exception exp) {
                    System.out.println("Unknown error: " + exp);
                    showStatus("Unknown error", 2000);
                    try { _client.close(); } catch (Exception exp2) { }
                }
            }
        };
        thread.start();
    }

    public abstract void runSession() throws IOException, MailException;
}
