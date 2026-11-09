/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.advancement.JscEvents;
import dev.jstech.computers.block.DataWires;
import dev.jstech.computers.block.MainframeStructure;
import dev.jstech.computers.config.ComputersServerConfig;
import dev.jstech.computers.engine.EngineRole;
import dev.jstech.computers.eventlog.EventLevel;
import dev.jstech.computers.eventlog.NetworkEvents;
import dev.jstech.computers.os.HostScope;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.portable.AlertKind;
import dev.jstech.computers.portable.PortableAlerts;
import dev.jstech.computers.storage.Corruption;
import dev.jstech.core.cable.Cables;
import dev.jstech.core.jstechseries.network.ConnectivityIndex;
import dev.jstech.core.jstechseries.network.FailoverRole;
import dev.jstech.core.jstechseries.network.MainframeNode;
import dev.jstech.core.jstechseries.network.NetworkSystem;
import dev.jstech.core.jstechseries.network.ServerNode;
import dev.jstech.core.jstechseries.network.SubframeNode;
import dev.jstech.core.persistence.NetworkRegistry;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.Loaded;
import dev.jstech.core.uuid.NetworkUuidState;
import dev.jstech.core.uuid.NodeUuid;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Which network a Mainframe owns, and what happens when two of them want the same one.
 *
 * <p>A Mainframe is the only computer that does not simply read a network off a cable: it owns one. That
 * makes it the only one that has to work out, every tick, which network it is on, whether anybody else is
 * claiming it, whether it should be running the thing or standing by for whoever is, and what to leave behind
 * when it is switched off or broken.
 *
 * <p>None of that is simple. It is a big block, so it looks for cables across a whole footprint and bridges
 * every one it touches into a single network. Two running Mainframes on one segment are a conflict, and the
 * network collapses until they are pulled apart. Switched to standby, it waits for the one it is backing to
 * disappear and then, after a delay and only if it is the lowest-placed of the standbys, takes that same
 * network over. The last one out leaves the network orphaned rather than deleting it, so that plugging a new
 * Mainframe in finds the storage still there.
 *
 * <p>All of it was written down the side of the block entity among the hardware and the readouts. It is one
 * subject and it is one class now.
 */
@TextHolder
final class MainframeNetworking {

    /** How long a standby waits, in ticks, before it decides the one it was backing is really gone. */
    private static final int PROMOTE_DELAY = 60;

    /** What every player is told when two Mainframes meet on one network, and when they part again. */
    private static final TextKey CONFLICT = TextKey.of("jsc.mainframe.network_conflict",
            "[J's Computers] NETWORK_CONFLICT: two Mainframes share one network near %s");
    private static final TextKey CONFLICT_RESOLVED = TextKey.of("jsc.mainframe.network_conflict_resolved",
            "[J's Computers] Network conflict resolved near %s");
    /* Where a Mainframe stands in the line to take a fallen orchestrator's network over, and outside it. */
    private static final int IN_FAILOVER = 0;
    private static final int SUBFRAME_SAME_ENGINE = 1;
    private static final int SUBFRAME_OTHER_ENGINE = 2;
    private static final int NOT_IN_LINE = 3;

    /** Every side, held once: Direction.values() hands back a fresh copy of the array on every call. */
    /*
     * Every side, held once, because asking the enum hands back a fresh array on every call. Kept as a
     * list rather than as that array, which nothing here wants to do more than walk.
     */
    private static final List<Direction> SIDES = List.of(Direction.values());

    private final MainframeBlockEntity mainframe;

    private boolean conflicted;
    /** The network a conflict under way is over, whose event log hears it end. */
    @Nullable
    private NetworkUuid conflictOn;
    private boolean failoverEnabled;
    private FailoverRole failoverRole = FailoverRole.NONE;
    private int waitTicks;
    // The part its network engine plays: the network's orchestrator, or a Subframe lending to it.
    private EngineRole role = EngineRole.ORCHESTRATOR;
    // The network a Subframe lends on, which it leaves when its role, its network or its power changes.
    @Nullable
    private NetworkUuid lentOn;
    // The orchestrator a Subframe last saw run its network: what it tells the log of if that one falls.
    @Nullable
    private Seen orchestratorSeen;
    // The devices of this Mainframe's own place, this side of every long-distance line, and the grid read for them.
    @Nullable
    private Set<Long> thisSide;
    private long thisSideVersion;

