/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.crew;

import dev.jstech.core.id.IStableId;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What a player may do aboard a starship, each role holding the ones under it: a passenger comes aboard, sits and
 * sleeps; the crew works the ship's fittings and machines; a pilot flies it, from its helm, and may change its blocks.
 */
@TextHolder
public enum CrewRole implements IStableId, IStableName {
    /** Not let aboard. */
    NONE(0, "none", TextKey.of("jsspace.crew.role.none", "NONE")),
    /** Comes aboard, sits and sleeps. */
    PASSENGER(1, "passenger", TextKey.of("jsspace.crew.role.passenger", "PASS")),
    /** Works the ship's fittings and machines. */
    CREW(2, "crew", TextKey.of("jsspace.crew.role.crew", "CREW")),
    /** Flies the ship and may change its blocks. */
    PILOT(3, "pilot", TextKey.of("jsspace.crew.role.pilot", "PILOT"));

    private final int id;
    private final String serializedName;
    private final TextKey shortName;

    CrewRole(final int id, final String serializedName, final TextKey shortName) {
        this.id = id;
        this.serializedName = serializedName;
        this.shortName = shortName;
    }

    @Override
    public int id() {
        return id;
    }

    @Override
    public String serializedName() {
        return serializedName;
    }

    /** Its name as the ship's console shows it. */
    public TextKey shortName() {
        return shortName;
    }

    /** Whether it holds {@code needed}: it is that role or one over it. */
    public boolean holds(final CrewRole needed) {
        return this.id >= needed.id;
    }
}
