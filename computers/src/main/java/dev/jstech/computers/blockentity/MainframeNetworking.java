/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.advancement.JscEvents;
import dev.jstech.computers.block.DataCableBlock;
import dev.jstech.computers.block.MainframeStructure;
import dev.jstech.core.network.ConnectivityIndex;
import dev.jstech.core.network.FailoverRole;
import dev.jstech.core.network.MainframeNode;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.persistence.NetworkRegistrySavedData;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NetworkUuidState;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
 * <p>None of that is simple. It is a multiblock, so it looks for cables across a whole footprint and bridges
 * every one it touches into a single network. Two running Mainframes on one segment are a conflict, and the
 * network collapses until they are pulled apart. Switched to standby, it waits for the one it is backing to
 * disappear and then, after a delay and only if it is the lowest-placed of the standbys, takes that same
 * network over. The last one out leaves the network orphaned rather than deleting it, so that plugging a new
 * Mainframe in finds the storage still there.
 *
 * <p>All of it was written down the side of the block entity among the hardware and the readouts. It is one
 * subject and it is one class now.
 */
final class MainframeNetworking {

    /** How long a standby waits, in ticks, before it decides the one it was backing is really gone. */
    private static final int PROMOTE_DELAY = 60;

    /** Every side, held once: Direction.values() hands back a fresh copy of the array on every call. */
    /*
     * Every side, held once, because asking the enum hands back a fresh array on every call. Kept as a
     * list rather than as that array, which nothing here wants to do more than walk.
     */
    private static final List<Direction> SIDES = List.of(Direction.values());

    private final MainframeBlockEntity mainframe;

    private boolean conflicted;
    private boolean failoverEnabled;
    private FailoverRole failoverRole = FailoverRole.NONE;
    private int waitTicks;

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

    boolean standingBy() {
        return failoverRole == FailoverRole.PASSIVE;
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
    }

    /**
     * Works out, for this tick, which network this Mainframe is on and what its part in it is.
     *
     * <p>Every cable the multiblock touches is bridged into one segment first, so whatever is wired through
     * the machine is one network rather than several that happen to meet at it.
     */
    void update(final ServerLevel level) {
        final NetworkSystem system = NetworkSystem.get(level);
        final ConnectivityIndex index = system.connectivity();
        final Set<Long> cables = adjacentCables(level);
        index.bridge(cables);

        NetworkUuid adopted = null;
        for (final long cable : cables) {
            final Optional<NetworkUuid> segment = index.networkOf(cable);
            if (segment.isPresent()) {
                adopted = segment.get();
                break;
            }
        }

        final List<MainframeBlockEntity> peers = otherRunningMainframesOnSegment(level, index, cables);
        final boolean primaryPeerPresent = peers.stream().anyMatch(peer -> !peer.failoverEnabled());

        if (!failoverEnabled) {
            updateAsOwner(level, system, index, cables, adopted, primaryPeerPresent);
            return;
        }
        updateAsStandby(level, system, index, cables, adopted, primaryPeerPresent, peers);
    }

    /** A Mainframe that owns its network: it takes the segment it is on, or lays down its own. */
    private void updateAsOwner(final ServerLevel level, final NetworkSystem system, final ConnectivityIndex index,
                               final Set<Long> cables, @Nullable final NetworkUuid adopted,
                               final boolean primaryPeerPresent) {
        failoverRole = FailoverRole.NONE;
        waitTicks = 0;
        final NetworkUuid effective = adopted != null ? adopted : mainframe.nativeNetworkUuid();
        if (adopted == null) {
            NetworkRegistrySavedData.get(level).addNetwork(effective);
        }
        setConflict(level, primaryPeerPresent);
        if (primaryPeerPresent) {
            // Two owners on one network: collapse it until they are physically separated.
            NetworkRegistrySavedData.get(level).setNetworkState(effective, NetworkUuidState.CONFLICTED);
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
        setConflict(level, false); // a standby never holds the network in conflict on its own
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
                .anyMatch(peer -> peer.getBlockPos().asLong() < mainframe.getBlockPos().asLong());
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
            if (index.contains(cable) && !effective.equals(index.networkOf(cable).orElse(null))) {
                index.assignUuid(cable, effective);
            }
        }
        // The cables it touches tell the index, when one goes, whether the rest still reaches this Mainframe.
        final long here = mainframe.getBlockPos().asLong();
        index.anchor(here, effective, cables);
        mainframe.networkAttachment().attachTo(effective);
        // Restore the network from any prior CONFLICTED or ORPHANED state, since adopting it revives it.
        NetworkRegistrySavedData.get(level).setNetworkState(effective, NetworkUuidState.ACTIVE);
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

    /** Lets go of the network, which is what a Mainframe losing power does. */
    void leave(final ServerLevel level) {
        mainframe.networkAttachment().attachTo(null);
        setConflict(level, false);
        unregister(NetworkSystem.get(level));
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
            index.networkOf(cable).ifPresent(owned::add);
        }
        final NetworkRegistrySavedData registry = NetworkRegistrySavedData.get(level);
        for (final NetworkUuid net : owned) {
            registry.setNetworkState(net, NetworkUuidState.ORPHANED);
        }
    }

