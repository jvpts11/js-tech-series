/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.network;

import dev.jstech.core.uuid.NetworkUuid;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * Spatial connectivity index for the J's Computers computation network.
 */
public final class ConnectivityIndex {

    private final DisjointSetUnion dsu = new DisjointSetUnion();

    private final Map<Long, Integer> posToId = new HashMap<>();

    private final Map<Integer, Long> idToPos = new HashMap<>();

    private final Map<Integer, NetworkUuid> rootToUuid = new HashMap<>();

    private final Map<Long, Set<Long>> adjacency = new HashMap<>();

    /*
     * Every component's positions, by root, kept until the topology changes: a Mainframe asks for its
     * segment every tick, and walking every cable of a big base to answer was a fixed cost on the idle tick.
     */
    private final Map<Integer, Set<Long>> componentCache = new HashMap<>();

    /*
     * The cables each network owner touches, keyed by the owner's own position. A Mainframe is not a position of
     * this index, so these are what let a removal tell the fragment that still reaches it from one it cut away.
     */
    private final Map<Long, Anchor> anchors = new HashMap<>();

    /* What kind of cable each position is; a position with none (a router) limits nothing that passes through it. */
    private final Map<Long, DataTier> tiers = new HashMap<>();

    /*
     * The cable runs each bridging device joins, by the device's own position, and the other way round. A Mainframe
     * or a rack is not a position of this index, so these are how a search for a way through the network crosses
     * one: from any cable it touches to any other.
     */
    private final Map<Long, Set<Long>> bridges = new HashMap<>();
    private final Map<Long, Set<Long>> bridgedBy = new HashMap<>();

    // Queries

    public Optional<NetworkUuid> networkOf(long encodedPos) {
        Integer id = posToId.get(encodedPos);
        if (id == null) {
            return Optional.empty();
        }
        int root = dsu.find(id);
        return Optional.ofNullable(rootToUuid.get(root));
    }

    public boolean inSameNetwork(long encodedA, long encodedB) {
        Integer idA = posToId.get(encodedA);
        Integer idB = posToId.get(encodedB);
        if (idA == null || idB == null) {
            return false;
        }
        return dsu.connected(idA, idB);
    }

    public boolean contains(long encodedPos) {
        return posToId.containsKey(encodedPos);
    }

    public int size() {
        return posToId.size();
    }

    public int componentCount() {
        return dsu.componentCount();
    }

    public int componentSize(long encodedPos) {
        return componentPositions(encodedPos).size();
    }

    public Set<Long> componentPositions(long encodedPos) {
        final Integer id = posToId.get(encodedPos);
        if (id == null) {
            return Set.of();
        }
        final int root = dsu.find(id);
        Set<Long> cached = componentCache.get(root);
        if (cached == null) {
            final Set<Long> result = new LinkedHashSet<>();
            for (final Map.Entry<Long, Integer> entry : posToId.entrySet()) {
                if (dsu.find(entry.getValue()) == root) {
                    result.add(entry.getKey());
                }
            }
            cached = Collections.unmodifiableSet(result);
            componentCache.put(root, cached);
        }
        return cached;
    }

