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

package j2meunit.rimui;

import j2meunit.framework.AssertionFailedError;
import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestFailure;
import j2meunit.framework.TestListener;
import j2meunit.framework.TestResult;
import j2meunit.framework.TestSuite;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import net.rim.device.api.system.Bitmap;
import net.rim.device.api.system.Characters;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.GaugeField;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.component.SeparatorField;
import net.rim.device.api.ui.component.TreeField;
import net.rim.device.api.ui.component.TreeFieldCallback;
import net.rim.device.api.ui.container.MainScreen;

/**
 * J2MEUnit Test Console Screen for the BlackBerry UI.
 * 
 * @author Derek Konigsberg
 */
public class TestConsoleScreen extends MainScreen implements TestListener {
    private LabelField titleLabel;
    private LabelField statusLabel;
    private LabelField failureLabel;
    private LabelField errorLabel;
    private LabelField timeLabel;
    private GaugeField progressGauge;
    private TreeField testTreeField;

    private TestSuite mainTestSuite;
    private Hashtable testTreeItems;
    private TestResult testResults;
    private Test selectedTest;
    private long elapsedTime;

    private Bitmap testInitialBitmap = Bitmap.getBitmapResource("test_initial.png");
    private Bitmap testPassedBitmap = Bitmap.getBitmapResource("test_passed.png");
    private Bitmap testFailedBitmap = Bitmap.getBitmapResource("test_failed.png");

    private static class TestTreeItem {
        public TestTreeItem(Test test) {
            this.test = test;
            if(test instanceof TestCase) {
                name = ((TestCase)test).getName();
            }
            else if(test instanceof TestSuite) {
                name = ((TestSuite)test).toString();
            }
            else {
                name = "Test";
            }
            this.hasRun = false;
            this.hasPassed = true;
            this.id = 0;
        }
        public Test test;
        public String name;
        public boolean hasRun;
        public boolean hasPassed;
        public int id;
    }

    /**
     * Creates a new instance of TestConsoleScreen
     */
    public TestConsoleScreen(String[] testCaseClasses) {
        initializeFields();
        testTreeItems = new Hashtable();
        mainTestSuite = createTestSuite(testCaseClasses);
        elapsedTime = -1;
        populateTestTree(mainTestSuite, 0);
    }

    private void initializeFields() {
        Font labelFont = Font.getDefault().derive(Font.PLAIN, 12);
        Font treeFont = Font.getDefault().derive(Font.PLAIN, 14);

        titleLabel = new LabelField("J2MEUnit " + j2meunit.util.Version.id());
        titleLabel.setFont(labelFont);
        statusLabel = new LabelField("Status: Idle");
        statusLabel.setFont(labelFont);
        failureLabel = new LabelField("Failures: 0");
        failureLabel.setFont(labelFont);
        errorLabel = new LabelField("Errors: 0");
        errorLabel.setFont(labelFont);
        timeLabel = new LabelField("Time: n/a");
        timeLabel.setFont(labelFont);
        progressGauge = new GaugeField(null, 0, 100, 0, GaugeField.NO_TEXT);
        progressGauge.setFont(labelFont);

        testTreeField = new TreeField(new TreeFieldCallback() {
            public void drawTreeItem(TreeField treeField, Graphics graphics, int node, int y, int width, int indent) {
                testTreeField_DrawTreeItem(treeField, graphics, node, y, width, indent);
            }
        }, Field.FOCUSABLE);
        testTreeField.setEmptyString("No tests", 0);
        testTreeField.setDefaultExpanded(true);
        testTreeField.setIndentWidth(20);
        testTreeField.setFont(treeFont);
        testTreeField.setRowHeight(treeFont.getHeight() + 2);

        add(titleLabel);
        add(statusLabel);
        add(failureLabel);
        add(errorLabel);
        add(timeLabel);
        add(progressGauge);
        add(new SeparatorField());
        add(testTreeField);
    }

    private void populateTestTree(TestSuite suite, int parentId) {
        // Get the tests
        Vector tests = new Vector();
        for(Enumeration e = suite.tests(); e.hasMoreElements(); ) {
            tests.addElement(e.nextElement());
        }

        // Iterate backwards so the tree is populated in the correct order
        int size = tests.size();
        for(int i = size - 1; i >= 0; i--) {
            Test test = (Test)tests.elementAt(i);
            TestTreeItem item = new TestTreeItem(test);
            int id = testTreeField.addChildNode(parentId, item);
            item.id = id;
            testTreeItems.put(test.toString(), item);
            if(test instanceof TestSuite) {
                populateTestTree((TestSuite)test, id);
            }
        }
    }

