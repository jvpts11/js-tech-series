/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.block.CaseStyle;
import dev.jstech.computers.os.media.EjectButton;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CaseFrontLayoutTest {

    private static final List<HardwareEra> CASE_ERAS = List.of(HardwareEra.VINTAGE, HardwareEra.LEGACY,
            HardwareEra.TRANSITION, HardwareEra.STANDARD, HardwareEra.ADVANCED);

    @Test
    void of_givesEachCaseTheBaysAndPortsOfItsEra() {
        for (final HardwareEra era : CASE_ERAS) {
            for (final CaseStyle style : CaseStyle.values()) {
                final CaseFrontLayout front = CaseFrontLayout.of(era, style);
                assertEquals(CaseBays.of(era), front.bays().size(), era + " " + style);
                assertEquals(CasePorts.of(era), front.ports().size(), era + " " + style);
            }
        }
    }

    @Test
    void parts_lieOnTheFrontAndApart() {
        for (final HardwareEra era : CASE_ERAS) {
            for (final CaseStyle style : CaseStyle.values()) {
                final List<FrontArea> parts = parts(CaseFrontLayout.of(era, style));
                for (int i = 0; i < parts.size(); i++) {
                    final FrontArea part = parts.get(i);
                    assertTrue(part.left() >= 0 && part.top() >= 0 && part.right() <= 64 && part.bottom() <= 64
                            && part.left() < part.right() && part.top() < part.bottom(), era + " " + style + " " + part);
                    for (int j = i + 1; j < parts.size(); j++) {
                        assertFalse(overlap(part, parts.get(j)), era + " " + style + " " + part + " " + parts.get(j));
                    }
                }
            }
        }
    }

    @Test
    void ejectButton_liesInItsBayForEveryDrive() {
        final CaseFrontLayout legacy = CaseFrontLayout.of(HardwareEra.LEGACY, CaseStyle.SOLE);
        for (int bay = 0; bay < legacy.bays().size(); bay++) {
            final FrontArea at = legacy.bays().get(bay);
            for (final BayDriveSpec.Kind kind : List.of(BayDriveSpec.Kind.CD, BayDriveSpec.Kind.DVD,
                    BayDriveSpec.Kind.BLU_RAY)) {
                final EjectButton button = legacy.ejectButton(bay, kind);
                assertNotNull(button, kind.name());
                assertTrue(button.left() > at.left() && button.right() < at.right()
                        && button.top() > at.top() && button.bottom() <= at.bottom(), kind + " in bay " + bay);
            }
        }
    }

    @Test
    void ejectButton_isNoneForTheHotSwapBayOrABayTheFrontLacks() {
        final CaseFrontLayout vintage = CaseFrontLayout.of(HardwareEra.VINTAGE, CaseStyle.SOLE);
        assertNull(vintage.ejectButton(0, BayDriveSpec.Kind.HOT_SWAP));
        assertNull(vintage.ejectButton(1, BayDriveSpec.Kind.CD));
        assertNull(CaseFrontLayout.of(HardwareEra.ADVANCED, CaseStyle.NEUTRAL).ejectButton(0, BayDriveSpec.Kind.CD));
    }

    @Test
    void ejectButton_ofACdDriveIsWhereItsBezelPaintsIt() {
        // The Legacy's top bay spans texels 4 to 26 from row 2; a CD bezel paints its button 5 to 2 texels in from
        // its right end, on its fourth row.
        final EjectButton button = CaseFrontLayout.of(HardwareEra.LEGACY, CaseStyle.SOLE)
                .ejectButton(0, BayDriveSpec.Kind.CD);
        assertEquals(new EjectButton(44, 10, 50, 12, 0), button);
    }

    @Test
    void texels_countsFromATexelInFromTheBlocksSide() {
        assertEquals(new FrontArea(2, 0, 4, 2), CaseFrontLayout.texels(0, 0, 1, 1));
        assertEquals(new FrontArea(48, 18, 56, 30), CaseFrontLayout.texels(23, 9, 4, 6));
    }

    @Test
    void contains_takesThePartAndHalfAPixelRoundIt() {
        final FrontArea power = CaseFrontLayout.of(HardwareEra.STANDARD, CaseStyle.NEUTRAL).power();
        assertTrue(power.contains(52, 22));
        assertTrue(power.contains(46, 16));
        assertTrue(power.contains(58, 28));
        assertFalse(power.contains(45.9, 22));
        assertFalse(power.contains(52, 28.1));
    }

    @Test
    void middle_isHalfwayAcrossAndDown() {
        final FrontArea area = new FrontArea(10, 4, 54, 14);
        assertEquals(32.0, area.middleX());
        assertEquals(9.0, area.middleY());
    }

    private static List<FrontArea> parts(final CaseFrontLayout front) {
        final List<FrontArea> parts = new ArrayList<>();
        parts.add(front.power());
        parts.addAll(front.bays());
        parts.addAll(front.ports());
        return parts;
    }

    private static boolean overlap(final FrontArea a, final FrontArea b) {
        return a.left() < b.right() && b.left() < a.right() && a.top() < b.bottom() && b.top() < a.bottom();
    }
}
