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
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.client.InstallerScreen;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.operation.payload.firmware.FirmwarePayloads;
import dev.jstech.computers.os.media.MediaItem;
import dev.jstech.computers.os.media.MediaKind;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.lwjgl.glfw.GLFW;

/**
 * bsdinstall and the System V installer, on the glass: opening on their first page and stepping through the ones
 * that ask nothing more than Enter; the System V case stops at the copy, and the bsdinstall case runs on through
 * its services page to the last one.
 *
 * <p>Every page a player sees belongs to the machine, the way every other installer's does; this only exercises
 * the two new shapes, {@code DIALOG_BOX} and the System V console, and the pages that are new to both of them.
 */
public final class GuidedInstallerClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 200;
    /** Long enough for the extraction to run its course under the 90-second ceiling every copy is held to. */
    private static final int COPY_WAIT = 2_000;

    private static final BlockPos MACHINE = new BlockPos(5, 2, 2);
    private static final BlockPos DRIVE = new BlockPos(4, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);

    private static final ResourceLocation FREEBSD =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "freebsd");
    private static final ResourceLocation UNIX = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "unix");

    private GuidedInstallerClientTests() {
    }

    /**
     * bsdinstall opens on its own welcome dialog, in its own navy-and-grey shape, and Enter carries it on
     * through the pages that only ask to be confirmed, through the extraction and on to the services page,
     * where a switch ticked there is still ticked once the machine reaches its last page.
     */
    @ClientTest(timeoutTicks = 2_400)
    public static void bsdInstall_stopsOnServicesAndCarriesSshdThroughToTheEnd(final ClientTestContext ctx) {
        atTheInstaller(ctx, FREEBSD, ComputingModule.CD_DRIVE.get(), ComputingModule.CD_ROM.get())
                .thenScreenshot(2, "bsdinstall-welcome")
                .thenAssert(1, () -> "WELCOME".equals(installer(ctx).pageName()), "bsdinstall opens on its welcome")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> "NAME".equals(installer(ctx).pageName()), SCREEN_WAIT,
                        "Enter carries the welcome on to the hostname page")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> "COMPONENTS".equals(installer(ctx).pageName()), SCREEN_WAIT,
                        "and on again to the components page")
                .thenScreenshot(2, "bsdinstall-components")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_SPACE))
                .thenScreenshot(2, "bsdinstall-components-ports-unticked")
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> "DISK".equals(installer(ctx).pageName()), SCREEN_WAIT,
                        "and on to the disk page")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> "MIRROR".equals(installer(ctx).pageName()), SCREEN_WAIT,
                        "and on to the Mirror page")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> "COPY".equals(installer(ctx).pageName()), SCREEN_WAIT,
                        "the extraction begins")
                .thenScreenshot(2, "bsdinstall-extraction")
                .thenWaitUntil(() -> "SERVICES".equals(installer(ctx).pageName()), COPY_WAIT,
                        "the extraction stops on the services page rather than finishing behind it")
                .thenScreenshot(2, "bsdinstall-services")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_DOWN))
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_SPACE))
                .thenAssert(1, () -> "SERVICES".equals(installer(ctx).pageName()),
                        "ticking sshd does not carry the page on by itself")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> "DONE".equals(installer(ctx).pageName())
                        || "DESKTOP".equals(installer(ctx).pageName()), SCREEN_WAIT, "Enter carries Services on")
                .then(SETTLE, () -> {
                    if ("DESKTOP".equals(installer(ctx).pageName())) {
                        ctx.key(GLFW.GLFW_KEY_ENTER);
                    }
                })
                .thenWaitUntil(() -> "DONE".equals(installer(ctx).pageName()), COPY_WAIT,
                        "and on to the last page once every page that asks has been answered")
                .thenWaitUntilServer(level -> level.getBlockEntity(ctx.abs(MACHINE)) instanceof MainframeBlockEntity m
                                && m.console() != null && m.console().settings().remoteAllowed(),
                        SCREEN_WAIT, "Down then Space on the services page ticks sshd, not the row the cursor "
                                + "started on", level -> "");
    }

    /** The System V installer asks the disk and the name on one page, in its own reverse-video console. */
    @ClientTest(timeoutTicks = 1200)
    public static void systemV_opensOnItsSettingsPageInItsOwnConsole(final ClientTestContext ctx) {
        atTheInstaller(ctx, UNIX, ComputingModule.FLOPPY_DRIVE.get(), ComputingModule.FLOPPY_DISK.get())
                .thenScreenshot(2, "sysv-settings")
                .thenAssert(1, () -> "SETTINGS".equals(installer(ctx).pageName()),
                        "the disk and the name are asked together, on the one page this style has before the copy")
                .then(SETTLE, () -> ctx.type("DESK"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> "COPY".equals(installer(ctx).pageName()), SCREEN_WAIT,
                        "the name typed in carries the page on to the one copy this system makes");
    }

    private static InstallerScreen installer(final ClientTestContext ctx) {
        return ctx.screen(InstallerScreen.class);
    }

    /**
     * A machine with a guided installer's medium already in its drive, the installer already open on the glass
     * and the player looking at it.
     */
    private static ClientTestContext atTheInstaller(final ClientTestContext ctx, final ResourceLocation system,
                                                     final Block driveBlock, final Item mediumItem) {
        return ctx.thenBuild(0, world -> {
                    world.setBlock(MACHINE, ComputingModule.MAINFRAME.get());
                    final MainframeBlockEntity mainframe = world.blockEntity(MACHINE, MainframeBlockEntity.class);
                    final ItemStackHandler inv = mainframe.getInventory();
                    inv.setStackInSlot(MainframeBlockEntity.MOTHERBOARD_SLOT,
                            new ItemStack(ComputingModule.MOTHERBOARD_MTX_P.get()));
                    inv.setStackInSlot(MainframeBlockEntity.CPU_SLOTS_START,
                            new ItemStack(ComputingModule.CPU_SERVO_2620.get()));
                    inv.setStackInSlot(MainframeBlockEntity.RAM_SLOTS_START,
                            new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
                    inv.setStackInSlot(MainframeBlockEntity.PSU_SLOT, new ItemStack(ComputingModule.PSU_650G.get()));
                    inv.setStackInSlot(MainframeBlockEntity.GPU_SLOTS_START,
                            new ItemStack(ComputingModule.GPU_HD_7970.get()));
                    inv.setStackInSlot(MainframeBlockEntity.DISK_SLOTS_START,
                            new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
                    mainframe.togglePower();
                    world.setBlock(DRIVE, driveBlock);
                    final ItemStack disc = new ItemStack(mediumItem);
                    MediaItem.setKind(disc, MediaKind.OS_INSTALL);
                    MediaItem.setPayload(disc, system);
                    world.blockEntity(DRIVE, MediaReaderBlockEntity.class).mediaSlot().setStackInSlot(0, disc);
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenServer(SETTLE * 3, level -> {
                    final MainframeBlockEntity mainframe = TestWorldBuilder.at(level, ctx.origin())
                            .blockEntity(MACHINE, MainframeBlockEntity.class);
                    mainframe.setNeedsPost(false);
                    FirmwarePayloads.beginInstall(level, mainframe, -1L, -1);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(InstallerScreen.class, SCREEN_WAIT);
    }
}
