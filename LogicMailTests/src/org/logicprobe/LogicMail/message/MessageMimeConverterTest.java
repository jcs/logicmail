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

import java.io.ByteArrayInputStream;
import java.util.Hashtable;

import net.rim.device.api.util.Arrays;

import org.logicprobe.LogicMail.util.MailMessageParser;

import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestMethod;
import j2meunit.framework.TestSuite;

/**
 * Unit test for MessageMimeConverter
 */
public class MessageMimeConverterTest extends TestCase {
    public MessageMimeConverterTest() {
    }
    
    public MessageMimeConverterTest(String testName, TestMethod testMethod) {
        super(testName, testMethod);
    }
    
    public void setUp() {
    }

    public void tearDown() {
    }
    
    public void testSinglePartMessage() throws Throwable {
        TextPart textPart = new TextPart("plain", "", "7bit", "us-ascii", "", "", 0);
        TextContent textContent = new TextContent(textPart, "Hello World");
        
        Message message = new Message(textPart);
        message.putContent(textPart, textContent);
        
        MessageMimeConverter instance = new MessageMimeConverter(message);
        String result = instance.toMimeString();
        
        Hashtable resultContentMap = new Hashtable();
        MimeMessagePart resultPart = MailMessageParser.parseRawMessage(
                    resultContentMap,
                    new ByteArrayInputStream(result.getBytes()));
        
        assertTrue(resultPart instanceof TextPart);
        assertEquals("plain", resultPart.getMimeSubtype().toLowerCase());
        assertEquals("7bit", ((TextPart)resultPart).getEncoding().toLowerCase());
        assertEquals("us-ascii", ((TextPart)resultPart).getCharset().toLowerCase());
        
        assertTrue(resultContentMap.containsKey(resultPart));
        TextContent resultTextContent = (TextContent)resultContentMap.get(resultPart);
        assertEquals("Hello World", resultTextContent.getText());
    }

    public void testMultiPartMessage() throws Throwable {
        TextPart textPart1 = new TextPart("plain", "", "7bit", "us-ascii", "", "", 0);
        TextContent textContent1 = new TextContent(textPart1, "Hello World");
        TextPart textPart2 = new TextPart("plain", "", "7bit", "us-ascii", "", "", 0);
        TextContent textContent2 = new TextContent(textPart2, "Goodbye World");

        MultiPart multiPart = new MultiPart("mixed");
        multiPart.addPart(textPart1);
        multiPart.addPart(textPart2);
        
        Message message = new Message(multiPart);
        message.putContent(textPart1, textContent1);
        message.putContent(textPart2, textContent2);
        
        MessageMimeConverter instance = new MessageMimeConverter(message);
        String result = instance.toMimeString();
        
        Hashtable resultContentMap = new Hashtable();
        MimeMessagePart resultPart = MailMessageParser.parseRawMessage(
                    resultContentMap,
                    new ByteArrayInputStream(result.getBytes()));
        
        assertTrue(resultPart instanceof MultiPart);
        MultiPart resultMultiPart = (MultiPart)resultPart;
        assertEquals("mixed", resultMultiPart.getMimeSubtype());
        
        MimeMessagePart[] resultParts = resultMultiPart.getParts();
        assertNotNull(resultParts);
        assertEquals(2, resultParts.length);
        assertTrue(resultParts[0] instanceof TextPart);
        assertTrue(resultParts[1] instanceof TextPart);
        
        TextPart resultTextPart1 = (TextPart)resultParts[0];
        assertEquals("plain", resultTextPart1.getMimeSubtype().toLowerCase());
        assertEquals("7bit", resultTextPart1.getEncoding().toLowerCase());
        assertEquals("us-ascii", resultTextPart1.getCharset().toLowerCase());
        assertTrue(resultContentMap.containsKey(resultTextPart1));
        TextContent resultTextContent1 = (TextContent)resultContentMap.get(resultTextPart1);
        assertEquals("Hello World", resultTextContent1.getText());
        
        TextPart resultTextPart2 = (TextPart)resultParts[1];
        assertEquals("plain", resultTextPart2.getMimeSubtype().toLowerCase());
        assertEquals("7bit", resultTextPart2.getEncoding().toLowerCase());
        assertEquals("us-ascii", resultTextPart2.getCharset().toLowerCase());
        assertTrue(resultContentMap.containsKey(resultTextPart2));
        TextContent resultTextContent2 = (TextContent)resultContentMap.get(resultTextPart2);
        assertEquals("Goodbye World", resultTextContent2.getText());
    }

