/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.blockentity.PatternEncoderBlockEntity;
import dev.jstech.computers.os.media.DiscTray;
import dev.jstech.computers.os.media.EjectButton;
import dev.jstech.computers.os.media.ITrayBlock;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;

/**
 * A player's hands on a disc device: the eject button pressed to bring the tray out and back, the disc lifted off an
 * open tray, and a disc from the hand set on it. Every device with a tray works the same way, a drive or the Pattern
 * Encoder.
 */
final class DiscSteps {

    private static final int SETTLE = 4;
    private static final int WAIT = 80;
    /** The hotbar slot the disc is handed to the player in. */
    private static final int HAND = 0;

    private DiscSteps() {
    }

    /** Presses the eject button of the disc device at {@code relative} and waits for its tray to be out. */
    static ClientTestContext openTray(final ClientTestContext ctx, final BlockPos relative) {
        return ctx.then(SETTLE, () -> pressEject(ctx, relative))
                .thenWaitUntilServer(level -> trayOf(ctx, level, relative).isOpen(), WAIT,
                        "the tray of the device at " + relative + " to come out", level -> "");
    }

    /** Presses it again and waits for the tray to be back in. */
    static ClientTestContext closeTray(final ClientTestContext ctx, final BlockPos relative) {
        return ctx.then(SETTLE, () -> pressEject(ctx, relative))
                .thenWaitUntilServer(level -> !trayOf(ctx, level, relative).isOpen(), WAIT,
                        "the tray of the device at " + relative + " to go back in", level -> "");
    }

    /**
     * Swaps whatever disc the drive at {@code relative} holds for {@code disc}, as a player does: the tray out, the
     * old disc lifted off it with a sneak-click, the new one set on it from the hand, and the tray pushed back in. A
     * floppy drive has no tray: its disk comes out with the sneak-click and goes in with the click.
     */
    static ClientTestContext swap(final ClientTestContext ctx, final BlockPos relative, final ItemStack disc) {
        ctx.thenGive(0, ItemStack.EMPTY).then(SETTLE, () -> ctx.selectHotbar(HAND));
        return trayIn(trayOut(ctx, relative)
                .thenSneakClick(1, relative, Direction.UP)
                .thenWaitUntilServer(level -> reader(ctx, level, relative).mediaSlot().getStackInSlot(0).isEmpty(),
                        WAIT, "the old disc to come off the tray of the drive at " + relative,
                        level -> "media=" + reader(ctx, level, relative).mediaSlot().getStackInSlot(0))
                .thenGive(0, disc.copy())
                .then(SETTLE, () -> ctx.selectHotbar(HAND))
                .thenRightClick(1, relative)
                .thenWaitUntilServer(level -> ItemStack.isSameItemSameComponents(
                                reader(ctx, level, relative).mediaSlot().getStackInSlot(0), disc), WAIT,
                        "the disc to sit in the drive at " + relative,
                        level -> "media=" + reader(ctx, level, relative).mediaSlot().getStackInSlot(0)), relative);
    }

    /** The drive at {@code relative}, read on the server. */
    static MediaReaderBlockEntity reader(final ClientTestContext ctx, final ServerLevel level,
                                         final BlockPos relative) {
        if (level.getBlockEntity(ctx.abs(relative)) instanceof MediaReaderBlockEntity drive) {
            return drive;
        }
        throw new ClientTestFailure("no media drive at " + relative);
    }

    /* The tray brought out when the device has one, nothing when it has none. */
    private static ClientTestContext trayOut(final ClientTestContext ctx, final BlockPos relative) {
        return ctx.then(SETTLE, () -> {
            if (hasTray(clientState(ctx, relative))) {
                pressEject(ctx, relative);
            }
        }).thenWaitUntilServer(level -> !hasTray(level.getBlockState(ctx.abs(relative)))
                        || trayOf(ctx, level, relative).isOpen(), WAIT,
                "the tray of the device at " + relative + " to come out", level -> "");
    }

    /* The tray pushed back in when the device has one, nothing when it has none. */
    private static ClientTestContext trayIn(final ClientTestContext ctx, final BlockPos relative) {
        return ctx.then(SETTLE, () -> {
            if (hasTray(clientState(ctx, relative))) {
                pressEject(ctx, relative);
            }
        }).thenWaitUntilServer(level -> !hasTray(level.getBlockState(ctx.abs(relative)))
                        || !trayOf(ctx, level, relative).isOpen(), WAIT,
                "the tray of the device at " + relative + " to go back in", level -> "");
    }

    /* Whether a device has an eject button: a disc drive's tray has one, a floppy drive's slot has none. */
    private static boolean hasTray(final BlockState state) {
        return state.getBlock() instanceof ITrayBlock device && device.ejectButton() != null;
    }

    /* The block at relative as the client sees it, which is what the player's hand reaches for. */
    private static BlockState clientState(final ClientTestContext ctx, final BlockPos relative) {
        return Objects.requireNonNull(ctx.mc().level, "no level").getBlockState(ctx.abs(relative));
    }

    /* The player's click on the middle of the eject button of the disc device at relative, whichever way it faces. */
    private static void pressEject(final ClientTestContext ctx, final BlockPos relative) {
        final BlockState state = clientState(ctx, relative);
        final EjectButton button = Objects.requireNonNull(((ITrayBlock) state.getBlock()).ejectButton(),
                "a device with a tray at " + relative);
        ctx.rightClickFront(relative, (button.left() + button.right()) / 2.0, (button.top() + button.bottom()) / 2.0,
                button.depth());
    }

    private static DiscTray trayOf(final ClientTestContext ctx, final ServerLevel level, final BlockPos relative) {
        final BlockEntity device = level.getBlockEntity(ctx.abs(relative));
        if (device instanceof MediaReaderBlockEntity drive) {
            return drive.tray();
        }
        return ((PatternEncoderBlockEntity) device).tray();
    }
}
