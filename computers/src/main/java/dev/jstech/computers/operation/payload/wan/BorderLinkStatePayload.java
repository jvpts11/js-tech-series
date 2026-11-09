/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.wan;

import dev.jstech.computers.blockentity.WanGatewayBlockEntity;
import dev.jstech.computers.operation.payload.NetworkEventsPayload;
import dev.jstech.computers.wan.PortSettings;
import dev.jstech.computers.wan.WanMode;
import dev.jstech.core.jstechseries.network.DataLink;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Server to client: what Nouvell BorderLink shows of its gateway, each of its four ports' line, far end, mode, state
 * and switches, and the newest lines the gateway wrote into its network's log.
 *
 * @param hostPos the gateway
 * @param name    the gateway's name
 * @param version the version of BorderLink it runs
 * @param ports   its four ports, in order
 * @param log     the gateway's own events, newest first, at most {@link #LOG_ROWS}
 */
public record BorderLinkStatePayload(BlockPos hostPos, String name, String version, List<Port> ports,
                                     List<NetworkEventsPayload.Row> log) implements CustomPacketPayload {

    /** How many of the gateway's own events go with its state. */
    public static final int LOG_ROWS = 24;
    public static final CustomPacketPayload.Type<BorderLinkStatePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "borderlink_state"));
    private static final int MOST_PORTS = 4;
    private static final int NAME = 64;
    private static final int SHORT = 32;

    public static final StreamCodec<RegistryFriendlyByteBuf, BorderLinkStatePayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, BorderLinkStatePayload::hostPos,
                    ByteBufCodecs.stringUtf8(NAME), BorderLinkStatePayload::name,
                    ByteBufCodecs.stringUtf8(SHORT), BorderLinkStatePayload::version,
                    Port.STREAM_CODEC.apply(ByteBufCodecs.list(MOST_PORTS)), BorderLinkStatePayload::ports,
                    NetworkEventsPayload.Row.STREAM_CODEC.apply(ByteBufCodecs.list(LOG_ROWS)),
                    BorderLinkStatePayload::log,
                    BorderLinkStatePayload::new);

    public BorderLinkStatePayload {
        ports = List.copyOf(ports);
        log = List.copyOf(log);
    }

    /** How many of the ports with a card have their line up. */
    public int linesUp() {
        int up = 0;
        for (final Port port : this.ports) {
            if (port.up()) {
                up++;
            }
        }
        return up;
    }

    /** How many of the ports have a card. */
    public int cards() {
        int cards = 0;
        for (final Port port : this.ports) {
            if (port.hasCard()) {
                cards++;
            }
        }
        return cards;
    }

    @Override
    public CustomPacketPayload.Type<BorderLinkStatePayload> type() {
        return TYPE;
    }

    /**
     * One port.
     *
     * @param state   its line: none (no card), up or down, as the gateway's lamps say it
     * @param mode    what its line does, or last did while it is down, the {@code WanMode} by its id
     * @param line    the line plugged in, or the card's own when none is, by its name; empty with no card
     * @param far     the gateway at the far end, or the one last seen there
     * @param network the far network's short id, in a federation
     * @param bits    the port's switches
     */
    public record Port(byte state, byte mode, String line, String far, String network, int bits) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Port> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BYTE, Port::state,
                ByteBufCodecs.BYTE, Port::mode,
                ByteBufCodecs.stringUtf8(SHORT), Port::line,
                ByteBufCodecs.stringUtf8(NAME), Port::far,
                ByteBufCodecs.stringUtf8(SHORT), Port::network,
                ByteBufCodecs.VAR_INT, Port::bits,
                Port::new);

        /** Whether the port has a card. */
        public boolean hasCard() {
            return state != WanGatewayBlockEntity.LINE_NONE;
        }

        /** Whether the port's line is up. */
        public boolean up() {
            return state == WanGatewayBlockEntity.LINE_UP;
        }

        /** The port's mode, NONE for an id that names none. */
        public WanMode wanMode() {
            return WanMode.byId(mode);
        }

        /** The line in the port, or null for a name that names none. */
        @Nullable
        public DataLink link() {
            return DataLink.byName(line);
        }

        /** Items a tick the line carries. */
        public long speed() {
            final DataLink link = link();
            return link == null ? 0L : link.throughput();
        }

        /** The port's switches. */
        public PortSettings settings() {
            return PortSettings.of(bits);
        }
    }
}
