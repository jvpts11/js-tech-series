/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.input;

import dev.jstech.core.JsCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * A player pressed the key of an action the server has a part in.
 *
 * @param action the action's id
 * @param shift  whether shift was held
 */
public record KeyActionPayload(ResourceLocation action, boolean shift) implements CustomPacketPayload {

    public static final Type<KeyActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "key_action"));
    public static final StreamCodec<ByteBuf, KeyActionPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, KeyActionPayload::action,
            ByteBufCodecs.BOOL, KeyActionPayload::shift,
            KeyActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
