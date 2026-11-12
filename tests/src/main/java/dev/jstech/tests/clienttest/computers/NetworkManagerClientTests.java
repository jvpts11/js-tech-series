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
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.client.os.NetworkManagerApp;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * The Network Manager's pages on a Frames XP Mainframe (computers/docs/design/network.md): its Devices with how long
 * each has been up and its Stats with what the Operations took, shot for the side-by-side with their mocks. Its Log
 * and its Services have tests of their own.
 */
public final class NetworkManagerClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 160;
    private static final int BOOT_WAIT = 600;
    /* Long enough for a page to ask the network for what it shows and draw it. */
    private static final int PAGE_WAIT = 30;
    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);
    private static final ResourceLocation FRAMES_XP =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "frames_xp");

    private NetworkManagerClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void networkManager_showsItsDevicesStatsAndServices(final ClientTestContext ctx) {
        mainframeDesktop(ctx)
                .thenWaitUntilServer(level -> mainframe(ctx, level).networkUuid() != null, SCREEN_WAIT,
                        "the Mainframe to own its network", level -> "running " + mainframe(ctx, level).isRunning())
                .then(SETTLE, () -> DesktopScreen.requestOpen(NetworkManagerApp.TITLE))
                .thenWaitUntil(() -> manager(ctx) != null, SCREEN_WAIT, "the Network Manager to open")
                .then(SETTLE * 4, () -> ctx.clickDesktop(manager(ctx).tabPoint("devices")))
                .thenScreenshot(PAGE_WAIT, "devices")
                .then(0, () -> ctx.clickDesktop(manager(ctx).tabPoint("stats")))
                .thenScreenshot(PAGE_WAIT, "stats");
    }

    /** A Frames XP Mainframe brought up at its desktop on a monitor. */
    private static ClientTestContext mainframeDesktop(final ClientTestContext ctx) {
        return ctx.thenBuild(0, world -> {
                    world.setBlock(COMPUTER, ComputingModule.MAINFRAME.get());
                    final MainframeBlockEntity machine = world.blockEntity(COMPUTER, MainframeBlockEntity.class);
                    final ItemStackHandler inv = machine.getInventory();
                    inv.setStackInSlot(MainframeBlockEntity.MOTHERBOARD_SLOT,
                            new ItemStack(ComputingModule.MOTHERBOARD_MTX_S_2011.get()));
                    inv.setStackInSlot(MainframeBlockEntity.CPU_SLOTS_START,
                            new ItemStack(ComputingModule.CPU_SERVO_2620.get()));
                    inv.setStackInSlot(MainframeBlockEntity.RAM_SLOTS_START,
                            new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
                    inv.setStackInSlot(MainframeBlockEntity.PSU_SLOT, new ItemStack(ComputingModule.PSU_650G.get()));
                    // A graphics card gives the machine peripheral ports, which is what the monitor links to.
                    inv.setStackInSlot(MainframeBlockEntity.GPU_SLOTS_START,
                            new ItemStack(ComputingModule.GPU_HD_7970.get()));
                    inv.setStackInSlot(MainframeBlockEntity.DISK_SLOTS_START,
                            new ItemStack(ComputingModule.disk(StorageTier.SSD, DiskSize.GB_500)));
                    machine.installOs(FRAMES_XP);
                    machine.togglePower();
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT);
    }

    private static MainframeBlockEntity mainframe(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(COMPUTER, MainframeBlockEntity.class);
    }

    @Nullable
    private static NetworkManagerApp manager(final ClientTestContext ctx) {
        final DesktopScreen desktop = ctx.openScreen(DesktopScreen.class);
        final DesktopWindow window = desktop == null ? null : desktop.windowFor(NetworkManagerApp.TITLE);
        return window != null && window.app() instanceof NetworkManagerApp app ? app : null;
    }
}
