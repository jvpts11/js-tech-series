/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.JsComputers;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.gui.CreativeTabsScreenPage;
import net.neoforged.neoforge.common.CreativeModeTabRegistry;

/**
 * The creative tabs of J's Computers as a player meets them: the creative inventory opened, each tab found on its page
 * and clicked, and what it shows looked at, with a picture of two of the eras.
 */
public final class CreativeTabsClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 40;
    /** How many times the page arrow is pressed at most to find a tab. */
    private static final int MOST_PAGES = 12;
    /** A tab button's size and the page arrow's place above the inventory, as the game draws them. */
    private static final int TAB_W = 27;
    private static final int TAB_H = 32;
    private static final int ARROW = 20;
    private static final int ARROW_ABOVE = 50;

    private CreativeTabsClientTests() {
    }

    @ClientTest(timeoutTicks = 600)
    public static void creativeTabs_eachEraShowsItsOwnThings(final ClientTestContext ctx) {
        ctx.then(0, () -> {
                    final LocalPlayer player = ctx.mc().player;
                    ctx.mc().setScreen(new CreativeModeInventoryScreen(player, player.connection.enabledFeatures(),
                            false));
                })
                .thenAwaitScreen(CreativeModeInventoryScreen.class, SCREEN_WAIT)
                .then(SETTLE, () -> openTab(ctx, "computing"))
                .then(2, () -> assertShows(ctx, ComputingModule.CRAFTING_CABLE.asItem(), "computing"))
                .then(SETTLE, () -> openTab(ctx, "computing_vintage"))
                .then(2, () -> assertShows(ctx, ComputingModule.FLOPPY_DISK.get(), "computing_vintage"))
                .then(SETTLE, () -> openTab(ctx, "computing_legacy"))
                .then(2, () -> assertShows(ctx, ComputingModule.CD_ROM.get(), "computing_legacy"))
                .then(SETTLE, () -> openTab(ctx, "computing_transition"))
                .then(2, () -> assertShows(ctx, HardwareItems.GPU_VERTEX_8800_GT.get(), "computing_transition"))
                .thenScreenshot(2, "transition-tab")
                .then(SETTLE, () -> openTab(ctx, "computing_standard"))
                .then(2, () -> assertShows(ctx, HardwareItems.MOTHERBOARD_ATX_STANDARD_LGA1150.get(),
                        "computing_standard"))
                .then(SETTLE, () -> openTab(ctx, "computing_advanced"))
                .then(2, () -> assertShows(ctx, HardwareItems.GPU_VERTEX_RTX_5090.get(), "computing_advanced"))
                .thenScreenshot(2, "advanced-tab")
                .then(SETTLE, () -> ctx.mc().setScreen(null));
    }

    /* Turns the pages until the tab is on the one shown, then clicks the tab's button. */
    private static void openTab(final ClientTestContext ctx, final String path) {
        final CreativeModeTab tab = CreativeModeTabRegistry.getTab(
                ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, path));
        ctx.assertTrue(tab != null, "the tab " + path + " is registered");
        final CreativeModeInventoryScreen screen = ctx.screen(CreativeModeInventoryScreen.class);
        for (int i = 0; i < MOST_PAGES && !screen.getCurrentPage().getVisibleTabs().contains(tab); i++) {
            ctx.click(screen.getGuiLeft() + screen.getXSize() - ARROW / 2.0,
                    screen.getGuiTop() - ARROW_ABOVE + ARROW / 2.0);
        }
        final CreativeTabsScreenPage page = screen.getCurrentPage();
        ctx.assertTrue(page.getVisibleTabs().contains(tab), "the tab " + path + " is on one of the pages");
        final double x = screen.getGuiLeft() + TAB_W * page.getColumn(tab) + TAB_W / 2.0;
        final double y = page.isTop(tab) ? screen.getGuiTop() - TAB_H / 2.0
                : screen.getGuiTop() + screen.getYSize() + TAB_H / 2.0;
        ctx.click(x, y);
    }

    /* The open tab shows {@code item} among its things. */
    private static void assertShows(final ClientTestContext ctx, final Item item, final String path) {
        final CreativeModeInventoryScreen screen = ctx.screen(CreativeModeInventoryScreen.class);
        boolean found = false;
        for (final ItemStack stack : screen.getMenu().items) {
            if (stack.is(item)) {
                found = true;
                break;
            }
        }
        ctx.assertTrue(found, "the tab " + path + " shows " + item + "; it shows " + screen.getMenu().items.size()
                + " things");
    }
}
