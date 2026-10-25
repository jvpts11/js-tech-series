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
import dev.jstech.computers.eventlog.EventLevel;
import dev.jstech.computers.eventlog.NetworkEvents;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.uuid.NodeUuid;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * The Network Manager's Log tab on a Frames XP Mainframe: the switch to the network's events, the rows newest first,
 * the filter by level, and Export writing them to a file on the machine's disk.
 */
public final class NetworkEventLogClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 160;
    private static final int BOOT_WAIT = 600;

    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);

    private static final ResourceLocation FRAMES_XP =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "frames_xp");

    private NetworkEventLogClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void events_showNewestFirstFilterByLevelAndExportToTheDisk(final ClientTestContext ctx) {
        mainframeDesktop(ctx)
                .thenWaitUntilServer(level -> mainframe(ctx, level).networkUuid() != null, SCREEN_WAIT,
                        "the Mainframe to own its network", level -> "running " + mainframe(ctx, level).isRunning())
                .thenServer(0, level -> writeEvents(ctx, level))
                .then(SETTLE, () -> DesktopScreen.requestOpen(NetworkManagerApp.TITLE))
                .thenWaitUntil(() -> manager(ctx) != null, SCREEN_WAIT, "the Network Manager to open")
                .then(SETTLE * 4, () -> ctx.clickDesktop(manager(ctx).tabPoint("log")))
                .then(SETTLE, () -> ctx.clickDesktop(manager(ctx).logViewPoint(true)))
                .thenWaitUntil(() -> manager(ctx).eventsAnswered() && manager(ctx).eventRows().size() == 3,
                        SCREEN_WAIT, "the three events to show", () -> "rows " + manager(ctx).eventRows())
                // Text crosses the wire as its key, so it is read in the language the client has loaded.
                .then(0, () -> ctx.assertTrue(GameText.resolve(manager(ctx).eventRows().get(0).message())
                        .startsWith("Network conflict"), "the newest first: " + manager(ctx).eventRows()))
                .then(2, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(SETTLE, "events")
                .then(0, () -> manager(ctx).showEventLevel(EventLevel.WARN))
                .thenWaitUntil(() -> manager(ctx).eventRows().size() == 1, SCREEN_WAIT, "only the warning",
                        () -> "rows " + manager(ctx).eventRows())
                .then(0, () -> ctx.assertTrue(GameText.resolve(manager(ctx).eventRows().get(0).message())
                        .equals("Solar storm expected"), "the warning: " + manager(ctx).eventRows()))
                .thenScreenshot(SETTLE, "events-warn")
                .then(0, () -> manager(ctx).showEventLevel(null))
                .thenWaitUntil(() -> manager(ctx).eventRows().size() == 3, SCREEN_WAIT, "every level again")
                .then(SETTLE, () -> ctx.clickDesktop(manager(ctx).exportPoint()))
                .thenWaitUntil(() -> manager(ctx).exportDialog().isOpen(), SCREEN_WAIT, "the Save window to open")
                .thenScreenshot(SETTLE, "export-dialog")
                .then(SETTLE, () -> ctx.clickDesktop(manager(ctx).exportDialog().primaryPoint()))
                .thenWaitUntil(() -> manager(ctx).exportedPath() != null, SCREEN_WAIT, "the export to be sent")
                .thenWaitUntilServer(level -> DiskFilesystem.read(mainframe(ctx, level).systemDisk(),
                                manager(ctx).exportedPath()).map(text -> text.contains("ERROR")
                                && text.contains("Solar storm expected")).orElse(false), SCREEN_WAIT,
                        "the file to be on the Mainframe's disk with every event",
                        level -> "file " + DiskFilesystem.read(mainframe(ctx, level).systemDisk(),
                                manager(ctx).exportedPath()))
                .thenScreenshot(SETTLE, "exported")
                // Back to the Operations, which stand under the switch as the events do.
                .then(SETTLE, () -> ctx.clickDesktop(manager(ctx).logViewPoint(false)))
                .thenScreenshot(SETTLE, "operations");
    }

    /* Three events of the Mainframe's network, the oldest first: a machine switched on, a storm coming, a conflict. */
    private static void writeEvents(final ClientTestContext ctx, final ServerLevel level) {
        final MainframeBlockEntity machine = mainframe(ctx, level);
        final NodeUuid office = new NodeUuid(UUID.randomUUID());
        NetworkEvents.record(level, machine.networkUuid(), EventLevel.INFO, office, "Office PC",
                NetworkEvents.TURNED_ON.text());
        NetworkEvents.record(level, machine.networkUuid(), EventLevel.WARN, null, "",
                Text.literal("Solar storm expected"));
        NetworkEvents.record(level, machine.networkUuid(), EventLevel.ERROR, machine.nodeUuid(), "Mainframe",
                NetworkEvents.CONFLICT.with("36163, -60, 0"));
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
        final DesktopScreen desktop = ctx.screen(DesktopScreen.class);
        if (desktop == null) {
            return null;
        }
        final DesktopWindow window = desktop.windowFor(NetworkManagerApp.TITLE);
        return window != null && window.app() instanceof NetworkManagerApp app ? app : null;
    }
}
