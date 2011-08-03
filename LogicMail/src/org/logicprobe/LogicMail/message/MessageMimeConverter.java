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

import net.rim.device.api.io.Base64OutputStream;
import net.rim.device.api.mime.MIMEOutputStream;
import net.rim.device.api.system.EncodedImage;
import net.rim.device.api.system.EventLogger;

import org.logicprobe.LogicMail.AnalyticsDataCollector;
import org.logicprobe.LogicMail.AppInfo;
import org.logicprobe.LogicMail.util.StringParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.util.Hashtable;

/**
 * Converts a message into the equivalent MIME structure
 */
public class MessageMimeConverter {
	private Message message;
    private ByteArrayOutputStream byteArrayOutputStream;
    private MIMEOutputStream mimeOutputStream;

    /** maps message parts to MIMEOutputStream objects */
    private Hashtable partMimeMap;

    private MessageMimeConverterPartVisitor partVisitor;
    
    private boolean processed;
    
    /** Creates a new instance of MessageMimeConverter */
    public MessageMimeConverter(Message message) {
    	this.message = message;
    	this.byteArrayOutputStream = new ByteArrayOutputStream();
    	this.mimeOutputStream = null;
    	this.partMimeMap = new Hashtable();
    	this.partVisitor = new MessageMimeConverterPartVisitor();
    }

    /**
     * Get the contents of this converter.
     *
     * @return Message encoded in MIME format
     */
    public String toMimeString() {
        processMessage();
        return byteArrayOutputStream.toString();
    }

    /**
     * Get the contents of this converter.
     *
     * @return Message encoded in MIME format
     */
    public byte[] toMimeByteArray() {
        processMessage();
        return byteArrayOutputStream.toByteArray();
    }

    private void processMessage() {
        if(!processed) {
            message.getStructure().accept(partVisitor);
            partMimeMap.clear();
            processed = true;
        }
    }
    
    private class MessageMimeConverterPartVisitor extends AbstractMimeMessagePartVisitor {
	    public void visitMultiPart(MultiPart part) {
	        MIMEOutputStream currentStream = startCurrentStream(part, null);
	        currentStream.setContentType(part.getMimeType() + '/' + part.getMimeSubtype());
	        
	        // Call the superclass implementation which will iterate through
	        // all the child parts and cause their handlers to be called.
	        super.visitMultiPart(part);

	        finishCurrentStream(currentStream);
	    }
	
	    public void visitTextPart(TextPart part) {
	        MimeMessageContent content = message.getContent(part);
	        if(!(content instanceof TextContent)) { return; }

	        if(((TextContent)content).getText() != null) {
	            addTextPartFromString(part, (TextContent)content);
	        }
	        else {
                addTextPartFromRawData(part, (TextContent)content);
	        }
	    }

	    private void addTextPartFromString(TextPart part, TextContent content) {
            String charset;
            boolean isBinary;
            boolean isQP;
            String encoding;
            String text = content.getText();
            
            // Determine the charset and encoding from the characteristics of the
            // text, instead of the properties of the TextPart.  We do it this way
            // because the TextPart already contains decoded text that could be
            // encoded in a variety of ways, and because there is no way to
            // easily create a TextPart from a local decoded String that has the
            // charset and encoding properties set correctly.
            switch(StringParser.getOptimalEncoding(text)) {
            case StringParser.ENCODING_QUOTED_PRINTABLE:
                charset = "ISO-8859-1";
                isBinary = false;
                isQP = true;
                encoding = "quoted-printable";
                break;
            case StringParser.ENCODING_BASE64:
                charset = "UTF-8";
                isBinary = true;
                isQP = false;
                encoding = "base64";
                break;
            case StringParser.ENCODING_7BIT:
            default:
                charset = "US-ASCII";
                isBinary = false;
                isQP = false;
                encoding = "7bit";
                break;
            }
    
            MIMEOutputStream currentStream = startCurrentStream(part, encoding);
    
            currentStream.setContentType(part.getMimeType() + '/' +
                part.getMimeSubtype());
            currentStream.addContentTypeParameter("charset", charset.toLowerCase());
    
            // Add the content, encoding as necessary
            try {
                if (!isBinary) {
                    if (isQP) {
                        currentStream.write(StringParser.encodeQuotedPrintable(text)
                                                        .getBytes(charset));
                    } else {
                        currentStream.write(text.getBytes(charset));
                    }
                } else {
                    byte[] data = text.getBytes(charset);
                    currentStream.write(Base64OutputStream.encode(data, 0, data.length, true, true));
                }
            } catch (IOException e) {
                EventLogger.logEvent(AppInfo.GUID,
                        ("MIME conversion error: " + e.toString()).getBytes(),
                        EventLogger.ERROR);
                AnalyticsDataCollector.getInstance().onApplicationError("MIME conversion error: " + e.toString());
            }
            
            finishCurrentStream(currentStream);
        }

