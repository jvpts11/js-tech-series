/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

/**
 * A run of text in one colour, which is what a line at a terminal is made of.
 *
 * <p>Real tools colour parts of a line, not lines: the green arrows a package manager opens with, the blue
 * brackets round its {@code ok}, a flag in red beside one in blue. A line that can only be one colour cannot
 * say any of that, so a line is a list of these.
 */
public record CliSpan(String text, CliStyle style) {

    public CliSpan {
        if (text == null) {
            text = "";
        }
        if (style == null) {
            style = CliStyle.PLAIN;
        }
    }

    /** A run in the terminal's ordinary colour. */
    public static CliSpan plain(final String text) {
        return new CliSpan(text, CliStyle.PLAIN);
    }
}
