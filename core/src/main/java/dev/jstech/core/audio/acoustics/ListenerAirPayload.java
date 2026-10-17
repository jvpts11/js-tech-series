/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.acoustics;

import dev.jstech.core.JsCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: the air round this player's head, which only the server knows (a sealed room's, or the place's
 * at that height), and the room's size when one holds them, for how sounds travel and echo where they stand.
 *
 * @param pressure    the air's pressure, in kilopascals
 * @param temperature the air's temperature, in degrees Celsius
 * @param roomVolume  the sealed room's air, in cubic metres, or 0 outside one
 * @param roomSurface the sealed room's walls, in square metres, or 0
 */
public record ListenerAirPayload(float pressure, float temperature, float roomVolume, float roomSurface)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ListenerAirPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "listener_air"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ListenerAirPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, ListenerAirPayload::pressure,
            ByteBufCodecs.FLOAT, ListenerAirPayload::temperature,
            ByteBufCodecs.FLOAT, ListenerAirPayload::roomVolume,
            ByteBufCodecs.FLOAT, ListenerAirPayload::roomSurface,
            ListenerAirPayload::new);

    /** Whether it says the same as {@code other}, near enough that telling the player again would change nothing. */
    public boolean same(final ListenerAirPayload other) {
        return Math.abs(this.pressure - other.pressure) <= Math.max(0.01F, this.pressure * 0.01F)
                && Math.abs(this.temperature - other.temperature) < 1F && this.roomVolume == other.roomVolume
                && this.roomSurface == other.roomSurface;
    }

    @Override
    public CustomPacketPayload.Type<ListenerAirPayload> type() {
        return TYPE;
    }
}
