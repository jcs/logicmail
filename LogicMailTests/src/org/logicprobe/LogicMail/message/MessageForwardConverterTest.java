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
package org.logicprobe.LogicMail.message;

import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestMethod;
import j2meunit.framework.TestSuite;
import java.util.Calendar;
import java.util.Vector;
import net.rim.device.api.ui.component.RichTextField;
import org.logicprobe.LogicMail.ui.MessageRenderer;
import org.logicprobe.LogicMail.ui.MessageRendererTest;
import org.logicprobe.LogicMail.util.StringParser;

/**
 * Unit test for MessageForwardConverter
 */
public class MessageForwardConverterTest extends TestCase {
    public MessageForwardConverterTest() {
    }
    
    public MessageForwardConverterTest(String testName, TestMethod testMethod) {
        super(testName, testMethod);
    }
    
    public void setUp() {
    }

    public void tearDown() {
    }
    
    public void testSimpleTextMessage() {
        String sampleText =
                "Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do\r\n" +
                "eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim\r\n" +
                "ad minim veniam, quis nostrud exercitation ullamco laboris\r\n" +
                "nisi ut aliquip ex ea commodo consequat.";
        MessagePart body = new TextPart("plain", sampleText);
        
        MessageEnvelope env = new MessageEnvelope();
        env.from = new String[] { "John Doe <jdoe@generic.org>" };
        env.to = new String[] { "Jim Smith <jsmith@something.net>",
                                "Jane Doe <jane.doe@things.org>" };
        env.date = Calendar.getInstance().getTime();
        env.subject = "The Subject";
        
        String expectedText =
                "----Original Message----\r\n"+
                "Subject: The Subject\r\n"+
                "Date: "+StringParser.createDateString(env.date)+"\r\n"+
                "From: John Doe <jdoe@generic.org>\r\n"+
                "To: Jim Smith <jsmith@something.net>, Jane Doe <jane.doe@things.org>\r\n"+
                "\r\n" +
                "Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do\r\n" +
                "eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim\r\n" +
                "ad minim veniam, quis nostrud exercitation ullamco laboris\r\n" +
                "nisi ut aliquip ex ea commodo consequat.\r\n"+
                "------------------------";
        
        MessageForwardConverter instance = new MessageForwardConverter(env);
        body.accept(instance);
        MessagePart result = instance.toForwardBody();
        
        assertNotNull("Null result", result);
        assertTrue("Bad type", result instanceof TextPart);
        
        assertEquals("Content mismatch", expectedText, ((TextPart)result).getText());
    }

    public Test suite() {
        TestSuite suite = new TestSuite("MessageForwardConverter");
        suite.addTest(new MessageForwardConverterTest("Simple text message", new TestMethod()
        { public void run(TestCase tc) {((MessageForwardConverterTest)tc).testSimpleTextMessage(); } }));
        
        return suite;
    }
}