    MainframeNetworking(final MainframeBlockEntity mainframe) {
        this.mainframe = mainframe;
    }

    boolean conflicted() {
        return conflicted;
    }

    boolean failoverEnabled() {
        return failoverEnabled;
    }

    FailoverRole failoverRole() {
        return failoverRole;
    }

    /** Whether it runs no Operations and no index: a standby in failover, or a Subframe. */
    boolean standingBy() {
        return role == EngineRole.SUBFRAME || failoverRole == FailoverRole.PASSIVE;
    }

    EngineRole role() {
        return role;
    }

    /** Gives its network engine the part it is to play; false when it plays it already. */
    boolean setRole(final EngineRole to) {
        if (to == role) {
            return false;
        }
        role = to;
        waitTicks = 0;
        orchestratorSeen = null;
        mainframe.setChanged();
        return true;
    }

    /**
     * Who takes this orchestrator's network over should it fall: a Mainframe in failover standing by on it first, else
     * a Subframe running the same engine, else one running another, the lowest-placed in each case; null for nobody.
     */
    @Nullable
    MainframeBlockEntity successor(final ServerLevel level) {
        final String engine = engineOf(mainframe);
        MainframeBlockEntity best = null;
        for (final MainframeBlockEntity peer : otherRunningMainframesOnSegment(level,
                NetworkSystem.get(level).connectivity(), adjacentCables(level))) {
            if (place(peer, engine) < NOT_IN_LINE && (best == null || before(peer, best, engine))) {
                best = peer;
            }
        }
        return best;
    }

    /** Switches this Mainframe between owning a network and standing by for whoever owns it. */
    void toggleFailover() {
        failoverEnabled = !failoverEnabled;
        failoverRole = FailoverRole.NONE;
        waitTicks = 0;
        mainframe.setChanged();
    }

    void save(final CompoundTag tag) {
        tag.putBoolean("Failover", failoverEnabled);
        tag.putByte("FailoverRole", (byte) failoverRole.id());
        tag.putInt("FailoverWaitTicks", waitTicks);
        tag.putByte("EngineRole", (byte) role.id());
        if (orchestratorSeen != null) {
            final CompoundTag seen = new CompoundTag();
            seen.putUUID("Node", orchestratorSeen.node().value());
            seen.putString("Name", orchestratorSeen.name());
            seen.putString("Engine", orchestratorSeen.engine());
            seen.putInt("InFlight", orchestratorSeen.inFlight());
            tag.put("OrchestratorSeen", seen);
        }
    }

    /**
     * Reads back which network this Mainframe was part of and how far into a takeover it had got.
     *
     * <p>The countdown is kept on purpose: a world reloading in the middle of a promotion would otherwise
     * start the wait again, and on a server where chunks come and go often a standby could wait forever
     * without ever taking over.
     */
    void load(final CompoundTag tag) {
        failoverEnabled = tag.getBoolean("Failover");
        if (tag.contains("FailoverRole")) {
            failoverRole = FailoverRole.byId(tag.getByte("FailoverRole"));
        }
        waitTicks = tag.getInt("FailoverWaitTicks");
        role = EngineRole.byId(tag.getByte("EngineRole"));
        orchestratorSeen = null;
        if (tag.contains("OrchestratorSeen")) {
            final CompoundTag seen = tag.getCompound("OrchestratorSeen");
            final UUID node = seen.hasUUID("Node") ? seen.getUUID("Node") : null;
            if (node != null) {
                orchestratorSeen = new Seen(new NodeUuid(node), seen.getString("Name"), seen.getString("Engine"),
                        seen.getInt("InFlight"));
            }
        }
    }

