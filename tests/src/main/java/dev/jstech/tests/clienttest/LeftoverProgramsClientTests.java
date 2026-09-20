/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * The eight programs of the closing slice, opened the way a player opens them.
 *
 * <p>What is being held to account here is the thing a unit test cannot answer: that each of them is
 * registered, that the desktop knows how to build its window, and that drawing one does not bring the
 * screen down. Every rule inside them is covered by plain JUnit over the engines; this is the wiring.
 *
 * <p>A screenshot of each goes with it, because these are programs to look at and a person reading the
 * report should be able to see what came up.
 */
public final class LeftoverProgramsClientTests {

    private LeftoverProgramsClientTests() {
    }

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 400;
    private static final int BOOT_WAIT = 600;

    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);

    private static ResourceLocation jsc(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, path);
    }

    /** The eight, by the label their launcher carries and the name of the shot taken of each. */
    private static final List<String> PROGRAMS = List.of(
            "Solitaire", "Snake", "67ark", "Paint", "Exceed", "Midsoft Messenger", "Knot", "Editor");

    private static DesktopScreen desktop(final ClientTestContext ctx) {
        return ctx.screen(DesktopScreen.class);
    }

    /** A desktop machine with every one of the new programs installed, and the player at its monitor. */
    private static ClientTestContext atTheDesktop(final ClientTestContext ctx) {
        return ctx.thenBuild(0, builder -> {
                    final CraftingComputerBlockEntity computer = builder.placeRunningCraftingComputer(COMPUTER);
                    computer.togglePower();
                    computer.formatDisk(0);
                    TestWorldBuilder.installDesktop(computer, jsc("frames_11"),
                            jsc("solitaire"), jsc("snake"), jsc("ark"), jsc("paint"),
                            jsc("exceed"), jsc("messenger"), jsc("knot"));
                    computer.togglePower();
                    builder.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT);
    }

    /**
     * Every one of them opens, draws, and closes again.
     *
     * <p>Opened one after another rather than all at once, so a window that breaks names itself instead of
     * bringing the desktop down with seven others on it.
     */
    @ClientTest(timeoutTicks = 7200)
    public static void everyNewProgram_opensAndDraws(final ClientTestContext ctx) {
        ClientTestContext run = atTheDesktop(ctx);
        for (final String label : PROGRAMS) {
            run = run
                    .then(SETTLE, () -> DesktopScreen.requestOpen(label))
                    .thenWaitUntil(() -> !desktop(ctx).windowsFor(label).isEmpty(), SCREEN_WAIT,
                            label + " to open a window")
                    .then(SETTLE * 2, () -> ctx.assertTrue(ctx.mc().screen instanceof DesktopScreen,
                            label + " took the desktop down; got " + ctx.mc().screen))
                    .thenScreenshot(2, "program_" + label.toLowerCase(java.util.Locale.ROOT)
                            .replace(' ', '_'));
        }
        run.then(SETTLE, () -> ctx.assertTrue(ctx.mc().screen instanceof DesktopScreen,
                "the desktop should still be up with all eight open"));
    }

    /**
     * The launchers are there at all.
     *
     * <p>A program that is installed and has no launcher is one a player can never reach, which has
     * happened here before: the rail was only rebuilt when two particular programs changed.
     */
    @ClientTest(timeoutTicks = 3600)
    public static void everyNewProgram_hasALauncher(final ClientTestContext ctx) {
        atTheDesktop(ctx)
                .then(SETTLE * 2, () -> {
                    final List<String> openable = DesktopScreen.openableLabels();
                    for (final String label : PROGRAMS) {
                        ctx.assertTrue(openable.contains(label),
                                label + " has no launcher; the desktop offers " + openable);
                    }
                });
    }
}
