/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.boot;

import java.util.ArrayList;
import java.util.List;

/**
 * What a system shows while it comes up: a heading, a line under it, and the steps it works through.
 *
 * <p>Every step is something the machine really did, so the sequence is built where the machine is and this only
 * carries it. A step is a label and what came of it, since that is how a machine of any age has reported its own
 * start: the thing it is doing on the left, what it found on the right.
 *
 * <p>The steps share the time evenly, so a sequence appears line by line over however long the system takes on
 * this machine rather than over a length of its own.
 */
public record BootSequence(String title, String subtitle, List<Line> lines) {

    /** How many steps a sequence may show, which is more than any system here has. */
    public static final int MOST_LINES = 16;

    /** Nothing to show: a system with no sequence of its own yet. */
    public static final BootSequence NONE = new BootSequence("", "", List.of());

    public BootSequence {
        title = title == null ? "" : title;
        subtitle = subtitle == null ? "" : subtitle;
        lines = lines == null ? List.of() : List.copyOf(lines.size() > MOST_LINES
                ? lines.subList(0, MOST_LINES) : lines);
    }

    /** Whether there is anything here to draw. */
    public boolean isEmpty() {
        return this.title.isEmpty() && this.lines.isEmpty();
    }

    /**
     * How many of the steps have been reached by then.
     *
     * <p>A step is shown once it is done, so the first appears after the first share of the time has gone and the
     * last as the system finishes coming up.
     */
    public int shownAt(final int ticksDone, final int ticksTotal) {
        if (this.lines.isEmpty() || ticksTotal <= 0) {
            return 0;
        }
        final int shown = (int) ((long) Math.max(0, ticksDone) * this.lines.size() / ticksTotal);
        return Math.min(this.lines.size(), shown);
    }

    /** One step: what the machine was doing, and what came of it, which may be nothing worth saying. */
    public record Line(String label, String value) {

        public Line {
            label = label == null ? "" : label;
            value = value == null ? "" : value;
        }

        /** A step that reports nothing beside it, the way a plain line of a starting system reads. */
        public static Line of(final String label) {
            return new Line(label, "");
        }
    }

    /** Builds one up a step at a time, since every system finds out what it has to say as it looks. */
    public static final class Builder {

        private final List<Line> lines = new ArrayList<>();
        private String title = "";
        private String subtitle = "";

        public Builder title(final String value) {
            this.title = value;
            return this;
        }

        public Builder subtitle(final String value) {
            this.subtitle = value;
            return this;
        }

        public Builder line(final String label) {
            this.lines.add(Line.of(label));
            return this;
        }

        public Builder line(final String label, final String value) {
            this.lines.add(new Line(label, value));
            return this;
        }

        public BootSequence build() {
            return new BootSequence(this.title, this.subtitle, this.lines);
        }
    }
}
