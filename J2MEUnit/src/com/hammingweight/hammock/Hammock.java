/*
 * Hammock.java
 *  
 * Copyright 2008 C.A. Meijer
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
 * The <code>Hammock</code> class is an <code>IInvocationHandler</code> that
 * causes a test double associated with the handler to behave like a mock
 * object. The class is an example of the composite pattern; it is an
 * aggregation of <code>MethodHandler</code> instances. A
 * <code>MethodHandler</code> can respond to only one method; the
 * <code>Hammock</code> can respond to more than one method. The methods that
 * this handler must respond to are set via the <code>setExpectation</code>
 * methods.
 * 
 * @author C.A. Meijer
 * @since Hammock 1.5
 */
public class Hammock extends ATestDoubleHandler {

	// Is the order that the methods must be invoked in strict?
	private boolean isStrict;

	// An exception that was thrown during a method invocation. If verify is
	// called we must throw the exception again.
	private HammockException verificationException;

	// This method sets the verification exception if it hasn't already been
	// set.
	private void setVerificationException(HammockException verificationException) {
		if (this.verificationException == null) {
			this.verificationException = verificationException;
		}
		throw verificationException;
	}

	/**
	 * A method that verifies that a handler was invoked by mock objects as
	 * expected.
	 * 
	 * @throws HammockException
	 *             if verification fails.
	 */
	public void verify() {
		if (this.verificationException != null) {
			throw this.verificationException;
		}

		for (int i = 0; i < getNumberOfExpectations(); i++) {
			MethodHandler mh = getExpectation(i);
			mh.verify();
		}
	}

	/**
	 * Checks whether all expectations were met and whether no methods were
	 * unexpectedly invoked.
	 * 
	 * @return <code>true</code> if, and only if, all expectations were met and
	 *         no methods were unepectedly invoked.
	 *         
	 * @since Hammock 2.0
	 */
	public boolean isVerified() {
		try {
			verify();
			return true;
		} catch (HammockException e) {
			return false;
		}
	}

	/**
	 * Sets that the mock objects associated with this handler are strict mocks
	 * and that the methods must be invoked in the order that they were added to
	 * the handler.
	 * 
	 */
	public void setStrictOrdering() {
		this.isStrict = true;
	}

	/**
	 * Called by a test double when a method is invoked. This method sets the
	 * return value for the invocation if this handler can process the
	 * invocation. If the handler cannot process the method invocation it throws
	 * a HammockException. An exception is also thrown if strict expectations
	 * have been set and a method is invoked out of sequence.
	 * 
	 * @param mi
	 *            The method invocation.
	 * 
	 * @throws HammockException
	 */
	public void invoke(MethodInvocation mi) {
		for (int i = 0; i < getNumberOfExpectations(); i++) {
			getExpectation(i).invoke(mi);
			if (mi.isEvaluated()) {
				return;
			}
			if (this.isStrict) {
				try {
					getExpectation(i).verify();
				} catch (HammockException he) {
					setVerificationException(new HammockException(mi
							.getMethod(), METHOD_INVOKED_OUT_OF_SEQUENCE));
				}
			}
		}

		this.setVerificationException(new HammockException(mi.getMethod(),
				METHOD_CANNOT_BE_EVALUATED));
	}
}
