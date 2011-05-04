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

import org.logicprobe.LogicMail.PlatformInfo;

import net.rim.device.api.i18n.Locale;
import net.rim.device.api.system.Characters;
import net.rim.device.api.system.KeypadListener;
import net.rim.device.api.ui.Keypad;
import net.rim.device.api.util.LongIntHashtable;

/**
 * Implements standard mapping tables for keybindings to be used throughout
 * the application.  These mappings are keypad layout and locale sensitive,
 * and are based on the messaging shortcuts listed in various device manuals.
 * It should be noted that keypad locale may be a device-level setting, as it
 * can only be changed on the simulator prior to startup.
 */
public class KeyHandler {
    private static final LongIntHashtable table = new LongIntHashtable();
    private static final LongIntHashtable browserTable = new LongIntHashtable();
    private static boolean isReduced;

    public final static int MESSAGE_REPLY        = 10;
    public final static int MESSAGE_REPLY_ALL    = 11;
    public final static int MESSAGE_FORWARD      = 12;
    public final static int MESSAGE_COMPOSE      = 13;
    public final static int MESSAGE_MARK_OPENED  = 14;

    public final static int SCROLL_TOP           = 21;
    public final static int SCROLL_BOTTOM        = 22;
    public final static int SCROLL_NEXT_DATE     = 23;
    public final static int SCROLL_PREV_DATE     = 24;
    public final static int SCROLL_NEXT_UNOPENED = 25;
    
    public final static int BROWSER_ZOOM_IN      = 30;
    public final static int BROWSER_ZOOM_OUT     = 31;
    
    static {
        buildTable();
    }
    
    /**
     * Get the shortcut mapped to the provided key character.
     * This method is intended to be called from within various
     * <code>keyChar(char c, int status, int time)</code> methods to assist in
     * shortcut implementation.
     *
     * @param ch Character generated.
     * @param status Modifier key status.
     * @return Shortcut for the key, or <code>-1</code> if nothing matched.
     */
    public static int keyCharShortcut(char ch, int status) {
        if(isReduced) {
            char altedChar = Keypad.getAltedChar(ch);
            long packed = ((long) altedChar << 32) | status;
            return table.get(packed);
        }
        else {
            int keyCode = Keypad.getKeyCode(ch, status);
            long packed = ((long) keyCode << 32) | status;
            return table.get(packed);
        }
    }
    
    /**
     * Get the shortcut mapped to the provided key character.
     * This method is intended to be called from within various
     * <code>keyChar(char c, int status, int time)</code> methods to assist in
     * shortcut implementation.
     *
     * @param ch Character generated.
     * @param status Modifier key status.
     * @return Shortcut for the key, or <code>-1</code> if nothing matched.
     */
    public static int keyCharBrowserShortcut(char ch, int status) {
        if(isReduced) {
            char altedChar = Keypad.getAltedChar(ch);
            long packed = ((long) altedChar << 32) | status;
            return browserTable.get(packed);
        }
        else {
            int keyCode = Keypad.getKeyCode(ch, status);
            long packed = ((long) keyCode << 32) | status;
            return browserTable.get(packed);
        }
    }

    /**
     * Causes the key mapping table to be constructed.
     * Normally this method is automatically called on the first use of this
     * class, but it can also be explicitly called to rebuild the table if
     * the device's hardware layout changes.
     */
    public static void buildTable() {
        synchronized(table) {
            table.clear();
            browserTable.clear();
            switch(Keypad.getHardwareLayout()) {
            case Keypad.HW_LAYOUT_39:
            case Keypad.HW_LAYOUT_32:
                buildTable_39_32();
                break;
            case Keypad.HW_LAYOUT_REDUCED_24:
                buildTable_REDUCED_24();
                isReduced = true;
                break;
            case 1230263636: // HW_LAYOUT_ITUT
                buildTable_ITUT();
                isReduced = true;
                break;
            default:
                if(!PlatformInfo.getInstance().hasTouchscreen()) {
                    buildTable_39_32();
                }
                break;
            }
        }
    }

