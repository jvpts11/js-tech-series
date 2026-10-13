/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.client.FirmwareScreen;
import dev.jstech.computers.client.ServerAssemblyScreen;
import dev.jstech.computers.client.ServerRackScreen;
import dev.jstech.computers.gui.layout.AssemblyGeometry;
import dev.jstech.computers.gui.layout.ServerRackLayout;
import dev.jstech.computers.item.DiskItem;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.rack.RaidMode;
import dev.jstech.tests.testkit.EraKit;
import dev.jstech.tests.testkit.EraNetwork;
import dev.jstech.tests.testkit.EraParts;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * Each era's storage in a player's hands, on the era's network: the player takes the rack's 1U server out through the
 * rack's screen and mounts the era's Storage Server with its Vaultis Array, puts three of the era's disks in its front
 * bays, links a monitor to the rack and, in the server's firmware, joins the disks as RAID 5, RAID 1 and RAID 0, the
 * volume the network's storage. With the mirror up a disk is pulled from its bay and another put in, and the array
 * rebuilds onto it. Last, the era's Vaultis Cache goes into the 1U server through its own screen, and the server back
 * into the rack takes that much off its drives' reads.
 */
public final class EraStorageClientTests {

    private static final int SETTLE = EraJourney.SETTLE;
    private static final int SCREEN_WAIT = EraJourney.SCREEN_WAIT;
    /** The rack's top row the storage server is mounted at: rows 0 and 1 hold the 1U server and a fibre interface. */
    private static final int STORAGE_ROW = 2;
    private static final int SERVER_ROW = 0;
    private static final BlockPos RACK_MONITOR = new BlockPos(6, 2, 6);
    private static final BlockPos AT_THE_RACK = new BlockPos(5, 2, 9);
    private static final BlockPos AT_THE_RACK_MONITOR = new BlockPos(6, 2, 8);
    /** The hotbar: the storage server, four disks, the monitor, the cache card, and two places left empty. */
    private static final int HOT_STORAGE = 0;
    private static final int HOT_FIRST_DISK = 1;
    private static final int HOT_SPARE_DISK = 4;
    private static final int HOT_MONITOR = 5;
    private static final int HOT_CACHE = 6;
    private static final int HOT_SERVER = 7;
    private static final int HOT_EMPTY = 8;
    /** How long a rebuild of three of the smallest disks may take, with room to spare. */
    private static final int REBUILD_WAIT = 1_200;
    private static final int TIMEOUT = 9_000;

