/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.network;

import dev.jstech.space.JsSpace;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * The landing map a Flight Computer asked for: the ground of the world it goes to round the spot it looks at, as a far
 * map holds it, each pixel's biome and height, how wide that world is, which only the server knows, and what the team
 * asking knows of that ground.
 *
 * @param world the world it is a map of
 * @param width how wide that world is, in blocks, its border's width
 * @param map   the ground round the spot
 * @param sight what the team knows of it
 */
public record LandingMapPayload(ResourceLocation world, double width, FarMapPayload map, LandingSight sight)
        implements CustomPacketPayload {

    public static final Type<LandingMapPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "landing_map"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LandingMapPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, LandingMapPayload::world,
            ByteBufCodecs.DOUBLE, LandingMapPayload::width,
            FarMapPayload.STREAM_CODEC, LandingMapPayload::map,
            LandingSight.STREAM_CODEC, LandingMapPayload::sight,
            LandingMapPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
