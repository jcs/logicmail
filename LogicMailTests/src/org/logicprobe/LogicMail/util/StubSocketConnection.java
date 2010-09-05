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
package org.logicprobe.LogicMail.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.SocketConnection;

public class StubSocketConnection implements SocketConnection {
    private static String REMOTE_ADDRESS = "192.168.20.20";
    private static String LOCAL_ADDRESS = "127.0.0.1";
    private boolean closed = false;
    private boolean isDataInputStreamOpen = false;
    private boolean isDataOutputStreamOpen = false;
    private byte[] inputData;
    private ByteArrayOutputStream byteOutput;
    
    public StubSocketConnection() {
        this(new byte[0]);
    }
    
    public StubSocketConnection(byte[] inputData) {
        this.inputData = inputData;
        this.byteOutput = new ByteArrayOutputStream();
    }
    
    public byte[] getSentBytes() {
        return byteOutput.toByteArray();
    }
    
    public void resetSentBytes() {
        byteOutput.reset();
    }
    
    public boolean isClosed() {
        return closed;
    }
    
    public String getAddress() throws IOException {
        return REMOTE_ADDRESS;
    }
    
    public int getPort() throws IOException {
        return 42;
    }

    public String getLocalAddress() throws IOException {
        return LOCAL_ADDRESS;
    }

    public int getLocalPort() throws IOException {
        return 1042;
    }

    public int getSocketOption(byte option) throws IllegalArgumentException, IOException {
        return 0;
    }

    public void setSocketOption(byte option, int value) throws IllegalArgumentException, IOException {
    }

    public DataInputStream openDataInputStream() throws IOException {
        if(isDataInputStreamOpen) { throw new IOException(); }
        isDataInputStreamOpen = true;
        return new DataInputStream(new ByteArrayInputStream(inputData));
    }

    public DataOutputStream openDataOutputStream() throws IOException {
        if(isDataOutputStreamOpen) { throw new IOException(); }
        isDataOutputStreamOpen = true;
        return new DataOutputStream(byteOutput);
    }
    
    public InputStream openInputStream() throws IOException {
        return openDataInputStream();
    }

    public OutputStream openOutputStream() throws IOException {
        return openDataOutputStream();
    }
    
    public void close() throws IOException {
        closed = true;
    }
}
