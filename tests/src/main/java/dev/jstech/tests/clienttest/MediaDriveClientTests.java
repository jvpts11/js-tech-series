/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.os.media.EjectButton;
import dev.jstech.computers.os.media.ITrayBlock;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.Objects;

/**
 * The floppy, CD, DVD and Blu-ray drives as a player sees them: each the drive of its day, the very medium the player
 * put in drawn in it (a floppy in the slot, a disc on the tray), a floppy taken out still drawn on its way out and then
 * gone, the power lamp lit once a computer is linked, and the drive's item dark in a frame. An optical drive's eject
 * button is outlined when looked at, and a click on it brings the tray out with its disc and takes it back in. The
 * Dock Station beside them stands with its stick in the port. Each drive is shot close up as its medium moves.
 */
public final class MediaDriveClientTests {

    private static final int SETTLE = 4;

    private static final BlockPos FLOPPY = new BlockPos(4, 2, 2);
    private static final BlockPos CD = new BlockPos(6, 2, 2);
    private static final BlockPos DVD = new BlockPos(8, 2, 2);
    private static final BlockPos DOCK = new BlockPos(10, 2, 2);
    private static final BlockPos BLU_RAY = new BlockPos(12, 2, 2);
    private static final BlockPos PLAYER = new BlockPos(7, 2, 6);
    /** The block an item frame hangs on, beside the Floppy Drive. */
    private static final BlockPos FRAME_WALL = new BlockPos(5, 2, 2);

    private MediaDriveClientTests() {
    }

