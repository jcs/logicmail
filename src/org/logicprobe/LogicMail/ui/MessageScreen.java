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

import java.util.Vector;
import net.rim.device.api.system.Application;
import net.rim.device.api.system.KeypadListener;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Keypad;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.component.RichTextField;
import net.rim.device.api.ui.component.SeparatorField;
import org.logicprobe.LogicMail.controller.MessageController;
import org.logicprobe.LogicMail.message.MessageEnvelope;
import org.logicprobe.LogicMail.util.Observable;

/**
 * Display an E-Mail message
 */
public class MessageScreen extends BaseScreen {
    private MessageController messageController;
    private MessageEnvelope envelope;
    private Vector msgFields;
    
    public MessageScreen(MessageController messageController)
    {
        super();
        this.messageController = messageController;
        this.messageController.addObserver(this);
        envelope = this.messageController.getMsgEnvelope();
        
        // Create screen elements
        if(envelope.from != null && envelope.from.length > 0) {
            add(new RichTextField("From: " + envelope.from[0]));
            if(envelope.from.length > 1)
                for(int i=1;i<envelope.from.length;i++)
                    if(envelope.from[i] != null)
                        add(new RichTextField("      " + envelope.from[i]));
        }
        if(envelope.subject != null)
            add(new RichTextField("Subject: " + envelope.subject));
        add(new SeparatorField());
        this.messageController.updateMessage();
    }

    public void update(Observable subject, Object arg) {
        super.update(subject, arg);
        if(subject.equals(this.messageController)) {
            if(((String)arg).equals("message")) {
                msgFields = this.messageController.getMessageFields();
                drawMessageFields();
            }
        }
    }

    private void drawMessageFields() {
        synchronized(Application.getEventLock()) {
            int size = msgFields.size();
            for(int i=0;i<size;++i) {
                if(msgFields.elementAt(i) != null) 
                    add((Field)msgFields.elementAt(i));
                if(i != size-1)
                    add(new SeparatorField());
            }
        }
    }

    private MenuItem propsItem = new MenuItem("Properties", 100, 10) {
        public void run() {
            org.logicprobe.LogicMail.ui.MessageScreen.this.messageController.showMsgProperties();
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

}

