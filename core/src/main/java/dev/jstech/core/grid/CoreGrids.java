/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.grid;

import dev.jstech.core.network.ConnectivityIndex;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.registry.CoreAttachments;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.OptionalLong;
import net.minecraft.server.level.ServerLevel;

/**
 * Where a dimension's grids are found: one of each kind, data's being the data network's own, and the numbers they
 * know their places by. A cable or a device puts itself in and takes itself out through here, so a wire of data takes
 * the identity of the network it joins as any other data cable does.
 */
public final class CoreGrids {

    private CoreGrids() {
    }

    /** The grid of {@code kind} in {@code level}. */
    public static Grid of(final ServerLevel level, final GridKind kind) {
        if (kind.carriesNetwork()) {
            return NetworkSystem.get(level).connectivity().grid();
        }
        return level.getData(CoreAttachments.GRIDS).of(kind);
    }

    /** The numbers every grid of {@code level} knows its places by. */
    public static GridPlaces places(final ServerLevel level) {
        return level.getData(CoreAttachments.GRIDS).places();
    }

    /**
     * Puts {@code member} in the grid of {@code kind} at {@code place}, joined to those of {@code neighbours} already
     * in that it joins; nothing changes when the place is in already.
     */
    public static void place(final ServerLevel level, final GridKind kind, final GridPlace place,
                             final GridMember member, final Collection<GridPlace> neighbours) {
        final GridPlaces places = places(level);
        final long number = places.number(place);
        if (of(level, kind).contains(number)) {
            return;
        }
        final List<Long> joined = new ArrayList<>(neighbours.size());
        for (final GridPlace neighbour : neighbours) {
            places.find(neighbour).ifPresent(joined::add);
        }
        if (kind.carriesNetwork()) {
            NetworkSystem.get(level).connectivity().place(number, joined, member);
        } else {
            of(level, kind).place(number, member, joined);
        }
    }

    /** Takes {@code place} out of the grid of {@code kind}, and forgets its number; nothing when it was not in. */
    public static void remove(final ServerLevel level, final GridKind kind, final GridPlace place) {
        final GridPlaces places = places(level);
        final OptionalLong number = places.find(place);
        if (number.isEmpty()) {
            return;
        }
        if (kind.carriesNetwork()) {
            final ConnectivityIndex index = NetworkSystem.get(level).connectivity();
            index.onCableRemovedIfRegistered(number.getAsLong());
        } else {
            final Grid grid = of(level, kind);
            if (grid.contains(number.getAsLong())) {
                grid.remove(number.getAsLong());
            }
        }
        places.forget(place);
    }

    /** Whether {@code place} is in the grid of {@code kind}. */
    public static boolean contains(final ServerLevel level, final GridKind kind, final GridPlace place) {
        final OptionalLong number = places(level).find(place);
        return number.isPresent() && of(level, kind).contains(number.getAsLong());
    }
}
