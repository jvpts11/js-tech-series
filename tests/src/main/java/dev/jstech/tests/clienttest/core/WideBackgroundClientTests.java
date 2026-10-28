/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import com.mojang.blaze3d.platform.NativeImage;
import dev.jstech.core.client.advancement.WideBackgrounds;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.io.IOException;
import java.io.InputStream;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.resources.ResourceLocation;

/**
 * An advancement tab whose background has a wide picture: the whole picture drawn once behind the tree, its middle in
 * the window while the tree stands where it started, in place of the 16-pixel tiles the game would repeat. The tab is
 * the test mod's, with J's Space's sky.
 */
public final class WideBackgroundClientTests {

    private static final int SETTLE = 4;
    private static final int WAIT = 40;
    private static final ResourceLocation TAB = ResourceLocation.fromNamespaceAndPath("jstests", "wide_sky");
    private static final ResourceLocation WIDE = ResourceLocation.parse(
            "jsspace:textures/gui/advancements/sky_wide.png");
    private static final int SKY_WIDTH = 400;
    private static final int SKY_HEIGHT = 200;
    /* The advancements screen's frame and where its window stands in it, as the game lays them out. */
    private static final int FRAME_WIDTH = 252;
    private static final int FRAME_HEIGHT = 140;
    private static final int WINDOW_LEFT = 9;
    private static final int WINDOW_TOP = 18;
    /* A corner of the window, far from the tree in its middle. */
    private static final int CORNER = 40;

    private WideBackgroundClientTests() {
    }

    @ClientTest(timeoutTicks = 400)
    public static void wideBackground_drawsTheWholeSkyOnceInPlaceOfItsTiles(final ClientTestContext ctx) {
        ctx.thenServer(0, level -> ctx.server().getCommands().performPrefixedCommand(
                        ctx.serverPlayer().createCommandSourceStack(), "advancement grant @s only " + TAB))
                .then(SETTLE, () -> {
                    final ClientAdvancements advancements = ctx.mc().player.connection.getAdvancements();
                    ctx.mc().setScreen(new AdvancementsScreen(advancements));
                    advancements.setSelectedTab(advancements.get(TAB), true);
                })
                .thenAwaitScreen(AdvancementsScreen.class, WAIT)
                .then(SETTLE, () -> {
                    final AdvancementsScreen screen = ctx.screen(AdvancementsScreen.class);
                    final int x = (screen.width - FRAME_WIDTH) / 2 + WINDOW_LEFT;
                    final int y = (screen.height - FRAME_HEIGHT) / 2 + WINDOW_TOP;
                    // The tree has not moved, so the window shows the sky's middle.
                    final int u = (SKY_WIDTH - WideBackgrounds.WINDOW_WIDTH) / 2;
                    final int v = (SKY_HEIGHT - WideBackgrounds.WINDOW_HEIGHT) / 2;
                    try (NativeImage drawn = ctx.capture(x, y, CORNER, CORNER);
                         NativeImage sky = read(ctx, WIDE);
                         NativeImage expected = new NativeImage(CORNER, CORNER, false)) {
                        sky.copyRect(expected, u, v, 0, 0, CORNER, CORNER, false, false);
                        ctx.assertSamePicture("wide-sky-corner", drawn, expected);
                    }
                })
                .thenScreenshot(0, "wide-sky-tab")
                .then(SETTLE, () -> ctx.mc().setScreen(null));
    }

    private static NativeImage read(final ClientTestContext ctx, final ResourceLocation texture) {
        try (InputStream in = ctx.mc().getResourceManager().getResourceOrThrow(texture).open()) {
            return NativeImage.read(in);
        } catch (final IOException failed) {
            throw new IllegalStateException("the picture " + texture + " could not be read", failed);
        }
    }
}
