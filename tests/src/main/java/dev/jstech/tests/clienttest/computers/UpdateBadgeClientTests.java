/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.SettingsApp;
import dev.jstech.computers.os.ProgramVersions;
import dev.jstech.computers.program.Programs;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * The "update available" mark in Settings (computers/docs/design/programs.md): a computer on the network whose
 * Mainframe runs the Mirror, with five programs installed and two of them a version behind the Mirror's, opens its
 * settings on the Programs page, which says how many updates the Mirror has, gives each of the two an Update button and
 * the line of what is available, on Frames 10 and on Cinnamon.
 */
public final class UpdateBadgeClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 200;
    private static final int BOOT_WAIT = 1_200;
    private static final BlockPos MAINFRAME = new BlockPos(1, 2, 2);
    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);
    private static final String SETTINGS = Programs.SETTINGS.toString();
    private static final String PAINT = jsc("paint");
    private static final String SOUNDFOUNDRY = Programs.SOUNDFOUNDRY.toString();
    /* The five programs the page lists, two of them out of date. */
    private static final List<String> INSTALLED = List.of(jsc("exceed"), PAINT, SOUNDFOUNDRY,
            Programs.STORAGE_INSIGHTS.toString(), jsc("knot"));
    /* The versions the two are put back to, one step under what the Mirror serves. */
    private static final String PAINT_BEHIND = "5.0";
    private static final String SOUNDFOUNDRY_BEHIND = "2.8";
    private static final int UPDATES = 2;

    private UpdateBadgeClientTests() {
    }

    @ClientTest(timeoutTicks = 3000)
    public static void frames10_marksTheUpdatesOnItsProgramsPage(final ClientTestContext ctx) {
        programsPage(ctx, "frames_10", null).thenScreenshot(SETTLE, "badge-frames-10");
    }

    @ClientTest(timeoutTicks = 3000)
    public static void cinnamon_marksTheUpdatesOnItsProgramsPage(final ClientTestContext ctx) {
        programsPage(ctx, "linux_mint", jsc("cinnamon")).thenScreenshot(SETTLE, "badge-cinnamon");
    }

    /* A computer of that system on the network, the Mirror on its Mainframe, and its settings on the Programs page. */
    private static ClientTestContext programsPage(final ClientTestContext ctx, final String system,
                                                  @Nullable final String desktop) {
        return ctx.thenBuild(0, world -> {
                    final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
                    final CraftingComputerBlockEntity computer = net.cc();
                    computer.installOs(ResourceLocation.parse(jsc(system)));
                    if (desktop != null) {
                        computer.console().install(desktop);
                        computer.console().setInstalledVersion(desktop, ProgramVersions.of(desktop));
                    }
                    // Installed as a package manager installs them: each at the version the Mirror serves.
                    for (final String program : INSTALLED) {
                        computer.console().install(program);
                        computer.console().setInstalledVersion(program, ProgramVersions.of(program));
                    }
                    computer.console().setInstalledVersion(PAINT, PAINT_BEHIND);
                    computer.console().setInstalledVersion(SOUNDFOUNDRY, SOUNDFOUNDRY_BEHIND);
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenServer(SETTLE, level -> mainframe(ctx, level).installMirror())
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .then(SETTLE * 4, () -> DesktopScreen.requestOpen(SETTINGS))
                .thenWaitUntil(() -> settings(ctx) != null && wayToPrograms(settings(ctx))[0] > 0, SCREEN_WAIT,
                        "Settings to show its way to the Programs page")
                .then(SETTLE, () -> ctx.clickDesktop(wayToPrograms(settings(ctx))))
                .thenWaitUntil(() -> settings(ctx).page() == SettingsApp.PAGE_PROGRAMS
                        && settings(ctx).updatesShown() == UPDATES, SCREEN_WAIT,
                        "the Programs page with the Mirror's two updates")
                .then(SETTLE * 4, () -> ctx.mc().getToasts().clear());
    }

    /* The home's Programs entry on a system that opens on a home, the side list's otherwise. */
    private static int[] wayToPrograms(final SettingsApp app) {
        return app.onHome() ? app.homeEntryCenter(SettingsApp.PAGE_PROGRAMS)
                : app.navCenter(SettingsApp.PAGE_PROGRAMS);
    }

    private static SettingsApp settings(final ClientTestContext ctx) {
        return DesktopSteps.app(ctx, SETTINGS, SettingsApp.class);
    }

    private static MainframeBlockEntity mainframe(final ClientTestContext ctx, final ServerLevel level) {
        return (MainframeBlockEntity) level.getBlockEntity(ctx.abs(MAINFRAME));
    }

    private static String jsc(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, path).toString();
    }
}
