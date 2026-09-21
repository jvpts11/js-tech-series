/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

/**
 * The workspaces a window occupies, as one number with a bit for each: a window may be on one of them, on
 * several, or on all, and which ones is all there is to say about it.
 *
 * <p>A set is never empty. A window on no workspace could never be seen again, so every way of making a set
 * that would leave it empty lands on the first workspace instead, and ticking off the last one a window is on
 * is refused rather than obeyed.
 */
public final class WorkspaceSet {

    /** How many workspaces a desktop has, which is what the one desktop that has them fixed it at. */
    public static final int COUNT = 4;

    /** Every workspace at once, which is what occupying all of them means. */
    public static final int EVERY = (1 << COUNT) - 1;

    private WorkspaceSet() {
    }

    /** A workspace number brought inside what a desktop has, so a bad one read back cannot hide a window. */
    public static int clampIndex(final int index) {
        return Math.max(0, Math.min(COUNT - 1, index));
    }

    /** The set that holds that one workspace, counted from nought. */
    public static int only(final int index) {
        return 1 << clampIndex(index);
    }

    /** A set as it may be kept: workspaces that do not exist dropped, and the first one when none is left. */
    public static int normalised(final int set) {
        final int known = set & EVERY;
        return known == 0 ? only(0) : known;
    }

    /** Whether the set holds that workspace. */
    public static boolean holds(final int set, final int index) {
        return index >= 0 && index < COUNT && (set >> index & 1) != 0;
    }

    /** The lowest workspace of the set, which is the one to show when the window has to be found. */
    public static int first(final int set) {
        return Integer.numberOfTrailingZeros(normalised(set));
    }

    /** The set with that workspace ticked the other way, or the set as it was when that would empty it. */
    public static int toggled(final int set, final int index) {
        if (index < 0 || index >= COUNT) {
            return normalised(set);
        }
        final int flipped = normalised(set) ^ 1 << index;
        return flipped == 0 ? normalised(set) : flipped;
    }
}
