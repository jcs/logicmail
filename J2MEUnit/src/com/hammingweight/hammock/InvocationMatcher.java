/*
 * InvocationMatcher.java
 *  
 * Copyright 2009 C.A. Meijer
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
 * The <code>InvocationMatcher</code> class is a helper class for
 * verifying that expected and actual method invocations are equal.
 * 
 * @author C.A. Meijer
 * @since Hammock 2.0
 */
public class InvocationMatcher implements IHammockExceptionErrors {

	// The method to be matched.
	private MockMethod mockMethod;

	// The (optional) expected arguments.
	private Object[] expectedArgs;

	// An optional Object associated with the arguments.
	private IMockObject mockObject;

	// The argument matchers
	private IArgumentMatcher[] argMatchers;

	/**
	 * Constructor.
	 * 
	 * @param mockMethod
	 *            A method to be matched.
	 */
	public InvocationMatcher(MockMethod mockMethod) {
		this(mockMethod, new Object[mockMethod.getNumberOfArguments()]);
		for (int i = 0; i < mockMethod.getNumberOfArguments(); i++) {
			ignoreArgument(i);
		}
	}

	public InvocationMatcher(MockMethod mockMethod, IMockObject mockObject) {
		this(mockMethod);
		this.mockObject = mockObject;
	} 
	/**
	 * Constructor.
	 * 
	 * @param mockMethod
	 *            A method to be matched.
	 * 
	 * @param expectedArgs
	 *            The arguments that are expected to be passed to the method.
	 */
	public InvocationMatcher(MockMethod mockMethod, Object[] expectedArgs) {
		if (mockMethod == null) {
			throw new NullPointerException();
		}
		this.mockMethod = mockMethod;
		mockMethod.validateArguments(expectedArgs);
		this.expectedArgs = expectedArgs;
		this.argMatchers = new IArgumentMatcher[expectedArgs.length];
	}

	/**
	 * Constructor.
	 * 
	 * @param mockMethod
	 *            A method to be matched.
	 * 
	 * @param mockObject
	 *            The object on which the method is invoked.
	 * 
	 * @param expectedArgs
	 *            The arguments that are expected to be passed to the method.
	 */
	public InvocationMatcher(MockMethod mockMethod,
			IMockObject mockObject, Object[] expectedArgs) {
		this(mockMethod, expectedArgs);
		this.mockObject = mockObject;
	}

	/**
	 * Verifies that the actual arguments passed to a method are as expected
	 * (i.e. as specified in the constructor of this class).
	 * 
	 * @param methodInvocation
	 *            The method invocation of interest.
	 * 
	 * @return <code>true</code> if the methodInvocation matches the
	 *         expectation; else <code>false</code>.
	 */
	public boolean isMatch(MethodInvocation methodInvocation) {
		if (!this.mockMethod.equals(methodInvocation.getMethod())) {
			return false;
		}

		if ((this.mockObject != null)
				&& (!this.mockObject.equals(methodInvocation.getMockObject()))) {
			return false;
		}

		Object[] actualArgs = methodInvocation.getMethodArguments();
		for (int i = 0; i < actualArgs.length; i++) {
			IArgumentMatcher argMatcher = this.argMatchers[i];
			if (argMatcher == null) {
				argMatcher = new DefaultArgumentMatcher();
			}
			if (!argMatcher.areArgumentsEqual(this.expectedArgs[i],
					actualArgs[i])) {
				return false;
			}
		}

		return true;
	}

	/**
	 * A method to set an argument matcher for a specific argument index.
	 * 
	 * @param argIndex
	 *            The argument index of interest.
	 * 
	 * @param argMatcher
	 *            The matcher to use for the specified index.
	 */
	public InvocationMatcher setArgumentMatcher(int argIndex,
			IArgumentMatcher argMatcher) {
		if (argIndex < 0) {
			throw new IllegalArgumentException();
		}

		if (argIndex >= this.argMatchers.length) {
			throw new HammockException(this.mockMethod, WRONG_NUMBER_OF_ARGS);
		}

		if (argMatcher == null) {
			throw new NullPointerException();
		}

		if (this.argMatchers[argIndex] != null) {
			throw new HammockException(this.mockMethod,
					ARGUMENT_MATCHER_ALREADY_SET);
		}
		this.argMatchers[argIndex] = argMatcher;
		return this;
	}

	/**
	 * A method to specify that we must ignore a particular argument when
	 * verifying whether expected and actual arguments are equal.
	 * 
	 * @param argIndex
	 *            The argument index to ignore.
	 */
	public InvocationMatcher ignoreArgument(int argIndex) {
		return setArgumentMatcher(argIndex, new PromiscuousArgumentMatcher());
	}

	/**
	 * A method to determine the method that is being validated.
	 * 
	 * @return The method that we expect to be invoked.
	 */
	public MockMethod getMethod() {
		return this.mockMethod;
	}
}
