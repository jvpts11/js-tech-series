/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.MediaDevices;
import dev.jstech.computers.block.ExtendedCdDriveBlock;
import dev.jstech.computers.blockentity.ExtendedCdDriveBlockEntity;
import dev.jstech.computers.client.ExtendedCdDriveScreen;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.fs.BackupFile;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.media.FormattedMediaItem;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.lwjgl.glfw.GLFW;

/**
 * The Extended CD Drive of each era copying a disc (computers/docs/design/peripherals.md): a backup disc in its reading
 * tray, six blanks in its writing trays and the seventh empty, its window open while the copies are written, in the
 * look of the mod's devices of its era, as the mocks show it.
 */
public final class ExtendedCdDriveClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 160;
    private static final int READ_WAIT = 400;
    /* Every copy here is about 120 ticks of writing at its format's pace; the window is shot most of the way in. */
    private static final int WRITING = 72;
    private static final int BLANKS = 6;
    private static final BlockPos TOWER = new BlockPos(4, 2, 3);
    /* The player in front of the tower, which faces them. */
    private static final BlockPos PLAYER = TOWER.south(2);

    private ExtendedCdDriveClientTests() {
    }

    @ClientTest(timeoutTicks = 1200)
    public static void legacy_copiesACdOntoSixBlanks(final ClientTestContext ctx) {
        copying(ctx, MediaDevices.LEGACY_EXTENDED_CD_DRIVE, ComputingModule.CD_ROM, ComputingModule.CD_RW, 122_880L,
                "extcd-legacy");
    }

    @ClientTest(timeoutTicks = 1200)
    public static void transition_copiesADvdOntoSixBlanks(final ClientTestContext ctx) {
        copying(ctx, MediaDevices.TRANSITION_EXTENDED_CD_DRIVE, ComputingModule.DVD_ROM, ComputingModule.DVD_RW,
                491_520L, "extcd-transition");
    }

    @ClientTest(timeoutTicks = 1200)
    public static void advanced_copiesABluRayOntoSixBlanks(final ClientTestContext ctx) {
        copying(ctx, MediaDevices.ADVANCED_EXTENDED_CD_DRIVE, ComputingModule.BD_ROM, ComputingModule.BD_RE,
                1_966_080L, "extcd-advanced");
    }

    /*
     * A tower of one era facing the player, a disc of {@code read} holding a backup of {@code bytes} in its reading
     * tray and six of {@code blank} in its writing trays; its window opened, the copy started, and the window shot
     * while the copies are written.
     */
    private static void copying(final ClientTestContext ctx, final BlockEntry<ExtendedCdDriveBlock> block,
                                final ItemEntry<FormattedMediaItem> read, final ItemEntry<FormattedMediaItem> blank,
                                final long bytes, final String label) {
        ctx.thenServer(0, level -> ctx.serverPlayer().getInventory().clearContent())
                .thenBuild(0, world -> {
                    final BlockState lower = block.get().defaultBlockState()
                            .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH);
                    world.setBlock(TOWER, lower);
                    world.setBlock(TOWER.above(), lower.setValue(ExtendedCdDriveBlock.HALF, DoubleBlockHalf.UPPER));
                    final ExtendedCdDriveBlockEntity tower = world.blockEntity(TOWER, ExtendedCdDriveBlockEntity.class);
                    tower.readTray().setStackInSlot(0, backupDisc(read, bytes));
                    for (int tray = 0; tray < BLANKS; tray++) {
                        tower.writeTrays().setStackInSlot(tray, new ItemStack(blank.get()));
                    }
                })
                .thenTeleport(SETTLE * 5, PLAYER, Direction.NORTH)
                .thenRightClick(SETTLE, TOWER)
                .thenAwaitScreen(ExtendedCdDriveScreen.class, SCREEN_WAIT)
                .thenServer(SETTLE, level -> tower(ctx, level).copy())
                .thenWaitUntilServer(level -> tower(ctx, level).work() == ExtendedCdDriveBlockEntity.Work.WRITING,
                        READ_WAIT, "the tower to read the disc and start writing", level -> "work="
                                + tower(ctx, level).work() + " problem=" + tower(ctx, level).problem())
                .then(WRITING, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(1, label)
                .then(0, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(SCREEN_WAIT);
    }

    private static ExtendedCdDriveBlockEntity tower(final ClientTestContext ctx, final ServerLevel level) {
        return (ExtendedCdDriveBlockEntity) level.getBlockEntity(ctx.abs(TOWER));
    }

    /* A pressed disc named "Plant backup" holding one backup of {@code bytes}. */
    private static ItemStack backupDisc(final ItemEntry<FormattedMediaItem> disc, final long bytes) {
        final ItemStack stack = new ItemStack(disc.get());
        DiskFilesystem.write(stack, "plant.bak", FileType.BAK, new BackupFile(UUID.randomUUID(), bytes).write(),
                Long.MAX_VALUE, FilesystemKind.HIERARCHICAL);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Plant backup"));
        return stack;
    }
}
