/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.install;

/**
 * The shape a page is drawn in.
 *
 * <p>The look is the installer's, not the machine's: a text-mode installer fills the screen with text whatever
 * the computer under it, and a wizard is a grey box with buttons along the bottom on every machine that can run
 * one. One installer can change shape partway through, which is why this belongs to the page and not to the
 * system: the one that starts blue and text-only finishes behind its own graphical phase.
 */
public enum InstallerChrome {

    /** The whole screen in text, a heading at the top and a line of keys along the bottom. */
    FULL_TEXT,

    /** A grey window over a coloured ground, its title in a tab on the top edge. */
    BOXED_TEXT,

    /** A grey dialog with a picture down one side and Back, Next and Cancel along the bottom. */
    WIZARD,

    /** A coloured ground with the steps listed down one side, and the question in a dialog over it. */
    SIDE_PANEL,

    /** A pale card, the question at the top and the buttons at the bottom right. */
    CARD
}
