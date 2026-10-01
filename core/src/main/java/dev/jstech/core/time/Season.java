/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.time;

import dev.jstech.core.id.IStableName;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** A season of the world's year, in the order the year goes through them. */
@TextHolder
public enum Season implements IStableName {

    SPRING("spring", TextKey.of("jscore.season.spring", "Spring")),
    SUMMER("summer", TextKey.of("jscore.season.summer", "Summer")),
    AUTUMN("autumn", TextKey.of("jscore.season.autumn", "Autumn")),
    WINTER("winter", TextKey.of("jscore.season.winter", "Winter"));

    private final String serializedName;
    private final TextKey name;

    /* The seasons in the order a year goes through them, the first at its start. */
    private static final Season[] YEAR = {SPRING, SUMMER, AUTUMN, WINTER};

    Season(final String serializedName, final TextKey name) {
        this.serializedName = serializedName;
        this.name = name;
    }

    /** The season {@code index} seasons into a year, round and round. */
    public static Season at(final long index) {
        return YEAR[(int) Math.floorMod(index, YEAR.length)];
    }

    /** How many seasons a year has. */
    public static int perYear() {
        return YEAR.length;
    }

    @Override
    public String serializedName() {
        return this.serializedName;
    }

    /** What a player calls it. */
    public TextKey text() {
        return this.name;
    }
}
