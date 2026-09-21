/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server: everything one messenger window asks of the network's service.
 *
 * <p>One payload for all of it rather than four nearly identical ones: what differs between opening a
 * conversation, saying something, nudging and closing is a word and a line of text, and four types that
 * carry the same two fields is four places for them to drift apart.
 */
public record MessengerActionPayload(BlockPos hostPos, BlockPos monitorPos, int action,
                                     String room, String text) implements CustomPacketPayload {

    /** Say nothing, only ask for the state of a room, and say this window is open. */
    public static final int LOOK = 0;

    /** Say something in a room. */
    public static final int SAY = 1;

    /** Nudge the room, which is a message with no words. */
    public static final int NUDGE = 2;

    /** Say this window is closed, so the service stops counting the memory it was holding for it. */
    public static final int LEAVE = 3;

    public static final CustomPacketPayload.Type<MessengerActionPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "messenger_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MessengerActionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, MessengerActionPayload::hostPos,
                    BlockPos.STREAM_CODEC, MessengerActionPayload::monitorPos,
                    ByteBufCodecs.VAR_INT, MessengerActionPayload::action,
                    ByteBufCodecs.stringUtf8(48), MessengerActionPayload::room,
                    ByteBufCodecs.stringUtf8(256), MessengerActionPayload::text,
                    MessengerActionPayload::new);

    @Override
    public CustomPacketPayload.Type<MessengerActionPayload> type() {
        return TYPE;
    }
}
