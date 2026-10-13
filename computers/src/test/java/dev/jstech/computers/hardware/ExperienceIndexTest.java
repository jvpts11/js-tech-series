/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.core.tier.HardwareEra;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExperienceIndexTest {

    @Test
    void of_ratesATransitionMachineAsFrames7DidItsAge() {
        final ExperienceIndex index = ExperienceIndex.of(build(e6600(), List.of(vertex8600Gt()), hdd()), 2048);
        assertEquals(56, index.processor(), "a Core 2 Duo E6600 near 5.6");
        assertEquals(55, index.memory(), "two gigabytes near 5.5");
        assertEquals(39, index.graphics());
        assertEquals(36, index.gaming());
        assertEquals(56, index.disk(), "a spinning disk near 5.6");
        assertEquals(36, index.base(), "the base score is the lowest of the five");
        assertTrue(index.runsEffects(), "a card of its age runs the effects");
    }

    @Test
    void of_givesTheTopOfTheScaleOnlyToTheLatestParts() {
        final ExperienceIndex index = ExperienceIndex.of(build(e6600(), List.of(hd7970()), ssd()), 16384);
        assertEquals(ExperienceIndex.HIGHEST, index.graphics());
        assertEquals(ExperienceIndex.HIGHEST, index.memory());
        assertEquals(74, index.disk());
    }

    @Test
    void of_putsTheWeakestCardsOfTheirAgeUnderTheEffects() {
        final GpuSpec x1300 = new GpuSpec(HardwareEra.TRANSITION, PcieGeneration.PCIE_1_0, 4, 128, 15)
                .on(Microarchitectures.R500, "RV515", 450);
        final GpuSpec gs7300 = new GpuSpec(HardwareEra.TRANSITION, PcieGeneration.PCIE_1_0, 4, 128, 19)
                .on(Microarchitectures.CURIE, "G72", 550);
        assertFalse(ExperienceIndex.of(build(e6600(), List.of(x1300), hdd()), 2048).runsEffects(),
                "a Radeon X1300 runs the basic look");
        assertTrue(ExperienceIndex.of(build(e6600(), List.of(gs7300), hdd()), 2048).runsEffects(),
                "a GeForce 7300 GS just runs the effects");
    }

    @Test
    void of_keepsGraphicsOnTheProcessorUnderTheEffects() {
        final CpuSpec withIgp = e6600().withGraphics(new IntegratedGraphics("Integra Graphics", 80, 1150));
        final ExperienceIndex index = ExperienceIndex.of(build(withIgp, List.of(), hdd()), 2048);
        assertTrue(index.graphics() < ExperienceIndex.EFFECTS_FROM, "graphics on the die run the basic look");
        assertFalse(index.runsEffects());
    }

    @Test
    void of_givesTheLowestScoreToAMachineWithNoGraphicsAtAll() {
        final ExperienceIndex index = ExperienceIndex.of(build(e6600(), List.of(), hdd()), 2048);
        assertEquals(ExperienceIndex.LOWEST, index.graphics());
        assertEquals(ExperienceIndex.LOWEST, index.base());
    }

    @Test
    void of_holdsAStrongCardWithLittleMemoryUnderTheEffects() {
        final GpuSpec starved = new GpuSpec(HardwareEra.STANDARD, PcieGeneration.PCIE_3_0, 2048, 64, 250)
                .on(Microarchitectures.GCN, "Tahiti", 925);
        final ExperienceIndex index = ExperienceIndex.of(build(e6600(), List.of(starved), hdd()), 2048);
        assertTrue(index.graphics() < ExperienceIndex.EFFECTS_FROM, "under 128 MB of video memory, no effects");
    }

    @Test
    void of_givesTheLowestDiskScoreToAMachineWithNoDisk() {
        final ComputerBuild noDisk = new ComputerBuild(board(), List.of(e6600()), List.of(vertex8600Gt()),
                List.of(ram()), new PsuSpec(450, 80));
        assertEquals(ExperienceIndex.LOWEST, ExperienceIndex.of(noDisk, 2048).disk());
    }

    @Test
    void scores_areHeldBetweenTheLowestAndTheHighest() {
        final ExperienceIndex index = new ExperienceIndex(0, 200, -5, 79, 10);
        assertEquals(ExperienceIndex.LOWEST, index.processor());
        assertEquals(ExperienceIndex.HIGHEST, index.memory());
        assertEquals(ExperienceIndex.LOWEST, index.graphics());
    }

    @Test
    void shown_writesAWholeAndATenth() {
        assertEquals("5.6", ExperienceIndex.shown(56));
        assertEquals("1.0", ExperienceIndex.shown(10));
        assertEquals("7.9", ExperienceIndex.shown(79));
    }

    private static ComputerBuild build(final CpuSpec cpu, final List<GpuSpec> gpus, final DiskSpec disk) {
        return new ComputerBuild(board(), List.of(cpu), List.copyOf(gpus), List.of(ram()), new PsuSpec(450, 80),
                List.of(disk));
    }

    private static MotherboardSpec board() {
        return new MotherboardSpec(FormFactor.ATX, HardwareEra.TRANSITION, CpuSocketId.LGA_775, 1,
                Set.of(RamGeneration.DDR2), 4, PcieGeneration.PCIE_1_0, 2, 4);
    }

    private static CpuSpec e6600() {
        return new CpuSpec(HardwareEra.TRANSITION, CpuSocketId.LGA_775, 2, 2400, 65, false)
                .on(Microarchitectures.CENTRO, "Conroe");
    }

    private static GpuSpec vertex8600Gt() {
        return new GpuSpec(HardwareEra.TRANSITION, PcieGeneration.PCIE_1_0, 32, 256, 47)
                .on(Microarchitectures.TESLA, "G84", 1190);
    }

    private static GpuSpec hd7970() {
        return new GpuSpec(HardwareEra.STANDARD, PcieGeneration.PCIE_3_0, 2048, 3072, 250)
                .on(Microarchitectures.GCN, "Tahiti", 925);
    }

    private static RamSpec ram() {
        return new RamSpec(HardwareEra.TRANSITION, RamGeneration.DDR2, 512, 12);
    }

    private static DiskSpec hdd() {
        return new DiskSpec(StorageTier.HDD, HardwareEra.TRANSITION, 128_000, 6);
    }

    private static DiskSpec ssd() {
        return new DiskSpec(StorageTier.SSD, HardwareEra.STANDARD, 128_000, 3);
    }
}
