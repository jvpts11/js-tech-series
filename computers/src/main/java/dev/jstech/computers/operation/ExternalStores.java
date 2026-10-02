/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation;

import dev.jstech.computers.block.part.ExternalStorageBusPart;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NodeUuid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Where each level's External Storage Buses are, so the network can count their inventories as its storage without
 * walking its cables. A bus says where it is as it ticks; one that is gone is forgotten the first time it is looked for
 * and not found. A bus in a chunk that is not loaded is skipped, never loaded to be asked.
 */
public final class ExternalStores {

    /* Each level's buses: the node each keeps the inventory under, and the cable face it sits on. */
    private static final Map<ServerLevel, Map<NodeUuid, Mount>> MOUNTS = new WeakHashMap<>();

    private ExternalStores() {
    }

    /** Where an External Storage Bus sits. */
    private record Mount(BlockPos cable, Direction face) {
    }

    /** Notes that the bus keeping {@code node} sits on {@code face} of the cable at {@code cable}. */
    public static void register(final ServerLevel level, final NodeUuid node, final BlockPos cable,
                                final Direction face) {
        MOUNTS.computeIfAbsent(level, l -> new LinkedHashMap<>()).put(node, new Mount(cable.immutable(), face));
    }

    /** The External Storage Buses on {@code network} now, in the order they were first seen. */
    public static List<ExternalStorageBusPart> of(final ServerLevel level, @Nullable final NetworkUuid network) {
        final List<ExternalStorageBusPart> found = new ArrayList<>();
        final Map<NodeUuid, Mount> mounts = MOUNTS.get(level);
        if (network == null || mounts == null) {
            return found;
        }
        for (final NodeUuid node : new ArrayList<>(mounts.keySet())) {
            final ExternalStorageBusPart bus = resolve(level, mounts, node);
            if (bus != null && network.equals(bus.networkNow())) {
                found.add(bus);
            }
        }
        return found;
    }

    /** The External Storage Bus keeping {@code node}, or null when the node is no such bus. */
    @Nullable
    public static ExternalStorageBusPart find(final ServerLevel level, final NodeUuid node) {
        final Map<NodeUuid, Mount> mounts = MOUNTS.get(level);
        return mounts == null || !mounts.containsKey(node) ? null : resolve(level, mounts, node);
    }

    /** Whether {@code node} is an External Storage Bus that will not take {@code key} in. */
    public static boolean refuses(final ServerLevel level, final NodeUuid node, final StorageKey key) {
        final ExternalStorageBusPart bus = find(level, node);
        return bus != null && !bus.takesIn(key);
    }

    /* The bus a mount points at, if it is still there; a mount whose bus is gone is forgotten. */
    @Nullable
    private static ExternalStorageBusPart resolve(final ServerLevel level, final Map<NodeUuid, Mount> mounts,
                                                  final NodeUuid node) {
        final Mount mount = mounts.get(node);
        if (mount == null || !level.isLoaded(mount.cable())) {
            return null;
        }
        if (level.getBlockEntity(mount.cable()) instanceof CableBlockEntity cable
                && cable.getPart(mount.face()) instanceof ExternalStorageBusPart bus && bus.node().equals(node)) {
            return bus;
        }
        mounts.remove(node);
        return null;
    }
}
