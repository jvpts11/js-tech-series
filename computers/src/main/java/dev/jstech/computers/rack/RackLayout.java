/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.rack;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure geometry of a rack measured in rack units: which vertical spans are free, and what role each of
 * the rack's front-panel hotswap slots plays given what is mounted. The front slots belong to the RACK
 * (5 per rack unit); a mounted chassis claims the slots of the rows it occupies, row-major, up to its
 * drive budget; every remaining slot is blocked, and each blocked slot knows why, so the
 * GUI can always explain itself. Minecraft-free on purpose: the fitting and claiming rules are unit
 * tested here and consumed by both the block entity and the screen.
 */
public final class RackLayout {

    /** Front-panel hotswap slots per rack unit. */
    public static final int SLOTS_PER_U = 5;

    /**
     * A mounted unit's occupancy, its front-slot budget ({@code 0} for most units that serve the cabinet) and what it
     * is to the rack.
     */
    public record Unit(int topU, int heightU, int driveSlots, Role role) {

        /** A computer's occupancy, which is what most of the racks' units are. */
        public Unit(final int topU, final int heightU, final int driveSlots) {
            this(topU, heightU, driveSlots, Role.COMPUTER);
        }

        public Unit {
            role = role == null ? Role.COMPUTER : role;
        }

        /** Whether this unit occupies the given rack-unit row. */
        public boolean occupies(final int uRow) {
            return uRow >= topU && uRow < topU + heightU;
        }
    }

    /** What a mounted unit is to the rack. */
    public enum Role {
        /** A machine that runs: a server, a storage or compute server, a node. */
        COMPUTER,
        /** A shelf of drives only, which extends the bays of the computer right above it. */
        SHELF,
        /** Equipment that serves the cabinet: a KVM switch, a UPS, a PDU, cooling. */
        EQUIPMENT
    }

    /** What one front-panel slot is, and, when blocked, why. */
    public enum SlotRole {
        /** Cabled as a drive bay by the chassis in this row. */
        DRIVE,
        /** No unit occupies this row, so the slot has nothing to cable it. */
        BLOCKED_NO_UNIT,
        /** A unit occupies the row but its chassis does not cable this many slots. */
        BLOCKED_BUDGET
    }

    private final int capacityU;

    public RackLayout(final int capacityU) {
        if (capacityU <= 0) {
            throw new IllegalArgumentException("a rack needs at least one rack unit, got " + capacityU);
        }
        this.capacityU = capacityU;
    }

    public int capacityU() {
        return capacityU;
    }

    /** Whether a unit of the given height fits at {@code topU} without leaving the rack or overlapping. */
    public boolean canPlace(final int topU, final int heightU, final List<Unit> mounted) {
        if (topU < 0 || heightU <= 0 || topU + heightU > capacityU) {
            return false;
        }
        for (final Unit unit : mounted) {
            for (int row = topU; row < topU + heightU; row++) {
                if (unit.occupies(row)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** The topmost row where a unit of the given height fits, or {@code -1} when the rack is full. */
    public int firstFit(final int heightU, final List<Unit> mounted) {
        for (int topU = 0; topU + heightU <= capacityU; topU++) {
            if (canPlace(topU, heightU, mounted)) {
                return topU;
            }
        }
        return -1;
    }

    /** The unit occupying the given row, or null. */
    public static Unit unitAt(final int uRow, final List<Unit> mounted) {
        for (final Unit unit : mounted) {
            if (unit.occupies(uRow)) {
                return unit;
            }
        }
        return null;
    }

    /**
     * The computer a drive shelf extends: the one mounted right above it (row 0 is the rack's top); null for anything
     * that is no shelf, and for a shelf with no computer right above it, which then does nothing.
     */
    public static Unit extendedBy(final Unit shelf, final List<Unit> mounted) {
        if (shelf == null || shelf.role() != Role.SHELF) {
            return null;
        }
        for (final Unit unit : mounted) {
            if (unit.role() == Role.COMPUTER && unit.topU() + unit.heightU() == shelf.topU()) {
                return unit;
            }
        }
        return null;
    }

    /** The drive shelf mounted right under {@code computer}, which extends its bays; null for none. */
    public static Unit shelfUnder(final Unit computer, final List<Unit> mounted) {
        if (computer == null || computer.role() != Role.COMPUTER) {
            return null;
        }
        final Unit below = unitAt(computer.topU() + computer.heightU(), mounted);
        return below != null && below.role() == Role.SHELF && below.topU() == computer.topU() + computer.heightU()
                ? below : null;
    }

    /** The rows whose drive bays serve {@code unit}: its own, then those of a drive shelf right under it. */
    public static List<Integer> rowsServing(final Unit unit, final List<Unit> mounted) {
        final List<Integer> rows = new ArrayList<>();
        if (unit == null) {
            return rows;
        }
        for (int row = unit.topU(); row < unit.topU() + unit.heightU(); row++) {
            rows.add(row);
        }
        final Unit shelf = shelfUnder(unit, mounted);
        if (shelf != null) {
            for (int row = shelf.topU(); row < shelf.topU() + shelf.heightU(); row++) {
                rows.add(row);
            }
        }
        return rows;
    }

    /**
     * The unit whose storage a row's drive bays belong to: the row's own unit, or, for a drive shelf's row, the
     * computer it extends; null for an empty row or a shelf that extends nothing.
     */
    public static Unit ownerOfRow(final int uRow, final List<Unit> mounted) {
        final Unit unit = unitAt(uRow, mounted);
        return unit != null && unit.role() == Role.SHELF ? extendedBy(unit, mounted) : unit;
    }

    /**
     * The role of the front slot at ({@code uRow}, {@code index}): the unit occupying the row claims its
     * rows' slots row-major for its drives, the rest blocked by budget.
     */
    public SlotRole roleAt(final int uRow, final int index, final List<Unit> mounted) {
        if (uRow < 0 || uRow >= capacityU || index < 0 || index >= SLOTS_PER_U) {
            return SlotRole.BLOCKED_NO_UNIT;
        }
        final Unit unit = unitAt(uRow, mounted);
        if (unit == null) {
            return SlotRole.BLOCKED_NO_UNIT;
        }
        final int ordinal = (uRow - unit.topU()) * SLOTS_PER_U + index;
        return ordinal < unit.driveSlots() ? SlotRole.DRIVE : SlotRole.BLOCKED_BUDGET;
    }

    /** How many rack units are currently occupied. */
    public static int usedU(final List<Unit> mounted) {
        int used = 0;
        for (final Unit unit : mounted) {
            used += unit.heightU();
        }
        return used;
    }
}
