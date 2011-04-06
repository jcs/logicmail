/*
 * MethodInvocation.java
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
 * The <code>MethodInvocation</code> class describes a method invocation. A
 * method invocation is described by the invoked method, the arguments passed in
 * the method and the object on which the method was invoked.
 * 
 * @author C.A. Meijer
 * @since Hammock 1.0
 */
public class MethodInvocation implements IHammockExceptionErrors {

	/**
	 * The mock object that the method was invoked on.
	 */
	private IMockObject mockObject;

	/**
	 * The method invoked.
	 */
	private MockMethod method;

	/**
	 * The arguments passed to the method.
	 */
	private Object[] args;

	/**
	 * Flag indicating whether the method was evaluated (i.e. a return value was
	 * set for the invocation).
	 */
	private boolean isEvaluated;

	/**
	 * The value returned by the method invocation.
	 */
	private Object retVal;

	/**
	 * The exception thrown by the method when invoked.
	 */
	private Throwable throwable;

	/**
	 * Constructor.
	 * 
	 * @param method
	 *            The invoked method.
	 * @param mockObject
	 *            The mock object on which the method was invoked.
	 * @param args
	 *            The arguments passed in the method invocation.
	 */
	public MethodInvocation(MockMethod method, IMockObject mockObject,
			Object[] args) {
		if (!method.getMethodClass().isAssignableFrom(mockObject.getClass())) {
			throw new HammockException(method, INCORRECT_MOCK_OBJECT);
		}
		method.validateArguments(args);

		this.mockObject = mockObject;
		this.method = method;
		this.args = args;
	}

	/**
	 * Returns the method of the invocation.
	 * 
	 * @return the method.
	 */
	public MockMethod getMethod() {
		return this.method;
	}

	/**
	 * Returns the Object on which the method was invoked.
	 * 
	 * @return The Object on which the method was invoked.
	 */
	public IMockObject getMockObject() {
		return this.mockObject;
	}

	/**
	 * The arguments passed to the method.
	 * 
	 * @return The arguments.
	 */
	public Object[] getMethodArguments() {
		return this.args;
	}

	/**
	 * Sets the value that will be returned by the method.
	 * 
	 * @param retVal
	 *            The value returned by the method.
	 */
	public void setReturnValue(Object retVal) {
		if (!this.isEvaluated) {
			this.retVal = retVal;
		}
		this.isEvaluated = true;
	}

	/**
	 * Sets an exception or error thrown by the method.
	 * 
	 * @param throwable
	 *            The exception or error thrown by the method.
	 */
	public void setThrowable(Throwable throwable) {
		if (throwable == null) {
			throw new NullPointerException();
		}

		if (!this.isEvaluated) {
			this.throwable = throwable;
		}
		this.isEvaluated = true;
	}

	/**
	 * Returns the value or throws an error or exception associated with the
	 * method invocation.
	 * 
	 * @return The value returned by the method.
	 * 
	 * @throws The
	 *             error or exception that the method is expected to throw.
	 */
	public Object getReturnValue() throws Throwable {
		if (!this.isEvaluated) {
			throw new HammockException(this.method, METHOD_CANNOT_BE_EVALUATED);
		}

		if (this.throwable != null) {
			throw this.throwable;
		}

		return this.retVal;
	}

	/**
	 * This method indicates whether the method has been evaluated; i.e. whether
	 * a value to return from the method invocation has been set.
	 * 
	 * @return true if a return value has been set for the method invocation.
	 */
	public boolean isEvaluated() {
		return this.isEvaluated;
	}
}