    /**
     * Works out, for this tick, which network this Mainframe is on and what its part in it is.
     *
     * <p>Every cable the cabinet touches is bridged into one segment first, so whatever is wired through
     * the machine is one network rather than several that happen to meet at it.
     */
    void update(final ServerLevel level) {
        final NetworkSystem system = NetworkSystem.get(level);
        final ConnectivityIndex index = system.connectivity();
        final Set<Long> cables = adjacentCables(level);
        index.bridge(mainframe.getBlockPos().asLong(), cables);

        NetworkUuid adopted = null;
        for (final long cable : cables) {
            final Optional<NetworkUuid> segment = index.joinedNetwork(cable);
            if (segment.isPresent()) {
                adopted = segment.get();
                break;
            }
        }

        final List<MainframeBlockEntity> peers = otherRunningMainframesOnSegment(level, index, cables);
        /*
         * A Mainframe beyond a long-distance line stands in another place, which a WAN link joined to this one
         * (computers/docs/design/network.md): it contests nothing here. When the network this one would take is that
         * Mainframe's, the link is extending its network over this place, and this one holds off for the tick the
         * gateways take to make the link a federation of two networks instead.
         */
        if (!peers.isEmpty() || adopted != null && !mainframe.getBlockPos().equals(ownerOf(system, adopted))) {
            final Set<Long> thisSide = thisSide(index, cables);
            peers.removeIf(peer -> !thisSide.contains(peer.getBlockPos().asLong()));
            final BlockPos owner = adopted == null ? null : ownerOf(system, adopted);
            if (owner != null && !owner.equals(mainframe.getBlockPos()) && !thisSide.contains(owner.asLong())
                    && Loaded.blockEntity(level, owner) instanceof MainframeBlockEntity across
                    && across.ownsANetwork()) {
                standBy(system, null);
                return;
            }
        }
        // A Subframe owns nothing and a standby only stands by, so only another owner contests the network.
        final boolean primaryPeerPresent = peers.stream().anyMatch(MainframeNetworking::owns);

        if (role == EngineRole.SUBFRAME) {
            updateAsSubframe(level, system, index, cables, adopted, peers);
            return;
        }
        stopLending(system);
        if (!failoverEnabled) {
            updateAsOwner(level, system, index, cables, adopted, peers, primaryPeerPresent);
            return;
        }
        updateAsStandby(level, system, index, cables, adopted, primaryPeerPresent, peers);
    }

    /*
     * A Mainframe whose engine plays Subframe: it counts for no conflict and keeps no index; it lends the network's
     * orchestrator its share while one runs, and once the orchestrator it saw has been gone the promotion delay, the
     * first in line takes the network over (computers/docs/design/computers.md).
     */
    private void updateAsSubframe(final ServerLevel level, final NetworkSystem system, final ConnectivityIndex index,
                                  final Set<Long> cables, @Nullable final NetworkUuid adopted,
                                  final List<MainframeBlockEntity> peers) {
        failoverRole = FailoverRole.NONE;
        setConflict(level, adopted, false);
        unregister(system);
        if (adopted == null) {
            waitTicks = 0;
            lend(system, null, null);
            return;
        }
        final Optional<MainframeNode> owner = system.mainframeOf(adopted);
        if (owner.isPresent()) {
            waitTicks = 0;
            see(level, system, adopted);
            lend(system, adopted, owner.get().nodeUuid());
            return;
        }
        lend(system, adopted, null);
        if (orchestratorSeen == null || !firstInLine(peers)) {
            waitTicks = 0;
            return;
        }
        if (++waitTicks >= PROMOTE_DELAY) {
            takeOver(level, system, index, cables, adopted);
        }
    }

    /*
     * Registers this Subframe on {@code network}, lending to {@code orchestrator}, or idle with none; an engine stopped
     * lends nothing either. It leaves the network it lent on before when that changes.
     */
    private void lend(final NetworkSystem system, @Nullable final NetworkUuid network,
                      @Nullable final NodeUuid orchestrator) {
        final NodeUuid node = mainframe.nodeUuid();
        if (lentOn != null && node != null && !lentOn.equals(network)) {
            system.unregisterSubframe(lentOn, node);
        }
        lentOn = network;
        mainframe.networkAttachment().attachTo(network);
        if (network == null || node == null) {
            return;
        }
        final boolean serving = mainframe.runningEngine() != null;
        system.registerSubframe(new SubframeNode(node, network, mainframe.capacity(),
                Optional.ofNullable(serving ? orchestrator : null), Math.max(0, mainframe.parallelQueues() - 1),
                engineOf(mainframe)));
    }

