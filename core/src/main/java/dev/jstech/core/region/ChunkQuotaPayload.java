/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.region;

import dev.jstech.core.JsCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * How many chunks a player's machines keep loaded and how many they may, sent to that player as they join and
 * whenever the count changes.
 *
 * @param held  the chunks their machines keep loaded, across every dimension
 * @param limit how many they may keep
 */
record ChunkQuotaPayload(int held, int limit) implements CustomPacketPayload {

    static final Type<ChunkQuotaPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "chunk_quota"));

    static final StreamCodec<RegistryFriendlyByteBuf, ChunkQuotaPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ChunkQuotaPayload::held,
            ByteBufCodecs.VAR_INT, ChunkQuotaPayload::limit,
            ChunkQuotaPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
