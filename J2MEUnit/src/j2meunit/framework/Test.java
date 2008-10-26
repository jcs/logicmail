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
 * A <em>Test</em> can be run and collect its results.
 *
 * @see TestResult
 */
public interface Test
{
	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Counts the number of test cases that will be run by this test.
	 *
	 * @return The number of test cases
	 */
	public abstract int countTestCases();

	/***************************************
	 * Counts the number of test steps that will be run by this test. Test steps
	 * are distinct parts of a test that shall be monitored separately. This can
	 * be used to show the progress of large tests that take a large amount of
	 * time (e.g. storage access or encryption). Each test step then needs to
	 * invoke TestCase.testStepFinished() after it ran successfully. 
	 * 
	 * For short tests this method should return the same value as
	 * countTestCases(). For long tests the return value should include the
	 * result of countTestCases() because completed tests are counted too. 
	 *
	 * @return The number of test steps
	 */
	public abstract int countTestSteps();

	/***************************************
	 * Runs a test and collects its result in a TestResult instance.
	 */
	public abstract void run(TestResult result);
}
