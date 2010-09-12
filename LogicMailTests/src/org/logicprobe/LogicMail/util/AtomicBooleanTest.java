/*-
 * Copyright (c) 2010, Derek Konigsberg
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution. 
 * 3. Neither the name of the project nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.logicprobe.LogicMail.util;

import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestMethod;
import j2meunit.framework.TestSuite;

public class AtomicBooleanTest extends TestCase {
    
    public AtomicBooleanTest() {
    }
    
    public AtomicBooleanTest(String testName, TestMethod testMethod) {
        super(testName, testMethod);
    }

    public void testCreate() {
        AtomicBoolean instance = new AtomicBoolean();
        assertTrue(instance.get() == false);
    }
    
    public void testCreateWithValue() {
        AtomicBoolean instance = new AtomicBoolean(true);
        assertTrue(instance.get() == true);
        
        instance = new AtomicBoolean(false);
        assertTrue(instance.get() == false);
    }
    
    public void testSet() {
        AtomicBoolean instance = new AtomicBoolean();
        instance.set(true);
        assertTrue(instance.get() == true);
        
        instance.set(false);
        assertTrue(instance.get() == false);
        
        instance.set(true);
        assertTrue(instance.get() == true);
    }
    
    public void testCompareAndSet() {
        AtomicBoolean instance = new AtomicBoolean();
        assertTrue(instance.compareAndSet(false, false));
        assertTrue(!instance.compareAndSet(true, false));
        assertTrue(!instance.compareAndSet(true, true));
        assertTrue(instance.get() == false);
        
        assertTrue(instance.compareAndSet(false, true));
        assertTrue(instance.get() == true);
    }
 
    public void testGetAndSet() {
        AtomicBoolean instance = new AtomicBoolean();
        assertTrue(instance.getAndSet(true) == false);
        assertTrue(instance.getAndSet(false) == true);
    }
    
    public void testToString() {
        AtomicBoolean instance = new AtomicBoolean();
        assertEquals("false", instance.toString());
        instance.set(true);
        assertEquals("true", instance.toString());
    }
    
    public Test suite() {
        TestSuite suite = new TestSuite("AtomicBooleanTest");
        
        suite.addTest(new AtomicBooleanTest("create", new TestMethod()
        { public void run(TestCase tc) throws Throwable {((AtomicBooleanTest)tc).testCreate(); } }));
        suite.addTest(new AtomicBooleanTest("createWithValue", new TestMethod()
        { public void run(TestCase tc) throws Throwable {((AtomicBooleanTest)tc).testCreateWithValue(); } }));
        suite.addTest(new AtomicBooleanTest("set", new TestMethod()
        { public void run(TestCase tc) throws Throwable {((AtomicBooleanTest)tc).testSet(); } }));
        suite.addTest(new AtomicBooleanTest("compareAndSet", new TestMethod()
        { public void run(TestCase tc) throws Throwable {((AtomicBooleanTest)tc).testCompareAndSet(); } }));
        suite.addTest(new AtomicBooleanTest("getAndSet", new TestMethod()
        { public void run(TestCase tc) throws Throwable {((AtomicBooleanTest)tc).testGetAndSet(); } }));
        suite.addTest(new AtomicBooleanTest("toString", new TestMethod()
        { public void run(TestCase tc) throws Throwable {((AtomicBooleanTest)tc).testToString(); } }));
        
        return suite;
    }
}
