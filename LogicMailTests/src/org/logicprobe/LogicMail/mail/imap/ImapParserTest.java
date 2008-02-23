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

package org.logicprobe.LogicMail.mail.imap;

import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestMethod;
import j2meunit.framework.TestSuite;

/**
 * Unit test for ImapParser
 */
public class ImapParserTest extends TestCase {
    
    public ImapParserTest() {
    }

    public ImapParserTest(String testName, TestMethod testMethod) {
        super(testName, testMethod);
    }
    
    public void setUp() {
    }
    
    public void tearDown() {
    }
    
    public void testParseFolderName() {
        String result = ImapParser.parseFolderName("Hello");
        assertEquals("US-ASCII Test 1", "Hello", result);

        result = ImapParser.parseFolderName("Hello &- Goodbye");
        assertEquals("US-ASCII Test 2", "Hello & Goodbye", result);

        result = ImapParser.parseFolderName("Entw&APw-rfe");
        assertEquals("Umlaut Test 1", "Entwürfe", result);

        result = ImapParser.parseFolderName("Gel&APY-schte Objekte");
        assertEquals("Umlaut Test 2", "Gelöschte Objekte", result);
    }
    
    public Test suite() {
        TestSuite suite = new TestSuite("ImapParser");

        suite.addTest(new ImapParserTest("parseFolderName", new TestMethod()
        { public void run(TestCase tc) {((ImapParserTest)tc).testParseFolderName(); } }));

        return suite;
    }    
}
