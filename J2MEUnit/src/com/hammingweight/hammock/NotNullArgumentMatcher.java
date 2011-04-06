/*
 * NotNullrgumentMatcher.java
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
 * The <code>NotNullArgumentMatcher</code> class checks that a method argument 
 * is not <code>null</code>.
 * 
 * @author C.A. Meijer
 * @since Hammock 2.0
 */
public class NotNullArgumentMatcher implements IArgumentMatcher {

	/**
	 * Checks whether an actual argument is not null.
	 * 
	 * @param argumentExpected
	 * @param argumentActual
	 * 
	 * @return <code>true</code> if and only the actual argument is not
	 *         <code>null</code>.
	 */
	public boolean areArgumentsEqual(Object argumentExpected,
			Object argumentActual) {
		return (argumentActual != null);
	}

}
