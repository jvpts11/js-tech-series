/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import org.joml.Quaternionf;
import org.joml.Vector3fc;

/**
 * Turns as Bedrock and Blockbench write them, in degrees about X, Y and Z, made into quaternions: a point is turned
 * about X first, then about Y, then about Z.
 */
public final class Rotations {

    private Rotations() {
    }

    /** The turn of {@code degrees} about X, Y and Z, X first. */
    public static Quaternionf ofEuler(final Vector3fc degrees) {
        return ofEuler(degrees.x(), degrees.y(), degrees.z(), new Quaternionf());
    }

    /** The turn of those degrees about X, Y and Z, X first, written into {@code into}. */
    public static Quaternionf ofEuler(final float x, final float y, final float z, final Quaternionf into) {
        // Turning about X first puts it innermost in the product.
        return into.identity().rotateZ((float) Math.toRadians(z)).rotateY((float) Math.toRadians(y))
                .rotateX((float) Math.toRadians(x));
    }
}
