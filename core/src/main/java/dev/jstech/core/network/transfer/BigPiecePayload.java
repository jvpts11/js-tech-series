/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.network.transfer;

import dev.jstech.core.JsCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * One piece of a value too large for one message, packed, on its way either way.
 *
 * @param kind     the declared kind of value it is a piece of
 * @param transfer which sending of that kind it belongs to
 * @param index    which piece it is, counted from nought
 * @param count    how many pieces the sending comes in
 * @param data     the piece's bytes
 */
public record BigPiecePayload(ResourceLocation kind, int transfer, int index, int count, byte[] data)
        implements CustomPacketPayload {

    public static final Type<BigPiecePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "big_piece"));
    public static final StreamCodec<ByteBuf, BigPiecePayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, BigPiecePayload::kind,
            ByteBufCodecs.VAR_INT, BigPiecePayload::transfer,
            ByteBufCodecs.VAR_INT, BigPiecePayload::index,
            ByteBufCodecs.VAR_INT, BigPiecePayload::count,
            ByteBufCodecs.byteArray(BigPayloads.PIECE_BYTES), BigPiecePayload::data,
            BigPiecePayload::new);

    /* Copied on the way in, so the piece cannot change under whoever holds it. */
    public BigPiecePayload {
        data = data.clone();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
