/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import dev.jstech.core.connect.FacePorts;
import dev.jstech.core.connect.FaceRule;
import dev.jstech.core.peripheral.PeripheralLine;
import dev.jstech.core.tier.HardwareEra;

/**
 * Where a peripheral takes its cable: the port of its era in the middle of its back, where its model has it. The port
 * takes its era's peripheral cable and every earlier one, never a newer one; a peripheral against its computer needs
 * no cable at all.
 */
public final class PeripheralSockets {

    private PeripheralSockets() {
    }

    /** The port of a peripheral of {@code era}, on its back. */
    public static FacePorts back(final HardwareEra era) {
        return FacePorts.builder().port(FaceRule.BACK, PeripheralLine.of(era)).build();
    }

    /**
     * The port of a screen of {@code era}, on its back. A screen faces the way its player looked when placing it, so
     * its glass turns to that player and its facing points out of its back: the port is on the face it faces.
     */
    public static FacePorts behindScreen(final HardwareEra era) {
        return FacePorts.builder().port(FaceRule.FRONT, PeripheralLine.of(era)).build();
    }
}