        private void addTextPartFromRawData(TextPart part, TextContent content) {
            String encoding;
            boolean isBinary;
            byte[] rawData = content.getRawData();
            if(rawData == null) { return; }

            switch(StringParser.getOptimalEncoding(rawData)) {
            case StringParser.ENCODING_BASE64:
                isBinary = true;
                encoding = "base64";
                break;
            case StringParser.ENCODING_7BIT:
            default:
                isBinary = false;
                encoding = "7bit";
                break;
            }
            
            MIMEOutputStream currentStream = startCurrentStream(part, encoding);
            
            currentStream.setContentType(part.getMimeType() + '/' +
                part.getMimeSubtype());
            
            // Attempt to add a charset parameter based on the file's
            // byte-order-mark and other information as described at:
            // http://codesnipers.com/?q=node/68
            if(isBinary) {
                String charset = null;
                if(rawData.length > 2) {
                    if((rawData[0] == (byte)0xFF && rawData[1] == (byte)0xFE)
                            || (rawData[0] == (byte)0x3C && rawData[1] == (byte)0x00)) {
                        charset = "utf-16le";
                    }
                    else if((rawData[0] == (byte)0xFE && rawData[1] == (byte)0xFF)
                            || (rawData[0] == (byte)0x00 && rawData[1] == (byte)0x3C)) {
                        charset = "utf-16be";
                    }
                    else if((rawData[0] == (byte)0xEF && rawData[1] == (byte)0xBB && rawData[2] == (byte)0xBF)
                            || (rawData[0] == (byte)0x3C && rawData[1] > (byte)0x00)) {
                        charset = "utf-8";
                    }
                }
                
                if(charset != null) {
                    currentStream.addContentTypeParameter("charset", charset);
                }
            }

            try {
                if(!isBinary) {
                    currentStream.write(rawData);
                }
                else {
                    currentStream.write(Base64OutputStream.encode(rawData, 0, rawData.length, true, true));
                }
            } catch (IOException e) {
                EventLogger.logEvent(AppInfo.GUID,
                        ("MIME conversion error: " + e.toString()).getBytes(),
                        EventLogger.ERROR);
                AnalyticsDataCollector.getInstance().onApplicationError("MIME conversion error: " + e.toString());
            }
            
            finishCurrentStream(currentStream);
        }

        public void visitImagePart(ImagePart part) {
	        MimeMessageContent content = message.getContent(part);
	        if(!(content instanceof ImageContent)) { return; }
	        
	        byte[] data;
	        EncodedImage image = ((ImageContent)content).getImage();
	        if(image != null) {
	            data = image.getData();
	        }
	        else {
	            data = content.getRawData();
	        }
	        
	        MIMEOutputStream currentStream = startCurrentStream(part, "base64");
	        
	        currentStream.setContentType(part.getMimeType() + '/' +
	            part.getMimeSubtype());
	
	        try {
	            currentStream.write(Base64OutputStream.encode(data, 0, data.length, true, true));
	        } catch (IOException e) {
                EventLogger.logEvent(AppInfo.GUID,
                        ("MIME conversion error: " + e.toString()).getBytes(),
                        EventLogger.ERROR);
                AnalyticsDataCollector.getInstance().onApplicationError("MIME conversion error: " + e.toString());
	        }
	        
	        finishCurrentStream(currentStream);
	    }
	
