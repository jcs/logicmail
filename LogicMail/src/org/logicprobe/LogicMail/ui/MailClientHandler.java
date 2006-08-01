/*
 * MailClientHandler.java
 *
 * Created on July 30, 2006, 8:50 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
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
    
    private class StatusRunnable implements Runnable {
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
