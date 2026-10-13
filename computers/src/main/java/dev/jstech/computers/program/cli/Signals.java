/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import java.util.List;
import java.util.Locale;

/**
 * The signals {@code kill} sends, by the numbers and names every Unix gives them, and how the shells read one off the
 * line: {@code -9}, {@code -KILL} and {@code -SIGKILL} all name the same, in either case.
 */
public final class Signals {

    /** What {@code kill} sends when it is not told which: the polite request to end. */
    public static final int TERM = 15;

    /** The signals a process can be sent here, in their numbers' order. */
    public static final List<Signal> ALL = List.of(new Signal(1, "HUP"), new Signal(2, "INT"),
            new Signal(3, "QUIT"), new Signal(9, "KILL"), new Signal(TERM, "TERM"));

    private static final String PREFIX = "SIG";

    private Signals() {
    }

    /** One signal: its number and its name without the {@code SIG} every one of them starts with. */
    public record Signal(int number, String name) {
    }

    /** The number of the signal {@code option} names, its leading dash taken off; -1 when it names none of them. */
    public static int parse(final String option) {
        final String named = option.startsWith("-") ? option.substring(1) : option;
        if (named.isEmpty()) {
            return -1;
        }
        if (named.chars().allMatch(Character::isDigit)) {
            final int number = named.length() > 3 ? -1 : Integer.parseInt(named);
            return ALL.stream().anyMatch(one -> one.number() == number) ? number : -1;
        }
        final String upper = named.toUpperCase(Locale.ROOT);
        final String bare = upper.startsWith(PREFIX) ? upper.substring(PREFIX.length()) : upper;
        return ALL.stream().filter(one -> one.name().equals(bare)).mapToInt(Signal::number).findFirst().orElse(-1);
    }

    /** The list {@code kill -l} prints: each number with its signal's whole name. */
    public static String listing() {
        final StringBuilder out = new StringBuilder();
        for (final Signal one : ALL) {
            if (!out.isEmpty()) {
                out.append("  ");
            }
            out.append(one.number()).append(") ").append(PREFIX).append(one.name());
        }
        return out.toString();
    }
}