    public void testComplexMultiPartMessage() throws Throwable {
        TextPart textPart1a = new TextPart("plain", "", "7bit", "us-ascii", "", "", 0);
        TextContent textContent1a = new TextContent(textPart1a, "Hello World");
        TextPart textPart1b = new TextPart("plain", "", "7bit", "us-ascii", "", "", 0);
        TextContent textContent1b = new TextContent(textPart1b, "Goodbye World");
        MultiPart multiPart1 = new MultiPart("mixed");
        multiPart1.addPart(textPart1a);
        multiPart1.addPart(textPart1b);
        
        TextPart textPart2a = new TextPart("plain", "", "7bit", "us-ascii", "", "", 0);
        TextContent textContent2a = new TextContent(textPart2a, "Foo");
        TextPart textPart2b = new TextPart("plain", "", "7bit", "us-ascii", "", "", 0);
        TextContent textContent2b = new TextContent(textPart2b, "Bar");
        MultiPart multiPart2 = new MultiPart("mixed");
        multiPart2.addPart(textPart2a);
        multiPart2.addPart(textPart2b);
        
        MultiPart multiPart = new MultiPart("mixed");
        multiPart.addPart(multiPart1);
        multiPart.addPart(multiPart2);
        
        Message message = new Message(multiPart);
        message.putContent(textPart1a, textContent1a);
        message.putContent(textPart1b, textContent1b);
        message.putContent(textPart2a, textContent2a);
        message.putContent(textPart2b, textContent2b);
        
        MessageMimeConverter instance = new MessageMimeConverter(message);
        String result = instance.toMimeString();
        
        Hashtable resultContentMap = new Hashtable();
        MimeMessagePart resultPart = MailMessageParser.parseRawMessage(
                    resultContentMap,
                    new ByteArrayInputStream(result.getBytes()));
        
        assertTrue(resultPart instanceof MultiPart);
        MultiPart resultMultiPart = (MultiPart)resultPart;
        assertEquals("mixed", resultMultiPart.getMimeSubtype());
        
        MimeMessagePart[] resultParts = resultMultiPart.getParts();
        assertNotNull(resultParts);
        assertEquals(2, resultParts.length);
        assertTrue(resultParts[0] instanceof MultiPart);
        assertTrue(resultParts[1] instanceof MultiPart);
        
        MultiPart resultMultiPart1 = (MultiPart)resultParts[0];
        assertEquals("mixed", resultMultiPart1.getMimeSubtype());
        MimeMessagePart[] resultSubParts1 = resultMultiPart1.getParts();
        assertEquals(2, resultSubParts1.length);
        assertTrue(resultSubParts1[0] instanceof TextPart);
        assertTrue(resultSubParts1[1] instanceof TextPart);
        TextPart resultTextPart1a = (TextPart)resultSubParts1[0];
        TextPart resultTextPart1b = (TextPart)resultSubParts1[1];
        
        assertTrue(resultContentMap.containsKey(resultTextPart1a));
        TextContent resultTextContent1a = (TextContent)resultContentMap.get(resultTextPart1a);
        assertEquals("Hello World", resultTextContent1a.getText());
        assertTrue(resultContentMap.containsKey(resultTextPart1b));
        TextContent resultTextContent1b = (TextContent)resultContentMap.get(resultTextPart1b);
        assertEquals("Goodbye World", resultTextContent1b.getText());
        
        MultiPart resultMultiPart2 = (MultiPart)resultParts[1];
        assertEquals("mixed", resultMultiPart2.getMimeSubtype());
        MimeMessagePart[] resultSubParts2 = resultMultiPart2.getParts();
        assertEquals(2, resultSubParts2.length);
        assertTrue(resultSubParts2[0] instanceof TextPart);
        assertTrue(resultSubParts2[1] instanceof TextPart);
        TextPart resultTextPart2a = (TextPart)resultSubParts2[0];
        TextPart resultTextPart2b = (TextPart)resultSubParts2[1];
        
        assertTrue(resultContentMap.containsKey(resultTextPart2a));
        TextContent resultTextContent2a = (TextContent)resultContentMap.get(resultTextPart2a);
        assertEquals("Foo", resultTextContent2a.getText());
        assertTrue(resultContentMap.containsKey(resultTextPart2b));
        TextContent resultTextContent2b = (TextContent)resultContentMap.get(resultTextPart2b);
        assertEquals("Bar", resultTextContent2b.getText());
    }
    
