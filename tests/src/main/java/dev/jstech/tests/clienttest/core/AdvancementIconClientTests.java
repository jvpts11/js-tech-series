/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import com.mojang.blaze3d.platform.NativeImage;
import dev.jstech.core.client.developer.Pictures;
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
 * over the frame behind it, wherever the tab's other advancements leave the root standing.
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
     * The window the tree is drawn in, 234 by 113: the root stands wherever the tab's other advancements leave it, so
     * its icon is looked for all over the window.
     */
    private static final int WINDOW_WIDTH = 234;
    private static final int WINDOW_HEIGHT = 113;
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
                // The pointer off the tree: an advancement under it would dim the rest of the tree round it.
                .then(0, () -> ctx.pointAt(1, 1))
                .then(SETTLE, () -> {
                    final AdvancementsScreen screen = ctx.screen(AdvancementsScreen.class);
                    final int x = (screen.width - FRAME_WIDTH) / 2 + WINDOW_LEFT;
                    final int y = (screen.height - FRAME_HEIGHT) / 2 + WINDOW_TOP;
                    try (NativeImage window = ctx.captureGuiPixels(x, y, WINDOW_WIDTH, WINDOW_HEIGHT);
                         NativeImage picture = read(ctx, ROCKET)) {
                        // Every pixel the picture has, somewhere in the tree; what shows through its clear ones is
                        // the frame's.
                        ctx.assertTrue(drawnSomewhere(window, picture), "the root's rocket is drawn in the tree, every "
                                + "pixel of the picture where the game would draw an item");
                    }
                })
                .thenScreenshot(0, "advancement-icon-tab")
                .then(SETTLE, () -> ctx.mc().setScreen(null));
    }

    /*
     * Whether the opaque pixels of {@code picture} stand somewhere in {@code window}, as a reference picture is
     * compared ({@link Pictures}): each a little off in a colour at most, and a very few off by more.
     */
    private static boolean drawnSomewhere(final NativeImage window, final NativeImage picture) {
        final int most = (int) Math.floor(ICON * ICON * Pictures.SHARE);
        for (int y = 0; y + ICON <= window.getHeight(); y++) {
            for (int x = 0; x + ICON <= window.getWidth(); x++) {
                if (offAt(window, picture, x, y, most) <= most) {
                    return true;
                }
            }
        }
        return false;
    }

    /* How many opaque pixels of the picture are off where it would stand at {@code x}, {@code y}, past {@code most}. */
    private static int offAt(final NativeImage window, final NativeImage picture, final int x, final int y,
                             final int most) {
        int off = 0;
        for (int py = 0; py < ICON && off <= most; py++) {
            for (int px = 0; px < ICON && off <= most; px++) {
                final int pixel = picture.getPixelRGBA(px, py);
                if ((pixel >>> 24) == OPAQUE && !near(pixel, window.getPixelRGBA(x + px, y + py))) {
                    off++;
                }
            }
        }
        return off;
    }

    private static boolean near(final int one, final int other) {
        for (int shift = 0; shift < 24; shift += 8) {
            if (Math.abs((one >> shift & OPAQUE) - (other >> shift & OPAQUE)) > Pictures.CHANNEL) {
                return false;
            }
        }
        return true;
    }

    private static NativeImage read(final ClientTestContext ctx, final ResourceLocation texture) {
        try (InputStream in = ctx.mc().getResourceManager().getResourceOrThrow(texture).open()) {
            return NativeImage.read(in);
        } catch (final IOException failed) {
            throw new IllegalStateException("the picture " + texture + " could not be read", failed);
        }
    }
}
