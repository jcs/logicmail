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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.TimeZone;
import java.util.Vector;
import net.rim.device.api.util.Arrays;
import net.rim.device.api.util.DateTimeUtilities;
import net.rim.device.api.util.NumberUtilities;

/**
 * This class provides a collection of string parsing
 * utilities that are generally useful for handling
 * E-Mail protocol server responses.
 */
public class StringParser {
    private StringParser() { }
    
    /**
     * Parse a string containing a date/time
     * and return a usable Date object.
     *
     * @param rawDate Text containing the date
     * @return Date object instance
     */
    public static Date parseDateString(String rawDate) {
        int p = 0;
        int q = 0;
        
        int[] fields = new int[7];
        // Clean up the date string for simple parsing
        p = rawDate.indexOf(",");
        if(p != -1) {
            p++;
            while(rawDate.charAt(p)==' ') p++;
            rawDate = rawDate.substring(p);
        }
        if(rawDate.charAt(rawDate.length()-1) == ')') {
            rawDate = rawDate.substring(0, rawDate.lastIndexOf(' '));
        }

        // Set the time zone
        Calendar cal;
        String tz = rawDate.substring(rawDate.lastIndexOf(' ')+1);
        cal = Calendar.getInstance(createTimeZone(tz));

        p = 0;
        q = rawDate.indexOf(" ", p+1);
        fields[2] = Integer.parseInt(rawDate.substring(p, q));
        
        p = q+1;
        q = rawDate.indexOf(" ", p+1);
        String monthStr = rawDate.substring(p, q);
        if(monthStr.equals("Jan")) { fields[1]=0; }
        else if(monthStr.equals("Feb")) { fields[1]=1; }
        else if(monthStr.equals("Mar")) { fields[1]=2; }
        else if(monthStr.equals("Apr")) { fields[1]=3; }
        else if(monthStr.equals("May")) { fields[1]=4; }
        else if(monthStr.equals("Jun")) { fields[1]=5; }
        else if(monthStr.equals("Jul")) { fields[1]=6; }
        else if(monthStr.equals("Aug")) { fields[1]=7; }
        else if(monthStr.equals("Sep")) { fields[1]=8; }
        else if(monthStr.equals("Oct")) { fields[1]=9; }
        else if(monthStr.equals("Nov")) { fields[1]=10; }
        else if(monthStr.equals("Dec")) { fields[1]=11; }
        
        p = q+1;
        q = rawDate.indexOf(" ", p+1);
        fields[0]=Integer.parseInt(rawDate.substring(p, q));
        
        p = q+1;
        q = rawDate.indexOf(":", p+1);
        fields[3]=Integer.parseInt(rawDate.substring(p, q));
        
        p = q+1;
        q = rawDate.indexOf(":", p+1);
        if(q == -1)
        {
            // The second field is missing, so handle accordingly
            q = rawDate.indexOf(" ", p+1);
            fields[4] = Integer.parseInt(rawDate.substring(p, q));
            fields[5] = 0;
            fields[6] = 0;
        }
        else
        {
            // Otherwise parse minutes and seconds as normal
            fields[4] = Integer.parseInt(rawDate.substring(p, q));

            p = q+1;
            q = rawDate.indexOf(" ", p+1);
            fields[5] = Integer.parseInt(rawDate.substring(p, q));
            fields[6] = 0;
        }

        DateTimeUtilities.setCalendarFields(cal, fields);
        return cal.getTime();
    }
    
