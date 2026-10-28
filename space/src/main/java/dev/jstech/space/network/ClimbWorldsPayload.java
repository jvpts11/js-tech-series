/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.network;

import dev.jstech.space.JsSpace;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * The worlds under a system's space as their climbs draw them, sent to a player arriving in a system's space: so one
 * going down through the floor of the low orbits sees, on the way, the very picture the world's climb shows where
 * they will arrive in it.
 *
 * @param worlds each world, by its sphere
 */
public record ClimbWorldsPayload(List<ClimbWorld> worlds) implements CustomPacketPayload {

    public static final Type<ClimbWorldsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "climb_worlds"));
    /* The most worlds a system holds. */
    private static final int MAX_WORLDS = 64;
    public static final StreamCodec<RegistryFriendlyByteBuf, ClimbWorldsPayload> STREAM_CODEC = StreamCodec.composite(
            ClimbWorld.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_WORLDS)), ClimbWorldsPayload::worlds,
            ClimbWorldsPayload::new);

    public ClimbWorldsPayload {
        worlds = List.copyOf(worlds);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * One world under the space.
     *
     * @param body   its sphere, by name
     * @param limit  its build limit
     * @param sea    its sea's height
     * @param width  how many blocks round it is
     */
    public record ClimbWorld(String body, int limit, int sea, double width) {

        static final StreamCodec<RegistryFriendlyByteBuf, ClimbWorld> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, ClimbWorld::body,
                ByteBufCodecs.VAR_INT, ClimbWorld::limit,
                ByteBufCodecs.VAR_INT, ClimbWorld::sea,
                ByteBufCodecs.DOUBLE, ClimbWorld::width,
                ClimbWorld::new);
    }
}
