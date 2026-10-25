/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.eventlog.NetworkEventLog;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextBounds;
import dev.jstech.core.text.TextCodecs;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: what the Network Manager's event log shows through the filters it asked with, newest first, and
 * the machines its filter by machine offers. Sent in reply to {@link RequestNetworkEventsPayload}; when the log is
 * still at the revision the window knows, {@code unchanged} says so and nothing else is sent.
 *
 * @param hostPos   the Mainframe the Network Manager runs on
 * @param revision  the log's revision these rows are of
 * @param unchanged the log has not changed since the window last asked, and the rows are left out
 * @param rows      the events the filters keep, newest first
 * @param machines  the machines the log names, the most recent first
 */
public record NetworkEventsPayload(BlockPos hostPos, long revision, boolean unchanged, List<Row> rows,
                                   List<Machine> machines) implements CustomPacketPayload {

    /** The most machines the filter offers. */
    public static final int MAX_MACHINES = 256;
    /** The longest machine name sent. */
    public static final int MAX_NAME = 48;

    public static final CustomPacketPayload.Type<NetworkEventsPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "network_events"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NetworkEventsPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, NetworkEventsPayload::hostPos,
                    ByteBufCodecs.VAR_LONG, NetworkEventsPayload::revision,
                    ByteBufCodecs.BOOL, NetworkEventsPayload::unchanged,
                    Row.STREAM_CODEC.apply(ByteBufCodecs.list(NetworkEventLog.MOST)), NetworkEventsPayload::rows,
                    Machine.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_MACHINES)), NetworkEventsPayload::machines,
                    NetworkEventsPayload::new);

    /* Copied on the way in, so what the client is handed cannot change under it after it arrives. */
    public NetworkEventsPayload {
        rows = List.copyOf(rows);
        machines = List.copyOf(machines);
    }

    /** The answer for a window that already shows the log at {@code revision}. */
    public static NetworkEventsPayload unchanged(final BlockPos hostPos, final long revision) {
        return new NetworkEventsPayload(hostPos, revision, true, List.of(), List.of());
    }

    /** {@code text} cut to what a name is sent as, so a long name never fails the whole payload. */
    public static String clip(final String text) {
        return TextBounds.clip(text, MAX_NAME);
    }

    @Override
    public CustomPacketPayload.Type<NetworkEventsPayload> type() {
        return TYPE;
    }

    /**
     * One event as the log shows it.
     *
     * @param time        the world's game time it happened at, which the period filters by
     * @param dayTime     the world's clock then, which the time column shows
     * @param level       its level, as {@code EventLevel}'s id
     * @param machineName the machine it happened to, or empty for the whole network
     * @param message     what happened
     */
    public record Row(long time, long dayTime, byte level, String machineName, Text message) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Row> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, Row::time,
                ByteBufCodecs.VAR_LONG, Row::dayTime,
                ByteBufCodecs.BYTE, Row::level,
                ByteBufCodecs.stringUtf8(MAX_NAME), Row::machineName,
                TextCodecs.STREAM_CODEC, Row::message,
                Row::new);
    }

    /**
     * A machine the filter by machine offers.
     *
     * @param id   its node's id, which the filter asks by
     * @param name what the log last called it
     */
    public record Machine(String id, String name) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Machine> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(RequestNetworkEventsPayload.MAX_MACHINE), Machine::id,
                ByteBufCodecs.stringUtf8(MAX_NAME), Machine::name,
                Machine::new);
    }
}
