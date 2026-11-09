/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.portable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.wireless.WirelessStandard;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import org.junit.jupiter.api.Test;

class PortableModelTest {

    @Test
    void values_areTheTwelveWithTheirKindsErasAndSystems() {
        assertEquals(12, PortableModel.values().length);
        assertSame(PortableKind.PDA, PortableModel.HIGHTIDE_POCKET.kind());
        assertSame(HardwareEra.LEGACY, PortableModel.HIGHTIDE_POCKET.era());
        assertSame(MobileSystem.FRAMES_POCKET_2002, PortableModel.HIGHTIDE_POCKET.system());
        assertSame(MobileSystem.DROIDIX_2009, PortableModel.MOTOROLLER_LANDMARK.system());
        assertSame(PortableKind.TABLET, PortableModel.QPAD_PRO.kind());
        assertSame(MobileSystem.QOS_2017, PortableModel.QPAD_PRO.system());
        assertSame(MobileFamily.DROIDIX, PortableModel.SUNSUNG_NEBULA_TAB_S7.system().family());
    }

    @Test
    void chip_writesOneCoresClockOrTheCoresAndTheirClock() {
        assertEquals("400 MHz", PortableModel.HIGHTIDE_POCKET.chip().english());
        assertEquals("2 x 1.3 GHz", PortableModel.QPHONE_5.chip().english());
        assertEquals("2 x 1 GHz", PortableModel.SUNSUNG_NEBULA_TAB.chip().english());
        assertEquals("Quince Q1, 8 x 3.2 GHz", PortableModel.QPAD_PRO.chip().english());
    }

    @Test
    void size_writesMegabytesBelowAGigabyteAndGigabytesFromOne() {
        assertEquals("64 MB", PortableModel.size(64).english());
        assertEquals("1 GB", PortableModel.size(1_024).english());
        assertEquals("128 GB", PortableModel.size(131_072).english());
    }

    @Test
    void storage_addsThePocketProsCard() {
        assertEquals(2_304, PortableModel.HIGHTIDE_POCKET_PRO.totalStorageMb());
        assertEquals(64, PortableModel.HIGHTIDE_POCKET.totalStorageMb());
    }

    @Test
    void wifi_isTheDevicesEras() {
        assertSame(WirelessStandard.LEGACY, PortableModel.HIGHTIDE_POCKET.wifi());
        assertSame(WirelessStandard.ADVANCED, PortableModel.SUNSUNG_NEBULA_S10.wifi());
    }

    @Test
    void byName_findsByTheItemsIdAndNothingElse() {
        assertSame(PortableModel.QPHONE_X, PortableModel.byName("qphone_x"));
        assertNull(PortableModel.byName("qphone_xx"));
    }

    @Test
    void screens_areTheFacesSizes() {
        assertEquals(144, PortableModel.HIGHTIDE_POCKET.screenWidth());
        assertEquals(243, PortableModel.QPHONE_X.screenHeight());
        assertTrue(PortableModel.SUNSUNG_NEBULA_TAB_S7.screenWidth() > PortableModel.SUNSUNG_NEBULA_TAB_S7
                .screenHeight(), "the Droidix tablets lie on their side");
    }
}
