/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.core.audio.media.MediaId;
import dev.jstech.core.audio.media.MediaInfo;
import dev.jstech.core.audio.media.MediaPlace;
import dev.jstech.core.audio.media.MediaSessions;
import dev.jstech.core.audio.media.MediaStore;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestSounds;
import dev.jstech.tests.testkit.TestMedia;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The recordings a server keeps and plays: kept once under their hash with what they are read from them, and played
 * in the world from a point that runs on, holds while paused, and ends where the recording does.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MediaGameTests {

    private static final String ARENA = "empty";
    private static final String CHIME = "assets/jsc/sounds/os/frames_11/startup.ogg";

    private MediaGameTests() {
    }

    @GameTest(template = ARENA)
    public static void store_keepsARecordingByItsHashAndReadsWhatItIs(final GameTestHelper helper) {
        final MediaStore store = store(helper);
        final byte[] ogg = read(helper, CHIME);
        try {
            final MediaId id = store.put(ogg, "ogg");
            helper.assertTrue(store.has(id) && id.equals(MediaId.of(ogg, "ogg")),
                    "the chime is kept under the hash of its bytes");
            final MediaInfo info = store.info(id);
            helper.assertTrue(info.millis() > 500 && info.channels() == 2 && info.sampleRate() >= 22_050,
                    "and read as the stereo recording it is; got " + info);
        } catch (final IOException unexpected) {
            helper.fail("the game's own Ogg is a recording the store keeps: " + unexpected.getMessage());
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void session_runsOnHoldsWhilePausedAndEndsWithItsRecording(final GameTestHelper helper) {
        final MediaId id = put(helper, TestMedia.wav(1500, "session"));
        final String key = "jstests:song/" + helper.absolutePos(BlockPos.ZERO).asLong();
        final boolean[] ended = {false};
        final long[] held = {0L};
        MediaSessions.play(helper.getLevel(), key, TestSounds.BEEP, id, List.of(place(helper)), 1.0F, 0L,
                () -> ended[0] = true);
        helper.startSequence()
                .thenExecuteAfter(10, () -> {
                    helper.assertTrue(MediaSessions.position(helper.getLevel(), key) > 0,
                            "the point runs on while it plays");
                    held[0] = MediaSessions.pause(helper.getLevel(), key);
                })
                .thenExecuteAfter(10, () -> {
                    helper.assertTrue(MediaSessions.position(helper.getLevel(), key) == held[0]
                                    && MediaSessions.paused(helper.getLevel(), key),
                            "and holds where it was paused");
                    MediaSessions.resume(helper.getLevel(), key);
                })
                .thenWaitUntil(() -> helper.assertTrue(ended[0] && !MediaSessions.has(helper.getLevel(), key),
                        "it ends when its recording does, and says so"))
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void session_startedPartWayCountsFromThere(final GameTestHelper helper) {
        final MediaId id = put(helper, TestMedia.wav(9000, "part way"));
        final String key = "jstests:part-way/" + helper.absolutePos(BlockPos.ZERO).asLong();
        MediaSessions.play(helper.getLevel(), key, TestSounds.BEEP, id, List.of(place(helper)), 1.0F, 5000L, null);
        helper.assertTrue(MediaSessions.position(helper.getLevel(), key) >= 5000L,
                "a song taken up five seconds in is five seconds in");
        MediaSessions.stop(helper.getLevel(), key);
        helper.assertFalse(MediaSessions.has(helper.getLevel(), key), "and stopping it leaves nothing playing");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void session_refusesASoundThatPlaysItsOwnFiles(final GameTestHelper helper) {
        final MediaId id = put(helper, TestMedia.wav(100, "own files"));
        try {
            MediaSessions.play(helper.getLevel(), "jstests:wrong", TestSounds.CLICK, id, List.of(), 1.0F, 0L, null);
            helper.fail("a sound with files of its own cannot be handed a recording");
        } catch (final IllegalArgumentException expected) {
            helper.succeed();
        }
    }

    private static MediaStore store(final GameTestHelper helper) {
        return MediaStore.current().orElseThrow(() -> new IllegalStateException("the server keeps no recordings"));
    }

    private static MediaId put(final GameTestHelper helper, final byte[] content) {
        try {
            return store(helper).put(content, "wav");
        } catch (final IOException unexpected) {
            throw new IllegalStateException("a WAV of silence is a recording", unexpected);
        }
    }

    private static MediaPlace place(final GameTestHelper helper) {
        final BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
        return new MediaPlace(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, null, null);
    }

    private static byte[] read(final GameTestHelper helper, final String path) {
        try {
            final Path file = ModList.get().getModFileById("jsc").getFile().findResource(path);
            return Files.readAllBytes(file);
        } catch (final IOException unreadable) {
            throw new IllegalStateException("the mod's own " + path + " can be read", unreadable);
        }
    }
}
