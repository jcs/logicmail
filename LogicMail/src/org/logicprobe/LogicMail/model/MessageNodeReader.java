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
package org.logicprobe.LogicMail.model;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Hashtable;

import org.logicprobe.LogicMail.AppInfo;
import org.logicprobe.LogicMail.mail.MessageToken;
import org.logicprobe.LogicMail.message.MimeMessageContent;
import org.logicprobe.LogicMail.message.MimeMessagePart;
import org.logicprobe.LogicMail.util.SerializationUtils;

import net.rim.device.api.system.EventLogger;
import net.rim.device.api.util.Arrays;
import net.rim.device.api.util.CRC32;

/**
 * Reads in <tt>MessageNode</tt> objects from an input stream.
 * <p>
 * Allows for separate reading of header information and content,
 * to facilitate partial loading of cached messages.
 * The specific data format is described in detail within
 * {@link MessageNodeWriter}.
 * </p>
 */
public class MessageNodeReader {
	private DataInputStream input;
	private int contentOffset = 0;
	private int contentCount = 0;
	private boolean headerParsed;
	
	private final static byte[] headerStart = {
		'L',  'M',  '-',  'M',  'S',  'G',  '\0', '\0'
	};
	
	/**
	 * Instantiates a new message node reader.
	 * 
	 * @param input the input
	 */
	public MessageNodeReader(DataInputStream input) {
		this.input = input;
	}
	
	/**
	 * Read the message file header, and return the MessageToken object.
	 * <p>
	 * This puts the stream in a position for reading content, if available,
	 * without actually creating another MessageNode instance.  The token
	 * should be used to validate that the file matches the existing node
	 * of interest.
	 * </p>
	 * 
	 * @return the message token
	 * 
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public MessageToken read() throws IOException {
		if(headerParsed) { throw new IllegalStateException(); }
		if(!readFileHeader()) { return null; }
		int offset = 32;
		
		int classLength = input.readInt();
		offset += 4;
		byte[] classBytes = new byte[classLength];
		input.read(classBytes);
		offset += classLength;
		MessageToken messageToken = (MessageToken)SerializationUtils.deserializeClass(classBytes);
		if(messageToken != null) {
			input.skipBytes(contentOffset - offset);
			headerParsed = true;
		}
		
		return messageToken;
	}
	
	/**
	 * Read the message file header, and create a new MessageNode
	 * with all the data except for the message content.
	 * <p>
	 * This puts the stream in a position for reading content, if available.
	 * </p>
	 * 
	 * @return the message node
	 * 
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public MessageNode readMessageNode() throws IOException {
		if(headerParsed) { throw new IllegalStateException(); }
		if(!readFileHeader()) { return null; }

		MessageToken messageToken = (MessageToken)SerializationUtils.deserializeClass(input);
		Hashtable messageHeaders = (Hashtable)SerializationUtils.deserializeClass(input);
		MimeMessagePart messageStructure = (MimeMessagePart)SerializationUtils.deserializeClass(input);
		
		MessageNode messageNode = buildMessageNode(messageToken, messageHeaders, messageStructure);
		if(messageNode != null) { headerParsed = true; }
		return messageNode;
	}

	private boolean readFileHeader() throws IOException {
		byte[] fileHeader = new byte[32];
		input.read(fileHeader, 0, 32);
		if(!validateHeader(fileHeader)) { return false; }
		
		contentOffset = byteArrayToInt(fileHeader, 20);
		contentCount = byteArrayToInt(fileHeader, 24);
		return true;
	}
	
	/**
	 * Checks if more content is available for reading.
	 * 
	 * @return true, if content is available
	 */
	public boolean isContentAvailable() {
		if(!headerParsed) { throw new IllegalStateException(); }
		return contentCount > 0;
	}
	
