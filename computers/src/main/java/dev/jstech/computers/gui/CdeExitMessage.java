/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui;

import java.util.List;

/**
 * What CDE's Exit dialog says before the machine goes down: how many programs are still open, in words the way
 * a person would say it, and that what is not saved goes with them. With nothing open there is nothing to lose,
 * and the warning is left out.
 */
public final class CdeExitMessage {

    private static final String[] WORDS = {"No", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight",
        "Nine", "Ten", "Eleven", "Twelve"};

    private CdeExitMessage() {
    }

    /** The dialog's lines for that many open programs, top to bottom. */
    public static List<String> lines(final int open) {
        final int count = Math.max(0, open);
        if (count == 0) {
            return List.of("No program is open", "on this workstation.");
        }
        final String many = count < WORDS.length ? WORDS[count] : Integer.toString(count);
        final String first = count == 1 ? "One program is still open" : many + " programs are still open";
        return List.of(first, "on this workstation.", "What is not saved will be lost.");
    }
}
