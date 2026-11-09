/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.portable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;

class MobileAppTest {

    @Test
    void runsOn_followsTheErasAndTheHardware() {
        assertFalse(MobileApp.CRYPTO_WALLET.runsOn(PortableModel.HIGHTIDE_POCKET), "the wallet from the Transition");
        assertTrue(MobileApp.CRYPTO_WALLET.runsOn(PortableModel.HIGHTIDE_POCKET_PRO));
        assertFalse(MobileApp.MESSENGER.runsOn(PortableModel.QPHONE), "the Messenger from the Standard");
        assertTrue(MobileApp.MESSENGER.runsOn(PortableModel.QPHONE_5));
        assertFalse(MobileApp.CAMERA.runsOn(PortableModel.HIGHTIDE_POCKET_PRO), "no camera on a PDA");
        assertFalse(MobileApp.PROSPECTING.runsOn(PortableModel.QPHONE), "prospecting from the Standard");
        assertTrue(MobileApp.PROSPECTING.runsOn(PortableModel.SUNSUNG_NEBULA_TAB));
    }

    @Test
    void factory_isTheNetworkTheAlertsAndTheHardwaresApps() {
        assertEquals(EnumSet.of(MobileApp.NETWORK, MobileApp.ALERTS), MobileApp.factory(
                PortableModel.HIGHTIDE_POCKET_PRO));
        assertEquals(EnumSet.of(MobileApp.NETWORK, MobileApp.ALERTS, MobileApp.CAMERA, MobileApp.GALLERY,
                MobileApp.MAPS), MobileApp.factory(PortableModel.QPAD_PRO));
    }

    @Test
    void words_areTheMocksNames() {
        assertEquals("Crypto Wallet", MobileApp.CRYPTO_WALLET.word().english());
        assertEquals("Wallet", MobileApp.CRYPTO_WALLET.label().english());
        assertEquals("Prospect", MobileApp.PROSPECTING.label().english());
        assertEquals("Maps", MobileApp.MAPS.label().english());
        assertEquals("Quince Store", MobileFamily.QOS.store().english());
    }
}
