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
package org.logicprobe.LogicMail.model;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

import net.rim.device.api.util.Arrays;

import org.logicprobe.LogicMail.message.ApplicationContent;
import org.logicprobe.LogicMail.message.ApplicationPart;
import org.logicprobe.LogicMail.message.MimeMessageContent;
import org.logicprobe.LogicMail.message.TextContent;
import org.logicprobe.LogicMail.message.TextPart;

import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestMethod;
import j2meunit.framework.TestSuite;

/**
 * Unit tests covering both MessageContentFileWriter and
 * MessageContentFileReader.
 */
public class MessageContentFileTest extends TestCase {
    private static String FILE_URL = "file:///SDCard/BlackBerry/MessageContentFileTest.dat";
    private FileConnection fileConnection;
    
    public MessageContentFileTest() {
    }

    public MessageContentFileTest(String testName, TestMethod testMethod) {
        super(testName, testMethod);
    }
    
    public void setUp() throws Exception {
        fileConnection = (FileConnection)Connector.open(FILE_URL);
        if(fileConnection.exists()) {
            fileConnection.delete();
        }
    }
    
    public void tearDown() throws Exception {
        if(fileConnection.exists()) {
            fileConnection.delete();
        }
    }
    
    public void testOpenNewFile() throws Throwable {
        MessageContentFileWriter writer = new MessageContentFileWriter(fileConnection, "12340000");
        writer.open();
        assertTrue(writer.isOpen());
        writer.close();
        
        MessageContentFileReader reader = new MessageContentFileReader(fileConnection, "12340000");
        reader.open();
        assertTrue(reader.isOpen());
        reader.close();
    }
    
    public void testOpenExistingFile() throws Throwable {
        MessageContentFileWriter writer = new MessageContentFileWriter(fileConnection, "12340000");
        writer.open();
        assertTrue(writer.isOpen());
        writer.close();
        
        writer.open();
        assertTrue(writer.isOpen());
        writer.close();
        
        MessageContentFileReader reader = new MessageContentFileReader(fileConnection, "12340000");
        reader.open();
        assertTrue(reader.isOpen());
        reader.close();
    }
    
    public void testOpenNewFileWithHeaderValues() throws Throwable {
        MessageContentFileWriter writer = new MessageContentFileWriter(fileConnection, "12340000");
        writer.setCustomValues(new int[] { 10, 20, 30, 40 });
        writer.open();
        assertTrue(writer.isOpen());
        writer.close();
        
        MessageContentFileReader reader = new MessageContentFileReader(fileConnection, "12340000");
        reader.open();
        assertTrue(reader.isOpen());
        int[] customValues = reader.getCustomValues();
        assertNotNull(customValues);
        assertEquals(4, customValues.length);
        assertEquals(10, customValues[0]);
        assertEquals(20, customValues[1]);
        assertEquals(30, customValues[2]);
        assertEquals(40, customValues[3]);
        reader.close();
    }
    
    public void testAddContentToNewFile() throws Throwable {
        TextPart part = new TextPart("plain", "", "", "", "", "", -1, "1");
        TextContent content = new TextContent(part, "Hello World");
        
        MessageContentFileWriter writer = new MessageContentFileWriter(fileConnection, "12340000");
        writer.open();
        assertTrue(writer.isOpen());
        writer.appendContent(content);
        writer.close();

        MessageContentFileReader reader = new MessageContentFileReader(fileConnection, "12340000");
        reader.open();
        assertTrue(reader.isOpen());
        assertTrue(reader.hasContent(part));
        MimeMessageContent readContent = reader.getContent(part);
        assertNotNull(readContent);
        assertTrue(readContent instanceof TextContent);
        TextContent readTextContent = (TextContent)readContent;
        assertEquals(content.getText(), readTextContent.getText());
        assertTrue(Arrays.equals(content.getRawData(), readTextContent.getRawData()));
        reader.close();
    }
    
