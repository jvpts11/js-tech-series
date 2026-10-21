/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.vehicle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * Which part a part's item is, and how worn, as its item carries it out of a vehicle.
 *
 * @param part   the part, by id
 * @param health how worn it is, 1 new and nought broken
 */
public record VehiclePartStack(ResourceLocation part, float health) {

    public static final Codec<VehiclePartStack> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("part").forGetter(VehiclePartStack::part),
            Codec.floatRange(0, 1).optionalFieldOf("health", 1F).forGetter(VehiclePartStack::health))
            .apply(instance, VehiclePartStack::new));

    public static final StreamCodec<ByteBuf, VehiclePartStack> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, VehiclePartStack::part,
            ByteBufCodecs.FLOAT, VehiclePartStack::health,
            VehiclePartStack::new);
}
