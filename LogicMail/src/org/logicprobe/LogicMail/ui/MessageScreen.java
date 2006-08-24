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

import net.rim.device.api.system.Application;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.component.RichTextField;
import net.rim.device.api.ui.component.SeparatorField;
import org.logicprobe.LogicMail.mail.Message;
import org.logicprobe.LogicMail.controller.MessageController;
import org.logicprobe.LogicMail.util.Observable;

/**
 * Display an E-Mail message
 */
public class MessageScreen extends BaseScreen {
    private MessageController _messageController;
    private Message.Envelope _envelope;
    private Field[] _msgFields;
    
    public MessageScreen(MessageController messageController)
    {
        super();
        _messageController = messageController;
        _messageController.addObserver(this);
        _envelope = _messageController.getMsgEnvelope();
        
        // Create screen elements
        if(_envelope.from != null && _envelope.from.length > 0) {
            add(new RichTextField("From: " + _envelope.from[0]));
            if(_envelope.from.length > 1)
                for(int i=1;i<_envelope.from.length;i++)
                    add(new RichTextField("      " + _envelope.from[i]));
        }
        if(_envelope.subject != null)
        add(new RichTextField("Subject: " + _envelope.subject));
        add(new SeparatorField());
        _messageController.updateMessageStructure();
    }

    public void update(Observable subject, Object arg) {
        super.update(subject, arg);
        if(subject.equals(_messageController)) {
            if(((String)arg).equals("structure")) {
                if(_messageController.getMsgStructure() != null)
                    _messageController.updateMessageFields();
            }
            else if(((String)arg).equals("fields")) {
                _msgFields = _messageController.getMessageFields();
                drawMessageFields();
            }
        }
    }

    private void drawMessageFields() {
        synchronized(Application.getEventLock()) {
            for(int i=0;i<_msgFields.length;i++) {
                if(_msgFields[i] != null) 
                    add(_msgFields[i]);
                if(i != _msgFields.length-1)
                    add(new SeparatorField());
            }
        }
    }

    private MenuItem propsItem = new MenuItem("Properties", 100, 10) {
        public void run() {
            _messageController.showMsgProperties();
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

