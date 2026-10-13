/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

/**
 * A run of a line as it reads in one language: the words, and the colour they are drawn in.
 *
 * <p>A {@link CliSpan} is what a command wrote, still to be put in a language; this is what is left once it has been,
 * which is what a terminal lays out cell by cell and draws.
 */
public record CliRun(String text, CliStyle style) {

    public CliRun {
        if (text == null) {
            text = "";
        }
        if (style == null) {
            style = CliStyle.PLAIN;
        }
    }

    /** A run in the terminal's ordinary colour. */
    public static CliRun plain(final String text) {
        return new CliRun(text, CliStyle.PLAIN);
    }
}
