/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.backup;

import dev.jstech.computers.blockentity.RackUnitHost;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.eventlog.NetworkEvents;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/**
 * The book of losses of every data network of a world, kept with the world: what items stopped existing on each
 * network's servers, how many, when and how, so a backup can give them back (computers/docs/design/programs.md).
 *
 * <p>Only a rack server's storage is the network's own, so only what is lost there is written down: a personal
 * computer's disks hold its own items beside the share it lends the network, and giving those back from a backup of
 * the network would make items that never were the network's.
 */
public final class NetworkLosses extends SavedData {

    private final Map<NetworkUuid, LossBook<StorageKey>> books = new HashMap<>();

    private static final String FILE = "jsc_network_losses";
    private static final String NETWORKS = "networks";
    private static final String NETWORK = "network";
    private static final String LOSSES = "losses";
    private static final String ID = "id";
    private static final String KEY = "key";
    private static final String COUNT = "count";
    private static final String RESTORED = "restored";
    private static final String AT = "at";
    private static final String CLOCK = "clock";
    private static final String CAUSE = "cause";
    private static final String MACHINE = "machine";
    private static final String DISK = "disk";

    /** The books of the world {@code level} is in, kept with its overworld as the storage volumes are. */
    public static NetworkLosses of(final ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(factory(), FILE);
    }

    /** How a world makes the books afresh and reads them back from its save. */
    public static SavedData.Factory<NetworkLosses> factory() {
        return new SavedData.Factory<>(NetworkLosses::new, NetworkLosses::load);
    }

    /** {@code network}'s book, an empty one for a network that never lost anything. */
    public LossBook<StorageKey> book(final NetworkUuid network) {
        return books.computeIfAbsent(network, any -> new LossBook<>());
    }

    /** Marks the books changed, after a backup was restored from one. */
    public void changed() {
        setDirty();
    }

    /**
     * Writes down what {@code host} lost, when it is a server of a network: each kind in {@code lost}, by
     * {@code cause}, on its disk {@code disk} (-1 for none in particular). Anything else loses nothing of the
     * network's.
     */
    public static void lost(final ServerLevel level, final IOsHost host, final Map<StorageKey, Long> lost,
                            final LossCause cause, final int disk) {
        final boolean inRack = host instanceof ServerRackBlockEntity || host instanceof RackUnitHost;
        if (!inRack || host.nodeUuid() == null) {
            return;
        }
        lost(level, host.networkUuid(), lost, cause,
                NetworkEvents.machineName(host.customName(), host.machine(), host.nodeUuid()), disk);
    }

    /** Writes down what {@code network} lost, by {@code cause}, on {@code machine}'s disk {@code disk}. */
    public static void lost(final ServerLevel level, @Nullable final NetworkUuid network,
                            final Map<StorageKey, Long> lost, final LossCause cause, final String machine,
                            final int disk) {
        if (network == null || lost.isEmpty()) {
            return;
        }
        final NetworkLosses losses = of(level);
        final LossBook<StorageKey> book = losses.book(network);
        for (final Map.Entry<StorageKey, Long> each : lost.entrySet()) {
            book.lose(each.getKey(), each.getValue(), level.getGameTime(), level.getDayTime(), cause,
                    machine == null ? "" : machine, disk);
        }
        losses.setDirty();
    }

    @Override
    public CompoundTag save(final CompoundTag tag, final HolderLookup.Provider registries) {
        final RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registries);
        final ListTag networks = new ListTag();
        for (final Map.Entry<NetworkUuid, LossBook<StorageKey>> entry : books.entrySet()) {
            final ListTag losses = new ListTag();
            for (final LossBook.Loss<StorageKey> loss : entry.getValue().all()) {
                final Optional<Tag> key = StorageKey.CODEC.encodeStart(ops, loss.key()).result();
                if (key.isEmpty()) {
                    continue;
                }
                final CompoundTag one = new CompoundTag();
                one.putLong(ID, loss.id());
                one.put(KEY, key.get());
                one.putLong(COUNT, loss.count());
                one.putLong(RESTORED, loss.restored());
                one.putLong(AT, loss.at());
                one.putLong(CLOCK, loss.clock());
                one.putString(CAUSE, loss.cause().serializedName());
                one.putString(MACHINE, loss.machine());
                one.putInt(DISK, loss.disk());
                losses.add(one);
            }
            final CompoundTag network = new CompoundTag();
            network.putString(NETWORK, entry.getKey().asString());
            network.put(LOSSES, losses);
            networks.add(network);
        }
        tag.put(NETWORKS, networks);
        return tag;
    }

    private static NetworkLosses load(final CompoundTag tag, final HolderLookup.Provider registries) {
        final RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registries);
        final NetworkLosses losses = new NetworkLosses();
        for (final Tag saved : tag.getList(NETWORKS, Tag.TAG_COMPOUND)) {
            final CompoundTag network = (CompoundTag) saved;
            final Optional<NetworkUuid> id = NetworkUuid.parse(network.getString(NETWORK));
            if (id.isEmpty()) {
                continue;
            }
            final List<LossBook.Loss<StorageKey>> kept = new ArrayList<>();
            for (final Tag oneTag : network.getList(LOSSES, Tag.TAG_COMPOUND)) {
                final CompoundTag one = (CompoundTag) oneTag;
                final Optional<StorageKey> key = StorageKey.CODEC.parse(ops, one.get(KEY)).result();
                final LossCause cause = LossCause.byName(one.getString(CAUSE));
                if (key.isPresent() && cause != null) {
                    kept.add(new LossBook.Loss<>(one.getLong(ID), key.get(), one.getLong(COUNT),
                            one.getLong(RESTORED), one.getLong(AT), one.getLong(CLOCK), cause,
                            one.getString(MACHINE), one.getInt(DISK)));
                }
            }
            losses.book(id.get()).restoreAll(kept);
        }
        return losses;
    }
}
