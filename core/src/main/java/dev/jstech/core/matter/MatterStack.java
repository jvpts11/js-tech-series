/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.matter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.Materials;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * An amount of a material in a state that is neither a solid nor a liquid: a gas, a supercritical fluid, a slurry, a
 * plasma, exotic matter. The amount is in millibuckets as the matter would be at the pressure of the open air, so a
 * tank that holds its volume at ten times that pressure holds ten times its volume; a gas is counted at 0 °C, a mole of
 * it to 22.4 millibuckets, which are litres.
 *
 * @param material the material
 * @param amount   how much, in millibuckets at the pressure of the open air; above nothing
 */
public record MatterStack(Material material, long amount) {

    /** How a stack is kept with the world: its material's id and its amount. */
    public static final Codec<MatterStack> CODEC = RecordCodecBuilder.create(stack -> stack.group(
            ResourceLocation.CODEC.fieldOf("material").forGetter(held -> held.material().id()),
            Codec.LONG.fieldOf("amount").forGetter(MatterStack::amount)
    ).apply(stack, (id, amount) -> new MatterStack(Materials.byId(id).orElseThrow(() ->
            new IllegalArgumentException("no material " + id)), amount)));

    public MatterStack {
        Objects.requireNonNull(material, "material");
        if (amount <= 0) {
            throw new IllegalArgumentException("a stack holds something, not " + amount);
        }
    }

    /** The same material, {@code amount} of it. */
    public MatterStack withAmount(final long amount) {
        return new MatterStack(this.material, amount);
    }
}
