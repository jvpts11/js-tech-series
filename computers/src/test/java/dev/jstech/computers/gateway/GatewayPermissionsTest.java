/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gateway;

import dev.jstech.core.operation.OperationPriority;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GatewayPermissionsTest {

    @Test
    void defaults_allowReadsAndOperationsWithReadableFiles() {
        final GatewayPermissions p = GatewayPermissions.DEFAULT;
        assertTrue(p.read());
        assertTrue(p.operations());
        assertEquals(GatewayPermissions.FileAccess.READ, p.files());
        assertEquals(OperationPriority.MEDIUM, p.ceiling());
        assertEquals(8, p.callCap());
        assertFalse(p.allowsWrite());
    }

    @Test
    void of_clampsTheWireNumbersOntoTheKnobs() {
        final GatewayPermissions p = GatewayPermissions.of(false, true, 9, -1, 7);
        assertEquals(GatewayPermissions.FileAccess.READ_WRITE, p.files());
        assertEquals(OperationPriority.LOW, p.ceiling());
        assertEquals(16, p.callCap());
        assertTrue(p.allowsWrite());
    }

    @Test
    void constructor_snapsOddValuesOntoTheNearestKnobAbove() {
        final GatewayPermissions p = new GatewayPermissions(true, true, GatewayPermissions.FileAccess.OFF,
                OperationPriority.MEDIUM_LOW, 5);
        assertEquals(OperationPriority.MEDIUM, p.ceiling());
        assertEquals(8, p.callCap());
        assertEquals(1, p.ceilingIndex());
        assertEquals(1, p.capIndex());
        assertEquals(0, p.filesIndex());
    }

    @Test
    void cap_neverLetsARequestOutrankTheCeiling() {
        final GatewayPermissions p = GatewayPermissions.DEFAULT.withCeiling(OperationPriority.MEDIUM);
        assertEquals(OperationPriority.MEDIUM, p.cap(OperationPriority.HIGH));
        assertEquals(OperationPriority.LOW, p.cap(OperationPriority.LOW));
        assertEquals(OperationPriority.MEDIUM, p.cap(null));
    }

    @Test
    void with_changesOneThingAndKeepsTheRest() {
        final GatewayPermissions p = GatewayPermissions.DEFAULT.withRead(false).withCallCap(16)
                .withFiles(GatewayPermissions.FileAccess.OFF).withOperations(false)
                .withCeiling(OperationPriority.HIGH);
        assertFalse(p.read());
        assertFalse(p.operations());
        assertEquals(GatewayPermissions.FileAccess.OFF, p.files());
        assertEquals(OperationPriority.HIGH, p.ceiling());
        assertEquals(16, p.callCap());
        assertEquals(2, p.capIndex());
    }
}