    public void testAddMultipleContentToNewFile() throws Throwable {
        TextPart textPart = new TextPart("plain", "", "", "", "", "", -1, "1");
        TextContent textContent = new TextContent(textPart, "Hello World");
        ApplicationPart appPart = new ApplicationPart("octet-stream", "", "", "", "", -1, "2");
        ApplicationContent appContent = new ApplicationContent(appPart, new byte[] { (byte)0xDE, (byte)0xAD, (byte)0xBE, (byte)0xEF } );
        
        MessageContentFileWriter writer = new MessageContentFileWriter(fileConnection, "12340000");
        writer.open();
        assertTrue(writer.isOpen());
        writer.appendContent(textContent);
        writer.appendContent(appContent);
        writer.close();

        MessageContentFileReader reader = new MessageContentFileReader(fileConnection, "12340000");
        reader.open();
        assertTrue(reader.isOpen());
        
        assertTrue(reader.hasContent(textPart));
        MimeMessageContent readContent = reader.getContent(textPart);
        assertNotNull(readContent);
        assertTrue(readContent instanceof TextContent);
        TextContent readTextContent = (TextContent)readContent;
        assertEquals(textContent.getText(), readTextContent.getText());
        assertTrue(Arrays.equals(textContent.getRawData(), readTextContent.getRawData()));
        
        assertTrue(reader.hasContent(appPart));
        readContent = reader.getContent(appPart);
        assertNotNull(readContent);
        assertTrue(readContent instanceof ApplicationContent);
        assertTrue(Arrays.equals(appContent.getRawData(), readContent.getRawData()));
        
        reader.close();
    }
    
    public void testAddContentToExistingFile() throws Throwable {
        TextPart textPart = new TextPart("plain", "", "", "", "", "", -1, "1");
        TextContent textContent = new TextContent(textPart, "Hello World");
        ApplicationPart appPart = new ApplicationPart("octet-stream", "", "", "", "", -1, "2");
        ApplicationContent appContent = new ApplicationContent(appPart, new byte[] { (byte)0xDE, (byte)0xAD, (byte)0xBE, (byte)0xEF } );
        
        // Create the file with the first content section
        MessageContentFileWriter writer = new MessageContentFileWriter(fileConnection, "12340000");
        writer.open();
        assertTrue(writer.isOpen());
        writer.appendContent(textContent);
        writer.close();

        // Open the file again, and add new content
        writer.open();
        assertTrue(writer.isOpen());
        writer.appendContent(appContent);
        writer.close();
        
        // Open the file for reading
        MessageContentFileReader reader = new MessageContentFileReader(fileConnection, "12340000");
        reader.open();
        assertTrue(reader.isOpen());
        
        // Check to make sure that the first content section exists
        assertTrue(reader.hasContent(textPart));
        MimeMessageContent readContent = reader.getContent(textPart);
        assertNotNull(readContent);
        assertTrue(readContent instanceof TextContent);
        assertEquals(textContent.getText(), ((TextContent)readContent).getText());
        assertTrue(Arrays.equals(textContent.getRawData(), readContent.getRawData()));
        
        // Check to make sure that the second content section exists
        assertTrue(reader.hasContent(appPart));
        readContent = reader.getContent(appPart);
        assertNotNull(readContent);
        assertTrue(readContent instanceof ApplicationContent);
        assertTrue(Arrays.equals(appContent.getRawData(), readContent.getRawData()));
        
        reader.close();
    }
    
    public Test suite() {
        TestSuite suite = new TestSuite("MessageContentFileTest");
        suite.addTest(new MessageContentFileTest("openNewFile", new TestMethod()
        { public void run(TestCase tc) throws Throwable {((MessageContentFileTest)tc).testOpenNewFile(); } }));
        suite.addTest(new MessageContentFileTest("openExistingFile", new TestMethod()
        { public void run(TestCase tc) throws Throwable {((MessageContentFileTest)tc).testOpenExistingFile(); } }));
        suite.addTest(new MessageContentFileTest("openNewFileWithHeaderValues", new TestMethod()
        { public void run(TestCase tc) throws Throwable {((MessageContentFileTest)tc).testOpenNewFileWithHeaderValues(); } }));
        suite.addTest(new MessageContentFileTest("addContentToNewFile", new TestMethod()
        { public void run(TestCase tc) throws Throwable {((MessageContentFileTest)tc).testAddContentToNewFile(); } }));
        suite.addTest(new MessageContentFileTest("addMultipleContentToNewFile", new TestMethod()
        { public void run(TestCase tc) throws Throwable {((MessageContentFileTest)tc).testAddMultipleContentToNewFile(); } }));
        suite.addTest(new MessageContentFileTest("addContentToExistingFile", new TestMethod()
        { public void run(TestCase tc) throws Throwable {((MessageContentFileTest)tc).testAddContentToExistingFile(); } }));
        
        return suite;
    }
}
