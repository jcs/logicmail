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

import java.io.DataOutputStream;
import java.io.IOException;

import net.rim.device.api.util.Arrays;
import net.rim.device.api.util.CRC32;

import org.logicprobe.LogicMail.mail.MessageToken;
import org.logicprobe.LogicMail.mail.NetworkMailSender;
import org.logicprobe.LogicMail.message.MimeMessageContent;
import org.logicprobe.LogicMail.message.MimeMessagePart;
import org.logicprobe.LogicMail.util.SerializableHashtable;
import org.logicprobe.LogicMail.util.SerializationUtils;

/**
 * Writes the contents of a <tt>MessageNode</tt> to an output stream
 * in a format suitable for cache file storage.
 * <p>
 * The specific format of the data is as follows:<br/>
 * <pre>
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |L M - M S G|0|0|   A   |   B   |   C   |   D   |   E   |  CRC  |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * | Len | Message token (MessageToken)                            |
 * +-+-+-+---------------------------------------------------------+
 * | Len | Message header fields (SerializableHashtable)           |
 * +-+-+-+   flags (int), date (long), subject (string),           |
 * |         from[], sender[], replyTo[], to[], cc[], bcc[],       |
 * |         inReplyTo, messageId                                  |
 * +-+-+-+---------------------------------------------------------+
 * | Len | Message structure (MimeMessagePart)                     |
 * +-+-+-+-------------------------------------------------+-+-+-+-+
 * | Content (MimeMessageContent)                          |  CRC  |
 * +-------------------------------------------------------+-+-+-+-+
 * 
 * Notes:
 *     One tick mark represents one byte position.
 *     The first row is byte-aligned, while the others are as long
 *     as they need to be.
 *     
 * </pre>
 * <table border=1>
 * <tr><td><b>Field</b></td><td><b>Description</b></td></tr>
 * <tr><td>A</td><td>Offset of the serialized MessageToken</td></tr>
 * <tr><td>B</td><td>Offset of the serialized header fields</td></tr>
 * <tr><td>C</td><td>Offset of the serialized MimeMessagePart structure (0 if not present)</td></tr>
 * <tr><td>D</td><td>Offset of the message content array (0 if not present)</td></tr>
 * <tr><td>E</td><td>Number of message content sections</td></tr>
 * <tr><td>CRC</td><td>
 *     CRC-32 checksum of the row<br/>
 *     &nbsp;&nbsp;For the header field, it is calculated with the field zeroed<br/>
 *     &nbsp;&nbsp;For the content, it does not include the bytes occupied by the CRC<br/>
 *     </td></tr>
 * </table>
 * </p>
 */
public class MessageNodeWriter {
	private DataOutputStream output;
	
	private final static byte[] headerTemplate = {
		'L',  'M',  '-',  'M',  'S',  'G',  '\0', '\0', // 8 bytes
		0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // 8 bytes
		0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // 8 bytes
		0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // 8 bytes
	};
	
	/**
	 * Instantiates a new message node writer.
	 * 
	 * @param output the output stream
	 */
	public MessageNodeWriter(DataOutputStream output) {
		this.output = output;
	}
	
	/**
	 * Write a complete message node to the output stream.
	 * 
	 * @param messageNode the message node
	 * 
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void write(MessageNode messageNode) throws IOException {
		byte[] messageToken = generateMessageToken(messageNode);
		byte[] messageHeaders = generateMessageHeaders(messageNode);
		byte[] messageStructure = generateMessageStructure(messageNode);
		
		MimeMessageContent[] content = messageNode.getAllMessageContent();
		
		byte[] fileHeader = generateFileHeader(
				messageNode,
				messageToken.length,
				messageHeaders.length,
				messageStructure.length,
				content.length);
		
		output.write(fileHeader);
		output.writeInt(messageToken.length);
		output.write(messageToken);
		output.writeInt(messageHeaders.length);
		output.write(messageHeaders);
		output.writeInt(messageStructure.length);
		output.write(messageStructure);
		output.flush();

		writeMessageContent(output, content);
		output.flush();
	}

	private byte[] generateMessageToken(MessageNode messageNode) {
		MessageToken token = messageNode.getMessageToken();
		return SerializationUtils.serializeClass(token);
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

	/** Indicates that this message should be deserialized as an outgoing message. */
	private static String HEADER_KEY_OUTGOING = "OUTGOING";
	/** UID of the outgoing account configuration. */
	private static String HEADER_KEY_OUTGOING_SENDING_ACCOUNT = "OUTGOING_SENDING_ACCOUNT";
	/** UID of the outgoing mail sender configuration. */
    private static String HEADER_KEY_OUTGOING_MAIL_SENDER = "OUTGOING_MAIL_SENDER";
    /** UID of the outgoing reply-to message's account configuration. */
    private static String HEADER_KEY_OUTGOING_REPLYTO_ACCOUNT = "OUTGOING_REPLYTO_ACCOUNT";
    /** Token of the outgoing reply-to message. */
    private static String HEADER_KEY_OUTGOING_REPLYTO_TOKEN = "OUTGOING_REPLYTO_TOKEN";
	
