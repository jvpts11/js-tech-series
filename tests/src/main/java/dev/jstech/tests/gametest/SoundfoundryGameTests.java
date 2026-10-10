/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.audio.MusicImports;
import dev.jstech.computers.audio.MusicPlayer;
import dev.jstech.computers.audio.SoundfoundryTexts;
import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.operation.payload.SoundfoundryStatePayload;
import dev.jstech.computers.operation.payload.music.SoundfoundryPayloads;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.SystemLayout;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.program.SoundfoundryState;
import dev.jstech.core.audio.media.MediaId;
import dev.jstech.core.audio.media.MediaInfo;
import dev.jstech.core.audio.media.MediaSessions;
import dev.jstech.core.audio.media.MediaStore;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestMedia;
import dev.jstech.tests.testkit.TestWorldBuilder;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Soundfoundry on a machine, from the server's side: songs brought to it join its playlist, one plays out of the
 * machine's own sound and the next follows it when it ends, it pauses, jumps and stops, it stops when the machine
 * goes off, and a song it cannot play is refused with the reason.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SoundfoundryGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos COMPUTER = new BlockPos(2, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(3, 2, 2);

    private SoundfoundryGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void play_goesOnToTheNextSongWhenOneEnds(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = withSongs(helper, 300, 300);
        final ServerLevel level = helper.getLevel();
        final MusicPlayer music = pc.musicPlayer();
        linked(helper, pc)
                .thenExecute(() -> {
                    music.play(level, 0);
                    helper.assertTrue(music.playing(level) && MediaSessions.has(level, music.key()),
                            "the first song plays out of the machine; trouble " + music.trouble().english());
                })
                .thenWaitUntil(() -> helper.assertTrue(state(pc).current() == 1 && music.playing(level),
                        "the second follows when it ends"))
                .thenWaitUntil(() -> helper.assertTrue(!MediaSessions.has(level, music.key())
                                && state(pc).current() == 1,
                        "and after the last, without repeat, it is quiet on the last song"))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void pause_holdsTheSongAndSeekJumpsInIt(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = withSongs(helper, 9000);
        final ServerLevel level = helper.getLevel();
        final MusicPlayer music = pc.musicPlayer();
        linked(helper, pc)
                .thenExecute(() -> {
                    music.play(level, 0);
                    music.pause(level);
                    helper.assertTrue(music.paused(level), "a pause holds it");
                    music.seek(level, 5000L);
                    helper.assertTrue(music.paused(level) && music.position(level) >= 5000L,
                            "a jump lands where it was asked and stays paused; at " + music.position(level));
                    music.pause(level);
                    helper.assertTrue(music.playing(level), "and it plays on again");
                    music.stop(level);
                    helper.assertTrue(!MediaSessions.has(level, music.key()) && state(pc).current() == 0,
                            "stopping it keeps the song it was on");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void tick_stopsTheMusicWhenTheMachineGoesOff(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = withSongs(helper, 9000);
        final ServerLevel level = helper.getLevel();
        linked(helper, pc)
                .thenExecute(() -> {
                    pc.musicPlayer().play(level, 0);
                    helper.assertTrue(pc.musicPlayer().playing(level), "it plays");
                    pc.setPowered(false);
                })
                .thenWaitUntil(() -> helper.assertTrue(!MediaSessions.has(level, pc.musicPlayer().key()),
                        "a machine switched off plays nothing"))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void play_refusesASongThatIsGone(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = withSongs(helper, 300);
        final ServerLevel level = helper.getLevel();
        final String path = state(pc).song(0);
        linked(helper, pc)
                .thenExecute(() -> {
                    DiskFilesystem.delete(pc.systemDisk(), path);
                    pc.musicPlayer().play(level, 0);
                    helper.assertTrue(!pc.musicPlayer().playing(level)
                                    && pc.musicPlayer().trouble().equals(SoundfoundryTexts.MISSING.with(
                                    path.substring(path.lastIndexOf('/') + 1))),
                            "a song whose file is gone is not played, and says why; said "
                                    + pc.musicPlayer().trouble().english());
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void play_refusesOnAMachineWhoseSoundOnlyBeeps(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final CraftingComputerBlockEntity legacy = world.placeRunningLegacyCraftingComputer(COMPUTER);
        // A monitor is there to play through: what is missing is a sound card for it to play.
        world.placeMonitor(MONITOR, Direction.EAST);
        legacy.console().install(Programs.SOUNDFOUNDRY.toString());
        final MediaId song = put(TestMedia.wav(300, "beeps " + helper.absolutePos(COMPUTER).asLong()));
        MusicImports.keep(helper.getLevel(), helper.absolutePos(COMPUTER), "", true, "song.wav", song, info(song));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(legacy.monitorsPlay(), "the monitor links itself"))
                .thenExecute(() -> {
                    legacy.musicPlayer().play(helper.getLevel(), 0);
                    helper.assertTrue(!legacy.musicPlayer().playing(helper.getLevel())
                                    && legacy.musicPlayer().trouble().equals(SoundfoundryTexts.NO_DEVICE.text()),
                            "a machine with no sound card has nothing to play a song on");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void state_travelsWithThePlaylistOnlyWhenItChanged(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = withSongs(helper, 300, 300);
        final ServerLevel level = helper.getLevel();
        final SoundfoundryStatePayload fresh = SoundfoundryPayloads.stateOf(level, pc, -1);
        helper.assertTrue(fresh.songs().map(List::size).orElse(0) == 2
                        && fresh.songs().get().getFirst().present(),
                "a window with no list is sent the list");
        final SoundfoundryStatePayload same = SoundfoundryPayloads.stateOf(level, pc, fresh.revision());
        helper.assertTrue(same.songs().isEmpty(), "and one holding this revision is not sent it again");
        final RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        SoundfoundryStatePayload.STREAM_CODEC.encode(buf, fresh);
        final SoundfoundryStatePayload read = SoundfoundryStatePayload.STREAM_CODEC.decode(buf);
        helper.assertTrue(read.songs().equals(fresh.songs()) && read.revision() == fresh.revision()
                        && read.device() == fresh.device() && buf.readableBytes() == 0,
                "and it arrives as it was sent");
        helper.succeed();
    }

    /*
     * A running personal computer with a monitor, whose sound comes out of it, and Soundfoundry, with songs of those
     * lengths brought to it and put on the list.
     */
    private static PersonalComputerBlockEntity withSongs(final GameTestHelper helper, final int... millis) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(COMPUTER);
        world.placeMonitor(MONITOR, Direction.EAST);
        pc.console().install(Programs.SOUNDFOUNDRY.toString());
        final BlockPos host = helper.absolutePos(COMPUTER);
        for (int i = 0; i < millis.length; i++) {
            final MediaId song = put(TestMedia.wav(millis[i], "song " + i + " at " + host.asLong()));
            MusicImports.keep(helper.getLevel(), host, "", true, "song " + i + ".wav", song, info(song));
        }
        helper.assertTrue(state(pc).size() == millis.length && state(pc).song(0)
                        .startsWith(SystemLayout.MUSIC_DIR + "/"),
                "every song brought went on the list, from the music folder");
        return pc;
    }

    /* Waits for the monitor to link itself to the computer, which is when its sound has somewhere to play. */
    private static GameTestSequence linked(final GameTestHelper helper, final AbstractComputerBlockEntity computer) {
        return helper.startSequence().thenWaitUntil(() -> helper.assertTrue(computer.playsRecordings(),
                "the monitor links itself and the board's sound plays through it"));
    }

    private static SoundfoundryState state(final AbstractComputerBlockEntity computer) {
        return computer.console().soundfoundry();
    }

    private static MediaId put(final byte[] wav) {
        try {
            return MediaStore.current().orElseThrow().put(wav, "wav");
        } catch (final IOException unexpected) {
            throw new IllegalStateException("the media store could not keep the WAV", unexpected);
        }
    }

    private static MediaInfo info(final MediaId media) {
        try {
            return MediaStore.current().orElseThrow().info(media);
        } catch (final IOException unexpected) {
            throw new IllegalStateException("the media store could not read " + media, unexpected);
        }
    }
}
