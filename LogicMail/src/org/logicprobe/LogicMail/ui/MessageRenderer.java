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

import java.io.ByteArrayOutputStream;
import java.util.Vector;

import net.rim.blackberry.api.browser.Browser;
import net.rim.blackberry.api.browser.BrowserSession;
import net.rim.device.api.i18n.ResourceBundle;
import net.rim.device.api.io.Base64OutputStream;
import net.rim.device.api.system.EventLogger;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.FieldChangeListener;
import net.rim.device.api.ui.component.BitmapField;
import net.rim.device.api.ui.component.ButtonField;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.component.RichTextField;
import net.rim.device.api.ui.container.VerticalFieldManager;
import net.rim.device.api.util.DataBuffer;

import org.logicprobe.LogicMail.AppInfo;
import org.logicprobe.LogicMail.LogicMailResource;
import org.logicprobe.LogicMail.message.ImagePart;
import org.logicprobe.LogicMail.message.MessagePart;
import org.logicprobe.LogicMail.message.MessagePartVisitor;
import org.logicprobe.LogicMail.message.MultiPart;
import org.logicprobe.LogicMail.message.TextPart;
import org.logicprobe.LogicMail.message.UnsupportedPart;

/**
 * This class implements a visitor that generates UI elements to display
 * a message tree to the user.
 */
public class MessageRenderer implements MessagePartVisitor {
	protected static ResourceBundle resources = ResourceBundle.getBundle(LogicMailResource.BUNDLE_ID, LogicMailResource.BUNDLE_NAME);
    private Vector messageFields;
    
    /** Creates a new instance of MessageRenderer */
    public MessageRenderer() {
        messageFields = new Vector();
    }

    public void visitMultiPart(MultiPart part) {
        // MultiPart parts are invisible to the user
    }

    public void visitTextPart(TextPart part) {
    	if(part.getMimeSubtype().equalsIgnoreCase("html")) {
    		ButtonField browserButtonField = new ButtonField("Open HTML in browser...", Field.FIELD_HCENTER);
    		browserButtonField.setChangeListener(new MessageFieldChangeListener(part) {
				public void fieldChanged(Field field, int context) {
					TextPart textPart = (TextPart)getPart();
					try {
						DataBuffer buffer = new DataBuffer();
						buffer.write(textPart.getText().getBytes());
						
						ByteArrayOutputStream output = new ByteArrayOutputStream();
						Base64OutputStream boutput = new Base64OutputStream(output);
						
						// Write out the special sequence which indicates to the browser
						// that it should treat this as HTML data in base64 format.
						output.write("data:text/html;base64,".getBytes());
						boutput.write(buffer.getArray());
						boutput.flush();
						boutput.close();
						output.flush();
						output.close();

						// Invoke the browser with the encoded HTML content.
						BrowserSession browserSession = Browser.getDefaultSession();
						browserSession.displayPage(output.toString());
					} catch (Throwable t) {
						EventLogger.logEvent(AppInfo.GUID, ("Error launching browser: " + t.toString()).getBytes(), EventLogger.ERROR);
						Dialog.alert("Unable to display the HTML message in the browser.");
					}
				}
    		});
    		
    		VerticalFieldManager browserButtonFieldManager = new VerticalFieldManager(Field.USE_ALL_WIDTH);
    		browserButtonFieldManager.add(new LabelField());
    		browserButtonFieldManager.add(browserButtonField);
    		browserButtonFieldManager.add(new LabelField());
    		messageFields.addElement(browserButtonFieldManager);
    	}
    	else {
    		messageFields.addElement(new RichTextField(part.getText()));
    	}
    }

    public void visitImagePart(ImagePart part) {
        messageFields.addElement(new BitmapField(part.getImage().getBitmap()));
    }

    public void visitUnsupportedPart(UnsupportedPart part) {
        messageFields.addElement(new RichTextField(resources.getString(LogicMailResource.MESSAGERENDERER_UNSUPPORTED) + ' ' + part.getMimeType() + '/' + part.getMimeSubtype()));
    }

    public Vector getMessageFields() {
        return messageFields;
    }

    private static abstract class MessageFieldChangeListener implements FieldChangeListener {
    	private MessagePart part;
    	public MessageFieldChangeListener(MessagePart part) {
    		this.part = part;
    	}
    	
    	protected MessagePart getPart() { return part; }
    }
}
