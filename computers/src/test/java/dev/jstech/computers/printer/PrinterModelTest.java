/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.printer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.tier.HardwareEra;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PrinterModelTest {

    @Test
    void of_givesEachEraItsOwnPrinter() {
        assertEquals(PrinterModel.EPSILON_FX_80, PrinterModel.of(HardwareEra.VINTAGE));
        assertEquals(PrinterModel.PAKARD_DESKJOT_940, PrinterModel.of(HardwareEra.LEGACY));
        assertEquals(PrinterModel.PAKARD_FOTOSMART_C4280, PrinterModel.of(HardwareEra.TRANSITION));
        assertEquals(PrinterModel.PAKARD_LASERJOT_1102, PrinterModel.of(HardwareEra.STANDARD));
        assertEquals(PrinterModel.EPSILON_ECOTONK_ET_2720, PrinterModel.of(HardwareEra.ADVANCED));
    }

    @Test
    void of_aLaterEraHasTheAdvancedPrinter() {
        assertEquals(PrinterModel.EPSILON_ECOTONK_ET_2720, PrinterModel.of(HardwareEra.SINGULARITY));
    }

    @Test
    void find_readsTheNameBack() {
        for (final PrinterModel model : PrinterModel.values()) {
            assertEquals(model, PrinterModel.find(model.serializedName()));
        }
        assertNull(PrinterModel.find("no_such_printer"));
    }

    @Test
    void queueNames_areEachTheirOwn() {
        final Set<String> names = new HashSet<>();
        for (final PrinterModel model : PrinterModel.values()) {
            assertTrue(names.add(model.queueName()), model.queueName() + " is taken twice");
        }
    }

    @Test
    void onlyTheDotMatrix_turnsOutFanfold() {
        for (final PrinterModel model : PrinterModel.values()) {
            assertEquals(model == PrinterModel.EPSILON_FX_80, model.sheet() == PrinterModel.Sheet.FANFOLD);
        }
    }

    @Test
    void pageTicks_areTheLengthOfEachPrintersSound() {
        assertEquals(144, PrinterModel.EPSILON_FX_80.pageTicks());
        assertEquals(70, PrinterModel.PAKARD_LASERJOT_1102.pageTicks());
        for (final PrinterModel model : PrinterModel.values()) {
            assertTrue(model.pageTicks() > 0);
        }
    }
}
