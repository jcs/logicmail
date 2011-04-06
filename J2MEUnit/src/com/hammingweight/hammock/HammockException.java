/*
 * HammockException.java
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
 * The HammockException class encapsulates various exceptions that may be thrown
 * by test doubles.
 * 
 * @author C.A. Meijer
 * @since Hammock 1.0
 */
public class HammockException extends RuntimeException {

	/**
	 * An error message that describes the exception.
	 */
	private String errorMessage;

	/**
	 * The class of the test double that threw the exception.
	 */
	private Class throwingClass;

	/**
	 * An identifier for the method that threw the exception.
	 */
	private String methodId;

	/**
	 * A constructor.
	 * 
	 * @param throwingClass
	 *            The class of the object that threw the exception.
	 * 
	 * @param errorMessage
	 *            A descriptive error message.
	 */
	public HammockException(Class throwingClass, String errorMessage) {
		this.throwingClass = throwingClass;
		this.errorMessage = errorMessage;
	}

	/**
	 * A constructor.
	 * 
	 * @param method
	 *            The method invoked on a test double that led to the exception.
	 * 
	 * @param errorMessage
	 *            A descriptive error message.
	 */
	public HammockException(MockMethod method, String errorMessage) {
		if (method != null) {
			this.throwingClass = method.getMethodClass();
			this.methodId = method.getId();
		}

		this.errorMessage = errorMessage;
	}

	/**
	 * A constructor that is used when a test double tries to throw an exception
	 * from a method where the exception is inconsistent with the method's
	 * signature.
	 * 
	 * @param t
	 *            An exception that a test double tried to throw.
	 */
	public HammockException(Throwable t) {
		this((Class) null, IHammockExceptionErrors.INCORRECT_THROWABLE_CLASS);
	}

	/**
	 * A getter method.
	 * 
	 * @return A descriptive error message.
	 */
	public String getError() {
		return this.errorMessage;
	}

	/**
	 * Returns a detailed error message describing the exception.
	 * 
	 * @return A message describing what caused an exception to be thrown.
	 */
	public String getMessage() {
		if (this.errorMessage == null) {
			return super.getMessage();
		}

		String s = this.errorMessage;
		if (this.throwingClass != null) {
			String clazzName = this.throwingClass.getName();
			clazzName = clazzName.substring(clazzName.lastIndexOf('.') + 1);
			s += "\n\tClass:  " + clazzName;
		}
		if (this.methodId != null) {
			s += "\n\tMethod: " + this.methodId;
		}

		return s;
	}
}
