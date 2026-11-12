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
import dev.jstech.computers.os.ProgramKind;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.program.Programs;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.testkit.client.ClientTestFailure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Each desktop's launcher on a computer with every program its system takes installed: however many there are, the
 * launcher stands whole on the screen, its top never above the desktop's.
 */
public final class StartMenuFitClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 120;
    private static final int BOOT_WAIT = 1_200;
    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);

    private StartMenuFitClientTests() {
    }

    @ClientTest(timeoutTicks = 3000)
    public static void frames95_launcherStaysOnTheScreenWithEveryProgram(final ClientTestContext ctx) {
        fits(ctx, "frames_95", null);
    }

    @ClientTest(timeoutTicks = 3000)
    public static void frames7_launcherStaysOnTheScreenWithEveryProgram(final ClientTestContext ctx) {
        fits(ctx, "frames_7", null);
    }

    @ClientTest(timeoutTicks = 3000)
    public static void frames10_launcherStaysOnTheScreenWithEveryProgram(final ClientTestContext ctx) {
        fits(ctx, "frames_10", null);
    }

    @ClientTest(timeoutTicks = 3000)
    public static void frames11_launcherStaysOnTheScreenWithEveryProgram(final ClientTestContext ctx) {
        fits(ctx, "frames_11", null);
    }

    @ClientTest(timeoutTicks = 3000)
    public static void kde_launcherStaysOnTheScreenWithEveryProgram(final ClientTestContext ctx) {
        fits(ctx, "ubuntu", "jsc:kde_plasma");
    }

    @ClientTest(timeoutTicks = 3000)
    public static void cinnamon_launcherStaysOnTheScreenWithEveryProgram(final ClientTestContext ctx) {
        fits(ctx, "ubuntu", "jsc:cinnamon");
    }

    /*
     * A running computer of {@code system}, with {@code desktop} over it when one is named and every program the
     * system takes installed, its launcher opened and held against the top of the desktop.
     */
    private static void fits(final ClientTestContext ctx, final String system, @Nullable final String desktop) {
        final ResourceLocation id = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, system);
        ctx.thenBuild(0, world -> {
                    final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(COMPUTER, id);
                    if (desktop != null) {
                        pc.console().install(desktop);
                    }
                    final OsDef os = OsRegistry.getOs(id);
                    final int rank = OsRegistry.osVersionRank(id);
                    // Every program the system lets a player install: its platform's, and none too new for it.
                    for (final ProgramSpec program : Programs.all()) {
                        if (JsComputers.MODID.equals(program.id().getNamespace()) && program.installable()
                                && program.kind() != ProgramKind.DESKTOP_ENVIRONMENT
                                && program.kind() != ProgramKind.OPERATING_SPACE
                                && program.platforms().contains(os.platform())
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
                    final DesktopScreen screen = ctx.screen(DesktopScreen.class);
                    ctx.click(screen.startButtonX(), screen.startButtonY());
                })
                .thenWaitUntil(() -> ctx.screen(DesktopScreen.class).isStartOpen(), SCREEN_WAIT,
                        "the launcher to open")
                .thenScreenshot(SETTLE * 3, system + (desktop == null ? "" : "-" + desktop.substring(4)) + "-start")
                .then(0, () -> {
                    final DesktopScreen screen = ctx.screen(DesktopScreen.class);
                    if (screen.startMenuTop() < screen.desktopY()) {
                        throw new ClientTestFailure("the launcher runs above the desktop with "
                                + screen.launcherLabels().size() + " programs, its top at " + screen.startMenuTop()
                                + " over the desktop's " + screen.desktopY());
                    }
                });
    }
}