	/**
	 * Gets the next content section of the message.
	 * 
	 * @return the next content section
	 * 
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public MimeMessageContent getNextContent() throws IOException {
		if(!headerParsed || contentCount == 0) { throw new IllegalStateException(); }
		if (EventLogger.getMinimumLevel() >= EventLogger.DEBUG_INFO) {
			EventLogger.logEvent(AppInfo.GUID,
                ("MessageNodeReader.getNextContent()\r\n"
            		+ "contentCount=" + contentCount
            		+ ", available=" + input.available()).getBytes(),
                EventLogger.DEBUG_INFO);
        }
		
		contentCount--;
		
		int length = input.readInt();
		int checksum = CRC32.update(CRC32.INITIAL_VALUE, length);
		byte[] contentBytes = new byte[length];
		
		input.read(contentBytes, 0, length);
		checksum = CRC32.update(checksum, contentBytes);
		
		int storedChecksum = input.readInt();
		
		if(storedChecksum != checksum) {
			EventLogger.logEvent(AppInfo.GUID,
                ("MessageNodeReader.getNextContent()\r\nContent checksum mismatch").getBytes(),
                EventLogger.ERROR);
			return null;
		}
		
		Object contentObject = SerializationUtils.deserializeClass(contentBytes);
		if(contentObject instanceof MimeMessageContent) {
			return (MimeMessageContent)contentObject;
		}
		else {
			EventLogger.logEvent(AppInfo.GUID,
                ("MessageNodeReader.getNextContent()\r\nContent deserialization error").getBytes(),
                EventLogger.ERROR);
			return null;
		}
	}
	
	private boolean validateHeader(byte[] fileHeader) {
		// Validate the header start text
		if(!Arrays.equals(fileHeader, 0, headerStart, 0, headerStart.length)) {
			EventLogger.logEvent(AppInfo.GUID,
                ("MessageNodeReader.validateHeader()\r\nHeader start mismatch").getBytes(),
                EventLogger.ERROR);
			return false;
		}
		
		// Validate the header checksum
		int headerChecksum = byteArrayToInt(fileHeader, 28);
		byte[] headerCopy = Arrays.copy(fileHeader);
		Arrays.fill(headerCopy, (byte)0x00, 28, 4);
		int checksum = CRC32.update(CRC32.INITIAL_VALUE, headerCopy);
		if(headerChecksum != checksum) {
			EventLogger.logEvent(AppInfo.GUID,
                ("MessageNodeReader.validateHeader()\r\nHeader checksum mismatch").getBytes(),
                EventLogger.ERROR);
			return false;
		}
		return true;
	}

	private MessageNode buildMessageNode(
			MessageToken messageToken,
			Hashtable messageHeaders,
			MimeMessagePart messageStructure) {

		MessageNode messageNode = new MessageNode(messageToken);
		
		populateMessageHeaders(messageNode, messageHeaders);
		
		messageNode.setMessageStructure(messageStructure);
		
		return messageNode;
	}

	private static String HEADER_KEY_FLAGS = "flags";
	private static String HEADER_KEY_DATE = "date";
	private static String HEADER_KEY_SUBJECT = "subject";
	private static String HEADER_KEY_FROM = "from";
	private static String HEADER_KEY_SENDER = "sender";
	private static String HEADER_KEY_REPLYTO = "replyTo";
	private static String HEADER_KEY_TO = "to";
	private static String HEADER_KEY_CC = "cc";
	private static String HEADER_KEY_BCC = "bcc";
	private static String HEADER_KEY_INREPLYTO = "inReplyTo";
	private static String HEADER_KEY_MESSAGEID = "messageId";

	private void populateMessageHeaders(MessageNode messageNode, Hashtable messageHeaders) {
		Object item;
		item = messageHeaders.get(HEADER_KEY_FLAGS);
		if(item instanceof Integer) { messageNode.setFlags(((Integer)item).intValue()); }
		
		item = messageHeaders.get(HEADER_KEY_DATE);
		if(item instanceof Date) { messageNode.setDate((Date)item); }

		item = messageHeaders.get(HEADER_KEY_SUBJECT);
		if(item instanceof String) { messageNode.setSubject((String)item); }

		item = messageHeaders.get(HEADER_KEY_FROM);
		if(item instanceof String[]) { messageNode.setFrom(createAddressArray((String[])item)); }
		
		item = messageHeaders.get(HEADER_KEY_SENDER);
		if(item instanceof String[]) { messageNode.setSender(createAddressArray((String[])item)); }
		
		item = messageHeaders.get(HEADER_KEY_REPLYTO);
		if(item instanceof String[]) { messageNode.setReplyTo(createAddressArray((String[])item)); }
		
		item = messageHeaders.get(HEADER_KEY_TO);
		if(item instanceof String[]) { messageNode.setTo(createAddressArray((String[])item)); }
		
		item = messageHeaders.get(HEADER_KEY_CC);
		if(item instanceof String[]) { messageNode.setCc(createAddressArray((String[])item)); }
		
		item = messageHeaders.get(HEADER_KEY_BCC);
		if(item instanceof String[]) { messageNode.setBcc(createAddressArray((String[])item)); }

		item = messageHeaders.get(HEADER_KEY_INREPLYTO);
		if(item instanceof String) { messageNode.setInReplyTo((String)item); }
		
		item = messageHeaders.get(HEADER_KEY_MESSAGEID);
		if(item instanceof String) { messageNode.setMessageId((String)item); }
	}
	
	private static Address[] createAddressArray(String[] addresses) {
		Address[] addressArray = new Address[addresses.length];
		for(int i=0; i<addresses.length; i++) {
			addressArray[i] = new Address(addresses[i]);
		}
		return addressArray;
	}
	
	private static final int byteArrayToInt(byte[] b, int off) {
		return (b[off] << 24)
          + ((b[off+1] & 0xFF) << 16)
          + ((b[off+2] & 0xFF) << 8)
          + (b[off+3] & 0xFF);
	}
}
