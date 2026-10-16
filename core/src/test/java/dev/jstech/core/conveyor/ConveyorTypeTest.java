/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.conveyor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ConveyorTypeTest {

    private static final double EPSILON = 1.0e-9;

    @Test
    void build_holdsFourItemsABlockMovingByItselfUnlessSaid() {
        final ConveyorType type = ConveyorType.moving(2.0).build();
        assertEquals(4, type.perBlock());
        assertEquals(0.25, type.gap(), EPSILON);
        assertInstanceOf(ConveyorDrive.Free.class, type.drive());
        assertEquals(ConveyorEnd.HOLD, type.end());
        assertFalse(type.carriesEntities());
        assertEquals(4, type.height());
    }

    @Test
    void build_keepsWhatTheModSaid() {
        final ConveyorType type = ConveyorType.moving(8.0).perBlock(2).drive(ConveyorDrive.energy(40.0))
                .end(ConveyorEnd.DROP).carriesEntities().height(6).build();
        assertEquals(8.0, type.speed(), EPSILON);
        assertEquals(0.5, type.gap(), EPSILON, "two items a block, half a block apart");
        assertEquals(new ConveyorDrive.Energy(40.0), type.drive());
        assertEquals(ConveyorEnd.DROP, type.end());
        assertTrue(type.carriesEntities());
        assertEquals(6, type.height());
    }

    @Test
    void moving_refusesNoSpeedAndMoreThanABlockATick() {
        assertThrows(IllegalArgumentException.class, () -> ConveyorType.moving(0.0));
        assertThrows(IllegalArgumentException.class, () -> ConveyorType.moving(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> ConveyorType.moving(ConveyorType.FASTEST + 1.0));
        assertEquals(ConveyorType.FASTEST, ConveyorType.moving(ConveyorType.FASTEST).build().speed(), EPSILON);
    }

    @Test
    void perBlock_refusesNoneAndMoreThanEight() {
        assertThrows(IllegalArgumentException.class, () -> ConveyorType.moving(1.0).perBlock(0));
        assertThrows(IllegalArgumentException.class,
                () -> ConveyorType.moving(1.0).perBlock(ConveyorType.MOST_A_BLOCK + 1));
    }

    @Test
    void height_refusesNoneAndMoreThanABlock() {
        assertThrows(IllegalArgumentException.class, () -> ConveyorType.moving(1.0).height(0));
        assertThrows(IllegalArgumentException.class, () -> ConveyorType.moving(1.0).height(17));
    }

    @Test
    void drives_refuseWhatCannotMoveABelt() {
        assertThrows(IllegalArgumentException.class, () -> ConveyorDrive.mechanical(0.0, 1.0));
        assertThrows(IllegalArgumentException.class, () -> ConveyorDrive.mechanical(60.0, -1.0));
        assertThrows(IllegalArgumentException.class, () -> ConveyorDrive.energy(0.0));
    }
}
