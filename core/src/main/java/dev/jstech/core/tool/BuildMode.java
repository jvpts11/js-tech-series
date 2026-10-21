/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.tool;

/**
 * What a building tool does with the face of a block it is used on: lays copies of the block out from it along a line
 * or over the plane of that face, as the Construction Wand does, or copies a region and pastes it, as Building
 * Gadgets does. The blocks placed come from the player's inventory, or from any other supply a mod adds
 * ({@link BuildSupplies}).
 */
public sealed interface BuildMode {

    /** Copies along the line of blocks like it, up to {@code most}. */
    static BuildMode line(final int most) {
        return new Line(most);
    }

    /** Copies over the plane of blocks like it, up to {@code most}. */
    static BuildMode plane(final int most) {
        return new Plane(most);
    }

    /** Marks two corners, one use each, and copies the region between them into the tool. */
    static BuildMode copy() {
        return Copy.INSTANCE;
    }

    /** Pastes the region the tool holds in front of the face it is used on. */
    static BuildMode paste() {
        return Paste.INSTANCE;
    }

    /**
     * Out from the face used on, a copy of each block like it in a line through it: across the face for a side, along
     * where the player looks for a top or a bottom.
     *
     * @param most the most blocks it places in one use
     */
    record Line(int most) implements BuildMode {

        public Line {
            if (most < 1) {
                throw new IllegalArgumentException("a line places one block or more, not " + most);
            }
        }
    }

    /**
     * Out from the face used on, a copy of each block like it touching it over that face's plane, and of those
     * touching them, nearest first.
     *
     * @param most the most blocks it places in one use
     */
    record Plane(int most) implements BuildMode {

        public Plane {
            if (most < 1) {
                throw new IllegalArgumentException("a plane places one block or more, not " + most);
            }
        }
    }

    /** Copies the region between two corners marked. */
    enum Copy implements BuildMode {
        INSTANCE
    }

    /** Pastes the region held. */
    enum Paste implements BuildMode {
        INSTANCE
    }
}
