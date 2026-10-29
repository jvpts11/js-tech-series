/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.network;

import dev.jstech.space.JsSpace;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * A player pressed the light of their suit's controls, held up: the face is lit, or not, from now on.
 *
 * @param on whether the face is lit
 */
public record SuitLightPayload(boolean on) implements CustomPacketPayload {

    public static final Type<SuitLightPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JsSpace.MODID,
            "suit_light"));
    public static final StreamCodec<ByteBuf, SuitLightPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SuitLightPayload::on, SuitLightPayload::new);

    @Override
    public Type<SuitLightPayload> type() {
        return TYPE;
    }
}
