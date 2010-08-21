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

import java.util.Vector;


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

    public void testParenStringLexer1() {
        String rawText = "(FLAGS (\\Answered \\Seen) " +
            "ENVELOPE (\"Mon, 12 Mar 2007 19:38:31 -0700\" \"Re: Calm down! :-)\" " +
            "((\"jim smith\" NIL \"jsmith\" \"scratch.test\")) " +
            "((\"jim smith\" NIL \"jsmith\" \"scratch.test\")) " +
            "((\"jim smith\" NIL \"jsmith\" \"scratch.test\")) " +
            "((\"John Doe\" NIL \"jdoe\" \"generic.test\")) " + "NIL NIL " +
            "\"<200703121933.25327.jdoe@generic.test>\" " +
            "\"<7b02460f0703121938sff23a05xd3c2a37dc6b9eb7d@mail.scratch.test>\"))";

        byte[] data = rawText.getBytes();
        Vector result = ImapParser.parenListLexer(data, 0, data.length);
        assertNotNull(result);

        Character LPAREN = new Character('(');
        Character RPAREN = new Character(')');

        Object[] expected = new Object[] {
                LPAREN, "FLAGS", LPAREN, "\\Answered", "\\Seen", RPAREN,
                "ENVELOPE", LPAREN, "Mon, 12 Mar 2007 19:38:31 -0700".getBytes(),
                "Re: Calm down! :-)".getBytes(), LPAREN, LPAREN, "jim smith".getBytes(), "NIL",
                "jsmith".getBytes(), "scratch.test".getBytes(), RPAREN, RPAREN, LPAREN, LPAREN,
                "jim smith".getBytes(), "NIL", "jsmith".getBytes(), "scratch.test".getBytes(), RPAREN, RPAREN,
                LPAREN, LPAREN, "jim smith".getBytes(), "NIL", "jsmith".getBytes(), "scratch.test".getBytes(),
                RPAREN, RPAREN, LPAREN, LPAREN, "John Doe".getBytes(), "NIL", "jdoe".getBytes(),
                "generic.test".getBytes(), RPAREN, RPAREN, "NIL", "NIL",
                "<200703121933.25327.jdoe@generic.test>".getBytes(),
                "<7b02460f0703121938sff23a05xd3c2a37dc6b9eb7d@mail.scratch.test>".getBytes(),
                RPAREN, RPAREN
            };

        assertEquals(expected.length, result.size());

        for (int i = 0; i < expected.length; i++) {
            if(expected[i] instanceof byte[] && result.elementAt(i) instanceof byte[]) {
                assertEquals(new String((byte[])expected[i]), new String((byte[])result.elementAt(i)));
            }
            else {
                assertEquals(expected[i], result.elementAt(i));
            }
        }
    }

    public void testParenStringLexer2() {
        String rawText = "(FLAGS () ENVELOPE (\"Sun, 18 Mar 2007 09:04:29 -0700\" {23}\r\n" +
            "[list] \"this is a test\" " +
            "((\"Jim Smith\" NIL \"jsmith\" \"XXXX\")) " +
            "((\"Jim Smith\" NIL \"jsmith\" \"XXXX\")) " +
            "((\"Jim Smith\" NIL \"jsmith\" \"XXXX\")) " +
            "((NIL NIL \"jsmith\" \"XXXXXXXX\")) " +
            "NIL NIL NIL \"<45FD630D.1040808@XXXXX>\"))";

        byte[] data = rawText.getBytes();
        Vector result = ImapParser.parenListLexer(data, 0, data.length);
        assertNotNull(result);

        Character LPAREN = new Character('(');
        Character RPAREN = new Character(')');

        Object[] expected = new Object[] {
                LPAREN, "FLAGS", LPAREN, RPAREN, "ENVELOPE", LPAREN,
                "Sun, 18 Mar 2007 09:04:29 -0700".getBytes(), "[list] \"this is a test\"".getBytes(),
                LPAREN, LPAREN, "Jim Smith".getBytes(), "NIL", "jsmith".getBytes(), "XXXX".getBytes(), RPAREN,
                RPAREN, LPAREN, LPAREN, "Jim Smith".getBytes(), "NIL", "jsmith".getBytes(), "XXXX".getBytes(),
                RPAREN, RPAREN, LPAREN, LPAREN, "Jim Smith".getBytes(), "NIL", "jsmith".getBytes(),
                "XXXX".getBytes(), RPAREN, RPAREN, LPAREN, LPAREN, "NIL", "NIL", "jsmith".getBytes(),
                "XXXXXXXX".getBytes(), RPAREN, RPAREN, "NIL", "NIL", "NIL",
                "<45FD630D.1040808@XXXXX>".getBytes(), RPAREN, RPAREN
            };

        assertEquals(expected.length, result.size());

        for (int i = 0; i < expected.length; i++) {
            if(expected[i] instanceof byte[] && result.elementAt(i) instanceof byte[]) {
                assertEquals(new String((byte[])expected[i]), new String((byte[])result.elementAt(i)));
            }
            else {
                assertEquals(expected[i], result.elementAt(i));
            }
        }
    }

    //    /**
    //     * Prints the token list for debugging purposes
    //     *
    //     * @param tokenList the token list
    //     */
    //    private void printTokenList(Vector tokenList) {
    //        int size = tokenList.size();
    //        System.err.println("-->");
    //        for(int i=0; i<size; i++) {
    //            Object element = tokenList.elementAt(i);
    //            if(element instanceof Character) {
    //                System.err.print("'" + element + "' ");
    //            }
    //            else if(element instanceof String) {
    //                System.err.print("[" + element + "] ");
    //            }
    //            else if(element == null) {
    //                System.err.print("<NIL> ");
    //            }
    //            else {
    //                System.err.println("ERROR!!!!");
    //            }
    //        }
    //        System.err.println();
    //    }
    public void testParenStringParserEnvelope1() {
        String rawText = "(FLAGS (\\Answered \\Seen) " +
            "ENVELOPE (\"Mon, 12 Mar 2007 19:38:31 -0700\" \"Re: Calm down! :-)\" " +
            "((\"jim smith\" NIL \"jsmith\" \"scratch.test\")) " +
            "((\"jim smith\" NIL \"jsmith\" \"scratch.test\")) " +
            "((\"jim smith\" NIL \"jsmith\" \"scratch.test\")) " +
            "((\"John Doe\" NIL \"jdoe\" \"generic.test\")) " + "NIL NIL " +
            "\"<200703121933.25327.jdoe@generic.test>\" " +
            "\"<7b02460f0703121938sff23a05xd3c2a37dc6b9eb7d@mail.scratch.test>\"))";

        Vector result = ImapParser.parenListParser(rawText.getBytes());
        Vector temp;

        //printTree(result, 0);
        assertEquals("FLAGS", (String) result.elementAt(0));

        Vector flags = (Vector) result.elementAt(1);
        assertEquals("\\Answered", (String) flags.elementAt(0));
        assertEquals("\\Seen", (String) flags.elementAt(1));

        assertEquals("ENVELOPE", (String) result.elementAt(2));

        Vector envelope = (Vector) result.elementAt(3);
        assertNotNull(envelope);
        assertEquals("Mon, 12 Mar 2007 19:38:31 -0700",
            (byte[]) envelope.elementAt(0));
        assertEquals("Re: Calm down! :-)", (byte[]) envelope.elementAt(1));

        temp = (Vector) envelope.elementAt(2);
        temp = (Vector) temp.elementAt(0);
        assertEquals("jim smith", (byte[]) temp.elementAt(0));
        assertEquals("NIL", (String) temp.elementAt(1));
        assertEquals("jsmith", (byte[]) temp.elementAt(2));
        assertEquals("scratch.test", (byte[]) temp.elementAt(3));

        temp = (Vector) envelope.elementAt(3);
        temp = (Vector) temp.elementAt(0);
        assertEquals("jim smith", (byte[]) temp.elementAt(0));
        assertEquals("NIL", (String) temp.elementAt(1));
        assertEquals("jsmith", (byte[]) temp.elementAt(2));
        assertEquals("scratch.test", (byte[]) temp.elementAt(3));

        temp = (Vector) envelope.elementAt(4);
        temp = (Vector) temp.elementAt(0);
        assertEquals("jim smith", (byte[]) temp.elementAt(0));
        assertEquals("NIL", (String) temp.elementAt(1));
        assertEquals("jsmith", (byte[]) temp.elementAt(2));
        assertEquals("scratch.test", (byte[]) temp.elementAt(3));

        temp = (Vector) envelope.elementAt(5);
        temp = (Vector) temp.elementAt(0);
        assertEquals("John Doe", (byte[]) temp.elementAt(0));
        assertEquals("NIL", (String) temp.elementAt(1));
        assertEquals("jdoe", (byte[]) temp.elementAt(2));
        assertEquals("generic.test", (byte[]) temp.elementAt(3));

        assertEquals("NIL", (String) envelope.elementAt(6));
        assertEquals("NIL", (String) envelope.elementAt(7));
        assertEquals("<200703121933.25327.jdoe@generic.test>",
            (byte[]) envelope.elementAt(8));
        assertEquals("<7b02460f0703121938sff23a05xd3c2a37dc6b9eb7d@mail.scratch.test>",
            (byte[]) envelope.elementAt(9));
    }

    public void testParenStringParserEnvelope2() {
        String rawText = "(FLAGS () ENVELOPE (\"Sun, 18 Mar 2007 09:04:29 -0700\" {23}\r\n" +
            "[list] \"this is a test\" " +
            "((\"Jim Smith\" NIL \"jsmith\" \"XXXX\")) " +
            "((\"Jim Smith\" NIL \"jsmith\" \"XXXX\")) " +
            "((\"Jim Smith\" NIL \"jsmith\" \"XXXX\")) " +
            "((NIL NIL \"jsmith\" \"XXXXXXXX\")) " +
            "NIL NIL NIL \"<45FD630D.1040808@XXXXX>\"))";

        Vector result = ImapParser.parenListParser(rawText.getBytes());
        Vector temp;

        //printTree(result, 0);
        assertEquals("FLAGS", (String) result.elementAt(0));

        Vector flags = (Vector) result.elementAt(1);
        assertEquals(0, flags.size());

        assertEquals("ENVELOPE", (String) result.elementAt(2));

        Vector envelope = (Vector) result.elementAt(3);
        assertNotNull(envelope);
        assertEquals("Sun, 18 Mar 2007 09:04:29 -0700",
            (byte[]) envelope.elementAt(0));
        assertEquals("[list] \"this is a test\"", (byte[]) envelope.elementAt(1));

        temp = (Vector) envelope.elementAt(2);
        temp = (Vector) temp.elementAt(0);
        assertEquals("Jim Smith", (byte[]) temp.elementAt(0));
        assertEquals("NIL", (String) temp.elementAt(1));
        assertEquals("jsmith", (byte[]) temp.elementAt(2));
        assertEquals("XXXX", (byte[]) temp.elementAt(3));

        temp = (Vector) envelope.elementAt(3);
        temp = (Vector) temp.elementAt(0);
        assertEquals("Jim Smith", (byte[]) temp.elementAt(0));
        assertEquals("NIL", (String) temp.elementAt(1));
        assertEquals("jsmith", (byte[]) temp.elementAt(2));
        assertEquals("XXXX", (byte[]) temp.elementAt(3));

        temp = (Vector) envelope.elementAt(4);
        temp = (Vector) temp.elementAt(0);
        assertEquals("Jim Smith", (byte[]) temp.elementAt(0));
        assertEquals("NIL", (String) temp.elementAt(1));
        assertEquals("jsmith", (byte[]) temp.elementAt(2));
        assertEquals("XXXX", (byte[]) temp.elementAt(3));

        temp = (Vector) envelope.elementAt(5);
        temp = (Vector) temp.elementAt(0);
        assertEquals("NIL", (String) temp.elementAt(0));
        assertEquals("NIL", (String) temp.elementAt(1));
        assertEquals("jsmith", (byte[]) temp.elementAt(2));
        assertEquals("XXXXXXXX", (byte[]) temp.elementAt(3));

        assertEquals("NIL", (String) envelope.elementAt(6));
        assertEquals("NIL", (String) envelope.elementAt(7));
        assertEquals("NIL", (String) envelope.elementAt(8));
        assertEquals("<45FD630D.1040808@XXXXX>", (byte[]) envelope.elementAt(9));
    }

    public void testParenStringParserEnvelope3() {
        String rawText = "(FLAGS (\\Seen) " +
            "ENVELOPE (\"Fri, 15 Jun 2007 12:37:27 -0400\" {42}\r\n" +
            "[Theelist] 19\" monitor free to a good home " +
            "((\"Jim Smith\" NIL \"jsmith\" \"smith.test\")) " +
            "((NIL NIL \"thelist-bounces\" \"thelist.test\")) " +
            "((\"This is the list\" NIL \"thelist\" \"thelist.test\")) " +
            "((\"This is the list\" NIL \"thelist\" \"thelist.test\")) " +
            "NIL NIL NIL " +
            "\"<1ECCDABD-5242-4180-9584-E5873C3FEA17@smith.test>\"))";

        Vector result = ImapParser.parenListParser(rawText.getBytes());
        Vector temp;

        //printTree(result, 0);
        assertEquals("FLAGS", (String) result.elementAt(0));

        Vector flags = (Vector) result.elementAt(1);
        assertEquals("\\Seen", (String) flags.elementAt(0));

        assertEquals("ENVELOPE", (String) result.elementAt(2));

        Vector envelope = (Vector) result.elementAt(3);
        assertNotNull(envelope);
        assertEquals("Fri, 15 Jun 2007 12:37:27 -0400",
            (byte[]) envelope.elementAt(0));
        assertEquals("[Theelist] 19\" monitor free to a good home",
            (byte[]) envelope.elementAt(1));

        temp = (Vector) envelope.elementAt(2);
        temp = (Vector) temp.elementAt(0);
        assertEquals("Jim Smith", (byte[]) temp.elementAt(0));
        assertEquals("NIL", (String) temp.elementAt(1));
        assertEquals("jsmith", (byte[]) temp.elementAt(2));
        assertEquals("smith.test", (byte[]) temp.elementAt(3));

        temp = (Vector) envelope.elementAt(3);
        temp = (Vector) temp.elementAt(0);
        assertEquals("NIL", (String) temp.elementAt(0));
        assertEquals("NIL", (String) temp.elementAt(1));
        assertEquals("thelist-bounces", (byte[]) temp.elementAt(2));
        assertEquals("thelist.test", (byte[]) temp.elementAt(3));

        temp = (Vector) envelope.elementAt(4);
        temp = (Vector) temp.elementAt(0);
        assertEquals("This is the list", (byte[]) temp.elementAt(0));
        assertEquals("NIL", (String) temp.elementAt(1));
        assertEquals("thelist", (byte[]) temp.elementAt(2));
        assertEquals("thelist.test", (byte[]) temp.elementAt(3));

        temp = (Vector) envelope.elementAt(5);
        temp = (Vector) temp.elementAt(0);
        assertEquals("This is the list", (byte[]) temp.elementAt(0));
        assertEquals("NIL", (String) temp.elementAt(1));
        assertEquals("thelist", (byte[]) temp.elementAt(2));
        assertEquals("thelist.test", (byte[]) temp.elementAt(3));

        assertEquals("NIL", (String) envelope.elementAt(6));
        assertEquals("NIL", (String) envelope.elementAt(7));
        assertEquals("NIL", (String) envelope.elementAt(8));
        assertEquals("<1ECCDABD-5242-4180-9584-E5873C3FEA17@smith.test>",
            (byte[]) envelope.elementAt(9));
    }

    public void testParenStringParserBodyStructure() {
        String rawText = "(BODYSTRUCTURE " +
            "((\"TEXT\" \"PLAIN\" (\"CHARSET\" \"us-ascii\") NIL NIL \"7BIT\" 165 8 NIL NIL NIL) " +
            "(\"TEXT\" \"HTML\" (\"CHARSET\" \"us-ascii\") NIL NIL \"7BIT\" 627 10 NIL NIL NIL) " +
            "\"ALTERNATIVE\" (\"BOUNDARY\" \"Boundary-00=_y9RuEFduwo6YU42\") (\"INLINE\" NIL) NIL))";

        Vector result = ImapParser.parenListParser(rawText.getBytes());
        Vector temp1;
        Vector temp2;
        Vector temp3;

        //printTree(result, 0);
        assertEquals("BODYSTRUCTURE", (String) result.elementAt(0));
        temp1 = (Vector) result.elementAt(1);

        temp2 = (Vector) temp1.elementAt(0);
        assertEquals("TEXT", (byte[]) temp2.elementAt(0));
        assertEquals("PLAIN", (byte[]) temp2.elementAt(1));
        temp3 = (Vector) temp2.elementAt(2);
        assertEquals("CHARSET", (byte[]) temp3.elementAt(0));
        assertEquals("us-ascii", (byte[]) temp3.elementAt(1));
        assertEquals("NIL", (String) temp2.elementAt(3));
        assertEquals("NIL", (String) temp2.elementAt(4));
        assertEquals("7BIT", (byte[]) temp2.elementAt(5));
        assertEquals("165", (String) temp2.elementAt(6));
        assertEquals("8", (String) temp2.elementAt(7));
        assertEquals("NIL", (String) temp2.elementAt(8));
        assertEquals("NIL", (String) temp2.elementAt(9));
        assertEquals("NIL", (String) temp2.elementAt(10));

        temp2 = (Vector) temp1.elementAt(1);
        assertEquals("TEXT", (byte[]) temp2.elementAt(0));
        assertEquals("HTML", (byte[]) temp2.elementAt(1));
        temp3 = (Vector) temp2.elementAt(2);
        assertEquals("CHARSET", (byte[]) temp3.elementAt(0));
        assertEquals("us-ascii", (byte[]) temp3.elementAt(1));
        assertEquals("NIL", (String) temp2.elementAt(3));
        assertEquals("NIL", (String) temp2.elementAt(4));
        assertEquals("7BIT", (byte[]) temp2.elementAt(5));
        assertEquals("627", (String) temp2.elementAt(6));
        assertEquals("10", (String) temp2.elementAt(7));
        assertEquals("NIL", (String) temp2.elementAt(8));
        assertEquals("NIL", (String) temp2.elementAt(9));
        assertEquals("NIL", (String) temp2.elementAt(10));

        assertEquals("ALTERNATIVE", (byte[]) temp1.elementAt(2));

        temp2 = (Vector) temp1.elementAt(3);
        assertEquals("BOUNDARY", (byte[]) temp2.elementAt(0));
        assertEquals("Boundary-00=_y9RuEFduwo6YU42", (byte[]) temp2.elementAt(1));

        temp2 = (Vector) temp1.elementAt(4);
        assertEquals("INLINE", (byte[]) temp2.elementAt(0));
        assertEquals("NIL", (String) temp2.elementAt(1));

        assertEquals("NIL", (String) temp1.elementAt(5));
    }

    //    /**
    //     * This method prints the parse tree for debugging purposes.
    //     * @param node Node to start at.
    //     * @param level Level to print from.
    //     */
    //    private void printTree(Object node, int level) {
    //        if(node instanceof Vector) {
    //            Vector vec = (Vector)node;
    //            int size = vec.size();
    //            for(int i=0; i<size; i++)
    //                printTree(vec.elementAt(i), level + 1);
    //        }
    //        else {
    //            StringBuffer buf = new StringBuffer();
    //            buf.append(level+">");
    //            for(int i=0; i<level; i++)
    //                buf.append("    ");
    //            if(node != null) {
    //                buf.append(node.toString());
    //            }
    //            else {
    //                buf.append("null");
    //            }
    //            System.err.println(buf.toString());
    //        }
    //    }
    
    protected void assertEquals(String expected, byte[] actual) {
        assertEquals(expected, new String(actual));
    }
    
    public Test suite() {
        TestSuite suite = new TestSuite("ImapParser");

        suite.addTest(new ImapParserTest("parseFolderName", new TestMethod()
        { public void run(TestCase tc) { ((ImapParserTest) tc).testParseFolderName(); }}));

        suite.addTest(new ImapParserTest("parenStringLexer1", new TestMethod()
        { public void run(TestCase tc) { ((ImapParserTest) tc).testParenStringLexer1(); }}));

        suite.addTest(new ImapParserTest("parenStringLexer2", new TestMethod()
        { public void run(TestCase tc) { ((ImapParserTest) tc).testParenStringLexer2(); }}));

        suite.addTest(new ImapParserTest("parenStringParserEnvelope1", new TestMethod()
        { public void run(TestCase tc) { ((ImapParserTest) tc).testParenStringParserEnvelope1(); }}));

        suite.addTest(new ImapParserTest("parenStringParserEnvelope2", new TestMethod()
        { public void run(TestCase tc) { ((ImapParserTest) tc).testParenStringParserEnvelope2(); }}));

        suite.addTest(new ImapParserTest("parenStringParserEnvelope3", new TestMethod()
        { public void run(TestCase tc) { ((ImapParserTest) tc).testParenStringParserEnvelope3(); }}));

        suite.addTest(new ImapParserTest("parenStringParserBodyStructure", new TestMethod()
        { public void run(TestCase tc) { ((ImapParserTest) tc).testParenStringParserBodyStructure(); }}));

        return suite;
    }
}
