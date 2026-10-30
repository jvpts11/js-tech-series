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
 * A finish a starship is painted in, chosen apart from its parts: every part of a ship is drawn in it. A part's formed
 * model in a finish is painted by the texture {@code textures/models/ship/<model>_<finish>.png} of the finish's
 * namespace, the model's own geometry the same in every finish.
 *
 * @param name its name's key
 */
public record ShipFinish(String name) {

    /** As a data pack's file writes one. */
    public static final Codec<ShipFinish> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(ShipFinish::name)
    ).apply(instance, ShipFinish::new));

    public ShipFinish {
        Objects.requireNonNull(name, "name");
    }
}