    /**
     * Create a TimeZone object from an input string.
     *
     * @param tz Input string
     * @return TimeZone object
     */
    private static TimeZone createTimeZone(String tz) {
        TimeZone result;
        if(tz.startsWith("-") || tz.startsWith("+")) {
            result = TimeZone.getTimeZone("GMT"+tz);
        }
        else if(tz.indexOf('/') != -1) { result = TimeZone.getTimeZone(tz); }
        else if(tz.equals("MIT")) { result = TimeZone.getTimeZone("GMT-11:00"); }
        else if(tz.equals("HST")) { result = TimeZone.getTimeZone("GMT-10:00"); }
        else if(tz.equals("AST")) { result = TimeZone.getTimeZone("GMT-9:00"); }
        else if(tz.equals("PST")) { result = TimeZone.getTimeZone("GMT-8:00"); }
        else if(tz.equals("PDT")) { result = TimeZone.getTimeZone("GMT-7:00"); }
        else if(tz.equals("PST8PDT")) { result = TimeZone.getTimeZone("GMT-8:00"); }
        else if(tz.equals("MST")) { result = TimeZone.getTimeZone("GMT-7:00"); }
        else if(tz.equals("MDT")) { result = TimeZone.getTimeZone("GMT-6:00"); }
        else if(tz.equals("MST7MDT")) { result = TimeZone.getTimeZone("GMT-7:00"); }
        else if(tz.equals("PNT")) { result = TimeZone.getTimeZone("GMT-7:00"); }
        else if(tz.equals("CST")) { result = TimeZone.getTimeZone("GMT-6:00"); }
        else if(tz.equals("CDT")) { result = TimeZone.getTimeZone("GMT-5:00"); }
        else if(tz.equals("CST6CDT")) { result = TimeZone.getTimeZone("GMT-6:00"); }
        else if(tz.equals("EST")) { result = TimeZone.getTimeZone("GMT-5:00"); }
        else if(tz.equals("EDT")) { result = TimeZone.getTimeZone("GMT-4:00"); }
        else if(tz.equals("EST5EDT")) { result = TimeZone.getTimeZone("GMT-5:00"); }
        else if(tz.equals("IET")) { result = TimeZone.getTimeZone("GMT-5:00"); }
        else if(tz.equals("PRT")) { result = TimeZone.getTimeZone("GMT-4:00"); }
        else if(tz.equals("CNT")) { result = TimeZone.getTimeZone("GMT-3:5"); }
        else if(tz.equals("AGT")) { result = TimeZone.getTimeZone("GMT-3:00"); }
        else if(tz.equals("BET")) { result = TimeZone.getTimeZone("GMT-3:00"); }
        else if(tz.equals("UCT")) { result = TimeZone.getTimeZone("GMT"); }
        else if(tz.equals("UTC")) { result = TimeZone.getTimeZone("GMT"); }
        else if(tz.equals("WET")) { result = TimeZone.getTimeZone("GMT"); }
        else if(tz.equals("CET")) { result = TimeZone.getTimeZone("GMT+1:00"); }
        else if(tz.equals("ECT")) { result = TimeZone.getTimeZone("GMT+1:00"); }
        else if(tz.equals("MET")) { result = TimeZone.getTimeZone("GMT+1:00"); }
        else if(tz.equals("ART")) { result = TimeZone.getTimeZone("GMT+2:00"); }
        else if(tz.equals("CAT")) { result = TimeZone.getTimeZone("GMT+2:00"); }
        else if(tz.equals("EET")) { result = TimeZone.getTimeZone("GMT+2:00"); }
        else if(tz.equals("EAT")) { result = TimeZone.getTimeZone("GMT+3:00"); }
        else if(tz.equals("NET")) { result = TimeZone.getTimeZone("GMT+4:00"); }
        else if(tz.equals("PLT")) { result = TimeZone.getTimeZone("GMT+5:00"); }
        else if(tz.equals("IST")) { result = TimeZone.getTimeZone("GMT+5:30"); }
        else if(tz.equals("BST")) { result = TimeZone.getTimeZone("GMT+6:00"); }
        else if(tz.equals("VST")) { result = TimeZone.getTimeZone("GMT+7:00"); }
        else if(tz.equals("CTT")) { result = TimeZone.getTimeZone("GMT+8:00"); }
        else if(tz.equals("PRC")) { result = TimeZone.getTimeZone("GMT+8:00"); }
        else if(tz.equals("JST")) { result = TimeZone.getTimeZone("GMT+9:00"); }
        else if(tz.equals("ROK")) { result = TimeZone.getTimeZone("GMT+9:00"); }
        else if(tz.equals("ACT")) { result = TimeZone.getTimeZone("GMT+9:30"); }
        else if(tz.equals("AET")) { result = TimeZone.getTimeZone("GMT+10:00"); }
        else if(tz.equals("SST")) { result = TimeZone.getTimeZone("GMT+11:00"); }
        else if(tz.equals("NST")) { result = TimeZone.getTimeZone("GMT+12:00"); }
        else { result = TimeZone.getTimeZone(tz); }
        return result;
    }
    