    /** Wipes the network out entirely rather than orphaning it, for a Mainframe told to forget it. */
    void eraseOwnedNetwork(final ServerLevel level) {
        final NetworkSystem system = NetworkSystem.get(level);
        final ConnectivityIndex index = system.connectivity();
        final NetworkRegistrySavedData registry = NetworkRegistrySavedData.get(level);
        for (final NetworkUuid net : new LinkedHashSet<>(Arrays.asList(
                mainframe.networkAttachment().network(), mainframe.networkAttachment().registered()))) {
            if (net != null) {
                index.clearNetwork(net);
                registry.removeNetwork(net);
            }
        }
        mainframe.networkAttachment().attachTo(null);
        unregister(system);
    }

    /** Takes this Mainframe off the network's register, for a caller tearing the machine down itself. */
    void unregisterFrom(final NetworkSystem system) {
        unregister(system);
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
            for (final long cablePos : index.componentPositions(anchor)) {
                if (!scanned.add(cablePos)) {
                    continue;
                }
                final BlockPos base = BlockPos.of(cablePos);
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

    /** The Mainframe at that position, whether the block there is the cabinet itself or one of its parts. */
    @Nullable
    private MainframeBlockEntity mainframeBehind(final ServerLevel level, final BlockPos pos) {
        final BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof MainframeBlockEntity found) {
            return found;
        }
        if (be instanceof MainframePartBlockEntity part && part.controllerPos() != null
                && level.getBlockEntity(part.controllerPos()) instanceof MainframeBlockEntity controller) {
            return controller;
        }
        return null;
    }

    /** Says so in chat when a conflict starts and when it clears, since nothing else would show it. */
    private void setConflict(final ServerLevel level, final boolean conflict) {
        if (conflict && !conflicted) {
            JscEvents.awardOperator(mainframe, JscEvents.MAINFRAME_CONFLICT);
        }
        if (conflict != conflicted && level.getServer() != null) {
            final String where = mainframe.getBlockPos().toShortString();
            final String message = conflict
                    ? "[J's Computers] NETWORK_CONFLICT: two Mainframes share one network near " + where
                    : "[J's Computers] Network conflict resolved near " + where;
            level.getServer().getPlayerList().broadcastSystemMessage(Component.literal(message), false);
        }
        conflicted = conflict;
    }

    /**
     * Every cable touching the outside of the multiblock.
     *
     * <p>The Mainframe is 3x2x2, so it looks across its whole footprint rather than at one side, and it takes
     * every cable it finds rather than the first, because it bridges all of them into its single network.
     */
    private Set<Long> adjacentCables(final ServerLevel level) {
        final Direction facing = mainframe.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        final BlockPos origin = mainframe.getBlockPos();
        final Set<Long> inside = new HashSet<>();
        for (final BlockPos p : MainframeStructure.allPositions(origin, facing)) {
            if (p.equals(origin)) {
                inside.add(p.asLong());
            } else if (level.getBlockEntity(p) instanceof MainframePartBlockEntity part
                    && origin.equals(part.controllerPos())) {
                inside.add(p.asLong());
            }
        }
        final Set<Long> cables = new LinkedHashSet<>();
        for (final long posLong : inside) {
            final BlockPos p = BlockPos.of(posLong);
            for (final Direction direction : SIDES) {
                final BlockPos neighbor = p.relative(direction);
                if (inside.contains(neighbor.asLong())) {
                    continue; // a face internal to the multiblock
                }
                if (level.getBlockState(neighbor).getBlock() instanceof DataCableBlock cable
                        && mainframe.acceptsDataTier(cable.tier())) {
                    cables.add(neighbor.asLong());
                }
            }
        }
        return cables;
    }
}
