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
 * The orbits a satellite may be put in, each giving its own coverage: the low orbit a strip that passes over, the polar
 * one the whole world bit by bit, the geostationary one a large fixed area, deep space whatever is there, the
 * Lagrange points or another world's orbit; and a world's radiation belt, where antimatter gathers.
 */
public enum OrbitKind implements IStableName {

    LOW("low"),
    POLAR("polar"),
    GEOSTATIONARY("geostationary"),
    DEEP_SPACE("deep_space"),
    RADIATION_BELT("radiation_belt");

    public static final Codec<OrbitKind> CODEC = StableCodecs.byName(OrbitKind.class);

    private final String serializedName;

    OrbitKind(final String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String serializedName() {
        return this.serializedName;
    }
}
