/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.flex;

/**
 * One column of a grid: as wide as it says, as wide as what it holds, or a share of the room the others leave.
 *
 * @param kind  which of the three
 * @param value the width in pixels for a fixed column, the share for a fraction, unused for one sized by what it
 *              holds
 */
public record Track(Kind kind, int value) {

    public Track {
        if (value < 0) {
            throw new IllegalArgumentException("a grid column cannot be " + value + " wide");
        }
    }

    /** A column {@code pixels} wide. */
    public static Track fixed(final int pixels) {
        return new Track(Kind.FIXED, pixels);
    }

    /** A column as wide as the widest thing it holds. */
    public static Track auto() {
        return new Track(Kind.AUTO, 0);
    }

    /** A column taking {@code share} parts of the room the fixed and the sized columns leave. */
    public static Track fraction(final int share) {
        return new Track(Kind.FRACTION, Math.max(1, share));
    }

    /** How a column's width is decided. */
    public enum Kind {
        /** Its width is given. */
        FIXED,
        /** As wide as the widest thing it holds. */
        AUTO,
        /** A share of the room left. */
        FRACTION
    }
}
