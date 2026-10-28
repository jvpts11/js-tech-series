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
 * What a player's game draws the Overworld's sphere from: a number the server makes from the world's seed, so every
 * player sees the same continents, while the seed itself never leaves the server.
 *
 * @param mapSeed the number the map is drawn from
 */
public record OverworldMapPayload(long mapSeed) implements CustomPacketPayload {

    public static final Type<OverworldMapPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "overworld_map"));
    public static final StreamCodec<ByteBuf, OverworldMapPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, OverworldMapPayload::mapSeed, OverworldMapPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** The number the map is drawn from, made from the world's seed one way only. */
    public static long mapSeedOf(final long worldSeed) {
        long mixed = worldSeed ^ 0x9E3779B97F4A7C15L;
        mixed = (mixed ^ (mixed >>> 33)) * 0xFF51AFD7ED558CCDL;
        mixed = (mixed ^ (mixed >>> 33)) * 0xC4CEB9FE1A85EC53L;
        return mixed ^ (mixed >>> 33);
    }
}
