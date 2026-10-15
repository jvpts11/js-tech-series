/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.overhead;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Where the wire in a player's hand is being hung from, kept in the item between the first insulator used and the
 * next.
 *
 * @param dimension where
 * @param end       the insulator
 */
public record HangStart(ResourceKey<Level> dimension, WireEnd end) {

    public static final Codec<HangStart> CODEC = RecordCodecBuilder.create(start -> start.group(
            Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(HangStart::dimension),
            WireEnd.CODEC.fieldOf("end").forGetter(HangStart::end)
    ).apply(start, HangStart::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, HangStart> STREAM_CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(Registries.DIMENSION), HangStart::dimension,
            WireEnd.STREAM_CODEC, HangStart::end,
            HangStart::new);
}
