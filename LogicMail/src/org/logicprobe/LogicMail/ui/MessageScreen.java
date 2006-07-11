/*
 * MessageScreen.java
 *
 * © <your company here>, 2003-2005
 * Confidential and proprietary.
 */

package org.logicprobe.LogicMail.ui;

import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.ui.container.*;
import net.rim.device.api.system.Application;
import org.logicprobe.LogicMail.mail.MailClient;
import org.logicprobe.LogicMail.mail.MailException;
import org.logicprobe.LogicMail.mail.Message;
import java.io.IOException;

/**
 * Display an E-Mail message
 */
public class MessageScreen extends MainScreen {
    private MailClient client;
    private MailClient.FolderItem folderItem;
    private MailClient.MessageEnvelope envelope;

    private RichTextField bodyField;
    private Message message;
    static class MessageBody {
        String text;
    };
    
    public MessageScreen(MailClient client,
                         MailClient.FolderItem folderItem,
                         MailClient.MessageEnvelope envelope)
    {
        super();
        this.client = client;
        this.folderItem = folderItem;
        this.envelope = envelope;
        message = null;
        
        // Create screen elements
        if(envelope.from != null && envelope.from.length > 0) {
            add(new RichTextField("From: " + envelope.from[0]));
            if(envelope.from.length > 1)
                for(int i=1;i<envelope.from.length;i++)
                    add(new RichTextField("      " + envelope.from[i]));
        }
        if(envelope.subject != null)
        add(new RichTextField("Subject: " + envelope.subject));
        add(new SeparatorField());
        bodyField = new RichTextField("");
        add(bodyField);
        getMessageBody();
    }

    protected boolean onSavePrompt() {
        return true;
    }

    private void getMessageBody() {
        Thread thread = new Thread() {
            public void run() {
                try {
                    if(!client.isConnected()) client.open();

                    client.setActiveMailbox(folderItem.path);
                    message = client.getMessage(envelope.index);
                } catch (IOException exp) {
                    message = new Message();
                    message.addBodyLine("IOException: " + exp);
                    try { client.close(); } catch (Exception exp2) { }
                } catch (MailException exp) {
                    message = new Message();
                    message.addBodyLine("Protocol error: " + exp);
                } catch (Exception exp) {
                    message = new Message();
                    message.addBodyLine("Unknown error: " + exp);
                    try { client.close(); } catch (Exception exp2) { }
                }

                // Update the UI
                synchronized(Application.getEventLock()) {
                    StringBuffer bodyBuffer = new StringBuffer();
                    for(int i=0;i<message.getBodyLineCount();i++)
                        bodyBuffer.append(message.getBodyLine(i) + "\n");
                    bodyField.setText(bodyBuffer.toString());
                    bodyField.setDirty(true);
                }
            }
        };
        thread.start();
    }

    private MenuItem propsItem = new MenuItem("Properties", 100, 10) {
        public void run() {
            // display a dialog with message properties
        }
    };
    private MenuItem closeItem = new MenuItem("Close", 200000, 10) {
        public void run() {
            onClose();
        }
    };
    protected void makeMenu(Menu menu, int instance) {
        menu.add(propsItem);
        menu.add(closeItem);
    }

}

