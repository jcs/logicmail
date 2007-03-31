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
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.FieldChangeListener;
import net.rim.device.api.ui.component.BasicEditField;
import net.rim.device.api.ui.component.ButtonField;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.PasswordEditField;
import org.logicprobe.LogicMail.mail.MailException;
import org.logicprobe.LogicMail.mail.MailClient;
import net.rim.device.api.ui.component.Status;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.Screen;
import net.rim.device.api.ui.component.LabelField;

/**
 * Handle network interaction, wrapping with the appropriate
 * UI popup elements for user feedback and interaction.
 */
public abstract class MailClientHandler {
    protected MailClient client;
    private String taskText;
    private MailClientHandlerListener handlerListener;
    private LoginDialog loginDialog;
    
    /**
     * Creates a new MailClientHandler
     * 
     * @param client MailClient instance to use
     * @param taskText UI popup text describing the task
     */
    public MailClientHandler(MailClient client, String taskText) {
        this.client = client;
        this.taskText = taskText;
        this.handlerListener = null;
    }
    
    /**
     * Set the listener for this handler.
     * This listener will be notified of completion of
     * the MailClient task.
     * 
     * 
     * @param handlerListener Listener
     */
    public void setListener(MailClientHandlerListener clientListener) {
        this.handlerListener = clientListener;
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
                if(activeScreen instanceof Status) {
                    UiApplication.getUiApplication().popScreen(activeScreen);
                }
            } catch (Exception e) { }
            if(this.message != null) {
                Status.show(this.message, this.time);
            }
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
     * Dialog to handle username and password entry
     */
    private static class LoginDialog extends Dialog {
        private BasicEditField fldUser;
        private PasswordEditField fldPass;
        public LoginDialog(String username, String password) {
            super("Authentication", null, null, 0,
                  Bitmap.getPredefinedBitmap(Bitmap.QUESTION),
                  Field.FOCUSABLE | Field.FIELD_HCENTER);
            fldUser = new BasicEditField("Username: ", username);
            fldPass = new PasswordEditField("Password: ", password);
            this.add(fldUser);
            this.add(fldPass);
            this.add(new LabelField("", Field.NON_FOCUSABLE));
            
            ButtonField btnOk = new ButtonField("OK", Field.FOCUSABLE | Field.FIELD_HCENTER);
            btnOk.setChangeListener(new FieldChangeListener() {
                public void fieldChanged(Field field, int context) {
                    if(fldUser.getText().length() > 0 && fldPass.getText().length() > 0) {
                        LoginDialog.this.select(Dialog.OK);
                        LoginDialog.this.close();
                    }
                }
            });
            this.add(btnOk);
        }
        
        public String getUsername() {
            return fldUser.getText();
        }
        
        public String getPassword() {
            return fldPass.getText();
        }
    }
    
    /**
     * Start the background task
     */
    public void start() {
        // Handle the need to enter a username and/or password
        // if the thread is going to need to open a connection,
        // and the username and/or password was not set in the
        // client configuration.
        // Note: In the future, this could be handled as a special
        //       case of the bad username/password handler.
        if(!client.isConnected()) {
            String username = client.getUsername();
            String password = client.getPassword();
            // If the username and password are not null,
            // but are empty, show the login dialog.
            if((username != null && password != null) &&
               (username.trim().equals("") || password.trim().equals(""))) {
               loginDialog = new LoginDialog(username, password);
               if(loginDialog.doModal() == Dialog.OK) {
                   client.setUsername(loginDialog.getUsername());
                   client.setPassword(loginDialog.getPassword());
               }
               else {
                   handlerListener.mailActionComplete(this, false);
               }
            }
        }
        
        Thread thread = new Thread() {
            public void run() {
                try {
                    if(!MailClientHandler.this.client.isConnected()) {
                        // Connect if necessary
                        showStatus("Connecting to server", 100000);
                        while(!MailClientHandler.this.client.open()) {
                            // Show the login dialog on the UI thread, and
                            // wait for it to complete before continuing
                            loginDialog = new LoginDialog(client.getUsername(), client.getPassword());
                            UiApplication.getUiApplication().invokeAndWait(new Runnable() {
                                public void run() {
                                    MailClientHandler.this.loginDialog.doModal();
                                }
                            });
                            if(loginDialog.getSelectedValue() == Dialog.OK) {
                                client.setUsername(loginDialog.getUsername());
                                client.setPassword(loginDialog.getPassword());
                            }
                            else {
                                try { MailClientHandler.this.client.close(); } catch (Exception exp) { }
                                throw new MailException("Authentication failure");
                            }
                        }
                    }
                    // Run the server interaction session
                    showStatus(MailClientHandler.this.taskText, 100000);
                    runSession();
                    showStatus(null, 0); // remove any status screens
                    // Notify the listener of completion
                    if(MailClientHandler.this.handlerListener != null) {
                        MailClientHandler.this.handlerListener.mailActionComplete(MailClientHandler.this, true);
                    }
                } catch (IOException exp) {
                    System.out.println(exp);
                    showStatus("I/O Error", 2000);
                    try { MailClientHandler.this.client.close(); } catch (Exception exp2) { }
                    // Notify the listener of failure
                    if(MailClientHandler.this.handlerListener != null) {
                        MailClientHandler.this.handlerListener.mailActionComplete(MailClientHandler.this, false);
                    }
                } catch (MailException exp) {
                    System.out.println("Protocol error: " + exp);
                    showStatus(exp.getMessage(), 10000);
                    // Notify the listener of failure
                    if(MailClientHandler.this.handlerListener != null) {
                        MailClientHandler.this.handlerListener.mailActionComplete(MailClientHandler.this, false);
                    }
                } catch (Exception exp) {
                    System.out.println("Unknown error: " + exp);
                    showStatus("Unknown error", 2000);
                    try { MailClientHandler.this.client.close(); } catch (Exception exp2) { }
                    // Notify the listener of failure
                    if(MailClientHandler.this.handlerListener != null) {
                        MailClientHandler.this.handlerListener.mailActionComplete(MailClientHandler.this, false);
                    }
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
