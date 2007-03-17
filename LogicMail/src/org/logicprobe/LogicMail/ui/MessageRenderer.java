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
import net.rim.device.api.ui.component.BitmapField;
import net.rim.device.api.ui.component.RichTextField;
import org.logicprobe.LogicMail.message.ImagePart;
import org.logicprobe.LogicMail.message.MessagePartVisitor;
import org.logicprobe.LogicMail.message.MultiPart;
import org.logicprobe.LogicMail.message.TextPart;
import org.logicprobe.LogicMail.message.UnsupportedPart;

/**
 * This class implements a visitor that generates UI elements to display
 * a message tree to the user.
 */
public class MessageRenderer implements MessagePartVisitor {
    private Vector messageFields;
    
    /** Creates a new instance of MessageRenderer */
    public MessageRenderer() {
        messageFields = new Vector();
    }

    public void visitMultiPart(MultiPart part) {
        // MultiPart parts are invisible to the user
    }

    public void visitTextPart(TextPart part) {
        messageFields.addElement(new RichTextField(part.getText()));
    }

    public void visitImagePart(ImagePart part) {
        messageFields.addElement(new BitmapField(part.getImage().getBitmap()));
    }

    public void visitUnsupportedPart(UnsupportedPart part) {
        messageFields.addElement(new RichTextField("Unsupported type: "+part.getMimeType()+"/"+part.getMimeSubtype()));
    }

    public Vector getMessageFields() {
        return messageFields;
    }
    
}
