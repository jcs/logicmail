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
 * Wrapper interface for single test methods to alleviate the lack of reflection 
 * in J2ME. Adding test methods to test suites can be done by wrapping each
 * single method in the run() method of an anonymous implementation of this
 * interface.
 *
 * @author eso
 */
public interface TestMethod
{
	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * This is the method that needs to be implemented to invoke the actual test 
	 * method of the TestCase class. This method should always invoke only a 
	 * single test method (i.e. with a name starting with "test") on the given 
	 * test case, and this should be exactly the method which has been defined 
	 * by name when invoking the constructor.
	 */
	public void run(TestCase rTestCase) throws Throwable;
}
