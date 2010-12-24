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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.microedition.io.file.FileConnection;

import net.rim.device.api.util.CRC32;
import net.rim.device.api.util.IntIntHashtable;
import net.rim.device.api.util.LongIntHashtable;

import org.logicprobe.LogicMail.message.MimeMessageContent;
import org.logicprobe.LogicMail.message.MimeMessageContentFactory;
import org.logicprobe.LogicMail.message.MimeMessagePart;
import org.logicprobe.LogicMail.message.UnsupportedContentException;

/**
 * Wraps an open <code>FileConnection</code> for a file that contains cached
 * message content data, and provides methods for reading that data.
 * <p>
 * The specific format of the file is described in {@link MessageContentFileBase}.
 * </p>
 */
public class MessageContentFileReader extends MessageContentFileBase {
    private final LongIntHashtable contentUidMap = new LongIntHashtable();
    private final IntIntHashtable contentTagMap = new IntIntHashtable();
    
    /**
     * Instantiates a new message content file wrapper.
     *
     * @param fileConnection the file connection to be used
     * @param messageUid the message UID from the message token
     */
    public MessageContentFileReader(FileConnection fileConnection, String messageUid) {
        super(fileConnection, messageUid);
    }
    
    /**
     * Open the file for reading.
     * The file's header will be validated against the UID supplied in the
     * constructor and it will be prepared for reading content.
     */
    public void open() throws IOException {
        checkFileNotOpen();
        
        // Open an input stream
        InputStream input = fileConnection.openInputStream();
        
        // Read and validate the header
        validateHeader(input);
        
        // Iterate over the file to build a map of content sections
        buildContentMap(input, fileConnection.fileSize());
        
        // Close the input stream
        input.close();
        
        super.open();
    }

    public void close() {
        this.contentUidMap.clear();
        this.contentTagMap.clear();
        super.close();
    }
    
    private void buildContentMap(InputStream input, long fileSize) throws IOException {
        int offset = contentStartOffset;

        DataInputStream dataInput = new DataInputStream(input);
        while(offset < fileSize) {
            long partUid = dataInput.readLong();
            int partTagHash = dataInput.readInt();
            int contentLen = dataInput.readInt();

            // 12 = UID(8) + Tag(4)
            int dataOffset = offset + 12;
            
            dataInput.skip(contentLen + 4); // Len + CRC
            
            contentUidMap.put(partUid, dataOffset);
            if(partTagHash != 0) {
                contentTagMap.put(partTagHash, dataOffset);
            }
            
            // UID(8) + Tag(4) + Len(4) + contentLen + CRC(4)
            offset += 16 + contentLen + 4;
        }
    }
    
    /**
     * Checks whether the file contains content for the provided message part.
     *
     * @param part the part to check
     * @return true, if content exists
     */
    public boolean hasContent(MimeMessagePart part) {
        checkFileOpened();
        
        if(contentUidMap.containsKey(part.getUniqueId())) {
            return true;
        }
        else if(contentTagMap.containsKey(createPartTagHash(part))) {
            return true;
        }
        else {
            return false;
        }
    }
    
    /**
     * Gets the content for the provided message part.
     *
     * @param part the part to get content for
     * @return the content, if available
     */
    public MimeMessageContent getContent(MimeMessagePart part) throws IOException, UnsupportedContentException {
        checkFileOpened();
        
        int contentOffset = -1;
        if(contentUidMap.containsKey(part.getUniqueId())) {
            contentOffset = contentUidMap.get(part.getUniqueId());
        }
        else {
            int tagHash = createPartTagHash(part);
            if(contentTagMap.containsKey(tagHash)) {
                contentOffset = contentTagMap.get(tagHash);
            }
        }
        if(contentOffset == -1) { return null; }
        
        DataInputStream dataInput = fileConnection.openDataInputStream();
        dataInput.skip(contentOffset);
        
        int contentLen = dataInput.readInt();
        byte[] data = new byte[contentLen];
        dataInput.read(data);
        int checksum = CRC32.update(CRC32.INITIAL_VALUE, data);
        
        int fileCRC = dataInput.readInt();
        
        dataInput.close();
        
        if(checksum != fileCRC) {
            throw new IOException("Invalid content: checksum mismatch");
        }
        
        MimeMessageContent content = MimeMessageContentFactory.createContentRaw(part, data);
        
        return content;
    }
}
