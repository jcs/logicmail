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
package org.logicprobe.LogicMail.message;

/**
 * Creates message parts, doing the necessary decoding.
 */
public class MessagePartFactory {
    /**
     * Create a new message part
     * @param mimeType MIME type
     * @param mimeSubtype MIME subtype
     * @param encoding Encoding type (i.e. 7bit, base64)
     * @param param Type-specific parameter (i.e. charset, filename)
     */
    public static MessagePart createMessagePart(
    		String mimeType,
    		String mimeSubtype,
    		String encoding,
    		String param) {
    	MessagePart part;
        if (mimeType.equalsIgnoreCase("multipart")) {
            part = new MultiPart(mimeSubtype);
        } else if (mimeType.equalsIgnoreCase("text")) {
        	part = new TextPart(mimeSubtype, encoding, param);
        } else if (mimeType.equalsIgnoreCase("image")) {
        	part = new ImagePart(mimeSubtype, encoding);
        } else {
            part = new UnsupportedPart(mimeType, mimeSubtype);
        }
        return part;
    }

    /**
     * Find out if a particular message part type is supported
     * without having to create it.  This is useful to optimize
     * downloads on protocols that support selective retrieval
     * of message parts.
     * Right now the supported subtypes are hard-coded, but they
     * should ultimately be programmatically determined upon
     * initialization of this factory.
     *
     * @param mimeType MIME type
     * @param mimeSubtype MIME subtype
     * @return True if supported, false if unsupported
     */
    public static boolean isMessagePartSupported(String mimeType,
        String mimeSubtype) {
        if (mimeType.equalsIgnoreCase("multipart")) {
            return isMultiPartSupported(mimeSubtype);
        } else if (mimeType.equalsIgnoreCase("text")) {
            return isTextPartSupported(mimeSubtype);
        } else if (mimeType.equalsIgnoreCase("image")) {
            return isImagePartSupported(mimeSubtype);
        } else {
            return false;
        }
    }

    private static boolean isMultiPartSupported(String mimeSubtype) {
        return (mimeSubtype.equalsIgnoreCase("mixed") ||
        mimeSubtype.equalsIgnoreCase("related") ||
        mimeSubtype.equalsIgnoreCase("alternative") ||
        mimeSubtype.equalsIgnoreCase("signed"));
    }

    private static boolean isTextPartSupported(String mimeSubtype) {
        // TODO: Add logic to only load plain or html, not both
        return (mimeSubtype.equalsIgnoreCase("plain") ||
        mimeSubtype.equalsIgnoreCase("html"));
    }

    private static boolean isImagePartSupported(String mimeSubtype) {
        return (mimeSubtype.equalsIgnoreCase("gif") ||
        mimeSubtype.equalsIgnoreCase("png") ||
        mimeSubtype.equalsIgnoreCase("vnd.wap.wbmp") ||
        mimeSubtype.equalsIgnoreCase("jpeg") ||
        mimeSubtype.equalsIgnoreCase("jpg") ||
        mimeSubtype.equalsIgnoreCase("pjpeg") ||
        mimeSubtype.equalsIgnoreCase("bmp") ||
        mimeSubtype.equalsIgnoreCase("tif"));
    }
}
