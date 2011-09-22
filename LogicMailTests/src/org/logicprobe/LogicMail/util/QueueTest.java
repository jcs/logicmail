/*-
 * Copyright (c) 2008, Derek Konigsberg
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

import java.util.NoSuchElementException;

import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestMethod;
import j2meunit.framework.TestSuite;

/**
 * Unit test for Queue
 */
public class QueueTest extends TestCase {
    private Queue instance = null;
    
    /** Creates a new instance of QueueTest */
    public QueueTest() {
    }
    
    public QueueTest(String testName, TestMethod testMethod) {
        super(testName, testMethod);
    }
    
    public void setUp() {
        instance = new Queue();
    }
    
    public void tearDown() {
        instance = null;
    }

    public void testAdd() {
    	instance.add(new Integer(1));
    	assertEquals(new Integer(1), instance.element());
    	instance.add(new Integer(2));
    	assertEquals(new Integer(1), instance.element());
    	instance.add(new Integer(3));
    	assertEquals(new Integer(1), instance.element());
    	instance.add(new Integer(4));
    	assertEquals(new Integer(1), instance.element());
    	
    	boolean expThrown = false;
    	try {
    		instance.add(null);
    	} catch (NullPointerException e) {
    		expThrown = true;
    	}
    	assertTrue(expThrown);
    }
    
    public void testPush() {
        instance.add(new Integer(1));
        assertEquals(new Integer(1), instance.element());
        instance.add(new Integer(2));
        assertEquals(new Integer(1), instance.element());
        instance.push(new Integer(10));
        assertEquals(new Integer(10), instance.element());
        
        assertEquals(new Integer(10), instance.remove());
        assertEquals(new Integer(1), instance.remove());
        assertEquals(new Integer(2), instance.remove());
    }
    
    public void testRemove() {
    	boolean expThrown = false;
    	try {
    		instance.remove();
    	} catch (NoSuchElementException e) {
    		expThrown = true;
    	}
    	assertTrue(expThrown);

    	instance.add(new Integer(1));
    	instance.add(new Integer(2));
    	instance.add(new Integer(3));
    	instance.add(new Integer(4));
    	
    	assertEquals(new Integer(1), instance.remove());
    	assertEquals(new Integer(2), instance.remove());
    	assertEquals(new Integer(3), instance.remove());
    	assertEquals(new Integer(4), instance.remove());

    	expThrown = false;
    	try {
    		instance.remove();
    	} catch (NoSuchElementException e) {
    		expThrown = true;
    	}
    	assertTrue(expThrown);
    }
    
    public void testElement() {
    	assertNull(instance.element());
    	instance.add(new Integer(1));
    	assertEquals(new Integer(1), instance.element());
    	instance.remove();
    	assertNull(instance.element());
    }
    
    public void testClear() {
    	instance.add(new Integer(1));
    	instance.add(new Integer(2));
    	instance.add(new Integer(3));
    	instance.add(new Integer(4));
    	assertEquals(new Integer(1), instance.element());
    	
    	instance.clear();
    	assertNull(instance.element());
    }
    
    public Test suite() {
        TestSuite suite = new TestSuite("Queue");

        suite.addTest(new QueueTest("add", new TestMethod()
        { public void run(TestCase tc) {((QueueTest)tc).testAdd(); } }));
        suite.addTest(new QueueTest("push", new TestMethod()
        { public void run(TestCase tc) {((QueueTest)tc).testPush(); } }));
        suite.addTest(new QueueTest("remove", new TestMethod()
        { public void run(TestCase tc) {((QueueTest)tc).testRemove(); } }));
        suite.addTest(new QueueTest("element", new TestMethod()
        { public void run(TestCase tc) {((QueueTest)tc).testElement(); } }));
        suite.addTest(new QueueTest("clear", new TestMethod()
        { public void run(TestCase tc) {((QueueTest)tc).testClear(); } }));

        return suite;
    }
}
