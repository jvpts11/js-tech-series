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
import dev.jstech.computers.operation.MemberRuns;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.core.jstechseries.network.NetworkSystem;
import dev.jstech.core.jstechseries.operation.OperationPriority;
import dev.jstech.tests.TestNetworkMembers;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Another mod's Operation in the Network Manager of a Frames XP Mainframe: the beam the test accelerator was given
 * listed on the Processes tab with its kind, its machine and what was asked, called off from its detail, which stops
 * the machine, and then in the Log tab under RUN.
 */
public final class MemberRunClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 160;
    private static final int BOOT_WAIT = 600;
    private static final int LONG_BEAM = 120;

    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos CABLE = new BlockPos(5, 2, 1);
    private static final BlockPos ACCELERATOR = new BlockPos(5, 2, 0);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);

    private static final ResourceLocation FRAMES_XP =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "frames_xp");

    private MemberRunClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void anotherModsOperation_showsInProcessesStopsFromThereAndGoesIntoTheLog(
            final ClientTestContext ctx) {
        mainframeDesktop(ctx)
                .thenWaitUntilServer(level -> able(ctx, level) == 1, SCREEN_WAIT,
                        "the accelerator to join the Mainframe's network", level -> "able " + able(ctx, level))
                .thenServer(0, level -> {
                    final MemberRuns.Outcome given = mainframe(ctx, level).networkOperations().run(
                            TestNetworkMembers.ACCELERATE, new TestNetworkMembers.AccelerateArgs(LONG_BEAM), "",
                            OperationPriority.HIGH);
                    ctx.assertTrue(given.ok(), "the beam is given out: " + given.said().english());
                })
                .thenWaitUntilServer(level -> accelerator(ctx, level).running(), SCREEN_WAIT,
                        "the accelerator to run the beam", level -> "running " + accelerator(ctx, level).running())
                .then(SETTLE, () -> DesktopScreen.requestOpen(NetworkManagerApp.TITLE))
                .thenWaitUntil(() -> manager(ctx) != null, SCREEN_WAIT, "the Network Manager to open")
                .then(SETTLE * 4, () -> ctx.clickDesktop(manager(ctx).tabPoint("processes")))
                .thenWaitUntil(() -> manager(ctx).processesShown().size() == 1, SCREEN_WAIT,
                        "the beam among the processes", () -> "shown " + manager(ctx).processesShown())
                .then(0, () -> {
                    final OperationRecord beam = manager(ctx).processesShown().getFirst();
                    ctx.assertTrue(beam.type() == OperationRecord.TYPE_RUN
                                    && beam.status() == OperationRecord.STATUS_PROCESSING
                                    && beam.priority() == OperationPriority.HIGH,
                            "running, high, as another mod's: " + beam);
                    // The kind's name is the test mod's own sentence, read in the language the client has loaded.
                    ctx.assertTrue(beam.name().getString().startsWith("Accelerate by Test accelerator-")
                                    && beam.name().getString().endsWith(": seconds " + LONG_BEAM),
                            "its kind, its machine and what was asked: " + beam.name().getString());
                })
                .then(2, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(SETTLE, "processes")
                .then(SETTLE, () -> ctx.clickDesktop(manager(ctx).processRowPoint(0)))
                .thenWaitUntil(() -> manager(ctx).detailOpen(), SCREEN_WAIT, "the beam's detail to open")
                .thenScreenshot(SETTLE, "detail")
                .then(SETTLE, () -> ctx.clickDesktop(manager(ctx).detailCancelPoint()))
                .thenWaitUntilServer(level -> !accelerator(ctx, level).running()
                                && accelerator(ctx, level).stopped() == 1, SCREEN_WAIT,
                        "the accelerator to stop its beam", level -> "stopped " + accelerator(ctx, level).stopped())
                .then(SETTLE, () -> ctx.clickDesktop(manager(ctx).tabPoint("log")))
                .thenWaitUntil(() -> logged(ctx).size() == 1, SCREEN_WAIT, "the beam in the log",
                        () -> "log " + manager(ctx).logShown())
                .then(0, () -> ctx.assertTrue(logged(ctx).getFirst().status() == OperationRecord.STATUS_DISCARDED,
                        "called off: " + logged(ctx).getFirst()))
                .thenScreenshot(SETTLE, "log");
    }

    /** A Frames XP Mainframe brought up at its desktop on a monitor, the test accelerator cabled to it. */
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
                    world.setBlock(CABLE, ComputingModule.HBW_CABLE);
                    world.setBlock(ACCELERATOR, TestNetworkMembers.ACCELERATOR.get());
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT);
    }

    /* How many machines on the Mainframe's network carry out the test mod's kind. */
    private static int able(final ClientTestContext ctx, final ServerLevel level) {
        final MainframeBlockEntity machine = mainframe(ctx, level);
        return machine.networkUuid() == null ? 0 : NetworkSystem.get(level)
                .membersCarryingOut(machine.networkUuid(), TestNetworkMembers.ACCELERATE.id()).size();
    }

    /* The other mods' Operations the Log tab lists, newest first. */
    private static List<OperationRecord> logged(final ClientTestContext ctx) {
        return manager(ctx).logShown().stream().filter(OperationRecord::ofAnotherMod).toList();
    }

    private static MainframeBlockEntity mainframe(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(COMPUTER, MainframeBlockEntity.class);
    }

    private static TestNetworkMembers.AcceleratorBlockEntity accelerator(final ClientTestContext ctx,
                                                                         final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(ACCELERATOR,
                TestNetworkMembers.AcceleratorBlockEntity.class);
    }

    @Nullable
    private static NetworkManagerApp manager(final ClientTestContext ctx) {
        final DesktopScreen desktop = ctx.screen(DesktopScreen.class);
        if (desktop == null) {
            return null;
        }
        final DesktopWindow window = desktop.windowFor(NetworkManagerApp.TITLE);
        return window != null && window.app() instanceof NetworkManagerApp app ? app : null;
    }
}
