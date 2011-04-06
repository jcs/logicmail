/*
 * Hamspy.java
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
 * The <code>Hamspy</code> class is an <code>IInvocationHandler</code> that
 * causes a test double associated with the handler to behave like a spy object.
 * The <code>Hamspy</code> class can also behave like a mock object that
 * responds to methods set via the setExpectation() method.
 * 
 * @author C.A. Meijer
 * @since Hammock 1.6
 */
public class Hamspy extends ATestDoubleHandler {

	// Should we stub concrete (non-abstract) classes? If true, the behaviour of
	// Hamspy is similar to Mockito's mocking of concrete classes.
	private boolean stubConcreteMethods;

	// The method invocations that have been spied on.
	private Vector invocations = new Vector();

	// A utility method to convert a Vector an array of IMethodInvocations.
	private MethodInvocation[] getVectorAsArray(Vector v) {
		MethodInvocation[] mi = new MethodInvocation[v.size()];
		for (int i = 0; i < mi.length; i++) {
			mi[i] = (MethodInvocation) v.elementAt(i);
		}
		return mi;
	}

	/**
	 * Constructor; equivalent to <code>Hamspy(false)</code>.
	 */
	public Hamspy() {
		this(false);
	}

	/**
	 * A constructor that determines whether this spy handler should respond to
	 * invocations of non-abstract methods.
	 * 
	 * If <code>false</code>, the handler will allow the mocked class's
	 * superclass to respond to a method invocation.
	 * 
	 * If <code>true</code>, the handler will respond with a default value (e.g.
	 * <code>false</code> if the return type is <code>boolean</code> or 0 if the
	 * return type is <code>int</code>).
	 * 
	 * @param stubConcreteMethods
	 *            Should this handler handle invocations of concrete methods or
	 *            delegate them to the superclass?
	 */
	public Hamspy(boolean stubConcreteMethods) {
		this.stubConcreteMethods = stubConcreteMethods;
	}

	/**
	 * A method to get all the invocations that were spied on.
	 * 
	 * @return The spied on invocations.
	 */
	public MethodInvocation[] getInvocations() {
		return getVectorAsArray(this.invocations);
	}

	// A method to get all method invocations that match a specified pattern.
	private MethodInvocation[] getInvocations(InvocationMatcher matcher) {
		Vector invocs = new Vector();
		for (int i = 0; i < this.invocations.size(); i++) {
			MethodInvocation mi = (MethodInvocation) this.invocations
					.elementAt(i);
			if (matcher.isMatch(mi)) {
				invocs.addElement(mi);
			}
		}
		return getVectorAsArray(invocs);
	}

	/**
	 * A method to get all invocations of a particular method.
	 * 
	 * @param method
	 *            The method that was spied on.
	 * 
	 * @return The invocations of the method of interest.
	 */
	public MethodInvocation[] getInvocations(MockMethod method) {
		return getInvocations(new InvocationMatcher(method));
	}

	/**
	 * A method to get a verifier for checking that a method was invoked as
	 * expected.
	 * 
	 * @param method
	 *            The method of interest.
	 * 
	 * @return A verifier for validating the spied-on method invocations.
	 * 
	 * @since Hammock 2.0
	 */
	public InvocationVerifier getExpectation(MockMethod method) {
		return new InvocationVerifier(new InvocationMatcher(method),
				getInvocations());
	}

	/**
	 * A method to get the invocations of a particular method on a specific
	 * (mock) object.
	 * 
	 * @param method
	 *            The method of interest.
	 * @param mockObject
	 *            The object of interest.
	 * 
	 * @return The invocations of the method on the object.
	 */
	public MethodInvocation[] getInvocations(MockMethod method,
			IMockObject mockObject) {
		return getInvocations(new InvocationMatcher(method, mockObject));
	}

	/**
	 * A method to get a verifier for checking that a method was invoked as
	 * expected.
	 * 
	 * @param method
	 *            The method of interest.
	 * @param mockObject
	 *            The mock object on which we were spying.
	 * 
	 * @return A verifier for validating the spied-on method invocations.
	 * 
	 * @since Hammock 2.0
	 */
	public InvocationVerifier getExpectation(MockMethod method,
			IMockObject mockObject) {
		return new InvocationVerifier(
				new InvocationMatcher(method, mockObject), getInvocations());
	}

	/**
	 * A method to get a verifier for checking that a method was invoked as
	 * expected.
	 * 
	 * @param method
	 *            The method of interest.
	 * @param mockObject
	 *            The mock object on which we were spying.
	 * @param methodArgs
	 *            The expected arguments.
	 * 
	 * @return A verifier for validating the spied-on method invocations.
	 * 
	 * @since Hammock 2.0
	 */
	public InvocationVerifier getExpectation(MockMethod method,
			IMockObject mockObject, Object[] methodArgs) {
		return new InvocationVerifier(new InvocationMatcher(method, mockObject,
				methodArgs), getInvocations());
	}

	/**
	 * A method to get a verifier for checking that a method was invoked as
	 * expected.
	 * 
	 * @param method
	 *            The method of interest.
	 * @param methodArgs
	 *            The expected arguments.
	 * 
	 * @return A verifier for validating the spied-on method invocations.
	 * 
	 * @since Hammock 2.0
	 */
	public InvocationVerifier getExpectation(MockMethod method,
			Object[] methodArgs) {
		return new InvocationVerifier(
				new InvocationMatcher(method, methodArgs), getInvocations());
	}

	/**
	 * Processes a method invocation on a spy object. The method invocation is
	 * recorded. If an expectation has been set for the method invocation, this
	 * handler responds as specified in the expectation. If no, expectation has
	 * been set, the handler provides a default response (e.g. returning
	 * <code>false</code> if the return type is a <code>boolean</code>) if the
	 * method in the superclass is abstract. If the super class has a concrete
	 * implementation of the method, the call is delegated to the super class.
	 * 
	 * @param mi
	 *            The method invocation.
	 */
	public void invoke(MethodInvocation mi) {
		this.invocations.addElement(mi);

		for (int i = 0; i < getNumberOfExpectations(); i++) {
			getExpectation(i).invoke(mi);
			if (mi.isEvaluated()) {
				return;
			}
		}

		if ((mi.getMethod().isAbstract()) || (this.stubConcreteMethods)) {
			mi.setReturnValue(DefaultValues.getDefaultValue(mi.getMethod()
					.getReturnClass()));
		}
	}

}
