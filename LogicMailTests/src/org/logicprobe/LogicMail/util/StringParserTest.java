/*-
 * Copyright (c) 2006, John Doe
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
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.TimeZone;
import java.util.Vector;

/**
 * Unit test for StringParser
 */
public class StringParserTest extends TestCase {

    /**
     * Test of parseDateString method, of class org.logicprobe.LogicMail.util.StringParser.
     */
    public void testParseDateString() {
        System.out.println("parseDateString");
        String rawDate;
        Date result;
        Calendar cal = Calendar.getInstance();
        
        rawDate = "Sat, 10 Feb 2007 21:27:01 -0800";
        result = StringParser.parseDateString(rawDate);
        cal.setTime(result);
        cal.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
        
        assertEquals("Test 1", 2007, cal.get(Calendar.YEAR));
        assertEquals("Test 1", 1, cal.get(Calendar.MONTH));
        assertEquals("Test 1", 10, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals("Test 1", 21, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals("Test 1", 27, cal.get(Calendar.MINUTE));
        assertEquals("Test 1", 1, cal.get(Calendar.SECOND));
        
        rawDate = "Sat, 10 Feb 2007 21:30:37 America/Los_Angeles";
        result = StringParser.parseDateString(rawDate);
        cal = Calendar.getInstance();
        cal.setTime(result);
        cal.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
        assertEquals("Test 2", 2007, cal.get(Calendar.YEAR));
        assertEquals("Test 2", 1, cal.get(Calendar.MONTH));
        assertEquals("Test 2", 10, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals("Test 2", 21, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals("Test 2", 30, cal.get(Calendar.MINUTE));
        assertEquals("Test 2", 37, cal.get(Calendar.SECOND));
    }

    /**
     * Test of createDateString method, of class org.logicprobe.LogicMail.util.StringParser.
     */
    public void testCreateDateString() {
        System.out.println("createDateString");
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"));
        
        cal.set(Calendar.YEAR, 2007);
        cal.set(Calendar.MONTH, 1);
        cal.set(Calendar.DAY_OF_MONTH, 10);
        cal.set(Calendar.HOUR_OF_DAY, 21);
        cal.set(Calendar.MINUTE, 27);
        cal.set(Calendar.SECOND, 1);
        
        String expected = "Sat, 10 Feb 2007 21:27:01 America/Los_Angeles";
        
        String actual = StringParser.createDateString(cal.getTime(), TimeZone.getTimeZone("America/Los_Angeles"));
        
        assertEquals(expected, actual);
    }
    
    /**
     * Test of nestedParenStringLexer method, of class org.logicprobe.LogicMail.util.StringParser.
     */
    public void testNestedParenStringLexerEnvelope() {
        System.out.println("nestedParenStringLexer (Envelope)");
        String rawText =
                "(FLAGS (\\Answered \\Seen) " +
                "ENVELOPE (\"Mon, 12 Mar 2007 19:38:31 -0700\" \"Re: Calm down! :-)\" " +
                "((\"jim smith\" NIL \"jsmith\" \"scratch.test\")) " +
                "((\"jim smith\" NIL \"jsmith\" \"scratch.test\")) " +
                "((\"jim smith\" NIL \"jsmith\" \"scratch.test\")) " +
                "((\"John Doe\" NIL \"jdoe\" \"generic.test\")) " +
                "NIL NIL " +
                "\"<200703121933.25327.jdoe@generic.test>\" " +
                "\"<7b02460f0703121938sff23a05xd3c2a37dc6b9eb7d@mail.scratch.test>\"))";

        Vector result = StringParser.nestedParenStringLexer(rawText);
        Vector temp;
        //printTree(result, 0);
        assertEquals("FLAGS", (String)result.elementAt(0));
        Vector flags = (Vector)result.elementAt(1);
        assertEquals("\\Answered", (String)flags.elementAt(0));
        assertEquals("\\Seen", (String)flags.elementAt(1));

        assertEquals("ENVELOPE", (String)result.elementAt(2));
        Vector envelope = (Vector)result.elementAt(3);
        assertEquals("Mon, 12 Mar 2007 19:38:31 -0700", (String)envelope.elementAt(0));
        assertEquals("Re: Calm down! :-)", (String)envelope.elementAt(1));
        
        temp = (Vector)envelope.elementAt(2);
        temp = (Vector)temp.elementAt(0);
        assertEquals("jim smith", (String)temp.elementAt(0));
        assertEquals("NIL", (String)temp.elementAt(1));
        assertEquals("jsmith", (String)temp.elementAt(2));
        assertEquals("scratch.test", (String)temp.elementAt(3));
        
        temp = (Vector)envelope.elementAt(3);
        temp = (Vector)temp.elementAt(0);
        assertEquals("jim smith", (String)temp.elementAt(0));
        assertEquals("NIL", (String)temp.elementAt(1));
        assertEquals("jsmith", (String)temp.elementAt(2));
        assertEquals("scratch.test", (String)temp.elementAt(3));

        temp = (Vector)envelope.elementAt(4);
        temp = (Vector)temp.elementAt(0);
        assertEquals("jim smith", (String)temp.elementAt(0));
        assertEquals("NIL", (String)temp.elementAt(1));
        assertEquals("jsmith", (String)temp.elementAt(2));
        assertEquals("scratch.test", (String)temp.elementAt(3));

        temp = (Vector)envelope.elementAt(5);
        temp = (Vector)temp.elementAt(0);
        assertEquals("John Doe", (String)temp.elementAt(0));
        assertEquals("NIL", (String)temp.elementAt(1));
        assertEquals("jdoe", (String)temp.elementAt(2));
        assertEquals("generic.test", (String)temp.elementAt(3));
        
        assertEquals("NIL", (String)envelope.elementAt(6));
        assertEquals("NIL", (String)envelope.elementAt(7));
        assertEquals("<200703121933.25327.jdoe@generic.test>", (String)envelope.elementAt(8));
        assertEquals("<7b02460f0703121938sff23a05xd3c2a37dc6b9eb7d@mail.scratch.test>", (String)envelope.elementAt(9));
    }
    
    /**
     * Test of nestedParenStringLexer method, of class org.logicprobe.LogicMail.util.StringParser.
     */
    public void testNestedParenStringLexerBodyStructure() {
        System.out.println("nestedParenStringLexer (BodyStructure)");
        String rawText =
                "(BODYSTRUCTURE " +
                "((\"TEXT\" \"PLAIN\" (\"CHARSET\" \"us-ascii\") NIL NIL \"7BIT\" 165 8 NIL NIL NIL) " +
                "(\"TEXT\" \"HTML\" (\"CHARSET\" \"us-ascii\") NIL NIL \"7BIT\" 627 10 NIL NIL NIL) " +
                "\"ALTERNATIVE\" (\"BOUNDARY\" \"Boundary-00=_y9RuEFduwo6YU42\") (\"INLINE\" NIL) NIL))";

        Vector result = StringParser.nestedParenStringLexer(rawText);
        Vector temp1;
        Vector temp2;
        Vector temp3;
        //printTree(result, 0);
        assertEquals("BODYSTRUCTURE", (String)result.elementAt(0));
        temp1 = (Vector)result.elementAt(1);
        
        temp2 = (Vector)temp1.elementAt(0);
        assertEquals("TEXT", (String)temp2.elementAt(0));
        assertEquals("PLAIN", (String)temp2.elementAt(1));
        temp3 = (Vector)temp2.elementAt(2);
        assertEquals("CHARSET", (String)temp3.elementAt(0));
        assertEquals("us-ascii", (String)temp3.elementAt(1));
        assertEquals("NIL", (String)temp2.elementAt(3));
        assertEquals("NIL", (String)temp2.elementAt(4));
        assertEquals("7BIT", (String)temp2.elementAt(5));
        assertEquals("165", (String)temp2.elementAt(6));
        assertEquals("8", (String)temp2.elementAt(7));
        assertEquals("NIL", (String)temp2.elementAt(8));
        assertEquals("NIL", (String)temp2.elementAt(9));
        assertEquals("NIL", (String)temp2.elementAt(10));
        
        temp2 = (Vector)temp1.elementAt(1);
        assertEquals("TEXT", (String)temp2.elementAt(0));
        assertEquals("HTML", (String)temp2.elementAt(1));
        temp3 = (Vector)temp2.elementAt(2);
        assertEquals("CHARSET", (String)temp3.elementAt(0));
        assertEquals("us-ascii", (String)temp3.elementAt(1));
        assertEquals("NIL", (String)temp2.elementAt(3));
        assertEquals("NIL", (String)temp2.elementAt(4));
        assertEquals("7BIT", (String)temp2.elementAt(5));
        assertEquals("627", (String)temp2.elementAt(6));
        assertEquals("10", (String)temp2.elementAt(7));
        assertEquals("NIL", (String)temp2.elementAt(8));
        assertEquals("NIL", (String)temp2.elementAt(9));
        assertEquals("NIL", (String)temp2.elementAt(10));
        
        assertEquals("ALTERNATIVE", (String)temp1.elementAt(2));

        temp2 = (Vector)temp1.elementAt(3);
        assertEquals("BOUNDARY", (String)temp2.elementAt(0));
        assertEquals("Boundary-00=_y9RuEFduwo6YU42", (String)temp2.elementAt(1));

        temp2 = (Vector)temp1.elementAt(4);
        assertEquals("INLINE", (String)temp2.elementAt(0));
        assertEquals("NIL", (String)temp2.elementAt(1));

        assertEquals("NIL", (String)temp1.elementAt(5));
    }
    
    private void printTree(Object node, int level) {
        if(node instanceof Vector) {
            Vector vec = (Vector)node;
            int size = vec.size();
            for(int i=0; i<size; i++)
                printTree(vec.elementAt(i), level + 1);
        }
        else {
            StringBuffer buf = new StringBuffer();
            buf.append(level+">");
            for(int i=0; i<level; i++)
                buf.append("    ");
            buf.append(node.toString());
            System.out.println(buf.toString());
        }
    }

    /**
     * Test of parseMailHeaders method, of class org.logicprobe.LogicMail.util.StringParser.
     */
    public void testParseMailHeaders() {
        System.out.println("parseMailHeaders");
        String[] rawLines = {
            "Return-Path: <jdoe@generic.test>",
            "Received: from hyperion.generic.test ([unix socket])",
            "         by hyperion.generic.test (Cyrus v2.3.0) with LMTPA;",
            "         Sun, 24 Dec 2006 20:59:45 -0500",
            "X-Sieve: CMU Sieve 2.3",
            "From: John Doe <jdoe@generic.test>",
            "To: neo@generic.test",
            "Subject: Some test message",
            "Date: Sun, 24 Dec 2006 20:59:41 -0500",
            "User-Agent: KMail/1.9.1",
            "MIME-Version: 1.0",
            "Content-Type: text/plain;",
            "  charset=\"utf-8\"",
            "Content-Transfer-Encoding: base64",
            "Content-Disposition: inline",
            "Message-Id: <200612242059.42552.jdoe@generic.test>",
            "X-Scanned-By: MIMEDefang 2.54 on 10.4.1.12"
        };
        
        Hashtable result = StringParser.parseMailHeaders(rawLines);

        // Print actual contents for debugging
//        for(java.util.Enumeration e = result.keys(); e.hasMoreElements();) {
//            String key = (String)e.nextElement();
//            System.err.println("-->"+key+" = "+result.get(key));
//        }
        
        assertEquals("Number of headers", 14, result.size());
        assertEquals("<jdoe@generic.test>", result.get("return-path"));
        assertEquals("from hyperion.generic.test ([unix socket])\r\n         by hyperion.generic.test (Cyrus v2.3.0) with LMTPA;\r\n         Sun, 24 Dec 2006 20:59:45 -0500", result.get("received"));
        assertEquals("CMU Sieve 2.3", result.get("x-sieve"));
        assertEquals("John Doe <jdoe@generic.test>", result.get("from"));
        assertEquals("neo@generic.test", result.get("to"));
        assertEquals("Some test message", result.get("subject"));
        assertEquals("Sun, 24 Dec 2006 20:59:41 -0500", result.get("date"));
        assertEquals("KMail/1.9.1", result.get("user-agent"));
        assertEquals("1.0", result.get("mime-version"));
        assertEquals("text/plain;\r\n  charset=\"utf-8\"", result.get("content-type"));
        assertEquals("base64", result.get("content-transfer-encoding"));
        assertEquals("inline", result.get("content-disposition"));
        assertEquals("<200612242059.42552.jdoe@generic.test>", result.get("message-id"));
        assertEquals("MIMEDefang 2.54 on 10.4.1.12", result.get("x-scanned-by"));
    }

    /**
     * Test of parseTokenString method, of class org.logicprobe.LogicMail.util.StringParser.
     */
    public void testParseTokenString() {
        System.out.println("parseTokenString");
        String text = "one.red, two.green, three.blue";
        String token = ", ";

        String[] expectedResult = {"one.red", "two.green", "three.blue"};
        String[] result = StringParser.parseTokenString(text, token);
        assertEquals("First test failed", expectedResult, result);

        token = ".";
        expectedResult = new String[] {"one", "red, two", "green, three", "blue"};
        result = StringParser.parseTokenString(text, token);
        assertEquals("Second test failed", expectedResult, result);
    }

    /**
     * Special assertion for arrays
     */
    private void assertEquals(String message, Object[] expected, Object[] actual) {
        assertEquals(message, expected.length, actual.length);
        for(int i=0; i<expected.length; i++) {
            assertEquals(message, expected[i], actual[i]);
        }
    }
    
    /**
     * Test of parseValidCharsetString method, of class org.logicprobe.LogicMail.util.StringParser.
     */
    public void testParseValidCharsetString() {
        System.out.println("parseValidCharsetString");
    
        this.assertEquals("Accepting bad input",
                "ISO-8859-1", StringParser.parseValidCharsetString("foo"));
        this.assertEquals("Accepting ISO-8859-1",
                "ISO-8859-1", StringParser.parseValidCharsetString("iso-8859-1"));
        this.assertEquals("Accepting UTF-8",
                "UTF-8", StringParser.parseValidCharsetString("utf-8"));
        this.assertEquals("Accepting UTF-16BE",
                "UTF-16BE", StringParser.parseValidCharsetString("utf-16be"));
        this.assertEquals("Accepting US-ASCII",
                "US-ASCII", StringParser.parseValidCharsetString("us-ascii"));
    }

    /**
     * Test of createInputStream method, of class org.logicprobe.LogicMail.util.StringParser.
     */
    public void testCreateInputStream() {
        System.out.println("createInputStream");
//        java.lang.String[] rawLines = null;
//        org.logicprobe.LogicMail.util.StringParser instance = null;
//        java.io.InputStream expectedResult = null;
//        java.io.InputStream result = instance.createInputStream(rawLines);
//        assertEquals(expectedResult, result);
        
        //TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of readWholeStream method, of class org.logicprobe.LogicMail.util.StringParser.
     */
    public void testReadWholeStream() {
        System.out.println("readWholeStream");
//        java.io.InputStream is = null;
//        org.logicprobe.LogicMail.util.StringParser instance = null;
//        byte[] expectedResult = null;
//        byte[] result = instance.readWholeStream(is);
//        assertEquals(expectedResult, result);
        
        //TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of decodeQuotedPrintable method, of class org.logicprobe.LogicMail.util.StringParser.
     */
    public void testDecodeQuotedPrintable() {
        System.out.println("decodeQuotedPrintable");
        String text = "=A1Hol=E1 Se=F1or!";
        String expectedResult = "¡Holá Señor!";
        String result = StringParser.decodeQuotedPrintable(text);
        assertEquals(expectedResult, result);
    }

    /**
     * Test of encodeQuotedPrintable method, of class org.logicprobe.LogicMail.util.StringParser.
     */
    public void testEncodeQuotedPrintable() {
        System.out.println("encodeQuotedPrintable");
        String text = "¡Holá Señor!";
        String expectedResult = "=A1Hol=E1 Se=F1or!".toLowerCase();
        String result = StringParser.encodeQuotedPrintable(text).toLowerCase();
        assertEquals(expectedResult, result);
    }

    public StringParserTest() {
    }

    public StringParserTest(String testName, TestMethod testMethod) {
        super(testName, testMethod);
    }
    
    public void setUp() {
    }

    public void tearDown() {
    }

    public Test suite() {
        TestSuite testSuite = new TestSuite("StringParser");
        testSuite.addTest(new StringParserTest("parseDateString", new TestMethod() {
            public void run(TestCase tc) {
                ((StringParserTest)tc).testParseDateString();
        }}));
        testSuite.addTest(new StringParserTest("createDateString", new TestMethod() {
            public void run(TestCase tc) {
                ((StringParserTest)tc).testCreateDateString();
        }}));
        testSuite.addTest(new StringParserTest("nestedParenStringLexerEnvelope", new TestMethod() {
            public void run(TestCase tc) {
                ((StringParserTest)tc).testNestedParenStringLexerEnvelope();
        }}));
        testSuite.addTest(new StringParserTest("nestedParenStringLexerBodyStructure", new TestMethod() {
            public void run(TestCase tc) {
                ((StringParserTest)tc).testNestedParenStringLexerBodyStructure();
        }}));
        testSuite.addTest(new StringParserTest("parseMailHeaders", new TestMethod() {
            public void run(TestCase tc) {
                ((StringParserTest)tc).testParseMailHeaders();
        }}));
        testSuite.addTest(new StringParserTest("parseTokenString", new TestMethod() {
            public void run(TestCase tc) {
                ((StringParserTest)tc).testParseTokenString();
        }}));
        testSuite.addTest(new StringParserTest("parseValidCharsetString", new TestMethod() {
            public void run(TestCase tc) {
                ((StringParserTest)tc).testParseValidCharsetString();
        }}));
//        testSuite.addTest(new StringParserTest("createInputStream", new TestMethod() {
//            public void run(TestCase tc) {
//                ((StringParserTest)tc).testCreateInputStream();
//        }}));
//        testSuite.addTest(new StringParserTest("readWholeStream", new TestMethod() {
//            public void run(TestCase tc) {
//                ((StringParserTest)tc).testReadWholeStream();
//        }}));
        testSuite.addTest(new StringParserTest("decodeQuotedPrintable", new TestMethod() {
            public void run(TestCase tc) {
                ((StringParserTest)tc).testDecodeQuotedPrintable();
        }}));
        testSuite.addTest(new StringParserTest("encodeQuotedPrintable", new TestMethod() {
            public void run(TestCase tc) {
                ((StringParserTest)tc).testEncodeQuotedPrintable();
        }}));
        return testSuite;
    }
}
