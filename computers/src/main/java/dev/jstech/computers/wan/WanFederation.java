/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.wan;

import dev.jstech.computers.blockentity.WanGatewayBlockEntity;
import dev.jstech.core.util.Loaded;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * The federations of one world as its WAN Gateway Computers say they stand: each gateway, every tick, publishes the
 * side of each of its links that federates two networks, and a side not said again since the tick before is gone. Never
 * saved: the gateways say it all again as soon as they run.
 */
public final class WanFederation {

    /* Each side published, by its gateway and port, with the tick it was said on. */
    private final Map<SideKey, Published> sides = new HashMap<>();
    /* The graph of this tick, worked out once when first asked for. */
    private FederationGraph graph;
    private long graphAt = Long.MIN_VALUE;

    /* Per world, held only as long as the world itself. */
    private static final Map<ServerLevel, WanFederation> BY_LEVEL = new WeakHashMap<>();
    /* A side said this tick or the one before stands: the gateways tick in no set order. */
    private static final long STANDS_FOR = 1L;

    private WanFederation() {
    }

    /** The federations of {@code level}. */
    public static WanFederation of(final ServerLevel level) {
        return BY_LEVEL.computeIfAbsent(level, key -> new WanFederation());
    }

    /** {@code edge} stands this tick, {@code now}. */
    public void publish(final FederationEdge edge, final long now) {
        sides.put(new SideKey(edge.gateway(), edge.port()), new Published(edge, now));
        graph = null;
    }

    /** The side of the gateway at {@code gateway} on port {@code port} is down. */
    public void withdraw(final long gateway, final int port) {
        if (sides.remove(new SideKey(gateway, port)) != null) {
            graph = null;
        }
    }

    /** The federations standing at {@code now}. */
    public FederationGraph graph(final long now) {
        if (graph == null || graphAt != now) {
            sides.values().removeIf(side -> side.at() < now - STANDS_FOR);
            final List<FederationEdge> standing = new ArrayList<>(sides.size());
            for (final Published side : sides.values()) {
                standing.add(side.edge());
            }
            graph = new FederationGraph(standing);
            graphAt = now;
        }
        return graph;
    }

    /** The way from {@code from} to {@code to} at {@code now}, or empty when there is none it may take. */
    public Optional<FederationPath> path(final NetworkUuid from, final NetworkUuid to, final long now) {
        return graph(now).path(from, to);
    }

    /** Every network {@code from} reaches at {@code now}, the nearest first. */
    public List<FederationPath> reachable(final NetworkUuid from, final long now) {
        return graph(now).reachable(from);
    }

    /**
     * What the network at the far end of {@code path} is called where a player sees it: by its last gateway, the one
     * the line ends in; empty while that gateway's chunk is not loaded.
     */
    public static String farName(final ServerLevel level, final FederationPath path) {
        return gatewayName(level, path.out().getLast().farGateway());
    }

    /** The name of the WAN Gateway Computer at {@code pos}, or empty when none is loaded there. */
    public static String gatewayName(final ServerLevel level, final long pos) {
        return Loaded.blockEntity(level, BlockPos.of(pos)) instanceof WanGatewayBlockEntity gateway
                ? gateway.displayName() : "";
    }

    /* A gateway's port. */
    private record SideKey(long gateway, int port) {
    }

    private record Published(FederationEdge edge, long at) {
    }
}
