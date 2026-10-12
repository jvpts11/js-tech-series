/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.client.CommandPromptScreen;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.tests.testkit.EraKit;
import dev.jstech.tests.testkit.EraNetwork;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;

/**
 * The steps every era's journey shares: the era's network laid as {@link EraNetwork} lays it, and the player sitting
 * down at its Personal Computer, which comes up in the era's own system: a command prompt for MC-DOS, a desktop for
 * every graphical system.
 */
final class EraJourney {

    static final int SETTLE = 4;
    static final int SCREEN_WAIT = 80;
    /** Long enough for a cold start's self-test and the system's start on the era's own disk. */
    static final int BOOT_WAIT = 2_400;

    private EraJourney() {
    }

    /** Lays the era's network in the test's area. */
    static ClientTestContext lay(final ClientTestContext ctx, final EraKit kit) {
        return ctx.thenGive(0).thenBuild(0, world -> EraNetwork.lay(world, kit));
    }

    /** Lays the era's network and sits the player at its computer until the era's system is up on the monitor. */
    static ClientTestContext atTheDesk(final ClientTestContext ctx, final EraKit kit) {
        return lay(ctx, kit)
                .thenTeleport(SETTLE, EraNetwork.AT_THE_DESK, Direction.WEST)
                .thenRightClick(SETTLE, EraNetwork.MONITOR)
                .thenAwaitScreen(systemScreen(kit), BOOT_WAIT);
    }

    /** The screen the era's desktop system shows a seated player. */
    static Class<? extends Screen> systemScreen(final EraKit kit) {
        return kit == EraKit.VINTAGE ? CommandPromptScreen.class : DesktopScreen.class;
    }
}
