/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.client.ServerRouterScreen;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.computers.EraKit;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import org.lwjgl.glfw.GLFW;

/**
 * The Server Router of each era in its window (computers/docs/design/servers-and-racks.md): its era where the window
 * names it, its name typed in, and the racks it manages of the most its era lets it, shot for the side-by-side with
 * its mock.
 */
public final class ServerRouterClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 120;
    private static final BlockPos ROUTER = new BlockPos(3, 2, 3);
    /** A point inside the window's name field, relative to the window. */
    private static final int NAME_X = 30;
    private static final int NAME_Y = 43;

    private ServerRouterClientTests() {
    }

    @ClientTest(timeoutTicks = 600)
    public static void serverRouter_legacyOpensItsWindow(final ClientTestContext ctx) {
        opened(ctx, EraKit.LEGACY, ComputingModule.LEGACY_SERVER_ROUTER.get());
    }

    @ClientTest(timeoutTicks = 600)
    public static void serverRouter_transitionOpensItsWindow(final ClientTestContext ctx) {
        opened(ctx, EraKit.TRANSITION, ComputingModule.TRANSITION_SERVER_ROUTER.get());
    }

    @ClientTest(timeoutTicks = 600)
    public static void serverRouter_standardOpensItsWindow(final ClientTestContext ctx) {
        opened(ctx, EraKit.STANDARD, ComputingModule.SERVER_ROUTER.get());
    }

    @ClientTest(timeoutTicks = 600)
    public static void serverRouter_advancedOpensItsWindow(final ClientTestContext ctx) {
        opened(ctx, EraKit.ADVANCED, ComputingModule.ADVANCED_SERVER_ROUTER.get());
    }

    /* The era's router placed, its window opened, a name typed into it and the window shot. */
    private static void opened(final ClientTestContext ctx, final EraKit kit, final Block router) {
        ctx.thenGive(0).thenBuild(0, world -> world.setBlock(ROUTER, router))
                .thenTeleport(SETTLE, ROUTER.south(2), Direction.NORTH)
                .thenRightClick(SETTLE, ROUTER)
                .thenAwaitScreen(ServerRouterScreen.class, SCREEN_WAIT)
                .then(2, () -> ctx.clickGui(NAME_X, NAME_Y))
                .then(2, () -> ctx.type("Row east"))
                .thenScreenshot(SETTLE, "router-" + kit.name().toLowerCase(Locale.ROOT))
                // The first Escape leaves the field, the second closes the window.
                .then(0, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .then(2, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(SCREEN_WAIT);
    }
}
