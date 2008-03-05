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

import java.util.*;


/********************************************************************
 * A <code>TestSuite</code> is a collection of Test instances which can be run
 * together. It is created by adding single tests (normally by using a
 * TestMethod) or other suites to it:
 * <pre>
 * TestSuite suite = new TestSuite();
 * suite.addTest(new MathTest("testAdd"));
 * suite.addTest(new MathTest("testDivideByZero"));
 * suite.addTest(new ValueTest().suite());
 * </pre>
 *
 * @author $author$
 * @version $Revision: 10 $
 */
public class TestSuite implements Test
{
	//~ Instance fields --------------------------------------------------------

	private String fName;
	private Vector fTests = new Vector(10);

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Default constructor.
	 */
	public TestSuite()
	{
	}

	/***************************************
	 * To create a test suite with a particular name.
	 *
	 * @param sName The name of the test suite.
	 */
	public TestSuite(String sName)
	{
		fName = sName;
	}

	/***************************************
	 * To create a test suite initialized with a single test.
	 *
	 * @param rTest The test to add to the suite
	 */
	public TestSuite(Test rTest)
	{
		addTest(rTest);
	}

	/***************************************
	 * To create a test suite initialized with multiple tests.
	 *
	 * @param rTests The tests to add to the suite
	 */
	public TestSuite(Test[] rTests)
	{
		for (int i = 0; i < rTests.length; i++)
			addTest(rTests[i]);
	}

	/***************************************
	 * Creates a new test suite for certain methods of a particular test case
	 * class. The methods are defined as strings that should contain the method 
	 * names. These names will be stored in the created TestCase instances as
	 * their names. Because J2ME doesn't provide reflection, the TestCase class
	 * must implement the runTest() method and invoke the corresponding methods
	 * by doing a string compare with the name attribute of the test case
	 * instance.
	 * <p>
	 * Since version 1.1 the recommended (and easier) method to create test
	 * instances and suites is to use the TestMethod interface to wrap the
	 * methods of a test case in an anonymous inner class, initialize a TestCase
	 * instance for each, and then hand the test(s) over to one of the
	 * constructors of TestSuite that accept Test instances.          
	 *
	 * @param theClass The Class instance of a TestCase subclass
	 * @param testNames The names of the methods to run 
	 */
	public TestSuite(Class theClass, String[] testNames)
	{
		this(theClass.getName());

		for (int i = 0; i < testNames.length; i++)
		{
			TestCase testCase = null;

			try
			{
				testCase = (TestCase) theClass.newInstance();
			}
			catch (Exception e)
			{
				String sMessage = "Need to have public default constructor in " +
								  theClass.getName();
				System.out.println(sMessage);
				throw new RuntimeException(sMessage);
			}

			testCase.setName(testNames[i]);
			addTest(testCase);
		}
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Adds a test to the suite.
	 *
	 * @param test The test to add
	 */
	public void addTest(Test test)
	{
		fTests.addElement(test);
	}

	/***************************************
	 * Counts the number of test cases that will be run by this suite.
	 *
	 * @return The number of test cases to be run by the suite
	 */
	public int countTestCases()
	{
		int count = 0;

		for (int i = 0, cnt = fTests.size(); i < cnt; i++)
			count += ((Test) fTests.elementAt(i)).countTestCases();

		return count;
	}

	/***************************************
	 * Counts the number of test steps that will be run by this suite.
	 *
	 * @return The number of test steps to be run by the suite
	 * 
	 * @see Test#countTestSteps() 
	 */
	public int countTestSteps()
	{
		int count = 0;

		for (int i = 0, cnt = fTests.size(); i < cnt; i++)
			count += ((Test) fTests.elementAt(i)).countTestSteps();

		return count;
	}

	/***************************************
	 * Runs the tests and collects their result in a TestResult.
	 *
	 * @param result The TestResult to collect the results in
	 */
	public void run(TestResult result)
	{
		for (Enumeration e = tests(); e.hasMoreElements();)
		{
			if (result.shouldStop())
				break;

			Test test = (Test) e.nextElement();
			test.run(result);
		}
	}

	/***************************************
	 * Returns the test at the given index.
	 *
	 * @param index The index position of the test
	 *
	 * @return The test at the index position
	 */
	public Test testAt(int index)
	{
		return (Test) fTests.elementAt(index);
	}

	/***************************************
	 * Returns the number of tests in this suite.
	 *
	 * @return The test count of the suite
	 */
	public int testCount()
	{
		return fTests.size();
	}

	/***************************************
	 * Returns the tests as an enumeration.
	 *
	 * @return A java.util.Enumeration for all tests
	 */
	public Enumeration tests()
	{
		return fTests.elements();
	}

	/***************************************
	 * Create a string description of the suite.
	 *
	 * @return A description of the suite
	 */
	public String toString()
	{
		if (fName != null)
			return fName;
		else

			return super.toString();
	}

// Commented to avoid warnings from unused code
//	/***************************************
//	 * Internal method to returns a test which will fail and log a warning
//	 * message.
//	 *
//	 * @param message The message to display
//	 *
//	 * @return A new TestCase instance
//	 */
//	private Test warning(final String message)
//	{
//		return new TestCase("warning")
//			{
//				protected void runTest()
//				{
//					fail(message);
//				}
//			};
//	}
}
