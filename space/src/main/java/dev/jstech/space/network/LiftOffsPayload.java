/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.network;

import dev.jstech.space.JsSpace;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Where a player last took off from in each world, sent to their game as they join and as one place changes: their
 * ship's HUD shows them the way back there.
 *
 * @param places each world's place, by the world
 */
public record LiftOffsPayload(Map<ResourceLocation, BlockPos> places) implements CustomPacketPayload {

    public static final Type<LiftOffsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "lift_offs"));
    /* The most worlds a player's places are sent for. */
    private static final int MAX_WORLDS = 256;
    public static final StreamCodec<RegistryFriendlyByteBuf, LiftOffsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, BlockPos.STREAM_CODEC, MAX_WORLDS),
            LiftOffsPayload::places, LiftOffsPayload::new);

    public LiftOffsPayload {
        places = Map.copyOf(places);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
