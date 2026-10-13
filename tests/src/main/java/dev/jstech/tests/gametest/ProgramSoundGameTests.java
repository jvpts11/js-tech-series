/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.audio.MachineVoices;
import dev.jstech.computers.audio.MusicImports;
import dev.jstech.computers.audio.MusicPlayer;
import dev.jstech.computers.audio.SoundfoundryTexts;
import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.SpeakerBlockEntity;
import dev.jstech.computers.machine.MachinePrograms;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.sigma.SigmaCompiler;
import dev.jstech.computers.sigma.SourceFile;
import dev.jstech.computers.vm.program.Halt;
import dev.jstech.computers.vm.program.IWorldCall;
import dev.jstech.computers.vm.program.IWorldFunction;
import dev.jstech.computers.vm.program.Values;
import dev.jstech.computers.vm.system.MemberId;
import dev.jstech.core.audio.media.MediaId;
import dev.jstech.core.audio.media.MediaInfo;
import dev.jstech.core.audio.media.MediaSessions;
import dev.jstech.core.audio.media.MediaStore;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestMedia;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * What a computer's programs play: a beep out of the speaker in its case, which cuts the one before it off; a tune
 * through its sound card, each note of a chord on a voice of its own, or from the case one note at a time on a
 * machine with no card; a recording out of its monitors and speakers, or out of one speaker found by its name. A sound
 * that finds no voice free takes those of the sound that started first, and a machine that goes off stops it all.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ProgramSoundGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos SPEAKER = new BlockPos(5, 2, 1);
    /** Long enough for a monitor beside a computer to link to it. */
    private static final int LINKED = 5;
    /** The voices of the sound built into a Standard board. */
    private static final int BOARD_VOICES = 64;
    /** Where a call says what it moved; nothing the machine's sound is asked is priced by its size. */
    private static final IWorldCall UNCOUNTED = bytes -> {
    };
    private static final String TUNE = """
            using System.*;
            using System.IO.*;
            using System.Sound.*;
            namespace Programs;
            class Tune {
                static void Main() {
                    Console.PrintLine("tune " + Sound.Tones("C4+E4+G4:2000"));
                    Sound.Beep(440, 1000);
                    Console.PrintLine("nobody " + (Speaker.Named("nobody") == null));
                }
            }
            """;

    private ProgramSoundGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void program_playsAChordOnTheCardAndBeepsFromTheCase(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = computerWithMonitor(helper);
        final ServerLevel level = helper.getLevel();
        linked(helper, pc)
                .thenExecute(() -> {
                    final SigmaCompiler.Result built =
                            SigmaCompiler.compile(List.of(new SourceFile("Tune.sgs", TUNE)));
                    helper.assertTrue(built.ok(), "the program compiles: " + String.join("\n", built.lines()));
                    final MachinePrograms programs = pc.programs();
                    final MachinePrograms.Started started = programs.start("tune.asm", built.assembly(), 1, pc);
                    helper.assertTrue(started.ok(), "it starts: " + started.message());
                    programs.tick(4096);
                    final List<String> said = programs.byId(started.id()).process().console();
                    helper.assertTrue(said.equals(List.of("tune true", "nobody true")),
                            "the tune plays, and no speaker goes by a name none has; got " + said);
                    helper.assertTrue(pc.voices().held(level, false) == 3,
                            "each note of the chord takes a voice of the board's sound; held "
                                    + pc.voices().held(level, false));
                    helper.assertTrue(pc.voices().held(level, true) == 1, "and the beep the speaker in the case");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void beep_cutsOffTheBeepBeforeIt(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = computerWithMonitor(helper);
        final ServerLevel level = helper.getLevel();
        final boolean[] cut = {false};
        linked(helper, pc)
                .thenExecute(() -> {
                    pc.voices().take(level, true, "earlier beep", 1, Long.MAX_VALUE, stopped -> cut[0] = true);
                    sound(pc, "Beep", 880, 500);
                    helper.assertTrue(cut[0] && !pc.voices().holds(level, "earlier beep"),
                            "the speaker in the case has one voice, and a new beep takes it");
                    helper.assertTrue(pc.voices().held(level, true) == 1, "which the new beep holds");
                    helper.assertTrue(pc.voices().held(level, false) == 0, "and the board's sound is not touched");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void tones_onAMachineWithNoCardComeFromTheCaseOneNoteAtATime(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc =
                TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(COMPUTER);
        final ServerLevel level = helper.getLevel();
        helper.startSequence()
                .thenExecuteAfter(LINKED, () -> {
                    helper.assertTrue(pc.voices().inCase(), "with no monitor the machine only beeps from its case");
                    helper.assertTrue(Boolean.TRUE.equals(sound(pc, "Tones", "C4+E4+G4:2000")), "the tune plays");
                    helper.assertTrue(pc.voices().held(level, true) == 1 && pc.voices().held(level, false) == 0,
                            "one note at a time, out of the case");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void tones_stopTheProgramAtWhatIsNoNote(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = computerWithMonitor(helper);
        linked(helper, pc)
                .thenExecute(() -> {
                    try {
                        sound(pc, "Tones", "C4 H9 E4");
                        helper.fail("a tune holding something that is no note stops the program");
                    } catch (final Halt halt) {
                        helper.assertTrue(halt.reason() == Halt.Reason.OUT_OF_RANGE
                                        && halt.getMessage().contains("H9"),
                                "naming what it could not read; got " + halt.getMessage());
                    }
                    helper.assertTrue(pc.voices().held(helper.getLevel(), false) == 0, "and none of it plays");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void chord_takesTheVoiceOfTheSongThatStartedFirst(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = withSong(helper);
        final ServerLevel level = helper.getLevel();
        final MusicPlayer music = pc.musicPlayer();
        linked(helper, pc)
                .thenExecute(() -> {
                    music.play(level, 0);
                    helper.assertTrue(music.playing(level), "the song plays; trouble " + music.trouble().english());
                    helper.assertTrue(pc.voices().held(level, false) == 1, "a mono song takes one voice");
                    pc.voices().take(level, false, "later sounds", BOARD_VOICES - 2, Long.MAX_VALUE,
                            MachineVoices.LET_RING);
                    helper.assertTrue(Boolean.TRUE.equals(sound(pc, "Tones", "C4+E4:2000")), "the chord plays");
                    helper.assertTrue(!MediaSessions.has(level, music.key())
                                    && music.trouble().equals(SoundfoundryTexts.VOICES_TAKEN.text()),
                            "on the voice of the song that started first, which stops and says why");
                    helper.assertTrue(pc.voices().held(level, false) == BOARD_VOICES
                                    && pc.voices().holds(level, "later sounds"),
                            "the sounds started after the song play on, on every voice there is");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void play_playsASongFromTheDisksAndStopEndsIt(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = withSong(helper);
        final ServerLevel level = helper.getLevel();
        final String path = pc.console().soundfoundry().song(0);
        linked(helper, pc)
                .thenExecute(() -> {
                    helper.assertTrue(Boolean.FALSE.equals(sound(pc, "Play", "Users/Public/Music/none.wav")),
                            "a file that is not there plays nothing");
                    helper.assertTrue(Boolean.TRUE.equals(sound(pc, "Play", path)), "a song on the disk plays");
                    helper.assertTrue(pc.programSounds().playing(level) && pc.voices().held(level, false) == 1,
                            "holding a voice of the board's sound");
                    sound(pc, "Stop");
                    helper.assertTrue(!pc.programSounds().playing(level) && pc.voices().held(level, false) == 0,
                            "and stopping it lets the voice go");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void speaker_isFoundByItsNameAndPlaysASongAlone(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = withSong(helper);
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        world.setBlock(SPEAKER, ComputingModule.SPEAKER.get());
        final SpeakerBlockEntity speaker = world.blockEntity(SPEAKER, SpeakerBlockEntity.class);
        final ServerLevel level = helper.getLevel();
        final String path = pc.console().soundfoundry().song(0);
        linked(helper, pc)
                .thenWaitUntil(() -> helper.assertTrue(pc.speakerCount() == 1, "the speaker links itself"))
                .thenExecute(() -> {
                    speaker.ask(level, "Desk");
                    speaker.takeAskedName();
                    helper.assertTrue(call(pc, "Speaker", "Named", null, "Hall") == null,
                            "no speaker goes by a name none has");
                    final Object desk = call(pc, "Speaker", "Named", null, "Desk");
                    helper.assertTrue(desk instanceof Values.Obj found && "Desk".equals(found.get("Name")),
                            "the one named so is found, with its name; got " + desk);
                    helper.assertTrue(Boolean.TRUE.equals(call(pc, "Speaker", "Play", desk, path)),
                            "and plays a song out of itself");
                    helper.assertTrue(pc.programSounds().playing(level) && pc.voices().held(level, false) == 1,
                            "holding one voice of the board's sound");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void machineOff_stopsEverythingItsProgramsPlay(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = withSong(helper);
        final ServerLevel level = helper.getLevel();
        final String path = pc.console().soundfoundry().song(0);
        linked(helper, pc)
                .thenExecute(() -> {
                    helper.assertTrue(Boolean.TRUE.equals(sound(pc, "Play", path)), "a song plays");
                    helper.assertTrue(Boolean.TRUE.equals(sound(pc, "Tones", "C4+E4:5000")), "and a chord");
                    helper.assertTrue(pc.voices().held(level, false) == 3, "on three voices");
                    pc.setPowered(false);
                })
                .thenWaitUntil(() -> helper.assertTrue(!pc.programSounds().playing(level)
                                && pc.voices().held(level, false) == 0,
                        "a machine switched off plays nothing and holds no voice"))
                .thenSucceed();
    }

    private static PersonalComputerBlockEntity computerWithMonitor(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(COMPUTER);
        world.placeMonitor(MONITOR, Direction.EAST);
        return pc;
    }

    /* A running personal computer with a monitor, and Soundfoundry with a mono song of nine seconds brought to it. */
    private static PersonalComputerBlockEntity withSong(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = computerWithMonitor(helper);
        pc.console().install(Programs.SOUNDFOUNDRY.toString());
        final BlockPos host = helper.absolutePos(COMPUTER);
        final MediaId song = put(TestMedia.wav(9000, "program song at " + host.asLong()));
        MusicImports.keep(helper.getLevel(), host, "", true, "song.wav", song, info(song));
        return pc;
    }

    /* Waits for the monitor to link itself to the computer, which is when its sound has somewhere to play. */
    private static GameTestSequence linked(final GameTestHelper helper, final AbstractComputerBlockEntity computer) {
        return helper.startSequence().thenWaitUntil(() -> helper.assertTrue(computer.playsRecordings(),
                "the monitor links itself and the board's sound plays through it"));
    }

    private static Object sound(final PersonalComputerBlockEntity pc, final String name, final Object... arguments) {
        return call(pc, "Sound", name, null, arguments);
    }

    /** Makes a call on the machine's sound the way a program's line does. */
    private static Object call(final PersonalComputerBlockEntity pc, final String type, final String name,
                               final Object target, final Object... arguments) {
        final List<String> parameters = new ArrayList<>();
        for (final Object argument : arguments) {
            parameters.add(argument instanceof Integer ? "int" : "string");
        }
        final MemberId id = new MemberId(type, name, parameters);
        final IWorldFunction bound = pc.services().bind(id);
        if (bound == null) {
            throw new IllegalStateException(id.describe() + " is not answered by the machine");
        }
        return bound.call(UNCOUNTED, target, arguments.clone(), 1);
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
}
