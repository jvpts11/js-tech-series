/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Server to client: the state of the network's messenger, as one window needs to draw it.
 *
 * <p>It carries what the service weighs as well as what was said, because the weight is the mechanic and
 * a player who cannot see it has no reason to care about it.
 */
public record MessengerStatePayload(Service service, List<String> people, List<String> rooms,
                                    String room, List<Line> lines) implements CustomPacketPayload {

    public MessengerStatePayload {
        people = List.copyOf(people);
        rooms = List.copyOf(rooms);
        lines = List.copyOf(lines);
    }

    /**
     * What the service is and what it costs.
     *
     * <p>Grouped rather than spread across the payload because a stream codec is built of at most six
     * pairs, and because these four belong together: they are the service, and the rest is the talk.
     */
    public record Service(boolean online, String host, long historyBytes, int ramMb) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Service> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.BOOL, Service::online,
                        ByteBufCodecs.stringUtf8(64), Service::host,
                        ByteBufCodecs.VAR_LONG, Service::historyBytes,
                        ByteBufCodecs.VAR_INT, Service::ramMb,
                        Service::new);
    }

    /** How many lines of one conversation travel at a time. */
    public static final int MAX_LINES = 64;

    /** How many people and rooms are listed. */
    public static final int MAX_NAMES = 64;

    /** One thing somebody said, as the window shows it. */
    public record Line(String from, String text, boolean nudge, boolean online) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Line> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.stringUtf8(48), Line::from,
                        ByteBufCodecs.stringUtf8(256), Line::text,
                        ByteBufCodecs.BOOL, Line::nudge,
                        ByteBufCodecs.BOOL, Line::online,
                        Line::new);
    }

    public static final CustomPacketPayload.Type<MessengerStatePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "messenger_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MessengerStatePayload> STREAM_CODEC =
            StreamCodec.composite(
                    Service.STREAM_CODEC, MessengerStatePayload::service,
                    ByteBufCodecs.stringUtf8(48).apply(ByteBufCodecs.list(MAX_NAMES)),
                    MessengerStatePayload::people,
                    ByteBufCodecs.stringUtf8(48).apply(ByteBufCodecs.list(MAX_NAMES)),
                    MessengerStatePayload::rooms,
                    ByteBufCodecs.stringUtf8(48), MessengerStatePayload::room,
                    Line.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_LINES)), MessengerStatePayload::lines,
                    MessengerStatePayload::new);

    @Override
    public CustomPacketPayload.Type<MessengerStatePayload> type() {
        return TYPE;
    }
}
