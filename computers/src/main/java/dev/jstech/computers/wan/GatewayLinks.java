/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.wan;

import dev.jstech.computers.advancement.JscEvents;
import dev.jstech.computers.block.WanGatewayStructure;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.WanGatewayBlockEntity;
import dev.jstech.computers.eventlog.EventLevel;
import dev.jstech.computers.hardware.WanCardSpec;
import dev.jstech.core.jstechseries.network.ConnectivityIndex;
import dev.jstech.core.jstechseries.network.DataLink;
import dev.jstech.core.jstechseries.network.NetworkSystem;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.Loaded;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import org.jetbrains.annotations.Nullable;

/**
 * What a WAN Gateway Computer's ports are doing, worked out once a tick (computers/docs/design/network.md).
 *
 * <p>A port's line is up while both gateways run BorderLink, both ports are on and both cards take the line laid
 * between them, and the run is no longer than the line reaches. A line that is up extends one network over the other place when
 * only one of the two places has a Mainframe of its own, and federates the two networks when both have one; which, each
 * gateway reads off its own place alone, never across a long-distance line, so the answer does not change with the link
 * it decides.
 *
 * <p>Extending, each gateway joins its own place's cables to the line on the grid, so the two places are one network.
 * Federating, each publishes its side of the link for the world's federations, and the two stay two.
 */
@TextHolder
public final class GatewayLinks {

    private final WanGatewayBlockEntity gateway;
    private final Port[] ports = new Port[WanGatewayStructure.PORTS];
    /* The grid the links were last found on. */
    private long linksVersion = Long.MIN_VALUE;
    /* The Mainframes of this gateway's own place, and the grid they were read off. */
    private List<BlockPos> mainframesHere = List.of();
    private long mainframesVersion = Long.MIN_VALUE;
    private boolean ownerHere;
    private boolean bridged;
    /* The gateway's own cables as the last tick found them: what letting go undoes, with no look at the world. */
    private Set<Long> local = Set.of();

    private static final TextKey LINE_UP = TextKey.of("jsc.event_log.wan_line_up", "Port %s: line up to %s, %s");
    private static final TextKey LINE_DOWN = TextKey.of("jsc.event_log.wan_line_down", "Port %s: line down");
    private static final TextKey EXTENSION = TextKey.of("jsc.wan.mode.extension", "Extension");
    private static final TextKey FEDERATION = TextKey.of("jsc.wan.mode.federation", "Federation");

    public GatewayLinks(final WanGatewayBlockEntity gateway) {
        this.gateway = gateway;
        for (int i = 0; i < ports.length; i++) {
            ports[i] = new Port();
        }
    }

    /** What port {@code port}'s line does: {@link WanGatewayBlockEntity#LINE_NONE}, LINE_UP or LINE_DOWN. */
    public int state(final int port) {
        return ports[port - 1].state;
    }

    /** What port {@code port}'s line, when up, does between the two places. */
    public WanMode mode(final int port) {
        return ports[port - 1].mode;
    }

    /** What BorderLink shows of port {@code port}'s mode: the mode while up, and what it last did while down. */
    public WanMode shownMode(final int port) {
        final Port p = ports[port - 1];
        return p.state == WanGatewayBlockEntity.LINE_UP ? p.mode : p.lastMode;
    }

    /** The link plugged into port {@code port}, up or not, or null when none ends in another gateway. */
    @Nullable
    public WanLink link(final int port) {
        return ports[port - 1].link;
    }

    /**
     * What the gateway at the far end of port {@code port}'s line is called: the one there now, or the one last seen
     * there while its chunk is not loaded; empty when the line ends in no gateway.
     */
    public String farName(final int port) {
        return ports[port - 1].farName;
    }

    /** The network at the far end of port {@code port}'s line, when it federates one with this gateway's. */
    @Nullable
    public NetworkUuid farNetwork(final int port) {
        return ports[port - 1].farNetwork;
    }

    /** Whether a Mainframe runs as an owner in this gateway's own place, on this side of every long-distance line. */
    public boolean ownerHere() {
        return ownerHere;
    }

