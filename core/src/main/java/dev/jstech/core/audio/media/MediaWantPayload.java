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
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * A client asks for a recording it is about to hear and does not have yet. The server sends it in pieces, as fast as
 * its owner lets it, or says it has none by that name.
 *
 * @param media the recording, by its name
 */
public record MediaWantPayload(MediaId media) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<MediaWantPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "media_want"));

    public static final StreamCodec<ByteBuf, MediaWantPayload> STREAM_CODEC =
            MediaCodecs.ID.map(MediaWantPayload::new, MediaWantPayload::media);

    @Override
    public CustomPacketPayload.Type<MediaWantPayload> type() {
        return TYPE;
    }
}
