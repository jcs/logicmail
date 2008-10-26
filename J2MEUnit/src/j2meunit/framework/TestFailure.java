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
 * A <code>TestFailure</code> collects a failed test together with the caught
 * exception.
 *
 * @see TestResult
 */
public class TestFailure extends Object
{
	//~ Instance fields --------------------------------------------------------

	protected Test	    fFailedTest;
	protected Throwable fThrownException;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Constructs a TestFailure with the given test and exception.
	 *
	 * @param failedTest The failed test
	 * @param thrownException The causing exception
	 */
	public TestFailure(Test failedTest, Throwable thrownException)
	{
		fFailedTest		 = failedTest;
		fThrownException = thrownException;
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Gets the failed test.
	 *
	 * @return The failed test
	 */
	public Test failedTest()
	{
		return fFailedTest;
	}

	/***************************************
	 * Gets the thrown exception.
	 *
	 * @return The exception causing the failure
	 */
	public Throwable thrownException()
	{
		return fThrownException;
	}

	/***************************************
	 * Returns a short description of the failure.
	 *
	 * @return A string describing the failure
	 */
	public String toString()
	{
		StringBuffer buffer = new StringBuffer();
		buffer.append(fFailedTest + ": " + fThrownException.getMessage());

		return buffer.toString();
	}
}