	    public void visitApplicationPart(ApplicationPart part) {
	        encodePartToCurrentStream(part);
	    }
	    
	    public void visitAudioPart(AudioPart part) {
            encodePartToCurrentStream(part);
	    }
	    
	    public void visitVideoPart(VideoPart part) {
            encodePartToCurrentStream(part);
	    }

        private void encodePartToCurrentStream(ContentPart part) {
            MimeMessageContent content = message.getContent(part);

            byte[] data = content.getRawData();

            MIMEOutputStream currentStream = startCurrentStream(part, "base64");

            currentStream.setContentType(part.getMimeType() + '/' +
                    part.getMimeSubtype());

            try {
                currentStream.write(Base64OutputStream.encode(data, 0, data.length, true, true));
            } catch (IOException e) {
                EventLogger.logEvent(AppInfo.GUID,
                        ("MIME conversion error: " + e.toString()).getBytes(),
                        EventLogger.ERROR);
                AnalyticsDataCollector.getInstance().onApplicationError("MIME conversion error: " + e.toString());
            }

            finishCurrentStream(currentStream);
        }

	    public void visitUnsupportedPart(UnsupportedPart part) {
	        MIMEOutputStream currentStream = startCurrentStream(part, "7bit");
	
	        currentStream.setContentType("text/plain");
	
	        try {
	            currentStream.write("Unable to encode part".getBytes());
	        } catch (IOException e) {
                EventLogger.logEvent(AppInfo.GUID,
                        ("MIME conversion error: " + e.toString()).getBytes(),
                        EventLogger.ERROR);
                AnalyticsDataCollector.getInstance().onApplicationError("MIME conversion error: " + e.toString());
	        }
	        
	        finishCurrentStream(currentStream);
	    }
	    
        private MIMEOutputStream startCurrentStream(MimeMessagePart part, String encoding) {
            MIMEOutputStream currentStream;
            boolean isMultiPart = (part instanceof MultiPart);
            if (mimeOutputStream == null) {
                mimeOutputStream = new MIMEOutputStream(
                        byteArrayOutputStream,
                        isMultiPart,
                        encoding);
                
                if(!isMultiPart) {
                    addPartHeaders(mimeOutputStream, part);
                }
                currentStream = mimeOutputStream;
            } else {
                MIMEOutputStream parentStream = (MIMEOutputStream) partMimeMap.get(part.getParent());
                currentStream = parentStream.getPartOutputStream(isMultiPart, encoding);
                addPartHeaders(currentStream, part);
            }
            partMimeMap.put(part, currentStream);
            return currentStream;
        }
        
        private void addPartHeaders(MIMEOutputStream mimeStream, MimeMessagePart part) {
            if(part instanceof ContentPart) {
                ContentPart contentPart = (ContentPart)part;
                String name = contentPart.getName();
                if(name.length() > 0) {
                    mimeStream.addContentTypeParameter("name", "\"" + StringParser.createEncodedHeader(10, name) + "\"");
                    if(contentPart.getDisposition().equalsIgnoreCase("attachment")) {
                        mimeStream.addHeaderField("Content-Disposition: attachment; filename=\""
                                + StringParser.createEncodedHeader(44, name) + "\"");
                    }
                }
            }
        }
        
        private void finishCurrentStream(MIMEOutputStream currentStream) {
            try {
                currentStream.flush();
                currentStream.close();
            } catch (IOException e) {
                EventLogger.logEvent(AppInfo.GUID,
                        ("MIME conversion error: " + e.toString()).getBytes(),
                        EventLogger.ERROR);
                AnalyticsDataCollector.getInstance().onApplicationError("MIME conversion error: " + e.toString());
            }
        }
    }
}