    /**
     * Every cable position belonging to the given network. A read-only scan (the same shape as
     * {@link #componentPositions}) used to locate a named device, such as a bus, mounted anywhere on the
     * network's cabling without needing a starting position.
     */
    public Set<Long> positionsOf(final NetworkUuid network) {
        if (network == null) {
            return Set.of();
        }
        final Set<Long> result = new LinkedHashSet<>();
        for (final Map.Entry<Long, Integer> entry : posToId.entrySet()) {
            if (network.equals(rootToUuid.get(dsu.find(entry.getValue())))) {
                result.add(entry.getKey());
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
     * travels no faster than, since it is only as fast as the slowest cable it has to pass.
     *
     * <p>Of every way between them, the one whose slowest cable is fastest is taken, the way traffic takes the best
     * route it has. Through the devices that bridge cable runs (a Mainframe, a rack) the way goes from any cable the
     * device touches to any other; a position that is no cable, such as a router, slows nothing down. Cables are
     * compared by their {@link DataTier#maxThroughput()}.
     *
     * @return the slowest cable on that way, or empty when nothing joins the two, or nothing but devices lies between
     */
    public Optional<DataTier> slowestBetween(final Collection<Long> from, final Collection<Long> to) {
        final Set<Long> goals = new HashSet<>();
        for (final long pos : to) {
            if (posToId.containsKey(pos)) {
                goals.add(pos);
            }
        }
        if (goals.isEmpty()) {
            return Optional.empty();
        }
        /*
         * A widest-path search: every position is reached first along the way whose slowest cable is fastest, so the
         * first goal taken off the queue is reached as fast as it can be.
         */
        final Map<Long, Long> best = new HashMap<>();
        final Map<Long, DataTier> slowest = new HashMap<>();
        final PriorityQueue<Reach> queue = new PriorityQueue<>(
                Comparator.comparingLong(Reach::throughput).reversed());
        for (final long pos : from) {
            if (posToId.containsKey(pos)) {
                offer(pos, null, best, slowest, queue);
            }
        }
        final Set<Long> done = new HashSet<>();
        while (!queue.isEmpty()) {
            final Reach reach = queue.poll();
            if (!done.add(reach.pos())) {
                continue;
            }
            if (goals.contains(reach.pos())) {
                return Optional.ofNullable(slowest.get(reach.pos()));
            }
            final DataTier sofar = slowest.get(reach.pos());
            for (final long next : neighboursOf(reach.pos())) {
                if (!done.contains(next) && posToId.containsKey(next)) {
                    offer(next, sofar, best, slowest, queue);
                }
            }
        }
        return Optional.empty();
    }

    // Mutations

    /** Puts in a position that carries the network but is no cable, such as a router. */
    public IPlacementResult onCablePlaced(long encodedPos, Set<Long> neighbors) {
        return onCablePlaced(encodedPos, neighbors, null);
    }

    /**
     * Puts in a cable of that tier, joined to those of its neighbours already in.
     *
     * @param tier the kind of cable, or null for a position that carries the network without being a cable
     */
    public IPlacementResult onCablePlaced(long encodedPos, Set<Long> neighbors, @Nullable DataTier tier) {
        if (posToId.containsKey(encodedPos)) {
            throw new IllegalStateException(
                    "Position already registered: " + encodedPos);
        }
        if (tier != null) {
            tiers.put(encodedPos, tier);
        }

        // 1. Allocate a fresh DSU element for the new cable.
        componentCache.clear();
        int newId = dsu.makeSet();
        posToId.put(encodedPos, newId);
        idToPos.put(newId, encodedPos);
        adjacency.put(encodedPos, new LinkedHashSet<>());

        // 2. Find which neighbors are actually in the index, and what UUIDs
        NetworkUuid firstSeenUuid = null;
        NetworkUuid conflictingUuid = null;
        boolean unionedWithAny = false;

        for (Long neighborPos : neighbors) {
            Integer neighborId = posToId.get(neighborPos);
            if (neighborId == null) {
                continue; // Unknown neighbor, ignore it.
            }
            // Record the edge in both directions for rediscovery.
            adjacency.get(encodedPos).add(neighborPos);
            adjacency.get(neighborPos).add(encodedPos);
            int neighborRoot = dsu.find(neighborId);
            NetworkUuid neighborUuid = rootToUuid.get(neighborRoot);

            if (neighborUuid != null) {
                if (firstSeenUuid == null) {
                    firstSeenUuid = neighborUuid;
                } else if (!firstSeenUuid.equals(neighborUuid) && conflictingUuid == null) {
                    conflictingUuid = neighborUuid;
                }
            }

            // Union regardless: even on conflict, we want the topology
            if (dsu.union(newId, neighborId)) {
                unionedWithAny = true;
            }
        }

        /*
         * 3. After all unions, the new cable's component has ONE root.
         *    Decide what UUID (if any) the merged component should have.
         */
        int newRoot = dsu.find(newId);

        if (conflictingUuid != null) {
            // Multiple distinct UUIDs were merged. Keep the first one as
            cleanupOrphanedUuids(newRoot);
            rootToUuid.put(newRoot, firstSeenUuid);
            return new IPlacementResult.Conflict(firstSeenUuid, conflictingUuid);
        }

        if (firstSeenUuid != null) {
            /*
             * All neighbors with UUIDs agreed on a single value. The
             * merged component now carries that UUID.
             */
            cleanupOrphanedUuids(newRoot);
            rootToUuid.put(newRoot, firstSeenUuid);
            return new IPlacementResult.Inherited(firstSeenUuid);
        }

        if (unionedWithAny) {
            /*
             * We merged with at least one neighbor, but none of them had
             * a UUID. The merged component is still UUID-less.
             */
            return IPlacementResult.MERGED_WITHOUT_UUID;
        }

        // No relevant neighbors at all, so the cable is isolated.
        return IPlacementResult.ISOLATED;
    }

    private void cleanupOrphanedUuids(int currentRoot) {
        // Collect ids to remove (avoid concurrent modification).
        var toRemove = new ArrayList<Integer>();
        for (Integer id : rootToUuid.keySet()) {
            if (id != currentRoot && dsu.find(id) == currentRoot) {
                toRemove.add(id);
            }
        }
        for (Integer id : toRemove) {
            rootToUuid.remove(id);
        }
    }

    /**
     * Joins the cable runs a device touches into one network, and remembers that it does, so a search for a way
     * through the network crosses the device. Reporting the same cables again changes nothing, so a device can
     * report them every tick.
     *
     * @param device where the device stands, which is no position of this index
     */
    public void bridge(final long device, final Collection<Long> positions) {
        final Set<Long> touched = Set.copyOf(positions);
        if (!touched.equals(bridges.get(device))) {
            forgetBridge(device);
            if (!touched.isEmpty()) {
                bridges.put(device, touched);
                for (final long cable : touched) {
                    bridgedBy.computeIfAbsent(cable, pos -> new HashSet<>()).add(device);
                }
            }
        }
        join(positions);
    }

    /** The device at {@code device} is gone: it joins nothing any more on a way through the network. */
    public void forgetBridge(final long device) {
        final Set<Long> touched = bridges.remove(device);
        if (touched == null) {
            return;
        }
        for (final long cable : touched) {
            final Set<Long> devices = bridgedBy.get(cable);
            if (devices != null) {
                devices.remove(device);
                if (devices.isEmpty()) {
                    bridgedBy.remove(cable);
                }
            }
        }
    }

    private void join(final Collection<Long> positions) {
        final List<Integer> ids = new ArrayList<>();
        NetworkUuid surviving = null;
        for (final long pos : positions) {
            final Integer id = posToId.get(pos);
            if (id == null) {
                continue;
            }
            ids.add(id);
            /*
             * Deterministic survivor: the lexicographically smallest UUID among the merged components, so
             * which network identity wins a merge does not depend on iteration order (unpredictable to the
             * player and unstable across reloads).
             */
            final NetworkUuid uuid = rootToUuid.get(dsu.find(id));
            if (uuid != null && (surviving == null || uuid.asString().compareTo(surviving.asString()) < 0)) {
                surviving = uuid;
            }
        }
        if (ids.size() < 2) {
            return; // nothing to bridge, since a device touching one run (or none) changes nothing
        }
        int root = dsu.find(ids.get(0));
        boolean merged = false;
        for (int k = 1; k < ids.size(); k++) {
            if (dsu.union(root, ids.get(k))) {
                merged = true;
            }
            root = dsu.find(root);
        }
        if (merged) {
            componentCache.clear();
            cleanupOrphanedUuids(root);
            if (surviving != null) {
                rootToUuid.put(root, surviving);
            }
        }
    }

    public void assignUuid(long encodedPos, NetworkUuid uuid) {
        Integer id = posToId.get(encodedPos);
        if (id == null) {
            throw new IllegalStateException(
                    "Position not registered: " + encodedPos);
        }
        int root = dsu.find(id);
        rootToUuid.put(root, uuid);
    }

    public void clearNetwork(NetworkUuid uuid) {
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
    public void onCableRemovedIfRegistered(long encodedPos) {
        if (posToId.containsKey(encodedPos)) {
            onCableRemoved(encodedPos);
        }
    }

    public RemovalResult onCableRemoved(long encodedPos) {
        if (!posToId.containsKey(encodedPos)) {
            throw new IllegalStateException(
                    "Position not registered: " + encodedPos);
        }

        final Optional<NetworkUuid> previousUuid = networkOf(encodedPos);
        final Set<Long> affectedComponent = collectComponent(encodedPos);

        // Snapshot every cable's current UUID before tearing the index down.
        final Map<Long, NetworkUuid> uuidByPos = new HashMap<>();
        for (final Long pos : posToId.keySet()) {
            networkOf(pos).ifPresent(uuid -> uuidByPos.put(pos, uuid));
        }

        // Detach the removed cable from the adjacency graph.
        for (final Long neighbor : adjacency.getOrDefault(encodedPos, Set.of())) {
            final Set<Long> neighborEdges = adjacency.get(neighbor);
            if (neighborEdges != null) {
                neighborEdges.remove(encodedPos);
            }
        }
        adjacency.remove(encodedPos);
        tiers.remove(encodedPos);

        // Rebuild the DSU from scratch over the surviving cables.
        final Set<Long> survivors = new LinkedHashSet<>(posToId.keySet());
        survivors.remove(encodedPos);
        dsu.clear();
        componentCache.clear();
        posToId.clear();
        idToPos.clear();
        rootToUuid.clear();
        for (final Long pos : survivors) {
            final int newId = dsu.makeSet();
            posToId.put(pos, newId);
            idToPos.put(newId, pos);
        }
        for (final Long pos : survivors) {
            for (final Long neighbor : adjacency.getOrDefault(pos, Set.of())) {
                final Integer neighborId = posToId.get(neighbor);
                if (neighborId != null) {
                    dsu.union(posToId.get(pos), neighborId);
                }
            }
        }

        // Count the distinct fragments the affected component split into.
        final Set<Integer> fragmentRoots = new HashSet<>();
        for (final Long pos : affectedComponent) {
            final Integer id = posToId.get(pos);
            if (id != null) {
                fragmentRoots.add(dsu.find(id));
            }
        }
        final boolean severed = fragmentRoots.size() >= 2;
        /*
         * Removing the only cable an owner touched cuts everything else away from that owner, even though the
         * surviving cables still form one fragment; that fragment used to keep the network with nothing left
         * connecting it to its Mainframe.
         */
        final boolean cutFromOwner = !severed && previousUuid.isPresent()
                && cutFromItsOwner(previousUuid.get(), encodedPos, affectedComponent);

        // Restore UUIDs. Networks untouched by this removal keep theirs. But
        for (final Map.Entry<Long, NetworkUuid> entry : uuidByPos.entrySet()) {
            if ((severed || cutFromOwner) && affectedComponent.contains(entry.getKey())) {
                continue;
            }
            final Integer id = posToId.get(entry.getKey());
            if (id != null) {
                rootToUuid.put(dsu.find(id), entry.getValue());
            }
        }
        return new RemovalResult(previousUuid, fragmentRoots.size());
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
                } else if (posToId.containsKey(cable) && affected.contains(cable)) {
                    return false;
                }
            }
        }
        return removedAnAnchor;
    }

    public Set<Long> reachableFrom(final long start, final Set<Long> blocked) {
        final Set<Long> visited = new LinkedHashSet<>();
        if (blocked.contains(start) || !posToId.containsKey(start)) {
            return visited;
        }
        final Deque<Long> queue = new ArrayDeque<>();
        visited.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            final Long current = queue.poll();
            for (final Long neighbor : adjacency.getOrDefault(current, Set.of())) {
                if (!blocked.contains(neighbor) && visited.add(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }
        return visited;
    }

    private Set<Long> collectComponent(long start) {
        final Set<Long> visited = new LinkedHashSet<>();
        final Deque<Long> queue = new ArrayDeque<>();
        visited.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            final Long current = queue.poll();
            for (final Long neighbor : adjacency.getOrDefault(current, Set.of())) {
                if (visited.add(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }
        return visited;
    }

    public void clear() {
        posToId.clear();
        idToPos.clear();
        rootToUuid.clear();
        adjacency.clear();
        dsu.clear();
        componentCache.clear();
        anchors.clear();
        tiers.clear();
        bridges.clear();
        bridgedBy.clear();
    }

    /* The positions a way through the network steps to from {@code pos}: its neighbours, and across its devices. */
    private Set<Long> neighboursOf(final long pos) {
        final Set<Long> devices = bridgedBy.get(pos);
        if (devices == null) {
            return adjacency.getOrDefault(pos, Set.of());
        }
        final Set<Long> out = new HashSet<>(adjacency.getOrDefault(pos, Set.of()));
        for (final long device : devices) {
            out.addAll(bridges.getOrDefault(device, Set.of()));
        }
        out.remove(pos);
        return out;
    }

    /* Offers {@code pos} to the search, reached along a way whose slowest cable so far is {@code before}. */
    private void offer(final long pos, @Nullable final DataTier before, final Map<Long, Long> best,
                       final Map<Long, DataTier> slowest, final PriorityQueue<Reach> queue) {
        final DataTier here = tiers.get(pos);
        final DataTier limit = throughputOf(here) < throughputOf(before) ? here : before;
        final long throughput = throughputOf(limit);
        if (throughput > best.getOrDefault(pos, -1L)) {
            best.put(pos, throughput);
            if (limit == null) {
                slowest.remove(pos);
            } else {
                slowest.put(pos, limit);
            }
            queue.add(new Reach(pos, throughput));
        }
    }

    /* How much a cable carries; a position that is no cable limits nothing. */
    private static long throughputOf(@Nullable final DataTier tier) {
        return tier == null ? Long.MAX_VALUE : tier.maxThroughput();
    }

    /** The network an owner holds and the cables it touches. */
    private record Anchor(NetworkUuid network, Set<Long> cables) {
    }

    /** A position the search has reached, and how fast the best way to it is. */
    private record Reach(long pos, long throughput) {
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
