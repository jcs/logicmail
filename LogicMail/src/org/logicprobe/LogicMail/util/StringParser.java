/*
 * StringParser.java
 *
 * Created on July 11, 2006, 10:44 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.logicprobe.LogicMail.util;

import java.util.Date;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.Vector;

/**
 * This class provides a collection of string parsing
 * utilities that are generally useful for handling
 * E-Mail protocol server responses.
 */
public class StringParser {
    public StringParser() { }
    
    /**
     * Parse a string containing a date/time
     * and return a usable Date object.
     * @param rawDate Text containing the date
     * @return Date object instance
     */
    public static Date parseDateString(String rawDate) {
        int p = 0;
        int q = 0;
        
        // Clean up the date string for simple parsing
        p = rawDate.indexOf(",");
        if(p != -1) {
            p++;
            while(rawDate.charAt(p)==' ') p++;
            rawDate = rawDate.substring(p);
        }
        if(rawDate.charAt(rawDate.length()-1) == ')')
            rawDate = rawDate.substring(0, rawDate.lastIndexOf(' '));

        // Set the time zone
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"+rawDate.lastIndexOf(' ')+1));

        p = 0;
        q = rawDate.indexOf(" ", p+1);
        cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(rawDate.substring(p, q)));

        p = q+1;
        q = rawDate.indexOf(" ", p+1);
        String monthStr = rawDate.substring(p, q);
        if(monthStr.equals("Jan")) cal.set(Calendar.MONTH, Calendar.JANUARY);
        else if(monthStr.equals("Feb")) cal.set(Calendar.MONTH, Calendar.FEBRUARY);
        else if(monthStr.equals("Mar")) cal.set(Calendar.MONTH, Calendar.MARCH);
        else if(monthStr.equals("Apr")) cal.set(Calendar.MONTH, Calendar.APRIL);
        else if(monthStr.equals("May")) cal.set(Calendar.MONTH, Calendar.MAY);
        else if(monthStr.equals("Jun")) cal.set(Calendar.MONTH, Calendar.JUNE);
        else if(monthStr.equals("Jul")) cal.set(Calendar.MONTH, Calendar.JULY);
        else if(monthStr.equals("Aug")) cal.set(Calendar.MONTH, Calendar.AUGUST);
        else if(monthStr.equals("Sep")) cal.set(Calendar.MONTH, Calendar.SEPTEMBER);
        else if(monthStr.equals("Oct")) cal.set(Calendar.MONTH, Calendar.OCTOBER);
        else if(monthStr.equals("Nov")) cal.set(Calendar.MONTH, Calendar.NOVEMBER);
        else if(monthStr.equals("Dec")) cal.set(Calendar.MONTH, Calendar.DECEMBER);
        
        p = q+1;
        q = rawDate.indexOf(" ", p+1);
        cal.set(Calendar.YEAR, Integer.parseInt(rawDate.substring(p, q)));
        
        p = q+1;
        q = rawDate.indexOf(":", p+1);
        cal.set(Calendar.HOUR, Integer.parseInt(rawDate.substring(p, q)));
        
        p = q+1;
        q = rawDate.indexOf(":", p+1);
        cal.set(Calendar.MINUTE, Integer.parseInt(rawDate.substring(p, q)));

        p = q+1;
        q = rawDate.indexOf(" ", p+1);
        cal.set(Calendar.SECOND, Integer.parseInt(rawDate.substring(p, q)));
        cal.set(Calendar.MILLISECOND, 0);
        
        return cal.getTime();
    }
    
    /**
     * Recursively parse a nested paren string.
     * Parses through a string of the form "(A (B C (D) E F))" and
     * returns a tree of Vector and String objects representing its
     * contents.  This is useful for parsing the response to the
     * IMAP "ENVELOPE" fetch command.
     * @param rawText The raw text to be parsed
     * @return A tree of Vector and String objects
     */
    public static Vector parseNestedParenString(String rawText) {
	Vector parsedText = new Vector();
	// Sanity checking
	if(!(rawText.charAt(0) == '(' &&
	     rawText.charAt(rawText.length()-1) == ')')) {
             return null;
        }

	int p = 1;
	int q = p;
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
	    else if(rawText.charAt(q) == ' ' && !inQuote) {
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
		for(int i=q+1;i<rawText.length();i++) {
		    if(rawText.charAt(i) == '(') level++;
		    else if(rawText.charAt(i) == ')') {
			if(level == 0) {
			    q = i;
			    break;
			}
			else
			    level--;
		    }
		}

		if(q == 1 || q<p) {
		    return null;
		}
		else {
		    parsedText.addElement(parseNestedParenString(rawText.substring(p, q+1)));
		}
		p = q+1;
	    }
	    q++;
	}
	return parsedText;
    }

}
