/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.core.id.IStableId;
import dev.jstech.core.id.StableIds;

/**
 * The visual weight of a console line. The shell and commands tag every line with one of these; the client maps them to the terminal palette, so the pure-logic side never mentions a colour value. A line travels with its style's id.
 */
public enum CliStyle implements IStableId {
    PLAIN(0),
    PROMPT(1),
    OK(2),
    ERROR(3),
    WARN(4),
    INFO(5),
    DIM(6),
    ACCENT(7),
    HEADER(8),
    /*
     * The terminal's extended palette: brand-tinted colors a program can paint with, the way a real terminal
     * exposes its 16-color set. screenfetch keys each distribution's logo to one of these.
     */
    ORANGE(9),
    MAGENTA(10),
    BLUE(11),
    CYAN(12),
    PURPLE(13),
    /*
     * The terminal's bold: the same white, brighter. It is what a real tool uses for the words it wants read
     * first, a heading over a list of packages or the question it has stopped to ask.
     */
    BRIGHT(14),
    /* One more of the extended palette: a brand red, which is not the red an error is written in. */
    RED(15);

    private static final StableIds<CliStyle> IDS = StableIds.of(CliStyle.class);

    private final int id;

    CliStyle(final int id) {
        this.id = id;
    }

    @Override
    public int id() {
        return id;
    }

    /** The style that declares {@code id}; an id no style declares reads as {@link #PLAIN}. */
    public static CliStyle byId(final int id) {
        return IDS.byId(id, PLAIN);
    }
}
