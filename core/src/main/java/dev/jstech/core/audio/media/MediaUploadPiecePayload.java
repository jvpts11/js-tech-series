/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.media;

import dev.jstech.core.JsCore;
import dev.jstech.core.network.transfer.Pieces;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * One piece of a recording a player brings, sent once the server has said to send it.
 *
 * @param token the offer the piece belongs to
 * @param index which piece it is, counted from nought
 * @param data  the piece's bytes
 */
public record MediaUploadPiecePayload(int token, int index, byte[] data) implements CustomPacketPayload {

    /** The most bytes one piece carries: a message to the server may be no bigger than 32 kilobytes in all. */
    public static final int PIECE_BYTES = 24 * 1024;

    public static final CustomPacketPayload.Type<MediaUploadPiecePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "media_upload_piece"));

    public static final StreamCodec<ByteBuf, MediaUploadPiecePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MediaUploadPiecePayload::token,
            ByteBufCodecs.VAR_INT, MediaUploadPiecePayload::index,
            ByteBufCodecs.byteArray(PIECE_BYTES), MediaUploadPiecePayload::data,
            MediaUploadPiecePayload::new);

    /* Copied on the way in, so the piece cannot change under whoever holds it. */
    public MediaUploadPiecePayload {
        data = data.clone();
    }

    /** How many pieces a recording of that many bytes is sent in; one at least, even for an empty one. */
    public static int piecesFor(final long bytes) {
        return Pieces.count(bytes, PIECE_BYTES);
    }

    @Override
    public CustomPacketPayload.Type<MediaUploadPiecePayload> type() {
        return TYPE;
    }
}
