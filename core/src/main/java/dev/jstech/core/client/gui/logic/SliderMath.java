/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.logic;

/**
 * The arithmetic of a slider: where along its track a point of the pointer falls, and the steps a slider in steps
 * stops at. A slider's value is a fraction, 0 at the start and 1 at the end; one in {@code steps} stops only at
 * {@code steps} evenly spaced places, its ends among them.
 */
public final class SliderMath {

    private SliderMath() {
    }

    /**
     * The fraction the pointer at {@code mouseX} puts a slider at, its track at {@code x} and {@code width} wide, its
     * handle {@code handle} wide and centred on the pointer.
     */
    public static double fraction(final double mouseX, final int x, final int width, final int handle) {
        final double room = width - handle;
        if (room <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(1, (mouseX - x - handle / 2.0) / room));
    }

    /** {@code fraction} put on the nearest of {@code steps} places; any fraction when {@code steps} is under 2. */
    public static double snap(final double fraction, final int steps) {
        final double clamped = Math.max(0, Math.min(1, fraction));
        if (steps < 2) {
            return clamped;
        }
        return Math.round(clamped * (steps - 1)) / (double) (steps - 1);
    }

    /** The fraction one step from {@code fraction} ({@code direction} 1 or -1); a twentieth for a free slider. */
    public static double step(final double fraction, final int steps, final int direction) {
        final double size = steps < 2 ? 0.05 : 1.0 / (steps - 1);
        return snap(fraction + Integer.signum(direction) * size, steps);
    }

    /** Which of {@code steps} places {@code fraction} is at, counting from 0. */
    public static int index(final double fraction, final int steps) {
        return steps < 2 ? 0 : (int) Math.round(snap(fraction, steps) * (steps - 1));
    }
}
