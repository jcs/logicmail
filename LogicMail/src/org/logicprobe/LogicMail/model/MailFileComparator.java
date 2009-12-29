/*-
 * Copyright (c) 2009, Derek Konigsberg
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

import net.rim.device.api.util.Comparator;

/**
 * Comparator used for sorting local mail cache files.
 * <p>
 * Sorting is based on the file name format, using the following
 * criteria:
 * <table border="1">
 * <tr><td>Format</td><td>Criteria</td></tr>
 * <tr><td><tt>NNNN.MM.msg</tt></td><td>Numerical sort on <tt>MM</tt></td></tr>
 * <tr><td><tt>NNNN.msg</tt></td><td>Numerical sort on <tt>NNNN</tt> as a hex number</td></tr>
 * <tr><td><tt>ABCDEFGHIJK.msg</td><td>String sorting</td></tr>
 * </table>
 * </p>
 */
class MailFileComparator implements Comparator {
    private static String MSG_SUFFIX = ".msg";
    
    public int compare(Object o1, Object o2) {
        // Get the strings and clean up the casing
        String str1 = o1.toString().toLowerCase();
        String str2 = o2.toString().toLowerCase();

        // Strip the suffixes and make sure we're lower-case
        int p1 = str1.indexOf(MSG_SUFFIX);
        int p2 = str2.indexOf(MSG_SUFFIX);
        if(p1 <= 0 || p2 <= 0) { return 0; }
        str1 = str1.substring(0, p1);
        str2 = str2.substring(0, p2);

        try {
            if(isNumeric(str1) && isNumeric(str2)) {
                int q1 = str1.indexOf('.');
                int q2 = str2.indexOf('.');
                if(q1 != -1 && q2 != -1) {
                    str1 = str1.substring(q1 + 1);
                    str2 = str2.substring(q2 + 1);
                }

                long num1 = Long.parseLong(str1, 16);
                long num2 = Long.parseLong(str2, 16);
                if(num1 < num2) { return -1; }
                else if(num1 > num2) { return 1; }
                else { return 0; }
            }
            else {
                return str1.compareTo(str2);
            }
        } catch (Exception e) {
            return str1.compareTo(str2);
        }
    }

    private static boolean isNumeric(String str) {
        boolean hasPeriod = false;
        char[] chars = str.toCharArray();
        for(int i=0; i<chars.length; i++) {
            if(chars[i] == '.') {
                if(hasPeriod) { return false; }
                else { hasPeriod = true; }
            }
            else if(!(chars[i] >= '0' && chars[i] <= '9')
                    && !(chars[i] >= 'a' && chars[i] <= 'f')) {
                return false;
            }
        }
        
        return true;
    }
}
