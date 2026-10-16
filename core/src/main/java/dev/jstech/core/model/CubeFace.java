/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import dev.jstech.core.id.IStableName;
import org.jetbrains.annotations.Nullable;

/** A face of a model's cube, by the way it faces in the model's own space, Y up and north toward -Z. */
public enum CubeFace implements IStableName {

    NORTH("north", 0, 0, -1),
    SOUTH("south", 0, 0, 1),
    EAST("east", 1, 0, 0),
    WEST("west", -1, 0, 0),
    UP("up", 0, 1, 0),
    DOWN("down", 0, -1, 0);

    private final String serializedName;
    private final int stepX;
    private final int stepY;
    private final int stepZ;

    CubeFace(final String serializedName, final int stepX, final int stepY, final int stepZ) {
        this.serializedName = serializedName;
        this.stepX = stepX;
        this.stepY = stepY;
        this.stepZ = stepZ;
    }

    /** The face named {@code name} in a model file, or none. */
    public static @Nullable CubeFace byName(final String name) {
        for (final CubeFace face : values()) {
            if (face.serializedName.equals(name)) {
                return face;
            }
        }
        return null;
    }

    @Override
    public String serializedName() {
        return this.serializedName;
    }

    public int stepX() {
        return this.stepX;
    }

    public int stepY() {
        return this.stepY;
    }

    public int stepZ() {
        return this.stepZ;
    }
}
