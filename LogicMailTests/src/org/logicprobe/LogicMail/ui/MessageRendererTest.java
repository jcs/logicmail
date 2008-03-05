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
package org.logicprobe.LogicMail.ui;

import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestMethod;
import j2meunit.framework.TestSuite;
import java.util.Vector;
import net.rim.device.api.ui.component.RichTextField;
import org.logicprobe.LogicMail.message.MessagePart;
import org.logicprobe.LogicMail.message.MultiPart;
import org.logicprobe.LogicMail.message.TextPart;
import org.logicprobe.LogicMail.message.UnsupportedPart;

/**
 * Unit test for MessageRenderer
 */
public class MessageRendererTest extends TestCase {
    public MessageRendererTest() {
    }
    
    public MessageRendererTest(String testName, TestMethod testMethod) {
        super(testName, testMethod);
    }
    
    public void setUp() {
    }

    public void tearDown() {
    }
    
    public void testSimpleTextMessage() {
        MessagePart body = new TextPart("plain", "some text");
        
        MessageRenderer instance = new MessageRenderer();
        body.accept(instance);
        Vector result = instance.getMessageFields();
        
        assertNotNull("Null result", result);
        assertEquals("Bad size", 1, result.size());
        assertTrue("Element 0", result.elementAt(0) instanceof RichTextField);
        RichTextField field = (RichTextField)result.elementAt(0);
        assertEquals("Element 0 contents", "some text", field.getText());
    }

    public void testSimpleMultipartMessage() {
        MultiPart body = new MultiPart("mixed");
        body.addPart(new TextPart("plain", "some text"));
        body.addPart(new UnsupportedPart("text", "html"));
        
        MessageRenderer instance = new MessageRenderer();
        body.accept(instance);
        Vector result = instance.getMessageFields();
        
        assertNotNull("Null result", result);
        assertEquals("Bad size", 2, result.size());
        assertTrue("Element 0", result.elementAt(0) instanceof RichTextField);
        assertEquals("Element 0 contents", "some text", ((RichTextField)(result.elementAt(0))).getText());
        assertTrue("Element 1", result.elementAt(1) instanceof RichTextField);
        assertTrue("Element 1 contents", ((RichTextField)(result.elementAt(1))).getText().toLowerCase().startsWith("unsupported"));
    }
    
    public void testComplexMultipartMessage() {
        MultiPart body = new MultiPart("mixed");
        body.addPart(new TextPart("plain", "some text"));
        MultiPart subBody = new MultiPart("alternative");
        subBody.addPart(new TextPart("plain", "other text"));
        subBody.addPart(new UnsupportedPart("text", "html"));
        body.addPart(subBody);
        body.addPart(new UnsupportedPart("text", "html"));
        
        MessageRenderer instance = new MessageRenderer();
        body.accept(instance);
        Vector result = instance.getMessageFields();
        
        assertNotNull("Null result", result);
        assertEquals("Bad size", 4, result.size());
        assertTrue("Element 0", result.elementAt(0) instanceof RichTextField);
        assertEquals("Element 0 contents", "some text", ((RichTextField)(result.elementAt(0))).getText());
        
        assertTrue("Element 1", result.elementAt(1) instanceof RichTextField);
        assertEquals("Element 1 contents", "other text", ((RichTextField)(result.elementAt(1))).getText());
        
        assertTrue("Element 2", result.elementAt(2) instanceof RichTextField);
        assertTrue("Element 2 contents", ((RichTextField)(result.elementAt(2))).getText().toLowerCase().startsWith("unsupported"));
        
        assertTrue("Element 3", result.elementAt(3) instanceof RichTextField);
        assertTrue("Element 3 contents", ((RichTextField)(result.elementAt(3))).getText().toLowerCase().startsWith("unsupported"));
    }

    public Test suite() {
        TestSuite suite = new TestSuite("MessageRenderer");
        suite.addTest(new MessageRendererTest("Simple text message", new TestMethod()
        { public void run(TestCase tc) {((MessageRendererTest)tc).testSimpleTextMessage(); } }));
        suite.addTest(new MessageRendererTest("Simple multipart message", new TestMethod()
        { public void run(TestCase tc) {((MessageRendererTest)tc).testSimpleMultipartMessage(); } }));
        suite.addTest(new MessageRendererTest("Complex multipart message", new TestMethod()
        { public void run(TestCase tc) {((MessageRendererTest)tc).testComplexMultipartMessage(); } }));
        
        return suite;
    }
}
