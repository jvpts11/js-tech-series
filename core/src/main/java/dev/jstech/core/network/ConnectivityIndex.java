/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.network;

import dev.jstech.core.grid.Grid;
import dev.jstech.core.grid.GridMember;
import dev.jstech.core.uuid.NetworkUuid;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * The data network's grid: the Core's {@link Grid} of the data cables and the devices that carry the network, with
 * what only the data grid has, the identity of the network each part of it belongs to. The data grid is the only
 * grid whose parts carry a {@link NetworkUuid}; power, fluid and the others are grids and nothing more.
 *
 * <p>A part of the grid takes the identity its owner gives it, keeps it when cables join it, and settles a conflict
 * when two networks' cables meet. A part cut away from its owner loses it.
 */
public final class ConnectivityIndex {

    private final Grid grid = new Grid();
    private final Map<Integer, NetworkUuid> rootToUuid = new HashMap<>();
    /*
     * The cables each network owner touches, keyed by the owner's own position. A Mainframe is not a position of
     * this index, so these are what let a removal tell the fragment that still reaches it from one it cut away.
     */
    private final Map<Long, Anchor> anchors = new HashMap<>();
    /* What kind of cable each position is; a position with none (a router) limits nothing that passes through it. */
    private final Map<Long, DataTier> tiers = new HashMap<>();

    /* Each tier's cable as the grid holds it: its line, how much it carries and how far a run of it reaches. */
    private static final Map<DataTier, GridMember> MEMBERS = new EnumMap<>(DataTier.class);

    static {
        for (final DataTier tier : DataTier.values()) {
            MEMBERS.put(tier, GridMember.cable(tier.line(), 0, tier.maxThroughput(), tier.maxLength()));
        }
    }

    /** The grid itself, for what any grid answers: the runs, their lengths, the ways through it. */
    public Grid grid() {
        return grid;
    }

    // Queries

    public Optional<NetworkUuid> networkOf(final long encodedPos) {
        final int root = grid.rootOf(encodedPos);
        return root < 0 ? Optional.empty() : Optional.ofNullable(rootToUuid.get(root));
    }

    public boolean inSameNetwork(final long encodedA, final long encodedB) {
        return grid.connected(encodedA, encodedB);
    }

    public boolean contains(final long encodedPos) {
        return grid.contains(encodedPos);
    }

    public int size() {
        return grid.size();
    }

    public int componentCount() {
        return grid.componentCount();
    }

    public int componentSize(final long encodedPos) {
        return componentPositions(encodedPos).size();
    }

    public Set<Long> componentPositions(final long encodedPos) {
        return grid.componentPositions(encodedPos);
    }

    /**
     * Every cable position belonging to the given network. A read-only scan used to locate a named device, such as a
     * bus, mounted anywhere on the network's cabling without needing a starting position.
     */
    public Set<Long> positionsOf(final NetworkUuid network) {
        if (network == null) {
            return Set.of();
        }
        final Set<Long> result = new LinkedHashSet<>();
        for (final long pos : grid.positions()) {
            if (network.equals(rootToUuid.get(grid.rootOf(pos)))) {
                result.add(pos);
            }
        }
        return result;
    }

    /** What kind of cable is at that position, or empty for a position that is no cable (a router) or none. */
    public Optional<DataTier> tierOf(final long encodedPos) {
        return Optional.ofNullable(tiers.get(encodedPos));
    }

    /**
     * The slowest cable on the fastest way from any of {@code from} to any of {@code to}: what data between them
     * travels no faster than, since it is only as fast as the slowest cable it has to pass. Worked out once each time
     * the grid changes, as {@link Grid#slowestBetween} is.
     *
     * @return the slowest cable on that way, or empty when nothing joins the two, or nothing but devices lies between
     */
    public Optional<DataTier> slowestBetween(final Collection<Long> from, final Collection<Long> to) {
        final OptionalLong at = grid.slowestBetween(from, to);
        return at.isPresent() ? Optional.ofNullable(tiers.get(at.getAsLong())) : Optional.empty();
    }

    // Mutations

    /** Puts in a position that carries the network but is no cable, such as a router. */
    public IPlacementResult onCablePlaced(final long encodedPos, final Set<Long> neighbors) {
        return onCablePlaced(encodedPos, neighbors, null);
    }

