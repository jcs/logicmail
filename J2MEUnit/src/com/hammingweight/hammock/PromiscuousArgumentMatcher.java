/*
 * PromiscuousArgumentMatcher.java
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
 * The <code>PromiscuousArgumentMatcher</code> always returns that two method
 * arguments are equal. This argument matcher is used if a
 * <code>MethodHandler</code> should ignore a method argument when determining
 * whether a method was invoked with the expected arguments.
 * 
 * @author C.A. Meijer
 * @since Hammock 1.4
 */
public class PromiscuousArgumentMatcher implements IArgumentMatcher {

	/**
	 * @param argumentExpected
	 *            The expected argument.
	 * 
	 * @param argumentActual
	 *            The argument that was actually passed to a method.
	 * 
	 * @return Always returns true.
	 */
	public boolean areArgumentsEqual(Object argumentExpected,
			Object argumentActual) {
		return true;
	}

}
