/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.client.audio.MusicImporter;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.fs.FilesystemContents;
import dev.jstech.computers.os.fs.RecordingFile;
import dev.jstech.computers.os.fs.StoredFile;
import dev.jstech.computers.os.fs.SystemLayout;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.core.audio.media.MediaId;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.tests.testkit.TestMedia;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Songs brought from the player's own computer to a computer in the world, the whole way: sent from the desktop the
 * player is at, kept in that system's music folder, a file that is not a song passed over, and nothing taken from a
 * player who is not at the computer's screen.
 */
public final class MusicImportClientTests {

    private static final int SETTLE = 4;
    private static final int BOOT_WAIT = 400;
    private static final int WAIT = 200;

    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);

    private MusicImportClientTests() {
    }

    @ClientTest(timeoutTicks = 1200)
    public static void bring_keepsTheSongOnTheComputerAndPassesOverWhatIsNotOne(final ClientTestContext ctx) {
        final byte[] wav = TestMedia.wav(900, "imported " + System.nanoTime());
        final MediaId id = MediaId.of(wav, "wav");
        final Path song = write(wav, ".wav");
        final Path notes = write("just notes".getBytes(StandardCharsets.US_ASCII), ".txt");
        final CraftingComputerBlockEntity[] machine = new CraftingComputerBlockEntity[1];
        final Outcome outcome = new Outcome();
        ctx.thenBuild(0, world -> {
                    machine[0] = world.placeRunningCraftingComputer(COMPUTER);
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .then(SETTLE, () -> MusicImporter.bring(List.of(song, notes), ctx.abs(COMPUTER), "", outcome))
                .thenWaitUntil(() -> outcome.done, WAIT, "both files to be dealt with")
                .thenAssert(0, () -> outcome.results.equals(List.of(true, false)),
                        "the song is kept and the notes are passed over")
                .thenWaitUntilServer(level -> keptSong(machine[0], id) != null, WAIT,
                        "the song is in the system's music folder, naming the recording the server keeps",
                        level -> "messages " + outcome.messages);
    }

    @ClientTest(timeoutTicks = 600)
    public static void bring_takesNothingFromAPlayerNotAtTheComputer(final ClientTestContext ctx) {
        final byte[] wav = TestMedia.wav(300, "not at it " + System.nanoTime());
        final Path song = write(wav, ".wav");
        final CraftingComputerBlockEntity[] machine = new CraftingComputerBlockEntity[1];
        final Outcome outcome = new Outcome();
        ctx.thenBuild(0, world -> machine[0] = world.placeRunningCraftingComputer(COMPUTER))
                .then(SETTLE, () -> MusicImporter.bring(List.of(song), ctx.abs(COMPUTER), "", outcome))
                .thenWaitUntil(() -> outcome.done, WAIT, "the song to be answered")
                .thenAssert(0, () -> outcome.results.equals(List.of(false))
                                && GameText.resolve(outcome.messages.getFirst()).contains("screen"),
                        "a player away from the computer's screen is told they cannot bring it music")
                .thenWaitUntilServer(level -> keptSong(machine[0], MediaId.of(wav, "wav")) == null, 1,
                        "and nothing is put on its disk", level -> "a song was kept");
    }

    /* The song on the computer's disk in its music folder that names that recording, or null. */
    private static StoredFile keptSong(final CraftingComputerBlockEntity computer, final MediaId id) {
        final FilesystemContents fs = computer.systemDisk()
                .getOrDefault(ComputingComponents.FILESYSTEM.get(), FilesystemContents.EMPTY);
        for (final StoredFile file : fs.files().values()) {
            final RecordingFile named = RecordingFile.read(file.content());
            if (file.path().startsWith(SystemLayout.MUSIC_DIR + "/") && file.type() == FileType.WAV
                    && named != null && named.media().equals(id)) {
                return file;
            }
        }
        return null;
    }

    private static Path write(final byte[] content, final String suffix) {
        try {
            final Path file = Files.createTempFile("jstests-music", suffix);
            Files.write(file, content);
            file.toFile().deleteOnExit();
            return file;
        } catch (final IOException unexpected) {
            throw new IllegalStateException("a temporary file can be written", unexpected);
        }
    }

    /** What bringing a set of songs said, in order. */
    private static final class Outcome implements MusicImporter.IListener {

        private final List<Boolean> results = new ArrayList<>();
        private final List<Text> messages = new ArrayList<>();
        private boolean done;

        @Override
        public void progress(final int song, final int songs, final long sent, final long total) {
            // A program's bar; only the ends are checked here.
        }

        @Override
        public void finished(final Path file, final boolean ok, final Text message) {
            results.add(ok);
            messages.add(message);
        }

        @Override
        public void done() {
            done = true;
        }
    }
}
