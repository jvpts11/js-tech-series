/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.layout;

/**
 * Where what tools show of themselves is drawn, in the GUI's pixels: the ring of an item's modes, held open by the
 * mode key, its segments one to a mode and the first at the top; the two short bars under the crosshair, the charge
 * and the heat; and the bars and the mode's letter on an item's icon. Pure, so the screen, the HUD and the tests read
 * the same numbers.
 */
public final class ToolHudLayout {

    /** The ring's inner and outer radius, and how far above the screen's middle its centre is. */
    public static final int INNER = 30;
    public static final int OUTER = 82;
    public static final int RAISED = 6;
    /** The gap between two segments, in degrees, half on each side. */
    public static final double GAP_DEGREES = 3.0;
    /** The side of the item drawn in the ring's middle, twice the game's. */
    public static final int ITEM = 32;
    /** How far below the ring the item's name and the hint are, from the ring's centre. */
    public static final int NAME_BELOW = OUTER + 6;
    public static final int HINT_BELOW = OUTER + 17;
    /** A mode's picture, above its name, and its name, from where the mode sits on the ring. */
    public static final int GLYPH_WIDTH = 12;
    public static final int GLYPH_HEIGHT = 10;
    public static final int GLYPH_ABOVE = 10;
    public static final int LABEL_BELOW = 4;
    /** The dot under the current mode's name, from where it sits on the ring, and its side. */
    public static final int DOT_BELOW = 12;
    public static final int DOT = 3;

    /** The bars under the crosshair: their left from the middle, their width, height and rows below the middle. */
    public static final int BAR_LEFT = -8;
    public static final int BAR_WIDTH = 17;
    public static final int BAR_HEIGHT = 2;
    public static final int CHARGE_ROW = 8;
    public static final int HEAT_ROW = 14;
    /** The mark at the end of the heat, a pixel wide and four tall. */
    public static final int MARK_HEIGHT = 4;

    /** The bars on an item's icon: their left, width and the row of the lowest, and how far apart they stack. */
    public static final int ICON_BAR_LEFT = 2;
    public static final int ICON_BAR_WIDTH = 13;
    public static final int ICON_BAR_ROW = 13;
    public static final int ICON_BAR_STEP = 2;
    /** The square behind the mode's letter in the icon's top left corner. */
    public static final int LETTER_BOX = 6;
    /** The most bars an icon stacks, its own durability among them, so they never cover its top half. */
    public static final int MOST_ICON_BARS = 4;

    private static final double FULL_TURN = 360.0;

    private ToolHudLayout() {
    }

    /** How wide each segment of a ring of {@code count} modes is, in degrees. */
    public static double span(final int count) {
        return FULL_TURN / Math.max(1, count);
    }

    /**
     * The angle the middle of mode {@code index}'s segment points at, in degrees clockwise from straight up: the
     * first at the top.
     */
    public static double middle(final int index, final int count) {
        return index * span(count);
    }

    /**
     * Which mode the pointer at ({@code dx}, {@code dy}) from the ring's centre points at, or -1 while it is still
     * within the inner circle: past it, the direction alone chooses, however far out.
     */
    public static int pointedAt(final double dx, final double dy, final int count) {
        if (count <= 0 || dx * dx + dy * dy < (double) INNER * INNER) {
            return -1;
        }
        return segmentOf(angle(dx, dy), count);
    }

    /**
     * Which segment the pixel at ({@code dx}, {@code dy}) from the centre is in, or -1 for one outside the ring or in
     * a gap between two segments.
     */
    public static int segmentAt(final double dx, final double dy, final int count) {
        final double distance = dx * dx + dy * dy;
        if (count <= 0 || distance < (double) INNER * INNER || distance > (double) OUTER * OUTER) {
            return -1;
        }
        final double angle = angle(dx, dy);
        final int segment = segmentOf(angle, count);
        double off = Math.abs(angle - middle(segment, count)) % FULL_TURN;
        off = Math.min(off, FULL_TURN - off);
        return off > span(count) / 2.0 - GAP_DEGREES / 2.0 ? -1 : segment;
    }

    /**
     * Whether the pixel at ({@code dx}, {@code dy}) lies on the edge of the segment it is in: inside it, with one of
     * its four neighbours outside it.
     */
    public static boolean onEdge(final int dx, final int dy, final int count) {
        final int segment = segmentAt(dx, dy, count);
        return segment >= 0 && (segmentAt(dx + 1, dy, count) != segment || segmentAt(dx - 1, dy, count) != segment
                || segmentAt(dx, dy + 1, count) != segment || segmentAt(dx, dy - 1, count) != segment);
    }

    /** The x, from the centre, of where mode {@code index} sits on the ring: halfway between the circles. */
    public static int placeX(final int index, final int count) {
        return (int) Math.round(Math.sin(Math.toRadians(middle(index, count))) * (INNER + OUTER) / 2.0);
    }

    /** The y, from the centre, of where mode {@code index} sits on the ring. */
    public static int placeY(final int index, final int count) {
        return (int) Math.round(-Math.cos(Math.toRadians(middle(index, count))) * (INNER + OUTER) / 2.0);
    }

    /** How many pixels of an icon's bar {@code fraction} of whole fills: at least one for anything above nothing. */
    public static int iconFill(final double fraction) {
        if (!(fraction > 0)) {
            return 0;
        }
        return Math.max(1, Math.min(ICON_BAR_WIDTH, (int) Math.round(ICON_BAR_WIDTH * fraction)));
    }

    /** How many pixels of a bar under the crosshair {@code fraction} of whole fills. */
    public static int barFill(final double fraction) {
        if (!(fraction > 0)) {
            return 0;
        }
        return Math.min(BAR_WIDTH, (int) Math.round(BAR_WIDTH * fraction));
    }

    /** The row of an icon's bar {@code index}, the lowest 0, from the icon's top. */
    public static int iconBarRow(final int index) {
        return ICON_BAR_ROW - index * ICON_BAR_STEP;
    }

    /* Degrees clockwise from straight up of the direction ({@code dx}, {@code dy}), y down as on a screen. */
    private static double angle(final double dx, final double dy) {
        double angle = Math.toDegrees(Math.atan2(dx, -dy));
        if (angle < 0) {
            angle += FULL_TURN;
        }
        return angle;
    }

    /* The segment whose middle is nearest {@code angle}: the first spans half a segment either side of the top. */
    private static int segmentOf(final double angle, final int count) {
        final double span = span(count);
        return (int) Math.floor(((angle + span / 2.0) % FULL_TURN) / span) % count;
    }
}