    /**
     * Puts in a cable of that tier, joined to those of its neighbours already in that it joins.
     *
     * @param tier the kind of cable, or null for a position that carries the network without being a cable
     */
    public IPlacementResult onCablePlaced(final long encodedPos, final Set<Long> neighbors,
                                         @Nullable final DataTier tier) {
        if (grid.contains(encodedPos)) {
            throw new IllegalStateException("Position already registered: " + encodedPos);
        }
        if (tier != null) {
            tiers.put(encodedPos, tier);
        }
        final Set<Integer> joinedRoots = grid.place(encodedPos, tier == null ? GridMember.DEVICE : MEMBERS.get(tier),
                neighbors);
        NetworkUuid firstSeen = null;
        NetworkUuid conflicting = null;
        for (final int root : joinedRoots) {
            final NetworkUuid uuid = rootToUuid.get(root);
            if (uuid == null) {
                continue;
            }
            if (firstSeen == null) {
                firstSeen = uuid;
            } else if (!firstSeen.equals(uuid) && conflicting == null) {
                conflicting = uuid;
            }
        }
        final int newRoot = grid.rootOf(encodedPos);
        forgetMerged(joinedRoots, newRoot);
        if (conflicting != null) {
            // Several networks met: the first keeps the merged part, and the caller is told of the conflict.
            rootToUuid.put(newRoot, firstSeen);
            return new IPlacementResult.Conflict(firstSeen, conflicting);
        }
        if (firstSeen != null) {
            rootToUuid.put(newRoot, firstSeen);
            return new IPlacementResult.Inherited(firstSeen);
        }
        return joinedRoots.isEmpty() ? IPlacementResult.ISOLATED : IPlacementResult.MERGED_WITHOUT_UUID;
    }

    /**
     * Joins the cable runs a device touches into one network, and remembers that it does, so a search for a way
     * through the network crosses the device. Reporting the same cables again changes nothing, so a device can
     * report them every tick.
     *
     * @param device where the device stands, which is no position of this index
     */
    public void bridge(final long device, final Collection<Long> positions) {
        final Set<Integer> rootsBefore = new LinkedHashSet<>();
        NetworkUuid surviving = null;
        for (final long pos : positions) {
            final int root = grid.rootOf(pos);
            if (root < 0) {
                continue;
            }
            rootsBefore.add(root);
            /*
             * Deterministic survivor: the lexicographically smallest UUID among the merged components, so which
             * network identity wins a merge does not depend on iteration order (unpredictable to the player and
             * unstable across reloads).
             */
            final NetworkUuid uuid = rootToUuid.get(root);
            if (uuid != null && (surviving == null || uuid.asString().compareTo(surviving.asString()) < 0)) {
                surviving = uuid;
            }
        }
        if (!grid.bridge(device, positions) || rootsBefore.isEmpty()) {
            return;
        }
        final int root = grid.rootOf(positions.stream().filter(grid::contains).findFirst().orElseThrow());
        forgetMerged(rootsBefore, root);
        if (surviving != null) {
            rootToUuid.put(root, surviving);
        }
    }

    /** The cables the device at {@code device} last said it touches, which is how the network reaches it. */
    public Set<Long> bridgedBy(final long device) {
        return grid.bridgedBy(device);
    }

    /** The device at {@code device} is gone: it joins nothing any more on a way through the network. */
    public void forgetBridge(final long device) {
        grid.forgetBridge(device);
    }

    public void assignUuid(final long encodedPos, final NetworkUuid uuid) {
        final int root = grid.rootOf(encodedPos);
        if (root < 0) {
            throw new IllegalStateException("Position not registered: " + encodedPos);
        }
        rootToUuid.put(root, uuid);
    }

    public void clearNetwork(final NetworkUuid uuid) {
        rootToUuid.values().removeIf(uuid::equals);
        anchors.values().removeIf(anchor -> anchor.network().equals(uuid));
    }

    /**
     * Records the cables the owner of {@code network} at {@code ownerPos} touches. Reporting the same cables again
     * changes nothing, so an owner can report them every tick.
     */
    public void anchor(final long ownerPos, final NetworkUuid network, final Set<Long> cables) {
        final Anchor current = anchors.get(ownerPos);
        if (current == null || !current.network().equals(network) || !current.cables().equals(cables)) {
            anchors.put(ownerPos, new Anchor(network, Set.copyOf(cables)));
        }
    }

    /**
     * Takes out a block that carries the network, if it was ever put in.
     *
     * <p>What carries the network is put in when it first loads, which comes after it is placed, so a block
     * placed and broken again within the same tick is gone before it was ever here. Every block that carries
     * the network is removed through this, so that race is answered in one place and not once per block.
     */
    public void onCableRemovedIfRegistered(final long encodedPos) {
        if (grid.contains(encodedPos)) {
            onCableRemoved(encodedPos);
        }
    }

