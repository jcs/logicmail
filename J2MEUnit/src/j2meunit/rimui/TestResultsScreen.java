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

import j2meunit.framework.TestCase;
import j2meunit.framework.TestFailure;
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
