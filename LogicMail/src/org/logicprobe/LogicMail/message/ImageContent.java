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

	public ImageContent(ImagePart imagePart, String encoding, byte[] data) throws UnsupportedContentException {
		super(imagePart);
        // Decode the binary data, and create an image
        if (encoding.equalsIgnoreCase(ENCODING_BASE64) && data != null && data.length > 0) {
        	try {
	        	byte[] imgBytes = decodeBase64(data);
	        	
	        	String mimeType = imagePart.getMimeType() + '/' + imagePart.getMimeSubtype().toLowerCase();
		        this.image = EncodedImage.createEncodedImage(
		        		imgBytes,
		                0, imgBytes.length, mimeType);
		        this.rawData = imgBytes;
        	} catch (IOException e) {
        		throw new UnsupportedContentException("Unable to decode");
        	}
        } else {
			throw new UnsupportedContentException("Unsupported encoding");
        }
	}
	
	public ImageContent(ImagePart imagePart, byte[] rawData, boolean decode) throws UnsupportedContentException {
        super(imagePart);
        imagePart.setEncoding(ENCODING_BASE64);
        this.rawData = rawData;
        
        if(decode) {
            try {
                String mimeType = imagePart.getMimeType() + '/' + imagePart.getMimeSubtype().toLowerCase();
                this.image = EncodedImage.createEncodedImage(
                        this.rawData, 0, this.rawData.length, mimeType);
            } catch (Exception e) {
                throw new UnsupportedContentException("Unable create image from data");
            }
        }
	}
	
	/**
	 * Instantiates a new image content object for deserialization.
	 */
	public ImageContent() {
		super(null);
	}
	
	/**
	 * Find out if a content object can be created for the provided
	 * MIME structure part.
	 * @param imagePart MIME part to check.
	 * @return True if the content type is supported, false otherwise.
	 */
	public static boolean isPartSupported(ImagePart imagePart) {
		String mimeSubtype = imagePart.getMimeSubtype();
        return (mimeSubtype.equalsIgnoreCase(ImagePart.SUBTYPE_GIF) ||
                mimeSubtype.equalsIgnoreCase(ImagePart.SUBTYPE_PNG) ||
                mimeSubtype.equalsIgnoreCase(ImagePart.SUBTYPE_VND_WAP_WBMP) ||
                mimeSubtype.equalsIgnoreCase(ImagePart.SUBTYPE_JPEG) ||
                mimeSubtype.equalsIgnoreCase(ImagePart.SUBTYPE_JPG) ||
                mimeSubtype.equalsIgnoreCase(ImagePart.SUBTYPE_PJPEG) ||
                mimeSubtype.equalsIgnoreCase(ImagePart.SUBTYPE_BMP) ||
                mimeSubtype.equalsIgnoreCase(ImagePart.SUBTYPE_TIFF));
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

	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.message.MimeMessageContent#putRawData(byte[])
	 */
	protected void putRawData(byte[] rawData) {
		this.rawData = rawData;
		ContentPart part = getMessagePart();
		String mimeType = part.getMimeType() + '/' + part.getMimeSubtype();
		this.image = EncodedImage.createEncodedImage(this.rawData, 0, this.rawData.length, mimeType);
	}
}
