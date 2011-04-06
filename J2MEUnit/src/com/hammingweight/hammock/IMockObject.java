/*
 * IMockObject.java
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
 * The <code>IMockObject</code> interface defines the methods that a mock
 * object must implement to work with the Hammock framework.
 * 
 * @author C.A. Meijer
 * @since Hammock 1.0
 */
public interface IMockObject {

	/**
	 * Sets the invocation handler that the mock object should use to evaluate
	 * method invocations.
	 * 
	 * @param handler
	 *            The invocation handler to be used by the mock object.
	 */
	public void setInvocationHandler(IInvocationHandler handler);

	/**
	 * Gets the invocation handler that the mock object uses to evaluate method
	 * invocations.
	 * 
	 * @return The invocation handler used by the mock object.
	 */
	public IInvocationHandler getInvocationHandler();
}
