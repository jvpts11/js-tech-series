/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The floppy, CD and DVD drives as a player sees them: each the drive of its day, the very medium the player put in
 * drawn in it (a floppy in the slot, a disc on the tray), a medium taken out still drawn on its way out and then
 * gone, the power lamp lit once a computer is linked, and the drive's item dark in a frame. The Dock Station beside
 * them keeps its block model. Each drive is shot close up in the middle of the clip that moves its medium.
 */
public final class MediaDriveClientTests {

    private static final int SETTLE = 4;
    /** The floppy half out of its slot, before the hand takes it. */
    private static final int FLOPPY_OUT = 9;
    /** The tray fully out with its disc on it, before the hand takes the disc. */
    private static final int TRAY_OUT = 21;
    /** Past every eject clip, when a medium taken out is no longer drawn. */
    private static final int AFTER_EJECT = 45;
    /** How far below level the player looks at a drive close up, in degrees. */
    private static final float CLOSE_UP_PITCH = 28F;

    private static final BlockPos FLOPPY = new BlockPos(4, 2, 2);
    private static final BlockPos CD = new BlockPos(6, 2, 2);
    private static final BlockPos DVD = new BlockPos(8, 2, 2);
    private static final BlockPos DOCK = new BlockPos(10, 2, 2);
    private static final BlockPos PLAYER = new BlockPos(7, 2, 6);
    /** The block an item frame hangs on, beside the Floppy Drive. */
    private static final BlockPos FRAME_WALL = new BlockPos(5, 2, 2);

    private MediaDriveClientTests() {
    }

    @ClientTest(timeoutTicks = 1000)
    public static void mediaDrives_drawThePlayersOwnMediaAndSeeThemOut(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
                    world.setBlock(FLOPPY, facingPlayer(ComputingModule.FLOPPY_DRIVE.get()));
                    world.setBlock(CD, facingPlayer(ComputingModule.CD_DRIVE.get()));
                    world.setBlock(DVD, facingPlayer(ComputingModule.DVD_DRIVE.get()));
                    world.setBlock(DOCK, facingPlayer(ComputingModule.DOCK_STATION.get()));
                })
                .thenTeleport(SETTLE, PLAYER, Direction.NORTH)
                .thenServer(SETTLE, level -> {
                    insert(ctx, level, FLOPPY, ComputingModule.FLOPPY_DISK.get());
                    insert(ctx, level, CD, ComputingModule.CD_ROM.get());
                    insert(ctx, level, DVD, ComputingModule.DVD_RW.get());
                    insert(ctx, level, DOCK, ComputingModule.USB_FLASH_DRIVE.get());
                })
                .thenScreenshot(30, "all-loaded")
                .thenAssert(0, () -> drawn(ctx, CD).is(ComputingModule.CD_ROM.get()),
                        "the CD Drive draws the CD-ROM the player put in");

        closeUp(ctx, FLOPPY)
                .thenScreenshot(SETTLE, "floppy-loaded")
                .thenServer(0, level -> insert(ctx, level, FLOPPY, null))
                .thenScreenshot(FLOPPY_OUT, "floppy-leaving")
                .thenAssert(0, () -> drawn(ctx, FLOPPY).is(ComputingModule.FLOPPY_DISK.get()),
                        "a floppy taken out is still drawn on its way out");

        closeUp(ctx, CD)
                .thenServer(SETTLE, level -> insert(ctx, level, CD, null))
                .thenScreenshot(TRAY_OUT, "cd-tray-out")
                .thenAssert(0, () -> drawn(ctx, CD).is(ComputingModule.CD_ROM.get()),
                        "a disc taken out is still drawn on the open tray")
                .thenAssert(AFTER_EJECT, () -> drawn(ctx, CD).isEmpty() && drawn(ctx, FLOPPY).isEmpty(),
                        "and no longer once it is out");

        closeUp(ctx, DVD)
                .thenServer(SETTLE, level -> insert(ctx, level, DVD, null))
                .thenScreenshot(TRAY_OUT, "dvd-tray-out")
                .thenServer(AFTER_EJECT, level -> insert(ctx, level, DVD, ComputingModule.CD_RW.get()))
                .thenScreenshot(30, "dvd-reads-a-cd");

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
        closeUp(ctx, FLOPPY)
                .thenScreenshot(SETTLE, "floppy-linked")
                .thenServer(0, level -> level.getEntitiesOfClass(ItemFrame.class,
                        new AABB(ctx.abs(FRAME_WALL.south()))).forEach(ItemFrame::discard));
    }

    private static BlockState facingPlayer(final Block block) {
        return block.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH);
    }

    /* Stands the player two blocks in front of that drive, looking down at its front. */
    private static ClientTestContext closeUp(final ClientTestContext ctx, final BlockPos drive) {
        final BlockPos at = drive.south(2);
        return ctx.thenServer(0, level -> {
                    final BlockPos abs = ctx.abs(at);
                    ctx.serverPlayer().teleportTo(level, abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5,
                            Direction.NORTH.toYRot(), CLOSE_UP_PITCH);
                })
                .thenWaitUntil(() -> ctx.player() != null
                                && ctx.player().position().distanceTo(Vec3.atBottomCenterOf(ctx.abs(at))) < 0.1,
                        200, "the client's player to stand at " + at);
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
