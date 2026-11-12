/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * The Media Player in the look of each desktop (computers/docs/design/programs.md), opened on a running machine with
 * nothing chosen yet, shot for the side-by-side with its mocks: its frame and its controls.
 */
public final class MediaPlayerClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 120;
    private static final int BOOT_WAIT = 1_200;
    /* Long enough for the player to ask the machine what it has to play and draw what came back. */
    private static final int LIBRARY_WAIT = 30;
    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);
    private static final String MEDIA_PLAYER = "jsc:media_player";

    private MediaPlayerClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void mediaPlayer_frames95(final ClientTestContext ctx) {
        opened(ctx, "frames_95", null, "frames-95");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void mediaPlayer_framesXp(final ClientTestContext ctx) {
        opened(ctx, "frames_xp", null, "frames-xp");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void mediaPlayer_frames7(final ClientTestContext ctx) {
        opened(ctx, "frames_7", null, "frames-7");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void mediaPlayer_frames10(final ClientTestContext ctx) {
        opened(ctx, "frames_10", null, "frames-10");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void mediaPlayer_frames11(final ClientTestContext ctx) {
        opened(ctx, "frames_11", null, "frames-11");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void mediaPlayer_cde(final ClientTestContext ctx) {
        opened(ctx, "ubuntu", "jsc:cde", "cde");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void mediaPlayer_gnome(final ClientTestContext ctx) {
        opened(ctx, "ubuntu", "jsc:gnome", "gnome");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void mediaPlayer_kde(final ClientTestContext ctx) {
        opened(ctx, "ubuntu", "jsc:kde_plasma", "kde");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void mediaPlayer_cinnamon(final ClientTestContext ctx) {
        opened(ctx, "ubuntu", "jsc:cinnamon", "cinnamon");
    }

    /*
     * A running computer of {@code system}, with {@code desktop} over it when one is named, its Media Player opened and
     * shot.
     */
    private static void opened(final ClientTestContext ctx, final String system, @Nullable final String desktop,
                               final String label) {
        ctx.thenBuild(0, world -> {
                    final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(COMPUTER,
                            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, system));
                    if (desktop != null) {
                        pc.console().install(desktop);
                    }
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> !ctx.screen(DesktopScreen.class).launcherLabels().isEmpty(), SCREEN_WAIT,
                        "the desktop to list its programs")
                .then(SETTLE, () -> DesktopScreen.requestOpen(MEDIA_PLAYER))
                .thenWaitUntil(() -> ctx.screen(DesktopScreen.class).windowFor(MEDIA_PLAYER) != null, SCREEN_WAIT,
                        "the Media Player to open in a window")
                .thenScreenshot(LIBRARY_WAIT, "media-player-" + label);
    }
}