    private void stopLending(final NetworkSystem system) {
        final NodeUuid node = mainframe.nodeUuid();
        if (lentOn != null && node != null) {
            system.unregisterSubframe(lentOn, node);
        }
        lentOn = null;
    }

    /* Remembers the orchestrator running {@code network} now: its name, its engine and the Operations it carries. */
    private void see(final ServerLevel level, final NetworkSystem system, final NetworkUuid network) {
        system.mainframePositionOf(network).ifPresent(pos -> {
            if (Loaded.blockEntity(level, BlockPos.of(pos)) instanceof MainframeBlockEntity orchestrator
                    && orchestrator.nodeUuid() != null) {
                orchestratorSeen = new Seen(orchestrator.nodeUuid(), NetworkEvents.machineName(
                        orchestrator.customName(), HostScope.MAINFRAME, orchestrator.nodeUuid()),
                        engineOf(orchestrator), orchestrator.liveOperations().size());
            }
        });
    }

    /* Whether this Subframe comes before every other in line: no Mainframe in failover, nobody ranked above it. */
    private boolean firstInLine(final List<MainframeBlockEntity> peers) {
        final String engine = orchestratorSeen.engine();
        for (final MainframeBlockEntity peer : peers) {
            if (place(peer, engine) < NOT_IN_LINE && before(peer, mainframe, engine)) {
                return false;
            }
        }
        return true;
    }

    /*
     * The orchestrator it saw is gone: this Subframe becomes the network's orchestrator, with its own engine, and the
     * network's log says who stopped, who took over, what engine runs now and how many Operations were lost.
     */
    private void takeOver(final ServerLevel level, final NetworkSystem system, final ConnectivityIndex index,
                          final Set<Long> cables, final NetworkUuid network) {
        final Seen fallen = orchestratorSeen;
        role = EngineRole.ORCHESTRATOR;
        waitTicks = 0;
        orchestratorSeen = null;
        stopLending(system);
        orchestrate(level, system, index, cables, network);
        mainframe.setChanged();
        NetworkEvents.record(level, network, EventLevel.ERROR, fallen.node(), fallen.name(),
                NetworkEvents.STOPPED_ANSWERING.with(PROMOTE_DELAY));
        final NodeUuid node = mainframe.nodeUuid();
        if (node != null) {
            NetworkEvents.record(level, network, EventLevel.WARN, node,
                    NetworkEvents.machineName(mainframe.customName(), HostScope.MAINFRAME, node),
                    NetworkEvents.TOOK_OVER.text());
        }
        final ResourceLocation engine = mainframe.activeEngine();
        final Text engineLine = engine == null ? NetworkEvents.NO_ENGINE.text()
                : (engine.toString().equals(fallen.engine()) ? NetworkEvents.ENGINE_STAYS : NetworkEvents.ENGINE_NOW)
                        .with(engineName(engine));
        NetworkEvents.record(level, network, EventLevel.INFO, null, "", engineLine);
        NetworkEvents.record(level, network, EventLevel.INFO, null, "",
                NetworkEvents.OPERATIONS_DISCARDED.with(fallen.inFlight()));
    }

    /* Whether {@code peer} owns its network: neither a Subframe nor a standby in failover. */
    private static boolean owns(final MainframeBlockEntity peer) {
        return !peer.failoverEnabled() && peer.engineRole() == EngineRole.ORCHESTRATOR;
    }

    /* Whether {@code peer} stands by in failover: an orchestrator's engine with the failover switch on. */
    private static boolean standsBy(final MainframeBlockEntity peer) {
        return peer.failoverEnabled() && peer.engineRole() == EngineRole.ORCHESTRATOR;
    }

    /* Where {@code candidate} stands in the line to take a network run with {@code engine} over. */
    private static int place(final MainframeBlockEntity candidate, final String engine) {
        if (standsBy(candidate)) {
            return IN_FAILOVER;
        }
        if (candidate.engineRole() != EngineRole.SUBFRAME) {
            return NOT_IN_LINE;
        }
        return engineOf(candidate).equals(engine) ? SUBFRAME_SAME_ENGINE : SUBFRAME_OTHER_ENGINE;
    }

