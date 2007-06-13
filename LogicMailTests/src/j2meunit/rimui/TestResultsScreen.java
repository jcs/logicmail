/*
 * TestResultsScreen.java
 *
 * Created on February 4, 2007, 2:27 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package j2meunit.rimui;

import j2meunit.framework.TestCase;
import j2meunit.framework.TestFailure;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.component.RichTextField;
import net.rim.device.api.ui.component.SeparatorField;
import net.rim.device.api.ui.container.MainScreen;

/**
 * This screen shows test results for a single TestCase
 * @author Derek Konigsberg
 */
public class TestResultsScreen extends MainScreen {
    private TestCase testCase;
    private TestFailure testFailure;
    private TestFailure testError;
    
    /**
     * Creates a new instance of TestResultsScreen.
     * It is normally expected that an unsuccessful test will produce
     * either an error or a failure.  Since they are both the same type,
     * they are passed as separate parameters, and passing both is possible.
     *
     * @param testCase    The test case being viewed
     * @param testFailure The failure object
     * @param testError   The error object
     */
    public TestResultsScreen(TestCase testCase, TestFailure testFailure, TestFailure testError) {
        this.testCase = testCase;
        this.testFailure = testFailure;
        this.testError = testError;
        initializeFields();
    }
    
    /**
     * Initialize the screen fields.
     */
    private void initializeFields() {
        add(new RichTextField(testCase.getName(), RichTextField.TEXT_ALIGN_HCENTER));
        if(testFailure != null) {
            add(new SeparatorField());
            add(new RichTextField("Failure:", RichTextField.TEXT_ALIGN_HCENTER));
            RichTextField messageField = new RichTextField(testFailure.thrownException().getMessage());
            messageField.setEditable(false);
            add(messageField);
        }
        if(testError != null) {
            add(new SeparatorField());
            add(new RichTextField("Error:", RichTextField.TEXT_ALIGN_HCENTER));
            RichTextField messageField = new RichTextField(testError.thrownException().getMessage());
            messageField.setEditable(false);
            add(messageField);
        }
        if(testFailure == null && testError == null) {
            add(new SeparatorField());
            add(new RichTextField("Test successful", RichTextField.TEXT_ALIGN_HCENTER));
        }
    }
}
