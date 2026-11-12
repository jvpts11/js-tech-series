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
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.program.Programs;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.testkit.client.ClientTestFailure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * Frames XP's Start on a computer with every program its system takes installed: the menu stands whole above the
 * taskbar, its left column holding the programs it has room for, and All Programs opening beside it the list of every
 * one, from which a program the column had no room for starts.
 */
public final class FramesXpStartClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 120;
    private static final int BOOT_WAIT = 1_200;
    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);
    private static final ResourceLocation FRAMES_XP = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
            "frames_xp");

    private FramesXpStartClientTests() {
    }

    @ClientTest(timeoutTicks = 3000)
    public static void framesXp_startStaysOnTheScreenAndAllProgramsListsTheRest(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
                    final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(COMPUTER, FRAMES_XP);
                    final OsDef system = OsRegistry.getOs(FRAMES_XP);
                    final int rank = OsRegistry.osVersionRank(FRAMES_XP);
                    // Every program XP lets a player install: the Frames', and none too new for it.
                    for (final ProgramSpec program : Programs.all()) {
                        if (JsComputers.MODID.equals(program.id().getNamespace()) && program.installable()
                                && program.platforms().contains(system.platform())
                                && (rank == 0 || rank >= program.minOsRank())) {
                            pc.console().install(program.id().toString());
                        }
                    }
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> !ctx.screen(DesktopScreen.class).launcherLabels().isEmpty(), SCREEN_WAIT,
                        "the desktop to list its programs")
                .then(SETTLE, () -> {
                    final DesktopScreen desktop = ctx.screen(DesktopScreen.class);
                    ctx.click(desktop.startButtonX(), desktop.startButtonY());
                })
                .thenWaitUntil(() -> ctx.screen(DesktopScreen.class).isStartOpen(), SCREEN_WAIT,
                        "the Start menu to open")
                .then(SETTLE * 3, () -> {
                    final DesktopScreen desktop = ctx.screen(DesktopScreen.class);
                    if (desktop.startMenuTop() < desktop.desktopY()) {
                        throw new ClientTestFailure("the Start menu runs above the desktop, its top at "
                                + desktop.startMenuTop() + " over the desktop's " + desktop.desktopY());
                    }
                })
                .thenScreenshot(0, "xp-start")
                .then(0, () -> clickLast(ctx))
                .thenWaitUntil(() -> ctx.screen(DesktopScreen.class).isAllProgramsOpen(), SCREEN_WAIT,
                        "All Programs to open, the last program having no room in the left column")
                .thenScreenshot(SETTLE * 3, "xp-all-programs")
                .then(0, () -> clickLast(ctx))
                .thenWaitUntil(() -> ctx.screen(DesktopScreen.class).windowFor(last(ctx)) != null, SCREEN_WAIT,
                        "the last program to open from All Programs")
                .thenWaitUntil(() -> !ctx.screen(DesktopScreen.class).isStartOpen(), SCREEN_WAIT,
                        "the Start menu to close behind it");
    }

    /* The last program of All Programs, the one furthest from the left column's room. */
    private static String last(final ClientTestContext ctx) {
        return ctx.screen(DesktopScreen.class).allProgramsLabels().getLast();
    }

    /* Clicks where the Start menu shows the last program: its row, or the All Programs row while it has none. */
    private static void clickLast(final ClientTestContext ctx) {
        final DesktopScreen desktop = ctx.screen(DesktopScreen.class);
        final int item = desktop.launcherLabels().indexOf(last(ctx));
        ctx.click(desktop.startMenuItemX(item), desktop.startMenuItemY(item));
    }
}
