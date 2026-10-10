/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.peripheral;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PeripheralLinkValidatorTest {

    // Arbitrary deterministic positions.
    private static final long OWNER_POS = 1_000_000L;
    private static final long ENDPOINT_POS = 2_000_000L;
    private static final long C1 = 10L;
    private static final long C2 = 20L;
    private static final long C3 = 30L;
    private static final long C4 = 40L;
    private static final long HUB_POS = 3_000_000L;

    @Test
    void ownerAdjacentToEndpoint_zeroCablePathSucceeds() {
        TestOwner owner = new TestOwner(PeripheralCableType.COMPUTING, 8);
        TestEndpoint endpoint = new TestEndpoint(PeripheralCableType.COMPUTING);
        Map<Long, List<Long>> adj = new HashMap<>();
        adj.put(OWNER_POS, List.of(ENDPOINT_POS));
        adj.put(ENDPOINT_POS, List.of(OWNER_POS));

        PeripheralLinkValidator validator = build(adj, owner, endpoint, Map.of());
        ILinkResult result = validator.tryEstablishLink(OWNER_POS, ENDPOINT_POS);

        ILinkResult.Established e = assertInstanceOf(ILinkResult.Established.class, result);
        assertEquals(0, e.pathLength());
        assertTrue(owner.linkedEndpoints().contains(ENDPOINT_POS));
        assertEquals(Optional.of(OWNER_POS), endpoint.linkedOwner());
    }

    @Test
    void singleCablePath_succeedsWithLength1() {
        TestOwner owner = new TestOwner(PeripheralCableType.COMPUTING, 8);
        TestEndpoint endpoint = new TestEndpoint(PeripheralCableType.COMPUTING);
        Map<Long, List<Long>> adj = new HashMap<>();
        adj.put(OWNER_POS, List.of(C1));
        adj.put(C1, List.of(OWNER_POS, ENDPOINT_POS));
        adj.put(ENDPOINT_POS, List.of(C1));

        Map<Long, PeripheralCableType> cables = Map.of(
                C1, PeripheralCableType.COMPUTING);

        PeripheralLinkValidator validator = build(adj, owner, endpoint, cables);
        ILinkResult result = validator.tryEstablishLink(OWNER_POS, ENDPOINT_POS);

        ILinkResult.Established e = assertInstanceOf(ILinkResult.Established.class, result);
        assertEquals(1, e.pathLength());
    }

    @Test
    void pathWithinMaxLength_succeeds() {
        // 3-cable path for COMPUTING (max 16), well within budget.
        TestOwner owner = new TestOwner(PeripheralCableType.COMPUTING, 8);
        TestEndpoint endpoint = new TestEndpoint(PeripheralCableType.COMPUTING);

        Map<Long, List<Long>> adj = new HashMap<>();
        adj.put(OWNER_POS, List.of(C1));
        adj.put(C1, List.of(OWNER_POS, C2));
        adj.put(C2, List.of(C1, C3));
        adj.put(C3, List.of(C2, ENDPOINT_POS));
        adj.put(ENDPOINT_POS, List.of(C3));

        Map<Long, PeripheralCableType> cables = Map.of(
                C1, PeripheralCableType.COMPUTING,
                C2, PeripheralCableType.COMPUTING,
                C3, PeripheralCableType.COMPUTING);

        PeripheralLinkValidator validator = build(adj, owner, endpoint, cables);
        ILinkResult result = validator.tryEstablishLink(OWNER_POS, ENDPOINT_POS);

        ILinkResult.Established e = assertInstanceOf(ILinkResult.Established.class, result);
        assertEquals(3, e.pathLength());
    }

    @Test
    void pathExceedsMaxLength_returnsTooLong() {
        // INDUSTRIAL_CONTROL has max=8. We build a 10-hop path.
        TestOwner owner = new TestOwner(PeripheralCableType.INDUSTRIAL_CONTROL, 5);
        TestEndpoint endpoint = new TestEndpoint(PeripheralCableType.INDUSTRIAL_CONTROL);

        Map<Long, List<Long>> adj = new HashMap<>();
        Map<Long, PeripheralCableType> cables = new HashMap<>();
        long prev = OWNER_POS;
        adj.put(OWNER_POS, new ArrayList<>());
        for (int i = 1; i <= 10; i++) {
            long cable = 1000L + i;
            adj.computeIfAbsent(prev, k -> new ArrayList<>()).add(cable);
            adj.computeIfAbsent(cable, k -> new ArrayList<>()).add(prev);
            cables.put(cable, PeripheralCableType.INDUSTRIAL_CONTROL);
            prev = cable;
        }
        adj.computeIfAbsent(prev, k -> new ArrayList<>()).add(ENDPOINT_POS);
        adj.put(ENDPOINT_POS, List.of(prev));

        PeripheralLinkValidator validator = build(adj, owner, endpoint, cables);
        ILinkResult result = validator.tryEstablishLink(OWNER_POS, ENDPOINT_POS);

        ILinkResult.ExceedsMaxLength too = assertInstanceOf(
                ILinkResult.ExceedsMaxLength.class, result);
        assertEquals(10, too.pathLength());
        assertEquals(8, too.maxAllowed());
    }

    @Test
    void mismatchedCableTypes_returnsCableTypeMismatch() {
        // Owner declares COMPUTING, endpoint declares TELEMETRY.
        TestOwner owner = new TestOwner(PeripheralCableType.COMPUTING, 8);
        TestEndpoint endpoint = new TestEndpoint(PeripheralCableType.TELEMETRY);

        Map<Long, List<Long>> adj = Map.of(
                OWNER_POS, List.of(ENDPOINT_POS),
                ENDPOINT_POS, List.of(OWNER_POS));

        PeripheralLinkValidator validator = build(adj, owner, endpoint, Map.of());
        ILinkResult result = validator.tryEstablishLink(OWNER_POS, ENDPOINT_POS);

        ILinkResult.CableTypeMismatch m = assertInstanceOf(
                ILinkResult.CableTypeMismatch.class, result);
        assertEquals(PeripheralCableType.COMPUTING, m.expected());
        assertEquals(PeripheralCableType.TELEMETRY, m.actual());
    }

    @Test
    void bfs_ignoresCablesOfWrongType() {
        /*
         * Path is OWNER -- C1(TELEMETRY) -- ENDPOINT, but owner needs COMPUTING.
         * Path should fail because BFS won't traverse the TELEMETRY cable.
         */
        TestOwner owner = new TestOwner(PeripheralCableType.COMPUTING, 8);
        TestEndpoint endpoint = new TestEndpoint(PeripheralCableType.COMPUTING);

        Map<Long, List<Long>> adj = Map.of(
                OWNER_POS, List.of(C1),
                C1, List.of(OWNER_POS, ENDPOINT_POS),
                ENDPOINT_POS, List.of(C1));
        Map<Long, PeripheralCableType> cables = Map.of(
                C1, PeripheralCableType.TELEMETRY);

        PeripheralLinkValidator validator = build(adj, owner, endpoint, cables);
        ILinkResult result = validator.tryEstablishLink(OWNER_POS, ENDPOINT_POS);

        assertInstanceOf(ILinkResult.NoPathFound.class, result);
    }

    @Test
    void endpointAlreadyLinkedToDifferentOwner_returnsAlreadyLinked() {
        TestOwner owner = new TestOwner(PeripheralCableType.COMPUTING, 8);
        TestEndpoint endpoint = new TestEndpoint(PeripheralCableType.COMPUTING);
        endpoint.onOwnerLinked(99_999_999L); // pre-linked to a fake owner

        Map<Long, List<Long>> adj = Map.of(
                OWNER_POS, List.of(ENDPOINT_POS),
                ENDPOINT_POS, List.of(OWNER_POS));

        PeripheralLinkValidator validator = build(adj, owner, endpoint, Map.of());
        ILinkResult result = validator.tryEstablishLink(OWNER_POS, ENDPOINT_POS);

        ILinkResult.AlreadyLinked al = assertInstanceOf(
                ILinkResult.AlreadyLinked.class, result);
        assertEquals(99_999_999L, al.existingOwnerPos());
    }

    @Test
    void ownerAtMaxEndpoints_returnsOwnerAtCapacity() {
        TestOwner owner = new TestOwner(PeripheralCableType.COMPUTING, 2);
        owner.onEndpointLinked(50L, PortKind.DEVICE);
        owner.onEndpointLinked(60L, PortKind.DEVICE);
        TestEndpoint endpoint = new TestEndpoint(PeripheralCableType.COMPUTING);

        Map<Long, List<Long>> adj = Map.of(
                OWNER_POS, List.of(ENDPOINT_POS),
                ENDPOINT_POS, List.of(OWNER_POS));

        PeripheralLinkValidator validator = build(adj, owner, endpoint, Map.of());
        ILinkResult result = validator.tryEstablishLink(OWNER_POS, ENDPOINT_POS);

        ILinkResult.OwnerAtCapacity cap = assertInstanceOf(
                ILinkResult.OwnerAtCapacity.class, result);
        assertEquals(PortKind.DEVICE, cap.kind());
        assertEquals(2, cap.currentCount());
        assertEquals(2, cap.maxAllowed());
    }

    @Test
    void cableReach_limitsThePathBelowItsSystem() {
        // Three cables of a run that reaches two: the path is too long, however far the system would go.
        TestOwner owner = new TestOwner(PeripheralCableType.COMPUTING, 8);
        TestEndpoint endpoint = new TestEndpoint(PeripheralCableType.COMPUTING);
        Map<Long, List<Long>> adj = Map.of(
                OWNER_POS, List.of(C1),
                C1, List.of(OWNER_POS, C2),
                C2, List.of(C1, C3),
                C3, List.of(C2, ENDPOINT_POS),
                ENDPOINT_POS, List.of(C3));
        Map<Long, PeripheralCableType> cables = Map.of(
                C1, PeripheralCableType.COMPUTING,
                C2, PeripheralCableType.COMPUTING,
                C3, PeripheralCableType.COMPUTING);

        ILinkResult result = build(adj, owner, endpoint, cables, pos -> 2).tryEstablishLink(OWNER_POS, ENDPOINT_POS);

        ILinkResult.ExceedsMaxLength too = assertInstanceOf(ILinkResult.ExceedsMaxLength.class, result);
        assertEquals(3, too.pathLength());
        assertEquals(2, too.maxAllowed());
    }

    @Test
    void cableReach_canGoPastItsSystemsLimit() {
        // A run of twenty cables that reaches twenty links, past the system's sixteen.
        TestOwner owner = new TestOwner(PeripheralCableType.COMPUTING, 8);
        TestEndpoint endpoint = new TestEndpoint(PeripheralCableType.COMPUTING);
        Map<Long, List<Long>> adj = new HashMap<>();
        Map<Long, PeripheralCableType> cables = new HashMap<>();
        long prev = OWNER_POS;
        for (int i = 1; i <= 20; i++) {
            long cable = 1000L + i;
            adj.computeIfAbsent(prev, k -> new ArrayList<>()).add(cable);
            adj.computeIfAbsent(cable, k -> new ArrayList<>()).add(prev);
            cables.put(cable, PeripheralCableType.COMPUTING);
            prev = cable;
        }
        adj.computeIfAbsent(prev, k -> new ArrayList<>()).add(ENDPOINT_POS);
        adj.put(ENDPOINT_POS, List.of(prev));

        ILinkResult result = build(adj, owner, endpoint, cables, pos -> 20).tryEstablishLink(OWNER_POS, ENDPOINT_POS);

        assertEquals(20, assertInstanceOf(ILinkResult.Established.class, result).pathLength());
    }

    @Test
    void cableThatDoesNotLeadBack_isNoPath() {
        // The cable beside the owner does not cross toward it (its wire takes no plug there): no path through it.
        TestOwner owner = new TestOwner(PeripheralCableType.COMPUTING, 8);
        TestEndpoint endpoint = new TestEndpoint(PeripheralCableType.COMPUTING);
        Map<Long, List<Long>> adj = Map.of(
                OWNER_POS, List.of(C1),
                C1, List.of(ENDPOINT_POS),
                ENDPOINT_POS, List.of(C1));

        ILinkResult result = build(adj, owner, endpoint, Map.of(C1, PeripheralCableType.COMPUTING))
                .tryEstablishLink(OWNER_POS, ENDPOINT_POS);

        assertInstanceOf(ILinkResult.NoPathFound.class, result);
    }

    @Test
    void portsFull_ofOneKind_leaveTheOtherKindsFree() {
        // Both device ports are taken, and a screen still finds its video output.
        TestOwner owner = new TestOwner(PeripheralCableType.COMPUTING, 2).with(PortKind.VIDEO, 1);
        owner.onEndpointLinked(50L, PortKind.DEVICE);
        owner.onEndpointLinked(60L, PortKind.DEVICE);
        TestEndpoint screen = new TestEndpoint(PeripheralCableType.COMPUTING, PortKind.VIDEO);

        ILinkResult result = build(adjacent(), owner, screen, Map.of()).tryEstablishLink(OWNER_POS, ENDPOINT_POS);

        assertInstanceOf(ILinkResult.Established.class, result);
        assertEquals(1, owner.portsInUse(PortKind.VIDEO));
        assertEquals(2, owner.portsInUse(PortKind.DEVICE));
    }

    @Test
    void ownerWithoutAPortOfTheKind_refusesTheEndpoint() {
        // Plenty of device ports, but no video output: a screen is not linked.
        TestOwner owner = new TestOwner(PeripheralCableType.COMPUTING, 8);
        TestEndpoint screen = new TestEndpoint(PeripheralCableType.COMPUTING, PortKind.VIDEO);

        ILinkResult result = build(adjacent(), owner, screen, Map.of()).tryEstablishLink(OWNER_POS, ENDPOINT_POS);

        ILinkResult.OwnerAtCapacity cap = assertInstanceOf(ILinkResult.OwnerAtCapacity.class, result);
        assertEquals(PortKind.VIDEO, cap.kind());
        assertEquals(0, cap.maxAllowed());
        assertTrue(owner.linkedEndpoints().isEmpty());
    }

    @Test
    void holdsPort_isLostByTheLastLinkedWhenPortsDrop() {
        TestOwner owner = new TestOwner(PeripheralCableType.COMPUTING, 8).with(PortKind.VIDEO, 2);
        owner.onEndpointLinked(50L, PortKind.VIDEO);
        owner.onEndpointLinked(60L, PortKind.VIDEO);
        assertTrue(owner.holdsPort(60L));

        // A card with one output taken out: the screen linked first keeps its port.
        owner.with(PortKind.VIDEO, 1);

        assertTrue(owner.holdsPort(50L));
        assertFalse(owner.holdsPort(60L));
        assertFalse(owner.holdsPort(70L));
    }

    @Test
    void reLinkSameEndpoint_atCapacity_succeeds() {
        /*
         * If endpoint is already linked to this owner, link is idempotent
         * and should succeed even when capacity is full.
         */
        TestOwner owner = new TestOwner(PeripheralCableType.COMPUTING, 2);
        owner.onEndpointLinked(ENDPOINT_POS, PortKind.DEVICE); // pre-linked
        owner.onEndpointLinked(60L, PortKind.DEVICE);          // at capacity now
        TestEndpoint endpoint = new TestEndpoint(PeripheralCableType.COMPUTING);
        endpoint.onOwnerLinked(OWNER_POS);    // matching the existing link

        Map<Long, List<Long>> adj = Map.of(
                OWNER_POS, List.of(ENDPOINT_POS),
                ENDPOINT_POS, List.of(OWNER_POS));

        PeripheralLinkValidator validator = build(adj, owner, endpoint, Map.of());
        ILinkResult result = validator.tryEstablishLink(OWNER_POS, ENDPOINT_POS);

        assertInstanceOf(ILinkResult.Established.class, result);
    }

    @Test
    void differentCableTypes_coexistInSameWorld() {
        // Two parallel paths exist from OWNER to ENDPOINT:
        TestOwner owner = new TestOwner(PeripheralCableType.COMPUTING, 8);
        TestEndpoint endpoint = new TestEndpoint(PeripheralCableType.COMPUTING);

        Map<Long, List<Long>> adj = Map.of(
                OWNER_POS, List.of(C1, C2),
                C1, List.of(OWNER_POS, ENDPOINT_POS),
                C2, List.of(OWNER_POS, ENDPOINT_POS),
                ENDPOINT_POS, List.of(C1, C2));
        Map<Long, PeripheralCableType> cables = Map.of(
                C1, PeripheralCableType.COMPUTING,
                C2, PeripheralCableType.TELEMETRY);

        PeripheralLinkValidator validator = build(adj, owner, endpoint, cables);
        ILinkResult result = validator.tryEstablishLink(OWNER_POS, ENDPOINT_POS);

        ILinkResult.Established e = assertInstanceOf(ILinkResult.Established.class, result);
        assertEquals(1, e.pathLength());
    }

    @Test
    void isLinkStillValid_returnsTrueWhenPathExists() {
        Map<Long, List<Long>> adj = Map.of(
                OWNER_POS, List.of(C1),
                C1, List.of(OWNER_POS, ENDPOINT_POS),
                ENDPOINT_POS, List.of(C1));
        Map<Long, PeripheralCableType> cables = Map.of(
                C1, PeripheralCableType.COMPUTING);

        PeripheralLinkValidator validator = build(
                adj,
                new TestOwner(PeripheralCableType.COMPUTING, 8),
                new TestEndpoint(PeripheralCableType.COMPUTING),
                cables);

        assertTrue(validator.isLinkStillValid(
                OWNER_POS, ENDPOINT_POS, PeripheralCableType.COMPUTING));
    }

    @Test
    void isLinkStillValid_returnsFalseWhenPathBroken() {
        // Adjacency exists but no cable connects them.
        Map<Long, List<Long>> adj = Map.of(
                OWNER_POS, List.of(C1),
                C1, List.of(OWNER_POS),
                ENDPOINT_POS, List.of());

        PeripheralLinkValidator validator = build(
                adj,
                new TestOwner(PeripheralCableType.COMPUTING, 8),
                new TestEndpoint(PeripheralCableType.COMPUTING),
                Map.of(C1, PeripheralCableType.COMPUTING));

        assertFalse(validator.isLinkStillValid(
                OWNER_POS, ENDPOINT_POS, PeripheralCableType.COMPUTING));
    }

    @Test
    void telemetryCable_supportsLongPaths() {
        // Build a 50-hop path of TELEMETRY cables, well under max 256.
        TestOwner owner = new TestOwner(PeripheralCableType.TELEMETRY, 6);
        TestEndpoint endpoint = new TestEndpoint(PeripheralCableType.TELEMETRY);

        Map<Long, List<Long>> adj = new HashMap<>();
        Map<Long, PeripheralCableType> cables = new HashMap<>();
        adj.put(OWNER_POS, new ArrayList<>());
        long prev = OWNER_POS;
        for (int i = 1; i <= 50; i++) {
            long cable = 1000L + i;
            adj.computeIfAbsent(prev, k -> new ArrayList<>()).add(cable);
            adj.computeIfAbsent(cable, k -> new ArrayList<>()).add(prev);
            cables.put(cable, PeripheralCableType.TELEMETRY);
            prev = cable;
        }
        adj.computeIfAbsent(prev, k -> new ArrayList<>()).add(ENDPOINT_POS);
        adj.put(ENDPOINT_POS, List.of(prev));

        PeripheralLinkValidator validator = build(adj, owner, endpoint, cables);
        ILinkResult result = validator.tryEstablishLink(OWNER_POS, ENDPOINT_POS);

        ILinkResult.Established e = assertInstanceOf(ILinkResult.Established.class, result);
        assertEquals(50, e.pathLength());
    }

    @Test
    void hub_givesItsPortsToTheDevicesCabledToIt() {
        // The owner's one device port is the hub's; the device behind the hub takes one of the hub's.
        TestOwner owner = new TestOwner(PeripheralCableType.COMPUTING, 1);
        TestHub hub = linkedHub(owner, 4);
        TestEndpoint device = new TestEndpoint(PeripheralCableType.COMPUTING);
        Map<Long, List<Long>> adj = Map.of(
                OWNER_POS, List.of(HUB_POS),
                HUB_POS, List.of(OWNER_POS, C1),
                C1, List.of(HUB_POS, ENDPOINT_POS),
                ENDPOINT_POS, List.of(C1));

        ILinkResult result = build(adj, owner, Map.of(HUB_POS, hub, ENDPOINT_POS, device),
                Map.of(C1, PeripheralCableType.COMPUTING), pos -> 0).tryEstablishLink(OWNER_POS, ENDPOINT_POS);

        assertEquals(1, assertInstanceOf(ILinkResult.Established.class, result).pathLength());
        assertEquals(OptionalLong.of(HUB_POS), owner.hubOf(ENDPOINT_POS));
        assertEquals(1, owner.portsInUseThrough(HUB_POS));
        assertEquals(1, owner.portsInUse(PortKind.DEVICE));
    }

    @Test
    void hubNotLinked_passesNothing() {
        TestOwner owner = new TestOwner(PeripheralCableType.COMPUTING, 8);
        TestHub hub = new TestHub(4);
        TestEndpoint device = new TestEndpoint(PeripheralCableType.COMPUTING);

        ILinkResult result = build(throughHub(), owner, Map.of(HUB_POS, hub, ENDPOINT_POS, device), Map.of(),
                pos -> 0).tryEstablishLink(OWNER_POS, ENDPOINT_POS);

        assertInstanceOf(ILinkResult.NoPathFound.class, result);
    }

    @Test
    void hubFull_refusesTheDevice() {
        TestOwner owner = new TestOwner(PeripheralCableType.COMPUTING, 8);
        TestHub hub = linkedHub(owner, 1);
        owner.onEndpointLinkedThrough(77L, PortKind.DEVICE, HUB_POS);
        TestEndpoint device = new TestEndpoint(PeripheralCableType.COMPUTING);

        ILinkResult result = build(throughHub(), owner, Map.of(HUB_POS, hub, ENDPOINT_POS, device), Map.of(),
                pos -> 0).tryEstablishLink(OWNER_POS, ENDPOINT_POS);

        ILinkResult.HubAtCapacity full = assertInstanceOf(ILinkResult.HubAtCapacity.class, result);
        assertEquals(HUB_POS, full.hubPos());
        assertEquals(1, full.currentCount());
        assertEquals(1, full.maxAllowed());
    }

    @Test
    void hubFull_letsTheDeviceTakeALongerWayWithRoom() {
        // Against the full hub, and two cables from the owner, which has a free port: the cables win.
        TestOwner owner = new TestOwner(PeripheralCableType.COMPUTING, 8);
        TestHub hub = linkedHub(owner, 1);
        owner.onEndpointLinkedThrough(77L, PortKind.DEVICE, HUB_POS);
        TestEndpoint device = new TestEndpoint(PeripheralCableType.COMPUTING);
        Map<Long, List<Long>> adj = Map.of(
                OWNER_POS, List.of(HUB_POS, C1),
                HUB_POS, List.of(OWNER_POS, ENDPOINT_POS),
                C1, List.of(OWNER_POS, C2),
                C2, List.of(C1, ENDPOINT_POS),
                ENDPOINT_POS, List.of(HUB_POS, C2));
        Map<Long, PeripheralCableType> cables = Map.of(C1, PeripheralCableType.COMPUTING,
                C2, PeripheralCableType.COMPUTING);

        ILinkResult result = build(adj, owner, Map.of(HUB_POS, hub, ENDPOINT_POS, device), cables, pos -> 0)
                .tryEstablishLink(OWNER_POS, ENDPOINT_POS);

        assertEquals(2, assertInstanceOf(ILinkResult.Established.class, result).pathLength());
        assertEquals(OptionalLong.empty(), owner.hubOf(ENDPOINT_POS));
    }

    @Test
    void hub_passesNoScreen() {
        TestOwner owner = new TestOwner(PeripheralCableType.COMPUTING, 8).with(PortKind.VIDEO, 2);
        TestHub hub = linkedHub(owner, 4);
        TestEndpoint screen = new TestEndpoint(PeripheralCableType.COMPUTING, PortKind.VIDEO);

        ILinkResult result = build(throughHub(), owner, Map.of(HUB_POS, hub, ENDPOINT_POS, screen), Map.of(),
                pos -> 0).tryEstablishLink(OWNER_POS, ENDPOINT_POS);

        assertInstanceOf(ILinkResult.NoPathFound.class, result);
    }

    @Test
    void hub_startsTheReachAgain() {
        // Two cables, the hub, two more: each run within a reach of two, which four cables in one run are not.
        TestOwner owner = new TestOwner(PeripheralCableType.COMPUTING, 8);
        TestHub hub = linkedHub(owner, 4);
        TestEndpoint device = new TestEndpoint(PeripheralCableType.COMPUTING);
        Map<Long, List<Long>> adj = Map.of(
                OWNER_POS, List.of(C1),
                C1, List.of(OWNER_POS, C2),
                C2, List.of(C1, HUB_POS),
                HUB_POS, List.of(C2, C3),
                C3, List.of(HUB_POS, C4),
                C4, List.of(C3, ENDPOINT_POS),
                ENDPOINT_POS, List.of(C4));
        Map<Long, PeripheralCableType> cables = Map.of(C1, PeripheralCableType.COMPUTING,
                C2, PeripheralCableType.COMPUTING, C3, PeripheralCableType.COMPUTING,
                C4, PeripheralCableType.COMPUTING);

        ILinkResult result = build(adj, owner, Map.of(HUB_POS, hub, ENDPOINT_POS, device), cables, pos -> 2)
                .tryEstablishLink(OWNER_POS, ENDPOINT_POS);

        assertEquals(4, assertInstanceOf(ILinkResult.Established.class, result).pathLength());
    }

    @Test
    void hub_routeIsFoundEvenWhenTheDirectArrivalAtASharedCableIsOverReach() {
        // Cables 1..9 in a line from the owner, the device beside the ninth; a hub beside the first, with cables
        // 21..27 from it to the ninth. Straight along the line the ninth cable is a run of nine, over a reach of
        // eight; through the hub the run counts afresh and reaches it legally.
        TestOwner owner = new TestOwner(PeripheralCableType.COMPUTING, 8);
        TestHub hub = linkedHub(owner, 4);
        TestEndpoint device = new TestEndpoint(PeripheralCableType.COMPUTING);
        Map<Long, List<Long>> adj = new HashMap<>();
        Map<Long, PeripheralCableType> cables = new HashMap<>();
        adj.put(OWNER_POS, List.of(101L));
        for (long i = 1; i <= 9; i++) {
            cables.put(100L + i, PeripheralCableType.COMPUTING);
        }
        adj.put(101L, List.of(OWNER_POS, 102L, HUB_POS));
        for (long i = 2; i <= 8; i++) {
            adj.put(100L + i, List.of(99L + i, 101L + i));
        }
        adj.put(109L, List.of(108L, 207L, ENDPOINT_POS));
        adj.put(HUB_POS, List.of(101L, 201L));
        for (long i = 1; i <= 7; i++) {
            cables.put(200L + i, PeripheralCableType.COMPUTING);
            adj.put(200L + i, List.of(i == 1 ? HUB_POS : 199L + i, i == 7 ? 109L : 201L + i));
        }
        adj.put(ENDPOINT_POS, List.of(109L));

        ILinkResult result = build(adj, owner, Map.of(HUB_POS, hub, ENDPOINT_POS, device), cables, pos -> 8)
                .tryEstablishLink(OWNER_POS, ENDPOINT_POS);

        assertInstanceOf(ILinkResult.Established.class, result);
        assertEquals(HUB_POS, owner.hubOf(ENDPOINT_POS).getAsLong());
    }

    @Test
    void hubHangingFromTheTarget_isNoWayToIt() {
        // The only hub on the way is recorded as hanging from the very hub being linked: it cannot carry it.
        TestOwner owner = new TestOwner(PeripheralCableType.COMPUTING, 8);
        TestHub hub = new TestHub(4);
        hub.onOwnerLinked(OWNER_POS);
        owner.onEndpointLinkedThrough(HUB_POS, PortKind.DEVICE, ENDPOINT_POS);
        TestHub target = new TestHub(4);

        ILinkResult result = build(throughHub(), owner, Map.of(HUB_POS, hub, ENDPOINT_POS, target), Map.of(),
                pos -> 0).tryEstablishLink(OWNER_POS, ENDPOINT_POS);

        assertInstanceOf(ILinkResult.NoPathFound.class, result);
    }

    @Test
    void isLinkStillValid_isFalseOnceAHubStandsInTheWay() {
        // Linked straight onto the owner's port, and now the only way runs through a hub: the link is made again.
        TestOwner owner = new TestOwner(PeripheralCableType.COMPUTING, 8);
        TestHub hub = linkedHub(owner, 4);
        owner.onEndpointLinked(ENDPOINT_POS, PortKind.DEVICE);
        TestEndpoint device = new TestEndpoint(PeripheralCableType.COMPUTING);
        PeripheralLinkValidator validator = build(throughHub(), owner, Map.of(HUB_POS, hub, ENDPOINT_POS, device),
                Map.of(), pos -> 0);

        assertFalse(validator.isLinkStillValid(OWNER_POS, ENDPOINT_POS, PeripheralCableType.COMPUTING));

        owner.onEndpointLinkedThrough(ENDPOINT_POS, PortKind.DEVICE, HUB_POS);
        assertTrue(validator.isLinkStillValid(OWNER_POS, ENDPOINT_POS, PeripheralCableType.COMPUTING));
    }

    @Test
    void isLinkStillValid_isFalseOnceTheHubUnlinks() {
        TestOwner owner = new TestOwner(PeripheralCableType.COMPUTING, 8);
        TestHub hub = linkedHub(owner, 4);
        owner.onEndpointLinkedThrough(ENDPOINT_POS, PortKind.DEVICE, HUB_POS);
        TestEndpoint device = new TestEndpoint(PeripheralCableType.COMPUTING);
        PeripheralLinkValidator validator = build(throughHub(), owner, Map.of(HUB_POS, hub, ENDPOINT_POS, device),
                Map.of(), pos -> 0);

        hub.onOwnerUnlinked();

        assertFalse(validator.isLinkStillValid(OWNER_POS, ENDPOINT_POS, PeripheralCableType.COMPUTING));
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    /* The owner, a hub against it, and the endpoint against the hub, with no cable anywhere. */
    private static Map<Long, List<Long>> throughHub() {
        return Map.of(
                OWNER_POS, List.of(HUB_POS),
                HUB_POS, List.of(OWNER_POS, ENDPOINT_POS),
                ENDPOINT_POS, List.of(HUB_POS));
    }

    /* A hub of {@code ports} ports linked to {@code owner} on one of its device ports. */
    private static TestHub linkedHub(TestOwner owner, int ports) {
        TestHub hub = new TestHub(ports);
        owner.onEndpointLinked(HUB_POS, PortKind.DEVICE);
        hub.onOwnerLinked(OWNER_POS);
        return hub;
    }

    /* Several endpoints, each at its position, every cable reaching {@code reach}. */
    private static PeripheralLinkValidator build(
            Map<Long, List<Long>> adjacency,
            IPeripheralOwner owner,
            Map<Long, IPeripheralEndpoint> endpoints,
            Map<Long, PeripheralCableType> cables,
            PeripheralLinkValidator.IReachLookup reach) {
        return new PeripheralLinkValidator(
                pos -> Optional.ofNullable(cables.get(pos)),
                pos -> pos == OWNER_POS ? Optional.of(owner) : Optional.empty(),
                pos -> Optional.ofNullable(endpoints.get(pos)),
                PeripheralLinkValidator.adjacencyFrom(adjacency),
                reach);
    }

    /* The owner and the endpoint side by side, with no cable between them. */
    private static Map<Long, List<Long>> adjacent() {
        return Map.of(OWNER_POS, List.of(ENDPOINT_POS), ENDPOINT_POS, List.of(OWNER_POS));
    }

    private static PeripheralLinkValidator build(
            Map<Long, List<Long>> adjacency,
            IPeripheralOwner owner,
            IPeripheralEndpoint endpoint,
            Map<Long, PeripheralCableType> cables) {
        return new PeripheralLinkValidator(
                pos -> Optional.ofNullable(cables.get(pos)),
                pos -> pos == OWNER_POS ? Optional.of(owner) : Optional.empty(),
                pos -> pos == ENDPOINT_POS ? Optional.of(endpoint) : Optional.empty(),
                PeripheralLinkValidator.adjacencyFrom(adjacency));
    }

    /* The same, every cable reaching {@code reach}. */
    private static PeripheralLinkValidator build(
            Map<Long, List<Long>> adjacency,
            IPeripheralOwner owner,
            IPeripheralEndpoint endpoint,
            Map<Long, PeripheralCableType> cables,
            PeripheralLinkValidator.IReachLookup reach) {
        return new PeripheralLinkValidator(
                pos -> Optional.ofNullable(cables.get(pos)),
                pos -> pos == OWNER_POS ? Optional.of(owner) : Optional.empty(),
                pos -> pos == ENDPOINT_POS ? Optional.of(endpoint) : Optional.empty(),
                PeripheralLinkValidator.adjacencyFrom(adjacency),
                reach);
    }

    /**
     * Mutable test-only owner with so many device ports, and whatever other ports {@link #with} gives it.
     */
    private static final class TestOwner implements IPeripheralOwnerSupport {
        private final PeripheralCableType cableType;
        private final Map<PortKind, Integer> ports = new EnumMap<>(PortKind.class);
        private final PeripheralPorts linked = new PeripheralPorts();

        TestOwner(PeripheralCableType cableType, int devicePorts) {
            this.cableType = cableType;
            this.ports.put(PortKind.DEVICE, devicePorts);
        }

        TestOwner with(PortKind kind, int count) {
            ports.put(kind, count);
            return this;
        }

        @Override public PeripheralCableType cableType() { return cableType; }
        @Override public int ports(PortKind kind) { return ports.getOrDefault(kind, 0); }
        @Override public PeripheralPorts peripheralPorts() { return linked; }
        @Override public void markPeripheralChange() { }
    }

    /**
     * Mutable test-only IPeripheralEndpoint, taking a device port unless told another kind.
     */
    private static final class TestEndpoint implements IPeripheralEndpoint {
        private final PeripheralCableType cableType;
        private final PortKind kind;
        private Optional<Long> ownerPos = Optional.empty();

        TestEndpoint(PeripheralCableType cableType) {
            this(cableType, PortKind.DEVICE);
        }

        TestEndpoint(PeripheralCableType cableType, PortKind kind) {
            this.cableType = cableType;
            this.kind = kind;
        }

        @Override public PeripheralCableType cableType() { return cableType; }
        @Override public Optional<Long> linkedOwner() { return ownerPos; }
        @Override public void onOwnerLinked(long pos) { this.ownerPos = Optional.of(pos); }
        @Override public void onOwnerUnlinked() { this.ownerPos = Optional.empty(); }
        @Override public PortKind portKind() { return kind; }
    }

    /**
     * Mutable test-only hub of so many device ports.
     */
    private static final class TestHub implements IPeripheralHub {
        private final int ports;
        private Optional<Long> ownerPos = Optional.empty();

        TestHub(int ports) {
            this.ports = ports;
        }

        @Override public int hubPorts() { return ports; }
        @Override public PeripheralCableType cableType() { return PeripheralCableType.COMPUTING; }
        @Override public Optional<Long> linkedOwner() { return ownerPos; }
        @Override public void onOwnerLinked(long pos) { this.ownerPos = Optional.of(pos); }
        @Override public void onOwnerUnlinked() { this.ownerPos = Optional.empty(); }
    }
}
