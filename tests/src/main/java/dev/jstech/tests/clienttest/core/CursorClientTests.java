/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

/**
 * The client tests leave whoever runs them their cursor: the game never takes the mouse while they play, not even as a
 * screen shuts and the world is played again, when it would take it otherwise; the tests turn, walk and click by
 * themselves.
 */
public final class CursorClientTests {

    private static final int SETTLE = 5;

    private CursorClientTests() {
    }

    @ClientTest(timeoutTicks = 200)
    public static void cursor_isLeftFreeWhileTheTestsPlay(final ClientTestContext ctx) {
        ctx.then(0, () -> ctx.mc().setScreen(new InventoryScreen(ctx.player())))
                .then(SETTLE, () -> ctx.mc().setScreen(null))
                .then(SETTLE, () -> ctx.assertTrue(ctx.mc().screen == null && !ctx.mc().mouseHandler.isMouseGrabbed(),
                        "a screen shut and the world played again, the cursor is still free"));
    }
}
