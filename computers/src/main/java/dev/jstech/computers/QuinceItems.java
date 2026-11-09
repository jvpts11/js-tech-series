/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers;

import dev.jstech.computers.hardware.CoolerSpec;
import dev.jstech.computers.hardware.CpuSocketId;
import dev.jstech.computers.hardware.CpuSpec;
import dev.jstech.computers.hardware.FormFactor;
import dev.jstech.computers.hardware.IntegratedGraphics;
import dev.jstech.computers.hardware.Isas;
import dev.jstech.computers.hardware.Microarchitecture;
import dev.jstech.computers.hardware.Microarchitectures;
import dev.jstech.computers.hardware.MotherboardSpec;
import dev.jstech.computers.hardware.PcieGeneration;
import dev.jstech.computers.hardware.QuinceBoards;
import dev.jstech.computers.hardware.RamGeneration;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import java.util.Set;
import net.minecraft.world.item.Item;

/**
 * Quince's sixteen logic boards (computers/docs/design/quince.md), each for one machine alone and only with the
 * processors of its era: Quince's own on the Vintage, the Legacy and the Advanced, the Integra ones in the Transition
 * and the Standard. The memory, the disks and the cards are the common ones.
 *
 * <p>The sockets follow the machines Quince's are drawn after: the 68000's long package in the Russet 128K, the G3 and
 * the G4 module in the Legacy ones, the Integra workstation and server sockets of 2006 and of the 2010s, Quince's own
 * chips on the Advanced. Beside the boards, the processors that go in no other socket, of the 68k, the PowerPC and the
 * ARM64, and the heat sinks of the two Russet Pros.
 */
public final class QuinceItems {

    /* The memory in the package of each of Quince's own chips, in buffer items: 8, 16 and 24 GB. */
    private static final long Q1_MEMORY = 2048L;
    private static final long Q2_MEMORY = 4096L;
    private static final long Q3_MEMORY = 6144L;
    /* The efficiency cores beside the performance ones on each of Quince's own chips. */
    private static final int Q_EFFICIENCY_CORES = 4;
    /* What the heat sink on the PowerQ G4's module takes away, as the Legacy's stock cooler does; an estimate. */
    private static final int G4_HEAT_SINK_WATTS = 60;

