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

import org.logicprobe.LogicMail.message.ApplicationPart;
import org.logicprobe.LogicMail.message.AudioPart;
import org.logicprobe.LogicMail.message.ImagePart;
import org.logicprobe.LogicMail.message.MessagePart;
import org.logicprobe.LogicMail.message.MimeMessagePart;
import org.logicprobe.LogicMail.message.MimeMessagePartVisitor;
import org.logicprobe.LogicMail.message.MultiPart;
import org.logicprobe.LogicMail.message.TextPart;
import org.logicprobe.LogicMail.message.UnsupportedPart;
import org.logicprobe.LogicMail.message.VideoPart;

import net.rim.device.api.system.Bitmap;

public class MessageIcons {
	private static MessageIcons instance = new MessageIcons();
	private MessagePartIconVisitor visitor = new MessagePartIconVisitor();

	private static final Bitmap mimeImageBitmap = Bitmap.getBitmapResource("mime_image.png");
	private static final Bitmap mimeTextBitmap = Bitmap.getBitmapResource("mime_text.png");
	private static final Bitmap mimeAudioBitmap = Bitmap.getBitmapResource("mime_audio.png");
	private static final Bitmap mimeVideoBitmap = Bitmap.getBitmapResource("mime_video.png");
	private static final Bitmap mimeApplicationBitmap = Bitmap.getBitmapResource("mime_application.png");
	private static final Bitmap mimeMessageBitmap = Bitmap.getBitmapResource("mime_message.png");
	
	private static String MIME_IMAGE = "image";
	private static String MIME_TEXT = "text";
	private static String MIME_AUDIO = "audio";
	private static String MIME_VIDEO = "video";
	private static String MIME_APPLICATION = "application";
	private static String MIME_MESSAGE = "message";
	
	private MessageIcons() {
		
	}
	
	public static Bitmap getIcon(MimeMessagePart mimeMessagePart) {
		return instance.getIconImpl(mimeMessagePart);
	}

	public static Bitmap getIcon(String mimeType) {
	    Bitmap icon;
	    if(mimeType.startsWith(MIME_IMAGE)) {
	        icon = mimeImageBitmap;
	    }
	    else if(mimeType.startsWith(MIME_TEXT)) {
            icon = mimeTextBitmap;
        }
        else if(mimeType.startsWith(MIME_AUDIO)) {
            icon = mimeAudioBitmap;
        }
        else if(mimeType.startsWith(MIME_VIDEO)) {
            icon = mimeVideoBitmap;
        }
        else if(mimeType.startsWith(MIME_APPLICATION)) {
            icon = mimeApplicationBitmap;
        }
        else if(mimeType.startsWith(MIME_MESSAGE)) {
            icon = mimeMessageBitmap;
        }
        else {
            icon = mimeApplicationBitmap;
        }
	    
	    return icon;
	}
	
	private Bitmap getIconImpl(MimeMessagePart mimeMessagePart) {
		visitor.clearIcon();
		mimeMessagePart.accept(visitor);
		return visitor.getIcon();
	}
	
	private class MessagePartIconVisitor implements MimeMessagePartVisitor {
		private Bitmap icon;

		public void visitApplicationPart(ApplicationPart part) {
			if(icon != null) { return; }
			icon = mimeApplicationBitmap;
		}

		public void visitAudioPart(AudioPart part) {
			if(icon != null) { return; }
			icon = mimeAudioBitmap;
		}

		public void visitImagePart(ImagePart part) {
			if(icon != null) { return; }
			icon = mimeImageBitmap;
		}

		public void visitTextPart(TextPart part) {
			if(icon != null) { return; }
			icon = mimeTextBitmap;
		}
		
		public void visitMessagePart(MessagePart part) {
			if(icon != null) { return; }
			icon = mimeMessageBitmap;
		}
		
		public void visitMultiPart(MultiPart part) {
			if(icon != null) { return; }
		}

		public void visitVideoPart(VideoPart part) {
			icon = mimeVideoBitmap;
		}

		public void visitUnsupportedPart(UnsupportedPart part) {
			// Use the application icon for unsupported types.
			// We can safely pass null since the parameter is
			// not currently used by any of the visitor methods.
			visitApplicationPart(null);
		}
		
		public void clearIcon() {
			this.icon = null;
		}
		
		public Bitmap getIcon() {
			return this.icon;
		}
	}
}
