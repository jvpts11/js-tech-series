/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.core.jstechseries.tier.Era;
import java.util.Objects;
import org.jetbrains.annotations.ApiStatus;

/**
 * A case fan (computers/docs/design/temperature.md): it sits in one of its case's fan seats, at the rear, on the top or
 * at the front, and takes away the heat it is rated for.
 *
 * @param era            the era it is made in: a case seats a fan of its own era or an earlier one
 * @param sizeMm         its size, in millimetres
 * @param takesAwayWatts the heat it takes away, in watts
 */
@ApiStatus.Experimental
public record CaseFanSpec(Era era, int sizeMm, int takesAwayWatts) {

    public CaseFanSpec {
        Objects.requireNonNull(era, "era");
        if (sizeMm <= 0 || takesAwayWatts < 0) {
            throw new IllegalArgumentException("a fan has a size and takes away no less than nothing");
        }
    }
}
