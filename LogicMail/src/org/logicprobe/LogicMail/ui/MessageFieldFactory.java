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

import java.util.Hashtable;

import org.logicprobe.LogicMail.LogicMailResource;
import org.logicprobe.LogicMail.conf.MailSettings;
import org.logicprobe.LogicMail.message.ImageContent;
import org.logicprobe.LogicMail.message.MimeMessageContent;
import org.logicprobe.LogicMail.message.MimeMessagePart;
import org.logicprobe.LogicMail.message.TextContent;
import org.logicprobe.LogicMail.message.TextPart;
import org.logicprobe.LogicMail.model.MessageNode;
import org.logicprobe.LogicMail.util.UnicodeNormalizer;

import net.rim.device.api.i18n.ResourceBundle;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.component.ActiveRichTextField;
import net.rim.device.api.ui.component.BitmapField;
import net.rim.device.api.ui.component.RichTextField;

/**
 * Factory to create {@link Field} instances for display of
 * {@link MimeMessageContent} objects on the user interface.
 */
public class MessageFieldFactory {
	private static ResourceBundle resources = ResourceBundle.getBundle(LogicMailResource.BUNDLE_ID, LogicMailResource.BUNDLE_NAME);
	
	public static Field createMessageField(MessageNode messageNode, MimeMessageContent content) {
		Field field;
		if(content instanceof TextContent) {
			field = createTextMessageField(messageNode, (TextContent)content);
		}
		else if(content instanceof ImageContent) {
			field = createImageMessageField((ImageContent)content);
		}
		else {
			field = createUnsupportedMessageField(content);
		}
		return field;
	}

	private static Field createTextMessageField(MessageNode messageNode, TextContent content) {
		TextPart part = (TextPart)content.getMessagePart();
    	if(part.getMimeSubtype().equalsIgnoreCase("html")) {
    		return createBrowserField(messageNode, content);
    	}
    	else {
            BrowserFieldManager fieldManager = new BrowserFieldManager();
            fieldManager.add(new ActiveRichTextField(getNormalizedText(content)));
            return fieldManager;
    	}
	}

	/**
     * Run the Unicode normalizer on the provide content,
     * only if normalization is enabled in the configuration.
     * If normalization is disabled, this method returns
     * the input unmodified.
     * 
     * @param input Input content
     * @return Normalized string
     */
    private static String getNormalizedText(TextContent content) {
        if(MailSettings.getInstance().getGlobalConfig().getUnicodeNormalization()) {
        	String text = content.getText();
        	
            // If the encoding type is QP, then we need to do a charset
            // juggle to make sure that the data is using the right
            // characters instead of ISO-8859-1 characters
        	if(((TextPart)content.getMessagePart()).getEncoding().equalsIgnoreCase("quoted-printable")) {
                try{
                    byte[] encodedBytes = text.getBytes();
                    text = new String(encodedBytes, ((TextPart)content.getMessagePart()).getCharset());
                } catch(Exception e){}
        	}
        	
        	return UnicodeNormalizer.getInstance().normalize(text);
        }
        else {
            return content.getText();
        }
    }
	
	private static Field createImageMessageField(ImageContent content) {
		return new BitmapField(content.getImage().getBitmap());
	}

	private static Field createUnsupportedMessageField(MimeMessageContent content) {
		MimeMessagePart part = content.getMessagePart();
		return new RichTextField(
				resources.getString(LogicMailResource.MESSAGERENDERER_UNSUPPORTED)
				+ ' ' + part.getMimeType()
				+ '/' + part.getMimeSubtype());
	}

	private static Hashtable createdBrowserFields = new Hashtable();
	
	private static Field createBrowserField(MessageNode messageNode, TextContent content) {
		BrowserFieldRenderer fieldRenderer = new BrowserFieldRenderer(messageNode, content);

		Field field = fieldRenderer.getBrowserField();
		
		if(field != null) {
			createdBrowserFields.put(field, fieldRenderer);
		}
		
		return field;
	}

	public static void handleRenderedField(Field field) {
		BrowserFieldRenderer fieldRenderer = (BrowserFieldRenderer)createdBrowserFields.get(field);
		if(fieldRenderer != null) {
			fieldRenderer.finishLoading();
			createdBrowserFields.remove(field);
		}
	}
}