    public void testMessageWithImageAttachment() throws Throwable {
        TextPart textPart = new TextPart("plain", "", "7bit", "us-ascii", "", "", 0);
        TextContent textContent = new TextContent(textPart, "Hello World");
        ImagePart imagePart = new ImagePart("png", "test.png", "base64", "", "", RAW_PNG_DATA.length);
        ImageContent imageContent = new ImageContent(imagePart, RAW_PNG_DATA);
        
        MultiPart multiPart = new MultiPart("mixed");
        multiPart.addPart(textPart);
        multiPart.addPart(imagePart);
        
        Message message = new Message(multiPart);
        message.putContent(textPart, textContent);
        message.putContent(imagePart, imageContent);
        
        MessageMimeConverter instance = new MessageMimeConverter(message);
        String result = instance.toMimeString();
        
        Hashtable resultContentMap = new Hashtable();
        MimeMessagePart resultPart = MailMessageParser.parseRawMessage(
                    resultContentMap,
                    new ByteArrayInputStream(result.getBytes()));
        
        assertTrue(resultPart instanceof MultiPart);
        MultiPart resultMultiPart = (MultiPart)resultPart;
        assertEquals("mixed", resultMultiPart.getMimeSubtype());
        
        MimeMessagePart[] resultParts = resultMultiPart.getParts();
        assertNotNull(resultParts);
        assertEquals(2, resultParts.length);
        assertTrue(resultParts[0] instanceof TextPart);
        assertTrue(resultParts[1] instanceof ImagePart);
        
        TextPart resultTextPart = (TextPart)resultParts[0];
        assertEquals("plain", resultTextPart.getMimeSubtype().toLowerCase());
        assertEquals("7bit", resultTextPart.getEncoding().toLowerCase());
        assertEquals("us-ascii", resultTextPart.getCharset().toLowerCase());
        assertTrue(resultContentMap.containsKey(resultTextPart));
        TextContent resultTextContent = (TextContent)resultContentMap.get(resultTextPart);
        assertEquals("Hello World", resultTextContent.getText());
        
        ContentPart resultContentPart = (ContentPart)resultParts[1];
        assertEquals(imagePart.getMimeSubtype(), resultContentPart.getMimeSubtype().toLowerCase());
        assertEquals(imagePart.getEncoding(), resultContentPart.getEncoding().toLowerCase());
        assertEquals(imagePart.getName(), resultContentPart.getName());
        assertTrue(resultContentMap.containsKey(resultContentPart));
        MimeMessageContent resultContent = (MimeMessageContent)resultContentMap.get(resultContentPart);
        assertTrue(Arrays.equals(imageContent.getRawData(), resultContent.getRawData()));
    }
    
