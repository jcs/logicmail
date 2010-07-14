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
package org.logicprobe.LogicMail.ui;

import net.rim.device.api.system.ControlledAccessException;
import net.rim.device.api.ui.Screen;
import net.rim.device.api.ui.TransitionContext;
import net.rim.device.api.ui.Ui;
import net.rim.device.api.ui.UiEngineInstance;
import net.rim.device.api.ui.picker.FilePicker;

public class ScreenFactoryBB50 extends ScreenFactoryBB42 {
    public void attachScreenTransition(Screen screen, int transitionType) {
        UiEngineInstance uiEngine = Ui.getUiEngineInstance();
        TransitionContext pushAction = null;
        TransitionContext popAction = null;
        
        switch (transitionType) {
        case TransitionContext.TRANSITION_FADE:
            pushAction = new TransitionContext(TransitionContext.TRANSITION_FADE);
            popAction = new TransitionContext(TransitionContext.TRANSITION_FADE);

            pushAction.setIntAttribute(TransitionContext.ATTR_DURATION, 750);
            popAction.setIntAttribute(TransitionContext.ATTR_DURATION, 750);
            break;
        case TransitionContext.TRANSITION_NONE:
            pushAction = new TransitionContext(TransitionContext.TRANSITION_NONE);
            popAction = new TransitionContext(TransitionContext.TRANSITION_NONE);
            break;
        case TransitionContext.TRANSITION_SLIDE:
            pushAction = new TransitionContext(TransitionContext.TRANSITION_SLIDE);
            popAction = new TransitionContext(TransitionContext.TRANSITION_SLIDE);
            pushAction.setIntAttribute(TransitionContext.ATTR_DIRECTION, TransitionContext.DIRECTION_LEFT);
            popAction.setIntAttribute(TransitionContext.ATTR_DIRECTION, TransitionContext.DIRECTION_RIGHT);
            break;
        case TransitionContext.TRANSITION_WIPE:
            pushAction = new TransitionContext(TransitionContext.TRANSITION_WIPE);
            popAction = new TransitionContext(TransitionContext.TRANSITION_WIPE);
            break;
        case TransitionContext.TRANSITION_ZOOM:
            pushAction = new TransitionContext(TransitionContext.TRANSITION_ZOOM);
            popAction = new TransitionContext(TransitionContext.TRANSITION_ZOOM);
            pushAction.setIntAttribute(TransitionContext.ATTR_KIND, TransitionContext.KIND_IN);
            popAction.setIntAttribute(TransitionContext.ATTR_KIND, TransitionContext.KIND_OUT);
            break;
        default:
            pushAction = new TransitionContext(TransitionContext.TRANSITION_NONE);
            popAction = new TransitionContext(TransitionContext.TRANSITION_NONE);
        }
        uiEngine.setTransition(null, screen, UiEngineInstance.TRIGGER_PUSH, pushAction);
        uiEngine.setTransition(screen, null, UiEngineInstance.TRIGGER_POP, popAction);
    }

    public void clearScreenTransition(Screen screen) {
        UiEngineInstance uiEngine = Ui.getUiEngineInstance();
        uiEngine.setTransition(null, screen, UiEngineInstance.TRIGGER_PUSH, null);
        uiEngine.setTransition(screen, null, UiEngineInstance.TRIGGER_POP, null);
    }
    
    public String showFilePicker() {
        String fileUrl;
        try {
            fileUrl = FilePicker.getInstance().show();
        } catch (ControlledAccessException e) {
            // There is a bug (JAVAAPI-830) in certain BlackBerry OS 5.0
            // releases that causes this exception to be thrown if the new
            // FilePicker API is used on an actual device.  To avoid having to
            // know specifically when the FilePicker will work, we catch this
            // exception and fall back to our own implementation.
            fileUrl = super.showFilePicker();
        }
        
        return fileUrl;
    }
}
