/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.help;

/**
 * How a help command hands the terminal to its viewer: the name the viewer is opened on, which says what to show.
 *
 * <p>The viewers draw on the player's side, where the manuals are, so all a machine sends is this name: the system for
 * the DOS family's HELP (whose title bar names it), what was typed after {@code info}, or the entry {@code man} found.
 * None of them is a file, and the machine has none of them to read; the viewer asks for nothing but the name.
 */
public final class HelpViews {

    private static final String DOS = "help:";
    private static final String INFO = "info:";
    private static final String MANUAL = "manual:";

    private HelpViews() {
    }

    /** Whether a name is any of the help viewers', which the machine answers with nothing to read. */
    public static boolean names(final String view) {
        return namesDos(view) || namesInfo(view) || namesManual(view);
    }

    /** The DOS family's HELP on a system, by its serialized name, opened on what was typed after it. */
    public static String dos(final String system, final String topic) {
        return DOS + system + ":" + (topic == null ? "" : topic);
    }

    /** Whether a name is the DOS family's HELP. */
    public static boolean namesDos(final String view) {
        return view != null && view.startsWith(DOS);
    }

    /** The system's serialized name a DOS HELP was opened on. */
    public static String dosSystem(final String view) {
        final String rest = view.substring(DOS.length());
        final int colon = rest.indexOf(':');
        return colon < 0 ? rest : rest.substring(0, colon);
    }

    /** What was typed after HELP. */
    public static String dosTopic(final String view) {
        final String rest = view.substring(DOS.length());
        final int colon = rest.indexOf(':');
        return colon < 0 ? "" : rest.substring(colon + 1);
    }

    /** info opened on what was typed after it. */
    public static String info(final String topic) {
        return INFO + (topic == null ? "" : topic);
    }

    /** Whether a name is info's. */
    public static boolean namesInfo(final String view) {
        return view != null && view.startsWith(INFO);
    }

    /** What was typed after info. */
    public static String infoTopic(final String view) {
        return view.substring(INFO.length());
    }

    /** man reading an entry of a manual, by the entry's id. */
    public static String manual(final String entry) {
        return MANUAL + entry;
    }

    /** Whether a name is an entry man is reading. */
    public static boolean namesManual(final String view) {
        return view != null && view.startsWith(MANUAL);
    }

    /** The entry's id. */
    public static String manualEntry(final String view) {
        return view.substring(MANUAL.length());
    }
}
