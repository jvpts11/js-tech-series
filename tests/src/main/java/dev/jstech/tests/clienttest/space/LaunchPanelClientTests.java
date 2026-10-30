/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.space.api.Propellants;
import dev.jstech.space.client.launch.LaunchPanelScreen;
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
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

/**
 * The Launch Panel's console on the player's screen, as the approved LCC-1: opened on a pad with a fuelled rocket, its
 * key and ignition armed and its count running, seen at T-42 as the approved console is drawn, the tower's arms moving.
 */
public final class LaunchPanelClientTests {

    private static final BlockPos PANEL = new BlockPos(0, 2, 3);
    private static final BlockPos EYE = new BlockPos(0, 2, 1);
    private static final BlockPos ARM = new BlockPos(4, 3, 8);
    private static final BlockPos COMPUTER = new BlockPos(5, 7, 9);
    private static final int OPEN = 60;
    private static final int SETTLE = 20;
    private static final int SECOND = 20;
    private static final int TO_T42 = (LaunchPanelBlockEntity.START - LaunchPanelBlockEntity.ARMS) * SECOND;

    private LaunchPanelClientTests() {
    }

    @ClientTest(timeoutTicks = 1200)
    public static void panel_isDrawnAsTheApprovedOne(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.server().execute(() -> {
            for (final BlockPos at : List.of(PANEL, ARM, COMPUTER)) {
                ctx.serverLevel().removeBlock(ctx.abs(at), false);
            }
        }));
        ctx.thenTeleport(0, EYE, Direction.SOUTH)
                .thenServer(0, level -> build(ctx, level))
                .thenServer(2 * SECOND, level -> {
                    if (level.getBlockEntity(ctx.abs(PANEL)) instanceof LaunchPanelBlockEntity panel) {
                        ctx.assertTrue(panel.go(), "the panel sees its fuelled rocket go: vehicle " + panel.vehicle()
                                + ", oxidiser " + panel.oxidiser() + ", fuel " + panel.fuel() + ", storm "
                                + panel.storm() + ", clamps " + panel.clamps());
                        panel.press(LaunchPanelBlockEntity.MASTER);
                        panel.press(LaunchPanelBlockEntity.GUARD);
                        panel.press(LaunchPanelBlockEntity.IGNITION);
                        panel.press(LaunchPanelBlockEntity.RUN);
                    }
                })
                .thenRightClick(SETTLE, PANEL)
                .thenAwaitScreen(LaunchPanelScreen.class, OPEN)
                .then(TO_T42 - SETTLE, () -> { })
                .thenScreenshot(0, "launch-panel");
    }

    /* The panel, and by it a launcher's measuring arm and up its axis a fuelled rocket's flight computer. */
    private static void build(final ClientTestContext ctx, final ServerLevel level) {
        level.setWeatherParameters(6000, 0, false, false);
        level.setThunderLevel(0.0F);
        level.setRainLevel(0.0F);
        level.setBlockAndUpdate(ctx.abs(PANEL), LaunchPanel.BLOCK.get().defaultBlockState());
        level.setBlockAndUpdate(ctx.abs(ARM), LauncherBlocks.HOLD_DOWN_ARM.get().defaultBlockState());
        level.setBlockAndUpdate(ctx.abs(COMPUTER), RocketKitBlocks.FLIGHT_COMPUTER.get().defaultBlockState());
        if (level.getBlockEntity(ctx.abs(ARM)) instanceof LauncherDeckBlockEntity arm) {
            arm.deck(new LauncherDeckBlockEntity.Deck(Direction.EAST, 1, new BlockPos(1, 0, 1), 6, 8, 6));
        }
        final RocketStack stack = RocketStack.of(new RocketStack.Part(SkeletonRocketParts.id("sea_level_engine_3"), 1),
                new RocketStack.Part(SkeletonRocketParts.id("tank_3"), 1),
                new RocketStack.Part(SkeletonRocketParts.id("capsule_3"), 1));
        if (level.getBlockEntity(ctx.abs(COMPUTER)) instanceof FlightComputerBlockEntity rocket) {
            rocket.carry(new RocketDesign("Orbital I", stack), new BlockPos(0, -4, 0), List.of());
            Propellants.get(level, SkeletonPropellants.METHALOX).ifPresent(methalox -> rocket.load(1,
                    TankLoad.filled(SkeletonPropellants.METHALOX, methalox, 5_800.0)));
        }
    }
}
