/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.damage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.multiblock.shape.FormedPlace;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * A strike as a block of a formed model it reached keeps it: which model, and which block of it, the strike is
 * laid on, a block taking its shape from more than one model telling them apart.
 *
 * @param model the formed model struck
 * @param x     the block's x among the model's
 * @param y     its y
 * @param z     its z
 * @param mark  the strike
 */
public record CellMark(ResourceLocation model, int x, int y, int z, DamageMark mark) {

    /** How a block keeps it and sends it. */
    public static final Codec<CellMark> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("model").forGetter(CellMark::model),
            Codec.INT.fieldOf("x").forGetter(CellMark::x),
            Codec.INT.fieldOf("y").forGetter(CellMark::y),
            Codec.INT.fieldOf("z").forGetter(CellMark::z),
            DamageMark.CODEC.fieldOf("mark").forGetter(CellMark::mark)).apply(instance, CellMark::new));

    public CellMark {
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(mark, "mark");
    }

    /** Whether it is laid on the block {@code place} of its model. */
    public boolean on(final FormedPlace place) {
        return this.model.equals(place.model()) && this.x == place.x() && this.y == place.y() && this.z == place.z();
    }
}
