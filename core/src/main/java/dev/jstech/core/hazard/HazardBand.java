/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.hazard;

import dev.jstech.core.text.TextKey;
import java.util.List;

/**
 * One band of a hazard, from how much has built up: what it is called (Rad II, a slight cough) and what it does.
 *
 * @param from    the least built up for the band, in the hazard's unit
 * @param name    what the band is called
 * @param effects what it does while the body stands in it
 */
public record HazardBand(double from, TextKey name, List<IHazardEffect> effects) {

    public HazardBand {
        if (from < 0) {
            throw new IllegalArgumentException("a band begins at nothing or more: " + from);
        }
        effects = List.copyOf(effects);
    }
}
