/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.universe;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

/**
 * The colours of the ground on a map of a far world, the same the climb paints its far map in, for the maps drawn
 * elsewhere than the climb: a Flight Computer's landing map. On the render thread.
 */
public final class MapGround {

    private static GroundColours colours;

    private MapGround() {
    }

    /** The colour of a pixel of {@code biome} whose ground is {@code height} blocks high, the sea at {@code sea}. */
    public static int colour(final Holder<Biome> biome, final int height, final int sea) {
        if (colours == null) {
            colours = new GroundColours();
        }
        return colours.ofMap(biome, height, sea);
    }
}
