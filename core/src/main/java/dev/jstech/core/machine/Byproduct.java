/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/**
 * What a recipe may make besides its outputs, by chance: a grinder's stone dust, a separator's trace of gold. A machine
 * starts a recipe only with room for its byproducts too, so it never ends one it cannot finish.
 *
 * @param stack  what it makes
 * @param chance how likely, from above none to always
 */
public record Byproduct(ItemStack stack, float chance) {

    public static final Codec<Byproduct> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.CODEC.fieldOf("item").forGetter(Byproduct::stack),
            Codec.floatRange(Float.MIN_NORMAL, 1.0F).fieldOf("chance").forGetter(Byproduct::chance)
    ).apply(instance, Byproduct::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, Byproduct> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC, Byproduct::stack,
            ByteBufCodecs.FLOAT, Byproduct::chance,
            Byproduct::new);

    public Byproduct {
        Objects.requireNonNull(stack, "stack");
        if (!(chance > 0.0F && chance <= 1.0F)) {
            throw new IllegalArgumentException("a byproduct comes now and then or always, not at " + chance);
        }
        stack = stack.copy();
    }

    /** Whether it comes this time. */
    public boolean comes(final RandomSource random) {
        return this.chance >= 1.0F || random.nextFloat() < this.chance;
    }
}
