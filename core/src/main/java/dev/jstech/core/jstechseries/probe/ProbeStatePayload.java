/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.probe;

import dev.jstech.core.JsCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * A player set the network probe in their hand from its keys.
 *
 * @param mainHand whether it is the probe in the main hand; the off hand's otherwise
 * @param state    how it is set now
 */
public record ProbeStatePayload(boolean mainHand, ProbeState state) implements CustomPacketPayload {

    public static final Type<ProbeStatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "probe_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ProbeStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ProbeStatePayload::mainHand,
            ProbeState.STREAM_CODEC, ProbeStatePayload::state,
            ProbeStatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
