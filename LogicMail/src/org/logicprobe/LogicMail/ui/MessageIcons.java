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

import org.logicprobe.LogicMail.message.ImagePart;
import org.logicprobe.LogicMail.message.MessagePart;
import org.logicprobe.LogicMail.message.MessagePartVisitor;
import org.logicprobe.LogicMail.message.MultiPart;
import org.logicprobe.LogicMail.message.TextPart;
import org.logicprobe.LogicMail.message.UnsupportedPart;

import net.rim.device.api.system.Bitmap;

public class MessageIcons {
	private static MessageIcons instance = new MessageIcons();
	private MessagePartIconVisitor visitor = new MessagePartIconVisitor();

	private Bitmap mimeImageBitmap;
	private Bitmap mimeTextBitmap;
	private Bitmap mimeAudioBitmap;
	private Bitmap mimeVideoBitmap;
	private Bitmap mimeApplicationBitmap;
	
	
	private MessageIcons() {
		
	}
	
	public static Bitmap getIcon(MessagePart messagePart) {
		return instance.getIconImpl(messagePart);
	}

	private Bitmap getIconImpl(MessagePart messagePart) {
		visitor.clearIcon();
		messagePart.accept(visitor);
		return visitor.getIcon();
	}
	
	private class MessagePartIconVisitor implements MessagePartVisitor {
		private Bitmap icon;

		public void visitImagePart(ImagePart part) {
			if(icon != null) { return; }
			if(mimeImageBitmap == null) {
				mimeImageBitmap = Bitmap.getBitmapResource("mime_image.png");
			}
			icon = mimeImageBitmap;
		}

		public void visitTextPart(TextPart part) {
			if(icon != null) { return; }
			if(mimeTextBitmap == null) {
				mimeTextBitmap = Bitmap.getBitmapResource("mime_text.png");
			}
			icon = mimeTextBitmap;
		}
		
		public void visitMultiPart(MultiPart part) {
			if(icon != null) { return; }
		}
		
		public void visitUnsupportedPart(UnsupportedPart part) {
			if(icon != null) { return; }
			String type = part.getMimeType();
			if(type.equals("audio")) {
				if(mimeAudioBitmap == null) {
					mimeAudioBitmap = Bitmap.getBitmapResource("mime_audio.png");
				}
				icon = mimeAudioBitmap;
			}
			else if(type.equals("video")) {
				if(mimeVideoBitmap == null) {
					mimeVideoBitmap = Bitmap.getBitmapResource("mime_video.png");
				}
				icon = mimeVideoBitmap;
			}
			else {
				if(mimeApplicationBitmap == null) {
					mimeApplicationBitmap = Bitmap.getBitmapResource("mime_application.png");
				}
				icon = mimeApplicationBitmap;
			}
		}
		
		public void clearIcon() {
			this.icon = null;
		}
		
		public Bitmap getIcon() {
			return this.icon;
		}
	}
}