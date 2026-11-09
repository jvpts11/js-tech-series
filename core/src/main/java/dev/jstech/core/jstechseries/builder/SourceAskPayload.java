/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.builder;

import dev.jstech.core.JsCore;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * A Multiblock Builder's display asks the server how many of some blocks a place to build from holds for its player,
 * when that place is counted on the server only, as a network's store is.
 *
 * @param source the place to build from, by id
 * @param items  the items of the blocks, by id
 */
public record SourceAskPayload(ResourceLocation source, List<ResourceLocation> items) implements CustomPacketPayload {

    /** The most blocks one ask names: more than any multiblock takes kinds of. */
    public static final int MOST_ITEMS = 256;
    public static final Type<SourceAskPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            JsCore.MODID, "builder_source_ask"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SourceAskPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, SourceAskPayload::source,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list(MOST_ITEMS)), SourceAskPayload::items,
            SourceAskPayload::new);

    public SourceAskPayload {
        items = List.copyOf(items);
    }

    @Override
    public Type<SourceAskPayload> type() {
        return TYPE;
    }
}
