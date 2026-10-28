/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui;

/**
 * A window onto a picture longer than it, sliding slower than the content dragged across it, as a far sky slides behind
 * near things: where in the picture the window stands. The window shows the picture's middle while the content stands
 * where it started, moves through the picture the way the content is dragged at a fraction of its speed, and never
 * leaves the picture, however far the content goes. Pure.
 */
public final class ParallaxWindow {

    /** How far the picture slides for each pixel the content is dragged: half, so it reads as far behind. */
    public static final double PARALLAX = 0.5;

    private ParallaxWindow() {
    }

    /**
     * Where a window {@code window} pixels long starts in a picture {@code picture} pixels long, when the content
     * stands at {@code scroll} having started at {@code start}; zero when the picture is no longer than the window.
     */
    public static int offset(final double scroll, final double start, final int picture, final int window) {
        final int room = picture - window;
        if (room <= 0) {
            return 0;
        }
        // Content dragged one way shows more of the picture's other side, so the window moves against the drag.
        final double centred = room / 2.0 - (scroll - start) * PARALLAX;
        return (int) Math.floor(Math.max(0, Math.min(room, centred)));
    }
}
