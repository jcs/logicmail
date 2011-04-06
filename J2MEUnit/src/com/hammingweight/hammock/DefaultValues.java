/*
 * DefaultValues.java
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
 * The DefaultValues class provides a utility method for providing default
 * instances of Java's wrapper classes. For example, the default instance of the
 * <code>Boolean</code> is <code>Boolean(false)</code>.
 * 
 * @author C.A. Meijer
 * @since Hammock 1.6
 */
public class DefaultValues {

	/**
	 * Returns a default object corresponding to a specified Java wrapper class.
	 * 
	 * @param clazz
	 *            A wrapper class.
	 * 
	 * @return A default instance for a wrapper class. <code>null</code> if the
	 *         class is not a wrapper class.
	 */
	public static Object getDefaultValue(Class clazz) {
		if (clazz == IClassDefinitions.BOOLEAN_CLASS) {
			return new Boolean(false);
		} else if (clazz == IClassDefinitions.BYTE_CLASS) {
			return new Byte((byte) 0);
		} else if (clazz == IClassDefinitions.BYTE_CLASS) {
			return new Byte((byte) 0);
		} else if (clazz == IClassDefinitions.CHARACTER_CLASS) {
			return new Character((char) 0);
		} else if (clazz == IClassDefinitions.INTEGER_CLASS) {
			return new Integer(0);
		} else if (clazz == IClassDefinitions.LONG_CLASS) {
			return new Long(0);
		} else if (clazz == IClassDefinitions.SHORT_CLASS) {
			return new Short((short) 0);
		} else if (clazz == IClassDefinitions.DOUBLE_CLASS) {
			return new Double(0.0);
		} else if (clazz == IClassDefinitions.FLOAT_CLASS) {
			return new Float(0.0f);
		} else {
			return null;
		}
	}
}