    /** The name of what a mode does, as BorderLink shows it. */
    public static TextKey modeName(final WanMode mode) {
        return mode == WanMode.FEDERATION ? FEDERATION : EXTENSION;
    }

    /**
     * Whether this gateway answers a line of {@code line} on port {@code port}: BorderLink runs, the port is on, and
     * the port's card takes that line.
     */
    public boolean answers(final int port, final DataLink line) {
        final List<WanCardSpec> cards = gateway.wanCards();
        return BorderLink.running(gateway) && gateway.portSettings(port).on() && port <= cards.size()
                && cards.get(port - 1).takes(line);
    }

    /** Works every port out for this tick, and joins, federates or lets go of what the lines say. */
    public void tick(final ServerLevel level) {
        final ConnectivityIndex index = NetworkSystem.get(level).connectivity();
        final long version = index.grid().version();
        local = Set.copyOf(gateway.networkCables(level));
        ownerHere = ownerHere(level, index, local, version);
        final Direction facing = gateway.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        if (version != linksVersion) {
            for (int port = 1; port <= ports.length; port++) {
                ports[port - 1].link = WanLinks.find(level, gateway.getBlockPos(), facing, port);
            }
            linksVersion = version;
        }
        final Set<Long> extend = new HashSet<>();
        for (int port = 1; port <= ports.length; port++) {
            final Port p = ports[port - 1];
            final int before = p.state;
            final WanMode modeBefore = p.mode;
            settle(level, index, port, p);
            if (p.state == WanGatewayBlockEntity.LINE_UP && p.mode == WanMode.EXTENSION) {
                extend.add(p.link.plugged());
            }
            publish(level, port, p);
            tell(level, port, p, before, modeBefore);
        }
        bridge(level, index, local, extend);
    }

    /**
     * The gateway is gone, broken or unloaded with its chunk: it joins nothing and federates nothing any more. Nothing
     * here reads the world, which may be taking the chunk down: the cables let go of are the ones the last tick saw.
     */
    public void leave(final ServerLevel level) {
        final ConnectivityIndex index = NetworkSystem.get(level).connectivity();
        bridge(level, index, local, Set.of());
        for (int port = 1; port <= ports.length; port++) {
            WanFederation.of(level).withdraw(gateway.getBlockPos().asLong(), port);
            ports[port - 1].clear(WanGatewayBlockEntity.LINE_NONE);
        }
    }

    /* Port {@code port}'s state and mode this tick. */
    private void settle(final ServerLevel level, final ConnectivityIndex index, final int port, final Port p) {
        final List<WanCardSpec> cards = gateway.wanCards();
        if (port > cards.size()) {
            p.clear(WanGatewayBlockEntity.LINE_NONE);
            return;
        }
        final WanLink link = p.link;
        final WanGatewayBlockEntity far = link == null ? null
                : Loaded.blockEntity(level, link.far()) instanceof WanGatewayBlockEntity found ? found : null;
        if (link == null) {
            p.farName = "";
            p.lastMode = WanMode.NONE;
        } else if (far != null) {
            p.farName = far.displayName();
        }
        if (link == null || far == null || index.runTooLong(link.plugged()) || !answers(port, link.line())
                || !far.links().answers(link.farPort(), link.line())) {
            p.clear(WanGatewayBlockEntity.LINE_DOWN);
            return;
        }
        p.state = WanGatewayBlockEntity.LINE_UP;
        p.mode = ownerHere && far.links().ownerHere() ? WanMode.FEDERATION : WanMode.EXTENSION;
        p.lastMode = p.mode;
        p.farNetwork = p.mode == WanMode.FEDERATION ? far.networkUuid() : null;
    }

    /* The side of a federation this port is, published while it stands and withdrawn when it does not. */
    private void publish(final ServerLevel level, final int port, final Port p) {
        final WanFederation federation = WanFederation.of(level);
        final NetworkUuid here = gateway.networkUuid();
        if (p.mode != WanMode.FEDERATION || here == null || p.farNetwork == null || here.equals(p.farNetwork)) {
            federation.withdraw(gateway.getBlockPos().asLong(), port);
            return;
        }
        federation.publish(new FederationEdge(here, p.farNetwork, gateway.getBlockPos().asLong(), port,
                p.link.far().asLong(), p.link.farPort(), p.link.line(), gateway.capacity(),
                gateway.portSettings(port)), level.getGameTime());
    }

