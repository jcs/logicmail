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

import net.rim.device.api.util.CharacterUtilities;

/**
 * Simple URL encoder utility class.
 * Assumes all input strings are confined to ASCII characters, and thus may
 * not correctly handle Unicode characters.
 */
public class URLEncoder {
    
    public static String encode(String string) {
        StringBuffer buf = new StringBuffer();
        int len = string.length();
        for(int i=0; i<len; i++) {
            char ch = string.charAt(i);
            if(CharacterUtilities.isLetter(ch)
                    || CharacterUtilities.isDigit(ch)
                    || isUnconvertedSpecial(ch)) {
                buf.append(ch);
            }
            else {
                buf.append('%');
                String hex = Integer.toHexString((byte)ch).toUpperCase();
                if(hex.length() == 1) {
                    buf.append('0');
                }
                buf.append(hex);
            }
        }
        
        return buf.toString();
    }
    
    private static boolean isUnconvertedSpecial(char ch) {
        return ch == '.' || ch == '-' || ch == '*' || ch == '_';
    }

    public static String decode(String string) {
        StringBuffer buf = new StringBuffer();
        int len = string.length();
        for(int i=0; i<len; i++) {
            char ch = string.charAt(i);
            if(ch == '%' && i + 2 < len
                    && isHexDigit(string.charAt(i + 1)) && isHexDigit(string.charAt(i + 2))) {
                int decodedChar = Integer.parseInt(string.substring(i + 1, i + 3), 16);
                buf.append((char)decodedChar);
                i += 2;
            }
            else {
                buf.append(ch);
            }
        }
        
        return buf.toString();
    }

    private static boolean isHexDigit(char ch) {
        return (ch >= '0' && ch <= '9')
            || (ch >= 'a' && ch <= 'f')
            || (ch >= 'A' && ch <= 'F');
    }
}