    public static final ItemEntry<Item> BOARD_RUSSET_128K = PartItems.board("quince_board_russet_128k",
            new MotherboardSpec(QuinceBoards.RUSSET_128K, HardwareEra.VINTAGE, CpuSocketId.DIP_64, 1,
                    Set.of(RamGeneration.SIMM), 2, PcieGeneration.ISA, 0, 1))
            .named("Quince Logic Board (Russet 128K)").register();
    public static final ItemEntry<Item> BOARD_QRUSSET_G3 = PartItems.board("quince_board_qrusset_g3",
            new MotherboardSpec(QuinceBoards.QRUSSET_G3, HardwareEra.LEGACY, CpuSocketId.POWERQ_G3, 1,
                    Set.of(RamGeneration.SDRAM), 2, PcieGeneration.PCI, 0, 1))
            .named("Quince Logic Board (qRusset G3)").register();
    public static final ItemEntry<Item> BOARD_POWER_G4 = PartItems.board("quince_board_power_g4",
            new MotherboardSpec(QuinceBoards.RUSSET_POWER_G4, HardwareEra.LEGACY, CpuSocketId.POWERQ_G4, 1,
                    Set.of(RamGeneration.SDRAM), 3, PcieGeneration.AGP_8X, 3, 2, PcieGeneration.PCI))
            .named("Quince Logic Board (Russet Power G4)").register();
    public static final ItemEntry<Item> BOARD_QRUSSET_WHITE = PartItems.board("quince_board_qrusset_white",
            spec(QuinceBoards.QRUSSET_TRANSITION, HardwareEra.TRANSITION, CpuSocketId.LGA_775, 1, RamGeneration.DDR2,
                    2, PcieGeneration.PCIE_1_0, 0, 1))
            .named("Quince Logic Board (qRusset, Transition)").register();
    public static final ItemEntry<Item> BOARD_PRO_2006 = PartItems.board("quince_board_pro_2006",
            spec(QuinceBoards.RUSSET_PRO_TRANSITION, HardwareEra.TRANSITION, CpuSocketId.LGA_771, 1,
                    RamGeneration.DDR2, 4, PcieGeneration.PCIE_1_0, 3, 2))
            .named("Quince Logic Board (Russet Pro, Transition)").register();
    public static final ItemEntry<Item> BOARD_QRUSSET_THIN = PartItems.board("quince_board_qrusset_thin",
            spec(QuinceBoards.QRUSSET_STANDARD, HardwareEra.STANDARD, CpuSocketId.LGA_1155, 1, RamGeneration.DDR3,
                    2, PcieGeneration.PCIE_3_0, 0, 1))
            .named("Quince Logic Board (qRusset, Standard)").register();
    public static final ItemEntry<Item> BOARD_PRO_2019 = PartItems.board("quince_board_pro_2019",
            spec(QuinceBoards.RUSSET_PRO_STANDARD, HardwareEra.STANDARD, CpuSocketId.LGA_2011, 1,
                    RamGeneration.DDR3, 4, PcieGeneration.PCIE_3_0, 3, 2))
            .named("Quince Logic Board (Russet Pro, Standard)").register();
    public static final ItemEntry<Item> BOARD_QRUSSET_COLOUR = PartItems.board("quince_board_qrusset_colour",
            spec(QuinceBoards.QRUSSET_ADVANCED, HardwareEra.ADVANCED, CpuSocketId.QUINCE_Q, 1, RamGeneration.DDR5,
                    2, PcieGeneration.PCIE_4_0, 0, 1))
            .named("Quince Logic Board (qRusset, Advanced)").register();
    public static final ItemEntry<Item> BOARD_RUSSET_STUDIO = PartItems.board("quince_board_russet_studio",
            spec(QuinceBoards.RUSSET_STUDIO, HardwareEra.ADVANCED, CpuSocketId.QUINCE_Q, 1, RamGeneration.DDR5, 2,
                    PcieGeneration.PCIE_4_0, 0, 2))
            .named("Quince Logic Board (Russet Studio)").register();
    public static final ItemEntry<Item> BOARD_QSERVE_LEGACY = PartItems.board("quince_board_qserve_legacy",
            spec(QuinceBoards.QSERVE_LEGACY, HardwareEra.LEGACY, CpuSocketId.POWERQ_G4, 2, RamGeneration.DDR, 4,
                    PcieGeneration.PCI, 2, 3))
            .named("Quince Logic Board (qServe, Legacy)").register();
    public static final ItemEntry<Item> BOARD_QSERVE_TRANSITION = PartItems.board("quince_board_qserve_transition",
            spec(QuinceBoards.QSERVE_TRANSITION, HardwareEra.TRANSITION, CpuSocketId.LGA_771, 2, RamGeneration.DDR2,
                    8, PcieGeneration.PCIE_1_0, 2, 5))
            .named("Quince Logic Board (qServe, Transition)").register();
    public static final ItemEntry<Item> BOARD_QSERVE_STANDARD = PartItems.board("quince_board_qserve_standard",
            spec(QuinceBoards.QSERVE_STANDARD, HardwareEra.STANDARD, CpuSocketId.LGA_2011, 2, RamGeneration.DDR3,
                    8, PcieGeneration.PCIE_3_0, 2, 4))
            .named("Quince Logic Board (qServe, Standard)").register();
    public static final ItemEntry<Item> BOARD_QSERVE_ADVANCED = PartItems.board("quince_board_qserve_advanced",
            spec(QuinceBoards.QSERVE_ADVANCED, HardwareEra.ADVANCED, CpuSocketId.QUINCE_Q, 2, RamGeneration.DDR5,
                    8, PcieGeneration.PCIE_4_0, 2, 5))
            .named("Quince Logic Board (qServe, Advanced)").register();
    public static final ItemEntry<Item> BOARD_MAINFRAME_T = PartItems.board("quince_board_mainframe_t",
            spec(QuinceBoards.MAINFRAME_TRANSITION, HardwareEra.TRANSITION, CpuSocketId.LGA_771, 4,
                    RamGeneration.DDR2, 32, PcieGeneration.PCIE_1_0, 8, 6))
            .named("Quince Logic Board (Quince Mainframe, Transition)").register();
    public static final ItemEntry<Item> BOARD_MAINFRAME_S = PartItems.board("quince_board_mainframe_s",
            spec(QuinceBoards.MAINFRAME_STANDARD, HardwareEra.STANDARD, CpuSocketId.LGA_2011, 4,
                    RamGeneration.DDR3, 32, PcieGeneration.PCIE_3_0, 8, 6))
            .named("Quince Logic Board (Quince Mainframe, Standard)").register();
    public static final ItemEntry<Item> BOARD_MAINFRAME_A = PartItems.board("quince_board_mainframe_a",
            spec(QuinceBoards.MAINFRAME_ADVANCED, HardwareEra.ADVANCED, CpuSocketId.QUINCE_Q, 4,
                    RamGeneration.DDR5, 64, PcieGeneration.PCIE_5_0, 8, 6))
            .named("Quince Logic Board (Quince Mainframe, Advanced)").register();

