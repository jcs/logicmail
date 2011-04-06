/*
 * IArgumentMatcher.java
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

/**
 * The <code>IArgumentMatcher</code> interface provides a method for a 
 * <code>MethodHandler</code> to determine whether two method arguments
 * should be regarded as equal.
 * 
 * @author C.A. Meijer
 * @since Hammock 1.4
 */
package com.hammingweight.hammock;

/**
 * The IArgumentMatcher is implemented by classes that determine whether two
 * objects should be regarded as equal. This is useful when one needs to
 * determine whether an argument passed to a mock object's method is expected.
 * 
 * @author C.A. Meijer
 * @since Hammock 1.4
 */
public interface IArgumentMatcher {
	/**
	 * A method to determine whether an argument passed in a method invocation
	 * of a mock object is equal to the expected argument.
	 * 
	 * @param argumentExpected
	 *            An expected argument.
	 * @param argumentActual
	 *            An actual argument passed to a mocked method.
	 * 
	 * @return true if the arguments should be regarded as equal.
	 */
	public boolean areArgumentsEqual(Object argumentExpected,
			Object argumentActual);
}
