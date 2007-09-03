/*-
 * Copyright (c) 2007, Derek Konigsberg
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

/**
 * Unit test for EventListenerList
 */
public class EventListenerListTest extends TestCase {
    private EventListenerList instance = null;
    
    /** Creates a new instance of EventListenerListTest */
    public EventListenerListTest() {
    }
    
    public EventListenerListTest(String testName, TestMethod testMethod) {
        super(testName, testMethod);
    }
    
    public void setUp() {
        instance = new EventListenerList();
    }
    
    public void tearDown() {
        instance = null;
    }
    
    class TestListener implements EventListener {
    }
    
    public void testAdd() {
        TestListener testListener = new TestListener();
        assertNotNull(instance.getListenerList());
        assertEquals(0, instance.getListenerCount(TestListener.class));
        instance.add(TestListener.class, testListener);
        
        assertEquals(1, instance.getListenerCount(TestListener.class));
        EventListener[] addedListeners = instance.getListeners(TestListener.class);
        assertEquals(1, addedListeners.length);
        assertTrue(addedListeners[0] == testListener);
    }
    
    public void testRemove() {
        TestListener testListener = new TestListener();
        instance.add(TestListener.class, testListener);
        instance.remove(TestListener.class, testListener);
        assertNotNull(instance.getListenerList());
        assertEquals(0, instance.getListenerCount(TestListener.class));        
    }
    
    public Test suite() {
        TestSuite suite = new TestSuite("EventListenerListTest");
        
        suite.addTest(new EventListenerListTest("add", new TestMethod()
        { public void run(TestCase tc) {((EventListenerListTest)tc).testAdd(); } }));
        suite.addTest(new EventListenerListTest("remove", new TestMethod()
        { public void run(TestCase tc) {((EventListenerListTest)tc).testRemove(); } }));
        
        return suite;
    }
}
