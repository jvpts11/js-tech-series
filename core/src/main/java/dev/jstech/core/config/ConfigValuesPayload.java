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
 * A world's settings as the server holds them, sent to a player as they join and again whenever one changes, so their
 * game shows and uses the server's values.
 *
 * @param file   the file's name, without its extension
 * @param values every setting's value, written as JSON
 */
record ConfigValuesPayload(String file, byte[] values) implements CustomPacketPayload {

    /** The most a file's values may take, well inside what the server may send in one payload. */
    static final int MOST_BYTES = 1 << 19;
    /** The longest name a file may have. */
    static final int MOST_NAME = 128;

    static final Type<ConfigValuesPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "config_values"));

    static final StreamCodec<RegistryFriendlyByteBuf, ConfigValuesPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(MOST_NAME), ConfigValuesPayload::file,
            ByteBufCodecs.byteArray(MOST_BYTES), ConfigValuesPayload::values,
            ConfigValuesPayload::new);

    ConfigValuesPayload {
        values = values.clone();
    }

    /** What was sent, a copy each time, so nobody changes what was received. */
    @Override
    public byte[] values() {
        return values.clone();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
