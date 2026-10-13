/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.core.client.hud.HudElements;
import dev.jstech.core.client.hud.IHudElement;
import dev.jstech.core.gui.HudStack;
import dev.jstech.tests.JsTests;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Two HUD elements of the test mod, declared as another mod would declare its own, that show only while a client
 * test turns them on: a tall red bar and a short blue one, stacked in the top right corner.
 */
public final class TestHud {

    public static final ResourceLocation TALL = ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "tall_bar");
    public static final ResourceLocation SHORT = ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "short_bar");

    private static volatile boolean showing;

    private TestHud() {
    }

    /** Declares the elements, from the test mod's client set-up. */
    public static void declare() {
        HudElements.declare(TALL, HudStack.Corner.TOP_RIGHT, new Bar(60, 24, 0xFFD04040));
        HudElements.declare(SHORT, HudStack.Corner.TOP_RIGHT, new Bar(40, 10, 0xFF4060D0));
    }

    /** Turns the elements on or off. */
    public static void show(final boolean on) {
        showing = on;
    }

    /* A plain bar of one colour. */
    private record Bar(int width, int height, int colour) implements IHudElement {

        @Override
        public boolean shown(final Minecraft minecraft) {
            return showing;
        }

        @Override
        public HudStack.Size size(final Minecraft minecraft) {
            return new HudStack.Size(width, height);
        }

        @Override
        public void render(final GuiGraphics graphics, final DeltaTracker delta, final int x, final int y) {
            graphics.fill(x, y, x + width, y + height, colour);
        }
    }
}
