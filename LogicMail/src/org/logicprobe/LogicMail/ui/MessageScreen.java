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
import net.rim.device.api.io.Base64InputStream;
import net.rim.device.api.system.EncodedImage;
import org.logicprobe.LogicMail.mail.MailClient;
import org.logicprobe.LogicMail.mail.MailException;
import org.logicprobe.LogicMail.mail.Message;
import java.io.IOException;
import java.util.Vector;

/**
 * Display an E-Mail message
 */
public class MessageScreen extends MainScreen {
    private MailClient client;
    private MailClient.FolderItem folderItem;
    private Message.Envelope envelope;
    private Message.Structure msgStructure;
    
    public static int maxSectionSize = 16384;

    static class MessageBody {
        String text;
    };
    
    public MessageScreen(MailClient client,
                         MailClient.FolderItem folderItem,
                         Message.Envelope envelope)
    {
        super();
        this.client = client;
        this.folderItem = folderItem;
        this.envelope = envelope;
        
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
        //bodyField = new RichTextField("");
        //add(bodyField);
        getMessageBody();
    }

    protected boolean onSavePrompt() {
        return true;
    }

    private void getMessageBody() {
        Thread thread = new Thread() {
            public void run() {
                Vector msgFields = new Vector();
                String msgError = null;
                try {
                    if(!client.isConnected()) client.open();
                    client.setActiveMailbox(folderItem);

                    // Get the structure of the message
                    msgStructure = client.getMessageStructure(envelope.index);
                    if(msgStructure == null)
                        msgError = "Unable to parse structure";
                    else {
                        boolean flag = false;
                        int i;
                        String mimeType;
                        String bodySection;
                        for(i=0;i<msgStructure.sections.length;i++) {
                            mimeType = msgStructure.sections[i].type + "/" +
                                       msgStructure.sections[i].subtype;
                            if(msgStructure.sections[i].size > maxSectionSize) {
                                msgFields.addElement(new RichTextField("Section too big: "+mimeType));
                            }
                            else if(mimeType.equals("text/plain")) {
                                flag = true;
                                bodySection = client.getMessageBody(envelope.index, i);
                                msgFields.addElement(new RichTextField(bodySection));
                            }
                            else if(msgStructure.sections[i].type.equals("image") &&
                                    msgStructure.sections[i].encoding.equals("base64")) {
                                try {
                                    bodySection = client.getMessageBody(envelope.index, i);
                                    byte[] imgBytes = Base64InputStream.decode(bodySection);
                                    EncodedImage encImage =
                                            EncodedImage.createEncodedImage(imgBytes, 0, imgBytes.length, mimeType);
                                    msgFields.addElement(new BitmapField(encImage.getBitmap()));
                                } catch (Exception exp) {
                                    msgFields.addElement(new RichTextField("Unable to decode image: "+mimeType));
                                }
                            }
                            else {
                                msgFields.addElement(new RichTextField("Unsupported type: "+mimeType));
                            }
                        }
                        if(!flag)
                            msgError = "Unable to find plain text body";
                    }
                } catch (IOException exp) {
                    msgError = "IOException: " + exp;
                    try { client.close(); } catch (Exception exp2) { }
                } catch (MailException exp) {
                    msgError = "Protocol error: " + exp;
                } catch (Exception exp) {
                    msgError = "Unknown error: " + exp;
                    try { client.close(); } catch (Exception exp2) { }
                }
                if(msgError != null)
                    msgFields.insertElementAt(new RichTextField(msgError), 0);
                
                // Update the UI
                synchronized(Application.getEventLock()) {
                    for(int i=0;i<msgFields.size();i++) {
                        add((Field)msgFields.elementAt(i));
                        if(i != msgFields.size()-1)
                            add(new SeparatorField());
                    }
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

