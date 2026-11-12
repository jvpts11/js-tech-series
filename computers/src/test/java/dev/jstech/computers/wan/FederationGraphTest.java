/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.wan;

import dev.jstech.core.jstechseries.network.DataLine;
import dev.jstech.core.jstechseries.network.DataLink;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FederationGraphTest {

    private static final NetworkUuid A = NetworkUuid.random();
    private static final NetworkUuid B = NetworkUuid.random();
    private static final NetworkUuid C = NetworkUuid.random();
    private static final NetworkUuid D = NetworkUuid.random();
    private static final NetworkUuid E = NetworkUuid.random();
    private static final PortSettings RELAYING = PortSettings.NEW.withRelay(true);
    private static final long FAST = 1_000L;

    @Test
    void path_needsBothSidesOfTheLinkUp() {
        assertTrue(new FederationGraph(List.of(side(A, B, PortSettings.NEW, FAST))).path(A, B).isEmpty());
        assertTrue(new FederationGraph(link(A, B, PortSettings.NEW, PortSettings.NEW)).path(A, B).isPresent());
    }

    @Test
    void path_grantsWhatTheReachedNetworksPortAllows() {
        final PortSettings bLetsTake = PortSettings.NEW.with(WanRight.TAKE, true);
        final FederationGraph graph = new FederationGraph(link(A, B, PortSettings.NEW, bLetsTake));
        assertTrue(graph.path(A, B).orElseThrow().allows(WanRight.TAKE), "B's port lets A take");
        assertFalse(graph.path(B, A).orElseThrow().allows(WanRight.TAKE), "A's port does not let B");
    }

    @Test
    void path_goesThroughANetworkOnlyWhenItsPortRelays() {
        final List<FederationEdge> quiet = new ArrayList<>(link(A, B, PortSettings.NEW, PortSettings.NEW));
        quiet.addAll(link(B, C, PortSettings.NEW, PortSettings.NEW));
        assertTrue(new FederationGraph(quiet).path(A, C).isEmpty(), "B relays for nobody");
        final List<FederationEdge> relaying = new ArrayList<>(link(A, B, PortSettings.NEW, RELAYING));
        relaying.addAll(link(B, C, PortSettings.NEW, PortSettings.NEW));
        final FederationPath way = new FederationGraph(relaying).path(A, C).orElseThrow();
        assertEquals(2, way.hops());
        assertEquals(List.of(B), way.relays());
    }

    @Test
    void path_goesOnFromANetworkByTheWayInThatRelaysThoughAnotherReachedItFirst() {
        // D is reached first through B, whose way into D does not relay; the way in from C does, and goes on to E.
        final List<FederationEdge> sides = new ArrayList<>(link(A, B, PortSettings.NEW, RELAYING));
        sides.addAll(link(A, C, PortSettings.NEW, RELAYING));
        sides.addAll(link(B, D, PortSettings.NEW, PortSettings.NEW));
        sides.addAll(link(C, D, PortSettings.NEW, RELAYING));
        sides.addAll(link(D, E, PortSettings.NEW, PortSettings.NEW));
        final FederationPath way = new FederationGraph(sides).path(A, E).orElseThrow();
        assertEquals(3, way.hops());
        assertEquals(List.of(C, D), way.relays());
    }

    @Test
    void path_crossesAtMostThreeLines() {
        final List<FederationEdge> chain = new ArrayList<>(link(A, B, PortSettings.NEW, RELAYING));
        chain.addAll(link(B, C, PortSettings.NEW, RELAYING));
        chain.addAll(link(C, D, PortSettings.NEW, RELAYING));
        chain.addAll(link(D, E, PortSettings.NEW, PortSettings.NEW));
        final FederationGraph graph = new FederationGraph(chain);
        assertEquals(3, graph.path(A, D).orElseThrow().hops());
        assertTrue(graph.path(A, E).isEmpty(), "four lines is too far");
    }

    @Test
    void path_takesTheShortestWayAndIsAsFastAsItsSlowestSide() {
        final List<FederationEdge> sides = new ArrayList<>(link(A, B, PortSettings.NEW, RELAYING));
        sides.addAll(link(B, C, PortSettings.NEW, PortSettings.NEW));
        sides.add(side(A, C, PortSettings.NEW, 10L));
        sides.add(side(C, A, PortSettings.NEW, FAST));
        final Optional<FederationPath> way = new FederationGraph(sides).path(A, C);
        assertEquals(1, way.orElseThrow().hops(), "straight over, not through B");
        assertEquals(10L, way.orElseThrow().speed(), "the slow gateway on A's side");
    }

    /* Both sides of a link between {@code one} and {@code other}, each with its own port's switches. */
    private static List<FederationEdge> link(final NetworkUuid one, final NetworkUuid other,
                                             final PortSettings onesPort, final PortSettings othersPort) {
        return List.of(side(one, other, onesPort, FAST), side(other, one, othersPort, FAST));
    }

    private static FederationEdge side(final NetworkUuid from, final NetworkUuid to, final PortSettings settings,
                                       final long capacity) {
        return new FederationEdge(from, to, from.value().getLeastSignificantBits(), 1,
                to.value().getLeastSignificantBits(), 1, new DataLink(DataLine.LONG_DISTANCE, HardwareEra.STANDARD),
                capacity, settings);
    }
}
