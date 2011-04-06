/*
 * IHammockExceptionErrors.java
 *  
 * Copyright 2007 C.A. Meijer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hammingweight.hammock;

/**
 * This interface contains text messages describing the most common problems
 * when invoking methods on a test double or verifying that a mock object was
 * invoked as expected.
 * 
 * @author C.A. Meijer
 * @since Hammock 1.0
 */
public interface IHammockExceptionErrors {
	/**
	 * A message stating that an invocation handler cannot evaluate some method
	 * invoked on a test double.
	 */
	public static final String METHOD_CANNOT_BE_EVALUATED = "A method was "
			+ "unexpectedly invoked.\nHint: Check the method, the expected "
			+ "number of invocations, the method arguments and the mock "
			+ "object.";

	/**
	 * An argument passed as an exception is not a subclass of Throwable.
	 */
	public static final String CLASS_IS_NOT_THROWABLE = "The exceptions thrown "
			+ "by a method must subclass Throwable.";

	/**
	 * The Hammock framework requires that some methods can only be invoked on
	 * objects that implement the <code>IMockObject</code> interface.
	 */
	public static final String DOES_NOT_SUBCLASS_MOCK_OBJECT = "The class does "
			+ "not implement the IMockObject interface.";

	/**
	 * It is a mistake to invoke a method on an instance of AMockObject if no
	 * invocation handler has been set.
	 */
	public static final String HANDLER_NOT_SET = "An invocation handler has not "
			+ "been set for this mock object.";

	/**
	 * If a method on a mock object is expected to return a primitive value, it
	 * is a mistake if the method tries to return <code>null</code>.
	 */
	public static final String NO_RETURN_VALUE = "This mock object tried to return "
			+ "a null value instead of a primitive value.";

	/**
	 * A return value for a method handler can only be set once, it does not
	 * make sense to specify two or more values that should be returned when the
	 * method is invoked.
	 */
	public static final String RETURN_VALUE_ALREADY_SET = "A return value has "
			+ "already been set for the mock handler.";

	/**
	 * If a method is of type <code>void</void>, it doesn't make sense to set a value
	 * that should be returned when the method is invoked.
	 */
	public static final String CANT_SET_VOID_RETURN_VALUE = "You can't set the return "
			+ "value for method of type void.";

	/**
	 * When setting the return value for a mocked method, the return value must
	 * be an instance of the class that is returned by the method.
	 */
	public static final String INCORRECT_RETURN_CLASS = "The return value is "
			+ "inconsistent with the method's signature.";

	/**
	 * When setting an exception to be thrown by a mock object, the exception
	 * must be in accordance with the method's signature.
	 */
	public static final String INCORRECT_THROWABLE_CLASS = "The exception is "
			+ "inconsistent with the method's signature.";

	/**
	 * When setting an expectation for a method invocation, the number of
	 * arguments specified in the expectation must agree with the number of
	 * arguments in the method's signature.
	 */
	public static final String WRONG_NUMBER_OF_ARGS = "The number of arguments "
			+ "is inconsistent with the method's signature.";

	/**
	 * When setting an expectation for a method invocation, the class of the
	 * arguments must agree with the method's signature.
	 */
	public static final String WRONG_ARGUMENT_CLASS = "An argument is "
			+ "inconsistent with the method signature.";

	/**
	 * For a non-void method, one must specify a return value when setting an
	 * expectation that a method will be invoked.
	 */
	public static final String NO_RETURN_VALUE_SET = "A return value was not "
			+ "supplied for a mocked method.";

	/**
	 * When verifying the behaviour of an object under test, it must invoke mock
	 * objects a certain number of times to satisfy the expectations.
	 */
	public static final String METHOD_INVOKED_UNEXPECTED_NUMBER_OF_TIMES = "A method "
			+ "was invoked less often than expected.";

	/**
	 * When verifying the behaviour of an object under test, it must invoke mock
	 * objects a certain number of times to satisfy the expectations.
	 * 
	 * @since Hammock 2.0
	 */
	public static final String METHOD_INVOKED_TOO_MANY_TIMES = "A method "
			+ "was invoked more often than expected.";

	/**
	 * For a strict mock object, methods must be invoked in the same order as
	 * specified in the expectations.
	 */
	public static final String METHOD_INVOKED_OUT_OF_SEQUENCE = "A method was "
			+ "invoked out of sequence.";

	/**
	 * An expectation was set that some method would be invoked on a mock object
	 * where the method is not applicable for the mock object's class.
	 */
	public static final String INCORRECT_MOCK_OBJECT = "The mock object is "
			+ "inconsistent with the method's signature.";

	/**
	 * The expected number of invocations can only be set once for a handler.
	 */
	public static final String NUM_INVOCATIONS_ALREADY_SET = "The expected number "
			+ "of invocations has already been set.";

	/**
	 * An argument matcher can only be set once for a particular argument for a
	 * method.
	 * 
	 * @since Hammock 1.4
	 */
	public static final String ARGUMENT_MATCHER_ALREADY_SET = "An argument matcher"
			+ "has already been set for a method handler argument.";
}