    private static void buildTable_39_32() {
        switch(Keypad.getLocale().getCode()) {
        case Locale.LOCALE_en:
        case Locale.LOCALE_en_US:
        case Locale.LOCALE_en_GB:
            buildTable_39_32_en();
            break;
        case Locale.LOCALE_de:
            buildTable_39_32_de();
            break;
        case Locale.LOCALE_fr:
        case Locale.LOCALE_fr_CA:
            buildTable_39_32_fr();
            break;
        case Locale.LOCALE_es:
        case Locale.LOCALE_es_MX:
            buildTable_39_32_es();
            break;
        case Locale.LOCALE_it:
            buildTable_39_32_it();
            break;
        default:
            // All other supported locales seem to copy the English key mappings
            buildTable_39_32_en();
            break;
        }
    }
    
    private static void buildTable_39_32_en() {
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_R << 32), MESSAGE_REPLY);
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_L << 32), MESSAGE_REPLY_ALL);
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_F << 32), MESSAGE_FORWARD);
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_C << 32), MESSAGE_COMPOSE);
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_U << 32) | KeypadListener.STATUS_ALT, MESSAGE_MARK_OPENED);
        
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_T << 32), SCROLL_TOP);
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_B << 32), SCROLL_BOTTOM);
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_N << 32), SCROLL_NEXT_DATE);
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_P << 32), SCROLL_PREV_DATE);
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_U << 32), SCROLL_NEXT_UNOPENED);
        
        browserTable.put(((long) Characters.LATIN_CAPITAL_LETTER_I << 32), BROWSER_ZOOM_IN);
        browserTable.put(((long) Characters.LATIN_CAPITAL_LETTER_O << 32), BROWSER_ZOOM_OUT);
    }
    
    private static void buildTable_39_32_de() {
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_R << 32), MESSAGE_REPLY);
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_L << 32), MESSAGE_REPLY_ALL);
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_W << 32), MESSAGE_FORWARD);
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_C << 32), MESSAGE_COMPOSE);
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_U << 32) | KeypadListener.STATUS_ALT, MESSAGE_MARK_OPENED);
        
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_O << 32), SCROLL_TOP);
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_U << 32), SCROLL_BOTTOM);
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_N << 32), SCROLL_NEXT_DATE);
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_V << 32), SCROLL_PREV_DATE);
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_H << 32), SCROLL_NEXT_UNOPENED);

        browserTable.put(((long) Characters.LATIN_CAPITAL_LETTER_E << 32), BROWSER_ZOOM_IN);
        browserTable.put(((long) Characters.LATIN_CAPITAL_LETTER_F << 32), BROWSER_ZOOM_OUT);
    }
    
    private static void buildTable_39_32_fr() {
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_R << 32), MESSAGE_REPLY);
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_O << 32), MESSAGE_REPLY_ALL);
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_W << 32), MESSAGE_FORWARD);
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_C << 32), MESSAGE_COMPOSE);
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_U << 32), MESSAGE_MARK_OPENED);
        
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_D << 32), SCROLL_TOP);
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_F << 32), SCROLL_BOTTOM);
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_S << 32), SCROLL_NEXT_DATE);
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_P << 32), SCROLL_PREV_DATE);
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_N << 32), SCROLL_NEXT_UNOPENED);
        
        browserTable.put(((long) Characters.LATIN_CAPITAL_LETTER_C << 32), BROWSER_ZOOM_IN);
        browserTable.put(((long) Characters.LATIN_CAPITAL_LETTER_O << 32) | KeypadListener.STATUS_ALT, BROWSER_ZOOM_OUT);
    }
    
    private static void buildTable_39_32_es() {
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_D << 32), MESSAGE_REPLY);
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_T << 32), MESSAGE_REPLY_ALL);
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_V << 32), MESSAGE_FORWARD);
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_R << 32), MESSAGE_COMPOSE);
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_C << 32), MESSAGE_MARK_OPENED);
        
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_P << 32), SCROLL_TOP);
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_F << 32), SCROLL_BOTTOM);
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_S << 32), SCROLL_NEXT_DATE);
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_A << 32), SCROLL_PREV_DATE);
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_N << 32), SCROLL_NEXT_UNOPENED);
        
        browserTable.put(((long) Characters.LATIN_CAPITAL_LETTER_M << 32) | KeypadListener.STATUS_ALT, BROWSER_ZOOM_IN);
        browserTable.put(((long) Characters.LATIN_CAPITAL_LETTER_O << 32) | KeypadListener.STATUS_ALT, BROWSER_ZOOM_OUT);
    }
    
    private static void buildTable_39_32_it() {
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_R << 32), MESSAGE_REPLY);
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_P << 32), MESSAGE_REPLY_ALL);
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_F << 32), MESSAGE_FORWARD);
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_C << 32), MESSAGE_COMPOSE);
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_U << 32) | KeypadListener.STATUS_ALT, MESSAGE_MARK_OPENED);
        
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_D << 32), SCROLL_TOP);
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_B << 32), SCROLL_BOTTOM);
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_I << 32), SCROLL_NEXT_DATE);
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_T << 32), SCROLL_PREV_DATE);
        table.put(((long) Characters.LATIN_CAPITAL_LETTER_U << 32), SCROLL_NEXT_UNOPENED);
        
        browserTable.put(((long) Characters.LATIN_CAPITAL_LETTER_I << 32), BROWSER_ZOOM_IN);
        browserTable.put(((long) Characters.LATIN_CAPITAL_LETTER_O << 32), BROWSER_ZOOM_OUT);
    }
    
    private static void buildTable_REDUCED_24() {
        // Pearl 81xx, Pearl 82xx, Pearl 9100
        table.put(((long) Characters.EXCLAMATION_MARK << 32), MESSAGE_REPLY);
        table.put(((long) Characters.FULL_STOP << 32), MESSAGE_FORWARD);
        table.put(((long) Characters.COMMA << 32), MESSAGE_COMPOSE);
        
        table.put(((long) Characters.DIGIT_ONE << 32), SCROLL_TOP);
        table.put(((long) Characters.DIGIT_SEVEN << 32), SCROLL_BOTTOM);
        table.put(((long) Characters.DIGIT_SIX << 32), SCROLL_NEXT_DATE);
        table.put(((long) Characters.DIGIT_FOUR << 32), SCROLL_PREV_DATE);
        
        browserTable.put(((long) Characters.COMMA << 32), BROWSER_ZOOM_IN);
        browserTable.put(((long) Characters.FULL_STOP << 32), BROWSER_ZOOM_OUT);
    }

    private static void buildTable_ITUT() {
        // Pearl 9105
        table.put(((long) Characters.DIGIT_SEVEN << 32), MESSAGE_REPLY);
        table.put(((long) Characters.DIGIT_ONE << 32), MESSAGE_REPLY_ALL);
        table.put(((long) Characters.DIGIT_NINE << 32), MESSAGE_FORWARD);
        
        table.put(((long) Characters.DIGIT_TWO << 32), SCROLL_TOP);
        table.put(((long) Characters.DIGIT_EIGHT << 32), SCROLL_BOTTOM);
        table.put(((long) Characters.DIGIT_SIX << 32), SCROLL_NEXT_DATE);
        table.put(((long) Characters.DIGIT_FOUR << 32), SCROLL_PREV_DATE);
        table.put(((long) Characters.DIGIT_FIVE << 32), SCROLL_NEXT_UNOPENED);
        
        browserTable.put(((long) Characters.DIGIT_THREE << 32), BROWSER_ZOOM_IN);
        browserTable.put(((long) Characters.DIGIT_NINE << 32), BROWSER_ZOOM_OUT);
    }
}
