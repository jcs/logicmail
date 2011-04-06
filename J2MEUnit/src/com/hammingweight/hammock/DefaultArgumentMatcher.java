/*
 * DefaultArgumentMatcher.java
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
 * The <code>DefaultArgumentMatcher</code> is used by default by the
 * <code>MethodHandler</code> class to determine whether a method was invoked
 * with the expected arguments. This <code>IArgumentMatcher</code> is used
 * unless no argument expectations were set, an argument is ignored or a
 * proprietary <code>IArgumentMatcher</code> has been set for the method
 * argument.
 * 
 * @author C.A. Meijer
 * @since Hammock 1.4
 */
public class DefaultArgumentMatcher implements IArgumentMatcher {

	private static boolean areArraysEqual(boolean[] a, boolean[] b) {
		if (a.length != b.length) {
			return false;
		}

		for (int i = 0; i < a.length; i++) {
			if (a[i] != b[i]) {
				return false;
			}
		}

		return true;
	}

	private static boolean areArraysEqual(byte[] a, byte[] b) {
		if (a.length != b.length) {
			return false;
		}

		for (int i = 0; i < a.length; i++) {
			if (a[i] != b[i]) {
				return false;
			}
		}

		return true;
	}

	private static boolean areArraysEqual(char[] a, char[] b) {
		if (a.length != b.length) {
			return false;
		}

		for (int i = 0; i < a.length; i++) {
			if (a[i] != b[i]) {
				return false;
			}
		}

		return true;
	}

	private static boolean areArraysEqual(short[] a, short[] b) {
		if (a.length != b.length) {
			return false;
		}

		for (int i = 0; i < a.length; i++) {
			if (a[i] != b[i]) {
				return false;
			}
		}

		return true;
	}

	private static boolean areArraysEqual(int[] a, int[] b) {
		if (a.length != b.length) {
			return false;
		}

		for (int i = 0; i < a.length; i++) {
			if (a[i] != b[i]) {
				return false;
			}
		}

		return true;
	}

	private static boolean areArraysEqual(long[] a, long[] b) {
		if (a.length != b.length) {
			return false;
		}

		for (int i = 0; i < a.length; i++) {
			if (a[i] != b[i]) {
				return false;
			}
		}

		return true;
	}

	private static boolean areArraysEqual(float[] a, float[] b) {
		if (a.length != b.length) {
			return false;
		}

		for (int i = 0; i < a.length; i++) {
			if (a[i] != b[i]) {
				return false;
			}
		}

		return true;
	}

	private static boolean areArraysEqual(double[] a, double[] b) {
		if (a.length != b.length) {
			return false;
		}

		for (int i = 0; i < a.length; i++) {
			if (a[i] != b[i]) {
				return false;
			}
		}

		return true;
	}

	private static boolean areArraysEqual(Object a, Object b) {
		if (a instanceof boolean[]) {
			return areArraysEqual((boolean[]) a, (boolean[]) b);
		} else if (a instanceof byte[]) {
			return areArraysEqual((byte[]) a, (byte[]) b);
		} else if (a instanceof short[]) {
			return areArraysEqual((short[]) a, (short[]) b);
		} else if (a instanceof char[]) {
			return areArraysEqual((char[]) a, (char[]) b);
		} else if (a instanceof int[]) {
			return areArraysEqual((int[]) a, (int[]) b);
		} else if (a instanceof long[]) {
			return areArraysEqual((long[]) a, (long[]) b);
		} else if (a instanceof float[]) {
			return areArraysEqual((float[]) a, (float[]) b);
		} else if (a instanceof double[]) {
			return areArraysEqual((double[]) a, (double[]) b);
		}

		Object[] a1 = (Object[]) a;
		Object[] b1 = (Object[]) b;
		if (a1.length != b1.length) {
			return false;
		}

		for (int i = 0; i < a1.length; i++) {
			Object o1 = a1[i];
			Object o2 = b1[i];
			if (!areObjectsEqual(o1, o2)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Tests two objects for equality.
	 * 
	 * @param a
	 *            Object 1.
	 * @param b
	 *            Object 2.
	 * 
	 * @return true if the two objects are equal.
	 */
	private static boolean areObjectsEqual(Object a, Object b) {
		if ((a == null) && (b == null)) {
			return true;
		}

		if ((a == null) || (b == null)) {
			return false;
		}

		if (!a.getClass().equals(b.getClass())) {
			return false;
		}

		if (a.getClass().isArray()) {
			return areArraysEqual(a, b);
		} else {
			return a.equals(b);
		}
	}
	
	/**
	 * @param argumentExpected
	 *            The expected argument.
	 * 
	 * @param argumentActual
	 *            The argument that was actually passed to a method.
	 * 
	 * @return Returns true if
	 *         <code>argumentExpected.equals(argumentActual)</code> returns
	 *         true, or both arguments are null, or both arguments are arrays
	 *         that contain the same elements; otherwise false.
	 */
	public boolean areArgumentsEqual(Object argumentExpected,
			Object argumentActual) {
		return areObjectsEqual(argumentExpected,
				argumentActual);
	}

}