    /**
     * Create a proper E-Mail date string from a Java.util.Date object
     *
     * @param time Date to convert
     * @return String to add to an E-Mail
     */
    public static String createDateString(Date time) {
        return createDateString(time, TimeZone.getDefault());
    }
    
    /**
     * Create a proper E-Mail date string from a Java.util.Date object
     *
     * @param time Date to convert
     * @param zone Time zone for the date
     * @return String to add to an E-Mail
     */
    public static String createDateString(Date time, TimeZone zone) {
        Calendar cal = Calendar.getInstance(zone);
        cal.setTime(time);
        StringBuffer buf = new StringBuffer();

        switch(cal.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.SUNDAY:
                buf.append("Sun, ");
                break;
            case Calendar.MONDAY:
                buf.append("Mon, ");
                break;
            case Calendar.TUESDAY:
                buf.append("Tue, ");
                break;
            case Calendar.WEDNESDAY:
                buf.append("Wed, ");
                break;
            case Calendar.THURSDAY:
                buf.append("Thu, ");
                break;
            case Calendar.FRIDAY:
                buf.append("Fri, ");
                break;
            case Calendar.SATURDAY:
                buf.append("Sat, ");
                break;
        }

        buf.append(cal.get(Calendar.DAY_OF_MONTH));
        buf.append(' ');
        
        switch(cal.get(Calendar.MONTH)) {
            case Calendar.JANUARY:
                buf.append("Jan ");
                break;
            case Calendar.FEBRUARY:
                buf.append("Feb ");
                break;
            case Calendar.MARCH:
                buf.append("Mar ");
                break;
            case Calendar.APRIL:
                buf.append("Apr ");
                break;
            case Calendar.MAY:
                buf.append("May ");
                break;
            case Calendar.JUNE:
                buf.append("Jun ");
                break;
            case Calendar.JULY:
                buf.append("Jul ");
                break;
            case Calendar.AUGUST:
                buf.append("Aug ");
                break;
            case Calendar.SEPTEMBER:
                buf.append("Sep ");
                break;
            case Calendar.OCTOBER:
                buf.append("Oct ");
                break;
            case Calendar.NOVEMBER:
                buf.append("Nov ");
                break;
            case Calendar.DECEMBER:
                buf.append("Dec ");
                break;
        }
        
        buf.append(cal.get(Calendar.YEAR));
        buf.append(' ');
        
        buf.append(NumberUtilities.toString(cal.get(Calendar.HOUR_OF_DAY), 10, 2));
        buf.append(':');
        buf.append(NumberUtilities.toString(cal.get(Calendar.MINUTE), 10, 2));
        buf.append(':');
        buf.append(NumberUtilities.toString(cal.get(Calendar.SECOND), 10, 2));
        
        buf.append(' ');
        
        int tzOffset = (cal.getTimeZone().getRawOffset())/36000;
        if(tzOffset < 0) {
            buf.append(NumberUtilities.toString(tzOffset, 10, 5));
        }
        else {
            buf.append("+");
            buf.append(NumberUtilities.toString(tzOffset, 10, 4));
        }
        
        return buf.toString();
    }
    
