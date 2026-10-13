/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.audio.MusicImports;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.fs.FilesystemContents;
import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.computers.os.fs.RecordingFile;
import dev.jstech.computers.os.fs.StoredFile;
import dev.jstech.computers.os.fs.SystemLayout;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.core.audio.media.MediaId;
import dev.jstech.core.audio.media.MediaInfo;
import dev.jstech.core.audio.media.MediaReceipt;
import dev.jstech.core.audio.media.MediaStore;
import dev.jstech.core.text.Text;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestMedia;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.io.IOException;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Songs on a computer's disk: brought to it and kept in its music folder as files naming the recordings the server
 * keeps, weighing what those recordings weigh, and never anything else. A song is not written unless the server keeps
 * it, never turns into text or text into one, and grows by nothing.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MusicFileGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos COMPUTER = new BlockPos(2, 2, 2);
    private static final String MUSIC = SystemLayout.MUSIC_DIR;

    private MusicFileGameTests() {
    }

    @GameTest(template = ARENA)
    public static void keep_putsASongInTheMusicFolderWeighingWhatItDoes(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = computer(helper);
        final MediaId song = put(TestMedia.wav(1200, "kept " + helper.absolutePos(COMPUTER).asLong()));
        final ItemStack disk = pc.systemDisk();
        final long before = DiskFilesystem.filesWeight(disk);
        final MediaReceipt said = MusicImports.keep(helper.getLevel(), helper.absolutePos(COMPUTER), "",
                "My Song.wav", song, info(song));
        final String path = MUSIC + "/My Song.wav";
        final StoredFile file = fileAt(disk, path);
        helper.assertTrue(said.accepted() && file != null && file.type() == FileType.WAV,
                "the song is in the music folder as a wave file; said " + said.message().english());
        final RecordingFile named = RecordingFile.read(file.content());
        helper.assertTrue(named != null && named.media().equals(song), "and it names the recording the server keeps");
        final long grew = DiskFilesystem.filesWeight(disk) - before;
        helper.assertTrue(grew == FsPaths.sizeMbEq(song.bytes(), DiskFilesystem.eraOf(disk)),
                "the disk fills by what the song weighs, not by its few lines; grew " + grew);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void keep_takesTheSameSongOnceAndNumbersAnotherOfTheSameName(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = computer(helper);
        final long at = helper.absolutePos(COMPUTER).asLong();
        final MediaId first = put(TestMedia.wav(300, "first " + at));
        final MediaId second = put(TestMedia.wav(300, "second " + at));
        final BlockPos host = helper.absolutePos(COMPUTER);
        MusicImports.keep(helper.getLevel(), host, "", "track.wav", first, info(first));
        MusicImports.keep(helper.getLevel(), host, "", "track.wav", first, info(first));
        MusicImports.keep(helper.getLevel(), host, "", "track.wav", second, info(second));
        final ItemStack disk = pc.systemDisk();
        helper.assertTrue(fileAt(disk, MUSIC + "/track.wav") != null && fileAt(disk, MUSIC + "/track_2.wav") != null
                        && fileAt(disk, MUSIC + "/track_3.wav") == null,
                "the same song brought twice is kept once, and another of that name is numbered");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void whyNot_refusesWhatIsNotMusicAndWhatDoesNotFit(final GameTestHelper helper) {
        computer(helper);
        final BlockPos host = helper.absolutePos(COMPUTER);
        final String hash = "ab".repeat(32);
        helper.assertTrue(MusicImports.NOT_MUSIC.text().equals(
                        MusicImports.whyNot(helper.getLevel(), host, "song.mp3", new MediaId(hash, "mp3", 10L))),
                "an mp3 is not kept");
        final Text tooBig = MusicImports.whyNot(helper.getLevel(), host, "huge.ogg",
                new MediaId(hash, "ogg", 1L << 50));
        helper.assertTrue(tooBig != null && tooBig.english().contains("huge.ogg"),
                "a song bigger than the disk is refused before a byte of it is sent");
        final Text fine = MusicImports.whyNot(helper.getLevel(), host, "fine.ogg", new MediaId(hash, "ogg", 10L));
        helper.assertTrue(fine == null, "and a small one is not");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void write_takesARecordingOnlyWhenTheServerKeepsIt(final GameTestHelper helper) {
        final MediaId kept = put(TestMedia.wav(200, "kept " + helper.absolutePos(COMPUTER).asLong()));
        final ItemStack disk = blankDisk();
        final String lines = new RecordingFile(kept, info(kept)).write();
        helper.assertTrue(write(disk, "song.wav", FileType.WAV, lines) == DiskFilesystem.WriteResult.OK,
                "a recording the server keeps is written");
        final MediaId unknown = new MediaId("0f".repeat(32), "wav", kept.bytes());
        helper.assertTrue(write(disk, "ghost.wav", FileType.WAV, new RecordingFile(unknown, info(kept)).write())
                        == DiskFilesystem.WriteResult.READ_ONLY,
                "one the server does not keep is not");
        final MediaId shrunk = new MediaId(kept.hash(), kept.format(), 1L);
        helper.assertTrue(write(disk, "small.wav", FileType.WAV, new RecordingFile(shrunk, info(kept)).write())
                        == DiskFilesystem.WriteResult.READ_ONLY,
                "nor one that names the right song with the wrong size, to weigh a byte");
        helper.assertTrue(write(disk, "song.ogg", FileType.OGG, lines) == DiskFilesystem.WriteResult.READ_ONLY,
                "nor a wave named as an Ogg");
        helper.assertTrue(write(disk, "hidden.txt", FileType.TXT, lines) == DiskFilesystem.WriteResult.READ_ONLY,
                "nor a song's lines kept as text, where they would weigh nothing");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void rename_neverTurnsASongIntoTextOrTextIntoASong(final GameTestHelper helper) {
        final MediaId kept = put(TestMedia.wav(200, "renamed " + helper.absolutePos(COMPUTER).asLong()));
        final ItemStack disk = blankDisk();
        write(disk, "song.wav", FileType.WAV, new RecordingFile(kept, info(kept)).write());
        write(disk, "notes.txt", FileType.TXT, "just notes");
        final FilesystemKind kind = FilesystemKind.HIERARCHICAL;
        helper.assertFalse(DiskFilesystem.rename(disk, "song.wav", "song.txt", kind), "a song is not renamed to text");
        helper.assertFalse(DiskFilesystem.rename(disk, "song.wav", "song.ogg", kind), "nor to another kind of song");
        helper.assertFalse(DiskFilesystem.rename(disk, "notes.txt", "notes.wav", kind), "nor text to a song");
        helper.assertTrue(DiskFilesystem.rename(disk, "song.wav", "better name.wav", kind),
                "and a song renamed as a song is");
        helper.assertTrue(DiskFilesystem.append(disk, "better name.wav", FileType.WAV, "more", Long.MAX_VALUE, kind,
                0L) == DiskFilesystem.WriteResult.READ_ONLY, "nothing is added to the end of a song");
        helper.succeed();
    }

    /* A running personal computer with Frames 11 on a standard disk. */
    private static PersonalComputerBlockEntity computer(final GameTestHelper helper) {
        return TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(COMPUTER);
    }

    private static MediaId put(final byte[] wav) {
        try {
            return MediaStore.current().orElseThrow(() -> new IllegalStateException("the server keeps no recordings"))
                    .put(wav, "wav");
        } catch (final IOException unexpected) {
            throw new IllegalStateException("a WAV of silence is a recording", unexpected);
        }
    }

    private static MediaInfo info(final MediaId media) {
        try {
            return MediaStore.current().orElseThrow().info(media);
        } catch (final IOException unexpected) {
            throw new IllegalStateException("a kept recording can be read", unexpected);
        }
    }

    private static ItemStack blankDisk() {
        return new ItemStack(ComputingModule.disk(StorageTier.SSD, DiskSize.GB_500));
    }

    private static DiskFilesystem.WriteResult write(final ItemStack disk, final String path, final FileType type,
                                                    final String content) {
        return DiskFilesystem.write(disk, path, type, content, Long.MAX_VALUE / 2, FilesystemKind.HIERARCHICAL, 0L);
    }

    private static StoredFile fileAt(final ItemStack disk, final String path) {
        return disk.getOrDefault(ComputingComponents.FILESYSTEM.get(), FilesystemContents.EMPTY).files().get(path);
    }
}
