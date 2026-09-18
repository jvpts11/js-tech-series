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

    /**
     * How many steps a sequence may show.
     *
     * <p>More than any system here prints, and worked from the glass: a screen of this size holds this many
     * rows of the size these are drawn at, with the heading and the air around it already taken off.
     */
    public static final int MOST_LINES = 20;

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

    /**
     * One step: what the machine was doing, and what came of it, which may be nothing worth saying.
     *
     * <p>A step can also carry a mark at the head of its line, because that is where these systems put the
     * part a player reads first: the kernel stamps the time, the service manager writes whether the thing
     * started, and the earliest systems name the drive. Whether that mark says a thing went well is kept
     * apart from the mark itself, so the screen colours it without reading the words.
     *
     * @param label what the machine was doing
     * @param value what came of it, drawn in its own column, which may be nothing
     * @param mark  what stands at the head of the line, which may be nothing
     * @param good  whether the mark and the value report success, which is what makes them green
     */
    public record Line(String label, String value, String mark, boolean good) {

        public Line {
            label = label == null ? "" : label;
            value = value == null ? "" : value;
            mark = mark == null ? "" : mark;
        }

        /** A step that reports nothing beside it, the way a plain line of a starting system reads. */
        public static Line of(final String label) {
            return new Line(label, "", "", false);
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
            this.lines.add(new Line(label, value, "", false));
            return this;
        }

        /** A step with a mark at the head of its line, which is how a kernel and an init report theirs. */
        public Builder marked(final String mark, final String label, final boolean good) {
            this.lines.add(new Line(label, "", mark, good));
            return this;
        }

        /** The same, with what came of it in its own column at the other end. */
        public Builder marked(final String mark, final String label, final String value, final boolean good) {
            this.lines.add(new Line(label, value, mark, good));
            return this;
        }

        public BootSequence build() {
            return new BootSequence(this.title, this.subtitle, this.lines);
        }
    }
}
