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
 * A <code>TestResult</code> collects the results of executing a test case. It
 * is an instance of the Collecting Parameter pattern. The test framework
 * distinguishes between <i>failures</i> and <i>errors</i>. A failure is
 * anticipated and checked for with assertions. Errors are unanticipated
 * problems like an <code>ArrayIndexOutOfBoundsException</code>.
 *
 * @see Test
 */
public class TestResult
{
	//~ Instance fields --------------------------------------------------------

	protected Vector fErrors;
	protected Vector fFailures;
	protected Vector fListeners;
	protected int    fAssertions;
	protected int    fRunTests;
	private boolean  fStop;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new TestResult object.
	 */
	public TestResult()
	{
		fFailures  = new Vector();
		fErrors    = new Vector();
		fListeners = new Vector();
		fRunTests  = 0;
		fStop	   = false;
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Adds an error to the list of errors. The passed in exception caused the
	 * error.
	 *
	 * @param test -
	 * @param t -
	 */
	public void addError(Test test, Throwable t)
	{
		fErrors.addElement(new TestFailure(test, t));

		for (int i = 0, cnt = fListeners.size(); i < cnt; i++)
			((TestListener) fListeners.elementAt(i)).addError(test, t);
	}

	/***************************************
	 * Adds a failure to the list of failures. The passed in exception caused
	 * the failure.
	 *
	 * @param test -
	 * @param t -
	 */
	public synchronized void addFailure(Test test, AssertionFailedError t)
	{
		fFailures.addElement(new TestFailure(test, t));

		for (int i = 0, cnt = fListeners.size(); i < cnt; i++)
			((TestListener) fListeners.elementAt(i)).addFailure(test, t);
	}

	/***************************************
	 * Registers a TestListener
	 *
	 * @param listener The listener
	 */
	public synchronized void addListener(TestListener listener)
	{
		fListeners.addElement(listener);
	}

	/***************************************
	 * Gets the number of detected failures.
	 *
	 * @return -
	 */
	public synchronized int assertionCount()
	{
		return fAssertions;
	}

	/***************************************
	 * Adds a failure to the list of failures. The passed in exception caused
	 * the failure.
	 */
	public synchronized void assertionMade()
	{
		fAssertions++;
	}

	/***************************************
	 * Informs the result that a test was completed.
	 *
	 * @param test -
	 */
	public void endTest(Test test)
	{
		for (int i = 0, cnt = fListeners.size(); i < cnt; i++)
			((TestListener) fListeners.elementAt(i)).endTest(test);
	}

	/***************************************
	 * Informs the result that a test was completed.
	 *
	 * @param test -
	 */
	public void endTestStep(Test test)
	{
		for (int i = 0, cnt = fListeners.size(); i < cnt; i++)
			((TestListener) fListeners.elementAt(i)).endTestStep(test);
	}

	/***************************************
	 * Gets the number of detected errors.
	 *
	 * @return -
	 */
	public synchronized int errorCount()
	{
		return fErrors.size();
	}

	/***************************************
	 * Returns an Enumeration for the errors
	 *
	 * @return -
	 */
	public synchronized Enumeration errors()
	{
		return fErrors.elements();
	}

	/***************************************
	 * Gets the number of detected failures.
	 *
	 * @return -
	 */
	public synchronized int failureCount()
	{
		return fFailures.size();
	}

	/***************************************
	 * Returns an Enumeration for the failures
	 *
	 * @return -
	 */
	public synchronized Enumeration failures()
	{
		return fFailures.elements();
	}

	/***************************************
	 * Gets the number of run tests.
	 *
	 * @return -
	 */
	public synchronized int runCount()
	{
		return fRunTests;
	}

	/***************************************
	 * Runs a TestCase.
	 *
	 * @param test -
	 * @param p -
	 */
	public void runProtected(final Test test, Protectable p)
	{
		try
		{
			p.protect();
		}
		catch (AssertionFailedError e)
		{
			addFailure(test, e);
		}
		catch (Throwable e)
		{
			addError(test, e);
		}
	}

	/***************************************
	 * Gets the number of run tests.
	 *
	 * @return -
	 *
	 * @deprecated use <code>runCount</code> instead
	 */
	public synchronized int runTests()
	{
		return runCount();
	}

	/***************************************
	 * Checks whether the test run should stop
	 *
	 * @return -
	 */
	public synchronized boolean shouldStop()
	{
		return fStop;
	}

	/***************************************
	 * Informs the result that a test will be started.
	 *
	 * @param test -
	 */
	public void startTest(Test test)
	{
		synchronized (this)
		{
			fRunTests++;
		}

		for (int i = 0, cnt = fListeners.size(); i < cnt; i++)
			((TestListener) fListeners.elementAt(i)).startTest(test);
	}

	/***************************************
	 * Marks that the test run should stop.
	 */
	public synchronized void stop()
	{
		fStop = true;
	}

	/***************************************
	 * Gets the number of detected errors.
	 *
	 * @return -
	 *
	 * @deprecated use <code>errorCount</code> instead
	 */
	public synchronized int testErrors()
	{
		return errorCount();
	}

	/***************************************
	 * Gets the number of detected failures.
	 *
	 * @return -
	 *
	 * @deprecated use <code>failureCount</code> instead
	 */
	public synchronized int testFailures()
	{
		return failureCount();
	}

	/***************************************
	 * Returns whether the entire test was successful or not.
	 *
	 * @return -
	 */
	public synchronized boolean wasSuccessful()
	{
		return (testFailures() == 0) && (testErrors() == 0);
	}

	/***************************************
	 * Runs a TestCase.
	 *
	 * @param test -
	 */
	protected void run(final TestCase test)
	{
		startTest(test);

		Protectable p = new Protectable()
		{
			public void protect() throws Throwable
			{
				test.runBare();
			}
		};

		runProtected(test, p);
		endTest(test);
	}
}
