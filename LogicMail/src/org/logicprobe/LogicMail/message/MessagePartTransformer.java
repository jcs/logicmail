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

/**
 * Utility class to take a {@link MessagePart} tree and transform
 * it into a list based on various rules.
 */
public class MessagePartTransformer {
	/**
	 * Gets a list of displayable message parts in order.
	 * 
	 * @param rootPart The root part of the message.
	 * @return The displayable parts.
	 */
	public static MessagePart[] getDisplayableParts(MessagePart rootPart) {
		DisplayablePartVisitor visitor = new DisplayablePartVisitor();
		rootPart.accept(visitor);
		Vector parts = visitor.getDisplayableParts();
		MessagePart[] result = new MessagePart[parts.size()];
		parts.copyInto(result);
		return result;
	}
	
	private static class DisplayablePartVisitor implements MessagePartVisitor {
		private Vector displayableParts = new Vector();
		
		public Vector getDisplayableParts() { return displayableParts; }
		
		public void visitTextPart(TextPart part) {
			// TODO: Add logic to deal with multipart/alternative cases
			displayableParts.addElement(part);
		}

		public void visitImagePart(ImagePart part) {
			displayableParts.addElement(part);
		}

		public void visitUnsupportedPart(UnsupportedPart part) {
			// No need to display unknown things
		}
		
		public void visitMultiPart(MultiPart part) {
			// MultiPart sections are not displayed
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
	public static MessagePart[] getAttachmentParts(MessagePart rootPart) {
		AttachmentPartVisitor visitor = new AttachmentPartVisitor();
		rootPart.accept(visitor);
		Vector parts = visitor.getAttachmentParts();
		MessagePart[] result = new MessagePart[parts.size()];
		parts.copyInto(result);
		return result;
	}

	private static class AttachmentPartVisitor implements MessagePartVisitor {
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

		public void visitUnsupportedPart(UnsupportedPart part) {
			// Not an attachment part
		}
		
		public void visitMultiPart(MultiPart part) {
			// Not an attachment part
		}
	}
}
