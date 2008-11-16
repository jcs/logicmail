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
package org.logicprobe.LogicMail.mail;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

import org.logicprobe.LogicMail.message.FolderMessage;
import org.logicprobe.LogicMail.message.MessageEnvelope;
import org.logicprobe.LogicMail.util.MailHeaderParser;

/**
 * Implements an interface to messages stored on device file storage
 * in the Maildir (http://cr.yp.to/proto/maildir.html) format.
 * 
 * In addition to the standard Maildir files, this implementation
 * also uses a file called "index.dat" that is written to the root
 * of the maildir.  This file is used to store pre-parsed message
 * headers, so that the actual message files do not need to be
 * parsed just to generate a mailbox listing.
 */
public class MaildirFolder {
	private String folderUrl;
	private FileConnection fileConnection;
	private Hashtable messageEnvelopeMap;
	private boolean messageEnvelopeMapDirty = true;
	private static String EOF_MARKER = "----";
	
	/**
	 * Creates a new instance of the <tt>MaildirFolder</tt> class.
	 * 
	 * @param folderUrl Path to the root folder of the maildir.
	 */
	public MaildirFolder(String folderUrl) {
		this.folderUrl = folderUrl;
		messageEnvelopeMap = new Hashtable();
	}
	
	/**
	 * Open the maildir for reading.
	 * This method make sure the underlying folder exists, and attempts
	 * to read the index file if available.
	 * 
	 * @throws IOException Thrown on I/O errors
	 */
	public void open() throws IOException {
		System.err.println("Opening: " + folderUrl);
		fileConnection = (FileConnection)Connector.open(folderUrl);
		if(!fileConnection.exists()) {
			fileConnection.create();
		}

		fileConnection.setFileConnection("cur");
		if(!fileConnection.exists()) {
			fileConnection.create();
		}
		
		// Read in the message envelope map
		FileConnection indexFileConnection = (FileConnection)Connector.open(folderUrl + "/index.dat");
		if(indexFileConnection.exists()) {
			DataInputStream inputStream = indexFileConnection.openDataInputStream();
			try {
				while(true) {
					String uniqueId = inputStream.readUTF();
					if(uniqueId.equals(EOF_MARKER)) {
						break;
					}
					MessageEnvelope envelope = new MessageEnvelope();
					envelope.deserialize(inputStream);
					messageEnvelopeMap.put(uniqueId, envelope);
				}
				messageEnvelopeMapDirty = false;
			} catch (IOException exp) {
				// Non-fatally force the map dirty on exceptions, since this can
				// only happen if the index file is truncated, and may not mean
				// that any other problems exist.
				messageEnvelopeMapDirty = true;
			}
		}

		System.err.println("Opened with " + messageEnvelopeMap.size() + " messages in index file");	
	}

	/**
	 * Closes the maildir, writing out the index file.
	 * 
	 * @throws IOException Thrown on I/O errors
	 */
	public void close() throws IOException {
		if(fileConnection != null) {
			fileConnection.close();
			fileConnection = null;
		}

		// Write out the message envelope map
		if(messageEnvelopeMapDirty) {
			FileConnection indexFileConnection = (FileConnection)Connector.open(folderUrl + "/index.dat");
			if(indexFileConnection.exists()) {
				indexFileConnection.truncate(0);
			}
			else {
				indexFileConnection.create();
			}
			DataOutputStream outputStream = indexFileConnection.openDataOutputStream();
			Enumeration e = messageEnvelopeMap.keys();
			while(e.hasMoreElements()) {
				String uniqueId = (String)e.nextElement();
				outputStream.writeUTF(uniqueId);
	
				MessageEnvelope envelope = (MessageEnvelope)messageEnvelopeMap.get(uniqueId);
				envelope.serialize(outputStream);
			}
			// Write the end-of-file marker so we can avoid the need for headers
			outputStream.writeUTF(EOF_MARKER);
			outputStream.close();
			indexFileConnection.close();
			messageEnvelopeMapDirty = false;
		}
	}
	
