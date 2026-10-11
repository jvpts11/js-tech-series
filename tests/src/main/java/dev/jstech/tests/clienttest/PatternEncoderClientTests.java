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
import dev.jstech.computers.client.PatternEncoderScreen;
import dev.jstech.computers.os.media.EjectButton;
import dev.jstech.computers.os.media.ITrayBlock;
import java.util.List;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;

/**
 * The Pattern Encoders as a player sees them: each era's device with the very medium the player put in drawn in its
 * bay (a floppy in the slot, a disc on the tray, a stick in the port, lying as its port does), a floppy taken out still
 * drawn on its way out, then gone, and a stick gone the moment it is pulled out. A burner's eject button is outlined
 * when looked at, and a click on it brings the tray out with its disc and takes it back in. Each device is shot close
 * up as its medium moves.
 */
public final class PatternEncoderClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 100;
    /** Long enough for the client to hear the stick was pulled out. */
    private static final int STICK_GONE = 3;
    /** Shots this far apart catch a lamp that blinks twice a second both lit and dark within four shots. */
    private static final int BLINK_STEP = 3;

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

    @ClientTest(timeoutTicks = 1500)
    public static void patternEncoders_drawThePlayersOwnMediaAndSeeThemOut(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
                    world.setBlock(VINTAGE, DeviceCloseUp.facingPlayer(ComputingModule.VINTAGE_PATTERN_ENCODER.get()));
                    world.setBlock(LEGACY, DeviceCloseUp.facingPlayer(ComputingModule.LEGACY_PATTERN_ENCODER.get()));
                    world.setBlock(STANDARD, DeviceCloseUp.facingPlayer(ComputingModule.PATTERN_ENCODER.get()));
                    world.setBlock(TRANSITION,
                            DeviceCloseUp.facingPlayer(ComputingModule.TRANSITION_PATTERN_ENCODER.get()));
                    world.setBlock(ADVANCED,
                            DeviceCloseUp.facingPlayer(ComputingModule.ADVANCED_PATTERN_ENCODER.get()));
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

        DeviceCloseUp.closeUp(ctx, VINTAGE)
                .thenScreenshot(SETTLE, "vintage-loaded")
                .thenServer(0, level -> insert(ctx, level, VINTAGE, null))
                .thenScreenshot(DeviceCloseUp.FLOPPY_OUT, "vintage-floppy-leaving")
                .thenAssert(0, () -> drawn(ctx, VINTAGE).is(ComputingModule.FLOPPY_DISK.get()),
                        "a floppy taken out is still drawn on its way out");

        // Looked at, the eject button is outlined on its own; a click on it brings the tray out with its disc.
        DeviceCloseUp.aimAtEjectButton(ctx, LEGACY, button(ComputingModule.LEGACY_PATTERN_ENCODER.get()))
                .thenScreenshot(SETTLE, "legacy-eject-button");
        DeviceCloseUp.clickCrosshair(ctx, 0)
                .thenScreenshot(DeviceCloseUp.TRAY_OUT, "legacy-tray-out")
                .thenAssert(0, () -> clientEncoder(ctx, LEGACY).tray().isOpen()
                                && drawn(ctx, LEGACY).is(ComputingModule.CD_RW.get()),
                        "the eject button brings the tray out with the CD-RW on it")
                .thenServer(0, level -> insert(ctx, level, LEGACY, null))
                .thenAssert(2, () -> drawn(ctx, LEGACY).isEmpty() && drawn(ctx, VINTAGE).isEmpty(),
                        "a disc lifted off the tray is gone from it at once");
        DeviceCloseUp.clickCrosshair(ctx, 0)
                .thenScreenshot(DeviceCloseUp.TRAY_OUT, "legacy-tray-closed");

        DeviceCloseUp.aimAtEjectButton(ctx, STANDARD, button(ComputingModule.PATTERN_ENCODER.get()));
        DeviceCloseUp.clickCrosshair(ctx, SETTLE)
                .thenScreenshot(DeviceCloseUp.TRAY_OUT, "standard-tray-out")
                .thenServer(0, level -> insert(ctx, level, STANDARD, null));
        DeviceCloseUp.clickCrosshair(ctx, SETTLE)
                .thenServer(DeviceCloseUp.TRAY_OUT,
                        level -> insert(ctx, level, STANDARD, ComputingModule.USB_FLASH_DRIVE.get()))
                .thenScreenshot(20, "standard-usb-in")
                .thenAssert(0, () -> drawn(ctx, STANDARD).is(ComputingModule.USB_FLASH_DRIVE.get()),
                        "the Standard encoder draws the stick in its port")
                .thenServer(0, level -> insert(ctx, level, STANDARD, null))
                .thenAssert(STICK_GONE, () -> drawn(ctx, STANDARD).isEmpty(),
                        "a stick pulled out is gone at once: no clip carries it out")
                .thenScreenshot(0, "standard-usb-out");

        // The Transition's LightScribe burner and the Advanced's Blu-ray writer, each with its disc on its tray.
        DeviceCloseUp.aimAtEjectButton(ctx, TRANSITION, button(ComputingModule.TRANSITION_PATTERN_ENCODER.get()));
        DeviceCloseUp.clickCrosshair(ctx, SETTLE)
                .thenScreenshot(DeviceCloseUp.TRAY_OUT, "transition-tray-out")
                .thenAssert(0, () -> drawn(ctx, TRANSITION).is(ComputingModule.DVD_RW.get()),
                        "the Transition encoder draws its DVD on the open tray");

        DeviceCloseUp.aimAtEjectButton(ctx, ADVANCED, button(ComputingModule.ADVANCED_PATTERN_ENCODER.get()))
                .thenScreenshot(SETTLE, "advanced-eject-button");
        DeviceCloseUp.clickCrosshair(ctx, 0)
                .thenScreenshot(DeviceCloseUp.TRAY_OUT, "advanced-tray-out")
                .thenAssert(0, () -> drawn(ctx, ADVANCED).is(ComputingModule.BD_RE.get()),
                        "the Advanced encoder draws its BD-RE on the open tray")
                .thenServer(0, level -> insert(ctx, level, ADVANCED, null));
        DeviceCloseUp.clickCrosshair(ctx, SETTLE)
                .thenServer(DeviceCloseUp.TRAY_OUT,
                        level -> insert(ctx, level, ADVANCED, ComputingModule.USB_FLASH_DRIVE.get()))
                .thenScreenshot(20, "advanced-usb-in")
                .thenAssert(0, () -> drawn(ctx, ADVANCED).is(ComputingModule.USB_FLASH_DRIVE.get()),
                        "and the stick in its USB-C port");

        // From the side the stick shows it stands straight out of the port.
        DeviceCloseUp.standAt(ctx, STANDARD.east(2).south(), Direction.WEST)
                .thenServer(DeviceCloseUp.AFTER_EJECT,
                        level -> insert(ctx, level, STANDARD, ComputingModule.USB_FLASH_DRIVE.get()))
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
        DeviceCloseUp.closeUp(ctx, VINTAGE)
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

    /*
     * An idle encoder's screen has Cancel dead from its first frame. The button was born live and only learned better
     * on the screen's first tick, which was long enough to click it.
     */
    @ClientTest(timeoutTicks = 600)
    public static void screen_opensWithCancelDeadOnAnIdleEncoder(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> world.setBlock(STANDARD,
                        DeviceCloseUp.facingPlayer(ComputingModule.PATTERN_ENCODER.get())))
                .thenTeleport(SETTLE, PLAYER, Direction.NORTH)
                // The top of the case, well clear of the eject button on its front.
                .then(SETTLE, () -> ctx.rightClick(STANDARD, Direction.UP))
                .thenAwaitScreen(PatternEncoderScreen.class, SCREEN_WAIT)
                .then(SETTLE, () -> {
                    final PatternEncoderScreen screen = ctx.screen(PatternEncoderScreen.class);
                    // Laid out from scratch, as on opening, and read before a tick has passed.
                    screen.resize(ctx.mc(), screen.width, screen.height);
                    final String cancel = I18n.get("jsc.pattern_encoder.screen.cancel_queue");
                    final List<Button> found = screen.children().stream().filter(Button.class::isInstance)
                            .map(Button.class::cast).filter(b -> b.getMessage().getString().equals(cancel)).toList();
                    ctx.assertTrue(found.size() == 1 && !found.getFirst().active,
                            "Cancel is there and dead from the first frame");
                })
                .then(0, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(SCREEN_WAIT);
    }

    private static EjectButton button(final Block encoder) {
        return Objects.requireNonNull(((ITrayBlock) encoder).ejectButton(), "an encoder with a tray");
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
