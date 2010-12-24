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

/**
 * Creates message content objects.
 */
public class MimeMessageContentFactory {
    /**
	 * Creates a new MessageContent object.
	 * 
	 * @param mimeMessagePart The message part describing the MIME properties for the content.
	 * @param encodedData The encoded data from which to decode the content.
	 * 
	 * @return The message content object.
	 * 
	 * @throws UnsupportedContentException Thrown if the content type was not supported or the data could not be decoded.
	 */
	public static MimeMessageContent createContentEncoded(MimeMessagePart mimeMessagePart, byte[] encodedData) throws UnsupportedContentException {
    	MimeMessageContent content;
    	if(mimeMessagePart instanceof TextPart) {
    		TextPart textPart = (TextPart)mimeMessagePart;
    		content = new TextContent(textPart, textPart.getEncoding(), textPart.getCharset(), encodedData);
    	}
    	else if(mimeMessagePart instanceof ImagePart) {
    		ImagePart imagePart = (ImagePart)mimeMessagePart;
    		content = new ImageContent(imagePart, imagePart.getEncoding(), encodedData);
    	}
    	else if(mimeMessagePart instanceof ApplicationPart) {
    		ApplicationPart applicationPart = (ApplicationPart)mimeMessagePart;
    		content = new ApplicationContent(applicationPart, applicationPart.getEncoding(), encodedData);
    	}
    	else if(mimeMessagePart instanceof AudioPart) {
    		AudioPart audioPart = (AudioPart)mimeMessagePart;
    		content = new AudioContent(audioPart, audioPart.getEncoding(), encodedData);
    	}
    	else if(mimeMessagePart instanceof VideoPart) {
    		VideoPart videoPart = (VideoPart)mimeMessagePart;
    		content = new VideoContent(videoPart, videoPart.getEncoding(), encodedData);
    	}
    	else if(mimeMessagePart instanceof MessagePart) {
    		MessagePart messagePart = (MessagePart)mimeMessagePart;
    		content = new MessageContent(messagePart, messagePart.getEncoding(), encodedData);
    	}
    	else {
    		throw new UnsupportedContentException("Unsupported content type");
    	}
    	return content;
    }

    /**
     * Creates a new MessageContent object from raw data.
     * 
     * @param mimeMessagePart The message part describing the MIME properties for the content.
     * @param rawData The raw data from which to encode the content.
     * 
     * @return The message content object.
     * 
     * @throws UnsupportedContentException Thrown if the content type was not supported or the data could not be decoded.
     */
    public static MimeMessageContent createContentRaw(MimeMessagePart mimeMessagePart, byte[] rawData) throws UnsupportedContentException {
        return createContentRaw(mimeMessagePart, rawData, true);
    }
    
    /**
     * Creates a new MessageContent object from raw data.
     * 
     * @param mimeMessagePart The message part describing the MIME properties for the content.
     * @param rawData The raw data from which to encode the content.
     * @param decode True, if the data should be decoded into a form usable for
     *   display.  This should always be the case, unless the content is being
     *   created solely for the purposes of serialization.
     * 
     * @return The message content object.
     * 
     * @throws UnsupportedContentException Thrown if the content type was not supported or the data could not be decoded.
     */
	public static MimeMessageContent createContentRaw(MimeMessagePart mimeMessagePart, byte[] rawData, boolean decode) throws UnsupportedContentException {
	    MimeMessageContent content;
        if(mimeMessagePart instanceof TextPart) {
            TextPart textPart = (TextPart)mimeMessagePart;
            content = new TextContent(textPart, rawData, decode);
        }
        else if(mimeMessagePart instanceof ImagePart) {
            ImagePart imagePart = (ImagePart)mimeMessagePart;
            content = new ImageContent(imagePart, rawData, decode);
        }
        else if(mimeMessagePart instanceof ApplicationPart) {
            ApplicationPart applicationPart = (ApplicationPart)mimeMessagePart;
            content = new ApplicationContent(applicationPart, rawData);
        }
        else if(mimeMessagePart instanceof AudioPart) {
            AudioPart audioPart = (AudioPart)mimeMessagePart;
            content = new AudioContent(audioPart, rawData);
        }
        else if(mimeMessagePart instanceof VideoPart) {
            VideoPart videoPart = (VideoPart)mimeMessagePart;
            content = new VideoContent(videoPart, rawData);
        }
        else if(mimeMessagePart instanceof MessagePart) {
            MessagePart messagePart = (MessagePart)mimeMessagePart;
            content = new MessageContent(messagePart, rawData);
        }
        else {
            throw new UnsupportedContentException("Unsupported content type");
        }
	    return content;
	}
	
	/**
     * Find out if a particular message content type is supported
     * without having to create it.  This is useful to optimize
     * downloads on protocols that support selective retrieval
     * of message content.
     *
     * @param mimeMessagePart Message part object
     * @return True if supported, false if unsupported
     */
    public static boolean isContentSupported(MimeMessagePart mimeMessagePart) {
    	boolean result;
    	if(mimeMessagePart instanceof TextPart) {
    		result = TextContent.isPartSupported((TextPart)mimeMessagePart);
    	}
    	else if(mimeMessagePart instanceof ImagePart) {
    		result = ImageContent.isPartSupported((ImagePart)mimeMessagePart);
    	}
    	else if(mimeMessagePart instanceof ApplicationPart) {
    		result = ApplicationContent.isPartSupported((ApplicationPart)mimeMessagePart);
    	}
    	else if(mimeMessagePart instanceof AudioPart) {
    		result = AudioContent.isPartSupported((AudioPart)mimeMessagePart);
    	}
    	else if(mimeMessagePart instanceof VideoPart) {
    		result = VideoContent.isPartSupported((VideoPart)mimeMessagePart);
    	}
    	else if(mimeMessagePart instanceof MessagePart) {
    		result = MessageContent.isPartSupported((MessagePart)mimeMessagePart);
    	}
    	else {
    		result = false;
    	}
    	return result;
    }
}
