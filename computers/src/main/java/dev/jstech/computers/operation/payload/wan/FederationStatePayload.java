/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.wan;

import dev.jstech.computers.operation.payload.NetworkInteractorPayload;
import dev.jstech.computers.operation.payload.NetworkItemEntry;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.wan.PortSettings;
import dev.jstech.computers.wan.WanRight;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Server to client: the networks a computer's network is federated with, for the Network Interactor's Federated tab,
 * and what one of them holds, in a view of its own that is never mixed with this network's.
 *
 * @param hostPos   the computer asked through
 * @param peers     every network this one reaches, the nearest first
 * @param shown     the network whose items follow, by its id; empty when none is shown
 * @param items     what the shown network holds, when it lets this one see it
 * @param craftable what the shown network can craft, when it lets this one craft there
 */
public record FederationStatePayload(BlockPos hostPos, List<Peer> peers, String shown, List<NetworkItemEntry> items,
                                     List<StorageKey> craftable) implements CustomPacketPayload {

    /** How many federated networks are listed at most, and how many of one's craftable items. */
    public static final int MOST_PEERS = 16;
    public static final int MOST_CRAFTABLE = 512;
    public static final CustomPacketPayload.Type<FederationStatePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "federation_state"));
    private static final int ID = 40;
    private static final int NAME = 64;
    private static final int LINE = 32;

    public static final StreamCodec<RegistryFriendlyByteBuf, FederationStatePayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, FederationStatePayload::hostPos,
                    Peer.STREAM_CODEC.apply(ByteBufCodecs.list(MOST_PEERS)), FederationStatePayload::peers,
                    ByteBufCodecs.stringUtf8(ID), FederationStatePayload::shown,
                    NetworkItemEntry.STREAM_CODEC.apply(ByteBufCodecs.list(NetworkInteractorPayload.MAX_ENTRIES)),
                    FederationStatePayload::items,
                    StorageKey.STREAM_CODEC.apply(ByteBufCodecs.list(MOST_CRAFTABLE)),
                    FederationStatePayload::craftable,
                    FederationStatePayload::new);

    public FederationStatePayload {
        peers = List.copyOf(peers);
        items = List.copyOf(items);
        craftable = List.copyOf(craftable);
    }

    /** No federation to show. */
    public static FederationStatePayload none(final BlockPos hostPos) {
        return new FederationStatePayload(hostPos, List.of(), "", List.of(), List.of());
    }

    /**
     * What a grid lists of the network shown: what it holds, then what it can craft and holds none of, at a count of
     * nought, so a thing it can make is there to be crafted.
     */
    public List<NetworkItemEntry> entries() {
        final List<NetworkItemEntry> out = new ArrayList<>(this.items);
        final Set<StorageKey> held = new HashSet<>();
        for (final NetworkItemEntry entry : this.items) {
            held.add(entry.key());
        }
        for (final StorageKey key : this.craftable) {
            if (held.add(key)) {
                out.add(new NetworkItemEntry(key, 0L));
            }
        }
        return out;
    }

    /** The network shown, or null when none is. */
    @Nullable
    public Peer shownPeer() {
        for (final Peer peer : this.peers) {
            if (peer.network().equals(this.shown)) {
                return peer;
            }
        }
        return null;
    }

    /** Where the network shown stands among them, counted from one; 0 when none is shown. */
    public int shownIndex() {
        for (int i = 0; i < this.peers.size(); i++) {
            if (this.peers.get(i).network().equals(this.shown)) {
                return i + 1;
            }
        }
        return 0;
    }

    /** The network after the one shown, by its id, round to the first; the one shown when it is the only one. */
    public String nextNetwork() {
        return this.peers.isEmpty() ? this.shown : this.peers.get(shownIndex() % this.peers.size()).network();
    }

    @Override
    public CustomPacketPayload.Type<FederationStatePayload> type() {
        return TYPE;
    }

    /**
     * One network this one reaches.
     *
     * @param network    its id
     * @param name       what it is called: by the gateway its line ends in
     * @param port       this network's port its line leaves by
     * @param line       the line out of that port, by its link's name
     * @param speed      items a tick the whole way carries, its slowest line
     * @param rights     what this network may do there, as {@link PortSettings} bits
     * @param hops       how many lines the way crosses; past one, networks between relay it
     * @param types      how many types of data it holds
     * @param servers    how many servers it has
     * @param craftables how many things it can craft
     * @param live       how many Operations of this network cross to it now
     * @param usedMb     what its storage holds, in megabytes
     * @param capacityMb how much its storage holds at most, in megabytes
     */
    public record Peer(String network, String name, int port, String line, long speed, int rights, int hops, int types,
                       int servers, int craftables, int live, long usedMb, long capacityMb) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Peer> STREAM_CODEC =
                StreamCodec.of(Peer::write, Peer::read);

        /** Whether this network may do {@code right} there. */
        public boolean allows(final WanRight right) {
            return PortSettings.of(rights).allows(right);
        }

        private static void write(final RegistryFriendlyByteBuf buf, final Peer peer) {
            buf.writeUtf(peer.network(), ID);
            buf.writeUtf(peer.name(), NAME);
            buf.writeVarInt(peer.port());
            buf.writeUtf(peer.line(), LINE);
            buf.writeVarLong(peer.speed());
            buf.writeVarInt(peer.rights());
            buf.writeVarInt(peer.hops());
            buf.writeVarInt(peer.types());
            buf.writeVarInt(peer.servers());
            buf.writeVarInt(peer.craftables());
            buf.writeVarInt(peer.live());
            buf.writeVarLong(peer.usedMb());
            buf.writeVarLong(peer.capacityMb());
        }

        private static Peer read(final RegistryFriendlyByteBuf buf) {
            return new Peer(buf.readUtf(ID), buf.readUtf(NAME), buf.readVarInt(), buf.readUtf(LINE), buf.readVarLong(),
                    buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                    buf.readVarInt(), buf.readVarLong(), buf.readVarLong());
        }
    }
}
