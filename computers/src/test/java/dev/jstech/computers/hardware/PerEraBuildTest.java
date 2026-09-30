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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Representative builds of each shipped hardware era (Vintage, Legacy, Standard), asserting that the
 * validation rules hold across the progression: a coherent era build powers on, a socket mismatch is
 * rejected, and a wrong RAM generation is rejected. An era the catalogue has filled out has a build at its
 * entry, its middle and its top, and its server board. The specs mirror the registered item catalog; the
 * pure-logic layer cannot touch the Minecraft item wrappers, so it works on the records.
 */
class PerEraBuildTest {

    private static PsuSpec psu(final int watts) {
        return new PsuSpec(watts, 90);
    }

    private static ComputerBuild build(final MotherboardSpec board, final CpuSpec cpu,
                                       final RamSpec ram, final PsuSpec psu) {
        return new ComputerBuild(board, List.of(cpu), List.of(), List.of(ram), psu);
    }

    // Vintage

    private static MotherboardSpec vintageBoard() {
        return new MotherboardSpec(FormFactor.BABY_AT, HardwareEra.VINTAGE, CpuSocketId.SOCKET_3, 1,
                Set.of(RamGeneration.SIMM), 4, PcieGeneration.PCI, 4, 2, 2);
    }

    private static CpuSpec vintageCpu() {
        return new CpuSpec(HardwareEra.VINTAGE, CpuSocketId.SOCKET_3, 1, 100, 5, false);
    }

    private static RamSpec vintageRam() {
        return new RamSpec(HardwareEra.VINTAGE, RamGeneration.SIMM, 1, 1);
    }

    @Test
    void vintageBuild_isPowered() {
        assertTrue(build(vintageBoard(), vintageCpu(), vintageRam(), psu(300)).isPowered());
    }

    @Test
    void vintageBuild_wrongSocket_isNotPowered() {
        final CpuSpec wrong = new CpuSpec(HardwareEra.VINTAGE, CpuSocketId.SOCKET_7, 1, 350, 15, false);
        assertFalse(build(vintageBoard(), wrong, vintageRam(), psu(300)).isPowered());
    }

    @Test
    void vintageBuild_wrongRamGeneration_isNotPowered() {
        final RamSpec ddr = new RamSpec(HardwareEra.LEGACY, RamGeneration.DDR, 128, 10);
        assertFalse(build(vintageBoard(), vintageCpu(), ddr, psu(300)).isPowered());
    }

    /*
     * The rest of the Vintage, as the catalogue has it: the Socket 7 board in the middle, the Slot 1 board at the top
     * with its SDRAM and its AGP card, and the two-way Socket 8 server board.
     */

    private static MotherboardSpec vintageSocket7Board() {
        return new MotherboardSpec(FormFactor.AT, HardwareEra.VINTAGE, CpuSocketId.SOCKET_7, 1,
                Set.of(RamGeneration.SIMM, RamGeneration.EDO), 8, PcieGeneration.PCI, 7, 4, 2);
    }

    private static MotherboardSpec vintageSlot1Board() {
        return new MotherboardSpec(FormFactor.AT, HardwareEra.VINTAGE, CpuSocketId.SLOT_1, 1,
                Set.of(RamGeneration.SDRAM), 8, PcieGeneration.AGP_2X, 7, 4, 2);
    }

    private static MotherboardSpec vintageServerBoard() {
        return new MotherboardSpec(FormFactor.EEB, HardwareEra.VINTAGE, CpuSocketId.SOCKET_8, 2,
                Set.of(RamGeneration.SIMM, RamGeneration.EDO), 16, PcieGeneration.PCI, 10, 8, 8);
    }

    private static CpuSpec pentiumIii() {
        return new CpuSpec(HardwareEra.VINTAGE, CpuSocketId.SLOT_1, 1, 600, 35, false);
    }

    private static CpuSpec pentiumPro() {
        return new CpuSpec(HardwareEra.VINTAGE, CpuSocketId.SOCKET_8, 1, 200, 35, false);
    }

