/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.acoustics;

import dev.jstech.core.JsCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: a blast close by left this player's ears ringing, so hard and for so long.
 *
 * @param strength how hard, from 0 to 1
 * @param ticks    for how long
 */
public record EarRingPayload(float strength, int ticks) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<EarRingPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "ear_ring"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EarRingPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, EarRingPayload::strength,
            ByteBufCodecs.VAR_INT, EarRingPayload::ticks,
            EarRingPayload::new);

    /** The ringing it tells of. */
    public EarRing ring() {
        return new EarRing(Math.clamp(this.strength, 0F, 1F), Math.max(0, this.ticks));
    }

    @Override
    public CustomPacketPayload.Type<EarRingPayload> type() {
        return TYPE;
    }
}
