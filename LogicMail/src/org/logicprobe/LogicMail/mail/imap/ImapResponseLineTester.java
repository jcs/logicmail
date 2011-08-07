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
package org.logicprobe.LogicMail.mail.imap;

import org.logicprobe.LogicMail.util.ConnectionResponseTester;
import org.logicprobe.LogicMail.util.StringArrays;

/**
 * Tests received data from a Connection for complete lines, making special
 * exception to ignore line breaks that are part of the IMAP "literal" format,
 * or that erroneously appear in the middle of the IMAP "quoted" format.
 */
public class ImapResponseLineTester extends ConnectionResponseTester {
    private static final byte CR = (byte)0x0D;
    private static final byte LF = (byte)0x0A;
    private static final byte LCBRACKET = (byte)'{';
    private static final byte RCBRACKET = (byte)'}';
    private static final byte DQUOTE = (byte)'\"';
    private static final byte BACKSLASH = (byte)'\\';
    private static final byte DIGIT_ZERO = (byte)'0';
    private static final byte DIGIT_NINE = (byte)'9';
    
    private int trimCount;
    private int lastLength = 0;
    private int literalLength = -1;
    private boolean inQuoted;
    private boolean inQuotedEscape;

    public int checkForCompleteResponse(byte[] buf, int len) {
        trimCount = 0;

        if (literalLength >= 0) {
            if ((lastLength + literalLength) < len) {
                lastLength += literalLength;
                literalLength = -1;
            } else {
                literalLength -= (len - lastLength);
                lastLength = len;

                return -1;
            }
        }
 
        int p = indexOfLinefeedIgnoringQuoted(buf, lastLength, len);

        while ((p != -1) && (p < len)) {
            if ((p > 0) && (buf[p - 1] == CR)) {
                if ((p > 3) && (buf[p - 2] == RCBRACKET)) {
                    int i = p - 3;

                    while (i >= 0) {
                        if ((buf[i] >= DIGIT_ZERO) && (buf[i] <= DIGIT_NINE)) {
                            i--;
                        }
                        else if (buf[i] == LCBRACKET && (p - i - 3) > 0) {
                            try {
                                literalLength = StringArrays.parseInt(buf, i + 1, p - i - 3);
                                break;
                            } catch (NumberFormatException e) { }
                        }
                        else {
                            // Not actually a literal
                            break;
                        }
                    }
                }

                trimCount = 2;
            }
            else {
                trimCount = 1;
            }

            p++;

            if (literalLength >= 0) {
                if ((len - p) >= literalLength) {
                    p += literalLength;
                    literalLength = -1;
                }
                else {
                    literalLength -= (len - p);
                    lastLength = len;

                    return -1;
                }
            }
            else {
                lastLength = 0;
                literalLength = -1;

                return p;
            }

            p = indexOfLinefeedIgnoringQuoted(buf, p, len);
        }

        lastLength = len;

        return -1;
    }

    private int indexOfLinefeedIgnoringQuoted(byte[] array, int fromIndex, int toIndex) {
        if(fromIndex >= array.length) {
            return -1;
        }
        
        for(int i = fromIndex; i<toIndex; i++) {
            if(inQuoted) {
                if(array[i] == BACKSLASH && !inQuotedEscape) {
                    inQuotedEscape = true;
                }
                else if(inQuotedEscape) {
                    inQuotedEscape = false;
                }
                else if(array[i] == DQUOTE) {
                    inQuoted = false;
                }
            }
            else {
                if(array[i] == DQUOTE) {
                    inQuoted = true;
                }
                else if(array[i] == LF) {
                    return i;
                }
            }
        }
        return -1;
    }
    
    
    public int trimCount() {
        return trimCount;
    }

    public String logString(byte[] result) {
        return new String(result);
    }
}
