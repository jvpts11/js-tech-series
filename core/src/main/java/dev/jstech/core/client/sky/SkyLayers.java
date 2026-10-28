/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.sky;

import dev.jstech.core.sky.SkySpec;
import org.joml.Matrix4f;

/**
 * One sky's parts drawn over another's, as the Core draws them in their own sky: for a mod that lets one sky come
 * through another, as the stars of space come out over a world's sky on the way up to it.
 *
 * <pre>{@code
 * SkyLayers.stars(Skies.of(level, SPACE), event.getModelViewMatrix(), event.getProjectionMatrix(), 0, 0, 0.35F);
 * }</pre>
 */
public final class SkyLayers {

    private SkyLayers() {
    }

    /**
     * Draws the stars, the band of the galaxy and the nebulae of {@code sky} as they stand with its first sun
     * {@code celestial} turns round the sky, seen from {@code latitude} degrees north, as bright as the game's
     * starlight {@code night} makes them (0.5 at midnight), added over whatever is drawn. On the render thread, with
     * the game's matrices for the level.
     */
    public static void stars(final SkySpec sky, final Matrix4f modelView, final Matrix4f projection,
                             final double celestial, final double latitude, final float night) {
        SkyRenderer.stars(sky, modelView, projection, celestial, latitude, night);
    }
}
