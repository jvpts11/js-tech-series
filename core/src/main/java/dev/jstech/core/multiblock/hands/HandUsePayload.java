/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.hands;

import dev.jstech.core.JsCore;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * A player clicked a part of a formed structure's model they were looking at: the server checks their line of sight
 * meets it, and works it.
 *
 * @param terminal the structure's terminal
 * @param part     the part, by its place among the parts its model declares
 */
public record HandUsePayload(BlockPos terminal, int part) implements CustomPacketPayload {

    public static final Type<HandUsePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "hand_use"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HandUsePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, HandUsePayload::terminal, ByteBufCodecs.VAR_INT, HandUsePayload::part,
            HandUsePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
