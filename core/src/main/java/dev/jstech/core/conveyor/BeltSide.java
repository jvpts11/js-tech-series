/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.conveyor;

import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;

/** A side of a belt, as one looks the way it moves. */
public enum BeltSide implements StringRepresentable {

    LEFT("left"),
    RIGHT("right");

    private final String id;

    BeltSide(final String id) {
        this.id = id;
    }

    /** The way this side of a belt moving {@code facing} lies. */
    public Direction of(final Direction facing) {
        return this == LEFT ? facing.getCounterClockWise() : facing.getClockWise();
    }

    /** The other side. */
    public BeltSide other() {
        return this == LEFT ? RIGHT : LEFT;
    }

    @Override
    public String getSerializedName() {
        return this.id;
    }
}
