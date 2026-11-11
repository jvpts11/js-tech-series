/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.backup;

import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.item.RackUnitItem;
import dev.jstech.computers.item.TapeItem;
import dev.jstech.computers.rack.RackLayout;
import dev.jstech.core.jstechseries.network.NetworkSystem;
import dev.jstech.core.jstechseries.network.ServerNode;
import dev.jstech.core.util.Loaded;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * One Tape Library as the Backup Service sees it: the 2U unit topped at a row of a rack, its ten tapes of its own era
 * and its drive (computers/docs/design/servers-and-racks.md). A library serves the network of the servers in its rack,
 * and the network calls it by the rack's number among its own and the row it is topped at.
 */
public final class TapeLibrary {

    private final ServerRackBlockEntity rack;
    private final int row;
    private final RackUnitItem unit;
    private final int rackNumber;

    private TapeLibrary(final ServerRackBlockEntity rack, final int row, final RackUnitItem unit,
                        final int rackNumber) {
        this.rack = rack;
        this.row = row;
        this.unit = unit;
        this.rackNumber = rackNumber;
    }

    /** The library topped at {@code row} of {@code rack}, or null when none is; its rack numbered 0. */
    @Nullable
    public static TapeLibrary at(final ServerRackBlockEntity rack, final int row) {
        return at(rack, row, 0);
    }

    /** Every library of {@code network}: those in the racks of its servers, rack by rack, top row first. */
    public static List<TapeLibrary> of(final ServerLevel level, final NetworkUuid network) {
        final List<TapeLibrary> out = new ArrayList<>();
        int number = 0;
        for (final ServerRackBlockEntity rack : racksOf(level, network)) {
            number++;
            for (final RackLayout.Unit unit : rack.mountedUnits()) {
                final TapeLibrary library = unit.role() == RackLayout.Role.LIBRARY
                        ? at(rack, unit.topU(), number) : null;
                if (library != null) {
                    out.add(library);
                }
            }
        }
        return out;
    }

    /** The library of {@code network} topped at {@code row} of the rack at {@code rackPos}, or null when none is. */
    @Nullable
    public static TapeLibrary find(final ServerLevel level, final NetworkUuid network, final long rackPos,
                                   final int row) {
        for (final TapeLibrary library : of(level, network)) {
            if (library.rack.getBlockPos().asLong() == rackPos && library.row == row) {
                return library;
            }
        }
        return null;
    }

    /** The racks holding {@code network}'s servers, in the order the network lists its servers. */
    public static List<ServerRackBlockEntity> racksOf(final ServerLevel level, final NetworkUuid network) {
        final NetworkSystem system = NetworkSystem.get(level);
        final Set<ServerRackBlockEntity> racks = new LinkedHashSet<>();
        for (final ServerNode server : system.serversOf(network)) {
            system.locationOf(server.nodeUuid()).ifPresent(at -> {
                if (Loaded.blockEntity(level, BlockPos.of(at.rackPos())) instanceof ServerRackBlockEntity rack) {
                    racks.add(rack);
                }
            });
        }
        return List.copyOf(racks);
    }

    /** The rack it is mounted in. */
    public ServerRackBlockEntity rack() {
        return rack;
    }

    /** The row it is topped at. */
    public int row() {
        return row;
    }

    /** The rack's number among its network's, from 1, or 0 when it was found without its network. */
    public int rackNumber() {
        return rackNumber;
    }

    /** The format of the tapes it takes, those of its own era. */
    public TapeFormat format() {
        return TapeFormat.of(unit.era());
    }

    /** The drive, which winds one tape at a time. */
    public TapeDrive drive() {
        return rack.tapeDrive(row);
    }

    /** What its drive is doing for the Backup Service, or null when nothing. */
    @Nullable
    public TapeWork work() {
        return rack.tapeWork(row);
    }

    /** How many slots it has: ten. */
    public int slots() {
        return frontSlots().size();
    }

    /** The tape in slot {@code n}, counted from 0, or an empty stack. */
    public ItemStack tape(final int n) {
        final List<Integer> slots = frontSlots();
        return n >= 0 && n < slots.size() ? rack.getFrontSlots().getStackInSlot(slots.get(n)) : ItemStack.EMPTY;
    }

    /** What the tape in slot {@code n} holds; blank for an empty slot. */
    public TapeContents contents(final int n) {
        return TapeItem.contents(tape(n));
    }

    /** Writes onto the tape in slot {@code n} what it holds now. */
    public void write(final int n, final TapeContents contents) {
        final ItemStack tape = tape(n);
        if (!tape.isEmpty()) {
            TapeItem.setContents(tape, contents);
            rack.setChanged();
        }
    }

    /** Takes the tape out of slot {@code n}, answering it, or an empty stack for an empty slot. */
    public ItemStack take(final int n) {
        final List<Integer> slots = frontSlots();
        if (n < 0 || n >= slots.size()) {
            return ItemStack.EMPTY;
        }
        return rack.getFrontSlots().extractItem(slots.get(n), 1, false);
    }

    /** How many of its slots hold a tape. */
    public int tapes() {
        int held = 0;
        for (int n = 0; n < slots(); n++) {
            held += tape(n).isEmpty() ? 0 : 1;
        }
        return held;
    }

    /** How much of its tapes is written, in items of its era. */
    public long usedItems() {
        long used = 0L;
        for (int n = 0; n < slots(); n++) {
            used += tape(n).isEmpty() ? 0L : contents(n).used();
        }
        return used;
    }

    /** The slot of the first tape with {@code size} items of room after what is written on it, or -1 for none. */
    public int tapeWithRoom(final long size) {
        for (int n = 0; n < slots(); n++) {
            if (!tape(n).isEmpty() && contents(n).spots().size() < TapeContents.MOST_SPOTS
                    && format().capacityItems() - contents(n).used() >= size) {
                return n;
            }
        }
        return -1;
    }

    /** The slot of the tape holding {@code backup}, or -1 when none of its tapes does. */
    public int tapeHolding(final UUID backup) {
        for (int n = 0; n < slots(); n++) {
            if (!tape(n).isEmpty() && contents(n).spotOf(backup) != null) {
                return n;
            }
        }
        return -1;
    }

    /* The rack's front slots that are this library's tape slots, in order. */
    private List<Integer> frontSlots() {
        final List<RackLayout.Unit> mounted = rack.mountedUnits();
        final RackLayout.Unit here = RackLayout.unitAt(row, mounted);
        final List<Integer> out = new ArrayList<>();
        for (int r = row; here != null && r < row + here.heightU(); r++) {
            for (int column = 0; column < RackLayout.SLOTS_PER_U; column++) {
                if (rack.layout().roleAt(r, column, mounted) == RackLayout.SlotRole.TAPE) {
                    out.add(r * RackLayout.SLOTS_PER_U + column);
                }
            }
        }
        return out;
    }

    @Nullable
    private static TapeLibrary at(final ServerRackBlockEntity rack, final int row, final int number) {
        final RackLayout.Unit unit = RackLayout.unitAt(row, rack.mountedUnits());
        if (unit == null || unit.role() != RackLayout.Role.LIBRARY || unit.topU() != row
                || !(rack.getServers().getStackInSlot(row).getItem() instanceof RackUnitItem item)) {
            return null;
        }
        return new TapeLibrary(rack, row, item, number);
    }
}