    /* Whether {@code a} comes before {@code b} in that line: by its place, then the lower-placed first. */
    private static boolean before(final MainframeBlockEntity a, final MainframeBlockEntity b, final String engine) {
        final int placeA = place(a, engine);
        final int placeB = place(b, engine);
        return placeA != placeB ? placeA < placeB : a.getBlockPos().asLong() < b.getBlockPos().asLong();
    }

    /* The engine {@code mainframe} has chosen, by its program's id; empty for none. */
    private static String engineOf(final MainframeBlockEntity mainframe) {
        final ResourceLocation engine = mainframe.activeEngine();
        return engine == null ? "" : engine.toString();
    }

    private static String engineName(final ResourceLocation engine) {
        final ProgramSpec spec = OsRegistry.getProgram(engine);
        return spec == null ? engine.toString() : spec.displayName();
    }

    /** A Mainframe that owns its network: it takes the segment it is on, or lays down its own. */
    private void updateAsOwner(final ServerLevel level, final NetworkSystem system, final ConnectivityIndex index,
                               final Set<Long> cables, @Nullable final NetworkUuid adopted,
                               final List<MainframeBlockEntity> peers, final boolean primaryPeerPresent) {
        failoverRole = FailoverRole.NONE;
        waitTicks = 0;
        final NetworkUuid effective = adopted != null ? adopted : mainframe.nativeNetworkUuid();
        if (adopted == null) {
            NetworkRegistry.addNetwork(level, effective);
        }
        setConflict(level, effective, primaryPeerPresent);
        if (primaryPeerPresent) {
            // The first owner to see the conflict corrupts both networks' storage, once for the whole conflict.
            if (NetworkRegistry.networkState(level, effective) != NetworkUuidState.CONFLICTED) {
                conflictCorrupts(level, system, effective, peers);
            }
            // Two owners on one network: collapse it until they are physically separated.
            NetworkRegistry.setNetworkState(level, effective, NetworkUuidState.CONFLICTED);
            mainframe.networkAttachment().attachTo(null);
            unregister(system);
            return;
        }
        orchestrate(level, system, index, cables, effective);
    }

    /** A Mainframe set to stand by: it takes over only once the one it was backing is gone. */
    private void updateAsStandby(final ServerLevel level, final NetworkSystem system,
                                 final ConnectivityIndex index, final Set<Long> cables,
                                 @Nullable final NetworkUuid adopted, final boolean primaryPeerPresent,
                                 final List<MainframeBlockEntity> peers) {
        setConflict(level, adopted, false); // a standby never holds the network in conflict on its own
        if (adopted == null) {
            // Not on any network yet: dormant until it reaches an owner's network.
            standBy(system, null);
            return;
        }
        if (primaryPeerPresent) {
            // The owner runs this network; the standby merely stands by on it.
            standBy(system, adopted);
            return;
        }
        /*
         * No owner present, so it is gone and the network is orphaned. The lowest-placed standby takes that
         * SAME network over after the delay; the rest keep standing by.
         */
        final boolean superiorStandbyPresent = peers.stream()
                .anyMatch(peer -> standsBy(peer) && peer.getBlockPos().asLong() < mainframe.getBlockPos().asLong());
        updateFailoverRole(superiorStandbyPresent);
        if (failoverRole == FailoverRole.PASSIVE) {
            mainframe.networkAttachment().attachTo(adopted);
            unregister(system);
            return;
        }
        orchestrate(level, system, index, cables, adopted);
    }

    private void standBy(final NetworkSystem system, @Nullable final NetworkUuid network) {
        failoverRole = FailoverRole.PASSIVE;
        waitTicks = 0;
        mainframe.networkAttachment().attachTo(network);
        unregister(system);
    }

