/*-
 * Copyright (c) 2008, Derek Konigsberg
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
package org.logicprobe.LogicMail.util;

import net.rim.device.api.io.SharedInputStream;
import net.rim.device.api.mime.MIMEInputStream;
import net.rim.device.api.mime.MIMEParsingException;

import org.logicprobe.LogicMail.AppInfo;
import org.logicprobe.LogicMail.message.MimeMessageContentFactory;
import org.logicprobe.LogicMail.message.MessageEnvelope;
import org.logicprobe.LogicMail.message.MimeMessagePart;
import org.logicprobe.LogicMail.message.MimeMessagePartFactory;
import org.logicprobe.LogicMail.message.MultiPart;
import org.logicprobe.LogicMail.message.UnsupportedContentException;

import java.io.IOException;
import java.io.InputStream;

import java.util.Calendar;
import java.util.Hashtable;


/**
 * This class contains static parser functions used for
 * parsing raw message source text.
 */
public class MailMessageParser {
    private static String strCRLF = "\r\n";

    private MailMessageParser() {
    }

    /**
     * Parses the message envelope from the message headers.
     *
     * @param rawHeaders The raw header text, separated into lines.
     * @return The message envelope.
     */
    public static MessageEnvelope parseMessageEnvelope(String[] rawHeaders) {
        Hashtable headers = StringParser.parseMailHeaders(rawHeaders);
        MessageEnvelope env = new MessageEnvelope();

        // Populate the common header field bits of the envelope
        env.subject = StringParser.parseEncodedHeader((String) headers.get(
                    "subject"));

        if (env.subject == null) {
            env.subject = "";
        }

        env.from = parseAddressList((String) headers.get("from"));
        env.sender = parseAddressList((String) headers.get("sender"));
        env.to = parseAddressList((String) headers.get("to"));
        env.cc = parseAddressList((String) headers.get("cc"));
        env.bcc = parseAddressList((String) headers.get("bcc"));

        try {
            env.date = StringParser.parseDateString((String) headers.get("date"));
        } catch (Exception e) {
            env.date = Calendar.getInstance().getTime();
        }

        env.replyTo = parseAddressList((String) headers.get("reply-to"));
        env.messageId = (String) headers.get("message-id");
        env.inReplyTo = (String) headers.get("in-reply-to");

        return env;
    }

    /**
     * Generates the message headers corresponding to the provided envelope.
     *
     * @param envelope The message envelope.
     * @param includeUserAgent True to include the User-Agent line.
     * @return The headers, one per line, with CRLF line separators.
     */
    public static String generateMessageHeaders(MessageEnvelope envelope,
        boolean includeUserAgent) {
        StringBuffer buffer = new StringBuffer();

        // Create the message headers
        buffer.append(StringParser.createEncodedRecipientHeader("From:", envelope.from));
        buffer.append(strCRLF);

        buffer.append(StringParser.createEncodedRecipientHeader("To:", envelope.to));
        buffer.append(strCRLF);

        if ((envelope.cc != null) && (envelope.cc.length > 0)) {
            buffer.append(StringParser.createEncodedRecipientHeader("Cc:", envelope.cc));
            buffer.append(strCRLF);
        }

        if ((envelope.replyTo != null) && (envelope.replyTo.length > 0)) {
            buffer.append(StringParser.createEncodedRecipientHeader("Reply-To:", envelope.replyTo));
            buffer.append(strCRLF);
        }

        buffer.append("Date: ");
        buffer.append(StringParser.createDateString(envelope.date));
        buffer.append(strCRLF);

        if (includeUserAgent) {
            buffer.append("User-Agent: ");
            buffer.append(AppInfo.getName());
            buffer.append('/');
            buffer.append(AppInfo.getVersion());
            buffer.append(strCRLF);
        }

        buffer.append(StringParser.createEncodedHeader("Subject:", envelope.subject));
        buffer.append(strCRLF);

        if (envelope.inReplyTo != null) {
            buffer.append("In-Reply-To: ");
            buffer.append(envelope.inReplyTo);
            buffer.append(strCRLF);
        }

        return buffer.toString();
    }

    /**
     * Separates a list of addresses contained within a message header.
     * This is slightly more complicated than a string tokenizer, as it
     * has to deal with quoting and escaping.
     *
     * @param text The header line containing the addresses.
     * @return The separated addresses.
     */
    private static String[] parseAddressList(String text) {
        String[] addresses = StringParser.parseCsvString(text);

        for (int i = 0; i < addresses.length; i++) {
            addresses[i] = StringParser.parseEncodedHeader(addresses[i]);

            if ((addresses[i].length() > 0) && (addresses[i].charAt(0) == '"')) {
                int p = addresses[i].indexOf('<');

                while ((p > 0) && (addresses[i].charAt(p) != '"'))
                    p--;

                if ((p > 0) && ((p + 1) < addresses[i].length())) {
                    addresses[i] = addresses[i].substring(1, p) +
                        addresses[i].substring(p + 1);
                }
            }
        }

        return addresses;
    }