    private static RamSpec edo() {
        return new RamSpec(HardwareEra.VINTAGE, RamGeneration.EDO, 8, 3);
    }

    @Test
    void vintageSocket7Build_isPowered() {
        final CpuSpec pentium = new CpuSpec(HardwareEra.VINTAGE, CpuSocketId.SOCKET_7, 1, 133, 11, false);
        assertTrue(build(vintageSocket7Board(), pentium, edo(), psu(300)).isPowered());
    }

    @Test
    void vintageSlot1Build_withItsAgpCard_isPowered() {
        final RamSpec sdram = new RamSpec(HardwareEra.VINTAGE, RamGeneration.SDRAM, 16, 4);
        final GpuSpec tnt = new GpuSpec(HardwareEra.VINTAGE, PcieGeneration.AGP_2X, 2, 16, 15);
        assertTrue(new ComputerBuild(vintageSlot1Board(), List.of(pentiumIii()), List.of(tnt), List.of(sdram),
                psu(300)).isPowered());
    }

    @Test
    void vintageSlot1Build_edoMemory_isNotPowered() {
        // The Slot 1 board takes SDRAM DIMMs only; the SIMMs of the boards before it do not fit.
        assertFalse(build(vintageSlot1Board(), pentiumIii(), edo(), psu(300)).isPowered());
    }

    @Test
    void vintageSlot1Build_pciCard_isNotPowered() {
        // A board has one bus, and the Slot 1 board's is its AGP, so a PCI card finds no slot on it.
        final RamSpec sdram = new RamSpec(HardwareEra.VINTAGE, RamGeneration.SDRAM, 16, 4);
        assertFalse(new ComputerBuild(vintageSlot1Board(), List.of(pentiumIii()),
                List.of(soundCard(HardwareEra.VINTAGE, PcieGeneration.PCI)), List.of(sdram), psu(300)).isPowered());
    }

    @Test
    void vintageServerBuild_twoPentiumPros_isPowered() {
        assertTrue(new ComputerBuild(vintageServerBoard(), List.of(pentiumPro(), pentiumPro()), List.of(),
                List.of(edo()), psu(300)).isPowered());
    }

    @Test
    void vintageServerBuild_socket7Cpu_isNotPowered() {
        final CpuSpec k6 = new CpuSpec(HardwareEra.VINTAGE, CpuSocketId.SOCKET_7, 1, 400, 20, false);
        assertFalse(build(vintageServerBoard(), k6, edo(), psu(300)).isPowered());
    }

    // Legacy

    private static MotherboardSpec legacyBoard() {
        return new MotherboardSpec(FormFactor.ATX, HardwareEra.LEGACY, CpuSocketId.LGA_775, 1,
                Set.of(RamGeneration.DDR, RamGeneration.DDR2), 4, PcieGeneration.PCIE_1_0, 4, 4, 4);
    }

    private static CpuSpec legacyCpu() {
        return new CpuSpec(HardwareEra.LEGACY, CpuSocketId.LGA_775, 2, 2400, 65, false);
    }

    private static RamSpec legacyRam() {
        return new RamSpec(HardwareEra.LEGACY, RamGeneration.DDR2, 512, 12);
    }

    @Test
    void legacyBuild_isPowered() {
        assertTrue(build(legacyBoard(), legacyCpu(), legacyRam(), psu(500)).isPowered());
    }

    @Test
    void legacyBuild_wrongSocket_isNotPowered() {
        final CpuSpec wrong = new CpuSpec(HardwareEra.LEGACY, CpuSocketId.SOCKET_A, 1, 2000, 65, false);
        assertFalse(build(legacyBoard(), wrong, legacyRam(), psu(500)).isPowered());
    }

    @Test
    void legacyBuild_wrongRamGeneration_isNotPowered() {
        final RamSpec ddr3 = new RamSpec(HardwareEra.STANDARD, RamGeneration.DDR3, 2048, 15);
        assertFalse(build(legacyBoard(), legacyCpu(), ddr3, psu(500)).isPowered());
    }

