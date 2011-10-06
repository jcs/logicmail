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
package org.logicprobe.LogicMail.ui;

import java.io.IOException;

import org.logicprobe.LogicMail.LogicMailResource;
import org.logicprobe.LogicMail.conf.AccountConfig;
import org.logicprobe.LogicMail.conf.OutgoingConfig;
import org.logicprobe.LogicMail.mail.MailClient;
import org.logicprobe.LogicMail.mail.MailClientFactory;
import org.logicprobe.LogicMail.mail.MailException;
import org.logicprobe.LogicMail.util.WrappedIOException;

import net.rim.device.api.i18n.MessageFormat;
import net.rim.device.api.i18n.ResourceBundle;
import net.rim.device.api.ui.Color;
import net.rim.device.api.ui.FieldChangeListener;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.container.VerticalFieldManager;

/**
 * Handles account configuration testing for the setup wizard.
 */
public class AccountConfigTester {
    protected static ResourceBundle resources = ResourceBundle.getBundle(LogicMailResource.BUNDLE_ID, LogicMailResource.BUNDLE_NAME);

    private final static int TYPE_INFO = 0;
    private final static int TYPE_SUCCESS = 1;
    private final static int TYPE_FAILURE = 2;
    private final static int TYPE_FAILURE_DETAIL = 3;
    
    public final static int TEST_COMPLETE = 1000;
    
    private VerticalFieldManager testOutputManager;
    private boolean testInProgress;
    private volatile boolean testCanceled;
    
    public AccountConfigTester() {
        testOutputManager = new VerticalFieldManager();
    }
    
    /**
     * Gets the test output manager, which should be added to the screen
     * being used to display connection test results.
     *
     * @return the test output manager
     */
    public VerticalFieldManager getTestOutputManager() {
        return testOutputManager;
    }
    
    /**
     * Checks if a connection test is in progress.
     *
     * @return true, if the test is in progress
     */
    public boolean isTestInProgress() {
        return testInProgress;
    }
    
    /**
     * Sets the test as canceled, so that it will cease at the earliest
     * opportunity.
     */
    public void setTestCanceled() {
        this.testCanceled = true;
        testOutputManager.deleteAll();
    }
    
    /**
     * Runs the connection test process on a background thread.
     * The test output manager will be updated as the test progresses,
     * and its {@link FieldChangeListener} will be notified upon test
     * completion with a TEST_COMPLETE context parameter.
     *
     * @param accountConfig the account configuration to test
     */
    public void runConnectionTest(final AccountConfig accountConfig) {
        testOutputManager.deleteAll();
        testInProgress = true;
        testCanceled = false;
        (new Thread() { public void run() {
            addText(MessageFormat.format(
                    resources.getString(LogicMailResource.WIZARD_SCREEN_FINAL_TESTING),
                    new Object[] { accountConfig.toString() }),
                    TYPE_INFO);
            MailClient mailClient = MailClientFactory.createTemporaryMailClient(accountConfig);
            testClientConnection(mailClient);
            
            if(testCanceled) { testComplete(); return; }
            
            if(accountConfig.getOutgoingConfig() != null) {
                OutgoingConfig outgoingConfig = accountConfig.getOutgoingConfig();
                addText(MessageFormat.format(
                        resources.getString(LogicMailResource.WIZARD_SCREEN_FINAL_TESTING),
                        new Object[] { outgoingConfig.toString() }),
                        TYPE_INFO);
                mailClient = MailClientFactory.createTemporaryOutgoingMailClient(outgoingConfig);
                testClientConnection(mailClient);
            }
            testComplete();
        }}).start();
    }
    
    private void testClientConnection(MailClient mailClient) {
        boolean success;
        String errorMessage = null;
        String errorDetail = null;
        try {
            if(checkLogin(mailClient)) {
                success = mailClient.open();
                errorMessage = resources.getString(LogicMailResource.WIZARD_SCREEN_FINAL_LOGIN_FAILED);
            }
            else {
                success = false;
                errorMessage = resources.getString(LogicMailResource.WIZARD_SCREEN_FINAL_LOGIN_CANCELED);
            }
        } catch (IOException exp) {
            success = false;
            errorMessage = exp.getMessage();
            if(exp instanceof WrappedIOException) {
                Throwable innerExp = ((WrappedIOException)exp).getInnerException();
                if(innerExp != null) {
                    errorDetail = innerExp.getMessage();
                }
            }
        } catch (MailException exp) {
            success = false;
            errorMessage = exp.getMessage();
        }
        
        if(mailClient.isConnected()) {
            try {
                mailClient.close();
            } catch (Exception e) { }
        }
        
        if(testCanceled) { return; }
        
        if(success) {
            addText(resources.getString(LogicMailResource.WIZARD_SCREEN_FINAL_CONNECTION_SUCCESSFUL), TYPE_SUCCESS);
        }
        else {
            addText(resources.getString(LogicMailResource.WIZARD_SCREEN_FINAL_CONNECTION_FAILED), TYPE_FAILURE);
            if(errorMessage != null) {
                addText(errorMessage, TYPE_FAILURE_DETAIL);
                if(errorDetail != null) {
                    addText(errorDetail, TYPE_FAILURE_DETAIL);
                }
            }
        }
    }
    
    private boolean checkLogin(final MailClient client) {
        if(!client.isLoginRequired()) { return true; }

        String username = client.getUsername();
        String password = client.getPassword();
        // If the username and password are not null,
        // but are empty, request login information.
        if((username != null && password != null)
                && (username.trim().equals("") || password.trim().equals(""))) {

            final boolean[] canceled = new boolean[1];
            UiApplication.getUiApplication().invokeAndWait(new Runnable() {
                public void run() {
                    String username = client.getUsername();
                    String password = client.getPassword();
                    LoginDialog dialog = new LoginDialog(username, password);
                    if(dialog.doModal() == Dialog.OK) {
                        client.setUsername(dialog.getUsername());
                        client.setPassword(dialog.getPassword());
                        canceled[0] = false;
                    }
                    else {
                        canceled[0] = true;
                    }
                }
            });

            return !canceled[0];
        }
        else {
            return true;
        }
    }
    
    private void testComplete() {
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                FieldChangeListener listener = testOutputManager.getChangeListener();
                if(listener != null) {
                    listener.fieldChanged(testOutputManager, TEST_COMPLETE);
                }
                testInProgress = false;
                if(testCanceled) {
                    testOutputManager.deleteAll();
                }
            }
        });
    }

    private void addText(final String text, final int type) {
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                testOutputManager.add(new StatusLabelField(text, type));
            }
        });
    }
    
    class StatusLabelField extends LabelField {
        private final int type;
        StatusLabelField(final String text, final int type) {
            super(text);
            this.type = type;
            if(type == TYPE_SUCCESS) {
                this.setFont(this.getFont().derive(Font.BOLD));
            }
            else if(type == TYPE_FAILURE) {
                this.setFont(this.getFont().derive(Font.BOLD));
            }
            else if(type == TYPE_FAILURE_DETAIL) {
                this.setFont(this.getFont().derive(Font.ITALIC));
            }
        }
        
        protected void paint(Graphics graphics) {
            if(type == TYPE_SUCCESS) {
                graphics.setColor(Color.GREEN);
            }
            else if(type == TYPE_FAILURE || type == TYPE_FAILURE_DETAIL) {
                graphics.setColor(Color.RED);
            }
            super.paint(graphics);
        };
    }
}
