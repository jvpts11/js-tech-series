/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.core.api.client.GuideBlockRenderers;
import dev.jstech.core.palette.PaletteRoles;
import dev.jstech.tests.TestGuide;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.resources.ResourceLocation;

/**
 * The test mod's special block for the manuals, registered through the Core's API as another mod would: a swatch of
 * the colour its entry hands it, which counts how often it is drawn so a test can see it was.
 */
public final class TestGuideClient {

    /** How many times a swatch was drawn. */
    public static final AtomicInteger SWATCHES_DRAWN = new AtomicInteger();

    private TestGuideClient() {
    }

    /** Registers the swatch's renderer, from the test mod's client setup. */
    public static void register() {
        GuideBlockRenderers.register(ResourceLocation.parse(TestGuide.SWATCH_KIND),
                (graphics, font, x, y, width, height, data, mouseX, mouseY) -> {
                    final Integer colour = PaletteRoles.parse(data.getString("colour"));
                    graphics.fill(x, y, x + width, y + height, colour == null ? 0 : colour);
                    SWATCHES_DRAWN.incrementAndGet();
                });
    }
}
