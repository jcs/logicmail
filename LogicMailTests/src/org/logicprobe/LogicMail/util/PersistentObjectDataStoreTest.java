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

import net.rim.device.api.system.PersistentStore;

/**
 * Unit test for PersistentObjectDataStore
 */
public class PersistentObjectDataStoreTest extends TestCase {
    // "org.logicprobe.LogicMail.util.PersistentObjectDataStoreTest"
    private static final long storeUid = 0xcf9cd4c50a72e8abL;
    
    /** Creates a new instance of RmsDataStoreTest */
    public PersistentObjectDataStoreTest() {
    }
    
    public PersistentObjectDataStoreTest(String testName, TestMethod testMethod) {
        super(testName, testMethod);
    }

    public void setUp() {
    }

    public void tearDown() {
        PersistentStore.destroyPersistentObject(storeUid);
    }

    public void testObject() {
        PersistentObjectDataStore instance = new PersistentObjectDataStore(storeUid);
        SerializableTestClass testObject1 = new SerializableTestClass();
        SerializableTestClass testObject2 = new SerializableTestClass();
        testObject1.setValue(40);
        testObject2.setValue(41);
        long testId1 = testObject1.getUniqueId();
        long testId2 = testObject2.getUniqueId();
        
        assertNull(instance.getObject(testId1));
        
        instance.putObject(testObject1);
        assertSame(testObject1, instance.getObject(testId1));

        instance.putObject(testObject2);
        assertSame(testObject1, instance.getObject(testId1));
        assertSame(testObject2, instance.getObject(testId2));
    
        instance.removeObject(testObject1);
        assertNull(instance.getObject(testId1));
        assertSame(testObject2, instance.getObject(testId2));
    }

    public void testNamedObject() {
        PersistentObjectDataStore instance = new PersistentObjectDataStore(storeUid);
        SerializableTestClass testObject1 = new SerializableTestClass();
        SerializableTestClass testObject2 = new SerializableTestClass();
        testObject1.setValue(40);
        testObject2.setValue(41);
        long testId1 = testObject1.getUniqueId();
        long testId2 = testObject2.getUniqueId();
        
        assertNull(instance.getObject(testId1));
        
        instance.putNamedObject("Test 1", testObject1);
        assertSame(testObject1, instance.getObject(testId1));
        assertSame(testObject1, instance.getNamedObject("Test 1"));

        instance.putNamedObject("Test 2", testObject2);
        assertSame(testObject1, instance.getObject(testId1));
        assertSame(testObject1, instance.getNamedObject("Test 1"));
        assertSame(testObject2, instance.getObject(testId2));
        assertSame(testObject2, instance.getNamedObject("Test 2"));

        instance.removeNamedObject("Test 1");
        assertNull(instance.getObject(testId1));
        assertNull(instance.getNamedObject("Test 1"));
        assertSame(testObject2, instance.getObject(testId2));
        assertSame(testObject2, instance.getNamedObject("Test 2"));
    }
    
    public void testSaveLoad() {
        PersistentObjectDataStore instance = new PersistentObjectDataStore(storeUid);
        SerializableTestClass testObject1 = new SerializableTestClass();
        SerializableTestClass testObject2 = new SerializableTestClass();
        testObject1.setValue(40);
        testObject2.setValue(41);
        long testId1 = testObject1.getUniqueId();
        long testId2 = testObject2.getUniqueId();

        instance.putNamedObject("Test 1", testObject1);
        instance.putObject(testObject2);
        
        instance.save();
        
        instance = new PersistentObjectDataStore(storeUid);
        assertNull(instance.getNamedObject("Test 1"));
        assertNull(instance.getObject(testId1));
        assertNull(instance.getObject(testId2));
        
        instance.load();
        
        Object testLoaded1 = instance.getNamedObject("Test 1");
        Object testLoaded2 = instance.getObject(testId2);
        assertNotNull(testLoaded1);
        assertNotNull(testLoaded2);
        assertTrue(testLoaded1 instanceof SerializableTestClass);
        assertTrue(testLoaded2 instanceof SerializableTestClass);
        testObject1 = (SerializableTestClass)testLoaded1;
        testObject2 = (SerializableTestClass)testLoaded2;
        
        assertEquals(testId1, testObject1.getUniqueId());
        assertEquals(testId2, testObject2.getUniqueId());
        assertEquals(40, testObject1.getValue());
        assertEquals(41, testObject2.getValue());
    }
    
    public Test suite() {
        TestSuite suite = new TestSuite("PersistentObjectDataStore");

        suite.addTest(new PersistentObjectDataStoreTest("Object", new TestMethod()
        { public void run(TestCase tc) {((PersistentObjectDataStoreTest)tc).testObject(); } }));
        suite.addTest(new PersistentObjectDataStoreTest("Named object", new TestMethod()
        { public void run(TestCase tc) {((PersistentObjectDataStoreTest)tc).testNamedObject(); } }));
        suite.addTest(new PersistentObjectDataStoreTest("Save Load", new TestMethod()
        { public void run(TestCase tc) {((PersistentObjectDataStoreTest)tc).testSaveLoad(); } }));

        return suite;
    }
}
