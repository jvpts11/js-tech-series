/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.wireless;

import net.minecraft.util.StringRepresentable;

/**
 * Where an access point is fixed (computers/docs/design/wireless.md): the Legacy's and the Transition's stand on a desk
 * and take their cables from behind; the Standard's and the Advanced's hang from a ceiling or sit on a wall on their
 * mounting plate, and take their cables through the face they are fixed to.
 */
public enum AccessPointMount implements StringRepresentable {

    DESK("desk"),
    CEILING("ceiling"),
    WALL("wall");

    private final String serializedName;

    AccessPointMount(final String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
