/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.ClusterManagementComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.client.os.ClusterManagerApp;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.storage.ServerStore;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * A datacenter section's diagnostics in the Cluster Manager (computers/docs/design/servers-and-racks.md): a Mainframe,
 * a Server Router with a rack of one server on its east branch, and the Cluster Management Computer on Frames 10; items
 * written into the network go through the section, and the Diagnostics tab of the section shows its latency, its
 * throughput and the alert its full disks raise, as the mock shows it.
 */
public final class ClusterDiagnosticsClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 200;
    private static final int WAIT = 400;
    private static final BlockPos MAINFRAME = new BlockPos(1, 2, 3);
    private static final BlockPos CABLE_A = new BlockPos(2, 2, 3);
    private static final BlockPos CABLE_B = new BlockPos(3, 2, 3);
    private static final BlockPos MANAGER = new BlockPos(3, 2, 2);
    private static final BlockPos MONITOR = MANAGER.west();
    private static final BlockPos PLAYER = MONITOR.north(2);
    private static final long DEMAND = 640L;
    private static final BlockPos ROUTER = new BlockPos(4, 2, 3);
    private static final BlockPos SECTION_CABLE = new BlockPos(5, 2, 3);
    private static final BlockPos RACK = new BlockPos(6, 2, 3);
    private static final String LABEL = "jsc:cluster_manager";
    private static final int DATACENTERS = 1;
    private static final int DIAGNOSTICS = 2;

    private ClusterDiagnosticsClientTests() {
    }

    @ClientTest(timeoutTicks = 3000)
    public static void datacenter_showsItsSectionsDiagnostics(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
                    world.placeRunningMainframe(MAINFRAME);
                    world.setBlock(CABLE_A, ComputingModule.HBW_CABLE);
                    world.setBlock(CABLE_B, ComputingModule.HBW_CABLE);
                    world.setBlock(ROUTER, ComputingModule.SERVER_ROUTER.get().defaultBlockState()
                            .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
                    world.setBlock(SECTION_CABLE, ComputingModule.HBW_CABLE);
                    world.setBlock(RACK, ComputingModule.SERVER_RACK.get().defaultBlockState()
                            .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
                    TestWorldBuilder.mountDefaultServer(world.blockEntity(RACK, ServerRackBlockEntity.class), 0);
                    world.setBlock(MANAGER, ComputingModule.CLUSTER_MANAGEMENT_COMPUTER.get());
                    world.faceRearTowardCable(MANAGER);
                    final ClusterManagementComputerBlockEntity manager = world.blockEntity(MANAGER,
                            ClusterManagementComputerBlockEntity.class);
                    build(manager.getHardware());
                    manager.installOs(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "frames_10"));
                    manager.console().install(Programs.CLUSTER_MANAGER.toString());
                    manager.togglePower();
                    world.placeMonitor(MONITOR, Direction.SOUTH);
                })
                .thenWaitUntilServer(level -> mainframe(ctx, level).networkUuid() != null, WAIT,
                        "the Mainframe to run its network", level -> "no network yet")
                .thenServer(SETTLE * 10, level -> mainframe(ctx, level).submitNetworkInsert(Items.COBBLESTONE, DEMAND,
                        "test"))
                // The server's disks filled, which the section raises an error for.
                .thenServer(SETTLE * 10, level -> {
                    final ServerStore store = ((ServerRackBlockEntity) level.getBlockEntity(ctx.abs(RACK)))
                            .getServerStorage(0);
                    store.insert(Items.COBBLESTONE, store.free());
                })
                .thenTeleport(SETTLE, PLAYER, Direction.SOUTH)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, SCREEN_WAIT * 2)
                .then(SETTLE * 4, () -> DesktopScreen.requestOpen(LABEL))
                .thenWaitUntil(() -> app(ctx) != null, SCREEN_WAIT, "the Cluster Manager to open")
                .then(SETTLE, () -> ctx.clickDesktop(ctx.screen(DesktopScreen.class).windowFor(LABEL)
                        .buttonCentre(DesktopWindow.BUTTON_MAXIMIZE)))
                .then(SETTLE * 2, () -> ctx.clickDesktop(app(ctx).tabPoint(DATACENTERS)))
                .thenWaitUntil(() -> app(ctx).listed() == 1, SCREEN_WAIT, "the Datacenters tab to list the section")
                .then(SETTLE, () -> ctx.clickDesktop(app(ctx).clusterPoint(0)))
                .then(SETTLE * 2, () -> ctx.clickDesktop(app(ctx).subTabPoint(DIAGNOSTICS)))
                .then(SETTLE * 10, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(SETTLE, "cluster-diagnostics");
    }

    /* The manager built as a Standard desktop computer, with a disk to install its system to. */
    private static void build(final ItemStackHandler hardware) {
        hardware.setStackInSlot(ClusterManagementComputerBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(HardwareItems.MOTHERBOARD_ATX_STANDARD_LGA1150.get()));
        hardware.setStackInSlot(ClusterManagementComputerBlockEntity.CPU_SLOT,
                new ItemStack(HardwareItems.CPU_INTEGRA_CENTRO_C7_4790K.get()));
        hardware.setStackInSlot(ClusterManagementComputerBlockEntity.RAM_SLOTS_START,
                new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
        hardware.setStackInSlot(ClusterManagementComputerBlockEntity.PSU_SLOT,
                new ItemStack(ComputingModule.PSU_650G.get()));
        hardware.setStackInSlot(ClusterManagementComputerBlockEntity.PCIE_SLOTS_START,
                new ItemStack(ComputingModule.FABRIC_HOST_ADAPTER.get()));
        hardware.setStackInSlot(ClusterManagementComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
    }

    private static MainframeBlockEntity mainframe(final ClientTestContext ctx, final ServerLevel level) {
        return (MainframeBlockEntity) level.getBlockEntity(ctx.abs(MAINFRAME));
    }

    private static ClusterManagerApp app(final ClientTestContext ctx) {
        return DesktopSteps.app(ctx, LABEL, ClusterManagerApp.class);
    }
}