    /* Writes a line's coming up and going down into the network's log, and credits a first federation. */
    private void tell(final ServerLevel level, final int port, final Port p, final int before,
                      final WanMode modeBefore) {
        if (p.state == WanGatewayBlockEntity.LINE_UP && (before != p.state || modeBefore != p.mode)) {
            gateway.log(level, EventLevel.INFO, LINE_UP.with(port, p.farName, modeName(p.mode)));
            if (p.mode == WanMode.FEDERATION) {
                JscEvents.awardOperator(gateway, JscEvents.WAN_JOINED);
            }
        } else if (before == WanGatewayBlockEntity.LINE_UP && p.state != WanGatewayBlockEntity.LINE_UP) {
            gateway.log(level, EventLevel.WARN, LINE_DOWN.with(port));
        }
    }

    /*
     * Joins this gateway's own cables to the lines it extends over, or lets go of them. A place the link extended a
     * network over keeps that network's id by position once let go of, as a part cut off does; it is cleared here when
     * the network's Mainframe is not in this place, so a Mainframe put here later makes a network of its own.
     */
    private void bridge(final ServerLevel level, final ConnectivityIndex index, final Set<Long> local,
                        final Set<Long> extend) {
        final long key = gateway.getBlockPos().asLong();
        if (!extend.isEmpty() && !local.isEmpty()) {
            final Set<Long> joined = new HashSet<>(local);
            joined.addAll(extend);
            index.bridge(key, joined);
            bridged = true;
            return;
        }
        if (!bridged) {
            return;
        }
        index.forgetBridge(key);
        bridged = false;
        final NetworkSystem system = NetworkSystem.get(level);
        final List<BlockPos> here = mainframesHere(level, index, local, index.grid().version());
        for (final long cable : local) {
            final NetworkUuid network = index.joinedNetwork(cable).orElse(null);
            final BlockPos owner = network == null ? null
                    : system.mainframePositionOf(network).map(BlockPos::of).orElse(null);
            if (network != null && (owner == null || !here.contains(owner))) {
                index.forgetNetworkOf(cable);
            }
        }
    }

    /* Whether a Mainframe of this gateway's own place runs as an owner. */
    private boolean ownerHere(final ServerLevel level, final ConnectivityIndex index, final Set<Long> local,
                              final long version) {
        for (final BlockPos at : mainframesHere(level, index, local, version)) {
            if (Loaded.blockEntity(level, at) instanceof MainframeBlockEntity mainframe && mainframe.ownsANetwork()) {
                return true;
            }
        }
        return false;
    }

    /* The Mainframes of this gateway's own place, read off the grid again only when it has changed. */
    private List<BlockPos> mainframesHere(final ServerLevel level, final ConnectivityIndex index,
                                          final Set<Long> local, final long version) {
        if (version != mainframesVersion) {
            final List<BlockPos> found = new ArrayList<>();
            for (final long device : index.devicesThisSide(local)) {
                final BlockPos at = BlockPos.of(device);
                if (Loaded.blockEntity(level, at) instanceof MainframeBlockEntity) {
                    found.add(at);
                }
            }
            mainframesHere = List.copyOf(found);
            mainframesVersion = version;
        }
        return mainframesHere;
    }

    /* One port's line, as this tick left it. */
    private static final class Port {

        @Nullable
        private WanLink link;
        private int state = WanGatewayBlockEntity.LINE_NONE;
        private WanMode mode = WanMode.NONE;
        /* What the line last did while up, which BorderLink still shows while it is down. */
        private WanMode lastMode = WanMode.NONE;
        private String farName = "";
        @Nullable
        private NetworkUuid farNetwork;

        /* The line is down, or the port has no card: it does nothing now, and with no card nothing is remembered. */
        private void clear(final int to) {
            state = to;
            mode = WanMode.NONE;
            farNetwork = null;
            if (to == WanGatewayBlockEntity.LINE_NONE) {
                farName = "";
                lastMode = WanMode.NONE;
            }
        }
    }
}
