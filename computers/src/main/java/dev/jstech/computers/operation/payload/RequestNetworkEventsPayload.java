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
 * Client to server: the Network Manager on the Mainframe at {@code hostPos} asks what its network's event log shows
 * through the filters it has set. The server replies with a {@link NetworkEventsPayload}, a short one when the log is
 * still at {@code known}.
 *
 * @param hostPos the Mainframe the Network Manager runs on
 * @param level   the one level shown, as {@code EventLevel}'s id, or {@link #ANY_LEVEL}
 * @param machine the one machine shown, by its node's id, {@link #ANY_MACHINE} or {@link #WHOLE_NETWORK}
 * @param period  how far back it shows, one of the {@code PERIOD_} numbers
 * @param known   the log's revision the window already shows, or {@link #NOTHING_KNOWN}
 */
public record RequestNetworkEventsPayload(BlockPos hostPos, byte level, String machine, byte period, long known)
        implements CustomPacketPayload {

    /** Every level. */
    public static final byte ANY_LEVEL = -1;
    /** Every machine, and the whole network's own events. */
    public static final String ANY_MACHINE = "";
    /** Only the events of the whole network, those of no machine. */
    public static final String WHOLE_NETWORK = "*";
    /** The longest machine filter sent: a node's id. */
    public static final int MAX_MACHINE = 40;

    public static final byte PERIOD_HOUR = 0;
    public static final byte PERIOD_DAY = 1;
    public static final byte PERIOD_WEEK = 2;
    public static final byte PERIOD_ALL = 3;

    /** What a window that shows nothing of the log yet says it knows. */
    public static final long NOTHING_KNOWN = -1L;

    public static final CustomPacketPayload.Type<RequestNetworkEventsPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "request_network_events"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestNetworkEventsPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, RequestNetworkEventsPayload::hostPos,
                    ByteBufCodecs.BYTE, RequestNetworkEventsPayload::level,
                    ByteBufCodecs.stringUtf8(MAX_MACHINE), RequestNetworkEventsPayload::machine,
                    ByteBufCodecs.BYTE, RequestNetworkEventsPayload::period,
                    ByteBufCodecs.VAR_LONG, RequestNetworkEventsPayload::known,
                    RequestNetworkEventsPayload::new);

    @Override
    public CustomPacketPayload.Type<RequestNetworkEventsPayload> type() {
        return TYPE;
    }

    /** How far back {@code period} reaches, in ticks of the world's clock; every event for {@link #PERIOD_ALL}. */
    public static long ticksOf(final byte period) {
        return switch (period) {
            case PERIOD_HOUR -> 1_000L;
            case PERIOD_DAY -> 24_000L;
            case PERIOD_WEEK -> 7L * 24_000L;
            default -> Long.MAX_VALUE;
        };
    }
}
