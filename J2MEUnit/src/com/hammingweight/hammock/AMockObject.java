/*
 * AMockObject.java
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
 * The AMockObject class is extended by mock objects so that they can use the
 * Hammock framework.
 * 
 * @author C.A. Meijer
 * @since Hammock 1.0
 */
public abstract class AMockObject implements IMockObject,
		IHammockExceptionErrors {

	/**
	 * The handler that will be called back when a method is invoked on this /*
	 * object.
	 */
	private IInvocationHandler handler;

	/**
	 * Returns the invocation handler associated with this object.
	 * 
	 * @return The invocation handler.
	 * @throws HammockException
	 *             if no handler has been set.
	 */
	public IInvocationHandler getInvocationHandler() {
		if (this.handler == null) {
			throw new HammockException(this.getClass(), HANDLER_NOT_SET);
		}

		return this.handler;
	}

	/**
	 * Sets the invocation handler associated with this object.
	 * 
	 * @throws HammockException
	 *             if a handler has already been set.
	 */
	public void setInvocationHandler(IInvocationHandler handler) {
		if (handler == null) {
			throw new NullPointerException();
		}
		this.handler = handler;
	}

	/**
	 * A convenience method that verifies that a value isn't <code>null</code>
	 * and throws a <code>HammockException</code> if it is. This method is
	 * used to check that methods that return a primitive value don't try to
	 * return <code>null</code> which is valid only as a value for an
	 * <code>Object</code> reference and not a primitive type.
	 * 
	 * @param method
	 *            The method that is checking that a return value is not
	 *            <code>null</code>.
	 * @param returnValue
	 *            the return value to be checked.
	 */
	public static final void assertReturnNotNull(MockMethod method,
			Object returnValue) {
		if (returnValue == null) {
			throw new HammockException(method, NO_RETURN_VALUE);
		}
	}

}
