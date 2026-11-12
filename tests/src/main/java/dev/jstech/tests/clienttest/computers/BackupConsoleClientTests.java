/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.backup.BackupJob;
import dev.jstech.computers.backup.BackupPart;
import dev.jstech.computers.backup.BackupService;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.blockentity.ServerServices;
import dev.jstech.computers.client.ServerRackScreen;
import dev.jstech.computers.client.os.BackupConsoleApp;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.gui.layout.ServerRackLayout;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.rack.RackLayout;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The Backup Console as a player sees it (computers/docs/design/programs.md), on Frames XP at a computer of a network
 * whose rack server runs the Backup Service, a Tape Library with two tapes in the rack below it and four jobs kept:
 * each of its tabs, as their mocks show them; and the rack's own window with the library on its card.
 */
public final class BackupConsoleClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 160;
    private static final int BOOT_WAIT = 1_200;
    /* Long enough for a tab to ask the service what it shows and draw it. */
    private static final int TAB_WAIT = 20;
    private static final BlockPos MAINFRAME = new BlockPos(1, 2, 2);
    private static final BlockPos RACK = new BlockPos(2, 2, 1);
    private static final BlockPos ROUTER = new BlockPos(3, 2, 2);
    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);
    private static final int SERVER = 0;
    /* The library's rows: a 2U unit topped at row 4 takes rows 4 and 5. */
    private static final int LIBRARY_ROW = 4;
    private static final int FIRST_TAPE = LIBRARY_ROW * RackLayout.SLOTS_PER_U;
    private static final String CONSOLE = "jsc:backup_console";
    private static final String[] TABS = {"jobs", "back-up", "restore", "tapes"};

    private BackupConsoleClientTests() {
    }

    @ClientTest(timeoutTicks = 4800)
    public static void backupConsole_framesXp_showsEachTab(final ClientTestContext ctx) {
        ctx.thenServer(0, level -> ctx.serverPlayer().getInventory().clearContent())
                .thenBuild(0, world -> {
                    world.placeRunningMainframe(MAINFRAME);
                    world.setBlock(MAINFRAME.east(), ComputingModule.HBW_CABLE);
                    final ServerRackBlockEntity rack = world.placeSeededRack(RACK);
                    rack.getServers().insertItem(LIBRARY_ROW, new ItemStack(ComputingModule.TAPE_LIBRARY.get()),
                            false);
                    rack.getFrontSlots().insertItem(FIRST_TAPE, new ItemStack(ComputingModule.LTO6_TAPE.get()),
                            false);
                    rack.getFrontSlots().insertItem(FIRST_TAPE + 1,
                            new ItemStack(ComputingModule.LTO6_TAPE.get()), false);
                    world.setBlock(ROUTER, ComputingModule.PERSONAL_ROUTER.get());
                    world.setBlock(ROUTER.east(), ComputingModule.ETHERNET_CABLE);
                })
                .thenServer(SETTLE, level -> {
                    final ServerRackBlockEntity rack = rack(ctx, level);
                    rack.unitHost(SERVER).installOs(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
                            "debian"));
                    rack.consoleOf(SERVER).install(Programs.BACKUP_SERVICE.toString());
                })
                .thenWaitUntilServer(level -> BackupService.serviceOf(level, rack(ctx, level).networkUuid()) != null,
                        BOOT_WAIT, "the rack's server to run the Backup Service", level -> "running="
                                + rack(ctx, level).unitRunning(SERVER))
                .thenServer(SETTLE, level -> {
                    final ServerServices.Host host = BackupService.serviceOf(level, rack(ctx, level).networkUuid());
                    final BackupJob.Target library = BackupJob.Target.library(rack(ctx, level).getBlockPos()
                            .asLong(), LIBRARY_ROW);
                    host.rack().backupsAt(host.slot()).put(job("Nightly", EnumSet.of(BackupPart.ITEMS,
                            BackupPart.FILES, BackupPart.SCRIPTS), library,
                            new BackupJob.Schedule(BackupJob.Schedule.Kind.DAILY, 2, 0)));
                    host.rack().backupsAt(host.slot()).put(job("Images", EnumSet.of(BackupPart.IMAGES), library,
                            new BackupJob.Schedule(BackupJob.Schedule.Kind.WEEKLY, 3, 6)));
                    host.rack().backupsAt(host.slot()).put(job("Settings", EnumSet.of(BackupPart.SETTINGS), library,
                            BackupJob.Schedule.BY_HAND));
                    host.rack().backupsAt(host.slot()).put(job("Patterns", EnumSet.of(BackupPart.PATTERNS),
                            library, new BackupJob.Schedule(BackupJob.Schedule.Kind.DAILY, 12, 0)));
                })
                .thenBuild(0, world -> {
                    final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(COMPUTER,
                            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "frames_xp"));
                    pc.console().install(CONSOLE);
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> !ctx.screen(DesktopScreen.class).launcherLabels().isEmpty(), SCREEN_WAIT,
                        "the desktop to list its programs")
                .then(SETTLE, () -> DesktopScreen.requestOpen(CONSOLE))
                .thenWaitUntil(() -> console(ctx) != null && console(ctx).hasState()
                        && console(ctx).jobNames().size() == 4, BOOT_WAIT, "the console to list the four jobs");
        for (int tab = 0; tab < TABS.length; tab++) {
            final int shown = tab;
            ctx.then(SETTLE, () -> {
                ctx.clickDesktop(console(ctx).tabCenter(shown));
                ctx.mc().getToasts().clear();
            }).thenScreenshot(TAB_WAIT, "backup-console-" + TABS[tab]);
        }
        // The rack's own window, the Tape Library on its card; an empty hand opens it.
        ctx.then(0, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(SCREEN_WAIT)
                .thenTeleport(SETTLE, RACK.north(2), Direction.SOUTH)
                .thenRightClick(SETTLE, RACK)
                .thenAwaitScreen(ServerRackScreen.class, SCREEN_WAIT)
                // A click on the library's faceplate, clear of its bays, puts it on the card.
                .then(SETTLE, () -> ctx.clickGui(ServerRackLayout.PWR_X - 8, ServerRackLayout.rowY(LIBRARY_ROW) + 10))
                .thenScreenshot(TAB_WAIT, "tape-library-in-the-rack");
    }

    private static BackupJob job(final String name, final Set<BackupPart> parts, final BackupJob.Target target,
                                 final BackupJob.Schedule when) {
        return new BackupJob(name, parts, target, when, -1L, BackupJob.RunState.NEVER);
    }

    private static ServerRackBlockEntity rack(final ClientTestContext ctx, final ServerLevel level) {
        return (ServerRackBlockEntity) level.getBlockEntity(ctx.abs(RACK));
    }

    @Nullable
    private static BackupConsoleApp console(final ClientTestContext ctx) {
        final DesktopScreen desktop = ctx.openScreen(DesktopScreen.class);
        final DesktopWindow window = desktop == null ? null : desktop.windowFor(CONSOLE);
        return window != null && window.app() instanceof BackupConsoleApp app ? app : null;
    }
}
