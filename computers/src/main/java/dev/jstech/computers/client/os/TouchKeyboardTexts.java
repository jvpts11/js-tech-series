/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What the touch keyboard of Frames 10 and Frames 11 says on its bar and its keys. */
@TextHolder
final class TouchKeyboardTexts {

    static final TextKey TITLE = TextKey.of("jsc.desktop.touch_keyboard.title", "Touch keyboard");
    static final TextKey CLOSE = TextKey.of("jsc.desktop.touch_keyboard.close", "x");
    static final TextKey BACKSPACE = TextKey.of("jsc.desktop.touch_keyboard.backspace", "Bksp");
    static final TextKey ENTER = TextKey.of("jsc.desktop.touch_keyboard.enter", "Enter");
    static final TextKey SHIFT = TextKey.of("jsc.desktop.touch_keyboard.shift", "Shift");
    static final TextKey SYMBOLS = TextKey.of("jsc.desktop.touch_keyboard.symbols", "&123");
    static final TextKey LETTERS = TextKey.of("jsc.desktop.touch_keyboard.letters", "abc");
    static final TextKey CTRL = TextKey.of("jsc.desktop.touch_keyboard.ctrl", "Ctrl");
    static final TextKey SMILE = TextKey.of("jsc.desktop.touch_keyboard.smile", ":)");
    static final TextKey LEFT = TextKey.of("jsc.desktop.touch_keyboard.left", "<");
    static final TextKey RIGHT = TextKey.of("jsc.desktop.touch_keyboard.right", ">");
    static final TextKey HIDE = TextKey.of("jsc.desktop.touch_keyboard.hide", "Hide");

    private TouchKeyboardTexts() {
    }
}
