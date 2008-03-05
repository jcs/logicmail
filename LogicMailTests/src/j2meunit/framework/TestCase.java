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
 * A test case defines the fixture to run multiple tests. To define a test case
 *
 * <ol>
 *   <li>implement a subclass of TestCase</li>
 *   <li>define instance variables that store the state of the fixture</li>
 *   <li>initialize the fixture state by overriding <code>setUp</code></li>
 *   <li>clean-up after a test by overriding <code>tearDown</code>.</li>
 * </ol>
 *
 * <p>Each test runs in its own fixture so there can be no side effects among
 * test runs. Here is an example:</p>
 *
 * <pre>
   public class MathTest extends TestCase
   {
       protected int nValue1;
       protected int nValue2;

       protected void setUp()
       {
           nValue1= 2;
           nValue2= 3;
       }
   }
 * </pre>
 *
 * For each test implement a method which interacts with the fixture. Verify the
 * expected results with assertions specified by calling <code>assertTrue</code>
 * (or another assert method) with a boolean:
 *
 * <pre>
       public void testAdd()
       {
           double result= nValue1 + nValue2;
           assertTrue(result == 5);
       }
 * </pre>
 *
 * Once the methods are defined you can run them. The framework supports both a
 * static type safe and more dynamic way to run a test. In the static way you
 * override the runTest method and define the method to be invoked. A convenient
 * way to do so is with an anonymous inner class:
 *
 * <pre>
   TestCase test= new MathTest("add")
   {
       public void runTest()
       {
           testAdd();
       }
   };
   test.run();
 * </pre>
 *
 * In JUnit, the dynamic way uses reflection in the default implementation of
 * <code>runTest</code>. Because reflection is not available in J2ME, here a
 * different approach is necessary. Together with a method name, an instance of
 * the class TestMethod can be given to the constructor of TestCase. It can be
 * created as an anonymous inner class that invokes the actual test method in
 * the implementation of the
 *
 * <pre>run</pre>
 *
 * method:
 *
 * <pre>
   TestCase test = new MathTest("testAdd", new TestMethod()
   { public void run(TestCase tc) { ((MathTest) tc).testAdd(); } });
   test.run();
 * </pre>
 *
 * To make this work subclasses need to implement the additional constructor and
 * forward the parameters to the corresponding TestCase constructor. Multiple
 * tests to be run can be grouped into a TestSuite. J2MEUnit provides different
 * <i>test runners</i> which can run a test suite and collect the results.
 * Because J2ME doesn't provide reflection, a test suite must always be created
 * manually from test instances associated with TestMethods. The missing
 * reflection is also the reason that the
 *
 * <pre>suite</pre>
 *
 * method cannot be static in J2MEUnit:
 *
 * <pre>
   public Test suite()
   {
       TestSuite suite = new TestSuite();
       {
       suite.addTest(new MathTest("testAdd", new TestMethod()
       { public void run(TestCase tc) {((MathTest) tc).testAdd(); } }));
       suite.addTest(new MathTest("testDivideByZero", new TestMethod()
       { public void run(TestCase tc) {((MathTest) tc).testDivideByZero(); } }));

       return suite;
   }
 * </pre>
 *
 * @see TestResult
 * @see TestSuite
 * @see TestMethod
 */
public abstract class TestCase extends Assert implements Test
{
	//~ Instance fields --------------------------------------------------------

	private String     sName	   = null;
	private TestMethod rTestMethod = null;
	private TestResult rResult     = null;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Default constructor.
	 */
	public TestCase()
	{
	}

	/***************************************
	 * Constructor for a named Test.
	 *
	 * @param name The name of the test (method)
	 */
	public TestCase(String name)
	{
		sName = name;
	}

	/***************************************
	 * Constructor for a test that will execute a particular test method.
	 *
	 * @param name       The name of the test method
	 * @param testMethod The TestMethod wrapper for the method to execute
	 */
	public TestCase(String name, TestMethod testMethod)
	{
		setTestMethod(name, testMethod);
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Always return 1.
	 *
	 * @return 1
	 */
	public int countTestCases()
	{
		return 1;
	}

	/***************************************
	 * Returns the same as countTestCases.
	 *
	 * @see j2meunit.framework.Test#countTestSteps()
	 */
	public int countTestSteps()
	{
		return countTestCases();
	}

	/***************************************
	 * Returns the test name.
	 *
	 * @return The test name
	 *
	 * @since  1.1
	 */
	public String getName()
	{
		return sName;
	}

	/***************************************
	 * Returns the Method to be executed by the test case instance.
	 *
	 * @return A TestMethod instance
	 */
	public TestMethod getTestMethod()
	{
		return rTestMethod;
	}

	/***************************************
	 * Deprecated, replaced by getName().
	 *
	 * @return     The test instance name
	 *
	 * @deprecated Replaced by getName()
	 */
	public String getTestMethodName()
	{
		return getName();
	}

	/***************************************
	 * A convenience method to run this test, collecting the results with a
	 * default TestResult object.
	 *
	 * @see TestResult
	 */
	public TestResult run()
	{
		TestResult result = createResult();

		run(result);

		return result;
	}

	/***************************************
	 * Runs the test case and collects the results in TestResult.
	 *
	 * @param result The TestResult to collect the data in
	 */
	public void run(TestResult result)
	{
		rResult = result;
		result.run(this);
	}

	/***************************************
	 * Runs the bare test sequence.
	 *
	 * @exception Throwable if any exception is thrown
	 */
	public void runBare() throws Throwable
	{
		setUp();

		try
		{
			runTest();
		}
		finally
		{
			tearDown();
		}
	}

	/***************************************
	 * To set the test name.
	 *
	 * @param name The new test name
	 *
	 * @since 1.1
	 */
	public void setName(String name)
	{
		sName = name;
	}

	/***************************************
	 * To set the method to be executed by the test case instance.
	 *
	 * @param testMethod The TestMethod to execute
	 */
	public void setTestMethod(TestMethod testMethod)
	{
		this.rTestMethod = testMethod;
	}

	/***************************************
	 * Convenience method to set the name and wrapper of the method to be
	 * executed by the test case instance.
	 *
	 * @param methodName The name of the test methoto execute
	 * @param testMethod The method wrapper
	 */
	public void setTestMethod(String methodName, TestMethod testMethod)
	{
		setName(methodName);
		setTestMethod(testMethod);
	}

	/***************************************
	 * Deprecated, replaced by setName(String).
	 *
	 * @param      name The test instance name
	 *
	 * @deprecated Replaced by setName(String)
	 */
	public void setTestMethodName(String name)
	{
		setName(name);
	}

	/***************************************
	 * This method should be overridden if a test case contains multiple test
	 * methods. It must return a TestSuite containing the test methods to be
	 * executed for this test case. This TestSuite can be constructed as below:
	 *
	 * <pre>
	   new TestSuite(new MyTestCase("testMethodOne", new TestMethod()
	   {   public void run(TestCase tc)
	       { ((MyTestCase) tc).testMethodOne(); }
	   }));
	 * </pre>
	 *
	 * @return A new test suite
	 */
	public Test suite()
	{
		return null;
	}

	/***************************************
	 * Returns a string representation of the test case.
	 *
	 * @return The name of the test case or, if not set, the class name
	 */
	public String toString()
	{
		if (sName != null)
		{
			return sName + "(" + getClass().getName() + ")";
		}
		else
		{
			return getClass().getName();
		}
	}

	/***************************************
	 * Creates a default TestResult object
	 *
	 * @see TestResult
	 */
	protected TestResult createResult()
	{
		return new TestResult();
	}

	/***************************************
	 * Callback from the Assert base class that will be invoked on assertions.
	 *
	 * @see j2meunit.framework.Assert#onAssertion()
	 */
	protected void onAssertion()
	{
		rResult.assertionMade();
	}

	/***************************************
	 * The default implementation will run the TestMethod associated with the
	 * TestCase instance and asserts that it is not null. To use the version 1.0
	 * test case style override this method (but don't call super!) and invoke
	 * the method corresponding to the name of the TestCase instance. Since
	 * there is no reflection this must be done by a string compare and an
	 * explicit call of the necessary method.
	 *
	 * @exception Throwable if any exception is thrown
	 */
	protected void runTest() throws Throwable
	{
		assertNotNull(rTestMethod);
		rTestMethod.run(this);
	}

	/***************************************
	 * Sets up the fixture, for example, open a network connection. This method
	 * is called before a test is executed.
	 *
	 * @throws Exception An arbitrary exception may be thrown
	 */
	protected void setUp() throws Exception
	{
	}

	/***************************************
	 * Tears down the fixture, for example, close a network connection. This
	 * method is called after a test is executed.
	 *
	 * @throws Exception An arbitrary exception may be thrown
	 */
	protected void tearDown() throws Exception
	{
	}

	/***************************************
	 * Notifies listeners that a test step has finished.
	 */
	protected void testStepFinished()
	{
		rResult.endTestStep(this);
	}
}
