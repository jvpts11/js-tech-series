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
import dev.jstech.computers.audio.SongFiles;
import dev.jstech.computers.audio.SoundfoundryShare;
import dev.jstech.computers.audio.SoundfoundryTexts;
import dev.jstech.computers.audio.catalog.SoundfoundryCatalog;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.operation.payload.SoundfoundryShareStatePayload;
import dev.jstech.computers.operation.payload.music.SoundfoundrySharePayloads;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.computers.os.fs.RecordingFile;
import dev.jstech.computers.os.fs.SystemLayout;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.program.SongDownload;
import dev.jstech.core.audio.media.MediaId;
import dev.jstech.core.audio.media.MediaInfo;
import dev.jstech.core.audio.media.MediaStore;
import dev.jstech.core.network.DataTier;
import dev.jstech.core.text.Text;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestMedia;
import dev.jstech.tests.testkit.TestWorldBuilder;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Soundfoundry's sharing over the network, from the server's side: a search finds what another computer of the
 * network puts in its shared folder, and nothing else of it, and what the catalogue offers; a song asked for comes in
 * at the speed of the slowest cable on its way and is kept in the music folder, on the playlist when asked; it waits
 * while the computer sharing it is off, and is given up on when that computer stops sharing it.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SoundfoundryShareGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 8;
    private static final String SHARER = "studio";

    private SoundfoundryShareGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void search_findsWhatAnotherComputerSharesAndNothingElseOfIt(final GameTestHelper helper) {
        final Base base = wire(helper);
        share(helper, base.sharer(), "Harbour Song.wav", 300);
        keep(helper, base.sharer(), MusicImports.musicFolderOf(base.sharer()), "Harbour Secret.wav", 300);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final List<SoundfoundryShare.Found> found =
                            SoundfoundryShare.search(helper.getLevel(), base.asker(), "harbour");
                    final List<SoundfoundryShare.Found> shared = found.stream()
                            .filter(one -> one.source() != SongDownload.FROM_CATALOG).toList();
                    helper.assertTrue(shared.size() == 1 && shared.getFirst().title().equals("Harbour Song.wav")
                                    && shared.getFirst().from().equals(SHARER)
                                    && shared.getFirst().link() == DataTier.T1_ETHERNET,
                            "the shared song is found, from the computer sharing it, over its Ethernet; found "
                                    + found);
                    helper.assertTrue(SoundfoundryShare.peerCount(helper.getLevel(), base.asker()) == 1,
                            "and that computer counts as one the search looks through");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void download_comesInAtItsCableSpeedAndGoesOnThePlaylist(final GameTestHelper helper) {
        final Base base = wire(helper);
        // Twenty seconds of 16-bit samples at 8 kHz, which Ethernet's half a megabyte a second takes a while over.
        final String path = share(helper, base.sharer(), "Long Song.wav", 20_000);
        final ServerLevel level = helper.getLevel();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final Text said = base.asker().musicDownloads().start(level,
                            base.sharer().getBlockPos().asLong(), path, true);
                    helper.assertTrue(said.isEmpty(), "the song is on its way; said " + said.english());
                    final SongDownload download = downloads(base.asker()).getFirst();
                    helper.assertTrue(download.status() == SongDownload.Status.RUNNING
                                    && download.link() == DataTier.T1_ETHERNET,
                            "over the Ethernet between them");
                })
                .thenExecuteAfter(3, () -> {
                    final SongDownload download = downloads(base.asker()).getFirst();
                    helper.assertTrue(download.done() > 0 && !download.complete(),
                            "it comes a piece at a time, not at once; " + download.done() + " of "
                                    + download.bytes());
                })
                .thenWaitUntil(() -> helper.assertTrue(
                        downloads(base.asker()).getFirst().status() == SongDownload.Status.DONE, "it comes in whole"))
                .thenExecute(() -> {
                    final String kept = FsPaths.join(MusicImports.musicFolderOf(base.asker()), "Long Song.wav");
                    final RecordingFile file = SongFiles.read(level, base.asker(), kept);
                    helper.assertTrue(file != null && file.media().equals(downloads(base.asker()).getFirst().media()),
                            "and is kept in the music folder, naming the same recording");
                    helper.assertTrue(base.asker().console().soundfoundry().songs().contains(kept),
                            "and put on the playlist, as asked");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void download_waitsWhileTheSharingComputerIsOff(final GameTestHelper helper) {
        final Base base = wire(helper);
        final String path = share(helper, base.sharer(), "Patient Song.wav", 90_000);
        final ServerLevel level = helper.getLevel();
        final long[] held = {0L};
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> base.asker().musicDownloads().start(level,
                        base.sharer().getBlockPos().asLong(), path, false))
                .thenExecuteAfter(2, () -> base.sharer().setPowered(false))
                .thenWaitUntil(() -> helper.assertTrue(
                        downloads(base.asker()).getFirst().status() == SongDownload.Status.WAITING,
                        "a song whose computer went off waits"))
                .thenExecute(() -> held[0] = downloads(base.asker()).getFirst().done())
                .thenExecuteAfter(20, () -> helper.assertTrue(
                        downloads(base.asker()).getFirst().done() == held[0], "and nothing comes in meanwhile"))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void download_isGivenUpOnWhenTheSongIsNoLongerShared(final GameTestHelper helper) {
        final Base base = wire(helper);
        final String path = share(helper, base.sharer(), "Fleeting Song.wav", 90_000);
        final ServerLevel level = helper.getLevel();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> base.asker().musicDownloads().start(level,
                        base.sharer().getBlockPos().asLong(), path, false))
                .thenExecuteAfter(2, () -> DiskFilesystem.delete(base.sharer().systemDisk(), path))
                .thenWaitUntil(() -> {
                    final SongDownload download = downloads(base.asker()).getFirst();
                    helper.assertTrue(download.status() == SongDownload.Status.FAILED && download.trouble()
                                    .equals(SoundfoundryTexts.NOT_SHARED.with("Fleeting Song.wav", SHARER)),
                            "a song taken out of the shared folder is given up on, saying why");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void download_bringsInASongOfTheCatalogue(final GameTestHelper helper) {
        final Base base = wire(helper);
        final ServerLevel level = helper.getLevel();
        final SoundfoundryShare.Found[] offered = {null};
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(SoundfoundryCatalog.current().songs() > 0,
                        "the tests' album is in the catalogue"))
                .thenExecuteAfter(SETTLE, () -> {
                    offered[0] = SoundfoundryShare.search(level, base.asker(), "").stream()
                            .filter(one -> one.source() == SongDownload.FROM_CATALOG).findFirst().orElse(null);
                    helper.assertTrue(offered[0] != null, "a search of nothing finds the catalogue's songs");
                    final Text said = base.asker().musicDownloads().start(level, SongDownload.FROM_CATALOG,
                            offered[0].path(), false);
                    helper.assertTrue(said.isEmpty(), "and one is on its way; said " + said.english());
                    helper.assertTrue(downloads(base.asker()).getFirst().link() == DataTier.T1_ETHERNET,
                            "at the speed of the computer's own cable");
                })
                .thenWaitUntil(() -> helper.assertTrue(
                        downloads(base.asker()).getFirst().status() == SongDownload.Status.DONE, "it comes in"))
                .thenExecute(() -> {
                    final List<String> songs = SongFiles.under(level, base.asker(),
                            MusicImports.musicFolderOf(base.asker()));
                    helper.assertTrue(songs.stream().anyMatch(song -> {
                        final RecordingFile file = SongFiles.read(level, base.asker(), song);
                        return file != null && file.media().equals(offered[0].media());
                    }), "and is kept in the music folder");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void start_refusesAComputerOnNoNetwork(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final PersonalComputerBlockEntity alone = world.placeRunningPersonalComputer(new BlockPos(2, 2, 2));
        alone.console().install(Programs.SOUNDFOUNDRY.toString());
        final Text said = alone.musicDownloads().start(helper.getLevel(), SongDownload.FROM_CATALOG, "x/y.ogg", false);
        helper.assertTrue(said.equals(SoundfoundryTexts.NO_NETWORK.text()), "a computer on no network reaches nothing");
        helper.assertTrue(SoundfoundryShare.search(helper.getLevel(), alone, "").isEmpty(),
                "not even the catalogue");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void layDownSharedFolder_makesTheFolderSongsAreSharedFrom(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(new BlockPos(2, 2, 2));
        SoundfoundryShare.layDownSharedFolder(pc);
        helper.assertTrue(DiskFilesystem.listDirs(pc.systemDisk(), SystemLayout.MUSIC_DIR,
                        FilesystemKind.HIERARCHICAL).contains(SoundfoundryShare.sharedFolderOf(pc)),
                "the music folder has a shared folder in it");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void state_arrivesAsItWasSent(final GameTestHelper helper) {
        final Base base = wire(helper);
        final String path = share(helper, base.sharer(), "Sent Song.wav", 90_000);
        final ServerLevel level = helper.getLevel();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    base.asker().musicDownloads().start(level, base.sharer().getBlockPos().asLong(), path, false);
                    final SoundfoundryShareStatePayload sent = SoundfoundrySharePayloads.stateOf(level, base.asker(),
                            Optional.of(SoundfoundryShare.search(level, base.asker(), "sent")), true, Text.EMPTY);
                    final RegistryFriendlyByteBuf buf =
                            new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
                    SoundfoundryShareStatePayload.STREAM_CODEC.encode(buf, sent);
                    final SoundfoundryShareStatePayload read = SoundfoundryShareStatePayload.STREAM_CODEC.decode(buf);
                    helper.assertTrue(read.found().equals(sent.found()) && read.downloads().equals(sent.downloads())
                                    && read.shared().equals(sent.shared()) && read.computers() == sent.computers()
                                    && read.ownLink().equals(sent.ownLink()) && buf.readableBytes() == 0,
                            "the state arrives as it was sent");
                    helper.assertTrue(sent.found().orElseThrow().size() == 1 && sent.downloads().size() == 1,
                            "with what the search found and the song on its way");
                })
                .thenSucceed();
    }

    /* A Mainframe with a personal computer on each side, both running Soundfoundry; the east one shares. */
    private static Base wire(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final MainframeBlockEntity mainframe = world.placeRunningMainframe(new BlockPos(3, 2, 3));
        world.setBlock(new BlockPos(4, 2, 3), ComputingModule.HBW_CABLE.get());
        world.setBlock(new BlockPos(5, 2, 3), ComputingModule.PERSONAL_ROUTER.get());
        world.setBlock(new BlockPos(5, 2, 4), ComputingModule.ETHERNET_CABLE.get());
        final PersonalComputerBlockEntity sharer = world.placeRunningPersonalComputer(new BlockPos(5, 2, 5));
        world.setBlock(new BlockPos(2, 2, 3), ComputingModule.HBW_CABLE.get());
        world.setBlock(new BlockPos(1, 2, 3), ComputingModule.PERSONAL_ROUTER.get());
        world.setBlock(new BlockPos(1, 2, 4), ComputingModule.ETHERNET_CABLE.get());
        final PersonalComputerBlockEntity asker = world.placeRunningPersonalComputer(new BlockPos(1, 2, 5));
        sharer.console().install(Programs.SOUNDFOUNDRY.toString());
        asker.console().install(Programs.SOUNDFOUNDRY.toString());
        sharer.console().setComputerName(SHARER);
        return new Base(mainframe, sharer, asker);
    }

    /* A song of that length put in the computer's shared folder; its path there. */
    private static String share(final GameTestHelper helper, final PersonalComputerBlockEntity computer,
                                final String name, final int millis) {
        return keep(helper, computer, SoundfoundryShare.sharedFolderOf(computer), name, millis);
    }

    private static String keep(final GameTestHelper helper, final PersonalComputerBlockEntity computer,
                               final String folder, final String name, final int millis) {
        final MediaId song = put(TestMedia.wav(millis, name + " " + computer.getBlockPos().asLong()));
        MusicImports.keep(helper.getLevel(), computer.getBlockPos(), folder, name, song, info(song));
        final String path = FsPaths.join(folder, name);
        helper.assertTrue(SongFiles.read(helper.getLevel(), computer, path) != null, "the song is kept at " + path);
        return path;
    }

    private static List<SongDownload> downloads(final PersonalComputerBlockEntity computer) {
        return computer.console().soundfoundry().downloads();
    }

    private static MediaId put(final byte[] wav) {
        try {
            return MediaStore.current().orElseThrow().put(wav, "wav");
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

    /** The Mainframe in the middle, the computer sharing and the one asking. */
    private record Base(MainframeBlockEntity mainframe, PersonalComputerBlockEntity sharer,
                        PersonalComputerBlockEntity asker) {
    }
}
