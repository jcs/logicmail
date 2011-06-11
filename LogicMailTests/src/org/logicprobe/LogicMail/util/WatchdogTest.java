/*-
 * Copyright (c) 2011, Derek Konigsberg
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

public class WatchdogTest extends TestCase {
    // Short timeout for test purposes
    private static final long TIMEOUT = 50;

    private Watchdog instance;
    private volatile boolean timeout;
    
    public WatchdogTest() {
    }
    
    public WatchdogTest(String testName, TestMethod testMethod) {
        super(testName, testMethod);
    }

    private WatchdogListener listener = new WatchdogListener() {
        public void watchdogTimeout() {
            timeout = true;
        }
    };
    
    public void setUp() {
        timeout = false;
        instance = new Watchdog(listener);
    }
    
    public void tearDown() {
        instance.shutdown();
        instance = null;
    }

    public void testStartCancelImmediate() {
        // Normal start/cancel operation without a delay
        instance.start(TIMEOUT);
        instance.cancel();
        assertTrue(!timeout);
    }
    
    public void testStartCancelDelay() {
        // Normal start/cancel operation with a short delay
        instance.start(TIMEOUT);
        sleepNoTimeout();
        instance.cancel();
        assertTrue(!timeout);
    }

    public void testStartCancelMultiple() {
        // Multiple start/cancel operations with different delays
        instance.start(TIMEOUT * 2);
        sleepNoTimeout();
        instance.cancel();
        
        instance.start(TIMEOUT);
        sleepNoTimeout();
        instance.cancel();
        
        assertTrue(!timeout);
    }
    
    public void testStartTimeout() {
        // Delay long enough to cause a timeout
        instance.start(TIMEOUT);
        sleepCauseTimeout();
        assertTrue(timeout);
        assertTrue(!instance.isStarted());
    }
    
    public void testDoubleStart() {
        // Test a double start, which is not allowed
        try {
            instance.start(TIMEOUT);
            sleepNoTimeout();
            instance.start(TIMEOUT);
            fail("Double start() should have thrown an exception");
        } catch (IllegalStateException e) {
            // Expected exception
        } finally {
            instance.cancel();
        }
    }
    
    public void testIllegalCancel() {
        // Test a standalone cancel, which is not allowed
        try {
            instance.cancel();
            fail("cancel() without start() should have thrown an exception");
        } catch (IllegalStateException e) {
            // Expected exception
        }
    }
    
    public void testIllegalKick() {
        // Test a standalone kick, which is not allowed
        try {
            instance.kick();
            fail("kick() without start() should have thrown an exception");
        } catch (IllegalStateException e) {
            // Expected exception
        }
    }
    
    public void testStartKickCancel() {
        // Test a long running operation that shouldn't cause a timeout
        // if the watchdog correctly response to kicks.
        instance.start(TIMEOUT);
        sleepNoTimeout();
        instance.kick();
        sleepNoTimeout();
        instance.cancel();
        assertTrue(!timeout);
    }
    
    public void testStartKickTimeout() {
        // Test a long running operation that should cause a timeout,
        // despite a kick.
        instance.start(TIMEOUT);
        sleepNoTimeout();
        instance.kick();
        sleepCauseTimeout();
        assertTrue(timeout);
        assertTrue(!instance.isStarted());
    }

    /**
     * Sleeps for 2/3 of the timeout period.
     * One call should not cause a timeout, but two of calls should.
     */
    private static void sleepNoTimeout() {
        // 2/3 of the timeout period.
        try { Thread.sleep(TIMEOUT / 2); } catch (InterruptedException e) { }
    }

    /**
     * Sleeps long enough to cause a timeout.
     */
    private static void sleepCauseTimeout() {
        try { Thread.sleep(TIMEOUT * 2); } catch (InterruptedException e) { }
    }
    
    public Test suite() {
        TestSuite suite = new TestSuite("WatchdogTest");
        
        suite.addTest(new WatchdogTest("startCancelImmediate", new TestMethod()
        { public void run(TestCase tc) throws Throwable {((WatchdogTest)tc).testStartCancelImmediate(); } }));
        suite.addTest(new WatchdogTest("startCancelDelay", new TestMethod()
        { public void run(TestCase tc) throws Throwable {((WatchdogTest)tc).testStartCancelDelay(); } }));
        suite.addTest(new WatchdogTest("startCancelMultiple", new TestMethod()
        { public void run(TestCase tc) throws Throwable {((WatchdogTest)tc).testStartCancelMultiple(); } }));
        suite.addTest(new WatchdogTest("startTimeout", new TestMethod()
        { public void run(TestCase tc) throws Throwable {((WatchdogTest)tc).testStartTimeout(); } }));
        suite.addTest(new WatchdogTest("doubleStart", new TestMethod()
        { public void run(TestCase tc) throws Throwable {((WatchdogTest)tc).testDoubleStart(); } }));
        suite.addTest(new WatchdogTest("illegalCancel", new TestMethod()
        { public void run(TestCase tc) throws Throwable {((WatchdogTest)tc).testIllegalCancel(); } }));
        suite.addTest(new WatchdogTest("illegalKick", new TestMethod()
        { public void run(TestCase tc) throws Throwable {((WatchdogTest)tc).testIllegalKick(); } }));
        suite.addTest(new WatchdogTest("startKickCancel", new TestMethod()
        { public void run(TestCase tc) throws Throwable {((WatchdogTest)tc).testStartKickCancel(); } }));
        suite.addTest(new WatchdogTest("startKickTimeout", new TestMethod()
        { public void run(TestCase tc) throws Throwable {((WatchdogTest)tc).testStartKickTimeout(); } }));

        return suite;
    }
}
