/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * How a line of a checklist or a step of a card is written in a manual's source, in Markdown and in code alike: an
 * optional box first, {@code [ ]}, or {@code [!]} for a line the manual's last revision changed; then a checklist's
 * line is what to look at, three dots or more and what it should be ({@code Rocket ... on pad}), and a card's step its
 * name, a colon and what it is ({@code Ignition: engines lit}). Either may have its first part alone. Pure.
 */
public final class ChecklistLines {

    /* The box in front of a line: blank, or marked as changed. */
    private static final Pattern BOX = Pattern.compile("^\\[([ !])]\\s*");
    /* Three dots or more between a checklist line's two parts. */
    private static final Pattern LEADER = Pattern.compile("\\s*\\.{3,}\\s*");
    private static final String COLON = ":";

    private ChecklistLines() {
    }

    /** A line of a checklist as written: what to look at, what it should be (empty for none), whether it changed. */
    public static Line check(final String written) {
        final Boxed boxed = Boxed.of(written);
        final Matcher leader = LEADER.matcher(boxed.rest());
        if (!leader.find()) {
            return new Line(boxed.rest().strip(), "", boxed.changed());
        }
        return new Line(boxed.rest().substring(0, leader.start()).strip(), boxed.rest().substring(leader.end())
                .strip(), boxed.changed());
    }

    /** A step of a card as written: its name, what it is (empty for none), whether it changed. */
    public static Line step(final String written) {
        final Boxed boxed = Boxed.of(written);
        final int colon = boxed.rest().indexOf(COLON);
        if (colon < 0) {
            return new Line(boxed.rest().strip(), "", boxed.changed());
        }
        return new Line(boxed.rest().substring(0, colon).strip(), boxed.rest().substring(colon + 1).strip(),
                boxed.changed());
    }

    /** Whether a list item starts with a checklist's box, which makes its list a checklist. */
    public static boolean boxed(final String item) {
        return BOX.matcher(item.strip()).find();
    }

    /**
     * A line of a checklist or a step of a card.
     *
     * @param first   what to look at, or the step's name
     * @param second  what it should be, or what the step is; empty for none
     * @param changed whether the manual's last revision changed it
     */
    public record Line(String first, String second, boolean changed) {
    }

    /* A line with its box taken off, and whether the box marked it as changed. */
    private record Boxed(String rest, boolean changed) {

        static Boxed of(final String written) {
            final String line = written.strip();
            final Matcher box = BOX.matcher(line);
            return box.find() ? new Boxed(line.substring(box.end()), "!".equals(box.group(1)))
                    : new Boxed(line, false);
        }
    }
}
