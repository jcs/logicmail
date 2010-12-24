/*-
 * Copyright (c) 2010, Derek Konigsberg
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.file.FileConnection;

import org.logicprobe.LogicMail.message.MimeMessageContent;
import org.logicprobe.LogicMail.message.MimeMessagePart;

import net.rim.device.api.util.CRC32;
import net.rim.device.api.util.DataBuffer;

/**
 * Wraps an open <code>FileConnection</code> for a file that contains cached
 * message content data, and provides methods for writing that data.
 * <p>
 * The specific format of the file is described in {@link MessageContentFileBase}.
 * </p>
 */
public class MessageContentFileWriter extends MessageContentFileBase {
    private int contentEndOffset;
    
    /**
     * Instantiates a new message content file wrapper.
     *
     * @param fileConnection the file connection to be used
     * @param messageUid the message UID from the message token
     */
    public MessageContentFileWriter(FileConnection fileConnection, String messageUid) {
        super(fileConnection, messageUid);
    }
    
    /**
     * Open the file for writing.
     * If the file exists, its header will be validated against the UID supplied
     * in the constructor and it will be prepared for appending new content.
     * If the file does not exist, it will be created.
     */
    public void open() throws IOException {
        checkFileNotOpen();
        if(fileConnection.exists()) {
            openExistingFile();
        }
        else {
            fileConnection.create();
            openNewFile();
        }
        
        super.open();
    }

    public void close() {
        this.contentEndOffset = 0;
        super.close();
    }
    
    private void openExistingFile() throws IOException {
        // Open an input stream
        InputStream input = fileConnection.openInputStream();
        
        // Read and validate the header
        validateHeader(input);
        
        // Skip over content sections to find the offset at the end of the file
        contentEndOffset = (int)fileConnection.fileSize();
        
        // Close the input stream
        input.close();
    }

    private void openNewFile() throws IOException {
        // Open an output stream
        OutputStream output = fileConnection.openOutputStream();
        
        // Write a fresh header
        byte[] headerData = generateHeader();
        output.write(headerData);
        
        // Note the offset for the start of data
        contentStartOffset = headerData.length;
        contentEndOffset = contentStartOffset;
        
        // Close the input stream
        output.close();
    }

    private byte[] generateHeader() throws IOException {
        DataBuffer buf = new DataBuffer();
        
        // Write the basic header fields in order
        buf.write(HEADER_TEMPLATE);
        buf.writeUTF(messageUid);

        // Insert the content offset into the header
        byte[] contentOffsetBytes = intToByteArray(buf.getLength() + 4);
        insertBytes(buf.getArray(), contentOffsetBytes, buf.getArrayStart() + 8, 4);
        
        // Calculate and insert the checksum
        int checksum = CRC32.update(CRC32.INITIAL_VALUE, buf.getArray(), buf.getArrayStart(), buf.getLength());
        buf.writeInt(checksum);
        
        return buf.toArray();
    }
    
    public void appendContent(MimeMessageContent content) throws IOException {
        checkFileOpened();
        
        DataBuffer buf = new DataBuffer();

        // Append the message part prefix 
        MimeMessagePart part = content.getMessagePart();
        buf.writeLong(part.getUniqueId());
        buf.writeInt(createPartTagHash(part));
        
        // Append the raw content data
        byte[] contentData = content.getRawData();
        buf.writeInt(contentData.length);
        buf.write(contentData);
        
        // Compute and append the checksum
        int checksum = CRC32.update(CRC32.INITIAL_VALUE, contentData);
        buf.writeInt(checksum);
        
        // Write the entry to the file
        byte[] data = buf.toArray();
        OutputStream output = fileConnection.openOutputStream(contentEndOffset);
        output.write(buf.toArray());
        output.close();
        contentEndOffset += data.length;
    }
    
    private static final void insertBytes(byte[] dest, byte[] src, int offset, int len) {
        for(int i=0; i<len; i++) {
            dest[offset+i] = src[i];
        }
    }
}
