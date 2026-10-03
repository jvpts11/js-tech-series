/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.peripheral;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Validates and establishes peripheral links via bounded BFS.
 */
public final class PeripheralLinkValidator {

    /**
     * Looks up the cable type at a position, or empty if no cable of any peripheral type is present.
     */
    @FunctionalInterface
    public interface ICableLookup {
        Optional<PeripheralCableType> cableTypeAt(long pos);
    }

    /**
     * Looks up an owner BlockEntity at a position, or empty if no owner is present.
     */
    @FunctionalInterface
    public interface IOwnerLookup {
        Optional<IPeripheralOwner> ownerAt(long pos);
    }

    /**
     * Looks up an endpoint BlockEntity at a position, or empty if no endpoint is present.
     */
    @FunctionalInterface
    public interface IEndpointLookup {
        Optional<IPeripheralEndpoint> endpointAt(long pos);
    }

    /**
     * The positions a position leads on to: for a block that is no cable, the six beside it; for a cable, only those
     * its wire crosses to, the cables it joins and the blocks it plugs into. A step counts only where it leads back.
     */
    @FunctionalInterface
    public interface INeighborLookup {
        List<Long> neighborsOf(long pos);
    }

    /**
     * How many cables a run of the cable at a position reaches, its era's reach, or 0 when the cable says nothing and
     * its system's limit holds.
     */
    @FunctionalInterface
    public interface IReachLookup {
        int reachAt(long pos);
    }

    private final ICableLookup cableLookup;
    private final IOwnerLookup ownerLookup;
    private final IEndpointLookup endpointLookup;
    private final INeighborLookup neighborLookup;
    private final IReachLookup reachLookup;

    /** A validator whose cables all reach as far as their system allows. */
    public PeripheralLinkValidator(
            final ICableLookup cableLookup,
            final IOwnerLookup ownerLookup,
            final IEndpointLookup endpointLookup,
            final INeighborLookup neighborLookup) {
        this(cableLookup, ownerLookup, endpointLookup, neighborLookup, pos -> 0);
    }

    /** A validator whose cables each say how far a run of them reaches: a path is as long as its shortest reach. */
    public PeripheralLinkValidator(
            final ICableLookup cableLookup,
            final IOwnerLookup ownerLookup,
            final IEndpointLookup endpointLookup,
            final INeighborLookup neighborLookup,
            final IReachLookup reachLookup) {
        this.cableLookup = cableLookup;
        this.ownerLookup = ownerLookup;
        this.endpointLookup = endpointLookup;
        this.neighborLookup = neighborLookup;
        this.reachLookup = reachLookup;
    }

    public ILinkResult tryEstablishLink(
            final long ownerPos,
            final long endpointPos) {

        final Optional<IPeripheralOwner> ownerOpt = ownerLookup.ownerAt(ownerPos);
        final Optional<IPeripheralEndpoint> endpointOpt = endpointLookup.endpointAt(endpointPos);

        if (ownerOpt.isEmpty() || endpointOpt.isEmpty()) {
            return new ILinkResult.NoPathFound(ownerPos, endpointPos);
        }

        final IPeripheralOwner owner = ownerOpt.get();
        final IPeripheralEndpoint endpoint = endpointOpt.get();

        if (owner.cableType() != endpoint.cableType()) {
            return new ILinkResult.CableTypeMismatch(
                    owner.cableType(), endpoint.cableType());
        }

        // Endpoint cardinality check: at most one owner.
        final Optional<Long> existingOwner = endpoint.linkedOwner();
        if (existingOwner.isPresent() && existingOwner.get() != ownerPos) {
            return new ILinkResult.AlreadyLinked(endpointPos, existingOwner.get());
        }

        /*
         * The endpoint needs a free port of its own kind, but re-linking one already linked takes nothing more
         * (idempotent re-establish after periodic validation).
         */
        final PortKind kind = endpoint.portKind();
        final int inUse = owner.portsInUse(kind);
        final int ports = owner.ports(kind);
        if (inUse >= ports && !owner.linkedEndpoints().contains(endpointPos)) {
            return new ILinkResult.OwnerAtCapacity(ownerPos, kind, inUse, ports);
        }

        final PeripheralCableType requiredType = owner.cableType();
        final IPathSearchResult pathResult = findPath(
                ownerPos, endpointPos, requiredType);

        return switch (pathResult) {
            case IPathSearchResult.Found(int length) -> {
                owner.onEndpointLinked(endpointPos, kind);
                endpoint.onOwnerLinked(ownerPos);
                yield new ILinkResult.Established(ownerPos, endpointPos, length);
            }
            case IPathSearchResult.NotFound notFound ->
                    new ILinkResult.NoPathFound(ownerPos, endpointPos);
            case IPathSearchResult.TooLong(int length, int max) ->
                    new ILinkResult.ExceedsMaxLength(
                            ownerPos, endpointPos, length, max);
        };
    }

