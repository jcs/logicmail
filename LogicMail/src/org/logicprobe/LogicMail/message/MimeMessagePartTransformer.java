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
package org.logicprobe.LogicMail.message;

import java.util.Vector;

import org.logicprobe.LogicMail.conf.GlobalConfig;
import org.logicprobe.LogicMail.conf.MailSettings;

/**
 * Utility class to take a {@link MimeMessagePart} tree and transform
 * it into a list based on various rules.
 */
public class MimeMessagePartTransformer {
	/**
	 * Gets a list of displayable message parts in order.
	 * 
	 * @param rootPart The root part of the message.
	 * @return The displayable parts.
	 */
	public static MimeMessagePart[] getDisplayableParts(MimeMessagePart rootPart) {
		DisplayablePartVisitor visitor = new DisplayablePartVisitor();
		rootPart.accept(visitor);
		Vector parts = visitor.getDisplayableParts();
		MimeMessagePart[] result = new MimeMessagePart[parts.size()];
		parts.copyInto(result);
		return result;
	}
	
	private static class DisplayablePartVisitor extends AbstractMimeMessagePartVisitor {
		private Vector displayableParts = new Vector();
		int displayFormat;
		
		public DisplayablePartVisitor() {
			displayFormat = MailSettings.getInstance().getGlobalConfig().getMessageDisplayFormat();
		}
		
		public Vector getDisplayableParts() { return displayableParts; }
		
		public void visitTextPart(TextPart part) {
			if(!MimeMessageContentFactory.isContentSupported(part)) { return; }
			
			if(part.getParent() instanceof MultiPart) {
				MultiPart parent = (MultiPart)part.getParent();
				
				if(parent.getMimeSubtype().equalsIgnoreCase(MultiPart.ALTERNATIVE)) {
					String subType = part.getMimeSubtype();
					
					if(displayFormat == GlobalConfig.MESSAGE_DISPLAY_PLAIN_TEXT) {
						if(subType.equalsIgnoreCase("plain")) {
							displayableParts.addElement(part);
						}
						else {
							MimeMessagePart[] siblings = parent.getParts();
							boolean otherPlain = false;
							for(int i=0; i<siblings.length; i++) {
								if(siblings[i] != part
									&& siblings[i] instanceof TextPart
									&& siblings[i].getMimeSubtype().equalsIgnoreCase("plain")) {
									otherPlain = true;
									break;
								}
							}
							if(!otherPlain) {
								displayableParts.addElement(part);
							}
						}
					}
					else if(displayFormat == GlobalConfig.MESSAGE_DISPLAY_HTML) {
						if(subType.equalsIgnoreCase("html")) {
							displayableParts.addElement(part);
						}
						else {
							MimeMessagePart[] siblings = parent.getParts();
							boolean otherHtml = false;
							for(int i=0; i<siblings.length; i++) {
								if(siblings[i] != part
									&& siblings[i] instanceof TextPart
									&& siblings[i].getMimeSubtype().equalsIgnoreCase("html")) {
									otherHtml = true;
									break;
								}
							}
							if(!otherHtml) {
								displayableParts.addElement(part);
							}
						}
					}
				}
				else {
					displayableParts.addElement(part);
				}
			}
			else {
				displayableParts.addElement(part);
			}
		}

		public void visitImagePart(ImagePart part) {
			if(MimeMessageContentFactory.isContentSupported(part)) {
				displayableParts.addElement(part);
			}
		}
	}
	
	/**
	 * Gets a list of message parts that are considered to be attachments.
	 * <p>
	 * This includes all parts that are <b>not</b> of one of the following types:
	 * <ul>
	 * <li>multipart</li>
	 * <li>text/plain</li>
	 * <li>text/html</li>
	 * <li>unsupported</li>
	 * </ul>
	 * </p>
	 * 
	 * @param rootPart The root part of the message.
	 * @return The displayable parts.
	 */
	public static MimeMessagePart[] getAttachmentParts(MimeMessagePart rootPart) {
		AttachmentPartVisitor visitor = new AttachmentPartVisitor();
		rootPart.accept(visitor);
		Vector parts = visitor.getAttachmentParts();
		MimeMessagePart[] result = new MimeMessagePart[parts.size()];
		parts.copyInto(result);
		return result;
	}

	private static class AttachmentPartVisitor extends AbstractMimeMessagePartVisitor {
		private Vector attachmentParts = new Vector();
		
		public Vector getAttachmentParts() { return attachmentParts; }
		
		public void visitTextPart(TextPart part) {
			String subtype = part.getMimeSubtype();
			if(!subtype.equalsIgnoreCase("plain") && !subtype.equalsIgnoreCase("html")) {
				attachmentParts.addElement(part);
			}
		}

		public void visitImagePart(ImagePart part) {
			attachmentParts.addElement(part);
		}
		
		public void visitApplicationPart(ApplicationPart part) {
			attachmentParts.addElement(part);
		}
		
		public void visitAudioPart(AudioPart part) {
			attachmentParts.addElement(part);
		}
		
		public void visitVideoPart(VideoPart part) {
			attachmentParts.addElement(part);
		}
	}
}
