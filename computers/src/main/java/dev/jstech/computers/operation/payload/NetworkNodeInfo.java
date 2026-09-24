/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * One node in the Mainframe's network overview. Beyond the kind, short id, a pre-formatted detail metric,
 * and the online flag, it carries the enrichment the Network Manager's Devices list and Map tooltip show:
 * the computer's custom name (empty when unnamed), its processor clock, graphics memory, free and total
 * storage, the public-share permille of its system disk ({@code -1} when not applicable, e.g. a server or
 * subframe with no directly readable disk), and its installed OS label.
 */
@TextHolder
public record NetworkNodeInfo(int kind, String id, String name, String detail, boolean online,
                              int cpuMhz, int vramMb, long storageFreeMb, long storageTotalMb,
                              int publicPermille, String osLabel) {

    // What kind of machine a node is, as the Network Manager tags it.
    private static final TextKey MAINFRAME = TextKey.of("jsc.network_node.mainframe", "MAINFRAME");
    private static final TextKey SERVER = TextKey.of("jsc.network_node.server", "SERVER");
    private static final TextKey SUBFRAME = TextKey.of("jsc.network_node.subframe", "SUBFRAME");
    private static final TextKey PC = TextKey.of("jsc.network_node.pc", "PC");
    private static final TextKey CRAFTING = TextKey.of("jsc.network_node.crafting", "CRAFTING");
    private static final TextKey SUPERCOMPUTER = TextKey.of("jsc.network_node.supercomputer", "SUPERCOMPUTER");
    private static final TextKey CLUSTER_MANAGEMENT =
            TextKey.of("jsc.network_node.cluster_management", "CLUSTER MGMT");
    private static final TextKey NODE = TextKey.of("jsc.network_node.node", "NODE");

    public static final int KIND_MAINFRAME = 0;
    public static final int KIND_SERVER = 1;
    public static final int KIND_SUBFRAME = 2;
    public static final int KIND_PC = 3;
    public static final int KIND_CRAFTING = 4;
    public static final int KIND_SUPERCOMPUTER = 5;
    public static final int KIND_CLUSTER_MANAGEMENT = 6;

    /** Sentinel for {@link #publicPermille} when a node has no directly readable disk share. */
    public static final int SHARE_UNKNOWN = -1;

    // Hand-written because the field count is past what StreamCodec.composite overloads accept.
    public static final StreamCodec<RegistryFriendlyByteBuf, NetworkNodeInfo> STREAM_CODEC =
            StreamCodec.of(NetworkNodeInfo::encode, NetworkNodeInfo::decode);

    private static void encode(final RegistryFriendlyByteBuf buf, final NetworkNodeInfo info) {
        buf.writeVarInt(info.kind);
        buf.writeUtf(info.id);
        buf.writeUtf(info.name);
        buf.writeUtf(info.detail);
        buf.writeBoolean(info.online);
        buf.writeVarInt(info.cpuMhz);
        buf.writeVarInt(info.vramMb);
        buf.writeVarLong(info.storageFreeMb);
        buf.writeVarLong(info.storageTotalMb);
        buf.writeVarInt(info.publicPermille);
        buf.writeUtf(info.osLabel);
    }

    private static NetworkNodeInfo decode(final RegistryFriendlyByteBuf buf) {
        final int kind = buf.readVarInt();
        final String id = buf.readUtf();
        final String name = buf.readUtf();
        final String detail = buf.readUtf();
        final boolean online = buf.readBoolean();
        final int cpuMhz = buf.readVarInt();
        final int vramMb = buf.readVarInt();
        final long storageFreeMb = buf.readVarLong();
        final long storageTotalMb = buf.readVarLong();
        final int publicPermille = buf.readVarInt();
        final String osLabel = buf.readUtf();
        return new NetworkNodeInfo(kind, id, name, detail, online, cpuMhz, vramMb,
                storageFreeMb, storageTotalMb, publicPermille, osLabel);
    }

    /** What kind of machine the node is, as a short tag in the player's language. */
    public Text kindLabel() {
        return (switch (kind) {
            case KIND_MAINFRAME -> MAINFRAME;
            case KIND_SERVER -> SERVER;
            case KIND_SUBFRAME -> SUBFRAME;
            case KIND_PC -> PC;
            case KIND_CRAFTING -> CRAFTING;
            case KIND_SUPERCOMPUTER -> SUPERCOMPUTER;
            case KIND_CLUSTER_MANAGEMENT -> CLUSTER_MANAGEMENT;
            default -> NODE;
        }).text();
    }

    /** The name to show, falling back to the short id when the computer has no custom name. */
    public String displayName() {
        return name.isEmpty() ? id : name;
    }
}
