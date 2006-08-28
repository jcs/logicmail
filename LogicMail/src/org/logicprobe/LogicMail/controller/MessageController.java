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

package org.logicprobe.LogicMail.controller;

import java.io.IOException;
import net.rim.device.api.io.Base64InputStream;
import net.rim.device.api.system.EncodedImage;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.BitmapField;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.RichTextField;
import net.rim.device.api.util.Arrays;
import org.logicprobe.LogicMail.conf.MailSettings;
import org.logicprobe.LogicMail.mail.MailClient;
import org.logicprobe.LogicMail.mail.MailException;
import org.logicprobe.LogicMail.util.Observable;
import org.logicprobe.LogicMail.mail.Message;
import org.logicprobe.LogicMail.ui.MailClientHandler;
import org.logicprobe.LogicMail.ui.MessageScreen;

/**
 * Controller for message screens
 */
public class MessageController extends Controller implements Observable {
    private MailSettings _mailSettings;
    private Message.Structure _msgStructure;
    private MailClient _client;
    private MailClient.FolderItem _folderItem;
    private Message.Envelope _envelope;
    private Field[] _msgFields;
    
    /** Creates a new instance of MessageController */
    public MessageController(MailClient client,
                             MailClient.FolderItem folderItem,
                             Message.Envelope envelope) {
        _client = client;
        _folderItem = folderItem;
        _envelope = envelope;
        _msgStructure = null;
        _msgFields = null;
        _mailSettings = MailSettings.getInstance();
    }

    public void viewMessage() {
        UiApplication.getUiApplication().pushScreen(new MessageScreen(this));
    }
    
    public void updateMessageStructure() {
        _client = MailboxController.getInstance().getMailClient();
        MailClientHandler clientHandler = new MailClientHandler(_client, "Retrieving message") {
            public void runSession() throws IOException, MailException {
                _client.setActiveMailbox(_folderItem);

                // Get the structure of the message
                _msgStructure = _client.getMessageStructure(_envelope);

                notifyObservers("structure");
            }
        };
        clientHandler.start();        
    }
    
    public void updateMessageFields() {
        MailClientHandler clientHandler = new MailClientHandler(_client, "Retrieving message") {
            public void runSession() throws IOException, MailException {
                Field[] msgFields = new Field[0];
                String msgError = null;

                if(_msgStructure == null)
                    msgError = "Unable to parse structure";
                else {
                    boolean flag = false;
                    int i;
                    String mimeType;
                    String bodySection;
                    for(i=0;i<_msgStructure.sections.length;i++) {
                        mimeType = _msgStructure.sections[i].type + "/" +
                                   _msgStructure.sections[i].subtype;
                        if(_msgStructure.sections[i].size > _mailSettings.getGlobalConfig().getMaxSectionSize()) {
                            Arrays.add(msgFields, new RichTextField("Section too big: "+mimeType));
                        }
                        else if(mimeType.equals("text/plain")) {
                            flag = true;
                            bodySection = _client.getMessageBody(_envelope, i);
                            Arrays.add(msgFields, new RichTextField(bodySection));
                        }
                        else if(_msgStructure.sections[i].type.equals("image") &&
                                _msgStructure.sections[i].encoding.equals("base64")) {
                            try {
                                bodySection = _client.getMessageBody(_envelope, i);
                                byte[] imgBytes = Base64InputStream.decode(bodySection);
                                EncodedImage encImage =
                                        EncodedImage.createEncodedImage(imgBytes, 0, imgBytes.length, mimeType);
                                Arrays.add(msgFields, new BitmapField(encImage.getBitmap()));
                            } catch (Exception exp) {
                                Arrays.add(msgFields, new RichTextField("Unable to decode image: "+mimeType));
                            }
                        }
                        else {
                            Arrays.add(msgFields, new RichTextField("Unsupported type: "+mimeType));
                        }
                    }
                    if(!flag)
                        msgError = "Unable to find plain text body";
                }

                if(msgError != null)
                    Arrays.insertAt(msgFields, new RichTextField(msgError), 0);
                
                _msgFields = msgFields;
                notifyObservers("fields");
            }
        };
        clientHandler.start();
    }
    
    public void showMsgProperties() {
        int i;
        StringBuffer msg = new StringBuffer();
        msg.append("Subject:\n  " + _envelope.subject + "\n");
        msg.append("Date:\n  " + _envelope.date + "\n");

        msg.append("From:\n");
        for(i=0;i<_envelope.from.length;i++)
            msg.append("  " + _envelope.from[i] + "\n");

        msg.append("To:\n");
        for(i=0;i<_envelope.to.length;i++)
            if(_envelope.to[i].length() > 0)
                msg.append("  " + _envelope.to[i] + "\n");

        if(_envelope.cc != null && _envelope.cc.length > 0) {
            msg.append("CC:\n");
            for(i=0;i<_envelope.cc.length;i++)
                if(_envelope.cc[i].length() > 0)
                msg.append("  " + _envelope.cc[i] + "\n");
        }
        
        if(_envelope.bcc != null && _envelope.bcc.length > 0) {
            msg.append("BCC:\n");
            for(i=0;i<_envelope.bcc.length;i++)
                if(_envelope.bcc[i].length() > 0)
                    msg.append("  " + _envelope.bcc[i] + "\n");
        }

        Dialog dialog = new Dialog(Dialog.D_OK, msg.toString(),
                                   0, null, Dialog.GLOBAL_STATUS);
        dialog.show();
    }
    
    public Message.Envelope getMsgEnvelope() {
        return _envelope;
    }
    
    public Message.Structure getMsgStructure() {
        return _msgStructure;
    }

    public Field[] getMessageFields() {
        return _msgFields;
    }
}
