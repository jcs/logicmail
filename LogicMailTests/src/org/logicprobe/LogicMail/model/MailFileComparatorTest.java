/*-
 * Copyright (c) 2009, Derek Konigsberg
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

import java.util.Vector;

import net.rim.device.api.util.SimpleSortingVector;
import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestMethod;
import j2meunit.framework.TestSuite;

public class MailFileComparatorTest extends TestCase {
    public MailFileComparatorTest() {
    }
    
    public MailFileComparatorTest(String testName, TestMethod testMethod) {
        super(testName, testMethod);
    }

    public void setUp() {
    }

    public void tearDown() {
    }
    
    public void testSortImapFiles() {
        String[] input = {
                "74.msg",
                "23.msg",
                "a0.msg",
                "ba.msg",
                "104.msg",
                "c.msg"
        };
        
        String[] expected = {
                "c.msg",
                "23.msg",
                "74.msg",
                "a0.msg",
                "ba.msg",
                "104.msg"
        };
        
        SimpleSortingVector vector = new SimpleSortingVector();
        vector.setSortComparator(new MailFileComparator());
        vector.setSort(true);
        addAll(vector, input);
        
        int size = vector.size();
        for(int i=0; i<size; i++) {
            assertEquals("Item " + i, expected[i], vector.elementAt(i));
        }
    }
    
    public void testSortPopFiles() {
        String[] input = {
                "1255368645.133.msg",
                "1255368645.134.msg",
                "1255368645.135.msg",
                "1255368645.88.msg",
                "1255368645.89.msg",
                "1255368645.90.msg"
        };
        
        String[] expected = {
                "1255368645.88.msg",
                "1255368645.89.msg",
                "1255368645.90.msg",
                "1255368645.133.msg",
                "1255368645.134.msg",
                "1255368645.135.msg"
        };
        
        SimpleSortingVector vector = new SimpleSortingVector();
        vector.setSortComparator(new MailFileComparator());
        vector.setSort(true);
        addAll(vector, input);
        
        int size = vector.size();
        for(int i=0; i<size; i++) {
            assertEquals("Item " + i, expected[i], vector.elementAt(i));
        }
    }

// Cannot test this case until we try to pick hex numbers out of non-numeric
// strings.  Since every mail server may use a different format for these,
// it almost might make sense to just to research and implement several
// different special cases.
//    public void testSortOtherFiles() {
//        String[] input = {
//                "AmailId1238a4df5d3901c1",
//                "AmailId12282f454e1f7cde",
//                "AmailId1235dcec9a38af53",
//                "AmailId1236766c21a73c07",
//                "AmailId123a65a0783f0692",
//                "AmailId122a05f95ad37fb9"
//        };
//        
//        String[] expected = {
//                "AmailId12282f454e1f7cde",
//                "AmailId122a05f95ad37fb9",
//                "AmailId1235dcec9a38af53",
//                "AmailId1236766c21a73c07",
//                "AmailId1238a4df5d3901c1",
//                "AmailId123a65a0783f0692"
//        };
//        
//        SimpleSortingVector vector = new SimpleSortingVector();
//        vector.setSortComparator(new MailFileComparator());
//        vector.setSort(true);
//        addAll(vector, input);
//        
//        int size = vector.size();
//        for(int i=0; i<size; i++) {
//            assertEquals("Item " + i, expected[i], vector.elementAt(i));
//        }
//    }
    
    private static void addAll(Vector vector, Object[] source) {
        for(int i=0; i<source.length; i++) {
            vector.addElement(source[i]);
        }
    }
    
    public Test suite() {
        TestSuite suite = new TestSuite("MailFileComparator");

        suite.addTest(new MailFileComparatorTest("sortImapFiles", new TestMethod()
        { public void run(TestCase tc) {((MailFileComparatorTest)tc).testSortImapFiles(); } }));
        suite.addTest(new MailFileComparatorTest("sortPopFiles", new TestMethod()
        { public void run(TestCase tc) {((MailFileComparatorTest)tc).testSortPopFiles(); } }));
//        suite.addTest(new MailFileComparatorTest("sortOtherFiles", new TestMethod()
//        { public void run(TestCase tc) {((MailFileComparatorTest)tc).testSortOtherFiles(); } }));

        return suite;
    }
}
