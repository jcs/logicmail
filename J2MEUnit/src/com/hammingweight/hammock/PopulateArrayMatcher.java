/*
 * ArrayArgumentMatcher.java
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
 * The PopulateArrayMatcher class allows a mocked method to populate an array
 * when the method is invoked.
 * 
 * @author C.A. Meijer
 * @since Hammock 1.6
 */
public class PopulateArrayMatcher implements IArgumentMatcher {

	// The offset from which to start copying.
	private int srcOffset;

	// The number of array elements to copy.
	private int srcLen;

	// The offset in the destination array from which to start copying.
	private int destOffset;

	/**
	 * Constructor.
	 * 
	 * @param srcOffset
	 *            The offset in the source array to start copying from.
	 * 
	 * @param srcLen
	 *            The number of elements to copy.
	 * 
	 * @param destOffset
	 *            The offset in the destination array to start copying to.
	 */
	public PopulateArrayMatcher(int srcOffset, int srcLen, int destOffset) {
		this.srcOffset = srcOffset;
		this.srcLen = srcLen;
		this.destOffset = destOffset;
	}

	/**
	 * A method to check whether this matcher can copy data to a destination
	 * array.
	 * 
	 * @param argumentExpected
	 *            The source array specified in the expectation.
	 * 
	 * @param argumentActual
	 *            The destination array.
	 * 
	 * @return <code>true</code> if this matcher can copy data into the
	 *         destination array.
	 */
	public boolean areArgumentsEqual(Object argumentExpected,
			Object argumentActual) {
		if (argumentActual == null) {
			return false;
		}
		if (!argumentActual.getClass().isAssignableFrom(
				argumentExpected.getClass())) {
			return false;
		}
		System.arraycopy(argumentExpected, this.srcOffset, argumentActual,
				this.destOffset, this.srcLen);
		return true;
	}

}
