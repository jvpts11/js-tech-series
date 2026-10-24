/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.transit;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

/**
 * A capsule on its way through the tubes.
 *
 * @param from    the station that sent it
 * @param to      the station it goes to
 * @param items   what it carries
 * @param arrives the game time it arrives at
 */
public record Capsule(BlockPos from, BlockPos to, List<ItemStack> items, long arrives) {

    /** How a capsule is saved with its world. */
    public static final Codec<Capsule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("from").forGetter(Capsule::from),
            BlockPos.CODEC.fieldOf("to").forGetter(Capsule::to),
            ItemStack.CODEC.listOf().fieldOf("items").forGetter(Capsule::items),
            Codec.LONG.fieldOf("arrives").forGetter(Capsule::arrives)
    ).apply(instance, Capsule::new));

    public Capsule {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        items = items.stream().map(ItemStack::copy).toList();
    }

    /** The same capsule, arriving at {@code time} instead. */
    public Capsule arrivingAt(final long time) {
        return new Capsule(this.from, this.to, this.items, time);
    }
}
