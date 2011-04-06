/*
 * InvocationVerifier.java
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
 * The <code>InvocationVerifier</code> class is a helper class for
 * verifying that methods were invoked as expected; in particular it is used
 * with spy objects.
 * 
 * @author C.A. Meijer
 * @since Hammock 2.0
 */
public class InvocationVerifier implements IHammockExceptionErrors {

	// The matcher to use for validating method invocations.
	private InvocationMatcher matcher;

	// An array of method invocations that we want to validate with the matcher.
	private MethodInvocation[] methodInvocations;

	// The minimum number of times that a method should have been invoked.
	private int minNumInvocations = 1;

	// The maximum number of times that a method should have been invoked.
	private int maxNumInvocations = 1;

	// A flag indicating whether the number of invocations has been set.
	private boolean numInvocationsSet;

	/**
	 * Constructor.
	 * 
	 * @param matcher
	 *            The <code>MethodInvocationMatcher</code> to match actual
	 *            invocations against expected invocations.
	 * 
	 * @param methodInvocations
	 *            The methods invoked on a test double.
	 */
	public InvocationVerifier(InvocationMatcher matcher,
			MethodInvocation[] methodInvocations) {
		if ((matcher == null) || (methodInvocations == null)) {
			throw new NullPointerException();
		}
		this.matcher = matcher;
		this.methodInvocations = methodInvocations;
	}

	/**
	 * A method to check whether an expected method was invoked an expected
	 * number of times. Unless <code>setInvocationCount</code> has been called,
	 * this method will verify that the expected method has been invoked at
	 * least once.
	 * 
	 * @return <code>true</code> if an expected method was invoked, an expected
	 *         number of times.
	 */
	public boolean isVerified() {
		if (!this.numInvocationsSet) {
			this.setInvocationCount(1, Integer.MAX_VALUE);
		}
		int count = getInvocationCount();
		return ((count >= this.minNumInvocations) && (count <= this.maxNumInvocations));
	}

	/**
	 * A method to check whether an expected method was invoked an expected
	 * number of times. Unless <code>setInvocationCount</code> has been called,
	 * this method will verify that the expected method has been invoked at
	 * least once.
	 * 
	 * @throws HammockException
	 *             if the method wasn't invoked as expected.
	 */
	public void verify() {
		if (!this.numInvocationsSet) {
			this.setInvocationCount(1, Integer.MAX_VALUE);
		}

		int count = getInvocationCount();
		if (count < this.minNumInvocations) {
			throw new HammockException(this.matcher.getMethod(),
					METHOD_INVOKED_UNEXPECTED_NUMBER_OF_TIMES);
		} else if (count > this.maxNumInvocations) {
			throw new HammockException(this.matcher.getMethod(),
					METHOD_INVOKED_TOO_MANY_TIMES);
		}
	}

	/**
	 * A method to set the number of times that the expected method should have
	 * been called.
	 * 
	 * @param numInvocations
	 *            The exact number of times that the method should have been
	 *            invoked.
	 * 
	 * @return <code>this</code>.
	 */
	public InvocationVerifier setInvocationCount(int numInvocations) {
		return setInvocationCount(numInvocations, numInvocations);
	}

	/**
	 * A method to set the number of times that the expected method should have
	 * been called.
	 * 
	 * @param minNumInvocations
	 *            The minimum allowed number of invocations.
	 * 
	 * @param maxNumInvocations
	 *            The maximum allowed number of invocations.
	 * 
	 * @return <code>this</code>.
	 */
	public InvocationVerifier setInvocationCount(int minNumInvocations,
			int maxNumInvocations) {
		if (this.numInvocationsSet) {
			throw new HammockException(this.matcher.getMethod(),
					NUM_INVOCATIONS_ALREADY_SET);
		}

		if ((minNumInvocations < 0) || (minNumInvocations > maxNumInvocations)) {
			throw new IllegalArgumentException();
		}

		this.numInvocationsSet = true;
		this.minNumInvocations = minNumInvocations;
		this.maxNumInvocations = maxNumInvocations;
		return this;
	}

	/**
	 * Sets the <code>IArgumentMatcher</code> to use for matching method
	 * invocations.
	 * 
	 * @param argIndex
	 *            The index of the method argument.
	 * 
	 * @param argMatcher
	 *            The matcher to use.
	 * 
	 * @return <code>this</code>.
	 */
	public InvocationVerifier setArgumentMatcher(int argIndex,
			IArgumentMatcher argMatcher) {
		this.matcher.setArgumentMatcher(argIndex, argMatcher);
		return this;
	}

	/**
	 * Ignores a particular argument when checking whether a method was invoked
	 * with the expected arguments.
	 * 
	 * @param argIndex
	 *            The argument index to ignore.
	 * 
	 * @return <code>this</code>.
	 */
	public InvocationVerifier ignoreArgument(int argIndex) {
		this.matcher.ignoreArgument(argIndex);
		return this;
	}

	/**
	 * @return The number of times that the expected method was invoked.
	 */
	public int getInvocationCount() {
		int count = 0;
		for (int i = 0; i < this.methodInvocations.length; i++) {
			if (this.matcher.isMatch(this.methodInvocations[i])) {
				count++;
			}
		}

		return count;
	}
}