	/**
	 * Gets all the <tt>FolderMessage</tt>s contained within this maildir.
	 * Message headers come from the index file if available, otherwise
	 * they are parsed from the message files.
	 * Message flags are always parsed from the message filenames.
	 * 
	 * @return Array of <tt>FolderMessage</tt>s.
	 * @throws IOException Thrown on I/O errors
	 */
	public FolderMessage[] getFolderMessages() throws IOException {
		System.err.println("Getting folder messages");
		if(fileConnection == null) {
			throw new IOException("Maildir not open");
		}
		Vector fileList = new Vector();
		Enumeration e = fileConnection.list();
		while(e.hasMoreElements()) {
			String file = (String)e.nextElement();
			// Quick and dirty check for maildir-like files
			// Actual maildir files use a colon to separate the unique id
			// from the flags, but that character is not supported here.
			// The framework seems to convert the colon to an underscore,
			// so we check for that instead.
			if(file.indexOf('_') != -1) {
				fileList.addElement(file);
			}
		}
		Vector folderMessageList = new Vector();
		int size = fileList.size();
		int index = 0;
		for(int i=0; i<size; i++) {
			String fileName = (String)fileList.elementAt(i);
			FileConnection mailFileConnection = (FileConnection)Connector.open(fileConnection.getURL() + '/' + fileName);
				
			if(mailFileConnection.exists() && !mailFileConnection.isDirectory() && mailFileConnection.canRead()) {
				try {
					int p = fileName.indexOf("_2,");
					if(p != -1) {
						String uniqueId = fileName.substring(0, p);
						
						MessageEnvelope envelope;
						if(messageEnvelopeMap.containsKey(uniqueId)) {
							envelope = (MessageEnvelope)messageEnvelopeMap.get(uniqueId);
						}
						else {
							InputStream inputStream = mailFileConnection.openInputStream();
							envelope = getMessageEnvelope(inputStream);
							inputStream.close();
							messageEnvelopeMap.put(uniqueId, envelope);
							messageEnvelopeMapDirty = true;
						}
						FolderMessage folderMessage = new FolderMessage(envelope, index++, -1);
						
						// Check for flags
						p += 3;
						folderMessage.setAnswered(fileName.indexOf('R', p) != -1);
						folderMessage.setDeleted(fileName.indexOf('T', p) != -1);
						folderMessage.setDraft(fileName.indexOf('D', p) != -1);
						folderMessage.setFlagged(fileName.indexOf('F', p) != -1);
						folderMessage.setSeen(fileName.indexOf('S', p) != -1);
						
						folderMessageList.addElement(folderMessage);
					}
				} catch (Exception exp) {
					// Prevent message-reading errors from being fatal
					// TODO: Log a useful error message here
				}
			}
			
			mailFileConnection.close();
		}
		FolderMessage[] result = new FolderMessage[folderMessageList.size()];
		folderMessageList.copyInto(result);
		return result;
	}

	/**
	 * Reads the raw mail file from the provided InputStream, and parses
	 * out the message envelope.
	 * 
	 * @param inputStream The input stream to read from.
	 * @return The message envelope.
	 * @throws IOException Thrown on I/O errors.
	 */
	private MessageEnvelope getMessageEnvelope(InputStream inputStream) throws IOException {
		InputStreamReader reader = new InputStreamReader(inputStream);
		Vector headerLines = new Vector();
		StringBuffer buf = new StringBuffer();
		int ch;
		while((ch = reader.read()) != -1) {
			if(ch == 0x0A) {
				if(buf.length() > 0) {
					headerLines.addElement(buf.toString());
					buf.delete(0, buf.length());
				}
				else {
					break;
				}
			}
			else if(ch != 0x0D) {
				buf.append((char)ch);
			}
		}

		String[] headerLinesArray = new String[headerLines.size()];
		headerLines.copyInto(headerLinesArray);
		MessageEnvelope envelope = MailHeaderParser.parseMessageEnvelope(headerLinesArray);
		return envelope;
	}
}

