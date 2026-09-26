/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.audio.SystemSound;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.SpeakerBlockEntity;
import dev.jstech.core.audio.Audio;
import dev.jstech.core.audio.AudioOutput;
import dev.jstech.core.audio.FrequencyResponse;
import dev.jstech.core.audio.StereoSide;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * The speakers: linked to a computer beside its monitor, each plays a side of a stereo recording by where it stands,
 * a Legacy one plays coarser than a Standard one, and a name is unique among one computer's speakers. The system
 * chooses whether its sound comes out of the monitor, the speakers or both, and how loud.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SpeakerGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    /** East of the computer, its screen turned east: whoever sits at it looks east, and their left is north. */
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos NORTH_SPEAKER = new BlockPos(5, 2, 1);
    private static final BlockPos SOUTH_SPEAKER = new BlockPos(5, 2, 3);
    private static final int LINKED = 5;

    private SpeakerGameTests() {
    }

    @GameTest(template = ARENA)
    public static void pairOfSpeakers_playTheSideTheyStandOn(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = computerWithMonitor(helper);
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        world.setBlock(NORTH_SPEAKER, ComputingModule.SPEAKER.get());
        world.setBlock(SOUTH_SPEAKER, ComputingModule.SPEAKER.get());
        helper.startSequence()
                .thenExecuteAfter(LINKED, () -> {
                    helper.assertTrue(pc.speakerCount() == 2, "both speakers link to the computer; got "
                            + pc.speakerCount());
                    final List<AudioOutput> outputs = pc.audioHost().outputs();
                    helper.assertTrue(outputs.contains(AudioOutput.at(centre(helper, MONITOR))),
                            "the monitor still plays both sides; got " + outputs);
                    helper.assertTrue(outputs.contains(new AudioOutput(centre(helper, NORTH_SPEAKER), StereoSide.LEFT,
                            FrequencyResponse.FULL)), "the speaker on the left plays the left; got " + outputs);
                    helper.assertTrue(outputs.contains(new AudioOutput(centre(helper, SOUTH_SPEAKER),
                            StereoSide.RIGHT, FrequencyResponse.FULL)), "the other plays the right; got " + outputs);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void speakerAlone_playsBothSidesAndALegacyOnePlaysCoarser(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = computerWithMonitor(helper);
        TestWorldBuilder.forGameTest(helper).setBlock(NORTH_SPEAKER, ComputingModule.LEGACY_SPEAKER.get());
        helper.startSequence()
                .thenExecuteAfter(LINKED, () -> {
                    final List<AudioOutput> outputs = pc.audioHost().outputs();
                    helper.assertTrue(outputs.contains(new AudioOutput(centre(helper, NORTH_SPEAKER), StereoSide.BOTH,
                                    new FrequencyResponse(22_050, 0, 150, 7_000))),
                            "a speaker alone plays both sides, a Legacy one at 22 kHz and cut; got " + outputs);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void speakerName_isUniqueAmongItsComputersSpeakers(final GameTestHelper helper) {
        computerWithMonitor(helper);
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final SpeakerBlockEntity first = speaker(world, NORTH_SPEAKER, ComputingModule.SPEAKER.get());
        final SpeakerBlockEntity second = speaker(world, SOUTH_SPEAKER, ComputingModule.SPEAKER.get());
        helper.startSequence()
                .thenExecuteAfter(LINKED, () -> {
                    first.ask(helper.getLevel(), "Desk");
                    first.takeAskedName();
                    helper.assertTrue(first.name().equals("Desk"), "the first takes the name; got " + first.name());
                    second.ask(helper.getLevel(), "desk");
                    helper.assertTrue(second.dataAccess().get(SpeakerBlockEntity.DATA_CLASH) == 1,
                            "the second's screen says the name clashes, whatever the case");
                    second.takeAskedName();
                    helper.assertTrue(second.name().isEmpty(),
                            "and closing it keeps the name it had; got " + second.name());
                    second.ask(helper.getLevel(), "Hall");
                    helper.assertTrue(second.dataAccess().get(SpeakerBlockEntity.DATA_CLASH) == 0,
                            "another name does not clash");
                    helper.assertTrue(second.name().isEmpty(), "and is not taken while it is being typed");
                    second.takeAskedName();
                    helper.assertTrue(second.name().equals("Hall"), "but when the screen closes; got "
                            + second.name());
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void systemOutput_choosesTheMonitorOrTheSpeakers(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = computerWithMonitor(helper);
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        world.setBlock(NORTH_SPEAKER, ComputingModule.SPEAKER.get());
        world.setBlock(SOUTH_SPEAKER, ComputingModule.SPEAKER.get());
        helper.startSequence()
                .thenExecuteAfter(LINKED, () -> {
                    final AudioOutput monitor = AudioOutput.at(centre(helper, MONITOR));
                    pc.console().settings().applySetting("output", "monitor");
                    final List<AudioOutput> monitorOnly = pc.audioHost().outputs();
                    helper.assertTrue(monitorOnly.equals(List.of(monitor)),
                            "the monitor alone plays when the system chooses it; got " + monitorOnly);
                    pc.console().settings().applySetting("output", "speakers");
                    final List<AudioOutput> speakersOnly = pc.audioHost().outputs();
                    helper.assertTrue(speakersOnly.size() == 2 && !speakersOnly.contains(monitor),
                            "the two speakers play and the monitor does not; got " + speakersOnly);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void systemOutput_speakersWithNoneLinkedPlayOutOfTheMonitor(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = computerWithMonitor(helper);
        helper.startSequence()
                .thenExecuteAfter(LINKED, () -> {
                    pc.console().settings().applySetting("output", "speakers");
                    final List<AudioOutput> outputs = pc.audioHost().outputs();
                    helper.assertTrue(outputs.equals(List.of(AudioOutput.at(centre(helper, MONITOR)))),
                            "with no speaker to play it, the sound gives way to the monitor; got " + outputs);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void systemVolume_setsHowLoudAndMuteSilences(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = computerWithMonitor(helper);
        helper.startSequence()
                .thenExecuteAfter(LINKED, () -> {
                    pc.console().settings().applySetting("volume", "25");
                    helper.assertTrue(pc.audioHost().audioVolume() == 0.25F,
                            "the system plays at a quarter; got " + pc.audioHost().audioVolume());
                    pc.console().settings().applySetting("mute", "on");
                    helper.assertTrue(pc.audioHost().audioVolume() == 0.0F, "muted, it plays nothing");
                    helper.assertTrue(Audio.cue(helper.getLevel(), pc.audioHost(), SystemSound.STARTUP.cue()) == 0,
                            "and a muted system raises no chime");
                })
                .thenSucceed();
    }

    private static PersonalComputerBlockEntity computerWithMonitor(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(COMPUTER);
        world.placeMonitor(MONITOR, Direction.EAST);
        return pc;
    }

    private static SpeakerBlockEntity speaker(final TestWorldBuilder world, final BlockPos pos, final Block block) {
        world.setBlock(pos, block);
        return world.blockEntity(pos, SpeakerBlockEntity.class);
    }

    private static Vec3 centre(final GameTestHelper helper, final BlockPos pos) {
        return Vec3.atCenterOf(helper.absolutePos(pos));
    }
}
