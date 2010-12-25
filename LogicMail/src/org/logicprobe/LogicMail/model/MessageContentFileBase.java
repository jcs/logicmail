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

import org.logicprobe.LogicMail.message.MimeMessagePart;

import net.rim.device.api.util.Arrays;
import net.rim.device.api.util.CRC32;
import net.rim.device.api.util.DataBuffer;

/**
 * Common base class for classes that wrap an open <code>FileConnection</code>
 * for a file that contains cached message content data.  This class contains
 * constants and methods that are common to both reading and writing operations.
 * <p>
 * The specific format of the file is as follows:<br/>
 * <pre>
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |L M - M S G|0|1|  Off  |   A   |   B   |   C   |   D   |   0   |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * | Message token UID (UTF)                               |  CRC  |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-----------------------+-------+
 * | Part UID      |  Tag  |  Cmp  |  Len  | Content data  |  CRC  |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-----------------------+-------+
 * |                            .  .  .                            |      
 * +---------------------------------------------------------------+
 * 
 * Notes:
 *     One tick mark represents one byte position.
 *     The first row is byte-aligned, while the others are as long
 *     as they need to be.
 * </pre>
 * <table border=1>
 * <tr><td><b>Field</b></td><td><b>Type</b></td><td><b>Description</b></td></tr>
 * <tr><td>Off</td><td>int</td><td>Offset of the message content array</td></tr>
 * <tr><td>A</td><td>int</td><td>Optional data</td></tr>
 * <tr><td>B</td><td>int</td><td>Optional data</td></tr>
 * <tr><td>C</td><td>int</td><td>Optional data</td></tr>
 * <tr><td>D</td><td>int</td><td>Optional data</td></tr>
 * <tr><td>CRC</td><td>int</td><td>CRC-32 checksum of the entire header</td></tr>
 * <tr><td>Part UID</td><td>long</td><td><code>MimeMessagePart.getUniqueId()</code></td></tr>
 * <tr><td>Tag</td><td>int</td><td>
 *  Hash of the part tag, MIME type, and MIME subtype, for matching when the
 *  part UID has changed or is unavailable.  This is set to <code>0</code> if
 *  the part tag is unset.</td></tr>
 * <tr><td>Cmp</td><td>int</td><td>Is the part complete? (-1, 0, or 1)</td></tr>
 * <tr><td>Len</td><td>int</td><td>Length of the raw data array</td></tr>
 * <tr><td>Content data</td><td>byte[]</td><td>Raw content data</td></tr>
 * <tr><td>CRC</td><td>int</td><td>CRC-32 checksum of just the content data array</td></tr>
 * </table>
 * </p>
 */
abstract class MessageContentFileBase {
    protected final FileConnection fileConnection;
    protected final String messageUid;
    protected int contentStartOffset;
    private boolean fileOpened;
    
    protected static final byte[] HEADER_TEMPLATE = {
        'L',  'M',  '-',  'M',  'S',  'G',  '\0', '1',  // 8 bytes
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // 8 bytes
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // 8 bytes
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // 8 bytes
    };
    
    protected MessageContentFileBase(FileConnection fileConnection, String messageUid) {
        this.fileConnection = fileConnection;
        this.messageUid = messageUid;
    }
    
    public void open() throws IOException {
        this.fileOpened = true;
    }
    
    public void close() {
        this.contentStartOffset = 0;
        this.fileOpened = false;
    }
    
    public boolean isOpen() {
        return fileOpened;
    }
    
    protected void checkFileNotOpen() {
        if(fileOpened) {
            throw new IllegalStateException("File already opened");
        }
    }
    
    protected void checkFileOpened() {
        if(!fileOpened) {
            throw new IllegalStateException("File has not been opened");
        }
    }
    
    protected int createPartTagHash(MimeMessagePart part) {
        String tag = part.getTag();
        if(tag == null || tag.length() == 0) {
            return 0;
        }
        else {
            int hash = 7;
            hash = 31 * hash + tag.hashCode();
            hash = 31 * hash + part.getMimeType().hashCode();
            hash = 31 * hash + part.getMimeSubtype().hashCode();
            return hash;
        }
    }
    
    protected int[] validateHeader(InputStream input) throws IOException {
        DataInputStream dataInput = new DataInputStream(input);
        
        byte[] fileHeader = new byte[32];
        input.read(fileHeader, 0, 32);
        if(!Arrays.equals(fileHeader, 0, HEADER_TEMPLATE, 0, 8)) {
            throw new IOException("Invalid header: format block");
        }
        
        int fileContentOffset = byteArrayToInt(fileHeader, 8);
        int[] customValues = new int[4];
        for(int i=0; i<4; i++) {
            customValues[i] = byteArrayToInt(fileHeader, 12 + (i * 4));
        }
        
        String fileMessageUid = dataInput.readUTF();
        int fileChecksum = dataInput.readInt();
        
        DataBuffer buf = new DataBuffer();
        buf.write(fileHeader);
        buf.writeUTF(fileMessageUid);
        int checksum = CRC32.update(CRC32.INITIAL_VALUE, buf.toArray());
        
        if(checksum != fileChecksum) {
            throw new IOException("Invalid header: checksum mismatch");
        }
        if(!fileMessageUid.equals(messageUid)) {
            throw new IOException("Invalid header: does not match message UID");
        }
        
        this.contentStartOffset = fileContentOffset;
        return customValues;
    }
    
    protected static final byte[] intToByteArray(int value) {
        return new byte[] {
                (byte)(value >>> 24),
                (byte)(value >>> 16),
                (byte)(value >>> 8),
                (byte)value};
    }
    
    protected static final int byteArrayToInt(byte[] b, int off) {
        return (b[off] << 24)
        + ((b[off+1] & 0xFF) << 16)
        + ((b[off+2] & 0xFF) << 8)
        + (b[off+3] & 0xFF);
    }
}
