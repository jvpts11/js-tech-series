/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.jstech.core.peripheral.PortKind;
import dev.jstech.core.tier.HardwareEra;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MachinePortsTest {

    @Test
    void devicePorts_growWithTheBoardsEra() {
        assertEquals(2, MachinePorts.devicePorts(HardwareEra.VINTAGE));
        assertEquals(4, MachinePorts.devicePorts(HardwareEra.LEGACY));
        assertEquals(6, MachinePorts.devicePorts(HardwareEra.TRANSITION));
        assertEquals(8, MachinePorts.devicePorts(HardwareEra.STANDARD));
        assertEquals(10, MachinePorts.devicePorts(HardwareEra.ADVANCED));
        assertEquals(10, MachinePorts.devicePorts(HardwareEra.EXA));
    }

    @Test
    void videoOutputs_ofOneCardFollowItsEra() {
        assertEquals(1, MachinePorts.videoOutputs(HardwareEra.VINTAGE));
        assertEquals(2, MachinePorts.videoOutputs(HardwareEra.LEGACY));
        assertEquals(2, MachinePorts.videoOutputs(HardwareEra.TRANSITION));
        assertEquals(4, MachinePorts.videoOutputs(HardwareEra.STANDARD));
        assertEquals(4, MachinePorts.videoOutputs(HardwareEra.ADVANCED));
    }

    @Test
    void of_aMachineWithoutAGraphicsCard_hasNoVideoOutput() {
        final ComputerBuild build = build(board(FormFactor.ATX, HardwareEra.STANDARD), List.of());

        assertEquals(0, MachinePorts.of(build, PortKind.VIDEO));
        assertEquals(8, MachinePorts.of(build, PortKind.DEVICE));
    }

    @Test
    void of_addsEachCardsOutputsByTheCardsOwnEra() {
        // A Legacy card in a Standard machine still has the two outputs it was made with.
        final ComputerBuild build = build(board(FormFactor.ATX, HardwareEra.STANDARD),
                List.of(gpu(HardwareEra.STANDARD), gpu(HardwareEra.LEGACY)));

        assertEquals(6, MachinePorts.of(build, PortKind.VIDEO));
    }

    @Test
    void of_aServerBoard_hasAVideoOutputOfItsOwn() {
        final ComputerBuild bare = build(board(FormFactor.EEB, HardwareEra.STANDARD), List.of());
        final ComputerBuild carded = build(board(FormFactor.EEB, HardwareEra.STANDARD),
                List.of(gpu(HardwareEra.STANDARD)));

        assertEquals(1, MachinePorts.of(bare, PortKind.VIDEO));
        assertEquals(5, MachinePorts.of(carded, PortKind.VIDEO));
    }

    @Test
    void of_audio_comesFromTheSoundCardBeforeTheTransition() {
        final ComputerBuild silent = build(board(FormFactor.ATX, HardwareEra.LEGACY), List.of());
        final ComputerBuild carded = build(board(FormFactor.ATX, HardwareEra.LEGACY),
                List.of(soundCard(HardwareEra.LEGACY)));

        assertEquals(0, MachinePorts.of(silent, PortKind.AUDIO));
        // One output, a pair of speakers.
        assertEquals(MachinePorts.SPEAKERS_PER_AUDIO_OUTPUT, MachinePorts.of(carded, PortKind.AUDIO));
    }

    @Test
    void of_audio_comesFromTheBoardFromTheTransitionOn() {
        final ComputerBuild build = build(board(FormFactor.ATX, HardwareEra.TRANSITION), List.of());

        assertEquals(1, MachinePorts.audioOutputs(build));
        assertEquals(2, MachinePorts.of(build, PortKind.AUDIO));
    }

    private static ComputerBuild build(final MotherboardSpec board, final List<IExpansionCardSpec> cards) {
        return new ComputerBuild(board, List.of(new CpuSpec(board.era(), CpuSocketId.LGA_1150, 4, 3500, 84, false)),
                cards, List.of(new RamSpec(board.era(), RamGeneration.DDR3, 2048, 15)), new PsuSpec(650, 90));
    }

    private static MotherboardSpec board(final FormFactor formFactor, final HardwareEra era) {
        return new MotherboardSpec(formFactor, era, CpuSocketId.LGA_1150, 1, Set.of(RamGeneration.DDR3), 4,
                PcieGeneration.PCIE_3_0, 4, 2);
    }

    private static GpuSpec gpu(final HardwareEra era) {
        return new GpuSpec(era, PcieGeneration.PCIE_3_0, 2048, 3072, 250);
    }

    private static SoundCardSpec soundCard(final HardwareEra era) {
        return new SoundCardSpec(era, PcieGeneration.PCI, 5, SoundCardSpec.Synthesis.FM, 9, 8, false,
                SoundCardSpec.SampleRate.KHZ_22);
    }
}
