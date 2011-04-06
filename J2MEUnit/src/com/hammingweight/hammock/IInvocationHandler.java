/*
 * IInvocationHandler.java
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
 * The IInvocarionHandler interface defines the methods that a method invocation
 * handler for a test double exposes. An IInvocationHandler is associated with a
 * priority so that high priority handlers will get the first chance to process
 * a method invocation.
 * 
 * @author C.A. Meijer
 * @since Hammock 1.0
 */
public interface IInvocationHandler {
	/**
	 * This method is called when a test double Object needs to respond to a
	 * method invocation.
	 * 
	 * @param mi
	 *            The method invocation.
	 *            
	 * @since Hammock 1.6.
	 */
	public void invoke(MethodInvocation mi);

}
