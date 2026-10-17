/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.tests.JsTests;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.resources.ResourceLocation;

/**
 * A passage between dimensions shown through a screen of the test mod's own, the glow of a re-entry, as a mod of the
 * series gives its passages one; it counts how often it was shown, for the test.
 */
public final class TestPassageScreen extends ReceivingLevelScreen {

    /** The screen's id, which the server names the passage with. */
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "test_passage");
    /** How many times it was shown. */
    public static final AtomicInteger SHOWN = new AtomicInteger();
    /* The glow of a re-entry. */
    private static final int GLOW = 0xFF << 24 | 0x6A2C12;

    public TestPassageScreen(final BooleanSupplier ready, final ReceivingLevelScreen.Reason reason) {
        super(ready, reason);
        SHOWN.incrementAndGet();
    }

    @Override
    public void renderBackground(final GuiGraphics graphics, final int mouseX, final int mouseY,
                                 final float partialTick) {
        graphics.fill(0, 0, this.width, this.height, GLOW);
    }
}
