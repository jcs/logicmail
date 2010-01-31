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
package org.logicprobe.LogicMail.ui;

import net.rim.device.api.ui.Screen;

import org.logicprobe.LogicMail.PlatformInfo;
import org.logicprobe.LogicMail.model.AccountNode;
import org.logicprobe.LogicMail.model.MailRootNode;
import org.logicprobe.LogicMail.model.MailboxNode;
import org.logicprobe.LogicMail.model.MessageNode;
import org.logicprobe.LogicMail.util.PlatformUtils;

public abstract class ScreenFactory {
    private static ScreenFactory instance;

    /**
     * Array of concrete ScreenFactory classes, in order from the highest
     * API version to the lowest.
     */
    private static String[] factoryClasses = {
        "org.logicprobe.LogicMail.ui.ScreenFactoryBB50",
        "org.logicprobe.LogicMail.ui.ScreenFactoryBB47",
        "org.logicprobe.LogicMail.ui.ScreenFactoryBB42"
    };

    /**
     * Array of concrete ScreenFactory classes, in order from the highest
     * API version to the lowest, for touch screen devices.
     */
    private static String[] factoryClassesTouch = {
        "org.logicprobe.LogicMail.ui.ScreenFactoryBB50T",
        "org.logicprobe.LogicMail.ui.ScreenFactoryBB47T",
    };
    
    public static synchronized ScreenFactory getInstance() {
        if(instance == null) {
            if(PlatformInfo.getInstance().hasTouchscreen()) {
                instance = (ScreenFactory)PlatformUtils.getFactoryInstance(factoryClassesTouch);
            }
            else {
                instance = (ScreenFactory)PlatformUtils.getFactoryInstance(factoryClasses);
            }
        }
        return instance;
    }

    /** Constant for a fade transition. */
    public static final int TRANSITION_FADE = 1;
    /** Constant for a slide transition. */
    public static final int TRANSITION_SLIDE = 2;
    /** Constant for a wipe transition. */
    public static final int TRANSITION_WIPE = 3;
    /** Constant for a zoom transition. */
    public static final int TRANSITION_ZOOM = 4;
    /** Constant for no transition. */
    public static final int TRANSITION_NONE = 5;
    
    public abstract void attachScreenTransition(Screen screen, int transitionType);
    
    public abstract void clearScreenTransition(Screen screen);
    
    public abstract StandardScreen getMailHomeScreen(NavigationController navigationController, MailRootNode mailRootNode);

    public abstract StandardScreen getMailboxScreen(NavigationController navigationController, MailboxNode mailboxNode);

    public abstract StandardScreen getMessageScreen(NavigationController navigationController, MessageNode messageNode);

    public abstract StandardScreen getCompositionScreen(NavigationController navigationController, AccountNode accountNode);

    public abstract StandardScreen getCompositionScreen(NavigationController navigationController, AccountNode accountNode, MessageNode messageNode);

    public abstract StandardScreen getCompositionScreen(NavigationController navigationController, AccountNode accountNode, String address);

    public abstract StandardScreen getCompositionReplyScreen(NavigationController navigationController, AccountNode accountNode, MessageNode messageNode, boolean replyAll);

    public abstract StandardScreen getCompositionForwardScreen(NavigationController navigationController, AccountNode accountNode, MessageNode messageNode);
}
