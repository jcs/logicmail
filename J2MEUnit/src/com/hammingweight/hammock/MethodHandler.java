/*
 * MethodHandler.java
 *  
 * Copyright 2007-2008 C.A. Meijer
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
 * The MethodHandler class creates a handler for processing a method invocation.
 * The handler can be configured to respond more than once to a method
 * invocation but will always respond identically. This class is used to set
 * expectations for mock objects.
 * 
 * @author C.A. Meijer
 * @since Hammock 1.0
 */
public class MethodHandler implements IHammockExceptionErrors,
		IClassDefinitions {

	/**
	 * The argument validator used to check that the method was invoked with the
	 * expected arguments.
	 */
	private InvocationMatcher matcher;

	/**
	 * A flag indicating whether a return value has been configured for the
	 * handler.
	 */
	private boolean returnValueSet;

	/**
	 * The value to be returned when the method is invoked.
	 */
	private Object retVal;

	/**
	 * An exception to be thrown when the method is invoked.
	 */
	private Throwable throwable;

	/**
	 * The minimum number of times that this method handler expects to handle a
	 * method invocation.
	 */
	private int minNumInvocations = 1;

	/**
	 * The maximum number of times that this method handler expects to handle a
	 * method invocation.
	 */
	private int maxNumInvocations = 1;

	/**
	 * A flag indicating whether the number of invocations has been set.
	 */
	private boolean numInvocationsSet;

	/**
	 * The number of times that this handler has been invoked.
	 */
	private int numInvocations;

	/**
	 * The length of time that this handler should take to respond to a method
	 * invocation.
	 */
	private long delayInMs;

	// A utility method to throw a HammockException.
	private void throwException(String reason) {
		throw new HammockException(this.matcher.getMethod(), reason);
	}

	/**
	 * Constructor.
	 * 
	 * @param mockMethod
	 *            The method that this handler will respond to.
	 */
	public MethodHandler(MockMethod mockMethod) {
		this.matcher = new InvocationMatcher(mockMethod);
	}

	/**
	 * Constructor.
	 * 
	 * @param mockMethod
	 *            The method that this handler responds to.
	 * 
	 * @param args
	 *            The expected arguments.
	 */
	public MethodHandler(MockMethod mockMethod, Object[] args) {
		this.matcher = new InvocationMatcher(mockMethod, args);
	}

	/**
	 * Constructor.
	 * 
	 * @param mi
	 *            The method invocation (specifying a method, a mock object and
	 *            arguments) that this handler will respond to.
	 * 
	 * @since Hammock 1.6.
	 */
	public MethodHandler(MethodInvocation mi) {
		this(mi.getMethod(), mi.getMockObject(), mi.getMethodArguments());
	}

	/**
	 * Constructor.
	 * 
	 * @param mockMethod
	 *            The method that this handler responds to.
	 * 
	 * @param mockObject
	 *            The object on which the method is invoked.
	 * 
	 * @since Hammock 2.0
	 */
	public MethodHandler(MockMethod mockMethod, IMockObject mockObject) {
		this.matcher = new InvocationMatcher(mockMethod, mockObject);
	}

	/**
	 * Constructor.
	 * 
	 * @param mockMethod
	 *            The method that this handler responds to.
	 * 
	 * @param mockObject
	 *            The object on which the method is invoked.
	 * 
	 * @param args
	 *            The expected arguments.
	 */
	public MethodHandler(MockMethod mockMethod, IMockObject mockObject,
			Object[] args) {
		this.matcher = new InvocationMatcher(mockMethod, mockObject,
				args);
	}

	/**
	 * Sets the number of times that we expect a method to be invoked.
	 * 
	 * @param expectedNumInvocations
	 *            The expected number of invocations.
	 * 
	 * @return <code>this</code>.
	 * 
	 * @since Hammock 1.6
	 */
	public MethodHandler setInvocationCount(int expectedNumInvocations) {
		return setInvocationCount(expectedNumInvocations,
				expectedNumInvocations);
	}

	/**
	 * Sets upper and lower limits for the number of times that we expect a
	 * method to be invoked.
	 * 
	 * @param minNumber
	 *            A lower expected limit.
	 * @param maxNumber
	 *            An upper expected limit.
	 * @return <code>this</code>.
	 * 
	 * @since Hammock 1.6
	 */
	public MethodHandler setInvocationCount(int minNumber, int maxNumber) {
		if (this.numInvocationsSet) {
			throwException(NUM_INVOCATIONS_ALREADY_SET);
		}

		if ((minNumber < 0) || (minNumber > maxNumber)) {
			throw new IllegalArgumentException();
		}

		this.minNumInvocations = minNumber;
		this.maxNumInvocations = maxNumber;

		this.numInvocationsSet = true;
		return this;
	}

	/**
	 * Sets the value to be returned if a method is invoked.
	 * 
	 * @param retVal
	 *            The value to be returned when the method is invoked. Primitive
	 *            values must be wrapped by the appropriate wrapper class (e.g.
	 *            an <code>int</code> must be wrapped by an <code>Integer</code>
	 *            .
	 * 
	 * @return <code>this</code>.
	 */
	public MethodHandler setReturnValue(Object retVal) {
		if (this.returnValueSet) {
			throwException(RETURN_VALUE_ALREADY_SET);
		}

		Class retClass = this.matcher.getMethod().getReturnClass();
		if (retClass == null) {
			throwException(CANT_SET_VOID_RETURN_VALUE);
		}

		if ((retVal != null) && (!retClass.isAssignableFrom(retVal.getClass()))) {
			throwException(INCORRECT_RETURN_CLASS);
		}

		this.retVal = retVal;
		this.returnValueSet = true;
		return this;
	}

	/**
	 * Sets an exception to be thrown when the method is invoked.
	 * 
	 * @param t
	 *            The exception (or error) to throw,
	 * 
	 * @return <code>this</code>.
	 */
	public MethodHandler setThrowable(Throwable t) {
		if (this.returnValueSet) {
			throwException(RETURN_VALUE_ALREADY_SET);
		}

		this.matcher.getMethod().validateThrowable(t);
		
		this.throwable = t;
		this.returnValueSet = true;
		return this;
	}

	/**
	 * A method that verifies that the handler was invoked as expected.
	 */
	public void verify() {
		if (this.numInvocations < this.minNumInvocations) {
			throwException(METHOD_INVOKED_UNEXPECTED_NUMBER_OF_TIMES);
		}

	}

	/**
	 * A method that invokes the handler. The handler sets the return value or
	 * exception to throw.
	 * 
	 * @param mi
	 *            The invoked method.
	 */
	public void invoke(MethodInvocation mi) {
		if (this.numInvocations == this.maxNumInvocations) {
			return;
		}

		if (!this.matcher.isMatch(mi)) {
			return;
		}

		if ((this.matcher.getMethod().getReturnClass() != null)
				&& (!this.returnValueSet)) {
			throwException(NO_RETURN_VALUE_SET);
		}

		this.numInvocationsSet = true;
		try {
			Thread.sleep(this.delayInMs);
		} catch (InterruptedException e) {

		}
		this.numInvocations++;

		if (this.throwable != null) {
			mi.setThrowable(this.throwable);
			return;
		}

		mi.setReturnValue(this.retVal);
	}

	/**
	 * A method to configure a delay before this handler responds to a method
	 * invocation.
	 * 
	 * @param delayInMs
	 *            The delay in milliseconds.
	 * 
	 * @return <code>this</code>.
	 */
	public MethodHandler setDelay(long delayInMs) {
		this.delayInMs = delayInMs;
		return this;
	}

	/**
	 * A method to set an argument that should be ignored when determining
	 * whether this handler responds to a particular method invocation.
	 * 
	 * @param argIndex
	 *            The argument number (indexed from zero) that can be ignored.
	 * 
	 * @return <code>this</code>.
	 */
	public MethodHandler ignoreArgument(int argIndex) {
		return setArgumentMatcher(argIndex, new PromiscuousArgumentMatcher());
	}

	/**
	 * This method adds an argument matcher to be added to a method handler so
	 * that two arguments, <code>object1</code> and <code>object2</code> can be
	 * determined to be equal even if <code>object1.equals(object2)</code>
	 * returns false.
	 * 
	 * @param argIndex
	 *            The index of the argument in the method's signature.
	 * 
	 * @param argMatcher
	 *            A class that can determine whether two objects are equal.
	 * 
	 * @return <code>this</code>
	 * @since Hammock 1.4.
	 */
	public MethodHandler setArgumentMatcher(int argIndex,
			IArgumentMatcher argMatcher) {
		this.matcher.setArgumentMatcher(argIndex, argMatcher);
		return this;
	}
}
