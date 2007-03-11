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

package org.logicprobe.LogicMail.util;

import java.io.IOException;
import net.rim.device.api.io.Base64InputStream;
import net.rim.device.api.io.Base64OutputStream;

/**
 * Proxy for utility classes and methods on BlackBerry OS v4.1
 */
public class UtilProxyBB41 extends UtilProxy {
    
    /**
     * Creates a new instance of UtilProxyBB41
     */
    protected UtilProxyBB41() {
    }
    
    public byte[] Base64Decode(String input) throws IOException {
        return Base64InputStream.decode(input);
    }

    public byte[] Base64Encode(byte[] input, int inputOffset, int inputLength, boolean insertCR, boolean insertLF) throws IOException {
        return Base64OutputStream.encode(input, inputOffset, inputLength, insertCR, insertLF);
    }

    public String Base64EncodeAsString(byte[] input, int inputOffset, int inputLength, boolean insertCR, boolean insertLF) throws IOException {
        return Base64OutputStream.encodeAsString(input, inputOffset, inputLength, insertCR, insertLF);
    }
}