    public void testMessageWithAttachments() throws Throwable {
        TextPart textPart = new TextPart("plain", "", "7bit", "us-ascii", "", "", 0);
        TextContent textContent = new TextContent(textPart, "Hello World");
        ImagePart imagePart = new ImagePart("png", "test.png", "base64", "", "", RAW_PNG_DATA.length);
        ImageContent imageContent = new ImageContent(imagePart, RAW_PNG_DATA);
        ApplicationPart applicationPart = new ApplicationPart("octet-stream", "test.dat", "base64", "", "", RAW_MISC_DATA.length, "");
        ApplicationContent applicationContent = new ApplicationContent(applicationPart, RAW_MISC_DATA);
        AudioPart audioPart = new AudioPart("mp3", "test.mp3", "base64", "", "", RAW_MISC_DATA.length, "");
        AudioContent audioContent = new AudioContent(audioPart, RAW_MISC_DATA);
        VideoPart videoPart = new VideoPart("mpg", "test.mpg", "base64", "", "", RAW_MISC_DATA.length, "");
        VideoContent videoContent = new VideoContent(videoPart, RAW_MISC_DATA);
        
        MultiPart multiPart = new MultiPart("mixed");
        multiPart.addPart(textPart);
        multiPart.addPart(imagePart);
        multiPart.addPart(applicationPart);
        multiPart.addPart(audioPart);
        multiPart.addPart(videoPart);
        
        Message message = new Message(multiPart);
        message.putContent(textPart, textContent);
        message.putContent(imagePart, imageContent);
        message.putContent(applicationPart, applicationContent);
        message.putContent(audioPart, audioContent);
        message.putContent(videoPart, videoContent);
        
        MessageMimeConverter instance = new MessageMimeConverter(message);
        String result = instance.toMimeString();
        
        Hashtable resultContentMap = new Hashtable();
        MimeMessagePart resultPart = MailMessageParser.parseRawMessage(
                    resultContentMap,
                    new ByteArrayInputStream(result.getBytes()));
        
        assertTrue(resultPart instanceof MultiPart);
        MultiPart resultMultiPart = (MultiPart)resultPart;
        assertEquals("mixed", resultMultiPart.getMimeSubtype());
        
        MimeMessagePart[] resultParts = resultMultiPart.getParts();
        assertNotNull(resultParts);
        assertEquals(5, resultParts.length);
        assertTrue(resultParts[0] instanceof TextPart);
        assertTrue(resultParts[1] instanceof ImagePart);
        assertTrue(resultParts[2] instanceof ApplicationPart);
        assertTrue(resultParts[3] instanceof AudioPart);
        assertTrue(resultParts[4] instanceof VideoPart);
        
        TextPart resultTextPart = (TextPart)resultParts[0];
        assertEquals("plain", resultTextPart.getMimeSubtype().toLowerCase());
        assertEquals("7bit", resultTextPart.getEncoding().toLowerCase());
        assertEquals("us-ascii", resultTextPart.getCharset().toLowerCase());
        assertTrue(resultContentMap.containsKey(resultTextPart));
        TextContent resultTextContent = (TextContent)resultContentMap.get(resultTextPart);
        assertEquals("Hello World", resultTextContent.getText());
        
        assertPartAndContent("Image",
                imagePart, imageContent,
                resultContentMap, (ContentPart)resultParts[1]);
        assertPartAndContent("Application",
                applicationPart, applicationContent,
                resultContentMap, (ContentPart)resultParts[2]);
        assertPartAndContent("Audio",
                audioPart, audioContent,
                resultContentMap, (ContentPart)resultParts[3]);
        assertPartAndContent("Video",
                videoPart, videoContent,
                resultContentMap, (ContentPart)resultParts[4]);
    }

