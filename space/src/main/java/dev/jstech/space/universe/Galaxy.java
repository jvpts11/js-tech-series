/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.universe;

import com.mojang.serialization.Codec;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.text.TextKey;

/**
 * The three galaxies of the universe: the Milky Way, whose systems are real stars at their real distances, and
 * Andromeda and Triangulum, whose systems are the series' own.
 */
public enum Galaxy implements IStableName {

    MILKY_WAY("milky_way"),
    ANDROMEDA("andromeda"),
    TRIANGULUM("triangulum");

    private final String serializedName;

    /** As a star system's file names its galaxy. */
    public static final Codec<Galaxy> CODEC = StableCodecs.byName(Galaxy.class);

    Galaxy(final String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String serializedName() {
        return this.serializedName;
    }

    /** Its name as a player reads it. */
    public TextKey title() {
        return switch (this) {
            case MILKY_WAY -> UniverseTexts.MILKY_WAY;
            case ANDROMEDA -> UniverseTexts.ANDROMEDA;
            case TRIANGULUM -> UniverseTexts.TRIANGULUM;
        };
    }
}
