/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.peripheral;

/**
 * The point-to-point cable systems: a computer's peripherals, and the industrial control link between a controller and
 * its machine. Telemetry is no longer one of them: it is a line of its own, which groups what it joins.
 */
public enum PeripheralCableType {

    COMPUTING(16, "peripheral_cable"),
    INDUSTRIAL_CONTROL(8, "industrial_peripheral_cable");

    private final int maxLength;
    private final String translationKey;

    PeripheralCableType(final int maxLength, final String translationKey) {
        this.maxLength = maxLength;
        this.translationKey = translationKey;
    }

    public int maxLength() {
        return maxLength;
    }

    public String translationKey() {
        return translationKey;
    }
}
