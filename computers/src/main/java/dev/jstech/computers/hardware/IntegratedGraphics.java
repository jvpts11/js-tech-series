/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import java.util.Objects;

/**
 * The graphics a processor carries on its own die, as the desktop chips of the Haswell generation did: enough to drive
 * a screen from the board's own video output without a graphics card.
 *
 * @param model the graphics' own name, as the chip's specifications give it, or empty when the chip has none
 * @param units how many execution units it has: few, beside a graphics card's cores
 * @param mhz   the clock it runs at, at most
 */
public record IntegratedGraphics(String model, int units, int mhz) {

    /** A processor with no graphics of its own. */
    public static final IntegratedGraphics NONE = new IntegratedGraphics("", 0, 0);

    public IntegratedGraphics {
        Objects.requireNonNull(model, "a model may be empty but not missing");
        if (units < 0 || mhz < 0) {
            throw new IllegalArgumentException("integrated graphics cannot have negative units or clock");
        }
    }

    /** Whether the processor carries graphics at all. */
    public boolean present() {
        return units > 0;
    }
}
