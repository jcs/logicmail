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
 * This class contains the current version number of J2MEUnit.
 */
public class Version
{
	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new Version object.
	 */
	private Version()
	{
		// don't instantiate
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Returns the current version of J2MEUnit.
	 *
	 * @return A string containing the version ID
	 */
	public static String id()
	{
		return "1.1.1";
	}
}
