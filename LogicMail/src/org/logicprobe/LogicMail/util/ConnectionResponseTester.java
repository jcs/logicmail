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

/**
 * Abstract base for classes that test the contents of a <code>byte[]</code>
 * array received from the network to see if it contains a complete response
 * that should be passed back to the requestor.
 */
public abstract class ConnectionResponseTester {
    /**
     * Checks the provided buffer for a complete response.
     * 
     * @param buf the raw socket receive buffer
     * @param len the length of the valid data within the buffer
     * @return length of the complete response, or <code>-1</code> if none
     *   is currently available within the buffer
     */
    public abstract int checkForCompleteResponse(byte[] buf, int len);
    
    /**
     * Number of characters to trim from the response prior to returning the
     * resulting data.  Primarily useful for cases such as trimming CRLF codes.
     * Expect this method to be called immediately following a call to
     * {{@link #checkForCompleteResponse(byte[], int)} that provides a
     * successful result.
     * 
     * @return number of characters to trim
     */
    public int trimCount() { return 0; }
    
    /**
     * Log string to use if connection debugging is enabled.
     * <p>
     * The default behavior is to return the length of the byte array.
     * </p>
     * @param the raw data about to be returned to the requestor
     * @return the string to put in the event log
     */
    public String logString(byte[] result) {
        return '[' + Integer.toString(result.length) + ']';
    }
}
