/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.core.audio.media.MediaId;
import dev.jstech.core.audio.media.MediaPlace;
import dev.jstech.core.audio.media.MediaSessions;
import dev.jstech.core.audio.media.MediaStore;
import dev.jstech.core.client.audio.media.MediaCache;
import dev.jstech.core.client.audio.media.MediaPlayer;
import dev.jstech.core.client.audio.media.MediaUploader;
import dev.jstech.core.text.Text;
import dev.jstech.tests.TestSounds;
import dev.jstech.tests.testkit.TestMedia;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Recordings between a player and the server, the whole way: a file on the player's own computer brought to the
 * server and kept there, and a recording playing in the world fetched by the client that is near it, heard out of the
 * place it plays from, let go when the player walks away and heard again from where it has got to when they come
 * back, and stopped when the server stops it.
 */
public final class MediaClientTests {

    private static final BlockPos STAND = new BlockPos(0, 2, 0);
    /** Well past the test sound's twelve blocks of range and the margin a listener is let go at. */
    private static final BlockPos AWAY = new BlockPos(40, 2, 0);
    private static final int SETTLE = 5;
    private static final int WAIT = 200;

    private MediaClientTests() {
    }

    @ClientTest(timeoutTicks = 400)
    public static void upload_bringsAFileFromThePlayersComputerAndTheServerKeepsIt(final ClientTestContext ctx) {
        final byte[] wav = TestMedia.wav(700, "brought " + System.nanoTime());
        final MediaId id = MediaId.of(wav, "wav");
        final boolean[] finished = {false, false};
        ctx.thenServer(0, level -> TestMedia.register())
                .then(0, () -> {
                    final Path file = write(wav);
                    MediaUploader.upload(file, TestMedia.PURPOSE, "the test's own", new MediaUploader.IListener() {
                        @Override
                        public void progress(final long sent, final long total) {
                            // The bar a program draws; nothing to check here but the end.
                        }

                        @Override
                        public void finished(final boolean ok, final Text message) {
                            finished[0] = true;
                            finished[1] = ok;
                        }
                    });
                })
                .thenWaitUntil(() -> finished[0], WAIT, "the upload to end")
                .thenAssert(0, () -> finished[1], "and to end kept")
                .thenWaitUntilServer(level -> MediaStore.current().map(store -> store.has(id)).orElse(false)
                                && TestMedia.received().stream().anyMatch(r -> r.media().equals(id)
                                && r.context().equals("the test's own") && r.info().millis() == 700),
                        WAIT, "the server keeps it and hands it over with what it is", level -> "not handed over");
    }

    @ClientTest(timeoutTicks = 600)
    public static void session_isHeardNearByLetGoAwayAndStopped(final ClientTestContext ctx) {
        final byte[] wav = TestMedia.wav(20_000, "heard " + System.nanoTime());
        final MediaId id = MediaId.of(wav, "wav");
        final String key = "jstests:heard";
        ctx.thenTeleport(SETTLE, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    try {
                        MediaStore.current().orElseThrow().put(wav, "wav");
                    } catch (final IOException unexpected) {
                        throw new IllegalStateException(unexpected);
                    }
                    final BlockPos at = ctx.abs(STAND);
                    MediaSessions.play(level, key, TestSounds.BEEP, id,
                            List.of(new MediaPlace(at.getX() + 0.5, at.getY() + 1.0, at.getZ() + 2.5, null, null)),
                            1.0F, 2000L, null);
                })
                .thenWaitUntil(() -> MediaPlayer.heard(key), WAIT, "the recording to be fetched and heard")
                .thenAssert(0, () -> MediaCache.has(id), "and kept in the client's cache")
                .thenTeleport(SETTLE, AWAY, Direction.SOUTH)
                .thenWaitUntil(() -> !MediaPlayer.heard(key), WAIT, "walking away lets it go")
                .thenTeleport(SETTLE, STAND, Direction.SOUTH)
                .thenWaitUntil(() -> MediaPlayer.heard(key), WAIT, "coming back hears it again from where it is")
                .thenServer(0, level -> MediaSessions.stop(level, key))
                .thenWaitUntil(() -> !MediaPlayer.heard(key), WAIT, "and the server stopping it stops it");
    }

    private static Path write(final byte[] content) {
        try {
            final Path file = Files.createTempFile("jstests-media", ".wav");
            Files.write(file, content);
            file.toFile().deleteOnExit();
            return file;
        } catch (final IOException unexpected) {
            throw new IllegalStateException("a temporary file can be written", unexpected);
        }
    }
}