    public boolean isLinkStillValid(
            final long ownerPos,
            final long endpointPos,
            final PeripheralCableType cableType) {
        return findPath(ownerPos, endpointPos, cableType)
                instanceof IPathSearchResult.Found;
    }

    // ─── BFS internals ──────────────────────────────────────────────────────

    /**
     * Internal sealed result of the path-finding step.
     */
    private sealed interface IPathSearchResult
            permits IPathSearchResult.Found,
            IPathSearchResult.NotFound,
            IPathSearchResult.TooLong {

        record Found(int length) implements IPathSearchResult {}

        record NotFound() implements IPathSearchResult {}

        record TooLong(int length, int max) implements IPathSearchResult {}
    }

    private IPathSearchResult findPath(
            final long source,
            final long target,
            final PeripheralCableType requiredType) {

        final int maxLength = requiredType.maxLength();
        final Set<Long> visited = new HashSet<>();
        // Each queued place: its position, how many cables it is from the owner, and the shortest reach on the way.
        final Deque<long[]> queue = new ArrayDeque<>();

        // The source may be a multiblock owner: seed BFS from every face of every
        final Set<Long> sources = ownerLookup.ownerAt(source)
                .map(owner -> owner.occupiedPositions(source))
                .filter(positions -> !positions.isEmpty())
                .orElseGet(() -> Set.of(source));
        visited.addAll(sources);
        for (final long src : sources) {
            for (final long neighbor : neighborLookup.neighborsOf(src)) {
                if (neighbor == target) {
                    // Owner adjacent to endpoint, zero cables between them.
                    return new IPathSearchResult.Found(0);
                }
                if (visited.add(neighbor) && enters(neighbor, src, requiredType)) {
                    queue.addLast(new long[]{neighbor, 1L, reach(neighbor, maxLength)});
                }
            }
        }

        while (!queue.isEmpty()) {
            final long[] current = queue.pollFirst();
            final long pos = current[0];
            final int distance = (int) current[1];
            final int limit = (int) current[2];

            for (final long neighbor : neighborLookup.neighborsOf(pos)) {
                if (neighbor == target) {
                    // First reach is shortest path (BFS invariant).
                    if (distance <= limit) {
                        return new IPathSearchResult.Found(distance);
                    }
                    return new IPathSearchResult.TooLong(distance, limit);
                }
                if (!visited.add(neighbor)) {
                    continue;
                }
                if (enters(neighbor, pos, requiredType)) {
                    queue.addLast(new long[]{neighbor, distance + 1, Math.min(limit, reach(neighbor, maxLength))});
                }
            }
        }

        return new IPathSearchResult.NotFound();
    }

    /* Whether a path steps from {@code from} into a cable of {@code type} at {@code pos} that leads back to it. */
    private boolean enters(final long pos, final long from, final PeripheralCableType type) {
        return cableLookup.cableTypeAt(pos).filter(t -> t == type).isPresent()
                && neighborLookup.neighborsOf(pos).contains(from);
    }

    /* How far a run of the cable at {@code pos} reaches: its own reach, or its system's when it says none. */
    private int reach(final long pos, final int systemMax) {
        final int own = reachLookup.reachAt(pos);
        return own > 0 ? own : systemMax;
    }

    // ─── Adjacency helper for tests ─────────────────────────────────────────

    public static INeighborLookup adjacencyFrom(
            final Map<Long, List<Long>> adjacency) {
        return pos -> adjacency.getOrDefault(pos, List.of());
    }
}
