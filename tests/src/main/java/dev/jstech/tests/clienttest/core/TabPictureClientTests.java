/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import com.mojang.blaze3d.platform.NativeImage;
import dev.jstech.tests.TestItems;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.io.IOException;
import java.io.InputStream;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.client.gui.CreativeTabsScreenPage;
import net.neoforged.neoforge.common.CreativeModeTabRegistry;

/**
 * A creative tab that shows a picture on its button in place of an item: the test mod's tab with J's Space's Saturn V,
 * found on its page and its button's icon compared with the picture.
 */
public final class TabPictureClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 40;
    /** How many times the page arrow is pressed at most to find a tab. */
    private static final int MOST_PAGES = 12;
    /** A tab button's width, the page arrow's place above the inventory, and an icon's place in a button. */
    private static final int TAB_W = 27;
    private static final int ARROW = 20;
    private static final int ARROW_ABOVE = 50;
    private static final int ICON_LEFT = 5;
    private static final int ICON_TOP_ROW = -19;
    private static final int ICON_BOTTOM_ROW = 3;
    private static final int ICON = 16;
    private static final ResourceLocation PICTURE = ResourceLocation.parse("jsspace:textures/gui/tab_icon.png");

    private TabPictureClientTests() {
    }

    @ClientTest(timeoutTicks = 400)
    public static void tabPicture_isDrawnInPlaceOfTheTabsItem(final ClientTestContext ctx) {
        ctx.then(0, () -> {
                    final LocalPlayer player = ctx.mc().player;
                    ctx.mc().setScreen(new CreativeModeInventoryScreen(player, player.connection.enabledFeatures(),
                            false));
                })
                .thenAwaitScreen(CreativeModeInventoryScreen.class, SCREEN_WAIT)
                .then(SETTLE, () -> turnTo(ctx))
                .then(SETTLE, () -> {
                    final CreativeModeInventoryScreen screen = ctx.screen(CreativeModeInventoryScreen.class);
                    final CreativeModeTab tab = tab(ctx);
                    final CreativeTabsScreenPage page = screen.getCurrentPage();
                    final int x = screen.getGuiLeft() + TAB_W * page.getColumn(tab) + ICON_LEFT;
                    final int y = page.isTop(tab) ? screen.getGuiTop() + ICON_TOP_ROW
                            : screen.getGuiTop() + screen.getYSize() + ICON_BOTTOM_ROW;
                    try (NativeImage drawn = ctx.capture(x, y, ICON, ICON); NativeImage picture = read(ctx)) {
                        ctx.assertSamePicture("tab-picture", drawn, picture);
                    }
                })
                .thenScreenshot(0, "pictured-tab")
                .then(SETTLE, () -> ctx.mc().setScreen(null));
    }

    private static CreativeModeTab tab(final ClientTestContext ctx) {
        final CreativeModeTab tab = CreativeModeTabRegistry.getTab(TestItems.PICTURED_TAB.id());
        ctx.assertTrue(tab != null, "the pictured tab is registered");
        return tab;
    }

    /* Turns the pages until the pictured tab is on the one shown. */
    private static void turnTo(final ClientTestContext ctx) {
        final CreativeModeTab tab = tab(ctx);
        final CreativeModeInventoryScreen screen = ctx.screen(CreativeModeInventoryScreen.class);
        for (int i = 0; i < MOST_PAGES && !screen.getCurrentPage().getVisibleTabs().contains(tab); i++) {
            ctx.click(screen.getGuiLeft() + screen.getXSize() - ARROW / 2.0,
                    screen.getGuiTop() - ARROW_ABOVE + ARROW / 2.0);
        }
        ctx.assertTrue(screen.getCurrentPage().getVisibleTabs().contains(tab), "the pictured tab is on a page");
    }

    private static NativeImage read(final ClientTestContext ctx) {
        try (InputStream in = ctx.mc().getResourceManager().getResourceOrThrow(PICTURE).open()) {
            return NativeImage.read(in);
        } catch (final IOException failed) {
            throw new IllegalStateException("the picture " + PICTURE + " could not be read", failed);
        }
    }
}
