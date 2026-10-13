/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.persistence;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.JsCore;
import dev.jstech.core.state.CoreState;
import dev.jstech.core.state.DimensionState;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NetworkUuidState;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * Which data networks exist in each dimension, and whether each is running, orphaned or in conflict: a state of each
 * dimension, kept in the file it has always been kept in.
 *
 * <p>A file from before the state had versions held a list of networks under {@code networks}; the oldest held only
 * their ids, every one of them running. Both are read by the first step of the layout.
 */
public final class NetworkRegistry {

    /** Where a file from before versions held its list. */
    private static final String OLD_NETWORKS = "networks";
    private static final String KEY_UUID = "uuid";
    private static final String KEY_STATE = "state";

    private static final Codec<NetworkUuidState> NETWORK_STATE = Codec.BYTE.xmap(
            id -> NetworkUuidState.byId(id), state -> (byte) state.id());
    private static final Codec<Entry> ENTRY = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.STRING_CODEC.xmap(NetworkUuid::new, NetworkUuid::value).fieldOf(KEY_UUID)
                    .forGetter(Entry::network),
            NETWORK_STATE.fieldOf(KEY_STATE).forGetter(Entry::state)
    ).apply(instance, Entry::new));
    private static final Codec<NetworkRegistryState> CODEC = ENTRY.listOf().xmap(NetworkRegistry::fromEntries,
            NetworkRegistry::toEntries);

    /** The networks of each dimension. */
    public static final DimensionState<NetworkRegistryState> NETWORKS = CoreState.builder(
                    ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "network_registry"), CODEC,
                    NetworkRegistryState.empty())
            .fileName("jstech_network_registry")
            .upgrade(0, NetworkRegistry::fromBeforeVersions)
            .dimension();

    private NetworkRegistry() {
    }

    /** The networks of {@code level}. */
    public static NetworkRegistryState of(final ServerLevel level) {
        return NETWORKS.get(level);
    }

    /** What {@code network} is in {@code level}, or null when the dimension does not know it. */
    public static @Nullable NetworkUuidState networkState(final ServerLevel level, final NetworkUuid network) {
        return of(level).stateOf(network);
    }

    /** Writes {@code network} down as running in {@code level}, unless it is already known there. */
    public static void addNetwork(final ServerLevel level, final NetworkUuid network) {
        NETWORKS.update(level, networks -> networks.withNetwork(network));
    }

    public static void setNetworkState(final ServerLevel level, final NetworkUuid network,
                                       final NetworkUuidState state) {
        NETWORKS.update(level, networks -> networks.withState(network, state));
    }

    public static void removeNetwork(final ServerLevel level, final NetworkUuid network) {
        NETWORKS.update(level, networks -> networks.withoutNetwork(network));
    }

    private static NetworkRegistryState fromEntries(final List<Entry> entries) {
        final Map<NetworkUuid, NetworkUuidState> states = new LinkedHashMap<>();
        for (final Entry entry : entries) {
            states.put(entry.network(), entry.state());
        }
        return NetworkRegistryState.ofStates(states);
    }

    private static List<Entry> toEntries(final NetworkRegistryState networks) {
        final List<Entry> entries = new ArrayList<>(networks.size());
        networks.states().forEach((network, state) -> entries.add(new Entry(network, state)));
        return entries;
    }

    /*
     * A file from before versions: its list of networks, which is what the layout holds now; or, from the oldest
     * files, a list of ids alone, each one a network that was running.
     */
    private static Tag fromBeforeVersions(final Tag before) {
        if (!(before instanceof CompoundTag old)) {
            return new ListTag();
        }
        final ListTag entries = old.getList(OLD_NETWORKS, Tag.TAG_COMPOUND);
        if (!entries.isEmpty()) {
            return entries;
        }
        final ListTag converted = new ListTag();
        for (final Tag id : old.getList(OLD_NETWORKS, Tag.TAG_STRING)) {
            final CompoundTag entry = new CompoundTag();
            entry.putString(KEY_UUID, id.getAsString());
            entry.putByte(KEY_STATE, (byte) NetworkUuidState.ACTIVE.id());
            converted.add(entry);
        }
        return converted;
    }

    /**
     * One network and what it is.
     *
     * @param network the network
     * @param state   whether it is running, orphaned or in conflict
     */
    private record Entry(NetworkUuid network, NetworkUuidState state) {
    }
}