    /** Lays this Mainframe's network over every cable it touches and registers it as that network's owner. */
    private void orchestrate(final ServerLevel level, final NetworkSystem system, final ConnectivityIndex index,
                             final Set<Long> cables, final NetworkUuid effective) {
        // A cable whose BlockEntity has not registered yet (mid chunk-load) is skipped, picked up later.
        for (final long cable : cables) {
            if (index.contains(cable) && !effective.equals(index.joinedNetwork(cable).orElse(null))) {
                index.assignUuid(cable, effective);
            }
        }
        // The cables it touches tell the index, when one goes, whether the rest still reaches this Mainframe.
        final long here = mainframe.getBlockPos().asLong();
        index.anchor(here, effective, cables);
        mainframe.networkAttachment().attachTo(effective);
        // Restore the network from any prior CONFLICTED or ORPHANED state, since adopting it revives it.
        NetworkRegistry.setNetworkState(level, effective, NetworkUuidState.ACTIVE);
        system.registerMainframe(new MainframeNode(mainframe.nodeUuid(), effective, mainframe.capacity(),
                FailoverRole.NONE, Optional.empty(), 0L));
        system.recordMainframePosition(effective, here);
        if (mainframe.networkAttachment().registered() == null) {
            JscEvents.awardOperator(mainframe, JscEvents.MAINFRAME_NETWORK);
        }
        mainframe.networkAttachment().registeredAs(effective);
    }

    private void updateFailoverRole(final boolean superiorPresent) {
        if (superiorPresent) {
            failoverRole = FailoverRole.PASSIVE; // a preferred owner is running, so stand by
            waitTicks = 0;
        } else if (failoverRole == FailoverRole.PASSIVE) {
            // The owner this member was backing is gone; take over after the promotion delay.
            if (++waitTicks >= PROMOTE_DELAY) {
                failoverRole = FailoverRole.ACTIVE;
                waitTicks = 0;
            }
        } else {
            // Lowest-placed and not standing by, so own the network immediately (initial election).
            failoverRole = FailoverRole.ACTIVE;
            waitTicks = 0;
        }
    }

    /** Lets go of the network, which is what a Mainframe losing power does: data no longer passes through it. */
    void leave(final ServerLevel level) {
        mainframe.networkAttachment().attachTo(null);
        setConflict(level, null, false);
        unregisterFrom(NetworkSystem.get(level));
    }

    /**
     * What a Mainframe being broken leaves behind.
     *
     * <p>The LAST one out orphans the network, whatever its part in it was, so the storage on it is still
     * there when somebody plugs a new Mainframe in. While another one is still standing, the network is
     * simply that one's.
     */
    void onBroken(final ServerLevel level) {
        final NetworkSystem system = NetworkSystem.get(level);
        final boolean survivorPresent = !otherRunningMainframesOnSegment(
                level, system.connectivity(), adjacentCables(level)).isEmpty();
        if (!survivorPresent) {
            orphanOwnedNetwork(level);
        }
        unregister(system);
        stopLending(system);
    }

    private void orphanOwnedNetwork(final ServerLevel level) {
        final NetworkSystem system = NetworkSystem.get(level);
        final ConnectivityIndex index = system.connectivity();
        final Set<NetworkUuid> owned = new LinkedHashSet<>();
        final NetworkUuid registered = mainframe.networkAttachment().registered();
        if (registered != null) {
            owned.add(registered);
        }
        for (final long cable : adjacentCables(level)) {
            index.joinedNetwork(cable).ifPresent(owned::add);
        }
        for (final NetworkUuid net : owned) {
            NetworkRegistry.setNetworkState(level, net, NetworkUuidState.ORPHANED);
        }
    }

    /** Wipes the network out entirely rather than orphaning it, for a Mainframe told to forget it. */
    void eraseOwnedNetwork(final ServerLevel level) {
        final NetworkSystem system = NetworkSystem.get(level);
        final ConnectivityIndex index = system.connectivity();
        for (final NetworkUuid net : new LinkedHashSet<>(Arrays.asList(
                mainframe.networkAttachment().network(), mainframe.networkAttachment().registered()))) {
            if (net != null) {
                index.clearNetwork(net);
                NetworkRegistry.removeNetwork(level, net);
            }
        }
        mainframe.networkAttachment().attachTo(null);
        unregister(system);
    }

