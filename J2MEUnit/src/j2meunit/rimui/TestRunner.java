/*
 * TestRunner.java
 *
 * Created on February 2, 2007, 10:03 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package j2meunit.rimui;

import net.rim.device.api.ui.UiApplication;

/**
 * Each test project needs to invoke TestRunner.
 * @author Derek Konigsberg
 */
public class TestRunner extends UiApplication {
    
    /** Creates a new instance of TestRunner */
    public TestRunner(String[] testCaseClasses) {
        pushScreen(new TestConsoleScreen(testCaseClasses));
    }
}
