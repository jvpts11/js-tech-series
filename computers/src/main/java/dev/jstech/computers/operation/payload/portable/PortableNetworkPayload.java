/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.portable;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.operation.payload.CraftCatalogPayload;
import dev.jstech.computers.operation.payload.NetworkItemEntry;
import dev.jstech.computers.operation.payload.OperationRecord;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: what a portable's network app shows (computers/docs/design/wireless.md), in the Network
 * Interactor's words: the network's stock, what the device's own storage holds and how much room it has left, what
 * the network can craft, and the jobs running on it.
 *
 * @param device   the device's id
 * @param network  the network it is on, by its short id; empty when it is on none
 * @param online   whether the network's Mainframe answers
 * @param types    how many kinds of thing the network holds, some of which may not be in {@code stock}
 * @param stock    the network's stock, as much of it as a list carries
 * @param stored   what the device's own storage holds
 * @param free     how many whole items more the device's storage takes
 * @param capacity how many items the device's storage holds
 * @param crafts   what the network can craft
 * @param jobs     the jobs running on the network, the latest few
 */
public record PortableNetworkPayload(UUID device, String network, boolean online, int types,
                                     List<NetworkItemEntry> stock, List<NetworkItemEntry> stored, long free,
                                     long capacity, List<CraftCatalogPayload.Entry> crafts,
                                     List<OperationRecord> jobs) implements CustomPacketPayload {

    /** The most kinds a stock list carries. */
    public static final int MOST_STOCK = 512;
    /** The most kinds the device's storage lists; it holds at most about a thousand items. */
    public static final int MOST_STORED = 1_024;
    /** The most crafts listed. */
    public static final int MOST_CRAFTS = 256;
    /** The most jobs listed. */
    public static final int MOST_JOBS = 6;
    private static final int MOST_NETWORK = 16;

    public static final CustomPacketPayload.Type<PortableNetworkPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
                    "portable_network"));

    private static final StreamCodec<RegistryFriendlyByteBuf, List<NetworkItemEntry>> STOCK_CODEC =
            NetworkItemEntry.STREAM_CODEC.apply(ByteBufCodecs.list(MOST_STOCK));
    private static final StreamCodec<RegistryFriendlyByteBuf, List<NetworkItemEntry>> STORED_CODEC =
            NetworkItemEntry.STREAM_CODEC.apply(ByteBufCodecs.list(MOST_STORED));
    private static final StreamCodec<RegistryFriendlyByteBuf, List<CraftCatalogPayload.Entry>> CRAFTS_CODEC =
            CraftCatalogPayload.Entry.STREAM_CODEC.apply(ByteBufCodecs.list(MOST_CRAFTS));
    private static final StreamCodec<RegistryFriendlyByteBuf, List<OperationRecord>> JOBS_CODEC =
            OperationRecord.STREAM_CODEC.apply(ByteBufCodecs.list(MOST_JOBS));

    public static final StreamCodec<RegistryFriendlyByteBuf, PortableNetworkPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                UUIDUtil.STREAM_CODEC.encode(buf, p.device());
                buf.writeUtf(p.network(), MOST_NETWORK);
                buf.writeBoolean(p.online());
                buf.writeVarInt(p.types());
                STOCK_CODEC.encode(buf, p.stock());
                STORED_CODEC.encode(buf, p.stored());
                buf.writeVarLong(p.free());
                buf.writeVarLong(p.capacity());
                CRAFTS_CODEC.encode(buf, p.crafts());
                JOBS_CODEC.encode(buf, p.jobs());
            },
            buf -> new PortableNetworkPayload(UUIDUtil.STREAM_CODEC.decode(buf), buf.readUtf(MOST_NETWORK),
                    buf.readBoolean(), buf.readVarInt(), STOCK_CODEC.decode(buf), STORED_CODEC.decode(buf),
                    buf.readVarLong(), buf.readVarLong(), CRAFTS_CODEC.decode(buf), JOBS_CODEC.decode(buf)));

    public PortableNetworkPayload {
        stock = List.copyOf(stock.subList(0, Math.min(MOST_STOCK, stock.size())));
        stored = List.copyOf(stored.subList(0, Math.min(MOST_STORED, stored.size())));
        crafts = List.copyOf(crafts.subList(0, Math.min(MOST_CRAFTS, crafts.size())));
        jobs = List.copyOf(jobs.subList(0, Math.min(MOST_JOBS, jobs.size())));
    }

    @Override
    public CustomPacketPayload.Type<PortableNetworkPayload> type() {
        return TYPE;
    }
}
