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
package org.logicprobe.LogicMail.mail.imap;

import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestMethod;
import j2meunit.framework.TestSuite;

public class ImapResponseLineTesterTest extends TestCase {
    private ImapResponseLineTester instance;
    
    public ImapResponseLineTesterTest() {
    }

    public ImapResponseLineTesterTest(String testName, TestMethod testMethod) {
        super(testName, testMethod);
    }

    public void setUp() {
        instance = new ImapResponseLineTester();
    }

    public void tearDown() {
        instance = null;
    }

    public void testNormalLines() {
        byte[] input = "Hello World\r\n".getBytes();
        int responseLength = instance.checkForCompleteResponse(input, input.length);
        int trimCount = instance.trimCount();
        assertEquals("Case 1", input.length, responseLength);
        assertEquals("Case 1", 2, trimCount);
        
        input = "Hello World\n".getBytes();
        responseLength = instance.checkForCompleteResponse(input, input.length);
        trimCount = instance.trimCount();
        assertEquals("Case 2", input.length, responseLength);
        assertEquals("Case 2", 1, trimCount);
        
        input = "Hello\r\nWorld\r\n".getBytes();
        responseLength = instance.checkForCompleteResponse(input, input.length);
        trimCount = instance.trimCount();
        assertEquals("Case 3", 7, responseLength); // Length of "Hello\r\n"
        assertEquals("Case 3", 2, trimCount);
    
        input = "Hello\nWorld\r\n".getBytes();
        responseLength = instance.checkForCompleteResponse(input, input.length);
        trimCount = instance.trimCount();
        assertEquals("Case 4", 6, responseLength); // Length of "Hello\n"
        assertEquals("Case 4", 1, trimCount);
    }
    
    public void testNormalLinesShortLength() {
        byte[] input = "Hello World\r\n".getBytes();
        int responseLength = instance.checkForCompleteResponse(input, 4);
        assertEquals("Case 1", -1, responseLength);
        
        input = "Hello World\n".getBytes();
        responseLength = instance.checkForCompleteResponse(input, 4);
        assertEquals("Case 2", -1, responseLength);

        input = "Hello\r\nWorld\r\n".getBytes();
        responseLength = instance.checkForCompleteResponse(input, 4);
        assertEquals("Case 3", -1, responseLength);
    
        input = "Hello\nWorld\r\n".getBytes();
        responseLength = instance.checkForCompleteResponse(input, 4);
        assertEquals("Case 4", -1, responseLength);
    }
    
    public void testLinesWithLiteral() {
        byte[] input = "Hello {5}\r\nWorld\r\n".getBytes();
        int responseLength = instance.checkForCompleteResponse(input, input.length);
        int trimCount = instance.trimCount();
        assertEquals("Case 1", input.length, responseLength);
        assertEquals("Case 1", 2, trimCount);
        
        input = "Hello {33}\r\nWorld Of Fun And Stuff And Things\r\n".getBytes();
        responseLength = instance.checkForCompleteResponse(input, input.length);
        trimCount = instance.trimCount();
        assertEquals("Case 2", input.length, responseLength);
        assertEquals("Case 2", 2, trimCount);
        
        byte[] input1 = "Hello {6}\r\n".getBytes();
        byte[] input2 = new byte[] {
                (byte)0xC4, (byte)0x80, (byte)0xC4, (byte)0x81, (byte)0xC4, (byte)0x82,
                '\r', '\n' };
        input = new byte[input1.length + input2.length];
        int j = 0;
        for(int i=0; i<input1.length; i++) {
            input[j++] = input1[i];
        }
        for(int i=0; i<input2.length; i++) {
            input[j++] = input2[i];
        }
        
        responseLength = instance.checkForCompleteResponse(input, input.length);
        trimCount = instance.trimCount();
        assertEquals("Case 3", input.length, responseLength);
        assertEquals("Case 3", 2, trimCount);

        // Zero-length literal
        input = "* 65 FETCH (UID 346 BODY[1] {0}\r\n)\r\n".getBytes();
        responseLength = instance.checkForCompleteResponse(input, input.length);
        trimCount = instance.trimCount();
        assertEquals("Case 4", input.length, responseLength);
        assertEquals("Case 4", 2, trimCount);
        
        // Zero-length literal with following data
        input = "((UID 346 BODY[1] {0}\r\n) (\\Foo))\r\n".getBytes();
        responseLength = instance.checkForCompleteResponse(input, input.length);
        trimCount = instance.trimCount();
        assertEquals("Case 5", input.length, responseLength);
        assertEquals("Case 5", 2, trimCount);
    }
    
