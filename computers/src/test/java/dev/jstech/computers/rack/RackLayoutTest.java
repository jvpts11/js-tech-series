/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.rack;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RackLayoutTest {

    private RackLayout rack;

    @BeforeEach
    void setUp() {
        rack = new RackLayout(8);
    }

    private static RackLayout.Unit of(final RackChassis chassis, final int topU) {
        return new RackLayout.Unit(topU, chassis.heightU(), chassis.driveSlots());
    }

    @Test
    void canPlace_rejectsOverlapAndOutOfBounds() {
        final List<RackLayout.Unit> mounted = List.of(of(RackChassis.STORAGE_SERVER, 0));
        assertFalse(rack.canPlace(0, 1, mounted), "row 0 is taken by the 2U storage chassis");
        assertFalse(rack.canPlace(1, 2, mounted), "row 1 is the storage chassis' second row");
        assertTrue(rack.canPlace(2, 2, mounted), "rows 2-3 are free");
        assertFalse(rack.canPlace(7, 2, mounted), "a 2U unit does not fit in the last row");
        assertFalse(rack.canPlace(-1, 1, mounted), "negative rows are outside the rack");
    }

    @Test
    void firstFit_findsTheTopmostGap() {
        final List<RackLayout.Unit> mounted = List.of(
                of(RackChassis.SERVER, 0),
                of(RackChassis.STORAGE_SERVER, 2));
        assertEquals(1, rack.firstFit(1, mounted), "the 1U gap between the units is used first");
        assertEquals(4, rack.firstFit(2, mounted), "a 2U unit skips the 1U gap");
    }

    @Test
    void firstFit_returnsMinusOneWhenFull() {
        final List<RackLayout.Unit> mounted = List.of(
                of(RackChassis.STORAGE_SERVER, 0),
                of(RackChassis.STORAGE_SERVER, 2),
                of(RackChassis.STORAGE_SERVER, 4),
                of(RackChassis.STORAGE_SERVER, 6));
        assertEquals(-1, rack.firstFit(1, mounted));
        assertEquals(8, RackLayout.usedU(mounted));
    }

    @Test
    void roleAt_serverChassisWiresFourDrivesOneBlocked() {
        final List<RackLayout.Unit> mounted = List.of(of(RackChassis.SERVER, 3));
        for (int index = 0; index < 4; index++) {
            assertEquals(RackLayout.SlotRole.DRIVE, rack.roleAt(3, index, mounted));
        }
        assertEquals(RackLayout.SlotRole.BLOCKED_BUDGET, rack.roleAt(3, 4, mounted));
    }

    @Test
    void roleAt_storageChassisFillsItsTenSlotsWithDrives() {
        final List<RackLayout.Unit> mounted = List.of(of(RackChassis.STORAGE_SERVER, 0));
        for (int row = 0; row < 2; row++) {
            for (int index = 0; index < RackLayout.SLOTS_PER_U; index++) {
                assertEquals(RackLayout.SlotRole.DRIVE, rack.roleAt(row, index, mounted),
                        "both rows are drives, the two slots that held gadgets among them");
            }
        }
    }

    @Test
    void roleAt_computeChassisBlocksEverythingPastItsTwoSlots() {
        final List<RackLayout.Unit> mounted = List.of(of(RackChassis.COMPUTE_SERVER, 6));
        assertEquals(RackLayout.SlotRole.DRIVE, rack.roleAt(6, 0, mounted));
        assertEquals(RackLayout.SlotRole.DRIVE, rack.roleAt(6, 1, mounted));
        for (int index = 2; index < RackLayout.SLOTS_PER_U; index++) {
            assertEquals(RackLayout.SlotRole.BLOCKED_BUDGET, rack.roleAt(6, index, mounted));
        }
        for (int index = 0; index < RackLayout.SLOTS_PER_U; index++) {
            assertEquals(RackLayout.SlotRole.BLOCKED_BUDGET, rack.roleAt(7, index, mounted),
                    "the compute chassis' second row is all cooling/accelerator bulk");
        }
    }

    @Test
    void roleAt_rowsWithoutAUnitAreBlockedForThatReason() {
        assertEquals(RackLayout.SlotRole.BLOCKED_NO_UNIT, rack.roleAt(5, 2, List.of()));
        assertEquals(RackLayout.SlotRole.BLOCKED_NO_UNIT, rack.roleAt(9, 0, List.of()),
                "rows outside the rack read as unoccupied");
    }

    @Test
    void constructor_rejectsANonPositiveCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new RackLayout(0));
    }

    @Test
    void extendedBy_isTheComputerRightAboveTheShelf() {
        final RackLayout.Unit server = of(RackChassis.STORAGE_SERVER, 0);
        final RackLayout.Unit shelf = shelf(2);
        final List<RackLayout.Unit> mounted = List.of(server, shelf);
        assertEquals(server, RackLayout.extendedBy(shelf, mounted));
        assertEquals(shelf, RackLayout.shelfUnder(server, mounted));
    }

    @Test
    void extendedBy_isNoneWithARowBetweenOrNothingAbove() {
        final RackLayout.Unit server = of(RackChassis.SERVER, 0);
        final RackLayout.Unit shelf = shelf(2);
        assertNull(RackLayout.extendedBy(shelf, List.of(server, shelf)), "a row lies between them");
        assertNull(RackLayout.extendedBy(shelf(0), List.of(shelf(0))), "nothing is above the top row");
        assertNull(RackLayout.shelfUnder(server, List.of(server, shelf)));
    }

    @Test
    void extendedBy_isNoneForAnythingButAShelf() {
        final RackLayout.Unit server = of(RackChassis.SERVER, 0);
        final RackLayout.Unit ups = new RackLayout.Unit(1, 1, 0, RackLayout.Role.EQUIPMENT);
        assertNull(RackLayout.extendedBy(ups, List.of(server, ups)));
        assertNull(RackLayout.shelfUnder(ups, List.of(server, ups)));
    }

    @Test
    void rowsServing_takesTheShelfsRowsAfterTheServersOwn() {
        final RackLayout.Unit server = of(RackChassis.STORAGE_SERVER, 0);
        final List<RackLayout.Unit> mounted = List.of(server, shelf(2));
        assertEquals(List.of(0, 1, 2, 3), RackLayout.rowsServing(server, mounted));
        assertEquals(List.of(0, 1), RackLayout.rowsServing(server, List.of(server)));
        assertTrue(RackLayout.rowsServing(null, mounted).isEmpty());
    }

    @Test
    void ownerOfRow_givesAShelfsRowsToTheServerItExtends() {
        final RackLayout.Unit server = of(RackChassis.STORAGE_SERVER, 0);
        final RackLayout.Unit shelf = shelf(2);
        final List<RackLayout.Unit> mounted = List.of(server, shelf);
        assertEquals(server, RackLayout.ownerOfRow(3, mounted));
        assertEquals(server, RackLayout.ownerOfRow(1, mounted));
        assertNull(RackLayout.ownerOfRow(5, mounted), "an empty row belongs to nobody");
        assertNull(RackLayout.ownerOfRow(4, List.of(shelf(4))), "nor does a lone shelf's");
    }

    @Test
    void roleAt_aShelfCablesItsTenBays() {
        final List<RackLayout.Unit> mounted = List.of(shelf(4));
        for (int row = 4; row < 6; row++) {
            for (int index = 0; index < RackLayout.SLOTS_PER_U; index++) {
                assertEquals(RackLayout.SlotRole.DRIVE, rack.roleAt(row, index, mounted));
            }
        }
    }

    private static RackLayout.Unit shelf(final int topU) {
        return new RackLayout.Unit(topU, 2, 10, RackLayout.Role.SHELF);
    }
}
