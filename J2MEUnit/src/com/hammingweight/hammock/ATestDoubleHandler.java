/*
 * ATestDoubleHandler.java
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

import java.util.Vector;

/**
 * The <code>ATestDoubleHandler</code> class is an
 * <code>IInvocationHandler</code> that can be extended to support mock and spy
 * objects. The class allows expectations to be set that certain methods will be
 * invoked on mock objects.
 * 
 * The class is an example of the composite pattern; it is an aggregation of
 * <code>MethodHandler</code> instances. A <code>MethodHandler</code> can
 * respond to only one method; the <code>ATestDouble</code> can respond to more
 * than one method. The methods that this handler must respond to are set via
 * the <code>setExpectation</code> and <code>setStubExpectation</code> methods.
 * 
 * @author C.A. Meijer
 * @since Hammock 2.0
 */
public abstract class ATestDoubleHandler implements IInvocationHandler,
		IClassDefinitions, IHammockExceptionErrors {

	// A vector containing the method handlers for the expectations.
	private Vector expectations = new Vector();

	// A method to add an expectation that a method will be invoked.
	private MethodHandler addMethodHandler(MethodHandler methodHandler) {
		this.expectations.addElement(methodHandler);
		return methodHandler;
	}

	/**
	 * This method sets an expectation that method will be called on a mock
	 * object. Any arguments passed when the method is invoked will be ignored.
	 * 
	 * @param method
	 *            The method that we expect to be invoked.
	 * 
	 * @return A method handler for processing an invocation of the specified
	 *         method.
	 */
	public MethodHandler setExpectation(MockMethod method) {
		return addMethodHandler(new MethodHandler(method));
	}

	/**
	 * This method sets an expectation that a particular method will be invoked
	 * with a particular set of arguments. No attention is paid to the
	 * particular object that the method is invoked on.
	 * 
	 * @param method
	 *            The method that we expect to be invoked.
	 * @param args
	 *            The arguments that are expected to be passed in the method
	 *            invocation.
	 * 
	 * @return A method handler for processing an invocation of the specified
	 *         method.
	 */
	public MethodHandler setExpectation(MockMethod method, Object[] args) {
		return addMethodHandler(new MethodHandler(method, args));
	}

	/**
	 * This method sets an expectation that a particular method will be invoked
	 * on a particular object.
	 * 
	 * @param method
	 *            The method that we expect to be invoked.
	 * @param mockObject
	 *            The object that we expect the method to be invoked on.
	 * 
	 * @return A method handler for processing an invocation of the specified
	 *         method.
	 */
	public MethodHandler setExpectation(MockMethod method,
			IMockObject mockObject) {
		return addMethodHandler(new MethodHandler(method, mockObject));
	}

	/**
	 * This method sets an expectation that a particular method will be invoked
	 * on a particular object with a particular set of arguments.
	 * 
	 * @param method
	 *            The method that we expect to be invoked.
	 * @param mockObject
	 *            The object that we expect the method to be invoked on.
	 * @param args
	 *            The arguments that are expected to be passed in the method
	 *            invocation.
	 * 
	 * @return A method handler for processing an invocation of the specified
	 *         method.
	 */
	public MethodHandler setExpectation(MockMethod method,
			IMockObject mockObject, Object[] args) {
		return addMethodHandler(new MethodHandler(method, mockObject, args));
	}

	/**
	 * This method allows the handler to respond identically to an arbitrary,
	 * possibly zero, number of invocations of a particular method. Any
	 * arguments passed when the method is invoked will be ignored.
	 * 
	 * @param method
	 *            The method that may be invoked.
	 * 
	 * @return A method handler for processing an invocation of the specified
	 *         method.
	 */
	public MethodHandler setStubExpectation(MockMethod method) {
		return setExpectation(method).setInvocationCount(0, Integer.MAX_VALUE);
	}

	/**
	 * This method allows the handler to respond identically to an arbitrary,
	 * possibly zero, number of invocations of a particular method with a
	 * particular set of arguments. No attention is paid to the particular
	 * object that the method is invoked on.
	 * 
	 * @param method
	 *            The method that may be invoked.
	 * @param args
	 *            The arguments that are expected to be passed in the method
	 *            invocation.
	 * 
	 * @return A method handler for processing an invocation of the specified
	 *         method.
	 */
	public MethodHandler setStubExpectation(MockMethod method, Object[] args) {
		return setExpectation(method, args).setInvocationCount(0,
				Integer.MAX_VALUE);
	}

	/**
	 * This method allows the handler to respond identically to an arbitrary,
	 * possibly zero, number of invocations of a particular method on a mock
	 * object.
	 * 
	 * @param method
	 *            The method that may be invoked.
	 * @param mockObject
	 *            The object that we expect the method to be invoked on.
	 * 
	 * @return A method handler for processing an invocation of the specified
	 *         method.
	 */
	public MethodHandler setStubExpectation(MockMethod method,
			IMockObject mockObject) {
		return setExpectation(method, mockObject).setInvocationCount(0,
				Integer.MAX_VALUE);
	}

	/**
	 * This method allows the handler to respond identically to an arbitrary,
	 * possibly zero, number of invocations of a particular method on a mock
	 * object with a particular set of arguments
	 * 
	 * @param method
	 *            The method that may be invoked.
	 * @param mockObject
	 *            The object that we expect the method to be invoked on.
	 * @param args
	 *            The arguments that are expected to be passed in the method
	 *            invocation.
	 * 
	 * @return A method handler for processing an invocation of the specified
	 *         method.
	 */
	public MethodHandler setStubExpectation(MockMethod method,
			IMockObject mockObject, Object[] args) {
		return setExpectation(method, mockObject, args).setInvocationCount(0,
				Integer.MAX_VALUE);
	}

	/**
	 * A method to determine how many expectations have been set.
	 * 
	 * @return The number of expectations.
	 */
	protected int getNumberOfExpectations() {
		return this.expectations.size();
	}

	/**
	 * A method to get the MethodHandler for a particular expectation.
	 * 
	 * @param expectationNumber
	 *            The expectation of interest.
	 * 
	 * @return The handler for the expectation.
	 */
	protected MethodHandler getExpectation(int expectationNumber) {
		return (MethodHandler) this.expectations.elementAt(expectationNumber);
	}

}