    /*
     * The rest of the Legacy, as the catalogue has it: the Socket 370 board at the entry with SDRAM and AGP, the
     * Socket 939 board at the top with DDR and PCIe, and the server boards on DDR.
     */

    private static MotherboardSpec legacySocket370Board() {
        return new MotherboardSpec(FormFactor.ATX, HardwareEra.LEGACY, CpuSocketId.SOCKET_370, 1,
                Set.of(RamGeneration.SDRAM), 4, PcieGeneration.AGP_4X, 4, 4, 4);
    }

    private static MotherboardSpec legacySocket939Board() {
        return new MotherboardSpec(FormFactor.ATX, HardwareEra.LEGACY, CpuSocketId.SOCKET_939, 1,
                Set.of(RamGeneration.DDR), 4, PcieGeneration.PCIE_1_0, 4, 4, 4);
    }

    private static CpuSpec fx55() {
        return new CpuSpec(HardwareEra.LEGACY, CpuSocketId.SOCKET_939, 1, 2600, 104, false);
    }

    private static RamSpec ddr() {
        return new RamSpec(HardwareEra.LEGACY, RamGeneration.DDR, 256, 12);
    }

    @Test
    void legacySocket370Build_withItsAgpCard_isPowered() {
        final CpuSpec pentiumIii = new CpuSpec(HardwareEra.LEGACY, CpuSocketId.SOCKET_370, 1, 700, 28, false);
        final RamSpec sdram = new RamSpec(HardwareEra.LEGACY, RamGeneration.SDRAM, 32, 5);
        final GpuSpec tnt2 = new GpuSpec(HardwareEra.LEGACY, PcieGeneration.AGP_4X, 2, 32, 10);
        assertTrue(new ComputerBuild(legacySocket370Board(), List.of(pentiumIii), List.of(tnt2), List.of(sdram),
                psu(350)).isPowered());
    }

    @Test
    void legacySocket370Build_ddrMemory_isNotPowered() {
        // The i815 took SDRAM only; DDR came with the boards after it.
        final CpuSpec pentiumIii = new CpuSpec(HardwareEra.LEGACY, CpuSocketId.SOCKET_370, 1, 700, 28, false);
        assertFalse(build(legacySocket370Board(), pentiumIii, ddr(), psu(350)).isPowered());
    }

    @Test
    void legacySocket939Build_withItsPcieCard_isPowered() {
        final GpuSpec x800 = new GpuSpec(HardwareEra.LEGACY, PcieGeneration.PCIE_1_0, 16, 256, 70);
        assertTrue(new ComputerBuild(legacySocket939Board(), List.of(fx55()), List.of(x800), List.of(ddr()),
                psu(500)).isPowered());
    }

    @Test
    void legacySocket939Build_agpCard_isNotPowered() {
        // The Socket 939 board's bus is PCIe; the AGP cards of the boards before it have no slot on it.
        final GpuSpec agp = new GpuSpec(HardwareEra.LEGACY, PcieGeneration.AGP_8X, 16, 256, 81);
        assertFalse(new ComputerBuild(legacySocket939Board(), List.of(fx55()), List.of(agp), List.of(ddr()),
                psu(500)).isPowered());
    }

    @Test
    void legacyServerBuild_twoServosOnSocket604_isPowered() {
        final MotherboardSpec board = new MotherboardSpec(FormFactor.EATX, HardwareEra.LEGACY, CpuSocketId.SOCKET_604,
                2, Set.of(RamGeneration.DDR), 8, PcieGeneration.PCIE_1_0, 6, 6, 4);
        final CpuSpec servo = new CpuSpec(HardwareEra.LEGACY, CpuSocketId.SOCKET_604, 1, 3200, 92, false);
        assertTrue(new ComputerBuild(board, List.of(servo, servo), List.of(), List.of(ddr()), psu(500)).isPowered());
    }