    /** Standard Escape-key handler */
    public boolean keyChar(char key, int status, int time) {
        boolean retval = false;
        switch (key) {
        case 'n':
            retval = nextSibling();
            break;
        case 'p':
            retval = prevSibling();
            break;
        case Characters.ENTER:
            retval = openResultsForCurrentNode();
            break;
        case Characters.ESCAPE:
            System.exit(1);
            break;
        default:
            retval = super.keyChar(key, status, time);
        }
        return retval;
    }

    private boolean nextSibling() {
        int id = testTreeField.getCurrentNode();
        if(id == -1) { return false; }
        int nextId = testTreeField.getNextSibling(id);
        if(nextId == -1) { return false; }
        
        testTreeField.setCurrentNode(nextId);
        return true;
    }

    private boolean prevSibling() {
        int id = testTreeField.getCurrentNode();
        if(id == -1) { return false; }
        int prevId = testTreeField.getPreviousSibling(id);
        if(prevId == -1) { return false; }
        
        testTreeField.setCurrentNode(prevId);
        return true;
    }

    private boolean openResultsForCurrentNode() {
        int node = testTreeField.getCurrentNode();
        if(node > 0) {
            TestTreeItem item = (TestTreeItem)testTreeField.getCookie(node);
            if(item.hasRun && item.test instanceof TestCase) {
                showTestResults();
                return true;
            }
        }
        return false;
    }

    private void testTreeField_DrawTreeItem(TreeField treeField, Graphics graphics, int node, int y, int width, int indent) {
        Object cookie = testTreeField.getCookie(node);

        if(cookie instanceof TestTreeItem) {
            TestTreeItem item = (TestTreeItem)cookie;
            int height = testTreeField.getRowHeight();

            Bitmap iconBitmap = getItemIcon(item);
            
            graphics.drawBitmap(indent, y, 16, 16, iconBitmap, 0, 0);

            graphics.drawText(item.name, indent + height + 2, y, Graphics.ELLIPSIS, width - height - 2);
        }
    }

    private Bitmap getItemIcon(TestTreeItem item) {
        Bitmap iconBitmap;
        if(item.hasRun) {
            if(item.hasPassed) {
                iconBitmap = testPassedBitmap;
            }
            else {
                iconBitmap = testFailedBitmap;
            }
        }
        else {
            iconBitmap = testInitialBitmap;
        }
        return iconBitmap;
    }

    private MenuItem resultsItem = new MenuItem("View results", 300100, 1010) {
        public void run() {
            showTestResults();
        }
    };
    private MenuItem runAllItem = new MenuItem("Run all", 400110, 1020) {
        public void run() {
            runAllTests();
        }
    };
    private MenuItem runSelectedItem = new MenuItem("Run selected", 400120, 1030) {
        public void run() {
            runSelectedTests();
        }
    };
    private MenuItem exitItem = new MenuItem("Exit", 60000100, 9000) {
        public void run() {
            System.exit(0);
        }
    };

    protected void makeMenu(Menu menu, int instance) {
        int node = testTreeField.getCurrentNode();
        if(node > 0) {
            TestTreeItem item = (TestTreeItem)testTreeField.getCookie(node);
            if(item.hasRun && item.test instanceof TestCase) {
                menu.add(resultsItem);
            }
        }
        menu.add(runAllItem);
        menu.add(runSelectedItem);
        menu.add(exitItem);
    }

    public synchronized void addError(Test test, Throwable throwable) {
        synchronized(UiApplication.getEventLock()) {
            errorLabel.setText("Errors: " + testResults.errorCount());
        }
    }

    public synchronized void addFailure(Test test, AssertionFailedError assertionFailedError) {
        synchronized(UiApplication.getEventLock()) {
            failureLabel.setText("Failures: " + testResults.failureCount());
        }
    }

    public void endTest(Test test) {
        synchronized(UiApplication.getEventLock()) {
            progressGauge.setValue(progressGauge.getValue() + 1);
        }
        updateTreeErrors();
        updateTestBranch(test);
        updateTestTree();
    }

    public void endTestStep(Test test) {
        synchronized(UiApplication.getEventLock()) {
            progressGauge.setValue(progressGauge.getValue() + 1);
        }
        updateTreeErrors();
        updateTestBranch(test);
        updateTestTree();
    }

    public synchronized void startTest(Test test) {
        TestTreeItem item = (TestTreeItem)testTreeItems.get(test.toString());
        item.hasRun = true;
    }

