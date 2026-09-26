/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.media;

import dev.jstech.core.JsCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * The server has no recording by the name a client asked for, so the client stops waiting for it.
 *
 * @param hash the recording asked for, by its hash
 */
public record MediaMissingPayload(String hash) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<MediaMissingPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "media_missing"));

    public static final StreamCodec<ByteBuf, MediaMissingPayload> STREAM_CODEC =
            ByteBufCodecs.stringUtf8(MediaId.HASH_DIGITS).map(MediaMissingPayload::new, MediaMissingPayload::hash);

    @Override
    public CustomPacketPayload.Type<MediaMissingPayload> type() {
        return TYPE;
    }
}
