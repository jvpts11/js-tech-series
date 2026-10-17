/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.hazard;

import dev.jstech.core.JsCore;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: what has built up in this player of each hazard, and how fast it builds up where they are, for the
 * instruments in their hands and the body's screen. Sent once a second, when anything changed.
 *
 * @param hazards each hazard the player has some of, or takes some of
 */
public record HazardsPayload(List<Entry> hazards) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<HazardsPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "hazards"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HazardsPayload> STREAM_CODEC = StreamCodec.composite(
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), HazardsPayload::hazards,
            HazardsPayload::new);

    public HazardsPayload {
        hazards = List.copyOf(hazards);
    }

    @Override
    public CustomPacketPayload.Type<HazardsPayload> type() {
        return TYPE;
    }

    /**
     * One hazard of the player's.
     *
     * @param id    the hazard's id
     * @param level what has built up, in its unit
     * @param rate     how fast it builds up where they are, in its unit a second, what they wear having taken its share
     * @param exposure how fast it comes at them where they are, in its unit a second, before what they wear
     */
    public record Entry(ResourceLocation id, float level, float rate, float exposure) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, Entry::id,
                ByteBufCodecs.FLOAT, Entry::level,
                ByteBufCodecs.FLOAT, Entry::rate,
                ByteBufCodecs.FLOAT, Entry::exposure,
                Entry::new);
    }
}
