/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Server to client: everything the Gateway Manager shows. The host and whether ComputerCraft is there
 * at all, every Gateway on the host's ports, the selected one in detail (its two sides, its buffer, its
 * permissions, the computers and shares it sees, its log), and a status line for the last action.
 */
public record GatewayManagerStatePayload(Head head, List<WireGateway> gateways, Detail detail, String status)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<GatewayManagerStatePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "gateway_manager_state"));

    public static final int MAX_GATEWAYS = 16;
    public static final int MAX_ROWS = 64;
    public static final int BUFFER_SLOTS = 9;

    /** The host: its name, whether CC: Tweaked is installed and which, and the fleet's load. */
    public record Head(String hostName, boolean ccInstalled, String ccVersion, int callsThisMinute, int ccReachable) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Head> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(64), Head::hostName,
                ByteBufCodecs.BOOL, Head::ccInstalled,
                ByteBufCodecs.stringUtf8(32), Head::ccVersion,
                ByteBufCodecs.VAR_INT, Head::callsThisMinute,
                ByteBufCodecs.VAR_INT, Head::ccReachable,
                Head::new);
    }

    /** One Gateway in the rail: where it stands and how it is linked, with its two lights. */
    public record WireGateway(long pos, String name, String where, boolean linked, boolean ccLinked) {
        public static final StreamCodec<RegistryFriendlyByteBuf, WireGateway> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, WireGateway::pos,
                ByteBufCodecs.stringUtf8(32), WireGateway::name,
                ByteBufCodecs.stringUtf8(64), WireGateway::where,
                ByteBufCodecs.BOOL, WireGateway::linked,
                ByteBufCodecs.BOOL, WireGateway::ccLinked,
                WireGateway::new);
    }

    /** One line of the log: when, who, what, how it went, and the colour of the result. */
    public record WireLog(String when, String who, String what, String result, int tone) {
        public static final StreamCodec<RegistryFriendlyByteBuf, WireLog> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(16), WireLog::when,
                ByteBufCodecs.stringUtf8(48), WireLog::who,
                ByteBufCodecs.stringUtf8(96), WireLog::what,
                ByteBufCodecs.stringUtf8(64), WireLog::result,
                ByteBufCodecs.VAR_INT, WireLog::tone,
                WireLog::new);
    }

    /** One ComputerCraft computer the Gateway knows. */
    public record WireComputer(int id, String label, boolean on, boolean agent, String lastSeen) {
        public static final StreamCodec<RegistryFriendlyByteBuf, WireComputer> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, WireComputer::id,
                ByteBufCodecs.stringUtf8(48), WireComputer::label,
                ByteBufCodecs.BOOL, WireComputer::on,
                ByteBufCodecs.BOOL, WireComputer::agent,
                ByteBufCodecs.stringUtf8(24), WireComputer::lastSeen,
                WireComputer::new);
    }

    /** One shared folder as ComputerCraft will see it through this Gateway. */
    public record WireShare(String computer, String share, String onCc, String mode) {
        public static final StreamCodec<RegistryFriendlyByteBuf, WireShare> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(64), WireShare::computer,
                ByteBufCodecs.stringUtf8(128), WireShare::share,
                ByteBufCodecs.stringUtf8(160), WireShare::onCc,
                ByteBufCodecs.stringUtf8(16), WireShare::mode,
                WireShare::new);
    }

    /**
     * The selected Gateway in full: this side (host, link, network, budget), the ComputerCraft side
     * (network, agents, what was served), the names, the buffer, the last requests, the permissions, and
     * the three tables.
     */
    public record Detail(long pos, String name, String link, int types, int servers, boolean mainframeOnline,
                         int budgetPermille, boolean ccOnline, int wiredComputers, int wiredDevices, int agents,
                         int agentsTotal, int calls, int operations, int files, String peripheralName, int rednetId,
                         List<ItemStack> buffer, List<WireLog> recent, boolean read, boolean operationsAllowed,
                         int filesAccess, int ceiling, int cap, List<WireComputer> computers, List<WireShare> shares,
                         List<WireLog> log) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Detail> STREAM_CODEC = StreamCodec.of(
                (buf, d) -> {
                    buf.writeVarLong(d.pos()).writeUtf(d.name(), 32).writeUtf(d.link(), 48)
                            .writeVarInt(d.types()).writeVarInt(d.servers()).writeBoolean(d.mainframeOnline())
                            .writeVarInt(d.budgetPermille()).writeBoolean(d.ccOnline())
                            .writeVarInt(d.wiredComputers()).writeVarInt(d.wiredDevices())
                            .writeVarInt(d.agents()).writeVarInt(d.agentsTotal())
                            .writeVarInt(d.calls()).writeVarInt(d.operations()).writeVarInt(d.files())
                            .writeUtf(d.peripheralName(), 48).writeVarInt(d.rednetId());
                    ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(BUFFER_SLOTS)).encode(buf, d.buffer());
                    WireLog.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_ROWS)).encode(buf, d.recent());
                    buf.writeBoolean(d.read()).writeBoolean(d.operationsAllowed()).writeVarInt(d.filesAccess())
                            .writeVarInt(d.ceiling()).writeVarInt(d.cap());
                    WireComputer.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_ROWS)).encode(buf, d.computers());
                    WireShare.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_ROWS)).encode(buf, d.shares());
                    WireLog.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_ROWS)).encode(buf, d.log());
                },
                buf -> {
                    final long pos = buf.readVarLong();
                    final String name = buf.readUtf(32);
                    final String link = buf.readUtf(48);
                    final int types = buf.readVarInt();
                    final int servers = buf.readVarInt();
                    final boolean mainframeOnline = buf.readBoolean();
                    final int budget = buf.readVarInt();
                    final boolean ccOnline = buf.readBoolean();
                    final int wiredComputers = buf.readVarInt();
                    final int wiredDevices = buf.readVarInt();
                    final int agents = buf.readVarInt();
                    final int agentsTotal = buf.readVarInt();
                    final int calls = buf.readVarInt();
                    final int operations = buf.readVarInt();
                    final int files = buf.readVarInt();
                    final String peripheralName = buf.readUtf(48);
                    final int rednetId = buf.readVarInt();
                    final List<ItemStack> buffer =
                            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(BUFFER_SLOTS)).decode(buf);
                    final List<WireLog> recent = WireLog.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_ROWS)).decode(buf);
                    final boolean read = buf.readBoolean();
                    final boolean operationsAllowed = buf.readBoolean();
                    final int filesAccess = buf.readVarInt();
                    final int ceiling = buf.readVarInt();
                    final int cap = buf.readVarInt();
                    final List<WireComputer> computers =
                            WireComputer.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_ROWS)).decode(buf);
                    final List<WireShare> shares = WireShare.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_ROWS)).decode(buf);
                    final List<WireLog> log = WireLog.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_ROWS)).decode(buf);
                    return new Detail(pos, name, link, types, servers, mainframeOnline, budget, ccOnline, wiredComputers,
                            wiredDevices, agents, agentsTotal, calls, operations, files, peripheralName, rednetId, buffer,
                            recent, read, operationsAllowed, filesAccess, ceiling, cap, computers, shares, log);
                });

        /** No Gateway selected. */
        public static Detail none() {
            return new Detail(0L, "", "", 0, 0, false, 0, false, 0, 0, 0, 0, 0, 0, 0, "", -1, emptyBuffer(),
                    List.of(), true, true, 1, 1, 1, List.of(), List.of(), List.of());
        }

        public boolean present() {
            return pos != 0L;
        }

        private static List<ItemStack> emptyBuffer() {
            final List<ItemStack> empty = new ArrayList<>(BUFFER_SLOTS);
            for (int i = 0; i < BUFFER_SLOTS; i++) {
                empty.add(ItemStack.EMPTY);
            }
            return empty;
        }
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, GatewayManagerStatePayload> STREAM_CODEC =
            StreamCodec.composite(
                    Head.STREAM_CODEC, GatewayManagerStatePayload::head,
                    WireGateway.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_GATEWAYS)), GatewayManagerStatePayload::gateways,
                    Detail.STREAM_CODEC, GatewayManagerStatePayload::detail,
                    ByteBufCodecs.stringUtf8(160), GatewayManagerStatePayload::status,
                    GatewayManagerStatePayload::new);

    @Override
    public CustomPacketPayload.Type<GatewayManagerStatePayload> type() {
        return TYPE;
    }
}