    private void assertPartAndContent(String message,
            ContentPart sourceContentPart,
            MimeMessageContent sourceContent, Hashtable resultContentMap,
            ContentPart resultContentPart) {
        assertEquals(message, sourceContentPart.getMimeSubtype(), resultContentPart.getMimeSubtype().toLowerCase());
        assertEquals(message, sourceContentPart.getEncoding(), resultContentPart.getEncoding().toLowerCase());
        assertEquals(message, sourceContentPart.getName(), resultContentPart.getName());
        assertTrue(message, resultContentMap.containsKey(resultContentPart));
        MimeMessageContent resultContent = (MimeMessageContent)resultContentMap.get(resultContentPart);
        assertTrue(message, Arrays.equals(sourceContent.getRawData(), resultContent.getRawData()));
    }
    
    public Test suite() {
        TestSuite suite = new TestSuite("MessageMimeConverter");

        suite.addTest(new MessageMimeConverterTest("singlePartMessage", new TestMethod()
        { public void run(TestCase tc) throws Throwable {((MessageMimeConverterTest)tc).testSinglePartMessage(); } }));
        suite.addTest(new MessageMimeConverterTest("multiPartMessage", new TestMethod()
        { public void run(TestCase tc) throws Throwable {((MessageMimeConverterTest)tc).testMultiPartMessage(); } }));
        suite.addTest(new MessageMimeConverterTest("complexMultiPartMessage", new TestMethod()
        { public void run(TestCase tc) throws Throwable {((MessageMimeConverterTest)tc).testComplexMultiPartMessage(); } }));
        suite.addTest(new MessageMimeConverterTest("messageWithImageAttachment", new TestMethod()
        { public void run(TestCase tc) throws Throwable {((MessageMimeConverterTest)tc).testMessageWithImageAttachment(); } }));
        suite.addTest(new MessageMimeConverterTest("messageWithAttachments", new TestMethod()
        { public void run(TestCase tc) throws Throwable {((MessageMimeConverterTest)tc).testMessageWithAttachments(); } }));
        
        return suite;
    }
    
    private static final byte[] RAW_PNG_DATA = {
        (byte)0x89, (byte)0x50, (byte)0x4E, (byte)0x47, (byte)0x0D, (byte)0x0A,
        (byte)0x1A, (byte)0x0A, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x0D,
        (byte)0x49, (byte)0x48, (byte)0x44, (byte)0x52, (byte)0x00, (byte)0x00,
        (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x01,
        (byte)0x08, (byte)0x02, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x90,
        (byte)0x77, (byte)0x53, (byte)0xDE, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x01, (byte)0x73, (byte)0x52, (byte)0x47, (byte)0x42, (byte)0x00,
        (byte)0xAE, (byte)0xCE, (byte)0x1C, (byte)0xE9, (byte)0x00, (byte)0x00,
        (byte)0x00, (byte)0x04, (byte)0x67, (byte)0x41, (byte)0x4D, (byte)0x41,
        (byte)0x00, (byte)0x00, (byte)0xB1, (byte)0x8F, (byte)0x0B, (byte)0xFC,
        (byte)0x61, (byte)0x05, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x09,
        (byte)0x70, (byte)0x48, (byte)0x59, (byte)0x73, (byte)0x00, (byte)0x00,
        (byte)0x0E, (byte)0xC3, (byte)0x00, (byte)0x00, (byte)0x0E, (byte)0xC3,
        (byte)0x01, (byte)0xC7, (byte)0x6F, (byte)0xA8, (byte)0x64, (byte)0x00,
        (byte)0x00, (byte)0x00, (byte)0x0C, (byte)0x49, (byte)0x44, (byte)0x41,
        (byte)0x54, (byte)0x18, (byte)0x57, (byte)0x63, (byte)0x60, (byte)0x60,
        (byte)0x60, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x04, (byte)0x00,
        (byte)0x01, (byte)0x5C, (byte)0xCD, (byte)0xFF, (byte)0x69, (byte)0x00,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x49, (byte)0x45, (byte)0x4E,
        (byte)0x44, (byte)0xAE, (byte)0x42, (byte)0x60, (byte)0x82
    };
    
    private static final byte[] RAW_MISC_DATA = {
        (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04, (byte)0x05, (byte)0x06
    };
}