    private EraStorageClientTests() {
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void vintage_raidAndCacheThroughTheFirmwareAndTheBays(final ClientTestContext ctx) {
        storage(ctx, EraKit.VINTAGE);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void legacy_raidAndCacheThroughTheFirmwareAndTheBays(final ClientTestContext ctx) {
        storage(ctx, EraKit.LEGACY);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void transition_raidAndCacheThroughTheFirmwareAndTheBays(final ClientTestContext ctx) {
        storage(ctx, EraKit.TRANSITION);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void standard_raidAndCacheThroughTheFirmwareAndTheBays(final ClientTestContext ctx) {
        storage(ctx, EraKit.STANDARD);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void advanced_raidAndCacheThroughTheFirmwareAndTheBays(final ClientTestContext ctx) {
        storage(ctx, EraKit.ADVANCED);
    }

    private static void storage(final ClientTestContext ctx, final EraKit kit) {
        final Item disk = EraParts.smallestDiskFor(kit.era());
        final long perDisk = ((DiskItem) disk).spec().capacityItems();
        final String era = kit.name().toLowerCase();
        EraJourney.lay(ctx, kit)
                .thenGive(0, EraNetwork.storageServer(kit),
                        new ItemStack(disk), new ItemStack(disk), new ItemStack(disk), new ItemStack(disk),
                        new ItemStack(kit.monitor()), new ItemStack(kit.cache()))
                // The rack's monitor, placed from the hand beside the cabinet.
                .thenTeleport(SETTLE, AT_THE_RACK_MONITOR, Direction.NORTH)
                .then(SETTLE, () -> ctx.selectHotbar(HOT_MONITOR))
                .thenPlace(1, RACK_MONITOR)
                // At the rack with an empty hand: its screen.
                .thenTeleport(SETTLE, AT_THE_RACK, Direction.NORTH)
                .then(SETTLE, () -> ctx.selectHotbar(HOT_EMPTY))
                .thenRightClick(SETTLE, EraNetwork.RACK)
                .thenAwaitScreen(ServerRackScreen.class, SCREEN_WAIT)
                // The 1U server out to the hotbar, and the storage server into its own two rows.
                .then(2, () -> ctx.clickGui(serverSlotX(), slotY(SERVER_ROW)))
                .then(2, () -> ctx.clickGui(hotbarX(HOT_SERVER), hotbarY()))
                .then(2, () -> ctx.clickGui(hotbarX(HOT_STORAGE), hotbarY()))
                .then(2, () -> ctx.clickGui(serverSlotX(), slotY(STORAGE_ROW)))
                .thenWaitUntilServer(level -> rack(ctx, level).getServers().getStackInSlot(SERVER_ROW).isEmpty()
                                && rack(ctx, level).getServers().getStackInSlot(STORAGE_ROW).is(kit.storageServer()),
                        SCREEN_WAIT, "the 1U server out and the storage server in, through the rack's screen",
                        level -> "rows=" + rack(ctx, level).getServers().getStackInSlot(SERVER_ROW) + " / "
                                + rack(ctx, level).getServers().getStackInSlot(STORAGE_ROW));
        // Three disks into the storage server's front bays.
        for (int column = 0; column < 3; column++) {
            final int bay = column;
            ctx.then(2, () -> ctx.clickGui(hotbarX(HOT_FIRST_DISK + bay), hotbarY()))
                    .then(2, () -> ctx.clickGui(frontX(bay), slotY(STORAGE_ROW)));
        }
        ctx.thenWaitUntilServer(level -> rack(ctx, level).claimedDriveStacks(STORAGE_ROW).size() == 3, SCREEN_WAIT,
                        "three disks in the storage server's bays",
                        level -> "claimed=" + rack(ctx, level).claimedDriveStacks(STORAGE_ROW).size())
                .thenScreenshot(SETTLE, era + "-storage-mounted")
                .then(0, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(SCREEN_WAIT)
                // The firmware on the rack's monitor, its storage page three tabs along.
                .thenTeleport(SETTLE, AT_THE_RACK_MONITOR, Direction.NORTH)
                .thenRightClick(SETTLE, RACK_MONITOR)
                // With no system on its disks the server stops at "No bootable device", where DEL opens the Setup.
                .thenWaitUntil(new PressDelete(ctx), EraJourney.BOOT_WAIT, "DEL to open the server's Setup")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_TAB))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_TAB))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_TAB))
                .thenScreenshot(SETTLE, era + "-firmware-storage");
        mode(ctx, GLFW.GLFW_KEY_DOWN, 3, RaidMode.RAID5);
        ctx.thenWaitUntilServer(level -> rack(ctx, level).getServerStorage(STORAGE_ROW).capacity() == perDisk * 2
                                && networkCapacity(ctx, level) == perDisk * 2,
                        SCREEN_WAIT, "RAID 5 over three disks to be the network's storage: two disks' room",
                        level -> "volume=" + rack(ctx, level).getServerStorage(STORAGE_ROW).capacity()
                                + " network=" + networkCapacity(ctx, level) + " disk=" + perDisk)
                .thenScreenshot(SETTLE, era + "-raid5");
        mode(ctx, GLFW.GLFW_KEY_UP, 1, RaidMode.RAID1);
        mode(ctx, GLFW.GLFW_KEY_UP, 1, RaidMode.RAID0);
        ctx.thenWaitUntilServer(level -> rack(ctx, level).getServerStorage(STORAGE_ROW).capacity() == perDisk * 3,
                SCREEN_WAIT, "RAID 0 to present the three disks' whole room",
                level -> "volume=" + rack(ctx, level).getServerStorage(STORAGE_ROW).capacity());
        // The mirror, for the pull: a member out of its bay, another in, and the rebuild onto it.
        mode(ctx, GLFW.GLFW_KEY_DOWN, 1, RaidMode.RAID1);
        ctx.then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(SCREEN_WAIT)
                .thenTeleport(SETTLE, AT_THE_RACK, Direction.NORTH)
                .thenRightClick(SETTLE, EraNetwork.RACK)
                .thenAwaitScreen(ServerRackScreen.class, SCREEN_WAIT)
                .then(2, () -> ctx.clickGui(frontX(0), slotY(STORAGE_ROW)))
                .then(2, () -> ctx.clickGui(hotbarX(HOT_FIRST_DISK), hotbarY()))
                .thenWaitUntilServer(level -> rack(ctx, level).raidDegraded(STORAGE_ROW), SCREEN_WAIT,
                        "the mirror to run degraded with a member pulled",
                        level -> "degraded=" + rack(ctx, level).raidDegraded(STORAGE_ROW))
                .thenScreenshot(SETTLE, era + "-degraded")
                .then(2, () -> ctx.clickGui(hotbarX(HOT_SPARE_DISK), hotbarY()))
                .then(2, () -> ctx.clickGui(frontX(0), slotY(STORAGE_ROW)))
                .thenWaitUntilServer(level -> rack(ctx, level).raidRebuilding(STORAGE_ROW)
                                || !rack(ctx, level).raidDegraded(STORAGE_ROW), SCREEN_WAIT,
                        "the replacement to start the rebuild", level -> "")
                .thenScreenshot(SETTLE, era + "-rebuilding")
                .thenWaitUntilServer(level -> !rack(ctx, level).raidRebuilding(STORAGE_ROW)
                                && !rack(ctx, level).raidDegraded(STORAGE_ROW), REBUILD_WAIT,
                        "the mirror to be whole again", level -> "permille="
                                + rack(ctx, level).raidRebuildPermille(STORAGE_ROW))
                .then(0, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(SCREEN_WAIT)
                // The cache into the 1U server through the server's own screen, the server back into its row.
                .then(SETTLE, () -> ctx.selectHotbar(HOT_SERVER))
                .then(2, () -> Objects.requireNonNull(ctx.mc().gameMode, "no game mode")
                        .useItem(ctx.player(), InteractionHand.MAIN_HAND))
                .thenAwaitScreen(ServerAssemblyScreen.class, SCREEN_WAIT)
                .then(SETTLE, () -> {
                    final AssemblyGeometry geometry = ctx.screen(ServerAssemblyScreen.class).geometry();
                    final AssemblyGeometry.Seat seat = geometry.seats(AssemblyGeometry.SeatKind.CARD).getFirst();
                    ctx.clickGui(hotbarX(HOT_CACHE), geometry.inventoryY() + 58 + 8);
                    ctx.clickGui(seat.x() + 8, seat.y() + 8);
                })
                .thenScreenshot(SETTLE, era + "-cache-seated")
                .then(0, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(SCREEN_WAIT)
                .then(SETTLE, () -> ctx.selectHotbar(HOT_EMPTY))
                .thenRightClick(SETTLE, EraNetwork.RACK)
                .thenAwaitScreen(ServerRackScreen.class, SCREEN_WAIT)
                .then(2, () -> ctx.clickGui(hotbarX(HOT_SERVER), hotbarY()))
                .then(2, () -> ctx.clickGui(serverSlotX(), slotY(SERVER_ROW)))
                .thenWaitUntilServer(level -> rack(ctx, level).cacheCutPercent(SERVER_ROW) == cut(kit),
                        SCREEN_WAIT, "the era's cache to take " + cut(kit) + "% off the 1U server's reads",
                        level -> "cut=" + rack(ctx, level).cacheCutPercent(SERVER_ROW))
                .thenScreenshot(SETTLE, era + "-cache-in-rack")
                .then(0, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(SCREEN_WAIT);
    }

    /* Moves the firmware's storage selection {@code steps} times with {@code key}, applies it and waits for it. */
    private static void mode(final ClientTestContext ctx, final int key, final int steps, final RaidMode mode) {
        for (int i = 0; i < steps; i++) {
            ctx.then(1, () -> ctx.key(key));
        }
        ctx.then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntilServer(level -> rack(ctx, level).raidModeOf(STORAGE_ROW) == mode, SCREEN_WAIT,
                        "the firmware to set " + mode, level -> "mode=" + rack(ctx, level).raidModeOf(STORAGE_ROW));
    }

    /** How much of its drives' reads the era's cache takes away: 10% on the Vintage's, five more in each era after. */
    private static int cut(final EraKit kit) {
        return 10 + 5 * kit.era().level();
    }

    private static long networkCapacity(final ClientTestContext ctx, final ServerLevel level) {
        final MainframeBlockEntity mainframe = TestWorldBuilder.at(level, ctx.origin())
                .blockEntity(EraNetwork.MAINFRAME, MainframeBlockEntity.class);
        return mainframe.networkUuid() == null ? -1 : NetworkStorage.of(level, mainframe.networkUuid()).capacity();
    }

    private static ServerRackBlockEntity rack(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(EraNetwork.RACK, ServerRackBlockEntity.class);
    }

    private static int serverSlotX() {
        return ServerRackLayout.serverItemX() + 8;
    }

    private static int frontX(final int column) {
        return ServerRackLayout.frontItemX(column) + 8;
    }

    private static int slotY(final int row) {
        return ServerRackLayout.itemY(row) + 8;
    }

    private static int hotbarX(final int slot) {
        return ServerRackLayout.INV_X + slot * 18 + 8;
    }

    private static int hotbarY() {
        return ServerRackLayout.HOTBAR_Y + 8;
    }

    /* Done once the Setup is up; until then presses DEL every so often, as a player does through the self-test. */
    private static final class PressDelete implements BooleanSupplier {

        private final ClientTestContext ctx;
        private int ticks;

        PressDelete(final ClientTestContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public boolean getAsBoolean() {
            if (ctx.openScreen(FirmwareScreen.class) != null) {
                return true;
            }
            if (ticks++ % 10 == 0 && ctx.mc().screen != null) {
                ctx.key(GLFW.GLFW_KEY_DELETE);
            }
            return false;
        }
    }
}
