/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries;

import dev.jstech.core.JsCore;
import dev.jstech.core.registry.StableNamespaces;
import java.util.List;

/** The mods of the J's Tech Series, by id: the Core and each module built on it. */
public final class SeriesMods {

    /** Every mod of the series, the Core first. */
    public static final List<String> IDS = List.of(JsCore.MODID, "jsc", "jsindustrial", "jsspace", "jswarfare",
            "jstransport", "jsagriculture", "jscivil", "jsrobotics", "jsgeology", "jsoceanics");

    private SeriesMods() {
    }

    /**
     * Says the data of every mod of the series is stable, so a world of the series' dimensions and entries opens with
     * no warning about experimental settings; from the Core's constructor.
     */
    public static void declareStable() {
        for (final String id : IDS) {
            StableNamespaces.declare(id);
        }
    }
}
