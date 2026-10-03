/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.PatternEncoderBlockEntity;
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
 * The Pattern Encoders as a player sees them: each era's device with the very medium the player put in drawn in its
 * bay (a floppy in the slot, a disc on the tray, a stick in the port), and a medium taken out still drawn on its way
 * out, then gone. Each device is shot close up in the middle of the clip that moves its medium.
 */
public final class PatternEncoderClientTests {

    private static final int SETTLE = 4;
    /** The floppy half out of its slot, before the hand takes it. */
    private static final int FLOPPY_OUT = 9;
    /** The tray fully out with its disc on it, before the hand takes the disc. */
    private static final int TRAY_OUT = 21;
    /** The stick half out of its port, before the hand takes it. */
    private static final int USB_OUT = 6;
    /** Past every eject clip, when a medium taken out is no longer drawn. */
    private static final int AFTER_EJECT = 45;
    /** Shots this far apart catch a lamp that blinks twice a second both lit and dark within four shots. */
    private static final int BLINK_STEP = 3;
    /** How far below level the player looks at a device close up, in degrees. */
    private static final float CLOSE_UP_PITCH = 28F;

    private static final BlockPos VINTAGE = new BlockPos(4, 2, 2);
    private static final BlockPos LEGACY = new BlockPos(6, 2, 2);
    private static final BlockPos STANDARD = new BlockPos(8, 2, 2);
    private static final BlockPos TRANSITION = new BlockPos(10, 2, 2);
    private static final BlockPos ADVANCED = new BlockPos(12, 2, 2);
    private static final BlockPos PLAYER = new BlockPos(6, 2, 6);
    /** The block an item frame hangs on, beside the Vintage encoder. */
    private static final BlockPos FRAME_WALL = new BlockPos(5, 2, 2);

    private PatternEncoderClientTests() {
    }

