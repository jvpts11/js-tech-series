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
 * A player at a ship's helm holds its boost, or has let it go; held, it is told again every so often, as the helm's
 * keys are.
 *
 * @param held whether they hold it
 */
public record ShipBoostPayload(boolean held) implements CustomPacketPayload {

    public static final Type<ShipBoostPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JsSpace.MODID,
            "ship_boost"));
    public static final StreamCodec<ByteBuf, ShipBoostPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ShipBoostPayload::held, ShipBoostPayload::new);

    @Override
    public Type<ShipBoostPayload> type() {
        return TYPE;
    }
}
