/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.model;

import net.minecraft.core.Direction;
import org.joml.Quaternionf;

/** How a part, made facing north, is turned about the block's middle to stand on another face. */
public final class FaceTurn {

    private static final float HALF_TURN = (float) Math.PI;
    private static final float QUARTER_TURN = (float) Math.PI / 2F;

    private FaceTurn() {
    }

    /**
     * The turn that takes a part made facing north, turned {@code turns} quarter turns about its face as its boxes
     * are turned, to {@code face}.
     */
    public static Quaternionf of(final Direction face, final int turns) {
        return of(face).rotateZ(QUARTER_TURN * turns);
    }

    /** The turn that takes a part made facing north to {@code face}. */
    public static Quaternionf of(final Direction face) {
        return switch (face) {
            case SOUTH -> new Quaternionf().rotationY(HALF_TURN);
            case WEST -> new Quaternionf().rotationY(QUARTER_TURN);
            case EAST -> new Quaternionf().rotationY(-QUARTER_TURN);
            case UP -> new Quaternionf().rotationX(QUARTER_TURN);
            case DOWN -> new Quaternionf().rotationX(-QUARTER_TURN);
            case NORTH -> new Quaternionf();
        };
    }
}
