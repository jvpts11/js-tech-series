/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.item.RackGadgetItem;
import dev.jstech.computers.item.ServerItem;
import dev.jstech.computers.rack.RackChassis;
import dev.jstech.computers.rack.RackLayout;
import dev.jstech.computers.rack.RaidMode;
import dev.jstech.computers.storage.DriveVolumes;
import dev.jstech.computers.storage.LocalStore;
import dev.jstech.computers.storage.StorageVolume;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NodeUuid;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * The drives in one rack bay treated as a single volume, and what happens when one of them is pulled.
 *
 * <p>A bay with a RAID Controller in it stops being a handful of separate drives and becomes one volume.
 * That changes what pulling a drive means, and the answer depends on the array: an independent bay's drive
 * simply leaves with its contents, a stripe loses everything because no member held a whole copy of
 * anything, and a redundant array keeps serving while it waits for a replacement to rebuild onto.
 *
 * <p>The rebuild takes time on purpose. A replacement drive is put in and the array works for a while before
 * it is whole again, which is the maintenance loop the hotswap bays exist to create.
 *
 * <p>One thing here matters more than it looks: when a member is pulled from a redundant array, what it held
 * is moved onto the survivors and only what they actually accepted is taken off it. Written the easy way,
 * copying what it held onto the others and then wiping it, a bay too full to take everything would duplicate
 * whatever did not fit. Storage that mints items under a common accident is worse than storage that loses
 * them, so the move is what the survivors took and nothing more.
 */
final class RackArrays {

    /** Ticks of rebuild work per item of array capacity (a balancing estimate). */
    private static final int TICKS_PER_1K_ITEMS = 20;
    private static final int MIN_TICKS = 40;
    private static final int MAX_TICKS = 20 * 60 * 3; // three minutes on a huge array

    private final ServerRackBlockEntity rack;

    /** Remaining rebuild ticks per unit, and the total each rebuild started with (for the bar). */
    private final int[] rebuildTicks;
    private final int[] rebuildTotal;

    /** Units whose redundant array lost a member and is waiting for a replacement drive. */
    private final Set<Integer> pendingRebuild = new HashSet<>();

    RackArrays(final ServerRackBlockEntity rack, final int capacityU) {
        this.rack = rack;
        this.rebuildTicks = new int[capacityU];
        this.rebuildTotal = new int[capacityU];
    }

    /** The front-slot index holding the unit's RAID Controller, or -1 when it has none. */
    int controllerSlot(final int serverSlot) {
        return gadgetSlot(serverSlot, RackGadgetItem.Kind.RAID_CONTROLLER);
    }

    /** Whether the unit has a Cache Card fitted, which is what lets its bay answer faster. */
    boolean hasCacheCard(final int serverSlot) {
        return gadgetSlot(serverSlot, RackGadgetItem.Kind.CACHE_CARD) >= 0;
    }

    /** The RAID mode the unit at {@code serverSlot} runs, or {@link RaidMode#NONE}. */
    RaidMode modeOf(final int serverSlot) {
        final int controller = controllerSlot(serverSlot);
        return controller < 0 ? RaidMode.NONE
                : RackGadgetItem.raidMode(rack.frontSlot(controller));
    }

    /**
     * Sets the RAID mode of the unit and stamps the member count the array forms with.
     *
     * <p>The count is what lets a later pull read as a degraded array rather than as a smaller one that was
     * always that size: without it, an array of four missing one looks exactly like an array of three.
     */
    boolean setMode(final int serverSlot, final RaidMode mode) {
        final int controller = controllerSlot(serverSlot);
        if (controller < 0) {
            return false;
        }
        final int drives = rack.claimedDriveStacks(serverSlot).size();
        if (mode != RaidMode.NONE && drives < mode.minDrives()) {
            return false; // not enough drives in the bay to form this array
        }
        final ItemStack stack = rack.frontSlot(controller);
        RackGadgetItem.setRaidMode(stack, mode);
        stack.set(ComputingModule.RAID_MEMBERS.get(), mode == RaidMode.NONE ? 0 : drives);
        rack.markStorageChanged(serverSlot);
        rack.setChanged();
        return true;
    }

    /** How many drives the unit's array was formed with (0 when it runs no array). */
    int memberCount(final int serverSlot) {
        final int controller = controllerSlot(serverSlot);
        if (controller < 0) {
            return 0;
        }
        final Integer members = rack.frontSlot(controller).get(ComputingModule.RAID_MEMBERS.get());
        return members == null ? 0 : members;
    }

