/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.energy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * What a clamp meter is clamped on, kept in the item: a wire of a line, named by its cable, or a block, read face by
 * face.
 *
 * @param dimension where
 * @param pos       the block: the cable block of the wire, or the block read
 * @param cable     the wire's cable, for a line; none for a block
 */
public record MeterClamp(ResourceKey<Level> dimension, BlockPos pos, Optional<ResourceLocation> cable) {

    public static final Codec<MeterClamp> CODEC = RecordCodecBuilder.create(clamp -> clamp.group(
            Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(MeterClamp::dimension),
            BlockPos.CODEC.fieldOf("pos").forGetter(MeterClamp::pos),
            ResourceLocation.CODEC.optionalFieldOf("cable").forGetter(MeterClamp::cable)
    ).apply(clamp, MeterClamp::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MeterClamp> STREAM_CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(Registries.DIMENSION), MeterClamp::dimension,
            BlockPos.STREAM_CODEC, MeterClamp::pos,
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), MeterClamp::cable,
            MeterClamp::new);

    /** Whether it is clamped on a line rather than a block. */
    public boolean onLine() {
        return this.cable.isPresent();
    }
}
