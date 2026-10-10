/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.peripheral;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

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

    /** How many hubs may hang one from another before the last one passes nothing; a guard, never a game rule. */
    private static final int MAX_HUB_DEPTH = 16;

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
         * The endpoint needs a free port of its own kind on the way it arrives by, the owner's own or a hub's; one
         * already linked on that way takes nothing more (idempotent re-establish after periodic validation). Of the
         * ways with room, the one with the fewest cables wins.
         */
        final PortKind kind = endpoint.portKind();
        final IPathSearchResult pathResult = findPath(ownerPos, endpointPos, owner.cableType(), kind,
                hub -> hasRoom(owner, endpointPos, kind, hub));

        return switch (pathResult) {
            case IPathSearchResult.Found(int length, Long hub) -> {
                if (hub == null) {
                    owner.onEndpointLinked(endpointPos, kind);
                } else {
                    owner.onEndpointLinkedThrough(endpointPos, kind, hub);
                }
                endpoint.onOwnerLinked(ownerPos);
                yield new ILinkResult.Established(ownerPos, endpointPos, length);
            }
            case IPathSearchResult.Refused(Long hub) -> hub == null
                    ? new ILinkResult.OwnerAtCapacity(ownerPos, kind, owner.portsInUse(kind), owner.ports(kind))
                    : new ILinkResult.HubAtCapacity(hub, kind, owner.portsInUseThrough(hub), hubPorts(hub));
            case IPathSearchResult.NotFound notFound -> new ILinkResult.NoPathFound(ownerPos, endpointPos);
            case IPathSearchResult.TooLong(int length, int max) ->
                    new ILinkResult.ExceedsMaxLength(ownerPos, endpointPos, length, max);
        };
    }

    /**
     * Whether the endpoint still reaches its owner the way it was linked: straight onto the owner's own port, or
     * through the same hub. A way that changed, a hub put between them or taken away, is a link to make again.
     */
    public boolean isLinkStillValid(
            final long ownerPos,
            final long endpointPos,
            final PeripheralCableType cableType) {
        final PortKind kind = endpointLookup.endpointAt(endpointPos).map(IPeripheralEndpoint::portKind)
                .orElse(PortKind.DEVICE);
        final OptionalLong linkedThrough = ownerLookup.ownerAt(ownerPos).map(owner -> owner.hubOf(endpointPos))
                .orElse(OptionalLong.empty());
        return findPath(ownerPos, endpointPos, cableType, kind, hub -> hub == null
                ? linkedThrough.isEmpty() : linkedThrough.isPresent() && linkedThrough.getAsLong() == hub)
                instanceof IPathSearchResult.Found;
    }

    // ─── BFS internals ──────────────────────────────────────────────────────

    /**
     * Internal sealed result of the path-finding step.
     */
    private sealed interface IPathSearchResult
            permits IPathSearchResult.Found,
            IPathSearchResult.Refused,
            IPathSearchResult.NotFound,
            IPathSearchResult.TooLong {

        /** Reached, {@code length} cables in all, through {@code hub} or straight onto the owner when null. */
        record Found(int length, @Nullable Long hub) implements IPathSearchResult {}

        /** Reached within reach, but only by ways not taken: through {@code hub}, or straight when null. */
        record Refused(@Nullable Long hub) implements IPathSearchResult {}

        record NotFound() implements IPathSearchResult {}

        record TooLong(int length, int max) implements IPathSearchResult {}
    }

    /** Which ways of arriving a search takes: straight onto the owner's own port when {@code hub} is null. */
    @FunctionalInterface
    private interface IWayFilter {
        boolean takes(@Nullable Long hub);
    }

    /**
     * A place the search has come to: how many cables the current run has and how far it reaches, how many cables
     * the whole way has, and the last hub on the way, or null while there is none.
     */
    private record Step(long pos, int run, int limit, int cables, @Nullable Long hub) {
    }

    /*
     * Breadth first from the owner to the target, the first arrival a way filter takes winning. A run ends at a hub,
     * the cable after it counting afresh; an arrival past its run's reach, or by a way not taken, is kept to say why
     * there was no link, the refused way before the too long one.
     */
    private IPathSearchResult findPath(
            final long source,
            final long target,
            final PeripheralCableType requiredType,
            final PortKind kind,
            final IWayFilter ways) {

        final int maxLength = requiredType.maxLength();
        final Map<Long, Map<Long, List<int[]>>> seen = new HashMap<>();
        final Deque<Step> queue = new ArrayDeque<>();
        IPathSearchResult missed = new IPathSearchResult.NotFound();

        // The source may be a multiblock owner: seed BFS from every face of every block it stands on.
        final Set<Long> sources = ownerLookup.ownerAt(source)
                .map(owner -> owner.occupiedPositions(source))
                .filter(positions -> !positions.isEmpty())
                .orElseGet(() -> Set.of(source));
        for (final long src : sources) {
            arrives(seen, src, null, 0, Integer.MAX_VALUE);
            queue.addLast(new Step(src, 0, Integer.MAX_VALUE, 0, null));
        }

        while (!queue.isEmpty()) {
            final Step at = queue.pollFirst();
            for (final long neighbor : neighborLookup.neighborsOf(at.pos())) {
                if (neighbor == target) {
                    if (at.run() > at.limit()) {
                        if (missed instanceof IPathSearchResult.NotFound) {
                            missed = new IPathSearchResult.TooLong(at.run(), at.limit());
                        }
                    } else if (ways.takes(at.hub())) {
                        return new IPathSearchResult.Found(at.cables(), at.hub());
                    } else if (!(missed instanceof IPathSearchResult.Refused)) {
                        missed = new IPathSearchResult.Refused(at.hub());
                    }
                    continue;
                }
                /*
                 * A place is visited once per state that could still do better, not once in all: the same cable
                 * reached over its reach straight from the owner must not hide the legal way to it past a hub, where
                 * the run counts afresh.
                 */
                if (enters(neighbor, at.pos(), requiredType)) {
                    final int limit = Math.min(at.limit(), reach(neighbor, maxLength));
                    if (arrives(seen, neighbor, at.hub(), at.run() + 1, limit)) {
                        queue.addLast(new Step(neighbor, at.run() + 1, limit, at.cables() + 1, at.hub()));
                    }
                } else if (at.run() <= at.limit() && passesThrough(neighbor, at.pos(), source, target, kind)
                        && arrives(seen, neighbor, neighbor, 0, Integer.MAX_VALUE)) {
                    queue.addLast(new Step(neighbor, 0, Integer.MAX_VALUE, at.cables(), neighbor));
                }
            }
        }

        return missed;
    }

    /*
     * Records an arrival at {@code pos} by way of {@code hub} with {@code run} cables in the current run under a reach
     * of {@code limit}, and says whether it is worth following: not when an earlier arrival at the same place by the
     * same hub had a run no longer and a reach no shorter, since it can do everything this one could.
     */
    private static boolean arrives(final Map<Long, Map<Long, List<int[]>>> seen, final long pos,
                                   @Nullable final Long hub, final int run, final int limit) {
        final List<int[]> states = seen.computeIfAbsent(pos, p -> new HashMap<>())
                .computeIfAbsent(hub, h -> new ArrayList<>());
        for (final int[] state : states) {
            if (state[0] <= run && state[1] >= limit) {
                return false;
            }
        }
        states.add(new int[] {run, limit});
        return true;
    }

    /* Whether a path steps from {@code from} into a cable of {@code type} at {@code pos} that leads back to it. */
    private boolean enters(final long pos, final long from, final PeripheralCableType type) {
        return cableLookup.cableTypeAt(pos).filter(t -> t == type).isPresent()
                && neighborLookup.neighborsOf(pos).contains(from);
    }

    /*
     * Whether a path from {@code from} goes on through a hub at {@code pos}: a hub linked to this owner, passing the
     * target's kind, touching {@code from}, and not itself hanging from the target, which would let two hubs hold each
     * other up with nothing behind them.
     */
    private boolean passesThrough(final long pos, final long from, final long owner, final long target,
                                  final PortKind kind) {
        final Optional<IPeripheralEndpoint> at = endpointLookup.endpointAt(pos);
        if (at.isEmpty() || !(at.get() instanceof IPeripheralHub hub) || !hub.passes(kind)
                || !hub.linkedOwner().map(o -> o == owner).orElse(false)
                || !neighborLookup.neighborsOf(pos).contains(from)) {
            return false;
        }
        final Optional<IPeripheralOwner> linkedTo = ownerLookup.ownerAt(owner);
        long up = pos;
        for (int depth = 0; depth < MAX_HUB_DEPTH && linkedTo.isPresent(); depth++) {
            final OptionalLong next = linkedTo.get().hubOf(up);
            if (next.isEmpty()) {
                return true;
            }
            up = next.getAsLong();
            if (up == target) {
                return false;
            }
        }
        // No owner to ask, or hubs hung deeper than any real chain: nothing goes through.
        return false;
    }

    /*
     * Whether the endpoint has a port on the way it would arrive by: a free one of the owner's own when {@code hub}
     * is null, else a free one of that hub's; or it already holds a port on that way.
     */
    private boolean hasRoom(final IPeripheralOwner owner, final long endpointPos, final PortKind kind,
                            @Nullable final Long hub) {
        final OptionalLong now = owner.hubOf(endpointPos);
        final boolean linked = owner.isLinked(endpointPos);
        if (hub == null) {
            return (linked && now.isEmpty()) || owner.portsInUse(kind) < owner.ports(kind);
        }
        return (linked && now.isPresent() && now.getAsLong() == hub) || owner.portsInUseThrough(hub) < hubPorts(hub);
    }

    /* How many ports the hub at {@code pos} has; 0 when there is none there. */
    private int hubPorts(final long pos) {
        return endpointLookup.endpointAt(pos).filter(IPeripheralHub.class::isInstance)
                .map(e -> ((IPeripheralHub) e).hubPorts()).orElse(0);
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
