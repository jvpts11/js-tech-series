/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.probe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * The wire a network probe is linked to, kept in the item: the line whose part it reads.
 *
 * @param dimension where
 * @param pos       the cable block of the wire
 * @param cable     the wire's cable
 */
public record ProbeLink(ResourceKey<Level> dimension, BlockPos pos, ResourceLocation cable) {

    public static final Codec<ProbeLink> CODEC = RecordCodecBuilder.create(link -> link.group(
            Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(ProbeLink::dimension),
            BlockPos.CODEC.fieldOf("pos").forGetter(ProbeLink::pos),
            ResourceLocation.CODEC.fieldOf("cable").forGetter(ProbeLink::cable)
    ).apply(link, ProbeLink::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ProbeLink> STREAM_CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(Registries.DIMENSION), ProbeLink::dimension,
            BlockPos.STREAM_CODEC, ProbeLink::pos,
            ResourceLocation.STREAM_CODEC, ProbeLink::cable,
            ProbeLink::new);
}
