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
import java.util.Vector;
import net.rim.device.api.system.Application;
import net.rim.device.api.system.KeypadListener;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Keypad;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.component.RichTextField;
import net.rim.device.api.ui.component.SeparatorField;
import org.logicprobe.LogicMail.conf.ImapConfig;
import org.logicprobe.LogicMail.mail.FolderTreeItem;
import org.logicprobe.LogicMail.mail.IncomingMailClient;
import org.logicprobe.LogicMail.mail.MailException;
import org.logicprobe.LogicMail.mail.imap.ImapClient;
import org.logicprobe.LogicMail.message.FolderMessage;
import org.logicprobe.LogicMail.message.Message;
import org.logicprobe.LogicMail.message.MessageEnvelope;
import org.logicprobe.LogicMail.util.StringParser;

/**
 * Display an E-Mail message
 */
public class MessageScreen extends BaseScreen {
    private IncomingMailClient client;
    private FolderTreeItem folderItem;
    private FolderMessage folderMessage;
    private MessageEnvelope envelope;
    private Vector msgFields;
    private Message msg;
    private UpdateMessageHandler updateMessageHandler;
    private boolean isSentFolder;
    
    public MessageScreen(IncomingMailClient client,
                         FolderTreeItem folderItem,
                         FolderMessage folderMessage)
    {
        super();
        this.client = client;
        this.folderItem = folderItem;
        this.folderMessage = folderMessage;
        this.envelope = folderMessage.getEnvelope();
        
        // Determine if this screen is viewing a sent message
        if(client.getAcctConfig() instanceof ImapConfig) {
            String sentFolderPath = ((ImapConfig)client.getAcctConfig()).getSentFolder();
            if(sentFolderPath != null) {
                this.isSentFolder = folderItem.getPath().equals(sentFolderPath);
            }
        }
        else {
            this.isSentFolder = false;
        }
        
        // Create screen elements
        if(isSentFolder) {
            if(envelope.to != null && envelope.to.length > 0) {
                add(new RichTextField("To: " + envelope.to[0]));
                if(envelope.to.length > 1) {
                    for(int i=1;i<envelope.to.length;i++) {
                        if(envelope.to[i] != null) {
                            add(new RichTextField("    " + envelope.to[i]));
                        }
                    }
                }
            }
        }
        else {
            if(envelope.from != null && envelope.from.length > 0) {
                add(new RichTextField("From: " + envelope.from[0]));
                if(envelope.from.length > 1) {
                    for(int i=1;i<envelope.from.length;i++) {
                        if(envelope.from[i] != null) {
                            add(new RichTextField("      " + envelope.from[i]));
                        }
                    }
                }
            }
        }
        if(envelope.subject != null) {
            add(new RichTextField("Subject: " + envelope.subject));
        }
        add(new SeparatorField());
        updateMessage();
    }

    private void drawMessageFields() {
        if(msgFields == null) {
            return;
        }
        synchronized(Application.getEventLock()) {
            int size = msgFields.size();
            for(int i=0;i<size;++i) {
                if(msgFields.elementAt(i) != null) {
                    add((Field)msgFields.elementAt(i));
                }
                if(i != size-1) {
                    add(new SeparatorField());
                }
            }
        }
    }

    private MenuItem propsItem = new MenuItem("Properties", 100, 10) {
        public void run() {
            showMsgProperties();
        }
    };
    private MenuItem replyItem = new MenuItem("Reply...", 110, 10) {
        public void run() {
            if(msg != null) {
                CompositionScreen screen =
                    new CompositionScreen(
                        client.getAcctConfig(),
                        msg.toReplyMessage());
                UiApplication.getUiApplication().pushModalScreen(screen);
                
                if(screen.getMessageSent()) {
                    updateMessageAnswered();
                }
            }
        }
    };
    private MenuItem replyAllItem = new MenuItem("Reply to all...", 115, 10) {
        public void run() {
            if(msg != null) {
                CompositionScreen screen =
                    new CompositionScreen(
                        client.getAcctConfig(),
                        msg.toReplyAllMessage(client.getAcctConfig().getIdentityConfig().getEmailAddress()));
                UiApplication.getUiApplication().pushModalScreen(screen);

                if(screen.getMessageSent()) {
                    updateMessageAnswered();
                }
            }
        }
    };
    private MenuItem forwardItem = new MenuItem("Forward...", 120, 10) {
        public void run() {
            if(msg != null) {
                CompositionScreen screen =
                    new CompositionScreen(
                        client.getAcctConfig(),
                        msg.toForwardMessage());
                UiApplication.getUiApplication().pushModalScreen(screen);
            }
        }
    };
    private MenuItem compositionItem = new MenuItem("Compose E-Mail", 150, 10) {
        public void run() {
            UiApplication.getUiApplication().pushScreen(new CompositionScreen(client.getAcctConfig()));
        }
    };
    private MenuItem closeItem = new MenuItem("Close", 200000, 10) {
        public void run() {
            onClose();
        }
    };
    protected void makeMenu(Menu menu, int instance) {
        menu.add(propsItem);
        menu.addSeparator();
        if(this.client.getAcctConfig().getOutgoingConfig() != null) {
            menu.add(replyItem);
            if(client.getAcctConfig().getIdentityConfig() != null) {
                menu.add(replyAllItem);
            }
            menu.add(forwardItem);
            menu.add(compositionItem);
            menu.addSeparator();
        }
        menu.add(closeItem);
    }