    /**
     * Recursively parse a nested paren string.
     * Parses through a string of the form "(A (B C (D) E F))" and
     * returns a tree of Vector and String objects representing its
     * contents.  This is useful for parsing the response to the
     * IMAP "ENVELOPE" fetch command.
     *
     * @param rawText The raw text to be parsed
     * @return A tree of Vector and String objects
     */
    public static Vector nestedParenStringLexer(String rawText) {
        Vector parsedText = new Vector();
	// Sanity checking
	if(!(rawText.charAt(0) == '(' &&
	     rawText.charAt(rawText.length()-1) == ')')) {
             return null;
        }

	int p = 1;
	int q = p;
        int len;
        String tmpText;
	boolean inQuote = false;
        while(q < rawText.length()) {
	    if(rawText.charAt(q) == '\"') {
                if(!inQuote) {
		    inQuote = true;
		    p = q;
		}
		else {
		    parsedText.addElement(rawText.substring(p+1, q));
		    p = q+1;
		    inQuote = false;
		}
	    }
            else if(rawText.charAt(q) == '{' && !inQuote) {
                p = rawText.indexOf('}', q);
                len = Integer.parseInt(rawText.substring(q+1, p));
                p++;
                while(rawText.charAt(p) == '\r' || rawText.charAt(p) == '\n')
                    p++;

                // Quick kludge for length miscalculation due to the way
                // line breaks are currently handled
                tmpText = rawText.substring(p, p+len);
                if(tmpText.endsWith(" NIL")) {
                    len-=4;
                }
                else if(tmpText.endsWith(" NI")) {
                    len-=3;
                }
                else if(tmpText.endsWith(" N")) {
                    len-=2;
                }
                else if(tmpText.endsWith(" ")) {
                    len-=1;
                }

                parsedText.addElement(rawText.substring(p, p+len));
                q = p + len;
            }
	    else if((rawText.charAt(q) == ' ' && !inQuote) || (q == rawText.length() - 1)) {
		if(q-p > 0) {
                    parsedText.addElement(rawText.substring(p, q).trim());
		    p = q;
		}
		else {
		    p++;
		}
	    }
	    else if(rawText.charAt(q) == '(' && !inQuote) {
		p = q;
		// paren matching
		int level = 0;
                boolean subInQuote = false;
		for(int i=q+1;i<rawText.length();i++) {
                    if(rawText.charAt(i) == '{' && !subInQuote) {
                        int matchIndex = rawText.indexOf('}', i);
                        if(matchIndex > i+1 && rawText.charAt(matchIndex+1) != ' ') {
                            int matchLen = Integer.parseInt(rawText.substring(i+1, matchIndex));
                            matchIndex++;
                            while(rawText.charAt(matchIndex) == '\r' || rawText.charAt(matchIndex) == '\n') {
                                matchIndex++;
                            }
                            i = matchIndex + matchLen;
                        }
                    }
                    if(rawText.charAt(i) == '\"' && !subInQuote) {
                        subInQuote = true;
                    }
                    else if(rawText.charAt(i) == '\"' && subInQuote) {
                        subInQuote = false;
                    }
                    
		    if(rawText.charAt(i) == '(' && !subInQuote) {
                        level++;
                    }
		    else if(rawText.charAt(i) == ')' && !subInQuote) {
			if(level == 0) {
			    q = i;
			    break;
			}
			else {
			    level--;
                        }
		    }
		}

		if(q == 1 || q<p) {
		    return null;
		}
		else {
                    parsedText.addElement(nestedParenStringLexer(rawText.substring(p, q+1)));
		}
		p = q+1;
	    }
	    q++;
	}
	return parsedText;
    }

