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

/*****************************************************************************
 * A Listener for test progress
 */
public interface TestListener
{
	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * An error occurred.
	 */
	public void addError(Test test, Throwable t);

	/***************************************
	 * A failure occurred.
	 */
	public void addFailure(Test test, AssertionFailedError e);

	/***************************************
	 * A test ended.
	 */
	public void endTest(Test test);

	/***************************************
	 * A test step ended.
	 */
	public void endTestStep(Test test);

	/***************************************
	 * A test started.
	 */
	public void startTest(Test test);
}
