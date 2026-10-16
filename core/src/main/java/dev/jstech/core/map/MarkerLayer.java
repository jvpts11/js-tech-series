/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.map;

import dev.jstech.core.text.TextKey;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * A layer of markers on the maps: places found, machines, veins found, or the areas of satellite coverage, pollution,
 * radiation and claims, each a layer a map shows or hides as one, in its own colour.
 *
 * @param id     its id
 * @param name   what it is called on a map
 * @param colour its markers' colour, {@code 0xRRGGBB}, unless a marker gives its own
 * @param areas  whether its markers are areas of chunks rather than points
 */
public record MarkerLayer(ResourceLocation id, TextKey name, int colour, boolean areas) {

    public MarkerLayer {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
    }
}
