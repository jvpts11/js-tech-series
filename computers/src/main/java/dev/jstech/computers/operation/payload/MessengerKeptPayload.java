/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: the conversations a Messenger Service keeps, for the Kept conversations window at its own server,
 * and what they weigh on that server's disk together.
 */
public record MessengerKeptPayload(List<Kept> conversations, long bytes) implements CustomPacketPayload {

    /** How many conversations travel; the service keeps at most this many messages, so never more rooms. */
    public static final int MOST = 500;
    /** The longest room name and list of names that travel, cut to here before they are sent. */
    public static final int MOST_LETTERS = 100;

    public static final CustomPacketPayload.Type<MessengerKeptPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "messenger_kept"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MessengerKeptPayload> STREAM_CODEC =
            StreamCodec.composite(
                    Kept.STREAM_CODEC.apply(ByteBufCodecs.list(MOST)), MessengerKeptPayload::conversations,
                    ByteBufCodecs.VAR_LONG, MessengerKeptPayload::bytes,
                    MessengerKeptPayload::new);

    public MessengerKeptPayload {
        conversations = List.copyOf(conversations);
    }

    @Override
    public CustomPacketPayload.Type<MessengerKeptPayload> type() {
        return TYPE;
    }

    /**
     * One conversation kept.
     *
     * @param room   its room, which a deletion names
     * @param names  its two people as they wrote their names, or empty for everybody's
     * @param lines  how many things were said in it
     * @param bytes  what it weighs on the disk
     * @param saidAt the world's clock when the last of them was said
     */
    public record Kept(String room, String names, int lines, long bytes, long saidAt) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Kept> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.stringUtf8(MOST_LETTERS), Kept::room,
                        ByteBufCodecs.stringUtf8(MOST_LETTERS), Kept::names,
                        ByteBufCodecs.VAR_INT, Kept::lines,
                        ByteBufCodecs.VAR_LONG, Kept::bytes,
                        ByteBufCodecs.VAR_LONG, Kept::saidAt,
                        Kept::new);
    }
}
