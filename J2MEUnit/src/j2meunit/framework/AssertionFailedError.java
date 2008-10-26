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

package j2meunit.framework;

/********************************************************************
 * Thrown when an assertion failed.
 */
public class AssertionFailedError extends Error
{
	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new AssertionFailedError object.
	 */
	public AssertionFailedError()
	{
	}

	/***************************************
	 * Creates a new AssertionFailedError object.
	 *
	 * @param message The error message
	 */
	public AssertionFailedError(String message)
	{
		super(message);
	}
}
