/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.api.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SurfaceRendererTest {

    @Test
    void constructor_acceptsTheLargestSize() {
        SurfaceRenderer renderer = renderer(SurfaceRenderer.MAX_SIZE, SurfaceRenderer.MAX_SIZE);
        assertEquals(SurfaceRenderer.MAX_SIZE, renderer.width());
        assertEquals(SurfaceRenderer.MAX_SIZE, renderer.height());
    }

    @Test
    void constructor_rejectsASizeThatWouldOverflowTheArea() {
        assertThrows(IllegalArgumentException.class, () -> renderer(65536, 65536));
    }

    @Test
    void constructor_rejectsAnEmptySide() {
        assertThrows(IllegalArgumentException.class, () -> renderer(0, 10));
        assertThrows(IllegalArgumentException.class, () -> renderer(10, -1));
    }

    private static SurfaceRenderer renderer(final int width, final int height) {
        return new SurfaceRenderer(width, height, false) {
            @Override
            public void frame(final ISurface surface, final double seconds) {
            }
        };
    }
}
