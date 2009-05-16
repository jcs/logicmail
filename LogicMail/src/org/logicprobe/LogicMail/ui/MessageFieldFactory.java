/*-
 * Copyright (c) 2009, Derek Konigsberg
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

import org.logicprobe.LogicMail.AppInfo;
import org.logicprobe.LogicMail.LogicMailResource;
import org.logicprobe.LogicMail.message.ImageContent;
import org.logicprobe.LogicMail.message.MessageContent;
import org.logicprobe.LogicMail.message.MessagePart;
import org.logicprobe.LogicMail.message.TextContent;
import org.logicprobe.LogicMail.message.TextPart;

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

/**
 * Factory to create {@link Field} instances for display of
 * {@link MessageContent} objects on the user interface.
 */
public class MessageFieldFactory {
	private static ResourceBundle resources = ResourceBundle.getBundle(LogicMailResource.BUNDLE_ID, LogicMailResource.BUNDLE_NAME);

	public static Field createMessageField(MessageContent content) {
		Field field;
		if(content instanceof TextContent) {
			field = createTextMessageField((TextContent)content);
		}
		else if(content instanceof ImageContent) {
			field = createImageMessageField((ImageContent)content);
		}
		else {
			field = createUnsupportedMessageField(content);
		}
		return field;
	}

	private static Field createTextMessageField(TextContent content) {
		TextPart part = (TextPart)content.getMessagePart();
    	if(part.getMimeSubtype().equalsIgnoreCase("html")) {
    		ButtonField browserButtonField = new ButtonField("Open HTML in browser...", Field.FIELD_HCENTER);
    		browserButtonField.setChangeListener(new MessageFieldChangeListener(content) {
				public void fieldChanged(Field field, int context) {
					TextContent content = (TextContent)getContent();
					try {
						DataBuffer buffer = new DataBuffer();
						buffer.write(content.getText().getBytes());
						
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
    		return browserButtonFieldManager;
    	}
    	else {
    		return new RichTextField(content.getText());
    	}
	}

	private static abstract class MessageFieldChangeListener implements FieldChangeListener {
    	private TextContent content;
    	public MessageFieldChangeListener(TextContent content) {
    		this.content = content;
    	}
    	
    	protected TextContent getContent() { return content; }
    }
	
	private static Field createImageMessageField(ImageContent content) {
		return new BitmapField(content.getImage().getBitmap());
	}

	private static Field createUnsupportedMessageField(MessageContent content) {
		MessagePart part = content.getMessagePart();
		return new RichTextField(
				resources.getString(LogicMailResource.MESSAGERENDERER_UNSUPPORTED)
				+ ' ' + part.getMimeType()
				+ '/' + part.getMimeSubtype());
	}
}
