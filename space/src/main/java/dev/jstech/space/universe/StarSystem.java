/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.universe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;

/**
 * A star system: its name, the galaxy it is in, how far it is from the Sun, and what shines at its heart. Its worlds
 * each name it as theirs, and it has a space of its own ({@link SystemSpaces}).
 *
 * @param name       the key of its name in the language files
 * @param galaxy     the galaxy it is in
 * @param lightYears how far it is from the Sun, in light-years; nothing for the Solar System
 * @param star       the key of what shines at its heart in the language files, or empty when nothing is said of it
 */
public record StarSystem(String name, Galaxy galaxy, double lightYears, String star) {

    /** As a data pack's file writes one: {@code {"name": "...", "galaxy": "milky_way", "light_years": 4.37}}. */
    public static final Codec<StarSystem> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(StarSystem::name),
            Galaxy.CODEC.fieldOf("galaxy").forGetter(StarSystem::galaxy),
            Codec.doubleRange(0.0, Double.MAX_VALUE).optionalFieldOf("light_years", 0.0)
                    .forGetter(StarSystem::lightYears),
            Codec.STRING.optionalFieldOf("star", "").forGetter(StarSystem::star)
    ).apply(instance, StarSystem::new));

    public StarSystem {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(galaxy, "galaxy");
        star = star == null ? "" : star;
        if (lightYears < 0) {
            throw new IllegalArgumentException("a star system lies at a distance from the Sun; got " + lightYears);
        }
    }
}