    @ClientTest(timeoutTicks = 1500)
    public static void mediaDrives_drawThePlayersOwnMediaAndSeeThemOut(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
                    world.setBlock(FLOPPY, DeviceCloseUp.facingPlayer(ComputingModule.FLOPPY_DRIVE.get()));
                    world.setBlock(CD, DeviceCloseUp.facingPlayer(ComputingModule.CD_DRIVE.get()));
                    world.setBlock(DVD, DeviceCloseUp.facingPlayer(ComputingModule.DVD_DRIVE.get()));
                    world.setBlock(DOCK, DeviceCloseUp.facingPlayer(ComputingModule.DOCK_STATION.get()));
                    world.setBlock(BLU_RAY, DeviceCloseUp.facingPlayer(ComputingModule.BLU_RAY_DRIVE.get()));
                })
                .thenTeleport(SETTLE, PLAYER, Direction.NORTH)
                .thenServer(SETTLE, level -> {
                    insert(ctx, level, FLOPPY, ComputingModule.FLOPPY_DISK.get());
                    insert(ctx, level, CD, ComputingModule.CD_ROM.get());
                    insert(ctx, level, DVD, ComputingModule.DVD_RW.get());
                    insert(ctx, level, DOCK, ComputingModule.USB_FLASH_DRIVE.get());
                    insert(ctx, level, BLU_RAY, ComputingModule.BD_ROM.get());
                })
                .thenScreenshot(30, "all-loaded")
                .thenAssert(0, () -> drawn(ctx, CD).is(ComputingModule.CD_ROM.get()),
                        "the CD Drive draws the CD-ROM the player put in");

        DeviceCloseUp.closeUp(ctx, FLOPPY)
                .thenScreenshot(SETTLE, "floppy-loaded")
                .thenServer(0, level -> insert(ctx, level, FLOPPY, null))
                .thenScreenshot(DeviceCloseUp.FLOPPY_OUT, "floppy-leaving")
                .thenAssert(0, () -> drawn(ctx, FLOPPY).is(ComputingModule.FLOPPY_DISK.get()),
                        "a floppy taken out is still drawn on its way out");

        // Looked at, the eject button is outlined on its own; a click on it brings the tray out with its disc.
        DeviceCloseUp.aimAtEjectButton(ctx, CD, button(ComputingModule.CD_DRIVE.get()))
                .thenScreenshot(SETTLE, "cd-eject-button");
        DeviceCloseUp.clickCrosshair(ctx, 0)
                .thenScreenshot(DeviceCloseUp.TRAY_OUT, "cd-tray-out")
                .thenAssert(0, () -> clientDrive(ctx, CD).tray().isOpen()
                                && drawn(ctx, CD).is(ComputingModule.CD_ROM.get()),
                        "the eject button brings the tray out with the CD-ROM on it")
                .thenServer(0, level -> insert(ctx, level, CD, null))
                .thenAssert(2, () -> drawn(ctx, CD).isEmpty() && drawn(ctx, FLOPPY).isEmpty(),
                        "a disc lifted off the tray is gone from it at once");
        DeviceCloseUp.clickCrosshair(ctx, 0)
                .thenScreenshot(DeviceCloseUp.TRAY_OUT, "cd-tray-closed")
                .thenAssert(0, () -> !clientDrive(ctx, CD).tray().isOpen(), "a second press takes the tray back in");

        DeviceCloseUp.aimAtEjectButton(ctx, DVD, button(ComputingModule.DVD_DRIVE.get()));
        DeviceCloseUp.clickCrosshair(ctx, SETTLE)
                .thenScreenshot(DeviceCloseUp.TRAY_OUT, "dvd-tray-out")
                .thenServer(0, level -> insert(ctx, level, DVD, ComputingModule.CD_RW.get()));
        DeviceCloseUp.clickCrosshair(ctx, SETTLE)
                .thenScreenshot(DeviceCloseUp.TRAY_OUT, "dvd-reads-a-cd");

        // The Blu-ray drive, white all over, its slim tray high on the front, with the BD-ROM on it.
        DeviceCloseUp.aimAtEjectButton(ctx, BLU_RAY, button(ComputingModule.BLU_RAY_DRIVE.get()))
                .thenScreenshot(SETTLE, "blu-ray-eject-button");
        DeviceCloseUp.clickCrosshair(ctx, 0)
                .thenScreenshot(DeviceCloseUp.TRAY_OUT, "blu-ray-tray-out")
                .thenAssert(0, () -> clientDrive(ctx, BLU_RAY).tray().isOpen()
                                && drawn(ctx, BLU_RAY).is(ComputingModule.BD_ROM.get()),
                        "the Blu-ray drive draws its disc on the open tray");

        /*
         * With a computer behind it the Floppy Drive's power lamp lights, which needs the link on the client; the
         * drive's item in the frame beside it shares the model and stays dark.
         */
        ctx.thenBuild(0, world -> {
                    world.placeRunningPersonalComputer(FLOPPY.north());
                    world.setBlock(FRAME_WALL, Blocks.STONE);
                })
                .thenServer(0, level -> {
                    insert(ctx, level, FLOPPY, ComputingModule.FLOPPY_DISK.get());
                    final ItemFrame frame = new ItemFrame(level, ctx.abs(FRAME_WALL.south()), Direction.SOUTH);
                    frame.setItem(new ItemStack(ComputingModule.FLOPPY_DRIVE.get()));
                    level.addFreshEntity(frame);
                })
                .thenWaitUntil(() -> clientDrive(ctx, FLOPPY) != null && clientDrive(ctx, FLOPPY).ownerPos() != null,
                        100, "the client to see the Floppy Drive linked to the computer behind it");
        DeviceCloseUp.closeUp(ctx, FLOPPY)
                .thenScreenshot(SETTLE, "floppy-linked")
                .thenServer(0, level -> level.getEntitiesOfClass(ItemFrame.class,
                        new AABB(ctx.abs(FRAME_WALL.south()))).forEach(ItemFrame::discard));
    }

    private static EjectButton button(final Block drive) {
        return Objects.requireNonNull(((ITrayBlock) drive).ejectButton(), "a drive with a tray");
    }

    /* Puts that medium in the drive the way its slot takes it, or empties the drive for null. */
    private static void insert(final ClientTestContext ctx, final ServerLevel level, final BlockPos at,
                               final Item medium) {
        drive(ctx, level, at).mediaSlot().setStackInSlot(0, medium == null ? ItemStack.EMPTY : new ItemStack(medium));
    }

    private static MediaReaderBlockEntity drive(final ClientTestContext ctx, final ServerLevel level,
                                                final BlockPos at) {
        return (MediaReaderBlockEntity) level.getBlockEntity(ctx.abs(at));
    }

    /* The client's copy of the drive, or null before the client has it. */
    private static MediaReaderBlockEntity clientDrive(final ClientTestContext ctx, final BlockPos at) {
        return ctx.mc().level != null && ctx.mc().level.getBlockEntity(ctx.abs(at))
                instanceof MediaReaderBlockEntity drive ? drive : null;
    }

    /* What the client's copy of the drive draws. */
    private static ItemStack drawn(final ClientTestContext ctx, final BlockPos at) {
        final MediaReaderBlockEntity drive = clientDrive(ctx, at);
        return drive == null ? ItemStack.EMPTY : drive.drawnMedium();
    }
}
