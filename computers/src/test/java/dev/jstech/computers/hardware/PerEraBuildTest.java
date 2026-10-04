/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.core.tier.HardwareEra;
import dev.jstech.core.tier.IndustrialTier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Representative builds of each shipped hardware era (Vintage, Legacy, Transition, Standard), asserting that the
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
                Set.of(RamGeneration.SIMM), 4, PcieGeneration.PCI, 4, 2);
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
                Set.of(RamGeneration.SIMM, RamGeneration.EDO), 8, PcieGeneration.PCI, 7, 4);
    }

    private static MotherboardSpec vintageSlot1Board() {
        return new MotherboardSpec(FormFactor.AT, HardwareEra.VINTAGE, CpuSocketId.SLOT_1, 1,
                Set.of(RamGeneration.SDRAM), 8, PcieGeneration.AGP_2X, 7, 4);
    }

    private static MotherboardSpec vintageServerBoard() {
        return new MotherboardSpec(FormFactor.EEB, HardwareEra.VINTAGE, CpuSocketId.SOCKET_8, 2,
                Set.of(RamGeneration.SIMM, RamGeneration.EDO), 16, PcieGeneration.PCI, 10, 8);
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
                Set.of(RamGeneration.DDR, RamGeneration.DDR2), 4, PcieGeneration.PCIE_1_0, 4, 4);
    }

    private static CpuSpec legacyCpu() {
        return new CpuSpec(HardwareEra.LEGACY, CpuSocketId.LGA_775, 1, 3600, 115, false);
    }

    private static RamSpec legacyRam() {
        return new RamSpec(HardwareEra.LEGACY, RamGeneration.DDR2, 128, 9);
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
                Set.of(RamGeneration.SDRAM), 4, PcieGeneration.AGP_4X, 4, 4);
    }

    private static MotherboardSpec legacySocket939Board() {
        return new MotherboardSpec(FormFactor.ATX, HardwareEra.LEGACY, CpuSocketId.SOCKET_939, 1,
                Set.of(RamGeneration.DDR), 4, PcieGeneration.PCIE_1_0, 4, 4);
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
                2, Set.of(RamGeneration.DDR), 8, PcieGeneration.PCIE_1_0, 6, 6);
        final CpuSpec servo = new CpuSpec(HardwareEra.LEGACY, CpuSocketId.SOCKET_604, 1, 3200, 92, false);
        assertTrue(new ComputerBuild(board, List.of(servo, servo), List.of(), List.of(ddr()), psu(500)).isPowered());
    }

    @Test
    void legacyMainframeBuild_ddr2Memory_isNotPowered() {
        // The four-way Socket 940 board of the Mainframe took DDR; DDR2 came after the Opteras it was built for.
        final MotherboardSpec board = new MotherboardSpec(FormFactor.MTX, HardwareEra.LEGACY, CpuSocketId.SOCKET_940,
                4, Set.of(RamGeneration.DDR), 24, PcieGeneration.PCIE_1_0, 8, 6);
        final CpuSpec optera = new CpuSpec(HardwareEra.LEGACY, CpuSocketId.SOCKET_940, 1, 2400, 89, false);
        final RamSpec ddr2 = new RamSpec(HardwareEra.LEGACY, RamGeneration.DDR2, 128, 9);
        assertFalse(build(board, optera, ddr2, psu(500)).isPowered());
    }

    /*
     * Transition: the LGA 775 board at the entry, which takes DDR2 and DDR3, the AM3 board in the middle, the LGA 1366
     * workstation board at the top with its PCIe 2.0 card, the two-way LGA 1366 server board, and the Mainframe's
     * four-way Socket F board, still on DDR2.
     */

    private static MotherboardSpec transitionBoard(final CpuSocketId socket, final Set<RamGeneration> ram,
                                                   final int sockets) {
        return new MotherboardSpec(FormFactor.ATX, HardwareEra.TRANSITION, socket, sockets, ram, 8,
                PcieGeneration.PCIE_2_0, 4, 4);
    }

    private static RamSpec ddr3() {
        return new RamSpec(HardwareEra.TRANSITION, RamGeneration.DDR3, 512, 10);
    }

    @Test
    void transitionEntryBuild_takesDdr2AndDdr3() {
        final MotherboardSpec board = transitionBoard(CpuSocketId.LGA_775,
                Set.of(RamGeneration.DDR2, RamGeneration.DDR3), 1);
        final CpuSpec celer = new CpuSpec(HardwareEra.TRANSITION, CpuSocketId.LGA_775, 2, 1600, 65, false);
        final RamSpec ddr2 = new RamSpec(HardwareEra.TRANSITION, RamGeneration.DDR2, 256, 10);
        assertTrue(build(board, celer, ddr2, psu(450)).isPowered());
        assertTrue(build(board, celer, ddr3(), psu(450)).isPowered());
    }

    @Test
    void transitionMiddleBuild_onAm3_isPowered() {
        final MotherboardSpec board = transitionBoard(CpuSocketId.AM3, Set.of(RamGeneration.DDR3), 1);
        final CpuSpec x4 = new CpuSpec(HardwareEra.TRANSITION, CpuSocketId.AM3, 4, 3400, 125, false);
        assertTrue(build(board, x4, ddr3(), psu(450)).isPowered());
    }

    @Test
    void transitionTopBuild_withItsPcie2Card_isPowered() {
        final MotherboardSpec board = transitionBoard(CpuSocketId.LGA_1366, Set.of(RamGeneration.DDR3), 1);
        final CpuSpec c7 = new CpuSpec(HardwareEra.TRANSITION, CpuSocketId.LGA_1366, 6, 3330, 130, false);
        final GpuSpec gtx480 = new GpuSpec(HardwareEra.TRANSITION, PcieGeneration.PCIE_2_0, 480, 1536, 250);
        assertTrue(new ComputerBuild(board, List.of(c7), List.of(gtx480), List.of(ddr3()), psu(650)).isPowered());
    }

    @Test
    void transitionServerBuild_twoServosOnLga1366_isPowered() {
        final MotherboardSpec board = transitionBoard(CpuSocketId.LGA_1366, Set.of(RamGeneration.DDR3), 2);
        final CpuSpec servo = new CpuSpec(HardwareEra.TRANSITION, CpuSocketId.LGA_1366, 6, 3330, 130, false);
        assertTrue(new ComputerBuild(board, List.of(servo, servo), List.of(), List.of(ddr3()), psu(650))
                .isPowered());
    }

    @Test
    void transitionMainframeBuild_ddr3Memory_isNotPowered() {
        // The four-way Socket F board of the Mainframe took DDR2; DDR3 came with the boards after it.
        final MotherboardSpec board = transitionBoard(CpuSocketId.SOCKET_F, Set.of(RamGeneration.DDR2), 4);
        final CpuSpec optera = new CpuSpec(HardwareEra.TRANSITION, CpuSocketId.SOCKET_F, 6, 2600, 75, false);
        assertFalse(build(board, optera, ddr3(), psu(650)).isPowered());
    }

    @Test
    void transitionBuild_takesTheCraftingCardsAndTheLegacyManagementNicByTheirBus() {
        // None of the three is bound to an age: a PCIe 1.0 or 2.0 card sits in the PCIe 2.0 slots of the board.
        final MotherboardSpec board = transitionBoard(CpuSocketId.LGA_1366, Set.of(RamGeneration.DDR3), 1);
        final CpuSpec c7 = new CpuSpec(HardwareEra.TRANSITION, CpuSocketId.LGA_1366, 4, 2660, 130, false);
        final IExpansionCardSpec t2 = new CraftingCardSpec(HardwareEra.TRANSITION, IndustrialTier.T2,
                PcieGeneration.PCIE_1_0, 0.05, 2, 75);
        final IExpansionCardSpec t3 = new CraftingCardSpec(HardwareEra.TRANSITION, IndustrialTier.T3,
                PcieGeneration.PCIE_2_0, 0.1, 4, 100);
        final IExpansionCardSpec nic = new ClusterInterfaceCardSpec(HardwareEra.LEGACY, IndustrialTier.T3,
                PcieGeneration.PCIE_1_0, ClusterInterfaceCardSpec.Reach.SUPERCOMPUTERS, 2, 20);
        assertTrue(new ComputerBuild(board, List.of(c7), List.of(t2, t3, nic), List.of(ddr3()), psu(650))
                .isPowered());
    }

    // Standard

    private static MotherboardSpec standardBoard() {
        return new MotherboardSpec(FormFactor.ATX, HardwareEra.STANDARD, CpuSocketId.LGA_1150, 1,
                Set.of(RamGeneration.DDR3), 4, PcieGeneration.PCIE_3_0, 4, 2);
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

    /*
     * The rest of the Standard: the LGA 1155 entry, the Velocion FX on AM3+, whose board has the PCIe 2.0 its chipset
     * had, the Fuse on FM2+, the Opteras two to a G34 server board, and the Mainframe's four LGA 2011 Servos on their
     * registered memory.
     */

    private static MotherboardSpec standardBoard(final CpuSocketId socket, final PcieGeneration bus,
                                                 final int sockets) {
        return new MotherboardSpec(FormFactor.ATX, HardwareEra.STANDARD, socket, sockets, Set.of(RamGeneration.DDR3),
                16, bus, 4, 2);
    }

    @Test
    void standardEntryBuild_onLga1155_isPowered() {
        final CpuSpec celer = new CpuSpec(HardwareEra.STANDARD, CpuSocketId.LGA_1155, 2, 2400, 65, false);
        final RamSpec ddr3l = new RamSpec(HardwareEra.STANDARD, RamGeneration.DDR3, 1024, 10);
        assertTrue(build(standardBoard(CpuSocketId.LGA_1155, PcieGeneration.PCIE_3_0, 1), celer, ddr3l, psu(450))
                .isPowered());
    }

    @Test
    void standardFxBuild_onAm3Plus_isPoweredWithItsCardHeldToThePcie2Slot() {
        final MotherboardSpec board = standardBoard(CpuSocketId.AM3_PLUS, PcieGeneration.PCIE_2_0, 1);
        final CpuSpec fx = new CpuSpec(HardwareEra.STANDARD, CpuSocketId.AM3_PLUS, 8, 4000, 125, false);
        final GpuSpec card = new GpuSpec(HardwareEra.STANDARD, PcieGeneration.PCIE_3_0, 1664, 4096, 145);
        final ComputerBuild build = new ComputerBuild(board, List.of(fx), List.of(card), List.of(standardRam()),
                psu(650));
        assertTrue(build.isPowered());
        assertTrue(build.hasBandwidthLimitedCard());
    }

    @Test
    void standardAm3PlusBoard_refusesATransitionAm3Processor() {
        final MotherboardSpec board = standardBoard(CpuSocketId.AM3_PLUS, PcieGeneration.PCIE_2_0, 1);
        final CpuSpec x6 = new CpuSpec(HardwareEra.STANDARD, CpuSocketId.AM3, 6, 3300, 125, false);
        assertFalse(build(board, x6, standardRam(), psu(650)).isPowered());
    }

    @Test
    void standardFuseBuild_onFm2Plus_isPowered() {
        final CpuSpec fuse = new CpuSpec(HardwareEra.STANDARD, CpuSocketId.FM2_PLUS, 4, 4100, 100, false);
        assertTrue(build(standardBoard(CpuSocketId.FM2_PLUS, PcieGeneration.PCIE_3_0, 1), fuse, standardRam(),
                psu(450)).isPowered());
    }

    @Test
    void standardServerBuild_twoOpterasOnG34_isPowered() {
        final CpuSpec optera = new CpuSpec(HardwareEra.STANDARD, CpuSocketId.G34, 16, 2500, 115, false);
        assertTrue(new ComputerBuild(standardBoard(CpuSocketId.G34, PcieGeneration.PCIE_2_0, 2),
                List.of(optera, optera), List.of(), List.of(standardRam()), psu(650)).isPowered());
    }

    @Test
    void standardMainframeBuild_fourServosOnRegisteredMemory_isPowered() {
        final CpuSpec servo = new CpuSpec(HardwareEra.STANDARD, CpuSocketId.LGA_2011, 8, 2700, 130, false);
        final RamSpec rdimm = new RamSpec(HardwareEra.STANDARD, RamGeneration.DDR3, 4096, 18);
        assertTrue(new ComputerBuild(standardBoard(CpuSocketId.LGA_2011, PcieGeneration.PCIE_3_0, 4),
                List.of(servo, servo, servo, servo), List.of(), List.of(rdimm, rdimm), psu(850)).isPowered());
    }

    /*
     * Advanced: DDR4 begins here and DDR5 follows it; the LGA 1700 board takes either, a hybrid processor counts its
     * efficiency cores, and the Mainframe's four Epics on SP5 need the server supplies.
     */

    private static MotherboardSpec advancedBoard(final CpuSocketId socket, final Set<RamGeneration> ram,
                                                 final int sockets) {
        return new MotherboardSpec(FormFactor.ATX, HardwareEra.ADVANCED, socket, sockets, ram, 16,
                PcieGeneration.PCIE_5_0, 4, 2);
    }

    private static RamSpec ddr4() {
        return new RamSpec(HardwareEra.ADVANCED, RamGeneration.DDR4, 4096, 13);
    }

    private static RamSpec ddr5() {
        return new RamSpec(HardwareEra.ADVANCED, RamGeneration.DDR5, 8192, 14);
    }

    @Test
    void advancedAm4Build_onDdr4_isPowered() {
        final CpuSpec awayken = new CpuSpec(HardwareEra.ADVANCED, CpuSocketId.AM4, 6, 3600, 65, false);
        assertTrue(build(advancedBoard(CpuSocketId.AM4, Set.of(RamGeneration.DDR4), 1), awayken, ddr4(), psu(650))
                .isPowered());
    }

    @Test
    void advancedAm5Board_refusesDdr4() {
        final CpuSpec awayken = new CpuSpec(HardwareEra.ADVANCED, CpuSocketId.AM5, 8, 4500, 105, false);
        final MotherboardSpec board = advancedBoard(CpuSocketId.AM5, Set.of(RamGeneration.DDR5), 1);
        assertTrue(build(board, awayken, ddr5(), psu(850)).isPowered());
        assertFalse(build(board, awayken, ddr4(), psu(850)).isPowered());
    }

    @Test
    void advancedHybridBuild_onLga1700_takesDdr4OrDdr5() {
        final CpuSpec centro = new CpuSpec(HardwareEra.ADVANCED, CpuSocketId.LGA_1700, 8, 3000, 125, false)
                .on(Microarchitectures.RAPTOR_LAKE, "").withSmt()
                .withEfficiencyCores(16, 2200, Microarchitectures.GRACEMONT);
        final MotherboardSpec board = advancedBoard(CpuSocketId.LGA_1700,
                Set.of(RamGeneration.DDR4, RamGeneration.DDR5), 1);
        assertTrue(build(board, centro, ddr4(), psu(850)).isPowered());
        assertTrue(build(board, centro, ddr5(), psu(850)).isPowered());
    }

    @Test
    void advancedMainframeBuild_fourEpicsOnSp5_isPowered() {
        final CpuSpec epic = new CpuSpec(HardwareEra.ADVANCED, CpuSocketId.SP5, 96, 2400, 360, false);
        final RamSpec rdimm = new RamSpec(HardwareEra.ADVANCED, RamGeneration.DDR5, 32768, 22);
        assertTrue(new ComputerBuild(advancedBoard(CpuSocketId.SP5, Set.of(RamGeneration.DDR5), 4),
                List.of(epic, epic, epic, epic), List.of(), List.of(rdimm, rdimm), new PsuSpec(3000, 94))
                .isPowered());
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
