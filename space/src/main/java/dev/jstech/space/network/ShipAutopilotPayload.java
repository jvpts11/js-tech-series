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
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** A player at a ship's helm pressed its autopilot's key: the ship's autopilot goes on to its next mode. */
public record ShipAutopilotPayload() implements CustomPacketPayload {

    public static final Type<ShipAutopilotPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            JsSpace.MODID, "ship_autopilot"));
    public static final StreamCodec<ByteBuf, ShipAutopilotPayload> STREAM_CODEC = StreamCodec.unit(
            new ShipAutopilotPayload());

    @Override
    public Type<ShipAutopilotPayload> type() {
        return TYPE;
    }
}
