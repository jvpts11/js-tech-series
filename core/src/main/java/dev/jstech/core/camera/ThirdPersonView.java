/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.camera;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

/**
 * Where a camera in the third person stands: this far back from what it turns round, moved this far to the right of
 * the view and up, over a shoulder, and turning round a place of its own instead of the eye, the middle of a long
 * vehicle; a change of view is eased over this long. The game's own stands 4 blocks back from the eye. Pure.
 *
 * @param distance how far back it stands, in blocks
 * @param right    how far to the right of the view, in blocks, to the left below 0
 * @param up       how far up, in blocks, down below 0
 * @param pivot    the place in the world it turns round, or the eye of whoever it follows
 * @param seconds  how long a change to or from it is eased over, none at 0
 */
public record ThirdPersonView(double distance, double right, double up, @Nullable Vector3d pivot, double seconds) {

    /** How far back the game's own camera stands from the eye, in blocks. */
    public static final double GAME_DISTANCE = 4.0;
    /** How long a change of view is eased over unless it says, in seconds. */
    public static final double EASED = 0.3;

    public ThirdPersonView {
        if (!(distance > 0) || !Double.isFinite(distance) || !Double.isFinite(right) || !Double.isFinite(up)
                || !(seconds >= 0) || !Double.isFinite(seconds) || pivot != null && !pivot.isFinite()) {
            throw new IllegalArgumentException("a view in the third person stands back further than 0, at a place "
                    + "and offsets that are numbers, eased over 0 seconds or more: " + distance + ", " + right + ", "
                    + up + ", " + pivot + ", " + seconds);
        }
        pivot = pivot == null ? null : new Vector3d(pivot);
    }

    /** A view {@code blocks} back from the eye, eased to and from as a change of view is unless it says. */
    public static ThirdPersonView behind(final double blocks) {
        return new ThirdPersonView(blocks, 0, 0, null, EASED);
    }

    /** It, moved {@code rightBlocks} to the right of the view and {@code upBlocks} up: over a shoulder. */
    public ThirdPersonView shoulder(final double rightBlocks, final double upBlocks) {
        return new ThirdPersonView(this.distance, rightBlocks, upBlocks, this.pivot, this.seconds);
    }

    /** It, turning round {@code at} instead of the eye. */
    public ThirdPersonView around(final Vector3d at) {
        return new ThirdPersonView(this.distance, this.right, this.up, at, this.seconds);
    }

    /** It, eased to and from over {@code over} seconds, at once at 0. */
    public ThirdPersonView easedOver(final double over) {
        return new ThirdPersonView(this.distance, this.right, this.up, this.pivot, over);
    }
}
