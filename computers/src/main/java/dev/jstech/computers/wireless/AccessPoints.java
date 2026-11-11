/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.wireless;

import dev.jstech.computers.blockentity.AccessPointBlockEntity;
import dev.jstech.core.util.Loaded;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The access points of each level that are loaded, so a computer with a Wi-Fi card finds the one it joins without
 * looking at the world round it: there are a handful in a world, and each says how far it reaches.
 */
public final class AccessPoints {

    private static final Map<ServerLevel, Set<BlockPos>> BY_LEVEL = new WeakHashMap<>();

    private AccessPoints() {
    }

    /** An access point at {@code pos} came into the world, or was loaded with its chunk. */
    public static void add(final ServerLevel level, final BlockPos pos) {
        BY_LEVEL.computeIfAbsent(level, any -> new LinkedHashSet<>()).add(pos.immutable());
    }

    /** The access point at {@code pos} was taken away, or unloaded with its chunk. */
    public static void remove(final ServerLevel level, final BlockPos pos) {
        final Set<BlockPos> known = BY_LEVEL.get(level);
        if (known != null) {
            known.remove(pos);
        }
    }

    /**
     * The access point a device at {@code pos} joins: of those on a network with their energy that reach it, the
     * nearest, as a real device joins the strongest signal. Null when none reaches it.
     */
    @Nullable
    public static AccessPointBlockEntity nearest(final ServerLevel level, final BlockPos pos) {
        AccessPointBlockEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        // Measured where each stands in the world, a ship's access point where its ship flies now.
        final Vec3 device = AccessPointBlockEntity.inWorld(level, pos);
        for (final AccessPointBlockEntity point : loaded(level)) {
            final double distance = AccessPointBlockEntity.inWorld(level, point.getBlockPos()).distanceToSqr(device);
            if (point.serving() != null && point.reaches(pos) && distance < bestDistance) {
                best = point;
                bestDistance = distance;
            }
        }
        return best;
    }

    /** The access points of that network that are loaded, for its Network Manager to list. */
    public static List<AccessPointBlockEntity> on(final ServerLevel level, final NetworkUuid network) {
        final List<AccessPointBlockEntity> out = new ArrayList<>();
        for (final AccessPointBlockEntity point : loaded(level)) {
            if (network.equals(point.network())) {
                out.add(point);
            }
        }
        return out;
    }

    /* Every access point known in the level that is still there, forgetting a place that no longer holds one. */
    private static List<AccessPointBlockEntity> loaded(final ServerLevel level) {
        final Set<BlockPos> known = BY_LEVEL.get(level);
        if (known == null || known.isEmpty()) {
            return List.of();
        }
        final List<AccessPointBlockEntity> out = new ArrayList<>(known.size());
        known.removeIf(pos -> {
            if (Loaded.blockEntity(level, pos) instanceof AccessPointBlockEntity point && !point.isRemoved()) {
                out.add(point);
                return false;
            }
            return level.isLoaded(pos);
        });
        return out;
    }
}
