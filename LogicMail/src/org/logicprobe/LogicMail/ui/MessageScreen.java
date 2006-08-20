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
import net.rim.device.api.io.Base64InputStream;
import net.rim.device.api.system.EncodedImage;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.component.BitmapField;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.component.RichTextField;
import net.rim.device.api.ui.component.SeparatorField;
import org.logicprobe.LogicMail.mail.MailClient;
import org.logicprobe.LogicMail.mail.MailException;
import org.logicprobe.LogicMail.mail.Message;
import org.logicprobe.LogicMail.conf.MailSettings;

/**
 * Display an E-Mail message
 */
public class MessageScreen extends BaseScreen {
    private MailSettings _mailSettings;
    private MailClient _client;
    private MailClient.FolderItem folderItem;
    private Message.Envelope envelope;
    private Message.Structure msgStructure;
    
    static class MessageBody {
        String text;
    };
    
    public MessageScreen(MailClient client,
                         MailClient.FolderItem folderItem,
                         Message.Envelope envelope)
    {
        super();
        _mailSettings = MailSettings.getInstance();
        _client = client;
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
        getMessageBody();
    }

    protected boolean onSavePrompt() {
        return true;
    }

    private void getMessageBody() {
        MailClientHandler clientHandler = new MailClientHandler(_client, "Retrieving message") {
            public void runSession() throws IOException, MailException {
                Vector msgFields = new Vector();
                String msgError = null;
                _client.setActiveMailbox(folderItem);

                // Get the structure of the message
                msgStructure = _client.getMessageStructure(envelope.index);
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
                        if(msgStructure.sections[i].size > _mailSettings.getGlobalConfig().getMaxSectionSize()) {
                            msgFields.addElement(new RichTextField("Section too big: "+mimeType));
                        }
                        else if(mimeType.equals("text/plain")) {
                            flag = true;
                            bodySection = _client.getMessageBody(envelope.index, i);
                            msgFields.addElement(new RichTextField(bodySection));
                        }
                        else if(msgStructure.sections[i].type.equals("image") &&
                                msgStructure.sections[i].encoding.equals("base64")) {
                            try {
                                bodySection = _client.getMessageBody(envelope.index, i);
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
        clientHandler.start();
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

