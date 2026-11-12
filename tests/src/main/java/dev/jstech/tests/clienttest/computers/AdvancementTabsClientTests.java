/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.JsComputers;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.resources.ResourceLocation;

/**
 * The advancement tabs Carbon brought (computers/docs/design/advancements.md): The Fab on its white concrete and
 * Teracoin on its gold, each with every advancement it holds earned, shot for the side-by-side with their mocks.
 */
public final class AdvancementTabsClientTests {

    private static final int SETTLE = 4;
    private static final int WAIT = 60;

    private AdvancementTabsClientTests() {
    }

    @ClientTest(timeoutTicks = 400)
    public static void fabTab_showsItsAdvancementsOnWhiteConcrete(final ClientTestContext ctx) {
        tab(ctx, "fab");
    }

    @ClientTest(timeoutTicks = 400)
    public static void teracoinTab_showsItsAdvancementsOnGold(final ClientTestContext ctx) {
        tab(ctx, "teracoin");
    }

    /* Every advancement of the tab {@code name} earned, the advancements' screen opened on it, and shot. */
    private static void tab(final ClientTestContext ctx, final String name) {
        final ResourceLocation root = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, name + "/root");
        ctx.thenServer(0, level -> ctx.server().getCommands().performPrefixedCommand(
                        ctx.serverPlayer().createCommandSourceStack(), "advancement grant @s from " + root))
                // The toasts of what was just earned go first, so the tab is seen alone.
                .then(SETTLE * 2, () -> ctx.mc().getToasts().clear())
                .then(SETTLE, () -> {
                    final ClientAdvancements advancements = ctx.mc().player.connection.getAdvancements();
                    ctx.mc().setScreen(new AdvancementsScreen(advancements));
                    advancements.setSelectedTab(advancements.get(root), true);
                })
                .thenAwaitScreen(AdvancementsScreen.class, WAIT)
                // The pointer off the tree: an advancement under it would dim the rest of the tree round it.
                .then(0, () -> ctx.pointAt(1, 1))
                .thenScreenshot(SETTLE, name + "-tab")
                .then(SETTLE, () -> ctx.mc().setScreen(null));
    }
}
