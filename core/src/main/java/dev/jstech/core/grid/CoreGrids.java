/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.grid;

import dev.jstech.core.registry.CoreAttachments;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Where a dimension's grids are found: one of each kind, those that carry networks kept by the networks' own keeper
 * ({@link INetworkGrids}), and the numbers they know their places by. A cable or a device puts itself in and takes
 * itself out through here, so a wire of a network takes the identity of the network it joins as any other cable of it
 * does.
 */
public final class CoreGrids {

    /* What keeps the grids that carry networks, handed in once as the game loads. */
    private static volatile @Nullable INetworkGrids networks;

    private CoreGrids() {
    }

    /** Hands in what keeps the grids that carry networks, once, from the constructor of the mod whose they are. */
    public static void keepNetworksIn(final INetworkGrids keeper) {
        if (networks != null) {
            throw new IllegalStateException("the grids that carry networks are kept already");
        }
        networks = Objects.requireNonNull(keeper, "keeper");
    }

    /** The grid of {@code kind} in {@code level}. */
    public static Grid of(final ServerLevel level, final GridKind kind) {
        if (kind.carriesNetwork()) {
            return networks().grid(level);
        }
        return level.getData(CoreAttachments.GRIDS).of(kind);
    }

    /** The network the place {@code number} of a grid that carries networks reaches, within its runs' reach. */
    public static Optional<NetworkUuid> networkOf(final ServerLevel level, final long number) {
        return networks().reached(level, number);
    }

    /** The network the place {@code number} of a grid that carries networks is joined into, reached or not. */
    public static Optional<NetworkUuid> joinedNetwork(final ServerLevel level, final long number) {
        return networks().joined(level, number);
    }

    /**
     * Joins the place {@code number} of a grid that carries networks into {@code network} again, unless it is in one.
     */
    public static void assignNetwork(final ServerLevel level, final long number, final NetworkUuid network) {
        networks().assign(level, number, network);
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
            networks().place(level, number, joined, member);
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
            networks().remove(level, number.getAsLong());
        } else {
            final Grid grid = of(level, kind);
            if (grid.contains(number.getAsLong())) {
                grid.remove(number.getAsLong());
            }
        }
        // A device in several grids is one number in each: it is forgotten only when no grid still holds it.
        for (final GridKind other : GridKind.all()) {
            if (of(level, other).contains(number.getAsLong())) {
                return;
            }
        }
        places.forget(place);
    }

    /** Whether {@code place} is in the grid of {@code kind}. */
    public static boolean contains(final ServerLevel level, final GridKind kind, final GridPlace place) {
        final OptionalLong number = places(level).find(place);
        return number.isPresent() && of(level, kind).contains(number.getAsLong());
    }

    /** The work that shares what the grid of {@code kind} carries in {@code level}, or none for a grid that joins. */
    public static Optional<IGridMover> mover(final ServerLevel level, final GridKind kind) {
        return kind.carriesNetwork() ? Optional.empty() : level.getData(CoreAttachments.GRIDS).mover(kind);
    }

    /** Hands the game's bus the ticking of every grid, once, from the Core's constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, CoreGrids::afterTick);
    }

    private static INetworkGrids networks() {
        final INetworkGrids keeper = networks;
        if (keeper == null) {
            throw new IllegalStateException("no mod keeps the grids that carry networks; hand one to keepNetworksIn");
        }
        return keeper;
    }

    /* After the machines have ticked, what each grid carries moves through its wires, the grids in declared order. */
    private static void afterTick(final LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            for (final GridKind kind : GridKind.all()) {
                mover(level, kind).ifPresent(mover -> mover.tick(level));
            }
        }
    }
}