    /** Takes this Mainframe off the network, and off every way through it: it is switched off, broken or unloaded. */
    void unregisterFrom(final NetworkSystem system) {
        unregister(system);
        stopLending(system);
        system.connectivity().forgetBridge(mainframe.getBlockPos().asLong());
    }

    private void unregister(final NetworkSystem system) {
        final NetworkUuid registered = mainframe.networkAttachment().registered();
        if (registered != null) {
            system.unregisterMainframe(registered, mainframe.nodeUuid());
            mainframe.networkAttachment().registeredAs(null);
        }
    }

    /** Every other running Mainframe wired into the same segment, which is who this one is competing with. */
    private List<MainframeBlockEntity> otherRunningMainframesOnSegment(
            final ServerLevel level, final ConnectivityIndex index, final Set<Long> cables) {
        final Map<Long, MainframeBlockEntity> found = new LinkedHashMap<>();
        final Set<Long> scanned = new HashSet<>();
        for (final long anchor : cables) {
            for (final long wire : index.componentPositions(anchor)) {
                final BlockPos base = Cables.blockOf(level, wire);
                if (base == null || !scanned.add(base.asLong())) {
                    continue;
                }
                for (final Direction direction : SIDES) {
                    final MainframeBlockEntity peer = mainframeBehind(level, base.relative(direction));
                    if (peer != null && peer != mainframe && peer.isRunning()) {
                        found.putIfAbsent(peer.getBlockPos().asLong(), peer);
                    }
                }
            }
        }
        return new ArrayList<>(found.values());
    }

    /* The devices of this Mainframe's own place, read off the grid again only when the grid has changed. */
    private Set<Long> thisSide(final ConnectivityIndex index, final Set<Long> cables) {
        final long version = index.grid().version();
        if (thisSide == null || thisSideVersion != version) {
            thisSide = index.devicesThisSide(cables);
            thisSideVersion = version;
        }
        return thisSide;
    }

    /* Where the Mainframe that runs {@code network} stands, or null when none does. */
    @Nullable
    private static BlockPos ownerOf(final NetworkSystem system, final NetworkUuid network) {
        return system.mainframePositionOf(network).map(BlockPos::of).orElse(null);
    }