    @Test
    void legacyMainframeBuild_ddr2Memory_isNotPowered() {
        // The four-way Socket 940 board of the Mainframe took DDR; DDR2 came after the Opteras it was built for.
        final MotherboardSpec board = new MotherboardSpec(FormFactor.MTX, HardwareEra.LEGACY, CpuSocketId.SOCKET_940,
                4, Set.of(RamGeneration.DDR), 24, PcieGeneration.PCIE_1_0, 8, 6, 8);
        final CpuSpec optera = new CpuSpec(HardwareEra.LEGACY, CpuSocketId.SOCKET_940, 1, 2400, 89, false);
        final RamSpec ddr2 = new RamSpec(HardwareEra.LEGACY, RamGeneration.DDR2, 512, 12);
        assertFalse(build(board, optera, ddr2, psu(500)).isPowered());
    }

    // Standard

    private static MotherboardSpec standardBoard() {
        return new MotherboardSpec(FormFactor.ATX, HardwareEra.STANDARD, CpuSocketId.LGA_1150, 1,
                Set.of(RamGeneration.DDR3), 4, PcieGeneration.PCIE_3_0, 4, 2, 4);
    }

    private static CpuSpec standardCpu() {
        return new CpuSpec(HardwareEra.STANDARD, CpuSocketId.LGA_1150, 4, 4000, 88, false);
    }

    private static RamSpec standardRam() {
        return new RamSpec(HardwareEra.STANDARD, RamGeneration.DDR3, 2048, 15);
    }

    @Test
    void standardBuild_isPowered() {
        assertTrue(build(standardBoard(), standardCpu(), standardRam(), psu(650)).isPowered());
    }

    @Test
    void standardBuild_wrongSocket_isNotPowered() {
        final CpuSpec wrong = new CpuSpec(HardwareEra.STANDARD, CpuSocketId.AM3, 4, 3400, 125, false);
        assertFalse(build(standardBoard(), wrong, standardRam(), psu(650)).isPowered());
    }

    @Test
    void standardBuild_wrongRamGeneration_isNotPowered() {
        // A Standard board wants DDR3; an older DDR2 module from the Legacy era must not power it.
        final RamSpec ddr2 = new RamSpec(HardwareEra.LEGACY, RamGeneration.DDR2, 512, 12);
        assertFalse(build(standardBoard(), standardCpu(), ddr2, psu(650)).isPowered());
    }

    // Sound cards

    private static SoundCardSpec soundCard(final HardwareEra era, final PcieGeneration bus) {
        return new SoundCardSpec(era, bus, 5, SoundCardSpec.Synthesis.FM, 9, 8, false,
                SoundCardSpec.SampleRate.KHZ_22);
    }

    private static ComputerBuild buildWithCard(final MotherboardSpec board, final CpuSpec cpu, final RamSpec ram,
                                               final IExpansionCardSpec card) {
        return new ComputerBuild(board, List.of(cpu), List.of(card), List.of(ram), psu(650));
    }

    @Test
    void vintageBuild_withItsOwnPciSoundCard_isPowered() {
        assertTrue(buildWithCard(vintageBoard(), vintageCpu(), vintageRam(),
                soundCard(HardwareEra.VINTAGE, PcieGeneration.PCI)).isPowered());
    }

    @Test
    void legacyBuild_withItsOwnPcieSoundCard_isPowered() {
        assertTrue(buildWithCard(legacyBoard(), legacyCpu(), legacyRam(),
                soundCard(HardwareEra.LEGACY, PcieGeneration.PCIE_1_0)).isPowered());
    }

    @Test
    void standardBuild_withALegacySoundCard_isNotPowered() {
        // The bus fits, the era does not: a Standard board has its sound built in and takes no card.
        assertFalse(buildWithCard(standardBoard(), standardCpu(), standardRam(),
                soundCard(HardwareEra.LEGACY, PcieGeneration.PCIE_1_0)).isPowered());
    }
}
