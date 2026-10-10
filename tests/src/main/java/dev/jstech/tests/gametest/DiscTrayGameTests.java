/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.PatternEncoderBlock;
import dev.jstech.computers.blockentity.PatternEncoderBlockEntity;
import dev.jstech.computers.menu.PatternEncoderMenu;
import dev.jstech.computers.os.media.DiscTray;
import dev.jstech.computers.os.media.EjectButton;
import dev.jstech.computers.os.media.ITrayBlock;
import dev.jstech.computers.os.media.MediaFormat;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.tests.JsTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * The disc tray of the optical drives and of the Pattern Encoders that burn discs: the eject button on the front opens
 * and closes it, wherever the device faces and from whatever height it is pressed, while a click anywhere else on the
 * front leaves it be; a disc goes on it and comes off it only while it is out, and is not read or written there; a
 * stick goes into its port whatever the tray is doing.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class DiscTrayGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos POS = new BlockPos(2, 2, 2);
    /** How far in front of the front the player's eyes are. */
    private static final double STANDING_OFF = 1.2;
    /*
     * How far above the button the player's eyes are when looking down at it: high enough that the look crosses the
     * face of the block well above a button set a pixel back, which is pressed all the same.
     */
    private static final double LOOKING_DOWN = 1.5;

    private DiscTrayGameTests() {
    }

    @GameTest(template = ARENA)
    public static void ejectButton_opensAndClosesTheTrayOfEveryDiscDeviceWhicheverWayItFaces(
            final GameTestHelper helper) {
        final List<Block> devices = List.of(ComputingModule.CD_DRIVE.get(), ComputingModule.DVD_DRIVE.get(),
                ComputingModule.BLU_RAY_DRIVE.get(), ComputingModule.LEGACY_PATTERN_ENCODER.get(),
                ComputingModule.TRANSITION_PATTERN_ENCODER.get(), ComputingModule.PATTERN_ENCODER.get(),
                ComputingModule.ADVANCED_PATTERN_ENCODER.get());
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        for (final Block device : devices) {
            final EjectButton button = ((ITrayBlock) device).ejectButton();
            helper.assertTrue(button != null, device + " has an eject button");
            for (final Direction facing : Direction.Plane.HORIZONTAL) {
                helper.setBlock(POS, device.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing));
                final DiscTray tray = trayOf(helper, POS);
                final String where = device + " facing " + facing;

                click(helper, player, facing, 32, 48, 0, 0.0);
                helper.assertFalse(tray.isOpen(), "a click low in the middle of the front leaves the tray of "
                        + where + " closed");
                click(helper, player, facing, centreAcross(button), centreDown(button), button.depth(), 0.0);
                helper.assertTrue(tray.isOpen(), "the eject button of " + where + " opens its tray");
                click(helper, player, facing, centreAcross(button), centreDown(button), button.depth(),
                        LOOKING_DOWN);
                helper.assertFalse(tray.isOpen(), "the eject button of " + where
                        + ", pressed from above, closes its tray again");
            }
        }
        helper.assertTrue(((ITrayBlock) ComputingModule.FLOPPY_DRIVE.get()).ejectButton() == null
                        && ((ITrayBlock) ComputingModule.VINTAGE_PATTERN_ENCODER.get()).ejectButton() == null,
                "the floppy drive and the Vintage encoder have no tray");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void driveTray_takesADiscOnlyWhileOpenAndReadsItOnlyClosed(final GameTestHelper helper) {
        final Direction facing = Direction.SOUTH;
        helper.setBlock(POS, ComputingModule.CD_DRIVE.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, facing));
        final MediaReaderBlockEntity drive = (MediaReaderBlockEntity) helper.getBlockEntity(POS);
        final EjectButton button = drive.driveType().ejectButton();
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ComputingModule.CD_ROM.get()));

        click(helper, player, facing, 32, 48, 0, 0.0);
        helper.assertFalse(drive.hasMedia(), "a disc held to the closed tray does not go in");
        helper.assertTrue(player.getMainHandItem().is(ComputingModule.CD_ROM.get()), "the player keeps the disc");

        click(helper, player, facing, centreAcross(button), centreDown(button), button.depth(), 0.0);
        click(helper, player, facing, 32, 48, 0, 0.0);
        helper.assertTrue(drive.hasMedia() && player.getMainHandItem().isEmpty(), "the disc goes on the open tray");
        helper.assertTrue(drive.readableMedium().isEmpty() && drive.insertedFormat() == null,
                "a disc on the open tray is not read");

        click(helper, player, facing, centreAcross(button), centreDown(button), button.depth(), 0.0);
        helper.assertTrue(drive.insertedFormat() == MediaFormat.CD, "the tray closed, the disc is read");

        player.setShiftKeyDown(true);
        click(helper, player, facing, 32, 48, 0, 0.0);
        helper.assertTrue(drive.hasMedia(), "a sneak-click cannot reach the disc in the closed drive");
        player.setShiftKeyDown(false);
        click(helper, player, facing, centreAcross(button), centreDown(button), button.depth(), 0.0);
        player.setShiftKeyDown(true);
        click(helper, player, facing, 32, 48, 0, 0.0);
        helper.assertFalse(drive.hasMedia(), "a sneak-click lifts the disc off the open tray");
        helper.assertTrue(player.getInventory().contains(new ItemStack(ComputingModule.CD_ROM.get())),
                "and hands it to the player");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void burnerTray_aJobWaitsForTheTrayToCloseAndTheTrayForTheJob(final GameTestHelper helper) {
        helper.setBlock(POS, ComputingModule.LEGACY_PATTERN_ENCODER.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        final PatternEncoderBlockEntity encoder = (PatternEncoderBlockEntity) helper.getBlockEntity(POS);
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        encoder.tray().press(helper.getLevel(), encoder.getBlockPos());
        encoder.insertMedia(new ItemStack(ComputingModule.CD_RW.get()));
        helper.assertTrue(encoder.queueBurn("oak_planks", "x".repeat(4000)), "the job is queued");
        helper.startSequence()
                .thenExecuteAfter(10, () -> {
                    helper.assertTrue(encoder.phase() == PatternEncoderBlockEntity.Phase.IDLE,
                            "nothing is written on a disc on the open tray");
                    helper.assertTrue("Tray open".equals(encoder.statusLine().english()),
                            "the display says the tray is open; it says " + encoder.statusLine().english());
                    encoder.tray().press(helper.getLevel(), encoder.getBlockPos());
                })
                .thenWaitUntil(() -> helper.assertTrue(encoder.busy(), "the job starts once the tray closes"))
                .thenExecute(() -> {
                    PatternEncoderBlock.pressEjectButton(encoder, player);
                    helper.assertFalse(encoder.tray().isOpen(), "the tray stays closed while the head is down");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void burnerPort_takesAStickWhateverTheTrayAndThePanelKeepsTheTraysRule(
            final GameTestHelper helper) {
        final Direction facing = Direction.SOUTH;
        helper.setBlock(POS, ComputingModule.PATTERN_ENCODER.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, facing));
        final PatternEncoderBlockEntity encoder = (PatternEncoderBlockEntity) helper.getBlockEntity(POS);
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ComputingModule.USB_FLASH_DRIVE.get()));

        click(helper, player, facing, 32, 48, 0, 0.0);
        helper.assertTrue(encoder.mediaStack().is(ComputingModule.USB_FLASH_DRIVE.get()),
                "a stick goes into its port with the tray closed");
        player.setShiftKeyDown(true);
        click(helper, player, facing, 32, 48, 0, 0.0);
        helper.assertFalse(encoder.hasMedia(), "and comes out of it with a sneak-click");

        final PatternEncoderMenu panel = new PatternEncoderMenu(0, player.getInventory(), encoder);
        final ItemStack dvd = new ItemStack(ComputingModule.DVD_RW.get());
        helper.assertFalse(panel.getSlot(PatternEncoderMenu.MEDIA_SLOT).mayPlace(dvd),
                "the panel's slot takes no disc while the tray is closed");
        helper.assertTrue(panel.getSlot(PatternEncoderMenu.MEDIA_SLOT)
                        .mayPlace(new ItemStack(ComputingModule.USB_FLASH_DRIVE.get())),
                "but takes a stick");
        panel.clickMenuButton(player, PatternEncoderMenu.BUTTON_EJECT);
        helper.assertTrue(encoder.tray().isOpen(), "the panel's Eject opens the tray");
        helper.assertTrue(panel.getSlot(PatternEncoderMenu.MEDIA_SLOT).mayPlace(dvd),
                "and the slot takes a disc on the open tray");
        helper.succeed();
    }

    /*
     * Clicks the front of the device at POS facing {@code facing} with {@code player}'s eyes square in front of the
     * point {@code (across, down)}, in sixty-fourths from the front's top left as seen, {@code depth} sixty-fourths
     * behind the face, or {@code above} blocks over it looking down. The click lands where that look crosses the face
     * of the block, as the game's own aim would.
     */
    private static void click(final GameTestHelper helper, final Player player, final Direction facing,
                              final double across, final double down, final int depth, final double above) {
        final BlockPos at = helper.absolutePos(POS);
        final Vec3 out = Vec3.atLowerCornerOf(facing.getNormal());
        final Vec3 right = Vec3.atLowerCornerOf(facing.getCounterClockWise().getNormal());
        final Vec3 faceCentre = Vec3.atCenterOf(at).add(out.scale(0.5));
        final Vec3 onFace = faceCentre.add(right.scale(across / EjectButton.FRONT - 0.5))
                .add(0, 0.5 - down / EjectButton.FRONT, 0);
        final Vec3 target = onFace.subtract(out.scale(depth / (double) EjectButton.FRONT));
        final Vec3 eye = target.add(out.scale(STANDING_OFF + depth / (double) EjectButton.FRONT)).add(0, above, 0);
        player.setPos(eye.x, eye.y - player.getEyeHeight(), eye.z);
        // Where the look from the eyes to the target crosses the plane of the face.
        final Vec3 look = target.subtract(eye);
        final double toFace = out.dot(faceCentre.subtract(eye)) / out.dot(look);
        final Vec3 hit = eye.add(look.scale(toFace));
        helper.useBlock(POS, player, new BlockHitResult(hit, facing, at, false));
    }

    private static DiscTray trayOf(final GameTestHelper helper, final BlockPos pos) {
        final BlockEntity device = helper.getBlockEntity(pos);
        if (device instanceof MediaReaderBlockEntity drive) {
            return drive.tray();
        }
        return ((PatternEncoderBlockEntity) device).tray();
    }

    private static double centreAcross(final EjectButton button) {
        return (button.left() + button.right()) / 2.0;
    }

    private static double centreDown(final EjectButton button) {
        return (button.top() + button.bottom()) / 2.0;
    }
}
