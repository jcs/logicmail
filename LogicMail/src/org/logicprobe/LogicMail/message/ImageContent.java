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

import java.io.IOException;

import net.rim.device.api.io.Base64InputStream;
import net.rim.device.api.system.EncodedImage;

/**
 * Represents message content of the image type
 */
public class ImageContent extends MimeMessageContent {
	private EncodedImage image;
	private byte[] rawData;
	
	public ImageContent(ImagePart imagePart, EncodedImage image) {
		super(imagePart);
		if(image == null) {
			throw new IllegalArgumentException();
		}
		this.image = image;
		this.rawData = null;
	}

	public ImageContent(ImagePart imagePart, String encoding, String data) throws UnsupportedContentException {
		super(imagePart);
        // Decode the binary data, and create an image
        if (encoding.equalsIgnoreCase("base64")) {
        	try {
	        	String mimeSubtype = imagePart.getMimeSubtype();
		        byte[] imgBytes = Base64InputStream.decode(data);
		        this.image = EncodedImage.createEncodedImage(
		        		imgBytes,
		                0, imgBytes.length, "image/" +
		                mimeSubtype.toLowerCase());
		        this.rawData = imgBytes;
        	} catch (IOException e) {
        		throw new UnsupportedContentException("Unable to decode");
        	}
        } else {
			throw new UnsupportedContentException("Unsupported encoding");
        }
	}
	
	/**
	 * Find out if a content object can be created for the provided
	 * MIME structure part.
	 * @param imagePart MIME part to check.
	 * @return True if the content type is supported, false otherwise.
	 */
	public static boolean isPartSupported(ImagePart imagePart) {
		String mimeSubtype = imagePart.getMimeSubtype();
        return (mimeSubtype.equalsIgnoreCase("gif") ||
                mimeSubtype.equalsIgnoreCase("png") ||
                mimeSubtype.equalsIgnoreCase("vnd.wap.wbmp") ||
                mimeSubtype.equalsIgnoreCase("jpeg") ||
                mimeSubtype.equalsIgnoreCase("jpg") ||
                mimeSubtype.equalsIgnoreCase("pjpeg") ||
                mimeSubtype.equalsIgnoreCase("bmp") ||
                mimeSubtype.equalsIgnoreCase("tiff"));
	}

	public void accept(MimeMessageContentVisitor visitor) {
		visitor.visit(this);
	}

	public EncodedImage getImage() {
        return image;
    }

	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.message.MessageContent#getRawData()
	 */
	public byte[] getRawData() {
		return rawData;
	}
}
