/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Client to server: a sheet asking what the network holds of the things its cells name.
 *
 * <p>It asks about the handful of things it actually mentions rather than being sent the whole network,
 * because a sheet with four asking cells has no use for ten thousand rows and the machine has better
 * things to do than send them.
 */
public record RequestSheetFactsPayload(BlockPos hostPos, BlockPos monitorPos,
                                       List<String> items) implements CustomPacketPayload {

    public RequestSheetFactsPayload {
        items = List.copyOf(items);
    }

    /** How many things one sheet may ask about, which is more than a sheet this size can hold anyway. */
    public static final int MAX_ITEMS = 128;

    public static final CustomPacketPayload.Type<RequestSheetFactsPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "request_sheet_facts"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestSheetFactsPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, RequestSheetFactsPayload::hostPos,
                    BlockPos.STREAM_CODEC, RequestSheetFactsPayload::monitorPos,
                    ByteBufCodecs.stringUtf8(128).apply(ByteBufCodecs.list(MAX_ITEMS)),
                    RequestSheetFactsPayload::items,
                    RequestSheetFactsPayload::new);

    @Override
    public CustomPacketPayload.Type<RequestSheetFactsPayload> type() {
        return TYPE;
    }
}
