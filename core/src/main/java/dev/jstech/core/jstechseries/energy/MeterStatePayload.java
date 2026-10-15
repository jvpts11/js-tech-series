/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.energy;

import dev.jstech.core.JsCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * A player set the clamp meter in a hand from its display: its dial and its keys.
 *
 * @param mainHand whether the meter is in the main hand rather than the other
 * @param state    how it is set now
 */
public record MeterStatePayload(boolean mainHand, MeterState state) implements CustomPacketPayload {

    public static final Type<MeterStatePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "meter_state"));
    public static final StreamCodec<ByteBuf, MeterStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, MeterStatePayload::mainHand,
            MeterState.STREAM_CODEC, MeterStatePayload::state,
            MeterStatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
