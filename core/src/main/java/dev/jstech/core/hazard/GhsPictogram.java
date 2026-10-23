/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.hazard;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * The nine pictograms of the GHS, the red diamonds on a chemical's label and its safety data sheet, by their codes.
 *
 * <p>Pure: no game types.
 */
@TextHolder
public enum GhsPictogram {
    EXPLOSIVE("GHS01", TextKey.of("jscore.ghs.explosive", "Explosive")),
    FLAME("GHS02", TextKey.of("jscore.ghs.flame", "Flammable")),
    FLAME_OVER_CIRCLE("GHS03", TextKey.of("jscore.ghs.flame_over_circle", "Oxidising")),
    GAS_CYLINDER("GHS04", TextKey.of("jscore.ghs.gas_cylinder", "Gas under pressure")),
    CORROSION("GHS05", TextKey.of("jscore.ghs.corrosion", "Corrosive")),
    SKULL("GHS06", TextKey.of("jscore.ghs.skull", "Toxic")),
    EXCLAMATION_MARK("GHS07", TextKey.of("jscore.ghs.exclamation_mark", "Harmful")),
    HEALTH_HAZARD("GHS08", TextKey.of("jscore.ghs.health_hazard", "Health hazard")),
    ENVIRONMENT("GHS09", TextKey.of("jscore.ghs.environment", "Hazardous to the environment"));

    private final String code;
    private final TextKey name;

    GhsPictogram(final String code, final TextKey name) {
        this.code = code;
        this.name = name;
    }

    /** Its code: GHS02. */
    public String code() {
        return this.code;
    }

    /** What it means. */
    public TextKey nameKey() {
        return this.name;
    }
}