    /** The Mainframe at that position, whether the block there is the cabinet itself or one of its parts. */
    @Nullable
    private MainframeBlockEntity mainframeBehind(final ServerLevel level, final BlockPos pos) {
        final BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof MainframeBlockEntity found) {
            return found;
        }
        if (be instanceof MainframePartBlockEntity part && part.originPos() != null
                && Loaded.blockEntity(level, part.originPos()) instanceof MainframeBlockEntity origin) {
            return origin;
        }
        return null;
    }

    /** Says so in chat when a conflict starts and when it clears, since nothing else would show it. */
    /*
     * A conflict starting is written into the log of the network it is over, and its end into the same one, even when
     * the Mainframe is leaving that network as it ends.
     */
    private void setConflict(final ServerLevel level, @Nullable final NetworkUuid network, final boolean conflict) {
        if (conflict && !conflicted) {
            JscEvents.awardOperator(mainframe, JscEvents.MAINFRAME_CONFLICT);
            conflictOn = network;
            logConflict(level, network, EventLevel.ERROR,
                    NetworkEvents.CONFLICT.with(mainframe.getBlockPos().toShortString()));
            PortableAlerts.fromLog(level, network, AlertKind.CONFLICT, "",
                    NetworkEvents.CONFLICT.with(mainframe.getBlockPos().toShortString()));
        } else if (!conflict && conflicted) {
            logConflict(level, conflictOn, EventLevel.INFO, NetworkEvents.CONFLICT_RESOLVED.text());
            conflictOn = null;
        }
        if (conflict != conflicted && level.getServer() != null) {
            final String where = mainframe.getBlockPos().toShortString();
            final TextKey message = conflict ? CONFLICT : CONFLICT_RESOLVED;
            level.getServer().getPlayerList().broadcastSystemMessage(GameText.component(message.with(where)), false);
        }
        conflicted = conflict;
    }

    /*
     * A conflict corrupts the storage of both networks (computers/docs/design/network.md): a tenth of every server's
     * drives, items and system files, and of every Personal Computer's public part. Both networks are the one the
     * cable now joins and each Mainframe's own from before, since a machine of the other side may not yet have moved
     * onto the joined one; a machine is on one network at a time, so none is corrupted twice.
     */
    private void conflictCorrupts(final ServerLevel level, final NetworkSystem system, final NetworkUuid effective,
                                  final List<MainframeBlockEntity> peers) {
        if (!ComputersServerConfig.conflictCosts() || !ComputersServerConfig.corruption()) {
            return;
        }
        final Set<NetworkUuid> networks = new LinkedHashSet<>();
        networks.add(effective);
        addIfSet(networks, mainframe.networkAttachment().registered());
        for (final MainframeBlockEntity peer : peers) {
            addIfSet(networks, peer.networkAttachment().registered());
        }
        int machines = 0;
        for (final NetworkUuid network : networks) {
            for (final ServerNode server : system.serversOf(network)) {
                final Optional<NetworkSystem.ServerLocation> where = system.locationOf(server.nodeUuid());
                if (where.isPresent() && Loaded.blockEntity(level, BlockPos.of(where.get().rackPos()))
                        instanceof ServerRackBlockEntity rack
                        && rack.corruptBay(where.get().slot(), Corruption.CONFLICT_SHARE, 1.0, level.random) > 0) {
                    machines++;
                }
            }
            for (final NetworkSystem.PersonalComputerNode pc : system.personalComputersOf(network)) {
                if (Loaded.blockEntity(level, BlockPos.of(pc.pos())) instanceof PersonalComputerBlockEntity computer
                        && computer.corruptPublic(Corruption.CONFLICT_SHARE, level.random)) {
                    machines++;
                }
            }
        }
        if (machines > 0) {
            NetworkEvents.record(level, effective, EventLevel.ERROR, null, "",
                    NetworkEvents.STORAGE_CORRUPTED.with(machines));
        }
    }

    private static void addIfSet(final Set<NetworkUuid> networks, @Nullable final NetworkUuid network) {
        if (network != null) {
            networks.add(network);
        }
    }

    private void logConflict(final ServerLevel level, @Nullable final NetworkUuid network, final EventLevel what,
                             final Text message) {
        final NodeUuid node = mainframe.nodeUuid();
        if (node != null) {
            NetworkEvents.record(level, network, what, node,
                    NetworkEvents.machineName(mainframe.customName(), HostScope.MAINFRAME, node), message);
        }
    }

    /**
     * Every data wire crossing into the outside of the cabinet, by the number its grid knows it by.
     *
     * <p>The Mainframe is 3x2x2, so it looks across its whole footprint rather than at one side, and it takes
     * every wire it finds rather than the first, because it bridges all of them into its single network.
     */
    Set<Long> adjacentCables(final ServerLevel level) {
        final Direction facing = mainframe.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        final BlockPos origin = mainframe.getBlockPos();
        final Set<Long> inside = new HashSet<>();
        for (final BlockPos p : MainframeStructure.allPositions(origin, facing)) {
            if (p.equals(origin)) {
                inside.add(p.asLong());
            } else if (level.getBlockEntity(p) instanceof MainframePartBlockEntity part
                    && origin.equals(part.originPos())) {
                inside.add(p.asLong());
            }
        }
        final Set<Long> cables = new LinkedHashSet<>();
        for (final long posLong : inside) {
            final BlockPos p = BlockPos.of(posLong);
            for (final Direction direction : SIDES) {
                final BlockPos neighbor = p.relative(direction);
                if (inside.contains(neighbor.asLong())) {
                    continue; // a face internal to the cabinet
                }
                cables.addAll(DataWires.numbersReaching(level, p, direction, DataWires::isNetwork));
            }
        }
        return cables;
    }

    /**
     * The orchestrator a Subframe last saw run its network.
     *
     * @param node     its node
     * @param name     what the network's log calls it
     * @param engine   the engine it ran, by its program's id; empty for none
     * @param inFlight the Operations it was carrying, which are lost with it
     */
    private record Seen(NodeUuid node, String name, String engine, int inFlight) {
    }
}