    public void testLinesWithMalformedLiteral() {
        byte[] input = "Hello {5\r\nWorld\r\n".getBytes();
        int responseLength = instance.checkForCompleteResponse(input, input.length);
        int trimCount = instance.trimCount();
        assertEquals("Case 1", 10, responseLength); // "Hello {5\r\n"
        assertEquals("Case 1", 2, trimCount);
        
        input = "Hello 5}\r\nWorld\r\n".getBytes();
        responseLength = instance.checkForCompleteResponse(input, input.length);
        trimCount = instance.trimCount();
        assertEquals("Case 2", 10, responseLength); // "Hello 5}\r\n"
        assertEquals("Case 2", 2, trimCount);
        
        input = "Hello {5{\r\nWorld\r\n".getBytes();
        responseLength = instance.checkForCompleteResponse(input, input.length);
        trimCount = instance.trimCount();
        assertEquals("Case 3", 11, responseLength); // "Hello {5{\r\n"
        assertEquals("Case 3", 2, trimCount);
        
        input = "Hello }5}\r\nWorld\r\n".getBytes();
        responseLength = instance.checkForCompleteResponse(input, input.length);
        trimCount = instance.trimCount();
        assertEquals("Case 4", 11, responseLength); // "Hello {5{\r\n"
        assertEquals("Case 4", 2, trimCount);
        
        input = "Hello {}\r\nWorld\r\n".getBytes();
        responseLength = instance.checkForCompleteResponse(input, input.length);
        trimCount = instance.trimCount();
        assertEquals("Case 5", 10, responseLength); // "Hello {}\r\n"
        assertEquals("Case 5", 2, trimCount);
        
        input = "Hello {4X}\r\nWorld\r\n".getBytes();
        responseLength = instance.checkForCompleteResponse(input, input.length);
        trimCount = instance.trimCount();
        assertEquals("Case 6", 12, responseLength); // "Hello {4X}\r\n"
        assertEquals("Case 6", 2, trimCount);
        
        input = "Hello {XZ}\r\nWorld\r\n".getBytes();
        responseLength = instance.checkForCompleteResponse(input, input.length);
        trimCount = instance.trimCount();
        assertEquals("Case 7", 12, responseLength); // "Hello {XZ}\r\n"
        assertEquals("Case 7", 2, trimCount);
    }
    
    public void testMultipleChecks() {
        byte[] input = "Hello World\r\n".getBytes();
        int responseLength = instance.checkForCompleteResponse(input, 4);
        assertEquals("Case 1", -1, responseLength);
        responseLength = instance.checkForCompleteResponse(input, input.length);
        int trimCount = instance.trimCount();
        assertEquals("Case 1", input.length, responseLength);
        assertEquals("Case 1", 2, trimCount);
        
        input = "Hello World\r\n".getBytes();
        responseLength = instance.checkForCompleteResponse(input, input.length - 1);
        assertEquals("Case 2", -1, responseLength);
        responseLength = instance.checkForCompleteResponse(input, input.length);
        trimCount = instance.trimCount();
        assertEquals("Case 2", input.length, responseLength);
        assertEquals("Case 2", 2, trimCount);
    }

