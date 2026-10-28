/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import com.mojang.blaze3d.platform.NativeImage;
import dev.jstech.space.api.SpaceAdvancementTab;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.io.IOException;
import java.io.InputStream;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.resources.ResourceLocation;

/**
 * An advancement whose icon is a picture, drawn as that picture in place of an item: the root of J's Space's Leaving
 * the Earth tab, earned, shows its rocket in the tree where the game would draw an item, every pixel the picture has
 * over the frame behind it.
 */
public final class AdvancementIconClientTests {

    private static final int SETTLE = 4;
    private static final int WAIT = 40;
    private static final ResourceLocation ROCKET = ResourceLocation.parse(
            "jsspace:textures/gui/advancements/rocket.png");
    private static final int ICON = 16;
    /* The advancements screen's frame and where its window stands in it, as the game lays them out. */
    private static final int FRAME_WIDTH = 252;
    private static final int FRAME_HEIGHT = 140;
    private static final int WINDOW_LEFT = 9;
    private static final int WINDOW_TOP = 18;
    /*
     * Where a tab of one advancement draws its icon in the window: the game centres the lone widget, 28 by 27, on the
     * window's middle (117, 56), and draws the icon 8 across and 5 down in it.
     */
    private static final int ICON_X = 117 - 28 / 2 + 8;
    private static final int ICON_Y = 56 - 27 / 2 + 5;
    private static final int OPAQUE = 0xFF;

    private AdvancementIconClientTests() {
    }

    @ClientTest(timeoutTicks = 400)
    public static void picture_isDrawnInPlaceOfTheIconsItem(final ClientTestContext ctx) {
        final ResourceLocation root = SpaceAdvancementTab.LEAVING_THE_EARTH.root();
        ctx.thenServer(0, level -> ctx.server().getCommands().performPrefixedCommand(
                        ctx.serverPlayer().createCommandSourceStack(), "advancement grant @s only " + root))
                .then(SETTLE, () -> {
                    final ClientAdvancements advancements = ctx.mc().player.connection.getAdvancements();
                    ctx.mc().setScreen(new AdvancementsScreen(advancements));
                    advancements.setSelectedTab(advancements.get(root), true);
                })
                .thenAwaitScreen(AdvancementsScreen.class, WAIT)
                .then(SETTLE, () -> {
                    final AdvancementsScreen screen = ctx.screen(AdvancementsScreen.class);
                    final int x = (screen.width - FRAME_WIDTH) / 2 + WINDOW_LEFT + ICON_X;
                    final int y = (screen.height - FRAME_HEIGHT) / 2 + WINDOW_TOP + ICON_Y;
                    try (NativeImage drawn = ctx.capture(x, y, ICON, ICON);
                         NativeImage picture = read(ctx, ROCKET);
                         NativeImage expected = new NativeImage(ICON, ICON, false)) {
                        // What shows through the picture's clear pixels is the frame's; every other pixel is its.
                        drawn.copyRect(expected, 0, 0, 0, 0, ICON, ICON, false, false);
                        for (int py = 0; py < ICON; py++) {
                            for (int px = 0; px < ICON; px++) {
                                final int pixel = picture.getPixelRGBA(px, py);
                                if ((pixel >>> 24) == OPAQUE) {
                                    expected.setPixelRGBA(px, py, pixel);
                                }
                            }
                        }
                        ctx.assertSamePicture("rocket-icon", drawn, expected);
                    }
                })
                .thenScreenshot(0, "advancement-icon-tab")
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