    public boolean keyChar(char key,
                           int status,
                           int time)
    {
        boolean retval = false;
        switch(key) {
            case Keypad.KEY_ENTER:
            case Keypad.KEY_SPACE:
                if(status == 0) {
                    scroll(Manager.DOWNWARD);
                    retval = true;
                }
                else if(status == KeypadListener.STATUS_ALT) {
                    scroll(Manager.UPWARD);
                    retval = true;
                }
                break;
        }
        return retval;
    }

    private void showMsgProperties() {
        int i;
        StringBuffer msg = new StringBuffer();
        msg.append("Subject:\n  " + ((envelope.subject!=null) ? envelope.subject : "") + "\n");
        msg.append("Date:\n  " + ((envelope.date!=null) ? StringParser.createDateString(envelope.date) : "") + "\n");

        if(envelope.from != null && envelope.from.length > 0) {
            msg.append("From:\n");
            for(i=0;i<envelope.from.length;i++) {
                msg.append("  " + ((envelope.from[i]!=null) ? envelope.from[i] : "") + "\n");
            }
        }
        
        if(envelope.to != null && envelope.to.length > 0) {
            msg.append("To:\n");
            for(i=0;i<envelope.to.length;i++) {
                if(envelope.to[i].length() > 0) {
                    msg.append("  " + ((envelope.to[i]!=null) ? envelope.to[i] : "") + "\n");
                }
            }
        }

        if(envelope.cc != null && envelope.cc.length > 0) {
            msg.append("CC:\n");
            for(i=0;i<envelope.cc.length;i++) {
                if(envelope.cc[i].length() > 0) {
                    msg.append("  " + ((envelope.cc[i]!=null) ? envelope.cc[i] : "") + "\n");
                }
            }
        }
        
        if(envelope.bcc != null && envelope.bcc.length > 0) {
            msg.append("BCC:\n");
            for(i=0;i<envelope.bcc.length;i++) {
                if(envelope.bcc[i].length() > 0) {
                    msg.append("  " + ((envelope.bcc[i]!=null) ? envelope.bcc[i] : "") + "\n");
                }
            }
        }

        Dialog dialog = new Dialog(Dialog.D_OK, msg.toString(),
                                   0, null, Dialog.GLOBAL_STATUS);
        dialog.show();
    }

    /**
     * Flags the currently viewed message as answered.
     */
    private void updateMessageAnswered() {
        if(client instanceof ImapClient) {
            MailClientHandler flagMessageHandler = new MailClientHandler(client, "Updating message status") {
                public void runSession(boolean retry) throws IOException, MailException {
                    ((ImapClient)client).messageAnswered(folderMessage);
                }
            };
            // Start the background process
            flagMessageHandler.start();
        }
    }
    
    private void updateMessage() {
        UpdateMessageHandler updateMessageHandler = new UpdateMessageHandler();
        updateMessageHandler.setListener(new MailClientHandlerListener() {
            public void mailActionComplete(MailClientHandler source, boolean result) {
                source.setListener(null);
                UpdateMessageHandler updateMessageHandler = (UpdateMessageHandler)source;
                if(updateMessageHandler.getMessageFields() != null) {
                    msgFields = updateMessageHandler.getMessageFields();
                    msg = updateMessageHandler.getMessage();
                    drawMessageFields();
                    folderMessage.setSeen(true);
                    folderMessage.setRecent(false);
                }
            }
        });

        // Start the background process
        updateMessageHandler.start();
    }
    
    private class UpdateMessageHandler extends MailClientHandler {
        private Vector msgFields;
        private Message msg;

        public UpdateMessageHandler() {
            super(MessageScreen.this.client, "Retrieving message");
        }

        public void runSession(boolean retry) throws IOException, MailException {
            // Set the active folder
            ((IncomingMailClient)client).setActiveFolder(MessageScreen.this.folderItem);

            // Download the message
            msg = ((IncomingMailClient)client).getMessage(MessageScreen.this.folderMessage);

            // Prepare the UI elements
            if(msg.getBody() != null) {
                MessageRenderer msgRenderer = new MessageRenderer();
                msg.getBody().accept(msgRenderer);
                msgFields = msgRenderer.getMessageFields();
            }
            else {
                msgFields = null;
            }
        }

        public Vector getMessageFields() {
            return msgFields;
        }
        
        public Message getMessage() {
            return msg;
        }
    }
}