    public RemovalResult onCableRemoved(final long encodedPos) {
        if (!grid.contains(encodedPos)) {
            throw new IllegalStateException("Position not registered: " + encodedPos);
        }
        final Optional<NetworkUuid> previousUuid = networkOf(encodedPos);
        // Snapshot every cable's current UUID before the grid works its parts out again.
        final Map<Long, NetworkUuid> uuidByPos = new HashMap<>();
        for (final long pos : grid.positions()) {
            networkOf(pos).ifPresent(uuid -> uuidByPos.put(pos, uuid));
        }
        final Grid.Removal removal = grid.remove(encodedPos);
        tiers.remove(encodedPos);
        final Set<Long> affected = removal.affected();
        final boolean severed = removal.fragments() >= 2;
        /*
         * Removing the only cable an owner touched cuts everything else away from that owner, even though the
         * surviving cables still form one fragment; that fragment used to keep the network with nothing left
         * connecting it to its Mainframe.
         */
        final boolean cutFromOwner = !severed && previousUuid.isPresent()
                && cutFromItsOwner(previousUuid.get(), encodedPos, affected);
        // Networks untouched by this removal keep theirs; a part severed or cut from its owner loses its own.
        rootToUuid.clear();
        for (final Map.Entry<Long, NetworkUuid> entry : uuidByPos.entrySet()) {
            if ((severed || cutFromOwner) && affected.contains(entry.getKey())) {
                continue;
            }
            final int root = grid.rootOf(entry.getKey());
            if (root >= 0) {
                rootToUuid.put(root, entry.getValue());
            }
        }
        return new RemovalResult(previousUuid, removal.fragments());
    }

    public Set<Long> reachableFrom(final long start, final Set<Long> blocked) {
        return grid.reachableFrom(start, blocked);
    }

    public void clear() {
        grid.clear();
        rootToUuid.clear();
        anchors.clear();
        tiers.clear();
    }

    /**
     * Whether removing {@code removed} took an anchor of {@code network} and left no other anchor of it among the
     * surviving cables of {@code affected}. A network whose owner has not reported its cables yet (right after a
     * world loads) is never treated as cut here.
     */
    private boolean cutFromItsOwner(final NetworkUuid network, final long removed, final Set<Long> affected) {
        boolean removedAnAnchor = false;
        for (final Anchor anchor : anchors.values()) {
            if (!anchor.network().equals(network)) {
                continue;
            }
            for (final long cable : anchor.cables()) {
                if (cable == removed) {
                    removedAnAnchor = true;
                } else if (grid.contains(cable) && affected.contains(cable)) {
                    return false;
                }
            }
        }
        return removedAnAnchor;
    }

    /* The parts in {@code roots} became the one at {@code root}: the identities they held are the merged part's. */
    private void forgetMerged(final Set<Integer> roots, final int root) {
        for (final int merged : roots) {
            if (merged != root) {
                rootToUuid.remove(merged);
            }
        }
    }

    /** The network an owner holds and the cables it touches. */
    private record Anchor(NetworkUuid network, Set<Long> cables) {
    }

    /**
     * Outcome of an {@link #onCableRemoved} invocation.
     */
    public record RemovalResult(Optional<NetworkUuid> previousUuid, int resultingComponents) {
    }

    // IPlacementResult

    /**
     * Outcome of a single {@link #onCablePlaced} invocation.
     */
    public sealed interface IPlacementResult
            permits IPlacementResult.Isolated,
            IPlacementResult.MergedWithoutUuid,
            IPlacementResult.Inherited,
            IPlacementResult.Conflict {

        /**
         * No registered neighbors, so the cable is alone in its own new component.
         */
        record Isolated() implements IPlacementResult {}

        /**
         * Unioned with neighbors, but none of them had a UUID.
         */
        record MergedWithoutUuid() implements IPlacementResult {}

        /**
         * Unioned with neighbors that all agreed on a single UUID; component now carries it.
         */
        record Inherited(NetworkUuid uuid) implements IPlacementResult {}

        /**
         * Two or more neighbors carried distinct UUIDs.
         */
        record Conflict(NetworkUuid first, NetworkUuid second) implements IPlacementResult {}

        // Stateless singletons for the cases without payload.
        Isolated ISOLATED = new Isolated();
        MergedWithoutUuid MERGED_WITHOUT_UUID = new MergedWithoutUuid();
    }
}
