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
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What a key is cut for, as its item carries it: the vehicle, the generation of its keys it was cut in, and the
 * vehicle's name for the key to show. Forgetting a vehicle's keys starts a new generation, so the old keys open
 * nothing.
 *
 * @param vehicle    the vehicle, by whom its keys are cut for ({@link VehicleEntity#keyId()})
 * @param generation the generation of its keys
 * @param name       the translation key of the vehicle's name
 */
public record VehicleKey(UUID vehicle, int generation, String name) {

    public static final Codec<VehicleKey> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("vehicle").forGetter(VehicleKey::vehicle),
            Codec.INT.fieldOf("generation").forGetter(VehicleKey::generation),
            Codec.STRING.optionalFieldOf("name", "").forGetter(VehicleKey::name))
            .apply(instance, VehicleKey::new));

    public static final StreamCodec<ByteBuf, VehicleKey> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, VehicleKey::vehicle,
            ByteBufCodecs.VAR_INT, VehicleKey::generation,
            ByteBufCodecs.STRING_UTF8, VehicleKey::name,
            VehicleKey::new);
}
