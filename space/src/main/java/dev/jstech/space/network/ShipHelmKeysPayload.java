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
 * Which of its own keys a player at a ship's helm holds: its boost and its pulse jump. Told as either changes, and
 * again every so often while one is held, as the helm's keys are.
 *
 * @param boost whether they hold the boost
 * @param pulse whether they hold the pulse jump
 */
public record ShipHelmKeysPayload(boolean boost, boolean pulse) implements CustomPacketPayload {

    public static final Type<ShipHelmKeysPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            JsSpace.MODID, "ship_helm_keys"));
    public static final StreamCodec<ByteBuf, ShipHelmKeysPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ShipHelmKeysPayload::boost,
            ByteBufCodecs.BOOL, ShipHelmKeysPayload::pulse,
            ShipHelmKeysPayload::new);

    @Override
    public Type<ShipHelmKeysPayload> type() {
        return TYPE;
    }
}
