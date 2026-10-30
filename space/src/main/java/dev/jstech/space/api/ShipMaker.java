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
 * A maker of starship parts: it gives each of its parts its shape and, to a part one goes into, its interior in its own
 * style, the paint chosen apart. A ship mixes makers from module to module.
 *
 * @param name its name's key
 * @param mark the key of its mark, three letters a builder's filters and cards show; its name's for none
 */
public record ShipMaker(String name, String mark) {

    /** As a data pack's file writes one. */
    public static final Codec<ShipMaker> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(ShipMaker::name),
            Codec.STRING.optionalFieldOf("mark", "").forGetter(ShipMaker::mark)
    ).apply(instance, ShipMaker::new));

    public ShipMaker {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(mark, "mark");
        if (mark.isEmpty()) {
            mark = name;
        }
    }

    /** A maker known by its name alone. */
    public ShipMaker(final String name) {
        this(name, "");
    }
}
