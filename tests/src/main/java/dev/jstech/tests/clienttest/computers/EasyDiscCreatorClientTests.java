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
import dev.jstech.computers.MediaDevices;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.VinylDriveBlockEntity;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.client.os.EasyDiscCreatorApp;
import dev.jstech.computers.machine.DiscCopies;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.fs.BackupFile;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.media.MediaItem;
import dev.jstech.computers.os.media.MediaKind;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.computers.storage.ServerStorageContents;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

/**
 * Roxa's Easy Disc Creator on Frames XP, the whole way (computers/docs/design/programs.md): a computer with three CD
 * drives and a Vinyl RW Drive against it, the first holding a backup disc, the second a blank, the third a disk of the
 * network's storage. The list of drives opens under Copy from: and says what each holds; a copy of the backup onto
 * the blank shows its steps and its bar; the storage disk is refused; and a music disc on the vinyl drive is recorded
 * in its own time, after which the program asks for a blank vinyl.
 */
public final class EasyDiscCreatorClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 80;
    private static final int BOOT_WAIT = 400;
    private static final int WAIT = 200;
    private static final int COPY_WAIT = 600;
    /* The song of music disc 11 is 71 seconds; the program asks for a blank once it is recorded. */
    private static final int RECORD_WAIT = 1800;
    /* About two thirds of the way through writing a 150 KB backup onto a CD-RW. */
    private static final int WRITING = 90;
    private static final long BACKUP_BYTES = 153_600L;
    private static final String LABEL = "Easy Disc Creator";
    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);
    private static final BlockPos BACKUP_DRIVE = new BlockPos(4, 2, 2);
    private static final BlockPos BLANK_DRIVE = new BlockPos(5, 2, 1);
    private static final BlockPos STORAGE_DRIVE = new BlockPos(5, 2, 3);
    private static final BlockPos VINYL = new BlockPos(5, 3, 2);

    private EasyDiscCreatorClientTests() {
    }

    @ClientTest(timeoutTicks = 4800)
    public static void discCopier_listsCopiesRefusesAndRecordsAVinyl(final ClientTestContext ctx) {
        final String[] letters = new String[3];
        atTheDesktop(ctx)
                .then(SETTLE, () -> DesktopScreen.requestOpen(LABEL))
                .thenWaitUntil(() -> app(ctx) != null, SCREEN_WAIT, "Easy Disc Creator to open")
                .thenWaitUntilServer(level -> {
                    letters[0] = letterOf(ctx, level, BACKUP_DRIVE);
                    letters[1] = letterOf(ctx, level, BLANK_DRIVE);
                    letters[2] = letterOf(ctx, level, STORAGE_DRIVE);
                    return letters[0] != null && letters[1] != null && letters[2] != null;
                }, WAIT, "the three CD drives to be the computer's", level -> "not all linked yet")
                .then(SETTLE * 4, () -> ctx.clickDesktop(app(ctx).fromPoint()))
                .thenWaitUntil(() -> app(ctx).listShown() && app(ctx).listPoint(letters[0]) != null, WAIT,
                        "Copy from: to open the list of the drives")
                .then(2, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(4, "edc-copy-list")
                .then(SETTLE, () -> ctx.clickDesktop(app(ctx).listPoint(letters[0])))
                .then(SETTLE, () -> ctx.clickDesktop(app(ctx).toPoint()))
                .thenWaitUntil(() -> app(ctx).listPoint(letters[1]) != null, WAIT, "Record to: to open its list")
                .then(SETTLE, () -> ctx.clickDesktop(app(ctx).listPoint(letters[1])))
                .then(2, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(4, "edc-copy")
                .then(SETTLE, () -> ctx.clickDesktop(app(ctx).copyPoint()))
                .thenWaitUntil(() -> app(ctx).phase() == DiscCopies.Phase.WRITING.id(), COPY_WAIT,
                        "the backup to be read and written onto the blank")
                .then(WRITING, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(1, "edc-progress")
                .thenWaitUntil(() -> app(ctx).phase() == DiscCopies.Phase.DONE.id(), COPY_WAIT,
                        "the copy to be checked and done")
                .then(SETTLE, () -> ctx.clickDesktop(app(ctx).fromPoint()))
                .thenWaitUntil(() -> app(ctx).listPoint(letters[2]) != null, WAIT, "the list to open again")
                .then(SETTLE, () -> ctx.clickDesktop(app(ctx).listPoint(letters[2])))
                .then(SETTLE, () -> ctx.clickDesktop(app(ctx).copyPoint()))
                .thenWaitUntil(() -> EasyDiscCreatorApp.promptOkPoint() != null, WAIT,
                        "a storage disk to be refused in a box")
                .then(2, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(4, "edc-refused")
                .then(SETTLE, () -> ctx.clickDesktop(EasyDiscCreatorApp.promptOkPoint()))
                .thenWaitUntil(() -> EasyDiscCreatorApp.promptOkPoint() == null, WAIT, "OK to close the box")
                .then(SETTLE, () -> ctx.clickDesktop(app(ctx).fromPoint()))
                .thenWaitUntil(() -> app(ctx).listPoint("V") != null, WAIT, "the list to name the vinyl drive V:")
                .then(SETTLE, () -> ctx.clickDesktop(app(ctx).listPoint("V")))
                .then(2, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(4, "edc-vinyl-chosen")
                .then(SETTLE, () -> ctx.clickDesktop(app(ctx).copyPoint()))
                .thenWaitUntil(() -> app(ctx).phase() == DiscCopies.Phase.RECORDING.id(), WAIT,
                        "the drive to record the music disc")
                .thenWaitUntil(() -> EasyDiscCreatorApp.promptOkPoint() != null, RECORD_WAIT,
                        "the program to ask for a blank vinyl once the song is recorded")
                .then(2, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(4, "edc-vinyl");
    }

    /*
     * A Frames XP computer with Easy Disc Creator and a monitor; a CD drive with a backup disc, one with a blank and
     * one with a storage disk against it, and a Vinyl RW Drive with music disc 11 on it on top; the player at its
     * desktop.
     */
    private static ClientTestContext atTheDesktop(final ClientTestContext ctx) {
        return ctx.thenBuild(0, builder -> {
                    final CraftingComputerBlockEntity computer = builder.placeRunningCraftingComputer(COMPUTER);
                    computer.togglePower();
                    computer.formatDisk(0);
                    TestWorldBuilder.installDesktop(computer, jsc("frames_xp"), jsc("easy_disc_creator"));
                    computer.togglePower();
                    builder.placeMonitor(MONITOR, Direction.EAST);
                    builder.setBlock(BACKUP_DRIVE, ComputingModule.CD_DRIVE.get());
                    builder.setBlock(BLANK_DRIVE, ComputingModule.CD_DRIVE.get());
                    builder.setBlock(STORAGE_DRIVE, ComputingModule.CD_DRIVE.get());
                    builder.setBlock(VINYL, MediaDevices.VINYL_DRIVE.get());
                    builder.blockEntity(BACKUP_DRIVE, MediaReaderBlockEntity.class).mediaSlot()
                            .setStackInSlot(0, backupDisc());
                    builder.blockEntity(BLANK_DRIVE, MediaReaderBlockEntity.class).mediaSlot()
                            .setStackInSlot(0, new ItemStack(ComputingModule.CD_RW.get()));
                    builder.blockEntity(STORAGE_DRIVE, MediaReaderBlockEntity.class).mediaSlot()
                            .setStackInSlot(0, storageDisc());
                    builder.blockEntity(VINYL, VinylDriveBlockEntity.class).platter()
                            .setStackInSlot(0, new ItemStack(Items.MUSIC_DISC_11));
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT);
    }

    /* The letter the program names the drive at {@code drive} by, once it is the computer's; null before. */
    @Nullable
    private static String letterOf(final ClientTestContext ctx, final ServerLevel level, final BlockPos drive) {
        if (!(level.getBlockEntity(ctx.abs(COMPUTER)) instanceof IOsHost host)) {
            return null;
        }
        final long at = ctx.abs(drive).asLong();
        for (final DiscCopies.Place place : DiscCopies.places(level, host)) {
            if (place.pos() == at) {
                return Character.toString(place.letter());
            }
        }
        return null;
    }

    /* A CD-RW named "Plant backup" holding one backup of 150 KB. */
    private static ItemStack backupDisc() {
        final ItemStack disc = new ItemStack(ComputingModule.CD_RW.get());
        DiskFilesystem.write(disc, "plant.bak", FileType.BAK, new BackupFile(UUID.randomUUID(), BACKUP_BYTES).write(),
                Long.MAX_VALUE, FilesystemKind.HIERARCHICAL);
        disc.set(DataComponents.CUSTOM_NAME, Component.literal("Plant backup"));
        return disc;
    }

    /* A CD-ROM holding items of the network's storage. */
    private static ItemStack storageDisc() {
        final ItemStack disc = new ItemStack(ComputingModule.CD_ROM.get());
        MediaItem.setKind(disc, MediaKind.DATA);
        MediaItem.setData(disc, new ServerStorageContents(Map.of(StorageKey.of(Items.IRON_INGOT), 64L)));
        return disc;
    }

    @Nullable
    private static EasyDiscCreatorApp app(final ClientTestContext ctx) {
        if (!(ctx.mc().screen instanceof DesktopScreen desktop)) {
            return null;
        }
        final List<DesktopWindow> windows = desktop.windowsFor(LABEL);
        return !windows.isEmpty() && windows.getFirst().app() instanceof EasyDiscCreatorApp app ? app : null;
    }

    private static ResourceLocation jsc(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, path);
    }
}
