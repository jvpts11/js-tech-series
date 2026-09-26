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
 * A recording a player was hearing stops for them: it was stopped or paused, it ended, or they walked away from it.
 *
 * @param key what was playing
 */
public record MediaStopPayload(String key) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<MediaStopPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "media_stop"));

    public static final StreamCodec<ByteBuf, MediaStopPayload> STREAM_CODEC =
            ByteBufCodecs.stringUtf8(MediaPlayPayload.MAX_KEY).map(MediaStopPayload::new, MediaStopPayload::key);

    @Override
    public CustomPacketPayload.Type<MediaStopPayload> type() {
        return TYPE;
    }
}
