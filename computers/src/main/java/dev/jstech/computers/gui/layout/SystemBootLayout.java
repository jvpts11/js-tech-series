/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import dev.jstech.computers.gui.MonitorGlass;
import dev.jstech.core.gui.layout.GuiLayout;

/**
 * Pure layout of the system-boot screen: a wall of text on the monitor's own glass, whose lines are the
 * machine's, not this class's. What is fixed is the glass, the margin the wall is written at, and the two
 * shapes a sequence with no lines of its own takes (a name and a subtitle centred over a bar that fills as
 * the machine comes up), and the step and scale of the wall's lines, which are those of every wall of text in
 * the mod (the screen checks the two agree when it loads). The rows a sequence actually lists are the machine's
 * content, not this screen's geometry, so they are not enumerated here.
 */
public final class SystemBootLayout {

    public static final int WIDTH = MonitorGlass.WIDTH;
    public static final int HEIGHT = MonitorGlass.HEIGHT;

    /** Where the wall of text begins, and how much air is left at the right edge. */
    public static final int MARGIN = 12;

    /** Where the title and the subtitle sit, and the air kept before the rows a sequence lists start. */
    public static final int WALL_TOP = 12;
    public static final int AFTER_HEADER_GAP = 4;

    /** The step from one line of the wall to the next, and the scale its lines are written at. */
    public static final int LINE_STEP = 8;
    public static final float TEXT_SCALE = 0.75f;

    /** Where a title and a subtitle sit when the sequence lists no lines of its own: centred over the bar. */
    public static final int TITLE_CENTER_DY = -22;
    public static final int SUBTITLE_CENTER_DY = -10;

    /** The bar a system that reports nothing fills while it comes up. */
    public static final int BAR_W = 140;
    public static final int BAR_H = 3;
    public static final int BAR_CENTER_DY = 12;

    /** The gap a clipped label is kept clear of the value column that follows it. */
    public static final int VALUE_GAP = 4;
    /** How far past the widest mark and label the value column starts. */
    public static final int LABEL_TO_VALUE_GAP = 10;

    private SystemBootLayout() {
    }

    /** Where the silent-system bar starts, centred on the glass. */
    public static int barX() {
        return (WIDTH - BAR_W) / 2;
    }

    public static int barY() {
        return HEIGHT / 2 + BAR_CENTER_DY;
    }

    /**
     * The glass as one box, the bar a silent system fills, and the two title/subtitle placements a sequence
     * takes (centred with no lines of its own, or at the wall's own margin when it lists some).
     */
    public static GuiLayout layout() {
        final GuiLayout l = new GuiLayout(WIDTH, HEIGHT);
        l.box("bar", barX(), barY(), BAR_W, BAR_H);
        // A title and a subtitle centred over the bar, at whatever length a machine's own name comes out to.
        l.text("centeredTitle", WIDTH / 2 - 60, HEIGHT / 2 + TITLE_CENTER_DY, 20, 1.0f);
        l.text("centeredSubtitle", WIDTH / 2 - 60, HEIGHT / 2 + SUBTITLE_CENTER_DY, 20, 1.0f);
        // The same pair, written at the wall's own margin when the sequence lists rows under them, one wall row down.
        l.text("wallTitle", MARGIN, WALL_TOP, 40, TEXT_SCALE);
        l.text("wallSubtitle", MARGIN, WALL_TOP + LINE_STEP, 40, TEXT_SCALE);
        return l;
    }
}