    /*
     * The processors of Quince's own boards, each only in its own socket: the Motoroller 68000's long package, the
     * PowerQ G3's ceramic one, the G4's whole module with its heat sink on it, and Quince's own chips, which carry
     * their memory in the package beside the die, shared with the graphics on it. Clocks, power and memory follow the
     * real chips; all are estimates.
     */
    public static final ItemEntry<Item> CPU_68000 = PartItems.cpu("cpu_motoroller_68000",
            new CpuSpec(HardwareEra.VINTAGE, Isas.M68K, CpuSocketId.DIP_64, 1, 8, 2, false)
                    .on(Microarchitectures.M68000, ""))
            .named("Motoroller 68000").register();
    public static final ItemEntry<Item> CPU_G3 = PartItems.cpu("cpu_powerq_g3",
            new CpuSpec(HardwareEra.LEGACY, Isas.POWERPC, CpuSocketId.POWERQ_G3, 1, 400, 8, false)
                    .on(Microarchitectures.PPC_750, "Arthur"))
            .named("PowerQ G3").register();
    public static final ItemEntry<Item> CPU_G4 = PartItems.cpu("cpu_powerq_g4",
            new CpuSpec(HardwareEra.LEGACY, Isas.POWERPC, CpuSocketId.POWERQ_G4, 1, 1250, 30, false)
                    .on(Microarchitectures.PPC_7450, "Apollo").withHeatSink(G4_HEAT_SINK_WATTS))
            .named("PowerQ G4").register();
    public static final ItemEntry<Item> CPU_Q1 = PartItems.cpu("cpu_quince_q1",
            quinceChip(4, 3200, 20, Microarchitectures.FIRESTORM, 2064, Microarchitectures.ICESTORM,
                    new IntegratedGraphics("Quince Q1 GPU", 8, 1278), Q1_MEMORY))
            .named("Quince Q1").register();
    public static final ItemEntry<Item> CPU_Q2 = PartItems.cpu("cpu_quince_q2",
            quinceChip(4, 3490, 22, Microarchitectures.AVALANCHE, 2420, Microarchitectures.BLIZZARD,
                    new IntegratedGraphics("Quince Q2 GPU", 10, 1398), Q2_MEMORY))
            .named("Quince Q2").register();
    public static final ItemEntry<Item> CPU_Q3 = PartItems.cpu("cpu_quince_q3",
            quinceChip(4, 4050, 22, Microarchitectures.EVEREST, 2750, Microarchitectures.SAWTOOTH,
                    new IntegratedGraphics("Quince Q3 GPU", 10, 1380), Q3_MEMORY))
            .named("Quince Q3").register();

    /*
     * The Russet Pros' heat sinks: the finned aluminium block that sits over the processor where a PC's cooler does,
     * with no fan of its own, the case's fans blowing through it. Watts each takes away; estimates.
     */
    public static final ItemEntry<Item> HEAT_SINK_PRO_2006 = PartItems.cooler("cooler_quince_pro_2006",
            new CoolerSpec(HardwareEra.TRANSITION, CoolerSpec.Kind.TOWER, 150))
            .named("Quince Heat Sink (Russet Pro, Transition)").register();
    public static final ItemEntry<Item> HEAT_SINK_PRO_2019 = PartItems.cooler("cooler_quince_pro_2019",
            new CoolerSpec(HardwareEra.STANDARD, CoolerSpec.Kind.TOWER, 300))
            .named("Quince Heat Sink (Russet Pro, Standard)").register();

    private QuinceItems() {
    }

    /** Loads the boards, which declares them, before the mod's content goes to its event bus. */
    public static void init() {
        // Referencing the class runs the declarations above.
    }

    /*
     * One of Quince's own chips, of the Advanced: its performance cores and their clock, its power, the designs of its
     * two kinds of core and the clock of the efficiency ones, the graphics on its die and the memory in its package.
     */
    private static CpuSpec quinceChip(final int cores, final int mhz, final int watts, final Microarchitecture design,
                                      final int efficiencyMhz, final Microarchitecture efficiencyDesign,
                                      final IntegratedGraphics graphics, final long memory) {
        return new CpuSpec(HardwareEra.ADVANCED, Isas.ARM64, CpuSocketId.QUINCE_Q, cores, mhz, watts, false)
                .on(design, "")
                .withEfficiencyCores(Q_EFFICIENCY_CORES, efficiencyMhz, efficiencyDesign)
                .withGraphics(graphics)
                .withMemory(memory);
    }

    private static MotherboardSpec spec(final FormFactor form, final Era era, final CpuSocketId socket,
                                        final int processors, final RamGeneration memory, final int memorySlots,
                                        final PcieGeneration bus, final int cardSlots, final int disks) {
        return new MotherboardSpec(form, era, socket, processors, Set.of(memory), memorySlots, bus, cardSlots, disks);
    }
}
