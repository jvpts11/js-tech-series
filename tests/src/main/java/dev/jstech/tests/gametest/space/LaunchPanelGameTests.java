/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.space.api.Propellants;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.launch.LaunchPanel;
import dev.jstech.space.launch.LaunchPanelBlockEntity;
import dev.jstech.space.launcher.LauncherBlocks;
import dev.jstech.space.launcher.LauncherDeckBlockEntity;
import dev.jstech.space.rocket.RocketKitBlocks;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.SkeletonPropellants;
import dev.jstech.space.rocket.SkeletonRocketParts;
import dev.jstech.space.rocket.TankLoad;
import dev.jstech.tests.JsTests;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Launch Panel: with no rocket on the pad, or in a thunderstorm, its count will not start; with its key and its
 * ignition armed and all go, the count runs from T-60 on the panel and on the rocket's clock, holds at T-6 while the
 * LAUNCH button has not been pressed, runs on once it is, and releases the rocket at T-0.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class LaunchPanelGameTests {

    /* A wide arena, the panel in its middle, so what it looks for round it is all its own test's. */
    private static final String WIDE = "bench";
    private static final String BATCH = "jsspace_launch_panel";
    private static final BlockPos PANEL = new BlockPos(24, 1, 24);
    private static final BlockPos ARM = new BlockPos(27, 3, 27);
    private static final BlockPos COMPUTER = new BlockPos(28, 7, 28);
    private static final int SECOND = 20;

    private LaunchPanelGameTests() {
    }

    @GameTest(template = WIDE, batch = BATCH + "_safe", timeoutTicks = 200)
    public static void panel_willNotCountWithoutARocketOrInAStorm(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        helper.setBlock(PANEL, LaunchPanel.BLOCK.get());
        final BlockPos panel = helper.absolutePos(PANEL);
        helper.startSequence()
                .thenIdle(SECOND + 2)
                .thenExecute(() -> {
                    final LaunchPanelBlockEntity console = panel(level, panel);
                    arm(console);
                    console.press(LaunchPanelBlockEntity.RUN);
                    helper.assertTrue(!console.vehicle() && console.phase() == LaunchPanelBlockEntity.Phase.SAFE,
                            "with no rocket on the pad the count will not start");
                    rocket(helper);
                })
                .thenWaitUntil(() -> helper.assertTrue(panel(level, panel).go(), "a fuelled rocket on the pad is go"))
                .thenExecute(() -> {
                    level.setWeatherParameters(0, 6000, true, true);
                    level.setRainLevel(1.0F);
                    level.setThunderLevel(1.0F);
                })
                .thenWaitUntil(() -> helper.assertTrue(panel(level, panel).storm(), "it sees the thunderstorm"))
                .thenExecute(() -> {
                    final LaunchPanelBlockEntity console = panel(level, panel);
                    console.press(LaunchPanelBlockEntity.RUN);
                    level.setWeatherParameters(6000, 0, false, false);
                    level.setThunderLevel(0.0F);
                    level.setRainLevel(0.0F);
                    helper.assertTrue(console.phase() == LaunchPanelBlockEntity.Phase.SAFE,
                            "and in a thunderstorm the count will not start");
                })
                .thenSucceed();
    }

    @GameTest(template = WIDE, batch = BATCH + "_count", timeoutTicks = 1700)
    public static void panel_countsDownHoldsForLaunchAndReleases(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        // Fair weather for the whole count, whatever the sky was doing.
        level.setWeatherParameters(6000, 0, false, false);
        level.setThunderLevel(0.0F);
        level.setRainLevel(0.0F);
        helper.setBlock(PANEL, LaunchPanel.BLOCK.get());
        rocket(helper);
        final BlockPos panel = helper.absolutePos(PANEL);
        final BlockPos computer = helper.absolutePos(COMPUTER);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(panel(level, panel).go(), "a fuelled rocket on the pad is go"))
                .thenExecute(() -> {
                    final LaunchPanelBlockEntity console = panel(level, panel);
                    arm(console);
                    console.press(LaunchPanelBlockEntity.RUN);
                    helper.assertTrue(console.phase() == LaunchPanelBlockEntity.Phase.COUNTING
                            && console.left() == LaunchPanelBlockEntity.START * SECOND, "the count starts at T-60");
                })
                .thenIdle(2 * SECOND)
                .thenExecute(() -> helper.assertTrue(level.getBlockEntity(computer) instanceof FlightComputerBlockEntity
                        rocket && rocket.clock() < 0 && rocket.clock() > -LaunchPanelBlockEntity.START,
                        "the count runs on the rocket's clock too"))
                .thenWaitUntil(() -> helper.assertTrue(panel(level, panel).phase()
                        == LaunchPanelBlockEntity.Phase.HOLDING, "without LAUNCH pressed it holds"))
                .thenExecute(() -> {
                    final LaunchPanelBlockEntity console = panel(level, panel);
                    helper.assertTrue(console.left() == LaunchPanelBlockEntity.LIGHT * SECOND && console.coverUp(),
                            "at T-6, the LAUNCH button's cover up: " + console.left());
                    console.press(LaunchPanelBlockEntity.LAUNCH);
                    helper.assertTrue(console.phase() == LaunchPanelBlockEntity.Phase.COUNTING,
                            "LAUNCH pressed, the count runs on");
                })
                .thenWaitUntil(() -> helper.assertTrue(panel(level, panel).phase()
                        == LaunchPanelBlockEntity.Phase.RELEASED, "and at T-0 the rocket is released"))
                .thenSucceed();
    }

    /* Turns the key and arms the ignition under its guard. */
    private static void arm(final LaunchPanelBlockEntity console) {
        console.press(LaunchPanelBlockEntity.MASTER);
        console.press(LaunchPanelBlockEntity.GUARD);
        console.press(LaunchPanelBlockEntity.IGNITION);
    }

    /* A launcher's measuring arm by the panel and, up its axis, a rocket's flight computer, its tank full. */
    private static void rocket(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        helper.setBlock(ARM, LauncherBlocks.HOLD_DOWN_ARM.get());
        helper.setBlock(COMPUTER, RocketKitBlocks.FLIGHT_COMPUTER.get());
        if (level.getBlockEntity(helper.absolutePos(ARM)) instanceof LauncherDeckBlockEntity arm) {
            arm.deck(new LauncherDeckBlockEntity.Deck(Direction.EAST, 1, new BlockPos(1, 0, 1), 6, 8, 6));
        }
        final RocketStack stack = RocketStack.of(new RocketStack.Part(SkeletonRocketParts.id("sea_level_engine_3"), 1),
                new RocketStack.Part(SkeletonRocketParts.id("tank_3"), 1),
                new RocketStack.Part(SkeletonRocketParts.id("capsule_3"), 1));
        if (level.getBlockEntity(helper.absolutePos(COMPUTER)) instanceof FlightComputerBlockEntity rocket) {
            rocket.carry(new RocketDesign("PANEL", stack), new BlockPos(0, -4, 0), List.of());
            Propellants.get(level, SkeletonPropellants.METHALOX).ifPresent(methalox -> rocket.load(1,
                    TankLoad.filled(SkeletonPropellants.METHALOX, methalox, 5_800.0)));
        }
    }

    private static LaunchPanelBlockEntity panel(final ServerLevel level, final BlockPos at) {
        if (level.getBlockEntity(at) instanceof LaunchPanelBlockEntity panel) {
            return panel;
        }
        throw new IllegalStateException("No launch panel at " + at);
    }
}
