/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.api;

import com.mojang.serialization.Codec;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableCodecs;

/**
 * The sizes a satellite or a probe is built in: the CubeSat, small and cheap, which rides along with another payload;
 * the medium one; the large one.
 */
public enum SpacecraftSize implements IStableName {

    CUBESAT("cubesat"),
    MEDIUM("medium"),
    LARGE("large");

    public static final Codec<SpacecraftSize> CODEC = StableCodecs.byName(SpacecraftSize.class);

    private final String serializedName;

    SpacecraftSize(final String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String serializedName() {
        return this.serializedName;
    }
}
