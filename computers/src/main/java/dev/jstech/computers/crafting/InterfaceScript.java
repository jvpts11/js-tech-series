/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.crafting;

import java.util.List;

/**
 * The lines that set a Crafting Interface the way it is set now, written the two ways software writes them, as its
 * window shows them under Software: IQL statements and Sigma calls. Pure, so it is tested without the game, and the
 * IQL it writes reads back to the same settings.
 */
public final class InterfaceScript {

    private InterfaceScript() {
    }

    /**
     * The IQL statements: {@code SET INTERFACE 'Kiln A' EXCLUSIVE ON}, its most jobs, and {@code PAUSE} or
     * {@code RESUME}.
     */
    public static List<String> iql(final String name, final boolean exclusive, final int maxJobs,
                                   final boolean paused) {
        final String named = " INTERFACE " + quoted(name);
        return List.of("SET" + named + " EXCLUSIVE " + (exclusive ? "ON" : "OFF"),
                "SET" + named + " MAX JOBS " + (maxJobs <= 0 ? "AUTO" : String.valueOf(maxJobs)),
                (paused ? "PAUSE" : "RESUME") + named);
    }

    /**
     * The Sigma calls: {@code var i = craftInterface("Kiln A");}, its mode and most jobs, and {@code i.Pause();} or
     * {@code i.Resume();}.
     */
    public static List<String> sigma(final String name, final boolean exclusive, final int maxJobs,
                                     final boolean paused) {
        return List.of("var i = craftInterface(" + text(name) + ");",
                "i.Exclusive(" + exclusive + ").MaxJobs(" + Math.max(0, maxJobs) + ");",
                paused ? "i.Pause();" : "i.Resume();");
    }

    /** The IQL statement that routes an input through a router: {@code SET INTERFACE ... ROUTE ...}. */
    public static String route(final String name, final String pattern, final String input, final String router) {
        return "SET INTERFACE " + quoted(name) + " ROUTE " + quoted(pattern) + " INPUT " + input + " TO ROUTER "
                + quoted(router);
    }

    /* A name in IQL's single quotes, a quote in it doubled. */
    private static String quoted(final String name) {
        return "'" + name.replace("'", "''") + "'";
    }

    /* A string in Sigma's double quotes, a quote or a backslash in it escaped. */
    private static String text(final String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