    public void testMultipleChecksWithLiteral() {
        byte[] input = "Hello {33}\r\nWorld Of Fun And Stuff And Things\r\n".getBytes();
        int responseLength = instance.checkForCompleteResponse(input, 7);
        assertEquals("Hello {", -1, responseLength);

        responseLength = instance.checkForCompleteResponse(input, 9);
        assertEquals("Hello {33", -1, responseLength);

        responseLength = instance.checkForCompleteResponse(input, 10);
        assertEquals("Hello {33}", -1, responseLength);
        
        responseLength = instance.checkForCompleteResponse(input, 12);
        assertEquals("Hello {33}\\r\\n", -1, responseLength);
        
        responseLength = instance.checkForCompleteResponse(input, input.length);
        assertEquals("Complete", input.length, responseLength);
        int trimCount = instance.trimCount();
        assertEquals("Complete", 2, trimCount);
    }
    
    public void testLinesWithQuoted() {
        byte[] input = "\"Hello World\"\r\nFoo\r\n".getBytes();
        int responseLength = instance.checkForCompleteResponse(input, input.length);
        int trimCount = instance.trimCount();
        assertEquals("Case 1", 15, responseLength); // Length of '"Hello World"\r\n'
        assertEquals("Case 1", 2, trimCount);
        
        input = "\"Hello \\\"Foo\\\\Bar\\\" World\"\r\nFoo\r\n".getBytes();
        responseLength = instance.checkForCompleteResponse(input, input.length);
        trimCount = instance.trimCount();
        assertEquals("Case 2", 28, responseLength); // Length of '"Hello \"Foo\\Bar\" World"\r\n'
        assertEquals("Case 2", 2, trimCount);
    }
    
    public void testLinesWithMalformedQuoted() {
        // This tests for a case that is technically invalid according to the
        // IMAP grammar, but an actual server has been observed to produce it.
        byte[] input = "\"Hello\n World\"\r\nFoo\r\n".getBytes();
        int responseLength = instance.checkForCompleteResponse(input, input.length);
        int trimCount = instance.trimCount();
        assertEquals("Case 1", 16, responseLength); // Length of '"Hello\n World"\r\n'
        assertEquals("Case 1", 2, trimCount);
        
        input = "\"Hello\r\n World\"\r\nFoo\r\n".getBytes();
        responseLength = instance.checkForCompleteResponse(input, input.length);
        trimCount = instance.trimCount();
        assertEquals("Case 2", 17, responseLength); // Length of '"Hello\r\n World"\r\n'
        assertEquals("Case 2", 2, trimCount);
    }
    
    public Test suite() {
        TestSuite suite = new TestSuite("ImapResponseLineTester");

        suite.addTest(new ImapResponseLineTesterTest("normalLines", new TestMethod()
        { public void run(TestCase tc) { ((ImapResponseLineTesterTest) tc).testNormalLines(); }}));
        suite.addTest(new ImapResponseLineTesterTest("normalLinesShortLength", new TestMethod()
        { public void run(TestCase tc) { ((ImapResponseLineTesterTest) tc).testNormalLinesShortLength(); }}));
        suite.addTest(new ImapResponseLineTesterTest("linesWithLiteral", new TestMethod()
        { public void run(TestCase tc) { ((ImapResponseLineTesterTest) tc).testLinesWithLiteral(); }}));
        suite.addTest(new ImapResponseLineTesterTest("linesWithMalformedLiteral", new TestMethod()
        { public void run(TestCase tc) { ((ImapResponseLineTesterTest) tc).testLinesWithMalformedLiteral(); }}));
        suite.addTest(new ImapResponseLineTesterTest("multipleChecks", new TestMethod()
        { public void run(TestCase tc) { ((ImapResponseLineTesterTest) tc).testMultipleChecks(); }}));
        suite.addTest(new ImapResponseLineTesterTest("multipleChecksWithLiteral", new TestMethod()
        { public void run(TestCase tc) { ((ImapResponseLineTesterTest) tc).testMultipleChecksWithLiteral(); }}));
        suite.addTest(new ImapResponseLineTesterTest("linesWithQuoted", new TestMethod()
        { public void run(TestCase tc) { ((ImapResponseLineTesterTest) tc).testLinesWithQuoted(); }}));
        suite.addTest(new ImapResponseLineTesterTest("linesWithMalformedQuoted", new TestMethod()
        { public void run(TestCase tc) { ((ImapResponseLineTesterTest) tc).testLinesWithMalformedQuoted(); }}));

        return suite;
    }
}