    /**
     * Parses the raw message body.
     *
     * @param contentMap Map to populate with MessagePart-to-MessageContent data.
     * @param inputStream The stream to read the raw message from
     * @return The root message part.
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static MimeMessagePart parseRawMessage(Hashtable contentMap, InputStream inputStream)
        throws IOException {
        MIMEInputStream mimeInputStream = null;

        try {
            mimeInputStream = new MIMEInputStream(inputStream);
        } catch (MIMEParsingException e) {
            return null;
        }

        MimeMessagePart rootPart = getMessagePart(contentMap, mimeInputStream);

        return rootPart;
    }

    /**
     * Recursively walk the provided MIMEInputStream, building a message
     * tree in the process.
     *
     * @param contentMap Map to populate with MessagePart-to-MessageContent data.
     * @param mimeInputStream MIMEInputStream of the downloaded message data
     * @return Root MessagePart element for this portion of the message tree
     */
    private static MimeMessagePart getMessagePart(Hashtable contentMap, MIMEInputStream mimeInputStream)
        throws IOException {
        // Parse out the MIME type and relevant header fields
        String mimeType = mimeInputStream.getContentType();
        String type = mimeType.substring(0, mimeType.indexOf('/'));
        String subtype = mimeType.substring(mimeType.indexOf('/') + 1);
        String encoding = mimeInputStream.getHeader("Content-Transfer-Encoding");
        String charset = mimeInputStream.getContentTypeParameter("charset");
        String name = mimeInputStream.getContentTypeParameter("name");
        String disposition = mimeInputStream.getHeader("Content-Disposition");
        String contentId = mimeInputStream.getHeader("Content-ID");

        // Default parameters used when headers are missing
        if (encoding == null) {
            encoding = "7bit";
        }

        // Clean up the disposition field
        if(disposition != null) {
	        int p = disposition.indexOf(';');
	        if(p != -1) {
	        	disposition = disposition.substring(0, p);
	        }
        	disposition = disposition.toLowerCase();
        }
        
        // Handle the multi-part case
        if (mimeInputStream.isMultiPart() &&
                type.equalsIgnoreCase("multipart")) {
            MimeMessagePart part = MimeMessagePartFactory.createMimeMessagePart(
            		type, subtype, null, null, null, null, null, -1);
            MIMEInputStream[] mimeSubparts = mimeInputStream.getParts();

            for (int i = 0; i < mimeSubparts.length; i++) {
                MimeMessagePart subPart = getMessagePart(contentMap, mimeSubparts[i]);

                if (subPart != null) {
                    ((MultiPart) part).addPart(subPart);
                }
            }

            return part;
        }
        // Handle the single-part case
        else {
            byte[] buffer;

            // Handle encoded binary data (should be more encoding-agnostic)
            if (encoding.equalsIgnoreCase("base64") &&
                    (mimeInputStream.isPartComplete() != 0)) {
                SharedInputStream sis = mimeInputStream.getRawMIMEInputStream();
                buffer = StringParser.readWholeStream(sis);

                int offset = 0;

                while (((offset + 3) < buffer.length) &&
                        !((buffer[offset] == '\r') &&
                        (buffer[offset + 1] == '\n') &&
                        (buffer[offset + 2] == '\r') &&
                        (buffer[offset + 3] == '\n'))) {
                    offset++;
                }

                int size = buffer.length - offset;

                String data = new String(buffer, offset, size);
                MimeMessagePart part = MimeMessagePartFactory.createMimeMessagePart(
                		type, subtype, name, encoding, charset, disposition, contentId, size);
                try {
					contentMap.put(part, MimeMessageContentFactory.createContent(part, data));
				} catch (UnsupportedContentException e) {
					System.err.println("UnsupportedContentException: " + e.getMessage());
				}
                return part;
            } else {
                buffer = StringParser.readWholeStream(mimeInputStream);

                String data = new String(buffer);
                MimeMessagePart part = MimeMessagePartFactory.createMimeMessagePart(
                		type, subtype, name, encoding, charset, disposition, contentId, data.length());
                try {
					contentMap.put(part, MimeMessageContentFactory.createContent(part, data));
				} catch (UnsupportedContentException e) {
					System.err.println("UnsupportedContentException: " + e.getMessage());
				}
                return part;
            }
        }
    }
}
