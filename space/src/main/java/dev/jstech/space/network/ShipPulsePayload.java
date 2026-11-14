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
 * How far a ship's pulse jump has carried it beyond where it stands, at a tick of the server, told every tick to
 * whoever is aboard: their game draws the worlds and the Sun moving round the ship, which stands still. Its jump over,
 * it is told once more, how far it went, no longer jumping, in the tick the ship is set that far on.
 *
 * @param entity  the ship's structure's entity
 * @param tick    the server's tick it is of
 * @param dx      how far east its jump has carried it, in blocks
 * @param dz      how far south
 * @param jumping whether it is still jumping; not, its jump is over
 */
public record ShipPulsePayload(int entity, long tick, double dx, double dz, boolean jumping)
        implements CustomPacketPayload {

    public static final Type<ShipPulsePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JsSpace.MODID,
            "ship_pulse"));
    public static final StreamCodec<ByteBuf, ShipPulsePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ShipPulsePayload::entity,
            ByteBufCodecs.VAR_LONG, ShipPulsePayload::tick,
            ByteBufCodecs.DOUBLE, ShipPulsePayload::dx,
            ByteBufCodecs.DOUBLE, ShipPulsePayload::dz,
            ByteBufCodecs.BOOL, ShipPulsePayload::jumping,
            ShipPulsePayload::new);

    @Override
    public Type<ShipPulsePayload> type() {
        return TYPE;
    }
}
