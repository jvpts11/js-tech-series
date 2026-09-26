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
 * A player offers the server a recording from their own computer, saying what it is before a byte of it is sent, so
 * the server can refuse one it would not keep, or take one it already has without it being sent at all.
 *
 * @param token   the player's own number for this offer, which the answers carry back
 * @param media   the recording, by its name, kind and size
 * @param name    what the file is called on the player's computer
 * @param purpose what it is brought for, which says who on the server takes it
 * @param context what the one who takes it needs to know to do with it, as it wrote it
 */
public record MediaOfferPayload(int token, MediaId media, String name, String purpose, String context)
        implements CustomPacketPayload {

    public static final int MAX_NAME = 128;
    public static final int MAX_PURPOSE = 64;
    public static final int MAX_CONTEXT = 512;

    public static final CustomPacketPayload.Type<MediaOfferPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "media_offer"));

    public static final StreamCodec<ByteBuf, MediaOfferPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MediaOfferPayload::token,
            MediaCodecs.ID, MediaOfferPayload::media,
            ByteBufCodecs.stringUtf8(MAX_NAME), MediaOfferPayload::name,
            ByteBufCodecs.stringUtf8(MAX_PURPOSE), MediaOfferPayload::purpose,
            ByteBufCodecs.stringUtf8(MAX_CONTEXT), MediaOfferPayload::context,
            MediaOfferPayload::new);

    @Override
    public CustomPacketPayload.Type<MediaOfferPayload> type() {
        return TYPE;
    }
}