    /** Whether the unit's array is missing members but still serving data. */
    boolean degraded(final int serverSlot) {
        return modeOf(serverSlot).rebuildable(memberCount(serverSlot),
                rack.claimedDriveStacks(serverSlot).size());
    }

    /** Whether the unit's array has lost more members than its mode tolerates. */
    boolean failed(final int serverSlot) {
        final RaidMode mode = modeOf(serverSlot);
        final int members = memberCount(serverSlot);
        return mode != RaidMode.NONE && members > 0
                && !mode.survives(members, rack.claimedDriveStacks(serverSlot).size());
    }

    /** Whether the unit's array is currently rebuilding onto a replacement member. */
    boolean rebuilding(final int serverSlot) {
        return serverSlot >= 0 && serverSlot < rebuildTicks.length && rebuildTicks[serverSlot] > 0;
    }

    /** Rebuild progress of the unit's array in permille, or 0 when it is not rebuilding. */
    int rebuildPermille(final int serverSlot) {
        if (!rebuilding(serverSlot) || rebuildTotal[serverSlot] <= 0) {
            return 0;
        }
        final int done = rebuildTotal[serverSlot] - rebuildTicks[serverSlot];
        return (int) (1000L * done / rebuildTotal[serverSlot]);
    }

    /**
     * A drive is leaving the front slot {@code frontSlot}, and what that costs depends on the array.
     *
     * <p>An independent volume travels with its drive, as any computer disk does. A stripe is destroyed,
     * because no member ever held a whole copy of anything. A redundant array keeps the volume: what the
     * departing member held is moved onto the drives that stay, and it comes out blank.
     */
    void onMemberRemoved(final int frontSlot) {
        final RackLayout.Unit unit =
                RackLayout.unitAt(frontSlot / RackLayout.SLOTS_PER_U, rack.mountedUnits());
        if (unit == null) {
            return;
        }
        final int top = unit.topU();
        final RaidMode mode = modeOf(top);
        if (mode == RaidMode.NONE) {
            /*
             * An independent volume goes with its drive, and the network index is left holding rows it has
             * not re-read: that is a hot pull, and the index has to be told.
             */
            notifyHotPull(top);
            return;
        }
        if (mode == RaidMode.RAID0) {
            notifyHotPull(top);
            // The stripe is gone: every member, including the one on its way out, is blanked.
            for (final ItemStack drive : rack.claimedDriveStacks(top)) {
                DriveVolumes.erase(drive);
            }
            rack.markStorageChanged(top);
            rack.setChanged();
            return;
        }
        rebuildOnSurvivors(top, rack.frontSlot(frontSlot));
    }

    /** Starts a rebuild on the unit if its array just got its missing member back. */
    void maybeStartRebuild(final int serverSlot) {
        if (!pendingRebuild.contains(serverSlot) || rebuilding(serverSlot)) {
            return;
        }
        final RaidMode mode = modeOf(serverSlot);
        final int members = memberCount(serverSlot);
        if (mode == RaidMode.NONE || members <= 0
                || rack.claimedDriveStacks(serverSlot).size() < members) {
            return; // still short a member
        }
        final long capacityItems = rack.getServerStorage(serverSlot).capacity();
        final int ticks = (int) Math.max(MIN_TICKS,
                Math.min(MAX_TICKS, capacityItems * TICKS_PER_1K_ITEMS / 1000L));
        rebuildTicks[serverSlot] = ticks;
        rebuildTotal[serverSlot] = ticks;
        pendingRebuild.remove(serverSlot);
        rack.setChanged();
    }

    /** One tick of every rebuild under way; a finished one puts its unit's storage back in service. */
    void tick() {
        for (int slot = 0; slot < rebuildTicks.length; slot++) {
            if (rebuildTicks[slot] > 0 && --rebuildTicks[slot] == 0) {
                rebuildTotal[slot] = 0;
                rack.markStorageChanged(slot);
                rack.setChanged();
            }
        }
    }

    void save(final CompoundTag tag) {
        tag.putIntArray("RebuildTicks", rebuildTicks.clone());
        tag.putIntArray("RebuildTotal", rebuildTotal.clone());
        tag.putIntArray("PendingRebuild", new ArrayList<>(pendingRebuild));
    }

