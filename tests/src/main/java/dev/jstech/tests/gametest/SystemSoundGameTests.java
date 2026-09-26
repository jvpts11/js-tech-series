/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.audio.ComputingAudioDevices;
import dev.jstech.computers.audio.ComputingSounds;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.core.audio.IAudioHost;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;

/**
 * Where a computer's system sound comes from: the sound built into a Standard board or a Legacy machine's sound
 * card, out of the monitor linked to it; and, with no monitor or no sound hardware, only the speaker in the case,
 * which plays no recording.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SystemSoundGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    /** Long enough for a monitor beside a computer to link to it. */
    private static final int LINKED = 5;

    private SystemSoundGameTests() {
    }

    @GameTest(template = ARENA)
    public static void standardPc_withAMonitor_playsItsBoardsSoundOutOfTheMonitor(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(COMPUTER);
        world.placeMonitor(MONITOR, Direction.EAST);
        helper.startSequence()
                .thenExecuteAfter(LINKED, () -> {
                    final IAudioHost host = pc.audioHost();
                    helper.assertTrue(host.audioDevice() == ComputingAudioDevices.ON_BOARD,
                            "a Standard board plays through its own sound; got " + host.audioDevice().id());
                    helper.assertTrue(host.audioOutputs().equals(List.of(Vec3.atCenterOf(helper.absolutePos(MONITOR)))),
                            "the sound comes out of the monitor; got " + host.audioOutputs());
                    helper.assertTrue("jsc:frames_11".equals(host.soundContext().get(ComputingSounds.SYSTEM)),
                            "the chime is picked by the system it runs; got " + host.soundContext());
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void standardPc_withoutAMonitor_onlyBeepsFromItsCase(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc =
                TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(COMPUTER);
        helper.startSequence()
                .thenExecuteAfter(LINKED, () -> {
                    final IAudioHost host = pc.audioHost();
                    helper.assertTrue(host.audioDevice() == ComputingAudioDevices.PC_SPEAKER,
                            "with no monitor the case only beeps; got " + host.audioDevice().id());
                    helper.assertFalse(host.audioDevice().samples(), "the case's speaker plays no recording");
                    final Vec3 caseCentre = Vec3.atCenterOf(helper.absolutePos(COMPUTER));
                    helper.assertTrue(host.audioOutputs().equals(List.of(caseCentre)),
                            "and it comes out of the case; got " + host.audioOutputs());
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void legacyPc_playsThroughItsSoundCardAndOnlyBeepsWithout(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final PersonalComputerBlockEntity pc = legacy(world);
        world.placeMonitor(MONITOR, Direction.EAST);
        helper.startSequence()
                .thenExecuteAfter(LINKED, () -> {
                    helper.assertTrue(pc.audioHost().audioDevice() == ComputingAudioDevices.PC_SPEAKER,
                            "a Legacy machine with no sound card only beeps; got " + pc.audioHost().audioDevice().id());
                    pc.getHardware().setStackInSlot(PersonalComputerBlockEntity.GPU_SLOTS_START,
                            new ItemStack(HardwareItems.SOUND_CARD_TONE_BLASTER_HI_FI.get()));
                })
                .thenExecuteAfter(1, () -> helper.assertTrue(
                        pc.audioHost().audioDevice() == ComputingAudioDevices.WAVETABLE_CARD,
                        "with its sound card it plays through the card; got " + pc.audioHost().audioDevice().id()))
                .thenSucceed();
    }

    private static PersonalComputerBlockEntity legacy(final TestWorldBuilder world) {
        world.setBlock(COMPUTER, ComputingModule.LEGACY_PERSONAL_COMPUTER.get());
        final PersonalComputerBlockEntity pc = world.blockEntity(COMPUTER, PersonalComputerBlockEntity.class);
        final ItemStackHandler hardware = pc.getHardware();
        hardware.setStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(HardwareItems.MOTHERBOARD_ATX_LEGACY_LGA775.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.CPU_SLOT,
                new ItemStack(HardwareItems.CPU_INTEGRA_DUO_E4300.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.RAM_SLOTS_START,
                new ItemStack(HardwareItems.RAM_DDR2_2048.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT, new ItemStack(HardwareItems.PSU_500B.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
        return pc;
    }
}