    /**
     * This method iterates through the raw lines that make up
     * standard E-Mail headers, and parses them out into hash
     * table entries that can be queried by other methods.
     * All header keys are stored in lower-case to ensure that
     * processing methods are case-insensitive.
     *
     * @param rawLines The raw header lines
     * @return Hash table providing the headers as key-value pairs
     */
    public static Hashtable parseMailHeaders(String[] rawLines) {
        String line = "";
        Hashtable table = new Hashtable();
        
        for(int i=0;i<rawLines.length;i++) {
            if(rawLines[i].startsWith(" ") || rawLines[i].startsWith("\t"))
                line = line + "\r\n" + rawLines[i];
            else {
                if(line.length() != 0) {
                    table.put((line.substring(0, line.indexOf(':'))).toLowerCase(),
                              line.substring(line.indexOf(' ') + 1));
                }
                line = rawLines[i];
            }
            
            // Special case for last item
            if(i == rawLines.length-1 && line.length() != 0) {
                table.put((line.substring(0, line.indexOf(':'))).toLowerCase(),
                          line.substring(line.indexOf(' ') + 1));
            }
        }
        return table;
    }
    
    public static String[] parseTokenString(String text, String token) {
        String[] tok = new String[0];
        if(text == null) {
            return tok;
        }
        int p = 0;
        int q = 0;
        while(q < text.length()) {
            q = text.indexOf(token, p+1);
            if(q == -1) {
                Arrays.add(tok, text.substring(p));
                break;
            }
            Arrays.add(tok, text.substring(p, q));
            p = q + token.length();
        }
        return tok;
    }
    
    /**
     * Parse an input string that contains an encoding type,
     * and return a valid encoding type supported by RIM.
     */
    public static String parseValidCharsetString(String encoding) {
        if(encoding == null)
            return "ISO-8859-1";
        else if(encoding.toUpperCase().indexOf("ISO-8859-1") != -1)
            return "ISO-8859-1";
        else if(encoding.toUpperCase().indexOf("UTF-8") != -1)
            return "UTF-8";
        else if(encoding.toUpperCase().indexOf("UTF-16BE") != -1)
            return "UTF-16BE";
        else if(encoding.toUpperCase().indexOf("US-ASCII") != -1)
            return "US-ASCII";
        else
            return "ISO-8859-1";
    }
    
    /**
     * Convert an array of lines into a usable InputStream
     */
    public static InputStream createInputStream(String[] rawLines) {
        StringBuffer buf = new StringBuffer();
        for(int i=0;i<rawLines.length;i++)
            buf.append(rawLines[i]  + "\r\n");
        return new ByteArrayInputStream(buf.toString().getBytes());        
    }
    
    /**
     * Read all available bytes from an InputStream
     */
    public static byte[] readWholeStream(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] tmpBuf;
        while(is.available() > 0) {
            tmpBuf = new byte[is.available()];
            if(is.read(tmpBuf) < 0) {
                break;
            }
            bos.write(tmpBuf);
        }
        return bos.toByteArray();
    }
    
    /**
     * Decode a quoted-printable string
     */
    public static String decodeQuotedPrintable(String text) {
        StringBuffer buffer = new StringBuffer();
        int index = 0;
        int length = text.length();
        while(index < length) {
            if(text.charAt(index) == '=') {
                if(index+2 >= length) {
                    break;
                }
                try {
                    int charVal = Integer.parseInt(text.substring(index+1, index+3), 16);
                    buffer.append((char)charVal);
                } catch (NumberFormatException exp) { }
                index += 3;
            }
            else {
                buffer.append(text.charAt(index));
                index++;
            }
        }
        return buffer.toString();
    }
    
    /**
     * Encode a quoted-printable string
     */
    public static String encodeQuotedPrintable(String text) {
        StringBuffer buffer = new StringBuffer();
        char ch;
        String charStr;
        for(int i=0; i<text.length(); i++) {
            ch = text.charAt(i);
            if(ch < 128) {
                buffer.append(ch);
            }
            else {
                charStr = Integer.toHexString((int)ch);
                buffer.append('=');
                if(charStr.length() == 1) {
                    buffer.append('0');
                }
                buffer.append(charStr);
            }
        }
        return buffer.toString();
    }
}
