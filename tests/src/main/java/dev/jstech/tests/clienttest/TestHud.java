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
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.tests.JsTests;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Two HUD elements of the test mod, declared as another mod would declare its own, that show only while a client
 * test turns them on: a tall red bar and a short blue one, stacked in the top right corner.
 */
@TextHolder
public final class TestHud {

    public static final ResourceLocation TALL = ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "tall_bar");
    public static final ResourceLocation SHORT = ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "short_bar");
    /** Their names in the HUD editor, under the keys of their ids. */
    public static final TextKey TALL_NAME = TextKey.of("hud.jstests.tall_bar", "Tall bar");
    public static final TextKey SHORT_NAME = TextKey.of("hud.jstests.short_bar", "Short bar");

    /* Where each bar was last drawn, its top left. */
    private static final Map<ResourceLocation, HudStack.Point> DRAWN = new ConcurrentHashMap<>();

    private static volatile boolean showing;

    private TestHud() {
    }

    /** Declares the elements, from the test mod's client set-up. */
    public static void declare() {
        HudElements.declare(TALL, HudStack.Corner.TOP_RIGHT, new Bar(TALL, 60, 24, 0xFFD04040));
        HudElements.declare(SHORT, HudStack.Corner.TOP_RIGHT, new Bar(SHORT, 40, 10, 0xFF4060D0));
    }

    /** Turns the elements on or off, forgetting where they were drawn. */
    public static void show(final boolean on) {
        showing = on;
        DRAWN.clear();
    }

    /** Where the bar {@code id} was last drawn, its top left, since they were last turned on or off. */
    public static Optional<HudStack.Point> drawnAt(final ResourceLocation id) {
        return Optional.ofNullable(DRAWN.get(id));
    }

    /* A plain bar of one colour. */
    private record Bar(ResourceLocation id, int width, int height, int colour) implements IHudElement {

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
            DRAWN.put(id, new HudStack.Point(x, y));
            graphics.fill(x, y, x + width, y + height, colour);
        }
    }
}
