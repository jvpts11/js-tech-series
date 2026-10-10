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
import dev.jstech.core.audio.AudioPrefs;
import dev.jstech.core.audio.media.MediaBalance;
import dev.jstech.core.audio.media.MediaStore;
import dev.jstech.core.audio.media.MediaTexts;
import dev.jstech.core.client.audio.AudioMixer;
import dev.jstech.core.client.audio.AudioPrefsStore;
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
    /** Longer than the second the game keeps a sound it has stopped before forgetting it. */
    private static final int FORGETS_STOPPED = 30;

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
    public static void upload_saysItDidNotGoInWhenItCouldNotBePutToUse(final ClientTestContext ctx) {
        final byte[] wav = TestMedia.wav(500, "unused " + System.nanoTime());
        final MediaId id = MediaId.of(wav, "wav");
        final Object[] first = {null, null};
        final Object[] again = {null, null};
        ctx.thenServer(0, level -> TestMedia.register())
                .then(0, () -> upload(write(wav), first))
                .thenWaitUntil(() -> first[0] != null, WAIT, "the upload to end")
                .thenAssert(0, () -> Boolean.FALSE.equals(first[0])
                                && TestMedia.NOT_USED.equals(((Text) first[1]).english()),
                        "the bytes came, but what they were for was not done, so it did not go in")
                .thenWaitUntilServer(level -> MediaStore.current().map(store -> store.has(id)).orElse(false),
                        WAIT, "the server keeps the bytes all the same", level -> "not kept")
                .then(0, () -> upload(write(wav), again))
                .thenWaitUntil(() -> again[0] != null, WAIT, "the same file brought again to end")
                .thenAssert(0, () -> Boolean.FALSE.equals(again[0])
                                && TestMedia.NOT_USED.equals(((Text) again[1]).english()),
                        "a file the server already keeps is not said to go in either when it could not be used");
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

    /* Brings the file for the test's taker, told to refuse it once it has come; how it ended lands in {@code end}. */
    private static void upload(final Path file, final Object[] end) {
        MediaUploader.upload(file, TestMedia.PURPOSE, TestMedia.REFUSE_ON_ARRIVAL, new MediaUploader.IListener() {
            @Override
            public void progress(final long sent, final long total) {
                // Only the end matters here.
            }

            @Override
            public void finished(final boolean ok, final Text message) {
                end[1] = message;
                end[0] = ok;
            }
        });
    }

    /** A player whose recordings already fill their share of the server is refused a new one before it is sent. */
    @ClientTest(timeoutTicks = 400)
    public static void upload_isRefusedPastThePlayersShareOfTheServer(final ClientTestContext ctx) {
        // Eighty seconds of 16-bit samples at 8 kHz, over a megabyte, so a share of one megabyte cannot take it.
        final byte[] wav = TestMedia.wav(80_000, "share " + System.nanoTime());
        final Object[] end = {null, null};
        // The default share comes back also when the upload never ends.
        ctx.afterTest(() -> ctx.server().submit(() -> MediaBalance.setPlayerQuotaMegabytes(
                MediaBalance.DEFAULT_PLAYER_QUOTA_MEGABYTES)));
        ctx.thenServer(0, level -> {
                    TestMedia.register();
                    MediaBalance.setPlayerQuotaMegabytes(1);
                })
                .then(0, () -> upload(write(wav), end))
                .thenWaitUntil(() -> end[0] != null, WAIT, "the upload to end")
                .thenAssert(0, () -> Boolean.FALSE.equals(end[0]) && end[1] instanceof Text.Translated said
                                && said.key().key().equals(MediaTexts.QUOTA_FULL.key()),
                        "it is refused, and the player is told their share is full");
    }

    /**
     * A recording playing goes quiet with its channel turned all the way down and is heard again when the channel
     * comes back up: the game stops a sound whose volume comes to nothing for good, and a song used to stay silent
     * until the next one started.
     */
    @ClientTest(timeoutTicks = 600)
    public static void session_outlastsItsChannelTurnedAllTheWayDown(final ClientTestContext ctx) {
        final byte[] wav = TestMedia.wav(20_000, "channel " + System.nanoTime());
        final MediaId id = MediaId.of(wav, "wav");
        final String key = "jstests:channel";
        final String channel = TestSounds.BEEP.spec().channel().id().toString();
        final AudioPrefs prefs = AudioPrefsStore.prefs();
        ctx.afterTest(() -> {
            prefs.setVolume(channel, 1.0F);
            AudioMixer.refreshVolumes();
        });
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
                            1.0F, 0L, null);
                })
                .thenWaitUntil(() -> MediaPlayer.heard(key), WAIT, "the recording to be heard")
                // The game forgets a stopped sound only a second after it started, so the test waits past that.
                .then(FORGETS_STOPPED, () -> {
                    prefs.setVolume(channel, 0.0F);
                    AudioMixer.refreshVolumes();
                })
                .thenAssert(FORGETS_STOPPED, () -> MediaPlayer.heard(key),
                        "it keeps playing with its channel all the way down")
                .then(0, () -> {
                    prefs.setVolume(channel, 1.0F);
                    AudioMixer.refreshVolumes();
                })
                .thenAssert(SETTLE, () -> MediaPlayer.heard(key), "and is heard again when the channel comes back")
                .thenServer(0, level -> MediaSessions.stop(level, key))
                .thenWaitUntil(() -> !MediaPlayer.heard(key), WAIT, "until the server stops it");
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
