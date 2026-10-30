/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;

/**
 * A kind of satellite, as its mission module makes it: its name and the orbits it does its work best in, none for one
 * that goes where it is needed. Declared in code ({@link SatelliteTypes#declare}) or by a data pack's file:
 *
 * <pre>{@code
 * {"name": "jsspace.satellite.mapping", "orbits": ["polar"]}
 * }</pre>
 *
 * @param name   the translation key of its name
 * @param orbits the orbits it works best in
 */
public record SatelliteType(String name, List<OrbitKind> orbits) {

    public static final Codec<SatelliteType> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(SatelliteType::name),
            OrbitKind.CODEC.listOf().optionalFieldOf("orbits", List.of()).forGetter(SatelliteType::orbits)
    ).apply(instance, SatelliteType::new));

    public SatelliteType {
        Objects.requireNonNull(name, "name");
        orbits = List.copyOf(orbits);
    }
}
