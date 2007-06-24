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

package org.logicprobe.LogicMail.util;

import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestMethod;
import j2meunit.framework.TestSuite;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;
import net.rim.device.api.util.Arrays;

/**
 * Unit test for SerializableHashtable
 */
public class SerializableHashtableTest extends TestCase {
    class TestOutputStream extends OutputStream {
        private int[] buffer;
        
        public TestOutputStream() {
            super();
            buffer = new int[0];
        }
        public void write(int b) throws IOException {
            Arrays.add(buffer, b);
        }
        public int[] getBuffer() {
            return buffer;
        }
    }
    
    class TestInputStream extends InputStream {
        private int[] buffer;
        private int index;
        
        public TestInputStream(int[] buffer) {
            this.buffer = buffer;
            index = 0;
        }
        public int read() throws IOException {
            return buffer[index++];
        }
    }
    
    public SerializableHashtableTest() {
    }
    
    public SerializableHashtableTest(String testName, TestMethod testMethod) {
        super(testName, testMethod);
    }
    
    public void setUp() {
    }
    
    public void tearDown() {
    }

    public void testSerialization() {
        try {
            SerializableHashtable table = new SerializableHashtable();
            table.put("One", "One");
            table.put("Two", new Integer(2));
            table.put("Three", new Long(3));
            table.put("Four", new Short((short)4));
            table.put("Five", new Character('5'));
            table.put("Six", new Byte((byte)0x42));
            table.put("Seven", new Float(1.42));
            table.put("Eight", new Double(2.14));
            table.put("Nine", Boolean.TRUE);
            int size = table.size();
            
            // Serialize
            TestOutputStream testOutput = new TestOutputStream();
            table.serialize(new DataOutputStream(testOutput));
            
            // Deserialize
            TestInputStream testInput = new TestInputStream(testOutput.getBuffer());
            table = new SerializableHashtable();
            table.deserialize(new DataInputStream(testInput));
            
            // Verify results
            assertEquals(size, table.size());
            assertEquals("One", (String)table.get("One"));
            assertEquals(new Integer(2), (Integer)table.get("Two"));
            assertEquals(new Long(3), (Long)table.get("Three"));
            assertEquals(new Short((short)4), (Short)table.get("Four"));
            assertEquals(new Character('5'), (Character)table.get("Five"));
            assertEquals(new Byte((byte)0x42), (Byte)table.get("Six"));
            assertEquals(new Float(1.42), (Float)table.get("Seven"));
            assertEquals(new Double(2.14), (Double)table.get("Eight"));
            assertTrue(((Boolean)table.get("Nine")).booleanValue());
        } catch (Throwable t) {
            fail("Exception thrown during test: "+t.toString());
            t.printStackTrace();
        }
    }
    
    public Test suite() {
        TestSuite suite = new TestSuite("SerializableHashtable");

        suite.addTest(new SerializableHashtableTest("serialization", new TestMethod()
        { public void run(TestCase tc) {((SerializableHashtableTest)tc).testSerialization(); } }));

        return suite;
    }
}
