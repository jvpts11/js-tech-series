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
import dev.jstech.computers.client.DockStationScreen;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.client.os.ThisPcApp;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.os.media.DockStationBlockEntity;
import dev.jstech.computers.os.media.FormattedMediaItem;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The Dock Station as a player sees it: its three trays each showing the player's own disk and the stick in its port,
 * its lamps lit with a computer behind it; its window listing each tray and the port with the letter the computer
 * gives what is docked, saying why a disk too new for it stays out, and each size of stick saying what it holds; and
 * This PC on that computer listing the docked disks as drives.
 */
public final class DockStationClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 120;
    private static final int BOOT_WAIT = 1_200;
    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos DOCK = new BlockPos(5, 2, 3);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);
    private static final String THIS_PC = "jsc:this_pc";
    /** How far the player looks down at the dock's front, as the drives are shot. */
    private static final float CLOSE_UP_PITCH = 28F;
    /** The sticks of each size, smallest first, and what their shots are called. */
    private static final List<ItemEntry<FormattedMediaItem>> STICKS = List.of(ComputingModule.USB_FLASH_DRIVE_8G,
            ComputingModule.USB_FLASH_DRIVE_16G, ComputingModule.USB_FLASH_DRIVE, ComputingModule.USB_FLASH_DRIVE_64G);
    private static final List<String> STICK_SIZES = List.of("8g", "16g", "32g", "64g");

    private DockStationClientTests() {
    }

    @ClientTest(timeoutTicks = 1200)
    public static void dock_showsTheDisksInItsTrays(final ClientTestContext ctx) {
        final BlockPos at = DOCK.south(2);
        build(ctx, "frames_11")
                .thenServer(SETTLE, level -> {
                    final BlockPos abs = ctx.abs(at);
                    ctx.serverPlayer().teleportTo(level, abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5,
                            Direction.NORTH.toYRot(), CLOSE_UP_PITCH);
                })
                .thenWaitUntil(() -> ctx.player() != null
                        && ctx.player().position().distanceTo(Vec3.atBottomCenterOf(ctx.abs(at))) < 0.1, SCREEN_WAIT,
                        "the player to stand in front of the dock, looking down at its trays")
                .thenWaitUntil(() -> clientDock(ctx) != null && clientDock(ctx).ownerPos() != null
                                && !clientDock(ctx).disk(DockStationBlockEntity.BAY_NVME).isEmpty(), SCREEN_WAIT,
                        "the client to see the dock linked with its disks")
                .thenScreenshot(20, "dock-with-disks");
    }

    @ClientTest(timeoutTicks = 1200)
    public static void dockWindow_listsEachTrayWithItsLetter(final ClientTestContext ctx) {
        // An empty hand: a dock clicked with a disk or a stick in it would take it instead of opening.
        build(ctx.thenGive(0), "frames_11")
                .thenTeleport(SETTLE, DOCK.south(2), Direction.NORTH)
                .thenRightClick(SETTLE, DOCK)
                .thenAwaitScreen(DockStationScreen.class, SCREEN_WAIT)
                .thenWaitUntil(() -> clientDock(ctx).letter(DockStationBlockEntity.BAY_HDD) != ' '
                                && clientDock(ctx).letter(DockStationBlockEntity.USB) != ' ', SCREEN_WAIT,
                        "the window to show the letters the computer gives the disks")
                .thenAssert(0, () -> clientDock(ctx).letter(DockStationBlockEntity.USB) == 'D'
                                && clientDock(ctx).letter(DockStationBlockEntity.BAY_HDD) == 'E',
                        "the stick is D: and the hard disk after it E:")
                .thenScreenshot(SETTLE, "dock-window")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(SCREEN_WAIT);
    }

    /** A disk newer than the dock, held over its M.2 tray, says why the tray keeps it out. */
    @ClientTest(timeoutTicks = 1200)
    public static void dockWindow_saysWhyADiskTooNewStaysOut(final ClientTestContext ctx) {
        openWindow(ctx)
                .thenServer(SETTLE, level -> {
                    ctx.serverPlayer().containerMenu.setCarried(
                            new ItemStack(ComputingModule.disk(StorageTier.NVME, DiskSize.TB_4)));
                    ctx.serverPlayer().containerMenu.broadcastChanges();
                })
                .thenWaitUntil(() -> !ctx.player().containerMenu.getCarried().isEmpty(), SCREEN_WAIT,
                        "the disk to be held in the cursor")
                .then(0, () -> pointAtSlot(ctx, DockStationBlockEntity.BAY_NVME))
                .thenScreenshot(SETTLE, "dock-refusal")
                .thenServer(0, level -> {
                    ctx.serverPlayer().containerMenu.setCarried(ItemStack.EMPTY);
                    ctx.serverPlayer().containerMenu.broadcastChanges();
                })
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(SCREEN_WAIT);
    }

    /** Each size of stick says, under the cursor, how much it holds, its kind and era, and how much is free. */
    @ClientTest(timeoutTicks = 1200)
    public static void dockWindow_eachStickSaysWhatItHolds(final ClientTestContext ctx) {
        openWindow(ctx)
                .thenServer(SETTLE, level -> {
                    for (int i = 0; i < STICKS.size(); i++) {
                        ctx.serverPlayer().getInventory().setItem(i, new ItemStack(STICKS.get(i).get()));
                    }
                    ctx.serverPlayer().containerMenu.broadcastChanges();
                })
                .thenWaitUntil(() -> !ctx.player().getInventory().getItem(STICKS.size() - 1).isEmpty(), SCREEN_WAIT,
                        "the sticks to reach the hotbar");
        for (int i = 0; i < STICKS.size(); i++) {
            final int hotbar = i;
            ctx.then(SETTLE, () -> pointAtSlot(ctx, hotbarSlot(ctx, hotbar)))
                    .thenScreenshot(SETTLE, "stick-" + STICK_SIZES.get(i));
        }
        ctx.then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE)).thenAwaitNoScreen(SCREEN_WAIT);
    }

    @ClientTest(timeoutTicks = 3000)
    public static void thisPc_listsTheDockedDisksAsDrives(final ClientTestContext ctx) {
        build(ctx, "frames_11")
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> !ctx.screen(DesktopScreen.class).launcherLabels().isEmpty(), SCREEN_WAIT,
                        "the desktop to list its programs")
                .then(SETTLE, () -> DesktopScreen.requestOpen(THIS_PC))
                .thenWaitUntil(() -> thisPc(ctx) != null && thisPc(ctx).mediaRowIndex("Bolt") >= 0, SCREEN_WAIT,
                        "This PC to list the NVMe in the dock's tray")
                .thenAssert(0, () -> thisPc(ctx).mediaRowIndex("Keep") >= 0, "and the hard disk")
                .thenScreenshot(SETTLE, "this-pc-docked-disks");
    }

    /*
     * The dock's window open, with an empty hand, on a computer holding its disks; what the test puts in the player's
     * inventory or cursor goes again as it ends, so the next test's hand is as empty as this one's.
     */
    private static ClientTestContext openWindow(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.server().execute(() -> {
            ctx.serverPlayer().getInventory().clearContent();
            ctx.serverPlayer().containerMenu.setCarried(ItemStack.EMPTY);
        }));
        return build(ctx.thenGive(0), "frames_11")
                .thenTeleport(SETTLE, DOCK.south(2), Direction.NORTH)
                .thenRightClick(SETTLE, DOCK)
                .thenAwaitScreen(DockStationScreen.class, SCREEN_WAIT);
    }

    /* Rests the pointer on the window's slot {@code index}. */
    private static void pointAtSlot(final ClientTestContext ctx, final int index) {
        final DockStationScreen screen = ctx.screen(DockStationScreen.class);
        final Slot slot = screen.getMenu().slots.get(index);
        ctx.pointAt(screen.getGuiLeft() + slot.x + 8, screen.getGuiTop() + slot.y + 8);
    }

    /* The window's slot index of the player's hotbar slot {@code hotbar}. */
    private static int hotbarSlot(final ClientTestContext ctx, final int hotbar) {
        final List<Slot> slots = ctx.screen(DockStationScreen.class).getMenu().slots;
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i).container instanceof Inventory && slots.get(i).getContainerSlot() == hotbar) {
                return i;
            }
        }
        throw new IllegalStateException("the window shows no hotbar slot " + hotbar);
    }

    /* A running computer with a monitor, and a dock behind it holding a disk of each size and a stick. */
    private static ClientTestContext build(final ClientTestContext ctx, final String system) {
        return ctx.thenBuild(0, world -> {
                    world.placeRunningPersonalComputer(COMPUTER,
                            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, system));
                    world.placeMonitor(MONITOR, Direction.EAST);
                    world.setBlock(DOCK, ComputingModule.DOCK_STATION.get().defaultBlockState()
                            .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                })
                .thenServer(SETTLE, level -> {
                    final DockStationBlockEntity dock = serverDock(ctx, level);
                    dock.insertMedia(new ItemStack(ComputingModule.USB_FLASH_DRIVE.get()));
                    dock.insertDisk(new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
                    dock.insertDisk(new ItemStack(ComputingModule.disk(StorageTier.SSD, DiskSize.GB_500)));
                    dock.insertDisk(new ItemStack(ComputingModule.disk(StorageTier.NVME, DiskSize.GB_500)));
                });
    }

    private static DockStationBlockEntity serverDock(final ClientTestContext ctx, final ServerLevel level) {
        return (DockStationBlockEntity) level.getBlockEntity(ctx.abs(DOCK));
    }

    @Nullable
    private static DockStationBlockEntity clientDock(final ClientTestContext ctx) {
        return ctx.mc().level.getBlockEntity(ctx.abs(DOCK)) instanceof DockStationBlockEntity dock ? dock : null;
    }

    @Nullable
    private static ThisPcApp thisPc(final ClientTestContext ctx) {
        final DesktopScreen desktop = ctx.openScreen(DesktopScreen.class);
        final DesktopWindow window = desktop == null ? null : desktop.windowFor(THIS_PC);
        return window != null && window.app() instanceof ThisPcApp app ? app : null;
    }
}
