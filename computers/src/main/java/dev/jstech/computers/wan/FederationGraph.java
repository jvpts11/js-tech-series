/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.wan;

import dev.jstech.core.uuid.NetworkUuid;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The networks of a world and their federations, as the sides of their links stand this tick, and the ways between
 * them (computers/docs/design/network.md).
 *
 * <p>Two networks are federated while both sides of a link between them are up. A network reaches one it is not
 * federated with only through a network between that relays for it, which it does when its port facing the asking
 * side has its relay switch on; the way is the shortest by networks, never comes back to one, and crosses at most
 * {@link #MOST_HOPS} lines. What the asking network may do on the one it reaches is that network's to say, on its port
 * the way comes in by.
 */
public final class FederationGraph {

    /* Each network's sides, by the network at the far end; of two links between the same two, the faster. */
    private final Map<NetworkUuid, Map<NetworkUuid, FederationEdge>> sides = new HashMap<>();

    /** The most lines a way crosses: two networks relaying between the asking one and the one it reaches. */
    public static final int MOST_HOPS = 3;

    public FederationGraph(final Collection<FederationEdge> edges) {
        for (final FederationEdge edge : edges) {
            sides.computeIfAbsent(edge.from(), key -> new LinkedHashMap<>())
                    .merge(edge.to(), edge, (one, other) -> other.speed() > one.speed() ? other : one);
        }
    }

    /** The side of the link {@code from} has toward {@code to}, while both sides are up; empty otherwise. */
    public Optional<FederationEdge> side(final NetworkUuid from, final NetworkUuid to) {
        final FederationEdge out = sides.getOrDefault(from, Map.of()).get(to);
        final FederationEdge back = sides.getOrDefault(to, Map.of()).get(from);
        return out != null && back != null ? Optional.of(out) : Optional.empty();
    }

    /** The way from {@code from} to {@code to}, or empty when there is none it may take. */
    public Optional<FederationPath> path(final NetworkUuid from, final NetworkUuid to) {
        for (final FederationPath path : reachable(from)) {
            if (path.target().equals(to)) {
                return Optional.of(path);
            }
        }
        return Optional.empty();
    }

    /** Every network {@code from} reaches, each by its shortest way, the nearest first. */
    public List<FederationPath> reachable(final NetworkUuid from) {
        final Map<NetworkUuid, FederationPath> found = new LinkedHashMap<>();
        final Deque<FederationPath> queue = new ArrayDeque<>();
        queue.add(new FederationPath(List.of(from), List.of(), PortSettings.NEW, Long.MAX_VALUE));
        while (!queue.isEmpty()) {
            final FederationPath way = queue.poll();
            final NetworkUuid at = way.target();
            if (way.hops() >= MOST_HOPS || !mayLeave(way)) {
                continue;
            }
            for (final NetworkUuid next : sides.getOrDefault(at, Map.of()).keySet()) {
                if (way.networks().contains(next) || found.containsKey(next)) {
                    continue;
                }
                final Optional<FederationEdge> out = side(at, next);
                final FederationEdge in = sides.getOrDefault(next, Map.of()).get(at);
                if (out.isEmpty() || in == null) {
                    continue;
                }
                final List<NetworkUuid> networks = new ArrayList<>(way.networks());
                networks.add(next);
                final List<FederationEdge> edges = new ArrayList<>(way.out());
                edges.add(out.get());
                final long speed = Math.min(way.speed(), Math.min(out.get().speed(), in.speed()));
                final FederationPath longer = new FederationPath(networks, edges, in.settings(), speed);
                found.put(next, longer);
                queue.add(longer);
            }
        }
        return List.copyOf(found.values());
    }

    /*
     * Whether a way may go on from the network it has reached: always from the asking network itself, and from one in
     * between only when that network's port it came in by relays.
     */
    private boolean mayLeave(final FederationPath way) {
        if (way.hops() == 0) {
            return true;
        }
        final NetworkUuid at = way.target();
        final NetworkUuid before = way.networks().get(way.networks().size() - 2);
        final FederationEdge in = sides.getOrDefault(at, Map.of()).get(before);
        return in != null && in.settings().relay();
    }
}
