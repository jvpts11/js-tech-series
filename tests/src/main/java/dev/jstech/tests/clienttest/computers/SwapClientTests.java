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
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.client.os.ShellApp;
import dev.jstech.computers.client.os.TaskManagerApp;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.machine.DiskTools;
import dev.jstech.computers.os.disk.DiskCommand;
import dev.jstech.computers.os.disk.PartitionScheme;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The swap behind a machine's memory as its programs show it (computers/docs/design/hardware.md), shot for the
 * side-by-side with its mocks: Frames 11's Task Manager on its Memory page, with the page file it gives itself, and a
 * Linux machine with a swap partition on its second disk in its System Monitor and at its prompt.
 */
public final class SwapClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 120;
    private static final int BOOT_WAIT = 1_200;
    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);
    private static final String TASK_MANAGER = "jsc:task_manager";
    private static final String TERMINAL = "jsc:command_prompt";
    /* The swap partition made on the second disk, sixteen gigabytes. */
    private static final long SWAP_MB = 16_384L;
    private static final int SECOND_DISK = 1;

    private SwapClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void taskManager_frames11ShowsItsPageFileOnTheMemoryPage(final ClientTestContext ctx) {
        desktop(ctx, "frames_11", null, false)
                .then(SETTLE, () -> DesktopScreen.requestOpen(TASK_MANAGER))
                .thenWaitUntil(() -> taskManager(ctx) != null && taskManager(ctx).pageCentre(2) != null, SCREEN_WAIT,
                        "the Task Manager to open")
                .then(SETTLE, () -> ctx.clickDesktop(taskManager(ctx).pageCentre(2)))
                .thenWaitUntil(() -> taskManager(ctx).page() == 2, SCREEN_WAIT, "the Memory page")
                .thenScreenshot(30, "frames-11-memory");
    }

    /* The System Monitor over a machine with a swap partition, for its picture: what it shows is read off that. */
    @ClientTest(timeoutTicks = 2400)
    public static void systemMonitor_kdeOpensOverASwapPartitionForItsPicture(final ClientTestContext ctx) {
        desktop(ctx, "ubuntu", "jsc:kde_plasma", true)
                .then(SETTLE, () -> DesktopScreen.requestOpen(TASK_MANAGER))
                .thenWaitUntil(() -> taskManager(ctx) != null && taskManager(ctx).pageCentre(0) != null, SCREEN_WAIT,
                        "the System Monitor to open")
                .thenScreenshot(30, "kde-system-monitor");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void prompt_linuxShowsTheSwapPartition(final ClientTestContext ctx) {
        desktop(ctx, "ubuntu", "jsc:kde_plasma", true)
                .then(SETTLE, () -> DesktopScreen.requestOpen(TERMINAL))
                .thenWaitUntil(() -> shell(ctx) != null, SCREEN_WAIT, "the terminal to open")
                .then(SETTLE, () -> shell(ctx).typeLines(List.of("free -h", "swapon --show", "top -o SWAP")))
                .thenWaitUntil(() -> shell(ctx).scrollbackText().contains("/dev/sdb1"), SCREEN_WAIT,
                        "swapon to list the swap partition on the second disk")
                .thenScreenshot(SETTLE * 3, "linux-free-swapon-top");
    }

    /*
     * A running computer of {@code system}, with {@code desktop} over it when one is named, a monitor and a second
     * disk, that disk given a swap partition when {@code swap}, and its desktop up.
     */
    private static ClientTestContext desktop(final ClientTestContext ctx, final String system,
                                             @Nullable final String desktop, final boolean swap) {
        return ctx.thenBuild(0, world -> {
                    final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(COMPUTER,
                            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, system));
                    if (desktop != null) {
                        pc.console().install(desktop);
                    }
                    pc.getHardware().setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START + SECOND_DISK,
                            new ItemStack(ComputingModule.disk(StorageTier.SSD, DiskSize.GB_500)));
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenServer(SETTLE, level -> {
                    if (swap) {
                        final PersonalComputerBlockEntity pc = TestWorldBuilder.at(level, ctx.origin())
                                .blockEntity(COMPUTER, PersonalComputerBlockEntity.class);
                        DiskTools.reply(pc, level, DiskCommand.table(SECOND_DISK, PartitionScheme.GPT));
                        DiskTools.reply(pc, level, DiskCommand.create(SECOND_DISK, 0L, SWAP_MB, "swap"));
                    }
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> !ctx.screen(DesktopScreen.class).launcherLabels().isEmpty(), SCREEN_WAIT,
                        "the desktop to list its programs");
    }

    @Nullable
    private static TaskManagerApp taskManager(final ClientTestContext ctx) {
        final DesktopWindow window = window(ctx, TASK_MANAGER);
        return window != null && window.app() instanceof TaskManagerApp manager ? manager : null;
    }

    @Nullable
    private static ShellApp shell(final ClientTestContext ctx) {
        final DesktopWindow window = window(ctx, TERMINAL);
        return window != null && window.app() instanceof ShellApp app ? app : null;
    }

    @Nullable
    private static DesktopWindow window(final ClientTestContext ctx, final String program) {
        final DesktopScreen desktop = ctx.openScreen(DesktopScreen.class);
        return desktop == null ? null : desktop.windowFor(program);
    }
}
