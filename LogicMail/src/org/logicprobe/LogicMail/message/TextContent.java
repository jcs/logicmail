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
import java.io.UnsupportedEncodingException;

import net.rim.device.api.io.Base64InputStream;

import org.logicprobe.LogicMail.util.StringFactory;
import org.logicprobe.LogicMail.util.StringParser;

/**
 * Represents message content of the text type
 */
public class TextContent extends MessageContent {
	private String text;
	
	public TextContent(TextPart textPart, String text) {
		super(textPart);
		this.text = text;
	}
	
    public TextContent(
    		TextPart textPart,
            String encoding,
            String charset,
            String data) throws UnsupportedContentException {
    	super(textPart);
    	String mimeSubtype = textPart.getMimeSubtype();
    	
        // Check for any encodings that need to be handled
        if (encoding.equalsIgnoreCase("quoted-printable")) {
            data = StringParser.decodeQuotedPrintable(data);
        }
        else if (encoding.equalsIgnoreCase("base64")) {
        	byte[] textBytes;

            try {
                textBytes = Base64InputStream.decode(data);
            } catch (IOException exp) {
                throw new UnsupportedContentException("Unable to decode");
            }

            try {
                // If a charset is not provided, ISO-8859-1 is assumed
                if (charset == null) {
                    charset = "ISO-8859-1";
                }

                data = StringFactory.create(textBytes, charset);
            } catch (UnsupportedEncodingException exp) {
                // If encoding type is bad, attempt with the default encoding
                // so the user will at least see something.
                data = new String(textBytes);
            }
        }
        else if ((charset != null) &&
                !charset.equalsIgnoreCase("ISO-8859-1") &&
                !charset.equalsIgnoreCase("US-ASCII")) {
            // If the text is not encoded (i.e. 7bit or 8bit) and uses a
            // non-Latin charset, then bring the text back to a byte array
            // and attempt to decode it based on the charset parameter.
            byte[] textBytes = data.getBytes();

            try {
                data = StringFactory.create(textBytes, charset);
            } catch (UnsupportedEncodingException exp) {
                // If encoding type is bad, leave the message text as
                // it was originally.  This may result in the user seeing
                // garbage, but at least they'll know there was a
                // decoding problem.
            }
        }

        // Check for a supported text sub-type and decode if necessary
        if (mimeSubtype.equalsIgnoreCase("plain") ||
                mimeSubtype.equalsIgnoreCase("html")) {
            this.text = data;
        }
        else {
        	throw new UnsupportedContentException("Unsupported subtype");
        }
    }
	
	/**
	 * Find out if a content object can be created for the provided
	 * MIME structure part.
	 * @param textPart MIME part to check.
	 * @return True if the content type is supported, false otherwise.
	 */
	public static boolean isPartSupported(TextPart textPart) {
		String mimeSubtype = textPart.getMimeSubtype();
        return (mimeSubtype.equalsIgnoreCase("plain") ||
                mimeSubtype.equalsIgnoreCase("html"));
	}
	
	public void accept(MessageContentVisitor visitor) {
		visitor.visit(this);
	}
	
	public String getText() {
		return this.text;
	}
}
