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
 * A player set the signal probe in their hand from its keys.
 *
 * @param mainHand whether it is the probe in the main hand; the off hand's otherwise
 * @param state    how it is set now
 */
public record SignalProbeStatePayload(boolean mainHand, SignalProbeState state) implements CustomPacketPayload {

    public static final Type<SignalProbeStatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            JsCore.MODID, "signal_probe_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SignalProbeStatePayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, SignalProbeStatePayload::mainHand,
                    SignalProbeState.STREAM_CODEC, SignalProbeStatePayload::state,
                    SignalProbeStatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
