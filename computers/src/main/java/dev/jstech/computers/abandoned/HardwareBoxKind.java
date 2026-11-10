/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.abandoned;

import net.minecraft.util.StringRepresentable;

/** What a hardware box is printed with, and so what it holds: a computer's parts, or a graphics card. */
public enum HardwareBoxKind implements StringRepresentable {
    COMPUTER("computer"),
    GRAPHICS_CARD("graphics_card");

    private final String name;

    HardwareBoxKind(final String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
