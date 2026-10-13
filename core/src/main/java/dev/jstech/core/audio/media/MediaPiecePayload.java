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
 * One piece of a recording on its way to a client, which puts the pieces together in order and checks them against
 * the recording's name when the last has come.
 *
 * @param hash  the recording the piece is of, by its hash
 * @param index which piece it is, counted from nought
 * @param count how many pieces the recording comes in
 * @param data  the piece's bytes
 */
public record MediaPiecePayload(String hash, int index, int count, byte[] data) implements CustomPacketPayload {

    /** The most bytes one piece carries, well inside what one message to a client may. */
    public static final int PIECE_BYTES = 32 * 1024;

    public static final CustomPacketPayload.Type<MediaPiecePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "media_piece"));

    public static final StreamCodec<ByteBuf, MediaPiecePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(MediaId.HASH_DIGITS), MediaPiecePayload::hash,
            ByteBufCodecs.VAR_INT, MediaPiecePayload::index,
            ByteBufCodecs.VAR_INT, MediaPiecePayload::count,
            ByteBufCodecs.byteArray(PIECE_BYTES), MediaPiecePayload::data,
            MediaPiecePayload::new);

    /* Copied on the way in, so the piece cannot change under whoever holds it. */
    public MediaPiecePayload {
        data = data.clone();
    }

    /** How many pieces a recording of that many bytes comes in; one at least, even for an empty one. */
    public static int piecesFor(final long bytes) {
        return Pieces.count(bytes, PIECE_BYTES);
    }

    @Override
    public CustomPacketPayload.Type<MediaPiecePayload> type() {
        return TYPE;
    }
}
