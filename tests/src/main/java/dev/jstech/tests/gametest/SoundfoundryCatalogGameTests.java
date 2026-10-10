/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.jstech.computers.audio.catalog.CatalogAlbum;
import dev.jstech.computers.audio.catalog.CatalogTrack;
import dev.jstech.computers.audio.catalog.SoundfoundryCatalog;
import dev.jstech.core.audio.media.MediaStore;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestMedia;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;

/**
 * The server's music catalogue: a folder of songs an owner drops in becomes an album named by its notes or by what
 * its songs say, a data pack's album is read the same way, a file that is not a song is passed over, and the songs
 * are kept in the server's store to be sent from there.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SoundfoundryCatalogGameTests {

    private static final String ARENA = "empty";
    /* The album the tests mod carries in its own data. */
    private static final String TONES = "jstests/test_tones";

    private SoundfoundryCatalogGameTests() {
    }

    @GameTest(template = ARENA)
    public static void load_readsAnOwnersFolderAndTheDataPacks(final GameTestHelper helper) {
        final Path folder = folder();
        try {
            readAndCheck(helper, folder);
        } finally {
            delete(folder);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void current_holdsTheCatalogueReadWhenTheServerStarted(final GameTestHelper helper) {
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(album(SoundfoundryCatalog.current(), TONES) != null,
                        "the catalogue read at the start has the data pack's album"))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void command_readsTheCatalogueAgain(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final int ran;
        try {
            ran = server.getCommands().getDispatcher().execute("soundfoundry catalog reload",
                    server.createCommandSourceStack().withSuppressedOutput());
        } catch (final CommandSyntaxException unknown) {
            throw new IllegalStateException("the command is there for an operator", unknown);
        }
        helper.assertTrue(ran == 1, "an operator's reload is taken");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(album(SoundfoundryCatalog.current(), TONES) != null,
                        "and the catalogue read again still has the data pack's album"))
                .thenSucceed();
    }

    private static void readAndCheck(final GameTestHelper helper, final Path folder) {
        final MediaStore store = MediaStore.current().orElseThrow();
        final String salt = Long.toString(System.nanoTime());
        write(folder.resolve("Some Album").resolve("b song.wav"), TestMedia.wav(300, "Second " + salt));
        write(folder.resolve("Some Album").resolve("a song.wav"), TestMedia.wav(300, "First " + salt));
        write(folder.resolve("Some Album").resolve(SoundfoundryCatalog.NOTES),
                "{\"title\": \"Named Album\", \"artist\": \"Someone\", \"year\": \"2001\"}"
                        .getBytes(StandardCharsets.UTF_8));
        write(folder.resolve("Broken").resolve("bad.ogg"), "not a song".getBytes(StandardCharsets.US_ASCII));
        write(folder.resolve("Empty").resolve("readme.txt"), "nothing".getBytes(StandardCharsets.US_ASCII));

        final SoundfoundryCatalog.Snapshot read = SoundfoundryCatalog.load(folder,
                helper.getLevel().getServer().getResourceManager(), store);

        final CatalogAlbum named = album(read, "config/Some Album");
        helper.assertTrue(named != null && named.title().equals("Named Album") && named.artist().equals("Someone")
                        && named.year().equals("2001"),
                "the owner's album is named by its notes; got " + named);
        helper.assertTrue(named.tracks().stream().map(CatalogTrack::title).toList()
                        .equals(List.of("First " + salt, "Second " + salt)),
                "and its songs by what they say about themselves, in the order of their files");
        helper.assertTrue(named.tracks().stream().allMatch(track -> store.has(track.media())),
                "every song is kept in the server's store");
        helper.assertTrue(album(read, "config/Broken") == null && album(read, "config/Empty") == null
                        && read.skipped() >= 1,
                "a folder with no song in it is no album, and a file that is not a song is passed over");

        final CatalogAlbum tones = album(read, TONES);
        helper.assertTrue(tones != null && tones.title().equals("Test Tones")
                        && tones.artist().equals("J's Tech Series")
                        && tones.tracks().stream().map(CatalogTrack::title).toList().equals(List.of("Tone A", "Tone B")),
                "a data pack's album is read the same way; got " + tones);
    }

    @GameTest(template = ARENA)
    public static void load_keepsTheSongsAfterANotesFileThatIsNotUtf8(final GameTestHelper helper) {
        final MediaStore store = MediaStore.current().orElseThrow();
        final Path folder = folder();
        final String salt = Long.toString(System.nanoTime());
        final Path album = folder.resolve("Accents");
        write(album.resolve("a song.wav"), TestMedia.wav(300, "Before " + salt));
        // A Windows-1252 accent: the byte 0xE9 on its own is not valid UTF-8.
        write(album.resolve(SoundfoundryCatalog.NOTES), new byte[]{'{', '"', 't', 'i', 't', 'l', 'e', '"', ':', '"',
                'C', 'a', 'f', (byte) 0xE9, '"', '}'});
        write(album.resolve("z song.wav"), TestMedia.wav(300, "After " + salt));

        final SoundfoundryCatalog.Snapshot read = SoundfoundryCatalog.load(folder,
                helper.getLevel().getServer().getResourceManager(), store);

        final CatalogAlbum found = album(read, "config/Accents");
        helper.assertTrue(found != null && found.tracks().size() == 2,
                "the songs on both sides of the odd notes file are kept; got " + found);
        helper.succeed();
    }

    @Nullable
    private static CatalogAlbum album(final SoundfoundryCatalog.Snapshot read, final String id) {
        return read.albums().stream().filter(album -> album.id().equals(id)).findFirst().orElse(null);
    }

    private static Path folder() {
        try {
            return Files.createTempDirectory("jstests-catalog");
        } catch (final IOException unexpected) {
            throw new IllegalStateException("a temporary folder can be made", unexpected);
        }
    }

    private static void write(final Path file, final byte[] content) {
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, content);
        } catch (final IOException unexpected) {
            throw new IllegalStateException("a temporary file can be written", unexpected);
        }
    }

    private static void delete(final Path root) {
        try (Stream<Path> walk = Files.walk(root)) {
            // Deepest paths first, so a folder is empty by the time it is deleted.
            for (final Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        } catch (final IOException unexpected) {
            throw new IllegalStateException("the temporary folder can be removed", unexpected);
        }
    }
}
