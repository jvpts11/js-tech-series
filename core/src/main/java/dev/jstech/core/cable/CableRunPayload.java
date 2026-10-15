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
 * A player dragged a run of cable from one block to another, with the cable in one hand: the server lays it.
 *
 * @param from     the block the run starts at
 * @param to       the block it was dragged to
 * @param mainHand whether the cable is in the main hand; the off hand otherwise
 */
public record CableRunPayload(BlockPos from, BlockPos to, boolean mainHand) implements CustomPacketPayload {

    public static final Type<CableRunPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "cable_run"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CableRunPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, CableRunPayload::from,
            BlockPos.STREAM_CODEC, CableRunPayload::to,
            ByteBufCodecs.BOOL, CableRunPayload::mainHand,
            CableRunPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
