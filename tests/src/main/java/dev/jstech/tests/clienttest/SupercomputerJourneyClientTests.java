/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.ClusterManagementComputerBlockEntity;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.HbwInterfaceBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.client.CommandPromptScreen;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.item.GpuItem;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.operation.payload.WireLine;
import dev.jstech.computers.os.media.InstallerStacks;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.tests.testkit.CraftFiles;
import dev.jstech.tests.testkit.EraKit;
import dev.jstech.tests.testkit.EraNetwork;
import dev.jstech.tests.testkit.EraParts;
import dev.jstech.tests.testkit.ServerStacks;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.lwjgl.glfw.GLFW;

/**
 * The supercomputer of the Standard and of the Advanced in a player's hands, on the era's network: the era's HBW
 * Interface hanging off the access router, its high-compute cable, and a supercomputer rack above the cable with two
 * nodes in it, each with its coprocessor and a drive. The Cluster Management Computer, with the era's cluster card,
 * runs MC-DOS beside the network's Crafting Computer; at its prompt the player lists the clusters and the nodes,
 * switches every bay off and on again, puts Debian's disc in the drive beside it and installs the system on every node,
 * and then asks for two long crafts, which the cluster runs side by side.
 */
public final class SupercomputerJourneyClientTests {

    private static final int SETTLE = EraJourney.SETTLE;
    private static final int WAIT = 200;
    private static final int INSTALL_WAIT = 3_000;
    private static final int TIMEOUT = 12_000;
    private static final ResourceLocation MC_DOS = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "mc_dos");
    private static final ResourceLocation DEBIAN = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "debian");
    /* The fabric: a backbone cable north of the access router, the interface at its end, the fabric cable east. */
    private static final BlockPos UPLINK = new BlockPos(7, 2, 1);
    private static final BlockPos HUB = new BlockPos(7, 2, 0);
    private static final BlockPos FABRIC = new BlockPos(8, 2, 0);
    private static final BlockPos NODE_RACK = new BlockPos(8, 3, 0);
    /* The manager west of the Crafting Computer's cable, its monitor north of it and its drive south. */
    private static final BlockPos MANAGER = new BlockPos(6, 2, 4);
    private static final BlockPos MANAGER_MONITOR = new BlockPos(6, 2, 3);
    private static final BlockPos AT_THE_MANAGER = new BlockPos(6, 2, 1);
    private static final BlockPos MANAGER_DRIVE = new BlockPos(6, 2, 5);
    private static final BlockPos AT_THE_MANAGER_DRIVE = new BlockPos(6, 2, 7);
    /* Logs for two crafts of planks each long enough to be running while the other starts. */
    private static final int LOGS = 8_000;
    private static final int PLANKS = 16_000;

    private SupercomputerJourneyClientTests() {
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void standard_theSupercomputerIsWorkedFromTheManagersPrompt(final ClientTestContext ctx) {
        supercomputer(ctx, EraKit.STANDARD);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void advanced_theSupercomputerIsWorkedFromTheManagersPrompt(final ClientTestContext ctx) {
        supercomputer(ctx, EraKit.ADVANCED);
    }

    private static void supercomputer(final ClientTestContext ctx, final EraKit kit) {
        final boolean advanced = kit == EraKit.ADVANCED;
        EraJourney.lay(ctx, kit).thenBuild(0, world -> {
            world.setBlock(UPLINK, ComputingModule.HBW_CABLE);
            world.setBlock(HUB, advanced ? ComputingModule.ADVANCED_HBW_INTERFACE.get()
                    : ComputingModule.HBW_INTERFACE.get());
            world.setBlock(FABRIC, advanced ? ComputingModule.OSFP_CABLE : ComputingModule.HPC_CABLE);
            world.setBlock(NODE_RACK, advanced ? ComputingModule.ADVANCED_SUPERCOMPUTER_RACK.get()
                    : ComputingModule.SUPERCOMPUTER_RACK.get());
            final ServerRackBlockEntity rack = world.blockEntity(NODE_RACK, ServerRackBlockEntity.class);
            for (final int row : new int[] {0, 2}) {
                rack.getServers().setStackInSlot(row, advanced ? ServerStacks.advancedSupercomputerNode()
                        : ServerStacks.defaultSupercomputerNode());
                if (!rack.insertDrive(row, new ItemStack(ComputingModule.disk(StorageTier.NVME, DiskSize.TB_1)))) {
                    throw new IllegalStateException("the node in row " + row + " took no drive");
                }
            }
            final CraftingComputerBlockEntity crafter = EraNetwork.craftingComputer(world, kit);
            crafter.loadPattern(CraftFiles.oakPlanks());
            manager(world, kit);
            world.placeMonitor(MANAGER_MONITOR, Direction.NORTH, kit.monitor());
            world.setBlock(MANAGER_DRIVE, opticalDrive(kit).defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        });
        ctx.thenServer(SETTLE, level -> NetworkStorage.of(level, mainframe(ctx, level).networkUuid())
                        .insert(StorageKey.of(Items.OAK_LOG), LOGS))
                .thenWaitUntilServer(level -> hub(ctx, level).clusterOnline(), WAIT,
                        "the supercomputer to come online on the network", level -> "offline");
        atThePrompt(ctx);
        line(ctx, "cluster list");
        ctx.thenWaitUntilServer(level -> glass(ctx, level).contains("SC-1") && glass(ctx, level).contains("online"),
                WAIT, "cluster list to name the supercomputer, online", level -> glass(ctx, level));
        line(ctx, "cluster nodes SC-1");
        ctx.thenWaitUntilServer(level -> glass(ctx, level).contains("U1") && glass(ctx, level).contains("U3"), WAIT,
                "cluster nodes to list both nodes by their units", level -> glass(ctx, level));
        line(ctx, "cluster power SC-1 off");
        ctx.thenWaitUntilServer(level -> !rack(ctx, level).bayPowerOn(0) && !rack(ctx, level).bayPowerOn(2), WAIT,
                "cluster power off to switch both bays off", level -> glass(ctx, level));
        line(ctx, "cluster power SC-1 on");
        ctx.thenWaitUntilServer(level -> rack(ctx, level).bayPowerOn(0) && rack(ctx, level).bayPowerOn(2), WAIT,
                        "cluster power on to switch them on again", level -> glass(ctx, level))
                .thenScreenshot(SETTLE, shot(kit, "cluster-nodes"))
                .thenWaitUntil(EraJourney.away(ctx), WAIT, "the prompt to be left")
                .thenTeleport(SETTLE, AT_THE_MANAGER_DRIVE, Direction.NORTH);
        DiscSteps.swap(ctx, MANAGER_DRIVE, InstallerStacks.forSystem(DEBIAN));
        atThePrompt(ctx);
        line(ctx, "cluster install SC-1 system");
        ctx.thenWaitUntilServer(level -> DEBIAN.equals(rack(ctx, level).unitHost(0).installedOsId())
                                && DEBIAN.equals(rack(ctx, level).unitHost(2).installedOsId()), INSTALL_WAIT,
                        "the manager to put Debian on both nodes", level -> glass(ctx, level));
        line(ctx, "cluster status");
        /*
         * Two long crafts asked for together: the cluster runs them side by side, each starting while the other is
         * still running, where one computer alone would take the second only once the first was done.
         */
        line(ctx, "cls");
        ctx.then(SETTLE, () -> {
            for (int i = 0; i < 2; i++) {
                ctx.type("iql CRAFT " + PLANKS + " oak_planks");
                ctx.key(GLFW.GLFW_KEY_ENTER);
            }
        });
        ctx.thenWaitUntilServer(level -> finished(ctx, level).size() >= 2, WAIT,
                        "both crafts to finish", level -> "crafts: " + crafts(ctx, level) + "; the prompt said "
                                + glass(ctx, level))
                .thenServer(0, level -> {
                    final List<OperationRecord> done = finished(ctx, level);
                    final OperationRecord first = done.get(0);
                    final OperationRecord second = done.get(1);
                    ctx.assertTrue(first.waitedTicks() < second.ranTicks() && second.waitedTicks() < first.ranTicks(),
                            "the cluster ran the two crafts side by side, each starting while the other ran; "
                                    + crafts(ctx, level) + " slots=" + hub(ctx, level).parallelCrafts());
                })
                .thenScreenshot(SETTLE, shot(kit, "parallel-crafts"));
    }

    /* The crafts of planks the network has finished, as its log keeps them. */
    private static List<OperationRecord> finished(final ClientTestContext ctx, final ServerLevel level) {
        return mainframe(ctx, level).recentOperations().stream()
                .filter(one -> one.type() == OperationRecord.TYPE_CRAFT && one.requested() == PLANKS
                        && one.status() == OperationRecord.STATUS_COMPLETED)
                .toList();
    }

    /* Every craft the network has on its log or in hand, with how long each waited and ran. */
    private static String crafts(final ClientTestContext ctx, final ServerLevel level) {
        final StringBuilder out = new StringBuilder();
        final MainframeBlockEntity mainframe = mainframe(ctx, level);
        for (final List<OperationRecord> records : List.of(mainframe.recentOperations(),
                mainframe.activeOperationRecords())) {
            for (final OperationRecord one : records) {
                if (one.type() == OperationRecord.TYPE_CRAFT) {
                    out.append("[status ").append(one.status()).append(' ').append(one.moved()).append('/')
                            .append(one.requested()).append(" waited ").append(one.waitedTicks()).append(" ran ")
                            .append(one.ranTicks()).append(']');
                }
            }
        }
        return out.toString();
    }

    /* The era's Cluster Management Computer with its cluster card, running MC-DOS, its back on the access cable. */
    private static void manager(final TestWorldBuilder world, final EraKit kit) {
        world.setBlock(MANAGER, kit == EraKit.ADVANCED ? ComputingModule.ADVANCED_CLUSTER_MANAGEMENT_COMPUTER.get()
                : ComputingModule.CLUSTER_MANAGEMENT_COMPUTER.get());
        world.faceRearTowardCable(MANAGER);
        final ClusterManagementComputerBlockEntity manager = world.blockEntity(MANAGER,
                ClusterManagementComputerBlockEntity.class);
        final ItemStackHandler hardware = manager.getHardware();
        final Item board = kit.computerBoard();
        hardware.setStackInSlot(ClusterManagementComputerBlockEntity.MOTHERBOARD_SLOT, new ItemStack(board));
        hardware.setStackInSlot(ClusterManagementComputerBlockEntity.CPU_SLOT, new ItemStack(EraParts.cpuFor(board)));
        hardware.setStackInSlot(ClusterManagementComputerBlockEntity.RAM_SLOTS_START,
                new ItemStack(EraParts.ramFor(board)));
        hardware.setStackInSlot(ClusterManagementComputerBlockEntity.PSU_SLOT,
                new ItemStack(EraParts.psuFor(kit.era())));
        hardware.setStackInSlot(ClusterManagementComputerBlockEntity.PCIE_SLOTS_START, new ItemStack(
                kit == EraKit.ADVANCED ? ComputingModule.FABRIC_DPU.get() : ComputingModule.FABRIC_HOST_ADAPTER.get()));
        hardware.setStackInSlot(ClusterManagementComputerBlockEntity.PCIE_SLOTS_START + 1,
                new ItemStack(EraParts.cardFor(board, GpuItem.class)));
        hardware.setStackInSlot(ClusterManagementComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(EraParts.diskFor(kit.era())));
        if (!manager.installOs(MC_DOS)) {
            throw new IllegalStateException("could not install MC-DOS on the " + kit + " manager");
        }
        manager.togglePower();
    }

    private static void atThePrompt(final ClientTestContext ctx) {
        ctx.thenWaitUntil(EraJourney.away(ctx), WAIT, "the screen to be left")
                .thenTeleport(SETTLE, AT_THE_MANAGER, Direction.SOUTH)
                .thenRightClick(SETTLE, MANAGER_MONITOR)
                .thenAwaitScreen(CommandPromptScreen.class, EraJourney.BOOT_WAIT);
    }

    /* Types the line at the manager's prompt, with the glass cleared first so what it answers stands alone. */
    private static void line(final ClientTestContext ctx, final String line) {
        ctx.then(SETTLE, () -> {
            ctx.type("cls");
            ctx.key(GLFW.GLFW_KEY_ENTER);
        }).then(SETTLE, () -> {
            ctx.type(line);
            ctx.key(GLFW.GLFW_KEY_ENTER);
        });
    }

    private static String glass(final ClientTestContext ctx, final ServerLevel level) {
        final StringBuilder out = new StringBuilder();
        for (final WireLine line : managerAt(ctx, level).console().glass()) {
            line.spans().forEach(span -> out.append(span.text().english()));
            out.append('\n');
        }
        return out.toString();
    }

    private static Block opticalDrive(final EraKit kit) {
        return kit == EraKit.ADVANCED ? ComputingModule.BLU_RAY_DRIVE.get() : ComputingModule.DVD_DRIVE.get();
    }

    private static ClusterManagementComputerBlockEntity managerAt(final ClientTestContext ctx,
                                                                  final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(MANAGER,
                ClusterManagementComputerBlockEntity.class);
    }

    private static HbwInterfaceBlockEntity hub(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(HUB, HbwInterfaceBlockEntity.class);
    }

    private static ServerRackBlockEntity rack(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(NODE_RACK, ServerRackBlockEntity.class);
    }

    private static MainframeBlockEntity mainframe(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(EraNetwork.MAINFRAME, MainframeBlockEntity.class);
    }

    private static String shot(final EraKit kit, final String what) {
        return kit.name().toLowerCase(Locale.ROOT) + "-" + what;
    }
}
