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

public class FilenameEncoderTest extends TestCase {
    public FilenameEncoderTest() {
    }

    public FilenameEncoderTest(String testName, TestMethod testMethod) {
        super(testName, testMethod);
    }
    
    public void testStringNotNeedingEncoding() {
        assertEquals("12", FilenameEncoder.encode("12"));
        assertEquals("3F", FilenameEncoder.encode("3F"));
        assertEquals("Hello", FilenameEncoder.encode("Hello"));
        assertEquals("1337DEADBEEF", FilenameEncoder.encode("1337DEADBEEF"));
    }
    
    public void testStringNeedingComplexEncoding() {
        String input = "<AANLkTi=HmeCMneW-U+BHamLyPLs11xN5x45CxOty6bqC@mail.gmail.com>";
        String expected = "!3CAANLkTi!3DHmeCMneW-U!2BBHamLyPLs11xN5x45CxOty6bqC!40mail.gmail.com!3E";
        String encoded = FilenameEncoder.encode(input);
        assertEquals(expected, encoded);
        //writeFileWithName(encoded);
    }

    public void testStringNotNeedingDecoding() {
        assertEquals("12", FilenameEncoder.decode("12"));
        assertEquals("3F", FilenameEncoder.decode("3F"));
        assertEquals("Hello", FilenameEncoder.decode("Hello"));
        assertEquals("1337DEADBEEF", FilenameEncoder.decode("1337DEADBEEF"));
    }
    
    public void testStringNeedingComplexDecoding() {
        String input = "!3CAANLkTi!3DHmeCMneW-U!2BBHamLyPLs11xN5x45CxOty6bqC!40mail.gmail.com!3E";
        String expected = "<AANLkTi=HmeCMneW-U+BHamLyPLs11xN5x45CxOty6bqC@mail.gmail.com>";
        assertEquals(expected, FilenameEncoder.decode(input));
    }

// This requires a configured SDCard space, and is thus disabled by default:
//    private static String URL_BASE = "file:///SDCard/BlackBerry/";
//    private static String FILE_EXT = ".dat";
//    private void writeFileWithName(String filename) {
//        try {
//            String fileUrl = URL_BASE + filename + FILE_EXT;
//            javax.microedition.io.file.FileConnection fc = (javax.microedition.io.file.FileConnection)javax.microedition.io.Connector.open(fileUrl);
//            fc.create();
//            fc.close();
//            
//            fc = (javax.microedition.io.file.FileConnection)Connector.open(fileUrl);
//            assertTrue(fc.exists());
//            fc.delete();
//            fc.close();
//        } catch (Throwable t) {
//            fail("Unable to use filename: \"" + filename + "\"");
//        }
//    }
    
    public Test suite() {
        TestSuite suite = new TestSuite("FilenameEncoder");
        
        suite.addTest(new FilenameEncoderTest("stringNotNeedingEncoding", new TestMethod()
        { public void run(TestCase tc) { ((FilenameEncoderTest) tc).testStringNotNeedingEncoding(); }}));
        suite.addTest(new FilenameEncoderTest("stringNeedingComplexEncoding", new TestMethod()
        { public void run(TestCase tc) { ((FilenameEncoderTest) tc).testStringNeedingComplexEncoding(); }}));
        suite.addTest(new FilenameEncoderTest("stringNotNeedingDecoding", new TestMethod()
        { public void run(TestCase tc) { ((FilenameEncoderTest) tc).testStringNotNeedingDecoding(); }}));
        suite.addTest(new FilenameEncoderTest("stringNeedingComplexDecoding", new TestMethod()
        { public void run(TestCase tc) { ((FilenameEncoderTest) tc).testStringNeedingComplexDecoding(); }}));
        
        return suite;
    }
}