    private void updateTestBranch(Test test) {
        TestTreeItem item = (TestTreeItem)testTreeItems.get(test.toString());
        if(item == null) return;
        boolean hasAllRun = true;
        boolean hasAllPassed = true;
        int parentId = testTreeField.getParent(item.id);
        if(parentId <= 0) return;
        int node = testTreeField.getFirstChild(parentId);
        if(node == -1) return;
        while(node != -1) {
            item = (TestTreeItem)testTreeField.getCookie(node);
            if(!item.hasRun) hasAllRun = false;
            if(!item.hasPassed) hasAllPassed = false;
            node = testTreeField.getNextSibling(node);
        }
        item = (TestTreeItem)testTreeField.getCookie(parentId);
        item.hasRun = hasAllRun;
        item.hasPassed = hasAllPassed;

        if(testTreeField.getParent(item.id) >= 0) {
            updateTestBranch(item.test);
        }
    }

    private void updateTreeErrors() {
        // Update the test data structures
        TestTreeItem item;
        Enumeration e;
        TestFailure failure;
        for(e = testResults.failures(); e.hasMoreElements(); ) {
            failure = (TestFailure)e.nextElement();
            item = (TestTreeItem)testTreeItems.get(failure.failedTest().toString());
            item.hasPassed = false;
        }
        for(e = testResults.errors(); e.hasMoreElements(); ) {
            failure = (TestFailure)e.nextElement();
            item = (TestTreeItem)testTreeItems.get(failure.failedTest().toString());
            item.hasPassed = false;
        }
    }

    private synchronized void updateTestTree() {
        // Repaint the tree
        UiApplication.getUiApplication().invokeAndWait(new Runnable() {
            public void run() {
                testTreeField.setDirty(true);
                invalidate();
                doPaint();
            }
        });        
    }

    private void showTestResults() {
        int node = testTreeField.getCurrentNode();
        if(node <= 0) return;
        TestTreeItem item = (TestTreeItem)testTreeField.getCookie(node);
        if(!(item.test instanceof TestCase)) return;
        TestCase testCase = (TestCase)item.test;

        Enumeration e;
        TestFailure testFailure = null;
        TestFailure testError = null;

        for(e = testResults.failures(); e.hasMoreElements(); ) {
            TestFailure failure = (TestFailure)e.nextElement();
            if(failure.failedTest() == testCase) {
                testFailure = failure;
                break;
            }
        }

        for(e = testResults.errors(); e.hasMoreElements(); ) {
            TestFailure error = (TestFailure)e.nextElement();
            if(error.failedTest() == testCase) {
                testError = error;
                break;
            }
        }

        UiApplication.getUiApplication().pushModalScreen(
                new TestResultsScreen(testCase, testFailure, testError));
    }

    /**
     * Builds a test suite from all test case classes in a string array.
     *
     * @param  testCaseClasses A string array containing the test case class names
     * @return A test suite containing all tests
     */
    protected TestSuite createTestSuite(String[] testCaseClasses) {
        TestSuite testSuite = new TestSuite();
        if (testCaseClasses.length < 1) {
            return testSuite;
        }

        for (int i = 0; i < testCaseClasses.length; i++) {
            try {
                String className = testCaseClasses[i];
                TestCase testCase =
                    (TestCase)Class.forName(className).newInstance();
                testSuite.addTest(testCase.suite());
            } catch (Throwable t) {
                System.out.println("Access to TestCase " + testCaseClasses[i] +
                        " failed: " + t.getMessage() + " - " +
                        t.getClass().getName());
            }
        }

        return testSuite;
    }

    /**
     * Run all tests in the given test suite.
     *
     * @param suite The test suite to run
     */
    protected void doRun(Test suite) {
        testResults = new TestResult();
        testResults.addListener(this);

        long startTime = System.currentTimeMillis();

        // Mark the suite's tree item
        TestTreeItem item = (TestTreeItem)testTreeItems.get(suite.toString());
        if(item != null)
            item.hasRun = true;

        // Run the suite
        suite.run(testResults);

        long endTime = System.currentTimeMillis();
        elapsedTime = endTime - startTime;
    }

    private void runAllTests() {
        selectedTest = mainTestSuite;
        runTests();
    }

    private void runSelectedTests() {
        int nodeId = testTreeField.getCurrentNode();
        if(nodeId == -1) return;
        selectedTest = ((TestTreeItem)testTreeField.getCookie(nodeId)).test;
        runTests();
    }

    private void runTests() {
        statusLabel.setText("Status: Running...");
        progressGauge.reset(null, 0, selectedTest.countTestCases(), 0);
        new Thread() {
            public void run() {
                Thread.yield();
                try {
                    doRun(selectedTest);
                    updateResults();
                } catch (Throwable t) {
                    System.out.println("Exception while running test: " + t);
                    t.printStackTrace();
                }
            }
        }.start();
    }

    private synchronized void updateResults() {
        UiApplication.getUiApplication().invokeAndWait(new Runnable() {
            public void run() {
                statusLabel.setText("Status: Idle");
                if(elapsedTime >= 0) {
                    timeLabel.setText("Time: " + elapsedTime + "ms");
                }
            }
        });
    }
}
