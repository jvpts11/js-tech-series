/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.tests.testkit.EraKit;
import org.lwjgl.glfw.GLFW;

/**
 * Each era's systems on its own network, as a player meets them: the era's Personal Computer, on the era's access
 * line, comes up in the era's own system on its monitor.
 */
public final class EraSystemsClientTests {

    private EraSystemsClientTests() {
    }

    @ClientTest(timeoutTicks = 3600)
    public static void vintage_computerComesUpInItsSystem(final ClientTestContext ctx) {
        comesUp(ctx, EraKit.VINTAGE);
    }

    @ClientTest(timeoutTicks = 3600)
    public static void legacy_computerComesUpInItsSystem(final ClientTestContext ctx) {
        comesUp(ctx, EraKit.LEGACY);
    }

    @ClientTest(timeoutTicks = 3600)
    public static void transition_computerComesUpInItsSystem(final ClientTestContext ctx) {
        comesUp(ctx, EraKit.TRANSITION);
    }

    @ClientTest(timeoutTicks = 3600)
    public static void standard_computerComesUpInItsSystem(final ClientTestContext ctx) {
        comesUp(ctx, EraKit.STANDARD);
    }

    @ClientTest(timeoutTicks = 3600)
    public static void advanced_computerComesUpInItsSystem(final ClientTestContext ctx) {
        comesUp(ctx, EraKit.ADVANCED);
    }

    private static void comesUp(final ClientTestContext ctx, final EraKit kit) {
        EraJourney.atTheDesk(ctx, kit)
                .thenScreenshot(EraJourney.SETTLE, kit.name().toLowerCase() + "-system")
                .then(0, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(EraJourney.SCREEN_WAIT);
    }
}
