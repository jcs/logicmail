/*
 * MockMethod.java
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
 * The <code>MockMethod</code> class models a method associated with a mock
 * object (or another test double such as a spy or stub). The method is defined
 * in some interface or class. Important attributes of the method are whether
 * the method is abstract, how many arguments it takes, the return type, etc.
 * 
 * @author C.A. Meijer
 * @since Hammock 1.0
 */
public class MockMethod implements IHammockExceptionErrors, IClassDefinitions {

	// The class associated with this method.
	private Class clazz;

	// An identifier.
	private String id;

	// The classes of the arguments.
	private Class[] argClasses;

	// The classes of the exceptions that might be thrown.
	private Class[] throwableClasses;

	// The class of the return type.
	private Class retClass;

	// Is the method in the super class or interface abstract?
	private boolean isAbstract;

	// A method to check that an argument list doesn't contain nulls.
	private static void validateArgList(Class[] clazzes) {
		for (int i = 0; i < clazzes.length; i++) {
			if (clazzes[i] == null) {
				throw new NullPointerException();
			}
		}
	}

	// A method to check that the Throwable list contains only Throwable
	// classes.
	private static void validateThrowableList(Class[] clazzes) {
		for (int i = 0; i < clazzes.length; i++) {
			if (!THROWABLE_CLASS.isAssignableFrom(clazzes[i])) {
				throw new HammockException(clazzes[i], CLASS_IS_NOT_THROWABLE);
			}
		}
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 *            An identifier for the method.
	 * 
	 * @param numArgs
	 *            The number of arguments that the method takes.
	 * 
	 * @param retClass
	 *            The class of the value to be returned by the method (null if
	 *            the method is void).
	 * 
	 * @param isAbstract
	 *            True if the method is abstract.
	 */
	public MockMethod(String id, int numArgs, Class retClass, boolean isAbstract) {
		this(IMOCKOBJECT_CLASS, id, new Class[0],
				new Class[] { THROWABLE_CLASS }, retClass, isAbstract);

		if (numArgs < 0) {
			throw new IllegalArgumentException();
		}
		this.argClasses = new Class[numArgs];
		for (int i = 0; i < numArgs; i++) {
			this.argClasses[i] = OBJECT_CLASS;
		}
	}

	/**
	 * Constructor.
	 * 
	 * @param clazz
	 *            The class that the method belongs to.
	 * 
	 * @param id
	 *            An identifier for the method.
	 * 
	 * @param argClasses
	 *            The classes of the arguments of the method.
	 * 
	 * @param throwableClasses
	 *            The classes of the exceptions that the method might throw.
	 * 
	 * @param retClass
	 *            The class of the value to be returned by the method (null if
	 *            the method is void).
	 * 
	 * @param isAbstract
	 *            True if the method is abstract.
	 */
	public MockMethod(Class clazz, String id, Class[] argClasses,
			Class[] throwableClasses, Class retClass, boolean isAbstract) {
		if ((clazz == null) || (id == null) || (argClasses == null)
				|| (throwableClasses == null)) {
			throw new NullPointerException();
		}

		validateArgList(argClasses);
		validateThrowableList(throwableClasses);

		if (!IMOCKOBJECT_CLASS.isAssignableFrom(clazz)) {
			throw new HammockException(clazz, DOES_NOT_SUBCLASS_MOCK_OBJECT);
		}

		this.clazz = clazz;
		this.id = id;
		this.argClasses = argClasses;
		this.throwableClasses = throwableClasses;
		this.retClass = retClass;
		this.isAbstract = isAbstract;
	}

	/**
	 * Gets the Class associated with a method.
	 * 
	 * @return The Class that the method belongs to.
	 */
	public Class getMethodClass() {
		return this.clazz;
	}

	/**
	 * Gets the identifier for a method.
	 * 
	 * @return The identifier for the method.
	 */
	public String getId() {
		return this.id;
	}

	/**
	 * Returns the classes of the method's arguments.
	 * 
	 * @return The classes of the method's arguments.
	 */
	protected Class[] getArgumentClasses() {
		return this.argClasses;
	}

	/**
	 * Returns the classes that the method throws.
	 * 
	 * @return The classes that the method throws.
	 */
	protected Class[] getThrowableClasses() {
		return this.throwableClasses;
	}

	/**
	 * Returns the Class of Object returned by this method.
	 * 
	 * @return The Class of Object returned by this method.
	 */
	protected Class getReturnClass() {
		return this.retClass;
	}

	/**
	 * Returns whether the method being modelled is an abstract method.
	 * 
	 * @return true if the modelled method is abstract.
	 */
	public boolean isAbstract() {
		return this.isAbstract;
	}

	/**
	 * This method checks that a list of arguments matches the signature of the
	 * method; i.e. is the number of arguments correct and are the classes of
	 * the arguments in keeping with the method's signature.
	 * 
	 * @param args
	 *            A list of arguments.
	 * 
	 * @throws HammockException
	 *             if the arguments are inconsistent with the method's
	 *             signature.
	 * 
	 */
	public void validateArguments(Object[] args) {
		if (args.length != this.argClasses.length) {
			throw new HammockException(this,
					IHammockExceptionErrors.WRONG_NUMBER_OF_ARGS);
		}

		for (int i = 0; i < args.length; i++) {
			if (args[i] == null) {
				continue;
			}
			if (!argClasses[i].isAssignableFrom(args[i].getClass())) {
				throw new HammockException(this, WRONG_ARGUMENT_CLASS);
			}
		}
	}

	/**
	 * This method checks that an exception can be thrown by this method.
	 * 
	 * @param t
	 *            The exception or error to throw.
	 * 
	 * @throws HammockException
	 *             if the exception is inconsistent with the method's signature.
	 * 
	 * @since Hammock 2.0
	 */
	public void validateThrowable(Throwable t) {
		boolean throwableOk = false;

		if ((t instanceof RuntimeException) || (t instanceof Error)) {
			throwableOk = true;
		}

		Class tClass = t.getClass();
		for (int i = 0; i < this.throwableClasses.length; i++) {
			Class th = this.throwableClasses[i];
			if (th.isAssignableFrom(tClass)) {
				throwableOk = true;
			}
		}

		if (!throwableOk) {
			throw new HammockException(this, INCORRECT_THROWABLE_CLASS);
		}
	}

	/**
	 * This method returns the number of arguments for the encapsulated method.
	 * 
	 * @return The number of arguments.
	 * 
	 * @since Hammock 2.0
	 */
	public int getNumberOfArguments() {
		return this.argClasses.length;
	}
}