    /** A rebuild in flight survives a reload, as does an array still waiting for its replacement. */
    void load(final CompoundTag tag) {
        final int[] savedTicks = tag.getIntArray("RebuildTicks");
        final int[] savedTotal = tag.getIntArray("RebuildTotal");
        for (int i = 0; i < rebuildTicks.length; i++) {
            rebuildTicks[i] = i < savedTicks.length ? savedTicks[i] : 0;
            rebuildTotal[i] = i < savedTotal.length ? savedTotal[i] : 0;
        }
        pendingRebuild.clear();
        for (final int slot : tag.getIntArray("PendingRebuild")) {
            pendingRebuild.add(slot);
        }
    }

    /**
     * Moves what the departing member held onto the drives that stay, and marks the array for rebuild.
     *
     * <p>Only what the survivors actually accept is taken off the leaving drive, so a bay with no room for
     * all of it comes out holding the remainder rather than the network holding two copies of it.
     */
    private void rebuildOnSurvivors(final int top, final ItemStack leaving) {
        pendingRebuild.add(top);
        rebuildTicks[top] = 0;
        rebuildTotal[top] = 0;
        final List<ItemStack> survivors = new ArrayList<>();
        for (final ItemStack drive : rack.claimedDriveStacks(top)) {
            if (drive != leaving) {
                survivors.add(drive);
            }
        }
        final StorageVolume leavingVolume = DriveVolumes.peek(leaving);
        if (!leavingVolume.isEmpty() && !survivors.isEmpty()) {
            final LocalStore rest = new LocalStore(survivors, () -> { });
            for (final var entry : leavingVolume.snapshot().items().entrySet()) {
                final long moved = rest.insert(entry.getKey(), entry.getValue());
                leavingVolume.take(entry.getKey(), moved);
            }
            if (leavingVolume.isEmpty()) {
                DriveVolumes.erase(leaving);
            } else {
                DriveVolumes.refreshUsage(leaving, leavingVolume);
            }
        }
        rack.markStorageChanged(top);
        rack.setChanged();
    }

    /**
     * Tells the network's index that a drive left this unit's bay while the machine was running, so the rows
     * it holds for that machine read as unconfirmed until somebody reindexes.
     *
     * <p>A machine running the Integrity Monitor re-reads its own bay after a hot event, so the index never
     * learns to doubt it. A fragmented index still wants a vacuum by hand.
     */
    private void notifyHotPull(final int topRow) {
        if (!(rack.getLevel() instanceof ServerLevel serverLevel) || rack.hasService(topRow, "integrity_monitor")) {
            return;
        }
        final ItemStack stack = rack.serverSlot(topRow);
        final UUID node = ServerItem.nodeUuid(stack);
        final NetworkUuid network = node == null ? null : rack.registeredNetworks().get(node);
        if (network == null) {
            return; // an unnetworked bay has no index to confuse
        }
        rack.mainframePositionOn(serverLevel, network).ifPresent(pos -> {
            if (serverLevel.getBlockEntity(BlockPos.of(pos)) instanceof MainframeBlockEntity mainframe) {
                final String name = ServerItem.customName(stack);
                mainframe.networkIndex().markBayHotPull(new NodeUuid(node),
                        name.isEmpty() ? "srv-" + node.toString().substring(0, 6) : name);
            }
        });
    }

    /** The front slot of this unit holding a gadget of that kind, or -1 when it has none. */
    private int gadgetSlot(final int serverSlot, final RackGadgetItem.Kind kind) {
        final RackChassis chassis = ServerItem.chassisOf(rack.serverSlot(serverSlot));
        if (chassis == null) {
            return -1;
        }
        final List<RackLayout.Unit> mounted = rack.mountedUnits();
        for (int row = serverSlot; row < serverSlot + chassis.heightU() && row < rebuildTicks.length; row++) {
            for (int column = 0; column < RackLayout.SLOTS_PER_U; column++) {
                if (rack.slotRoleAt(row, column, mounted) != RackLayout.SlotRole.GADGET) {
                    continue;
                }
                final int index = row * RackLayout.SLOTS_PER_U + column;
                if (RackGadgetItem.kindOf(rack.frontSlot(index)) == kind) {
                    return index;
                }
            }
        }
        return -1;
    }
}
