/*
 * ClassArgumentMatcher.java
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
 * The ClassArgumentMatcher class checks that a method argument is an instance
 * of the expected class.
 * 
 * @author C.A. Meijer
 * @since Hammock 1.6
 */
public class ClassArgumentMatcher implements IArgumentMatcher {

	/**
	 * A method to check that the class of the actual argument is the same as or
	 * a subclass of the expected argument.
	 * 
	 * @param argumentExpected
	 *            The expected argument.
	 * @param argumentActual
	 *            The actual argument.
	 * 
	 * @return <code>true</code> if the actual argument can be cast to the class
	 *         of the expected argument.
	 */
	public boolean areArgumentsEqual(Object argumentExpected,
			Object argumentActual) {
		if ((argumentExpected == null) || (argumentActual == null)) {
			return false;
		}

		return argumentExpected.getClass().isAssignableFrom(
				argumentActual.getClass());
	}

}
