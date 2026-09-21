/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui;

/**
 * How big the glass of a monitor is, which is one size whatever the machine behind it is showing.
 *
 * <p>A monitor does not change size when the machine leaves its self-test for a prompt, or a prompt for a
 * desktop. Each of those screens used to pick a size of its own, so the glass jumped as a machine came up, and
 * a terminal was a smaller monitor than the desktop of the same machine. They all ask here.
 */
public final class MonitorGlass {

    /** The glass at its full size, which is what it is on any window with the room for it. */
    public static final int WIDTH = 384;
    public static final int HEIGHT = 256;

    /** What is kept clear beside the glass and above and below it, for the monitor's own shell. */
    private static final int BESIDE = 44;
    private static final int ABOVE_AND_BELOW = 60;

    private MonitorGlass() {
    }

    /** How wide the glass is in a window that wide: all of it, or what the window leaves. */
    public static int width(final int windowWidth) {
        return Math.min(windowWidth - BESIDE, WIDTH);
    }

    /** How tall the glass is in a window that tall. */
    public static int height(final int windowHeight) {
        return Math.min(windowHeight - ABOVE_AND_BELOW, HEIGHT);
    }
}
