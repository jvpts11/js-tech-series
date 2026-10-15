/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.config;

import dev.jstech.core.JsCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Changes an operator made to a world's settings on their own game, sent to the server, which checks they may make
 * them, keeps them and sends the new values to every player. A save of many changes goes in as many of these as it
 * takes to stay under what a player may send at once.
 *
 * @param file    the file's name, without its extension
 * @param changes each setting changed, by its dotted path, written as JSON
 */
record ConfigEditPayload(String file, byte[] changes) implements CustomPacketPayload {

    /** The most the changes of one payload take: under the most a player may send in one. */
    static final int MOST_BYTES = 30_000;

    static final Type<ConfigEditPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "config_edit"));

    static final StreamCodec<RegistryFriendlyByteBuf, ConfigEditPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(ConfigValuesPayload.MOST_NAME), ConfigEditPayload::file,
            ByteBufCodecs.byteArray(MOST_BYTES), ConfigEditPayload::changes,
            ConfigEditPayload::new);

    ConfigEditPayload {
        changes = changes.clone();
    }

    /** What was sent, a copy each time, so nobody changes what was received. */
    @Override
    public byte[] changes() {
        return changes.clone();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
