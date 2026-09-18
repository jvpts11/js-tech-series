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
 * The colour each style has on a terminal that can show colour.
 *
 * <p>One palette for both terminals, since they are two windows onto one console and a line that is green in
 * one had better be the same green in the other. A glass that cannot show colour, a one-colour tube, maps
 * what this answers onto the one colour it has.
 */
public final class TermPalette {

    private TermPalette() {
    }

    public static int colorOf(final CliStyle style) {
        return switch (style) {
            case PROMPT -> 0xFFCDD6E2;         // light gray-white for the echoed command line
            case ACCENT, HEADER -> 0xFF39D6C4; // cyan for system messages
            case OK -> 0xFF5FE07A;             // green for success
            case ERROR -> 0xFFEF6A5A;          // red for errors
            case WARN -> 0xFFF0B23A;           // amber for warnings
            case INFO -> 0xFF2AA7E0;           // blue for informational output
            case DIM -> 0xFF7D8A9C;            // dim gray for hints and secondary output
            // The extended palette: brand-tinted terminal colours (screenfetch logos and the like).
            case ORANGE -> 0xFFE95420;
            case MAGENTA -> 0xFFE0447C;
            case BLUE -> 0xFF5A8FD6;
            case CYAN -> 0xFF2FA6E8;
            case PURPLE -> 0xFF9E8FD6;
            case BRIGHT -> 0xFFFFFFFF;         // the terminal's bold: what a tool wants read first
            default -> 0xFFCDD6E2;             // plain = light gray
        };
    }
}
