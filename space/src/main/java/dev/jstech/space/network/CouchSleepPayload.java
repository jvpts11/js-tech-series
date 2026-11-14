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

/** A player fell asleep in their crew seat, or woke: their game darkens their view, or clears it. */
public record CouchSleepPayload(boolean asleep) implements CustomPacketPayload {

    public static final Type<CouchSleepPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            JsSpace.MODID, "couch_sleep"));
    public static final StreamCodec<ByteBuf, CouchSleepPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, CouchSleepPayload::asleep, CouchSleepPayload::new);

    @Override
    public Type<CouchSleepPayload> type() {
        return TYPE;
    }
}
