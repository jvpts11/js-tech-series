/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.media;

import dev.jstech.core.JsCore;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * The last piece of a recording a player brought has come: it was kept and taken, or it was not, and why.
 *
 * @param token   the offer it ends
 * @param ok      whether the recording was kept and taken
 * @param message what taking it did, or why it was not kept
 */
public record MediaUploadDonePayload(int token, boolean ok, Text message) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<MediaUploadDonePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "media_upload_done"));

    public static final StreamCodec<ByteBuf, MediaUploadDonePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MediaUploadDonePayload::token,
            ByteBufCodecs.BOOL, MediaUploadDonePayload::ok,
            TextCodecs.STREAM_CODEC, MediaUploadDonePayload::message,
            MediaUploadDonePayload::new);

    @Override
    public CustomPacketPayload.Type<MediaUploadDonePayload> type() {
        return TYPE;
    }
}
