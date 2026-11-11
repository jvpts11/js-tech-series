/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.MediaDevices;
import dev.jstech.computers.block.ExtendedCdDriveBlock;
import dev.jstech.computers.blockentity.ExtendedCdDriveBlockEntity;
import dev.jstech.computers.blockentity.VinylDriveBlockEntity;
import dev.jstech.computers.machine.DiscCopies;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.tests.JsTests;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Copying media (computers/docs/design/peripherals.md): the Extended CD Drive reads the disc in its reading tray and
 * writes it onto every blank in its writing trays, files and all, and refuses to start with nothing to read or no
 * blank; the Vinyl RW Drive plays a music disc while it records it, and cuts only onto a blank vinyl and only a song
 * that came from a disc.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MediaCopyGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos TOWER = new BlockPos(2, 2, 2);
    private static final BlockPos VINYL = new BlockPos(4, 2, 2);
    private static final String FILE = "notes.txt";
    private static final String WORDS = "copied, every word";

    private MediaCopyGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void tower_copiesTheDiscOntoEveryBlank(final GameTestHelper helper) {
        final ExtendedCdDriveBlockEntity tower = tower(helper);
        final ItemStack source = new ItemStack(ComputingModule.CD_RW.get());
        DiskFilesystem.write(source, FILE, FileType.TXT, WORDS, Long.MAX_VALUE, FilesystemKind.HIERARCHICAL);
        tower.readTray().setStackInSlot(0, source);
        tower.writeTrays().setStackInSlot(0, new ItemStack(ComputingModule.CD_RW.get()));
        tower.writeTrays().setStackInSlot(3, new ItemStack(ComputingModule.CD_RW.get()));
        helper.assertTrue(DiscCopies.blank(tower.writeTrays().getStackInSlot(0)) && !DiscCopies.blank(source),
                "an empty CD-RW is a blank and one with a file is not");
        tower.copy();
        helper.assertTrue(tower.work() == ExtendedCdDriveBlockEntity.Work.READING && tower.copies() == 2,
                "it reads the disc for its two blanks; " + tower.work() + ", " + tower.problem());
        helper.succeedWhen(() -> {
            helper.assertTrue(tower.work() == ExtendedCdDriveBlockEntity.Work.IDLE && tower.written(0)
                    && tower.written(3), "both copies are written");
            for (final int tray : new int[] {0, 3}) {
                helper.assertTrue(WORDS.equals(DiskFilesystem.read(tower.writeTrays().getStackInSlot(tray), FILE)
                        .orElse("")), "tray " + tray + " holds the file");
            }
            helper.assertTrue(tower.burned() == 2, "two discs burned; " + tower.burned());
        });
    }

    @GameTest(template = ARENA)
    public static void tower_startsNothingWithNothingToReadOrNoBlank(final GameTestHelper helper) {
        final ExtendedCdDriveBlockEntity tower = tower(helper);
        tower.copy();
        helper.assertTrue(tower.problem() == ExtendedCdDriveBlockEntity.Problem.NOTHING_TO_READ
                && tower.work() == ExtendedCdDriveBlockEntity.Work.IDLE, "an empty reading tray reads nothing");
        final ItemStack source = new ItemStack(ComputingModule.CD_RW.get());
        DiskFilesystem.write(source, FILE, FileType.TXT, WORDS, Long.MAX_VALUE, FilesystemKind.HIERARCHICAL);
        tower.readTray().setStackInSlot(0, source);
        tower.copy();
        helper.assertTrue(tower.problem() == ExtendedCdDriveBlockEntity.Problem.NO_BLANKS
                && tower.work() == ExtendedCdDriveBlockEntity.Work.IDLE, "with no blank it copies nothing");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void vinyl_recordsAMusicDiscWhileItPlaysAndStops(final GameTestHelper helper) {
        final VinylDriveBlockEntity drive = vinyl(helper);
        helper.assertTrue(drive.record(helper.getLevel()).isEmpty()
                && drive.problem() == VinylDriveBlockEntity.Problem.NO_DISC, "with no disc it records nothing");
        drive.platter().setStackInSlot(0, new ItemStack(Items.MUSIC_DISC_CAT));
        helper.assertTrue(drive.record(helper.getLevel()).isPresent()
                && drive.work() == VinylDriveBlockEntity.Work.RECORDING, "a music disc on it records");
        helper.assertTrue(drive.length() > 0, "for as long as its song: " + drive.length());
        drive.stop(helper.getLevel());
        helper.assertTrue(drive.work() == VinylDriveBlockEntity.Work.NONE, "and stops when told");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void vinyl_cutsOnlyOntoABlankAndOnlyASongFromADisc(final GameTestHelper helper) {
        final VinylDriveBlockEntity drive = vinyl(helper);
        final ResourceLocation cat = ResourceLocation.withDefaultNamespace("cat");
        helper.assertTrue(!drive.cut(helper.getLevel(), cat)
                && drive.problem() == VinylDriveBlockEntity.Problem.NO_BLANK, "with no blank vinyl nothing is cut");
        drive.platter().setStackInSlot(0, new ItemStack(MediaDevices.BLANK_VINYL_RECORD.get()));
        helper.assertTrue(!drive.cut(helper.getLevel(), null)
                && drive.problem() == VinylDriveBlockEntity.Problem.NOT_FROM_A_DISC,
                "a song that came from no disc is refused");
        helper.assertTrue(drive.cut(helper.getLevel(), cat) && drive.work() == VinylDriveBlockEntity.Work.CUTTING,
                "a disc's song is cut onto the blank");
        drive.stop(helper.getLevel());
        helper.assertTrue(drive.onPlatter().is(MediaDevices.BLANK_VINYL_RECORD.get()),
                "a cut stopped leaves the vinyl blank");
        helper.succeed();
    }

    /* A Legacy tower, both its halves, and its block entity. */
    private static ExtendedCdDriveBlockEntity tower(final GameTestHelper helper) {
        helper.setBlock(TOWER, MediaDevices.LEGACY_EXTENDED_CD_DRIVE.get().defaultBlockState());
        helper.setBlock(TOWER.above(), MediaDevices.LEGACY_EXTENDED_CD_DRIVE.get().defaultBlockState()
                .setValue(ExtendedCdDriveBlock.HALF, DoubleBlockHalf.UPPER));
        return (ExtendedCdDriveBlockEntity) helper.getBlockEntity(TOWER);
    }

    private static VinylDriveBlockEntity vinyl(final GameTestHelper helper) {
        helper.setBlock(VINYL, MediaDevices.VINYL_DRIVE.get());
        return (VinylDriveBlockEntity) helper.getBlockEntity(VINYL);
    }
}
