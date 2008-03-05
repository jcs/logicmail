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
 * A set of assert methods.
 */
public class Assert
{
	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new Assert object.
	 */
	public Assert()
	{
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Asserts that two longs are equal.
	 *
	 * @param expected the expected value of an object
	 * @param actual the actual value of an object
	 */
	public void assertEquals(long expected, long actual)
	{
		assertEquals(null, expected, actual);
	}

	/***************************************
	 * Asserts that two objects are equal. If they are not an
	 * AssertionFailedError is thrown.
	 *
	 * @param expected the expected value of an object
	 * @param actual the actual value of an object
	 */
	public void assertEquals(Object expected, Object actual)
	{
		assertEquals(null, expected, actual);
	}

	/***************************************
	 * Asserts that two longs are equal.
	 *
	 * @param message the detail message for this assertion
	 * @param expected the expected value of an object
	 * @param actual the actual value of an object
	 */
	public void assertEquals(String message, long expected, long actual)
	{
		assertEquals(message, new Long(expected), new Long(actual));
	}

	/***************************************
	 * Asserts that two objects are equal. If they are not an
	 * AssertionFailedError is thrown.
	 *
	 * @param message The detail message for this assertion
	 * @param expected The expected value of an object
	 * @param actual The actual value of an object
	 */
	public void assertEquals(String message, Object expected, Object actual)
	{
		onAssertion();

		if ((expected == null) && (actual == null))
			return;

		if ((expected != null) && expected.equals(actual))
			return;

		failNotEquals(message, expected, actual);
	}

	/***************************************
	 * Asserts that an object isn't null.
	 *
	 * @param object The object to test
	 */
	public void assertNotNull(Object object)
	{
		assertNotNull(null, object);
	}

	/***************************************
	 * Asserts that an object isn't null.
	 *
	 * @param message The detail message for this assertion
	 * @param object The object to test
	 */
	public void assertNotNull(String message, Object object)
	{
		assertTrue(message, object != null);
	}

	/***************************************
	 * Asserts that an object is null.
	 *
	 * @param object The object to test
	 */
	public void assertNull(Object object)
	{
		assertNull(null, object);
	}

	/***************************************
	 * Asserts that an object is null.
	 *
	 * @param message The detail message for this assertion
	 * @param object The object to test
	 */
	public void assertNull(String message, Object object)
	{
		assertTrue(message, object == null);
	}

	/***************************************
	 * Asserts that two objects refer to the same object. If they are not the
	 * same an AssertionFailedError is thrown.
	 *
	 * @param expected The expected value of an object
	 * @param actual The actual value of an object
	 */
	public void assertSame(Object expected, Object actual)
	{
		assertSame(null, expected, actual);
	}

	/***************************************
	 * Asserts that two objects refer to the same object. If they are not an
	 * AssertionFailedError is thrown.
	 *
	 * @param message The detail message for this assertion
	 * @param expected The expected value of an object
	 * @param actual The actual value of an object
	 */
	public void assertSame(String message, Object expected, Object actual)
	{
		onAssertion();

		if (expected == actual)
			return;

		failNotSame(message, expected, actual);
	}

	/***************************************
	 * Asserts that a condition is true. If it isn't it throws an
	 * AssertionFailedError with the given message.
	 *
	 * @param message The detail message for this assertion
	 * @param condition The boolean value to test
	 */
	public void assertTrue(String message, boolean condition)
	{
		onAssertion();

		if (!condition)
			fail(message);
	}

	/***************************************
	 * Asserts that a condition is true. If it isn't it throws an
	 * AssertionFailedError.
	 *
	 * @param condition The boolean value to test
	 */
	public void assertTrue(boolean condition)
	{
		assertTrue(null, condition);
	}

	/***************************************
	 * Fails a test with no message.
	 */
	public void fail()
	{
		fail(null);
	}

	/***************************************
	 * Fails a test with the given error message.
	 *
	 * @param message The message to display
	 *
	 * @throws AssertionFailedError Containing the error message
	 */
	public void fail(String message)
	{
		throw new AssertionFailedError(message);
	}

	/***************************************
	 * Fails an "assertEquals" test with the given error message.
	 *
	 * @param message The message to display
	 * @param expected The expected value
	 * @param actual The actual value
	 */
	protected void failNotEquals(String message, Object expected, Object actual)
	{
		String formatted = "";

		if (message != null)
			formatted = message + " ";

		fail(formatted + "expected:<" + expected + "> but was:<" + actual +
			 ">");
	}

	/***************************************
	 * Fails an "assertSame" test with the given error message.
	 *
	 * @param message The message to display
	 * @param expected The expected value
	 * @param actual The actual value
	 */
	protected void failNotSame(String message, Object expected, Object actual)
	{
		String formatted = "";

		if (message != null)
			formatted = message + " ";

		fail(formatted + "expected same");
	}

	/***************************************
	 * This method is called every time an assertion is made (this does not
	 * include direct fails).
	 */
	protected void onAssertion()
	{
	}
}
