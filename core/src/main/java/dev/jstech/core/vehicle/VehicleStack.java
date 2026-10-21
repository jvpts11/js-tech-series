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
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * Which vehicle a vehicle's item places, the shape it comes in, and, for one picked up, all it was and held.
 *
 * @param vehicle the kind of vehicle, by id
 * @param variant the shape it comes in, by the variant's id, or empty for its first
 * @param state   what it was and held when it was picked up, or none for a new one
 */
public record VehicleStack(ResourceLocation vehicle, String variant, Optional<CompoundTag> state) {

    public static final Codec<VehicleStack> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("vehicle").forGetter(VehicleStack::vehicle),
            Codec.STRING.optionalFieldOf("variant", "").forGetter(VehicleStack::variant),
            CompoundTag.CODEC.optionalFieldOf("state").forGetter(VehicleStack::state))
            .apply(instance, VehicleStack::new));

    public static final StreamCodec<ByteBuf, VehicleStack> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, VehicleStack::vehicle,
            ByteBufCodecs.STRING_UTF8, VehicleStack::variant,
            ByteBufCodecs.optional(ByteBufCodecs.COMPOUND_TAG), VehicleStack::state,
            VehicleStack::new);

    /** A new vehicle of the kind {@code vehicle}, in its first shape. */
    public static VehicleStack of(final ResourceLocation vehicle) {
        return new VehicleStack(vehicle, "", Optional.empty());
    }
}
