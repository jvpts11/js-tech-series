/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.boot;

import dev.jstech.computers.os.Platform;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableNames;
import org.jetbrains.annotations.Nullable;

/**
 * The picture a system puts up while it comes up, and the one it puts up on its way down.
 *
 * <p>Which one a system wears, not how it is drawn: the drawing is the client's business, and this is the
 * answer a machine gives when asked what its screen should look like. A system that says nothing here shows
 * its name over a bar, which is what a system with no picture of its own has always shown and what every
 * system showed before each of them was given the one it really had.
 */
public enum BootSplash implements IStableName {

    /** A name in the middle of the glass over a bar: what a system with no picture of its own gets. */
    PLAIN("plain"),

    /** The sky: a logo over the horizon with the bar running along the foot of it. */
    FRAMES_95("frames_95"),

    /** Black, the logo above the middle, and three blocks running through a trough beneath it. */
    FRAMES_XP("frames_xp"),

    /** Near black, the maker's mark in the middle, and a ring of dots turning below it. */
    FRAMES_11("frames_11");

    private final String serializedName;

    BootSplash(final String serializedName) {
        this.serializedName = serializedName;
    }

    /**
     * The picture a system of that platform, at that place in its family, comes up behind.
     *
     * <p>Asked of the two things that decide it rather than of the system itself, so the answer is worked out
     * without the game being loaded and a distribution an addon brings gets the plain one without anybody
     * having to write its name down anywhere.
     *
     * @param platform    the family the system belongs to
     * @param familyRank  where it sits in that family, counting from one; zero for a family with no order
     */
    public static BootSplash of(@Nullable final Platform platform, final int familyRank) {
        if (platform != Platform.FRAMES) {
            return PLAIN;
        }
        return switch (familyRank) {
            case 1 -> FRAMES_95;
            case 2 -> FRAMES_XP;
            case 3 -> FRAMES_11;
            default -> PLAIN;
        };
    }

    /** The splash with that name, or the plain one for a name nothing answers to. */
    public static BootSplash byName(final String name) {
        final BootSplash found = StableNames.of(BootSplash.class).find(name);
        return found == null ? PLAIN : found;
    }

    @Override
    public String serializedName() {
        return this.serializedName;
    }
}
