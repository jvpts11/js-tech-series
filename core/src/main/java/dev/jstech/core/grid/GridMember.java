/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.grid;

import java.util.Objects;
import org.jetbrains.annotations.Nullable;

/**
 * What stands at one position of a grid: a cable of a line, in one generation of it, in a colour or in none, with how
 * much it carries and how far a run of it reaches; or a device that passes the grid through without being a cable (a
 * router, a junction), which joins whatever touches it and limits nothing.
 *
 * <p>Two cables join when they are of the same line and generation, and their colours agree: the same colour, or
 * either of them in none, so an uncoloured cable joins every colour and two coloured ones only their own. A device
 * joins every cable that touches it.
 *
 * <p>This holds nothing of the game: lines are named by their ids as text.
 *
 * @param line       the line's id, or null for a device that is no cable
 * @param generation the line's generation, from 0
 * @param colour     the colour's number, or {@link #NO_COLOUR}
 * @param throughput how much the cable carries, in the grid's own unit; a device limits nothing
 * @param range      how many cables a run of this line reaches before it has to be renewed; 0 for no limit
 */
public record GridMember(@Nullable String line, int generation, int colour, long throughput, int range) {

    /** The colour of a cable in none. */
    public static final int NO_COLOUR = -1;
    /** A device that passes the grid through: it joins every cable and limits nothing. */
    public static final GridMember DEVICE = new GridMember(null, 0, NO_COLOUR, Long.MAX_VALUE, 0);

    public GridMember {
        if (generation < 0) {
            throw new IllegalArgumentException("a line's generation counts from 0, not " + generation);
        }
        if (throughput < 0) {
            throw new IllegalArgumentException("a cable carries nothing below 0: " + throughput);
        }
        if (range < 0) {
            throw new IllegalArgumentException("a range is never below 0: " + range);
        }
    }

    /** A cable of {@code line} in no colour. */
    public static GridMember cable(final String line, final int generation, final long throughput,
                                   final int range) {
        return new GridMember(Objects.requireNonNull(line, "line"), generation, NO_COLOUR, throughput, range);
    }

    /** The same cable in colour {@code colour}. */
    public GridMember coloured(final int colour) {
        return new GridMember(this.line, this.generation, colour, this.throughput, this.range);
    }

    /** Whether this stands for a device rather than a cable. */
    public boolean isDevice() {
        return this.line == null;
    }

    /** Whether this and {@code other} join when they touch. */
    public boolean joins(final GridMember other) {
        if (isDevice() || other.isDevice()) {
            return true;
        }
        return this.line.equals(other.line) && this.generation == other.generation
                && (this.colour == NO_COLOUR || other.colour == NO_COLOUR || this.colour == other.colour);
    }

    /** Whether this and {@code other} are cables of one run: the same line, generation and colour. */
    public boolean sameRun(final GridMember other) {
        return !isDevice() && !other.isDevice() && this.line.equals(other.line)
                && this.generation == other.generation && this.colour == other.colour;
    }
}
