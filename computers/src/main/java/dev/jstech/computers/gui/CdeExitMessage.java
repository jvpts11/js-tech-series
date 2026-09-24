/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;

/**
 * What CDE's Exit dialog says before the machine goes down: how many programs are still open, in words the way
 * a person would say it, and that what is not saved goes with them. With nothing open there is nothing to lose,
 * and the warning is left out.
 */
@TextHolder
public final class CdeExitMessage {

    /** The dialog's title. */
    public static final TextKey TITLE = TextKey.of("jsc.cde.exit.title", "Exit");
    /** Its buttons, left to right. */
    public static final TextKey SHUT_DOWN = TextKey.of("jsc.cde.exit.shut_down", "Shut Down");
    public static final TextKey RESTART = TextKey.of("jsc.cde.exit.restart", "Restart");
    public static final TextKey CANCEL = TextKey.of("jsc.cde.exit.cancel", "Cancel");

    private static final TextKey NONE_OPEN = TextKey.of("jsc.cde.exit.none_open", "No program is open");
    private static final TextKey ONE_OPEN = TextKey.of("jsc.cde.exit.one_open", "One program is still open");
    private static final TextKey MANY_OPEN = TextKey.of("jsc.cde.exit.many_open", "%s programs are still open");
    private static final TextKey WHERE = TextKey.of("jsc.cde.exit.where", "on this workstation.");
    private static final TextKey WARNING = TextKey.of("jsc.cde.exit.warning", "What is not saved will be lost.");
    private static final TextKey TWO = TextKey.of("jsc.cde.exit.count.two", "Two");
    private static final TextKey THREE = TextKey.of("jsc.cde.exit.count.three", "Three");
    private static final TextKey FOUR = TextKey.of("jsc.cde.exit.count.four", "Four");
    private static final TextKey FIVE = TextKey.of("jsc.cde.exit.count.five", "Five");
    private static final TextKey SIX = TextKey.of("jsc.cde.exit.count.six", "Six");
    private static final TextKey SEVEN = TextKey.of("jsc.cde.exit.count.seven", "Seven");
    private static final TextKey EIGHT = TextKey.of("jsc.cde.exit.count.eight", "Eight");
    private static final TextKey NINE = TextKey.of("jsc.cde.exit.count.nine", "Nine");
    private static final TextKey TEN = TextKey.of("jsc.cde.exit.count.ten", "Ten");
    private static final TextKey ELEVEN = TextKey.of("jsc.cde.exit.count.eleven", "Eleven");
    private static final TextKey TWELVE = TextKey.of("jsc.cde.exit.count.twelve", "Twelve");

    /* The counts a person says in words, from two; none and one have sentences of their own. */
    private static final List<TextKey> WORDS =
            List.of(TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE, TEN, ELEVEN, TWELVE);
    private static final int FIRST_WORD = 2;

    private CdeExitMessage() {
    }

    /** The dialog's lines for that many open programs, top to bottom. */
    public static List<Text> lines(final int open) {
        final int count = Math.max(0, open);
        if (count == 0) {
            return List.of(NONE_OPEN.text(), WHERE.text());
        }
        final Text first;
        if (count == 1) {
            first = ONE_OPEN.text();
        } else {
            final int word = count - FIRST_WORD;
            first = MANY_OPEN.with(word < WORDS.size() ? WORDS.get(word).text()
                    : Text.literal(Integer.toString(count)));
        }
        return List.of(first, WHERE.text(), WARNING.text());
    }
}
