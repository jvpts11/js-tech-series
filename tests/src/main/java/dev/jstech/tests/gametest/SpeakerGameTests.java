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
import dev.jstech.computers.block.SpeakerBlock;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.SpeakerBlockEntity;
import dev.jstech.core.audio.Audio;
import dev.jstech.core.audio.AudioOutput;
import dev.jstech.core.audio.FrequencyResponse;
import dev.jstech.core.audio.StereoSide;
import dev.jstech.core.tier.HardwareEra;
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
 * a Legacy one plays coarser than a Standard one, a Transition satellite without the bass until a subwoofer stands
 * against one of its set, and a name is unique among one computer's speakers. The system chooses whether its sound
 * comes out of the monitor, the speakers or both, and how loud.
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
    /** Against the south speaker, and nothing else of the desk. */
    private static final BlockPos SOUTH_SUBWOOFER = new BlockPos(5, 2, 4);
    /** Against the north speaker, and nothing else of the desk. */
    private static final BlockPos NORTH_SUBWOOFER = new BlockPos(5, 2, 0);
    /** What a Transition satellite plays with no subwoofer: everything but the bass. */
    private static final FrequencyResponse SATELLITE = new FrequencyResponse(0, 0, 150, 0);
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

    /** A speaker's power light is on while its computer runs, off when it stops, and off on one with no computer. */
    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void powerLight_followsItsComputerRunning(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = computerWithMonitor(helper);
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        world.setBlock(NORTH_SPEAKER, ComputingModule.SPEAKER.get());
        world.setBlock(new BlockPos(1, 2, 1), ComputingModule.ADVANCED_SPEAKER.get());
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(helper.getBlockState(NORTH_SPEAKER).getValue(SpeakerBlock.LIT),
                        "the speaker's light comes on with its computer running"))
                .thenExecute(() -> helper.assertTrue(
                        !helper.getBlockState(new BlockPos(1, 2, 1)).getValue(SpeakerBlock.LIT),
                        "a speaker linked to no computer stays dark"))
                .thenExecute(pc::togglePower)
                .thenWaitUntil(() -> helper.assertTrue(!helper.getBlockState(NORTH_SPEAKER).getValue(SpeakerBlock.LIT),
                        "and goes out when the computer stops"))
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
    public static void transitionSatellites_playTheBassOnlyWithASubwooferAgainstOne(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = computerWithMonitor(helper);
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final SpeakerBlockEntity north = speaker(world, NORTH_SPEAKER, ComputingModule.TRANSITION_SPEAKER.get());
        speaker(world, SOUTH_SPEAKER, ComputingModule.TRANSITION_SPEAKER.get());
        helper.startSequence()
                .thenExecuteAfter(LINKED, () -> {
                    final List<AudioOutput> alone = pc.audioHost().outputs();
                    helper.assertTrue(alone.contains(new AudioOutput(centre(helper, NORTH_SPEAKER), StereoSide.LEFT,
                                    SATELLITE)) && alone.contains(new AudioOutput(centre(helper, SOUTH_SPEAKER),
                                    StereoSide.RIGHT, SATELLITE)),
                            "two satellites alone play a side each, without the bass; got " + alone);
                    helper.assertTrue(!north.subwoofer(), "and their screens say there is no subwoofer");
                    world.setBlock(SOUTH_SUBWOOFER, ComputingModule.TRANSITION_SUBWOOFER.get());
                    final List<AudioOutput> withSubwoofer = pc.audioHost().outputs();
                    helper.assertTrue(withSubwoofer.contains(new AudioOutput(centre(helper, NORTH_SPEAKER),
                                    StereoSide.LEFT, FrequencyResponse.FULL))
                                    && withSubwoofer.contains(new AudioOutput(centre(helper, SOUTH_SPEAKER),
                                    StereoSide.RIGHT, FrequencyResponse.FULL)),
                            "a subwoofer against one of them gives both the whole range; got " + withSubwoofer);
                    helper.assertTrue(north.subwoofer(), "and the screen of the one it does not touch says so too");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void subwoofer_leavesASpeakerOfAnotherEraAsItIs(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = computerWithMonitor(helper);
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        speaker(world, NORTH_SPEAKER, ComputingModule.LEGACY_SPEAKER.get());
        world.setBlock(NORTH_SUBWOOFER, ComputingModule.TRANSITION_SUBWOOFER.get());
        helper.startSequence()
                .thenExecuteAfter(LINKED, () -> {
                    final List<AudioOutput> outputs = pc.audioHost().outputs();
                    helper.assertTrue(outputs.contains(new AudioOutput(centre(helper, NORTH_SPEAKER), StereoSide.BOTH,
                                    new FrequencyResponse(22_050, 0, 150, 7_000))),
                            "a Legacy speaker plays as coarse beside a Transition subwoofer; got " + outputs);
                    helper.assertTrue(!pc.hasSubwoofer(), "which serves only the satellites of its own set");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void speakers_eachEraHasItsModel(final GameTestHelper helper) {
        final List<SpeakerBlock> speakers = List.of(ComputingModule.LEGACY_SPEAKER.get(),
                ComputingModule.TRANSITION_SPEAKER.get(), ComputingModule.SPEAKER.get(),
                ComputingModule.ADVANCED_SPEAKER.get());
        final List<HardwareEra> eras = speakers.stream().map(SpeakerBlock::era).toList();
        helper.assertTrue(eras.equals(List.of(HardwareEra.LEGACY, HardwareEra.TRANSITION, HardwareEra.STANDARD,
                HardwareEra.ADVANCED)), "the ToneWorks, the Inspira 2.1, the WattWorks T20 and the Cobble, an era"
                + " each; got " + eras);
        helper.assertTrue(ComputingModule.ADVANCED_SPEAKER.get().response(false).full()
                        && ComputingModule.SPEAKER.get().response(false).full(),
                "the Standard and the Advanced pairs play the whole range");
        helper.succeed();
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
                    helper.assertTrue(second.nameClashes(),
                            "the second's screen says the name clashes, whatever the case");
                    second.takeAskedName();
                    helper.assertTrue(second.name().isEmpty(),
                            "and closing it keeps the name it had; got " + second.name());
                    second.ask(helper.getLevel(), "Hall");
                    helper.assertTrue(!second.nameClashes(), "another name does not clash");
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

    @GameTest(template = ARENA)
    public static void speakerScreen_saysOffWhileTheSystemPlaysOnlyItsMonitor(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = computerWithMonitor(helper);
        final SpeakerBlockEntity speaker = speaker(TestWorldBuilder.forGameTest(helper), NORTH_SPEAKER,
                ComputingModule.SPEAKER.get());
        helper.startSequence()
                .thenExecuteAfter(LINKED, () -> {
                    helper.assertTrue(speaker.channel() == SpeakerBlockEntity.CHANNEL_ALONE,
                            "a speaker alone plays both sides");
                    pc.console().settings().applySetting("output", "monitor");
                    helper.assertTrue(speaker.channel() == SpeakerBlockEntity.CHANNEL_OFF,
                            "and its screen says it is off while the system plays only out of the monitor");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void soundSettings_nameTheHardwareAndTheSpeakers(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = computerWithMonitor(helper);
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final SpeakerBlockEntity named = speaker(world, NORTH_SPEAKER, ComputingModule.SPEAKER.get());
        speaker(world, SOUTH_SPEAKER, ComputingModule.SPEAKER.get());
        helper.startSequence()
                .thenExecuteAfter(LINKED, () -> {
                    named.ask(helper.getLevel(), "Desk left");
                    named.takeAskedName();
                    helper.assertTrue(pc.soundHardwareLabel().english().equals("On-board audio"),
                            "a Standard board plays through its own sound; got " + pc.soundHardwareLabel().english());
                    helper.assertTrue(pc.playsRecordings(), "which plays recordings, with a monitor to play them");
                    helper.assertTrue(pc.linkedSpeakers().size() == 2
                                    && pc.linkedSpeakers().getFirst().name().equals("Desk left"),
                            "the two speakers are listed with their names, in a fixed order");
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
