/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import dev.jstech.core.connect.Connection;
import dev.jstech.core.connect.FacePorts;
import dev.jstech.core.connect.FaceRule;
import dev.jstech.core.jstechseries.peripheral.PeripheralLine;
import dev.jstech.core.jstechseries.tier.HardwareEra;

/**
 * Where a device takes its peripheral cable: the port in the middle of its back, where its model has it. A device's
 * port takes the peripheral cable of every era, as an old floppy drive or screen still works on a newer computer with
 * that computer's own cable. The era is held at the other end: a computer's port takes its era's cable and the earlier
 * ones only, so a newer cable never plugs into an older computer. A device against its computer needs no cable at all.
 */
public final class PeripheralSockets {

    /* The newest generation of the line, which takes every era's cable. */
    private static final Connection EVERY_ERA = PeripheralLine.of(HardwareEra.SERIES.getLast());
    private static final FacePorts BACK = FacePorts.builder().port(FaceRule.BACK, EVERY_ERA).build();
    /*
     * A screen faces the way its player looked when placing it, so its glass turns to that player and its facing
     * points out of its back: the port is on the face it faces.
     */
    private static final FacePorts BEHIND_SCREEN = FacePorts.builder().port(FaceRule.FRONT, EVERY_ERA).build();
    private static final FacePorts EVERY_FACE = FacePorts.builder().port(FaceRule.EVERY, EVERY_ERA).build();

    private PeripheralSockets() {
    }

    /** The port of a device, on its back. */
    public static FacePorts back() {
        return BACK;
    }

    /** The port of a screen, on its back. */
    public static FacePorts behindScreen() {
        return BEHIND_SCREEN;
    }

    /** The ports of a hub, one on every face. */
    public static FacePorts everyFace() {
        return EVERY_FACE;
    }
}
