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

/**
 * Unit test for StringArrays
 */
public class StringArraysTest extends TestCase {
    public StringArraysTest() {
    }

    public StringArraysTest(String testName, TestMethod testMethod) {
        super(testName, testMethod);
    }

    public void setUp() {
    }

    public void tearDown() {
    }

    public void testParseInt() {
        assertEquals(4, StringArrays.parseInt("4".getBytes(), 0, 1));
        assertEquals(42, StringArrays.parseInt("42".getBytes(), 0, 2));
        assertEquals(42, StringArrays.parseInt("x42x".getBytes(), 1, 2));
        
        try {
            StringArrays.parseInt("x42x".getBytes(), 0, 4);
            fail("Did not throw exception");
        } catch (NumberFormatException e) { }
    }
    
    public void testParseHexInt() {
        assertEquals(0x0A, StringArrays.parseHexInt("A".getBytes(), 0, 1));
        assertEquals(0x4A, StringArrays.parseHexInt("4A".getBytes(), 0, 2));
        assertEquals(0x42, StringArrays.parseHexInt("x42x".getBytes(), 1, 2));
        
        try {
            StringArrays.parseInt("x42x".getBytes(), 0, 4);
            fail("Did not throw exception");
        } catch (NumberFormatException e) { }
    }
    
    public Test suite() {
        TestSuite testSuite = new TestSuite("StringArrays");
        
        testSuite.addTest(new StringArraysTest("parseInt", new TestMethod()
        { public void run(TestCase tc) { ((StringArraysTest) tc).testParseInt(); }}));
        testSuite.addTest(new StringArraysTest("parseHexInt", new TestMethod()
        { public void run(TestCase tc) { ((StringArraysTest) tc).testParseHexInt(); }}));
        
        return testSuite;
    }
}