    @ClientTest(timeoutTicks = 1000)
    public static void patternEncoders_drawThePlayersOwnMediaAndSeeThemOut(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
                    world.setBlock(VINTAGE, facingPlayer(ComputingModule.VINTAGE_PATTERN_ENCODER.get()));
                    world.setBlock(LEGACY, facingPlayer(ComputingModule.LEGACY_PATTERN_ENCODER.get()));
                    world.setBlock(STANDARD, facingPlayer(ComputingModule.PATTERN_ENCODER.get()));
                    world.setBlock(TRANSITION, facingPlayer(ComputingModule.TRANSITION_PATTERN_ENCODER.get()));
                    world.setBlock(ADVANCED, facingPlayer(ComputingModule.ADVANCED_PATTERN_ENCODER.get()));
                })
                .thenTeleport(SETTLE, PLAYER, Direction.NORTH)
                .thenServer(SETTLE, level -> {
                    insert(ctx, level, VINTAGE, ComputingModule.FLOPPY_DISK.get());
                    insert(ctx, level, LEGACY, ComputingModule.CD_RW.get());
                    insert(ctx, level, STANDARD, ComputingModule.DVD_RW.get());
                    insert(ctx, level, TRANSITION, ComputingModule.DVD_RW.get());
                    insert(ctx, level, ADVANCED, ComputingModule.BD_RE.get());
                })
                .thenScreenshot(30, "all-loaded")
                .thenAssert(0, () -> drawn(ctx, LEGACY).is(ComputingModule.CD_RW.get()),
                        "the Legacy encoder draws the CD-RW the player put in");

        closeUp(ctx, VINTAGE)
                .thenScreenshot(SETTLE, "vintage-loaded")
                .thenServer(0, level -> insert(ctx, level, VINTAGE, null))
                .thenScreenshot(FLOPPY_OUT, "vintage-floppy-leaving")
                .thenAssert(0, () -> drawn(ctx, VINTAGE).is(ComputingModule.FLOPPY_DISK.get()),
                        "a floppy taken out is still drawn on its way out");

        closeUp(ctx, LEGACY)
                .thenServer(SETTLE, level -> insert(ctx, level, LEGACY, null))
                .thenScreenshot(TRAY_OUT, "legacy-tray-out")
                .thenAssert(0, () -> drawn(ctx, LEGACY).is(ComputingModule.CD_RW.get()),
                        "a disc taken out is still drawn on the open tray")
                .thenAssert(AFTER_EJECT, () -> drawn(ctx, LEGACY).isEmpty() && drawn(ctx, VINTAGE).isEmpty(),
                        "and no longer once it is out");

        closeUp(ctx, STANDARD)
                .thenServer(SETTLE, level -> insert(ctx, level, STANDARD, null))
                .thenScreenshot(TRAY_OUT, "standard-tray-out")
                .thenServer(AFTER_EJECT, level -> insert(ctx, level, STANDARD, ComputingModule.USB_FLASH_DRIVE.get()))
                .thenScreenshot(20, "standard-usb-in")
                .thenAssert(0, () -> drawn(ctx, STANDARD).is(ComputingModule.USB_FLASH_DRIVE.get()),
                        "the Standard encoder draws the stick in its port")
                .thenServer(0, level -> insert(ctx, level, STANDARD, null))
                .thenScreenshot(USB_OUT, "standard-usb-leaving");

        // The Transition's LightScribe burner and the Advanced's Blu-ray writer, each with its disc on its tray.
        closeUp(ctx, TRANSITION)
                .thenScreenshot(SETTLE, "transition-loaded")
                .thenServer(0, level -> insert(ctx, level, TRANSITION, null))
                .thenScreenshot(TRAY_OUT, "transition-tray-out")
                .thenAssert(0, () -> drawn(ctx, TRANSITION).is(ComputingModule.DVD_RW.get()),
                        "the Transition encoder draws its DVD on the open tray");

        closeUp(ctx, ADVANCED)
                .thenScreenshot(SETTLE, "advanced-loaded")
                .thenServer(0, level -> insert(ctx, level, ADVANCED, null))
                .thenScreenshot(TRAY_OUT, "advanced-tray-out")
                .thenAssert(0, () -> drawn(ctx, ADVANCED).is(ComputingModule.BD_RE.get()),
                        "the Advanced encoder draws its BD-RE on the open tray")
                .thenServer(AFTER_EJECT, level -> insert(ctx, level, ADVANCED, ComputingModule.USB_FLASH_DRIVE.get()))
                .thenScreenshot(20, "advanced-usb-in")
                .thenAssert(0, () -> drawn(ctx, ADVANCED).is(ComputingModule.USB_FLASH_DRIVE.get()),
                        "and the stick in its USB-C port");

        // From the side the stick shows it stands straight out of the port.
        standAt(ctx, STANDARD.east(2).south(), Direction.WEST)
                .thenServer(AFTER_EJECT, level -> insert(ctx, level, STANDARD, ComputingModule.USB_FLASH_DRIVE.get()))
                .thenScreenshot(20, "standard-usb-side");

        /*
         * With a computer behind it the power lamp lights, and while a pattern is written the activity lamp blinks;
         * the encoder item in the frame beside it shares the model and stays dark.
         */
        ctx.thenBuild(0, world -> {
                    world.placeRunningPersonalComputer(VINTAGE.north());
                    world.setBlock(FRAME_WALL, Blocks.STONE);
                })
                .thenServer(0, level -> {
                    insert(ctx, level, VINTAGE, ComputingModule.FLOPPY_DISK.get());
                    final ItemFrame frame = new ItemFrame(level, ctx.abs(FRAME_WALL.south()), Direction.SOUTH);
                    frame.setItem(new ItemStack(ComputingModule.VINTAGE_PATTERN_ENCODER.get()));
                    level.addFreshEntity(frame);
                })
                .thenWaitUntilServer(level -> encoder(ctx, level, VINTAGE).ownerPos() != null, 100,
                        "the Vintage encoder to link to the computer behind it", level -> "no link");
        closeUp(ctx, VINTAGE)
                .thenServer(SETTLE, level -> encoder(ctx, level, VINTAGE).queueBurn("smelt_iron", "x".repeat(5000)))
                .thenWaitUntil(() -> clientEncoder(ctx, VINTAGE) != null && clientEncoder(ctx, VINTAGE).busy(), 60,
                        "the client to see the pattern being written")
                .thenScreenshot(1, "vintage-writing-1")
                .thenScreenshot(BLINK_STEP, "vintage-writing-2")
                .thenScreenshot(BLINK_STEP, "vintage-writing-3")
                .thenScreenshot(BLINK_STEP, "vintage-writing-4")
                .thenAssert(0, () -> clientEncoder(ctx, VINTAGE).busy(), "the pattern is still being written")
                .thenServer(0, level -> level.getEntitiesOfClass(ItemFrame.class,
                        new AABB(ctx.abs(FRAME_WALL.south()))).forEach(ItemFrame::discard));
    }

    private static BlockState facingPlayer(final Block block) {
        return block.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH);
    }

    /* Stands the player two blocks in front of that device, looking down at its front. */
    private static ClientTestContext closeUp(final ClientTestContext ctx, final BlockPos device) {
        return standAt(ctx, device.south(2), Direction.NORTH);
    }

    /* Stands the player there looking that way and down, and waits for the client to have got there. */
    private static ClientTestContext standAt(final ClientTestContext ctx, final BlockPos at, final Direction facing) {
        return ctx.thenServer(0, level -> {
                    final BlockPos abs = ctx.abs(at);
                    ctx.serverPlayer().teleportTo(level, abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5,
                            facing.toYRot(), CLOSE_UP_PITCH);
                })
                .thenWaitUntil(() -> ctx.player() != null
                                && ctx.player().position().distanceTo(Vec3.atBottomCenterOf(ctx.abs(at))) < 0.1,
                        200, "the client's player to stand at " + at);
    }

    /* Puts that medium in the bay the way the bay's slot takes it, or empties the bay for null. */
    private static void insert(final ClientTestContext ctx, final ServerLevel level, final BlockPos at,
                               final Item medium) {
        encoder(ctx, level, at).media().setStackInSlot(0, medium == null ? ItemStack.EMPTY : new ItemStack(medium));
    }

    private static PatternEncoderBlockEntity encoder(final ClientTestContext ctx, final ServerLevel level,
                                                     final BlockPos at) {
        return (PatternEncoderBlockEntity) level.getBlockEntity(ctx.abs(at));
    }

    /* The client's copy of the encoder, or null before the client has it. */
    private static PatternEncoderBlockEntity clientEncoder(final ClientTestContext ctx, final BlockPos at) {
        return ctx.mc().level != null && ctx.mc().level.getBlockEntity(ctx.abs(at))
                instanceof PatternEncoderBlockEntity encoder ? encoder : null;
    }

    /* What the client's copy of the encoder draws in its bay. */
    private static ItemStack drawn(final ClientTestContext ctx, final BlockPos at) {
        final PatternEncoderBlockEntity encoder = clientEncoder(ctx, at);
        return encoder == null ? ItemStack.EMPTY : encoder.drawnMedium();
    }
}
