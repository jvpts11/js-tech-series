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
 * The server's answer to an offer: send it, at this pace; or no need, it already has it and it has been taken; or
 * not at all, and why.
 *
 * @param token        the offer it answers
 * @param verdict      {@link #SEND}, {@link #TAKEN} or {@link #REFUSED}
 * @param bytesPerTick how much of it to send a tick, when it is to be sent
 * @param reason       why it was refused, or what taking it did
 */
public record MediaOfferReplyPayload(int token, int verdict, int bytesPerTick, Text reason)
        implements CustomPacketPayload {

    public static final int SEND = 0;
    public static final int TAKEN = 1;
    public static final int REFUSED = 2;

    public static final CustomPacketPayload.Type<MediaOfferReplyPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "media_offer_reply"));

    public static final StreamCodec<ByteBuf, MediaOfferReplyPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MediaOfferReplyPayload::token,
            ByteBufCodecs.VAR_INT, MediaOfferReplyPayload::verdict,
            ByteBufCodecs.VAR_INT, MediaOfferReplyPayload::bytesPerTick,
            TextCodecs.STREAM_CODEC, MediaOfferReplyPayload::reason,
            MediaOfferReplyPayload::new);

    @Override
    public CustomPacketPayload.Type<MediaOfferReplyPayload> type() {
        return TYPE;
    }
}
