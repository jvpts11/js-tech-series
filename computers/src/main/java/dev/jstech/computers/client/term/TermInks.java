/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.term;

import dev.jstech.computers.program.cli.CliStyle;

/**
 * The inks a terminal writes each style of line in, on the ground they are made for.
 *
 * @param ground    what the inks are written on
 * @param plain     plain output
 * @param prompt    the echoed command line
 * @param accent    system messages and headings
 * @param ok        success
 * @param error     errors
 * @param warn      warnings
 * @param info      informational output
 * @param dim       hints and secondary output
 * @param orange    the extended colours a program can paint with, the way a real terminal offers its set:
 *                  screenfetch draws each distribution's logo in them
 * @param magenta   the same, magenta
 * @param blue      the same, blue
 * @param cyan      the same, cyan
 * @param purple    the same, purple
 * @param red       the same, a brand red, which is not the red an error is written in
 * @param green     the same, green
 * @param yellow    the same, yellow
 * @param bright    the terminal's bold: what a tool wants read first
 * @param selection the wash laid under picked-out text
 */
public record TermInks(int ground, int plain, int prompt, int accent, int ok, int error, int warn, int info, int dim,
                       int orange, int magenta, int blue, int cyan, int purple, int red, int green, int yellow,
                       int bright, int selection) {

    /** The ink a line of that style is written in. */
    public int of(final CliStyle style) {
        return switch (style) {
            case PLAIN -> this.plain;
            case PROMPT -> this.prompt;
            case ACCENT, HEADER -> this.accent;
            case OK -> this.ok;
            case ERROR -> this.error;
            case WARN -> this.warn;
            case INFO -> this.info;
            case DIM -> this.dim;
            case ORANGE -> this.orange;
            case MAGENTA -> this.magenta;
            case BLUE -> this.blue;
            case CYAN -> this.cyan;
            case PURPLE -> this.purple;
            case RED -> this.red;
            case GREEN -> this.green;
            case YELLOW -> this.yellow;
            case BRIGHT -> this.bright;
        };
    }
}
