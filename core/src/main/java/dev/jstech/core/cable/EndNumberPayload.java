/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.cable;

import dev.jstech.core.JsCore;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * A number a player typed on the screen of a line's end: its priority or its rate.
 *
 * @param pos   the cable block the screen is open on
 * @param rate  whether it is the rate; the priority otherwise
 * @param value the number typed
 */
public record EndNumberPayload(BlockPos pos, boolean rate, long value) implements CustomPacketPayload {

    public static final Type<EndNumberPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "end_number"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EndNumberPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, EndNumberPayload::pos,
            ByteBufCodecs.BOOL, EndNumberPayload::rate,
            ByteBufCodecs.VAR_LONG, EndNumberPayload::value,
            EndNumberPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
