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
import java.util.Objects;

/**
 * A kind of probe, as its mission module makes it: its name, and whether it goes down onto the world it is sent to, as
 * a lander, a rover or a sample return does, rather than passing it, orbiting it or going on. Declared in code
 * ({@link ProbeTypes#declare}) or by a data pack's file:
 *
 * <pre>{@code
 * {"name": "jsspace.probe.lander", "lands": true}
 * }</pre>
 *
 * @param name  the translation key of its name
 * @param lands whether it goes down onto its world
 */
public record ProbeType(String name, boolean lands) {

    public static final Codec<ProbeType> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(ProbeType::name),
            Codec.BOOL.optionalFieldOf("lands", false).forGetter(ProbeType::lands)
    ).apply(instance, ProbeType::new));

    public ProbeType {
        Objects.requireNonNull(name, "name");
    }
}