	private byte[] generateMessageHeaders(MessageNode messageNode) throws IOException {
		SerializableHashtable table = new SerializableHashtable();
		table.put(HEADER_KEY_FLAGS, new Integer(messageNode.getFlags()));
		putInTable(table, HEADER_KEY_DATE, messageNode.getDate());
		putInTable(table, HEADER_KEY_SUBJECT, messageNode.getSubject());
		table.put(HEADER_KEY_FROM, createAddressArray(messageNode.getFrom()));
		table.put(HEADER_KEY_SENDER, createAddressArray(messageNode.getSender()));
		table.put(HEADER_KEY_REPLYTO, createAddressArray(messageNode.getReplyTo()));
		table.put(HEADER_KEY_TO, createAddressArray(messageNode.getTo()));
		table.put(HEADER_KEY_CC, createAddressArray(messageNode.getCc()));
		table.put(HEADER_KEY_BCC, createAddressArray(messageNode.getBcc()));
		putInTable(table, HEADER_KEY_INREPLYTO, messageNode.getInReplyTo());
		putInTable(table, HEADER_KEY_MESSAGEID, messageNode.getMessageId());
		
		if(messageNode instanceof OutgoingMessageNode) {
		    OutgoingMessageNode outgoingMessage = (OutgoingMessageNode)messageNode;
		    
		    // Put the outgoing flag
		    table.put(HEADER_KEY_OUTGOING, Boolean.TRUE);
		    
		    if(outgoingMessage.getSendingAccount() != null && outgoingMessage.getSendingAccount() instanceof NetworkAccountNode) {
		        table.put(HEADER_KEY_OUTGOING_SENDING_ACCOUNT,
		                new Long(((NetworkAccountNode)outgoingMessage.getSendingAccount()).getUniqueId()));
		    }
		    if(outgoingMessage.getMailSender() != null && outgoingMessage.getMailSender() instanceof NetworkMailSender) {
		        NetworkMailSender mailSender = (NetworkMailSender)outgoingMessage.getMailSender();
		        if(mailSender.getOutgoingConfig() != null) {
		            table.put(HEADER_KEY_OUTGOING_MAIL_SENDER, new Long(mailSender.getOutgoingConfig().getUniqueId()));
		        }
		        
		    }
		    
		    if(outgoingMessage.getReplyToAccount() != null) {
	            AccountNode replyToAccount = outgoingMessage.getReplyToAccount();
	            if(replyToAccount instanceof NetworkAccountNode) {
	                table.put(HEADER_KEY_OUTGOING_REPLYTO_ACCOUNT, new Long(((NetworkAccountNode)replyToAccount).getUniqueId()));
	            }
		    }
		    
		    if(outgoingMessage.getReplyToToken() != null) {
		        byte[] tokenBytes = SerializationUtils.serializeClass(outgoingMessage.getReplyToToken());
		        table.put(HEADER_KEY_OUTGOING_REPLYTO_TOKEN, tokenBytes);
		    }
		}
		
		return SerializationUtils.serializeClass(table);
	}
	
	/**
	 * Puts an item in the table, checking the value for null first.
	 * This is a shortcut to simplify methods that populate a table.
	 * 
	 * @param table the table being populated
	 * @param key the item key
	 * @param value the item value
	 */
	private static void putInTable(SerializableHashtable table, Object key, Object value) {
	    if(value != null) {
	        table.put(key, value);
	    }
	}
	
	private static String[] createAddressArray(Address[] addresses) {
		if(addresses == null) { return new String[0]; }
		
		String[] addressArray = new String[addresses.length];
		for(int i=0; i<addresses.length; i++) {
			addressArray[i] = addresses[i].toString();
		}
		return addressArray;
	}

	private byte[] generateMessageStructure(MessageNode messageNode) {
		MimeMessagePart structure = messageNode.getMessageStructure();
		if(structure != null) {
			return SerializationUtils.serializeClass(structure);
		}
		else {
			return new byte[0];
		}
	}

	private byte[] generateFileHeader(
			MessageNode messageNode,
			int messageTokenLength,
			int messageHeadersLength,
			int messageStructureLength,
			int contentCount) {
		
		byte[] header = Arrays.copy(headerTemplate);

		int mark = headerTemplate.length;
		insertBytes(header, intToByteArray(mark), 8, 4);
		mark += messageTokenLength + 4;
		
		insertBytes(header, intToByteArray(mark), 12, 4);
		
		if(messageHeadersLength > 0) {
			mark += messageHeadersLength + 4;
			
			insertBytes(header, intToByteArray(mark), 16, 4);
			mark += messageStructureLength + 4;
			
			insertBytes(header, intToByteArray(mark), 20, 4);
			
			insertBytes(header, intToByteArray(contentCount), 24, 4);
		}
		
		int checksum = CRC32.update(CRC32.INITIAL_VALUE, header);
		insertBytes(header, intToByteArray(checksum), 28, 4);
		
		return header;
	}
	
	private void writeMessageContent(DataOutputStream output, MimeMessageContent[] content) throws IOException {
		for(int i=0; i<content.length; i++) {
			byte[] contentBytes = SerializationUtils.serializeClass(content[i]);
			int checksum = CRC32.update(CRC32.INITIAL_VALUE, contentBytes.length);
			output.writeInt(contentBytes.length);
			output.write(contentBytes);
			checksum = CRC32.update(checksum, contentBytes);
			output.writeInt(checksum);
		}
	}
	
	private static final void insertBytes(byte[] dest, byte[] src, int offset, int len) {
		for(int i=0; i<len; i++) {
			dest[offset+i] = src[i];
		}
	}
	
	private static final byte[] intToByteArray(int value) {
        return new byte[] {
                (byte)(value >>> 24),
                (byte)(value >>> 16),
                (byte)(value >>> 8),
                (byte)value};
	}
}
