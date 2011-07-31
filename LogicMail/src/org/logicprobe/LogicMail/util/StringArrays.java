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
 * Provides utility methods that operate on <code>byte</code> arrays, with
 * similar functionality to common <code>String</code> methods.
 * <p>
 * Some method comments and specifications were copied from
 * <code>java.lang.String</code>.
 * </p>
 */
public class StringArrays {
    private StringArrays() {
    }
    
    /**
     * Returns a hash code based on the contents of the specified array.
     *
     * @param array the array representing the string.
     * @param offset the offset of the data within the array
     * @param length the length of the data within the array
     * @return a content-based hash code.
     */
    public static int hashCode(byte[] array) {
        return hashCode(array, 0, array.length);
    }
    
    /**
     * Returns a hash code based on the contents of the specified array.
     *
     * @param array the array representing the string.
     * @param offset the offset of the data within the array
     * @param length the length of the data within the array
     * @return a content-based hash code.
     */
    public static int hashCode(byte[] array, int offset, int length) {
        if (array == null || array.length == 0) {
            return 0;
        }
        if(offset + length > array.length) {
            throw new ArrayIndexOutOfBoundsException(offset+length);
        }

        int result = 1;
        for (int i=0; i<length; i++)
            result = 31 * result + array[offset + i];

        return result;
    }
    
    /**
     * Returns the index within this string of the first occurrence of the
     * specified substring, starting at the specified index.
     *
     * @param array the array representing the string.
     * @param str the substring to search for.
     * @param fromIndex the index to start the search from.
     * @return If the string argument occurs as a substring within this object
     *   at a starting index no smaller than <code>fromIndex</code>, then the
     *   index of the first character of the first such substring is returned.
     *   If it does not occur as a substring starting at <code>fromIndex</code>
     *   or beyond, <code>-1</code> is returned.
     * @throws NullPointerException if <code>str</code> is <code>null</code>
     */
    public static int indexOf(byte[] array, byte[] str, int fromIndex) {
        return indexOf(array, str, fromIndex, array.length, false);
    }
    
    /**
     * Returns the index within this string of the first occurrence of the
     * specified substring, starting at the specified index.
     *
     * @param array the array representing the string.
     * @param str the substring to search for.
     * @param fromIndex the index to start the search from.
     * @param arrayLength the length of the data in the array that is valid for
     *   the search
     * @param ignoreCase true, if the search should be case insensitive
     * @return If the string argument occurs as a substring within this object
     *   at a starting index no smaller than <code>fromIndex</code>, then the
     *   index of the first character of the first such substring is returned.
     *   If it does not occur as a substring starting at <code>fromIndex</code>
     *   or beyond, <code>-1</code> is returned.
     * @throws NullPointerException if <code>str</code> is <code>null</code>
     */
    public static int indexOf(byte[] array, byte[] str, int fromIndex, int arrayLength, boolean ignoreCase) {
        if(str == null) {
            throw new NullPointerException();
        }
        if(fromIndex + str.length > arrayLength) {
            return -1;
        }
        
        int max = arrayLength - str.length;

        if(ignoreCase) {
            for (int i = fromIndex; i <= max; i++) {
                if (!equalsIgnoreCase(array[i], str[0])) {
                    while (++i <= max && !equalsIgnoreCase(array[i], str[0]));
                }

                if (i <= max) {
                    int j = i + 1;
                    int end = j + str.length - 1;
                    for (int k = 1; j < end && equalsIgnoreCase(array[j], str[k]); j++, k++);

                    if (j == end) {
                        return i;
                    }
                }
            }
        }
        else {
            for (int i = fromIndex; i <= max; i++) {
                if (array[i] != str[0]) {
                    while (++i <= max && array[i] != str[0]);
                }

                if (i <= max) {
                    int j = i + 1;
                    int end = j + str.length - 1;
                    for (int k = 1; j < end && array[j] == str[k]; j++, k++);

                    if (j == end) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }
    
    private static boolean equalsIgnoreCase(byte ch1, byte ch2) {
        if(CharacterUtilities.isLetter((char)ch1) && CharacterUtilities.isLetter((char)ch2)) {
            return CharacterUtilities.toUpperCase((char)ch1) == CharacterUtilities.toUpperCase((char)ch2);
        }
        else {
            return ch1 == ch2;
        }
    }
    
    /**
     * Returns the index within this array of the first occurrence of the
     * specified element, starting at the specified index.
     * 
     * @param array the array representing the string.
     * @param ch the element to search for.
     * @param fromIndex the index to start the search from.
     * @return the index the element was found, or <code>-1</code> if it was
     *   not found.
     */
    public static int indexOf(byte[] array, byte ch, int fromIndex) {
        if(fromIndex >= array.length) {
            return -1;
        }
        
        for(int i = fromIndex; i<array.length; i++) {
            if(array[i] == ch) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Tests if this string starts with the specified prefix.
     *
     * @param array the array representing the string.
     * @param prefix the prefix.
     * @return <code>true</code> if the character sequence represented by the
     *   argument is a prefix of the character sequence represented by this
     *   string; <code>false</code> otherwise.  Note also that <code>true</code>
     *   will be returned if the argument is an empty string or is equal to this
     *   <code>byte</code> array.
     * @throws NullPointerException if <code>prefix</code> is <code>null</code>. 
     */
    public static boolean startsWith(byte[] array, byte[] prefix) {
        if(prefix == null) {
            throw new NullPointerException();
        }
        
        if(array.length < prefix.length) {
            return false;
        }
        else {
            for(int i=0; i<prefix.length; i++) {
                if(array[i] != prefix[i]) {
                    return false;
                }
            }
            return true;
        }
    }
    
    /**
     * Tests if this string starts with the specified prefix beginning a
     * specified index.
     *
     * @param array the array representing the string.
     * @param prefix the prefix.
     * @param toffset where to begin looking in the string.
     * @return <code>true</code> if the character sequence represented by the
     *   argument is a prefix of the substring of this object starting at index
     *   <code>toffset</code>; <code>false</code> otherwise. 
     */
    public static boolean startsWith(byte[] array, byte[] prefix, int toffset) {
        if(prefix == null) {
            throw new NullPointerException();
        }
        
        if(toffset + prefix.length > array.length) {
            return false;
        }
        else {
            for(int i=0; i<prefix.length; i++) {
                if(array[toffset + i] != prefix[i]) {
                    return false;
                }
            }
            return true;
        }
    }
    
    public static int parseInt(byte[] array, int offset, int length) throws NumberFormatException {
        if (array == null || array.length == 0) {
            throw new NumberFormatException();
        }
        if(offset + length > array.length) {
            throw new ArrayIndexOutOfBoundsException(offset+length);
        }
        
        int num = 0;
        for(int i=0; i<length; i++) {
            int digit = Character.digit((char)array[offset + i], 10);
            if(digit < 0) {
                throw new NumberFormatException();
            }
            num = (num * 10) + digit;
        }
        return num;
    }
    
    public static int parseHexInt(byte[] array, int offset, int length) throws NumberFormatException {
        if (array == null || array.length == 0) {
            throw new NumberFormatException();
        }
        if(offset + length > array.length) {
            throw new ArrayIndexOutOfBoundsException(offset+length);
        }
        
        int num = 0;
        for(int i=0; i<length; i++) {
            int digit = Character.digit((char)array[offset + i], 16);
            if(digit < 0) {
                throw new NumberFormatException();
            }
            num = (num * 16) + digit;
        }
        return num;
    }
}
