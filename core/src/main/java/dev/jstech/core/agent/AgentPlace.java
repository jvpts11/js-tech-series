/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.agent;

import java.util.Objects;
import net.minecraft.world.phys.Vec3;

/**
 * A place an agent is told of ({@link AgentPlaces}), in the dimension the player is in.
 *
 * @param kind what kind of place it is, in a word or two joined by underscores: {@code respawn}, {@code low_orbit}
 * @param name which one it is, as a world's or a machine's name; empty where its kind says it all
 * @param at   where it is
 */
public record AgentPlace(String kind, String name, Vec3 at) {

    public AgentPlace {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(at, "at");
        if (kind.isBlank()) {
            throw new IllegalArgumentException("a place has a kind");
        }
    }
}
