/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.conveyor;

import net.minecraft.util.StringRepresentable;

/**
 * How a belt lies across its block: flat, rising a block to its front, or falling a block to its front from the top
 * of its back.
 */
public enum BeltSlope implements StringRepresentable {

    FLAT("flat"),
    RISING("rising"),
    FALLING("falling");

    private final String id;

    BeltSlope(final String id) {
        this.id = id;
    }

    /** The slope a wrench turns the belt to next: flat, rising, falling, and flat again. */
    public BeltSlope next() {
        return switch (this) {
            case FLAT -> RISING;
            case RISING -> FALLING;
            case FALLING -> FLAT;
        };
    }

    @Override
    public String getSerializedName() {
        return this.id;
    }
}
