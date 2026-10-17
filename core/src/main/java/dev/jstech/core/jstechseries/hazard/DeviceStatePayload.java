/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.hazard;

import dev.jstech.core.JsCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server: the player set the hazard device in one of their hands from its buttons.
 *
 * @param mainHand whether the device is in the main hand, not the off hand
 * @param state    how it is set now
 */
public record DeviceStatePayload(boolean mainHand, DeviceState state) implements CustomPacketPayload {

    public static final Type<DeviceStatePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "hazard_device_state"));
    public static final StreamCodec<ByteBuf, DeviceStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, DeviceStatePayload::mainHand,
            DeviceState.STREAM_CODEC, DeviceStatePayload::state,
            DeviceStatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
