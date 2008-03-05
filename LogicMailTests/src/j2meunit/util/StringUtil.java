//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is part of J2MEUnit, a Java 2 Micro Edition unit testing framework.
//
// J2MEUnit is free software distributed under the Common Public License (CPL).
// It may be redistributed and/or modified under the terms of the CPL. You 
// should have received a copy of the license along with J2MEUnit. It is also 
// available from the website of the Open Source Initiative at 
// http://www.opensource.org.
//
// J2MEUnit is distributed in the hope that it will be useful, but WITHOUT ANY 
// WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
// FOR A PARTICULAR PURPOSE.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
package j2meunit.util;

/********************************************************************
 * A class with junit related String utilities.
 */
public class StringUtil
{
	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new StringUtil object.
	 */
	protected StringUtil()
	{
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Assumes the argument to be a decimal number of milliseconds and formats 
	 * it into a string.    
	 *
	 * @param nTime The time value to convert 
	 *
	 * @return A string containing the converted value 
	 */
	public static String elapsedTimeAsString(long nTime)
	{
		return nTime + "ms";
	}

	/***************************************
	 * Truncates a string to a maximum length.
	 *
	 * @param s The string to truncate
	 * @param length The maximum length of the string
	 *
	 * @return If the string is longer than length, the truncated string, else
	 *         the original string 
	 */
	public static String truncate(String s, int length)
	{
		length -= 3;
		
		if (s.length() > length)
			s = s.substring(0, length) + "...";

		return s;
	}
}
