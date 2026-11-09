/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.builder;

import dev.jstech.core.JsCore;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * The server answers a Multiblock Builder's display: how many of each block it asked of a place to build from that
 * place holds for its player now.
 *
 * @param source the place to build from, by id
 * @param counts how many of each item it holds, by the item's id
 */
public record SourceCountsPayload(ResourceLocation source, Map<ResourceLocation, Long> counts)
        implements CustomPacketPayload {

    public static final Type<SourceCountsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            JsCore.MODID, "builder_source_counts"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SourceCountsPayload> STREAM_CODEC =
            StreamCodec.composite(ResourceLocation.STREAM_CODEC, SourceCountsPayload::source,
                    ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.VAR_LONG,
                            SourceAskPayload.MOST_ITEMS), SourceCountsPayload::counts,
                    SourceCountsPayload::new);

    public SourceCountsPayload {
        counts = Map.copyOf(counts);
    }

    @Override
    public Type<SourceCountsPayload> type() {
        return TYPE;
    }
}
