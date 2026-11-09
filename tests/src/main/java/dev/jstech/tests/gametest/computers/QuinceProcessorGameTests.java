/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.CoolingItems;
import dev.jstech.computers.QuinceItems;
import dev.jstech.computers.api.language.IProgrammingLanguage;
import dev.jstech.computers.blockentity.ComputerHardwareLayout;
import dev.jstech.computers.blockentity.PartRefusals;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.hardware.ComputerBuild;
import dev.jstech.computers.hardware.CpuSocketId;
import dev.jstech.computers.hardware.CpuSpec;
import dev.jstech.computers.hardware.IsaSpec;
import dev.jstech.computers.hardware.Isas;
import dev.jstech.computers.machine.MachineListing;
import dev.jstech.computers.machine.SigmaLanguage;
import dev.jstech.computers.os.boot.SystemIntegrity;
import dev.jstech.computers.registry.HardwareRegistry;
import dev.jstech.computers.vm.listing.AsmWriter;
import dev.jstech.computers.vm.listing.ListingProblem;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.EraParts;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Quince's processors and instruction sets (computers/docs/design/quince.md): the Motoroller 68000, the PowerQ G3 and
 * G4 and Quince's own chips, each only in its own socket; the 68k, the PowerPC and the ARM64, each running only what
 * was built for it; a system built for another processor neither installed nor started; and a universal build,
 * which runs on each processor it carries a build for.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class QuinceProcessorGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos AT = new BlockPos(2, 2, 2);
    private static final BlockPos BESIDE = new BlockPos(5, 2, 2);
    private static final BlockPos THIRD = new BlockPos(8, 2, 2);
    private static final ResourceLocation MC_DOS = ResourceLocation.fromNamespaceAndPath("jsc", "mc_dos");
    /* Room enough for a program that does nothing. */
    private static final long ROOM = 1024L * 1024L;
    /* The memory the Quince Q1 carries in its package, in buffer items: 8 GB. */
    private static final long Q1_MEMORY = 2048L;
    /* What the Russet Power G4's case takes away, and the heat sink on the G4's module. */
    private static final int G4_CASE_WATTS = 150;
    private static final int G4_HEAT_SINK_WATTS = 60;
    private static final String QUIET = """
            using System.IO.*;
            namespace Programs;
            class Quiet {
                static void Main() { Console.PrintLine("quiet"); }
            }
            """;

    private QuinceProcessorGameTests() {
    }

    @GameTest(template = ARENA)
    public static void quinceProcessors_goInTheirOwnSocketsBuiltForTheirIsas(final GameTestHelper helper) {
        assertCpu(helper, QuinceItems.CPU_68000.get(), CpuSocketId.DIP_64, Isas.M68K, HardwareEra.VINTAGE);
        assertCpu(helper, QuinceItems.CPU_G3.get(), CpuSocketId.POWERQ_G3, Isas.POWERPC, HardwareEra.LEGACY);
        assertCpu(helper, QuinceItems.CPU_G4.get(), CpuSocketId.POWERQ_G4, Isas.POWERPC, HardwareEra.LEGACY);
        for (final Item chip : List.of(QuinceItems.CPU_Q1.get(), QuinceItems.CPU_Q2.get(), QuinceItems.CPU_Q3.get())) {
            final CpuSpec spec = assertCpu(helper, chip, CpuSocketId.QUINCE_Q, Isas.ARM64, HardwareEra.ADVANCED);
            helper.assertTrue(spec.hasIntegratedGraphics(), "a Quince chip carries its graphics: " + chip);
            helper.assertTrue(spec.design().memoryItems() > 0, "and its memory in the package: " + chip);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void russet128k_runsOnTheMotoroller68000(final GameTestHelper helper) {
        final PersonalComputerBlockEntity russet = russet128k(helper, AT);
        final ComputerBuild build = russet.currentBuild();
        helper.assertTrue(build != null && build.validate().valid(),
                "the Russet 128K is a computer on its 68000: " + (build == null ? "no build" : build.validate()));
        helper.assertValueEqual(build.isa(), Isas.M68K, "the machine's instruction set");
        helper.succeed();
    }

    /** A Quince chip's memory is the machine's: the coloured qRusset runs on it with no module in a slot. */
    @GameTest(template = ARENA)
    public static void qRussetAdvanced_runsOnTheMemoryInItsChipAlone(final GameTestHelper helper) {
        final PersonalComputerBlockEntity colour = built(helper, AT, ComputingModule.QRUSSET_COLOURS.getFirst().get(),
                QuinceItems.BOARD_QRUSSET_COLOUR.get(), QuinceItems.CPU_Q1.get(), HardwareEra.ADVANCED, false);
        final ComputerBuild build = colour.currentBuild();
        helper.assertTrue(build != null && build.validate().valid(),
                "no module is needed: " + (build == null ? "no build" : build.validate()));
        helper.assertValueEqual(colour.ramBuffer(), Q1_MEMORY, "the memory is the Q1's");
        helper.succeed();
    }

    /**
     * The PowerQ G4 is the whole module, its heat sink on it, as the approved drawing has it: the sink cools it with no
     * cooler over it, and its seat takes no cooler while it is there, nor the module while a cooler is.
     */
    @GameTest(template = ARENA)
    public static void powerQG4_isCooledByItsOwnHeatSink(final GameTestHelper helper) {
        final PersonalComputerBlockEntity g4 = powerG4(helper, AT);
        final ComputerHardwareLayout map = g4.hardwareLayout();
        helper.assertValueEqual(g4.thermal().bareProcessors(), 0, "the G4 is not a bare processor");
        helper.assertValueEqual(g4.thermal().budgetWatts(), G4_CASE_WATTS + G4_HEAT_SINK_WATTS,
                "its sink takes away its watts beside the case's");
        final ItemStack cooler = new ItemStack(CoolingItems.LEGACY_STOCK_COOLER.get());
        helper.assertValueEqual(g4.refusal(map.coolerStart(), cooler), PartRefusals.OWN_HEAT_SINK,
                "no cooler goes over its sink");
        final PersonalComputerBlockEntity other = powerG4(helper, BESIDE);
        final ItemStackHandler hardware = other.getHardware();
        hardware.setStackInSlot(map.cpuSlot(0), ItemStack.EMPTY);
        hardware.setStackInSlot(map.coolerStart(), cooler);
        helper.assertValueEqual(other.refusal(map.cpuSlot(0), new ItemStack(QuinceItems.CPU_G4.get())),
                PartRefusals.COOLER_IN_ITS_PLACE, "and the module goes in no seat a cooler sits on");
        helper.succeed();
    }

    /** MC-DOS is built for the x86 line, so a 68000 never takes it, while an Integra qRusset does. */
    @GameTest(template = ARENA)
    public static void aSystemBuiltForAnotherProcessor_isNotInstalled(final GameTestHelper helper) {
        final PersonalComputerBlockEntity russet = russet128k(helper, AT);
        helper.assertFalse(russet.installOs(MC_DOS), "MC-DOS is not put on the 68000's disk");
        helper.assertFalse(russet.hasOs(), "and the disk carries no system");
        final PersonalComputerBlockEntity white = built(helper, BESIDE, ComputingModule.QRUSSET_WHITE.get(),
                QuinceItems.BOARD_QRUSSET_WHITE.get(), null, HardwareEra.TRANSITION, true);
        helper.assertTrue(white.installOs(MC_DOS), "the Integra qRusset takes it, as any x86 machine does");
        helper.succeed();
    }

    /** A disk carried from an x86 machine to a PowerPC one shows its firmware no system it can start. */
    @GameTest(template = ARENA)
    public static void aDiskCarriedToAnotherProcessor_startsNothing(final GameTestHelper helper) {
        final PersonalComputerBlockEntity white = built(helper, AT, ComputingModule.QRUSSET_WHITE.get(),
                QuinceItems.BOARD_QRUSSET_WHITE.get(), null, HardwareEra.TRANSITION, true);
        helper.assertTrue(white.installOs(TestWorldBuilder.LEGACY_DESKTOP_OS), "the system goes on the x86 disk");
        helper.assertTrue(SystemIntegrity.check(white).whole(), "and starts there");
        final PersonalComputerBlockEntity g4 = powerG4(helper, BESIDE);
        g4.getHardware().setStackInSlot(g4.hardwareLayout().diskStart(), white.systemDisk().copy());
        helper.assertTrue(g4.hasOs(), "the disk still carries its system");
        helper.assertValueEqual(SystemIntegrity.check(g4).state(), SystemIntegrity.State.NO_SYSTEM,
                "but the G4 finds nothing it can start on it");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void aProgramBuiltForPowerPc_runsOnlyOnPowerPc(final GameTestHelper helper) {
        final String listing = compiled(helper, Isas.POWERPC);
        final PersonalComputerBlockEntity g4 = powerG4(helper, AT);
        final PersonalComputerBlockEntity white = built(helper, BESIDE, ComputingModule.QRUSSET_WHITE.get(),
                QuinceItems.BOARD_QRUSSET_WHITE.get(), null, HardwareEra.TRANSITION, true);
        helper.assertTrue(MachineListing.start(listing, ROOM, g4, List.of()) != null, "the G4 runs it");
        helper.assertTrue(MachineListing.start(listing, ROOM, white, List.of()) == null,
                "an x86-64 machine with no translator does not");
        final ListingProblem why = MachineListing.firstProblem(listing, white);
        helper.assertTrue(why != null && why.message().english().contains("built for PowerPC; this machine is x86-64"),
                "and says why: " + (why == null ? "nothing" : why.message().english()));
        helper.succeed();
    }

    /** A universal build runs on each processor it carries a build for, and on no other. */
    @GameTest(template = ARENA)
    public static void aUniversalBuild_runsOnEachProcessorItCarries(final GameTestHelper helper) {
        final String universal = AsmWriter.universal(List.of(compiled(helper, Isas.POWERPC),
                compiled(helper, Isas.X86_64)));
        final PersonalComputerBlockEntity g4 = powerG4(helper, AT);
        final PersonalComputerBlockEntity white = built(helper, BESIDE, ComputingModule.QRUSSET_WHITE.get(),
                QuinceItems.BOARD_QRUSSET_WHITE.get(), null, HardwareEra.TRANSITION, true);
        final PersonalComputerBlockEntity colour = built(helper, THIRD,
                ComputingModule.QRUSSET_COLOURS.getFirst().get(), QuinceItems.BOARD_QRUSSET_COLOUR.get(),
                QuinceItems.CPU_Q1.get(), HardwareEra.ADVANCED, false);
        helper.assertTrue(MachineListing.start(universal, ROOM, g4, List.of()) != null, "the PowerPC build runs");
        helper.assertTrue(MachineListing.start(universal, ROOM, white, List.of()) != null, "so does the x86-64 one");
        helper.assertTrue(MachineListing.start(universal, ROOM, colour, List.of()) == null,
                "and an ARM64 machine runs neither");
        final ListingProblem why = MachineListing.firstProblem(universal, colour);
        helper.assertTrue(why != null
                        && why.message().english().contains("built for PowerPC, x86-64; this machine is ARM64"),
                "naming every build it carries: " + (why == null ? "nothing" : why.message().english()));
        helper.succeed();
    }

    /* Fails unless the item is a processor of that socket, instruction set and era, and gives back its sheet. */
    private static CpuSpec assertCpu(final GameTestHelper helper, final Item item, final CpuSocketId socket,
                                     final IsaSpec isa, final Era era) {
        final HardwareRegistry.Part part = HardwareRegistry.part(item);
        helper.assertTrue(part != null && part.spec() instanceof CpuSpec, item + " is a processor");
        final CpuSpec spec = (CpuSpec) part.spec();
        helper.assertValueEqual(spec.socket(), socket, "the socket of " + item);
        helper.assertValueEqual(spec.isa(), isa, "the instruction set of " + item);
        helper.assertValueEqual(spec.era(), era, "the era of " + item);
        return spec;
    }

    /* The quiet program, compiled for that instruction set. */
    private static String compiled(final GameTestHelper helper, final IsaSpec isa) {
        final IProgrammingLanguage.CompileResult built = SigmaLanguage.SIGMA_SHARP.compile(
                List.of(new IProgrammingLanguage.SourceText("Quiet.sgs", QUIET)), isa.id());
        helper.assertTrue(built.ok(), "the program compiles for " + isa.name() + ": " + built.complaints());
        return built.binary();
    }

    private static PersonalComputerBlockEntity russet128k(final GameTestHelper helper, final BlockPos at) {
        return built(helper, at, ComputingModule.RUSSET_128K.get(), QuinceItems.BOARD_RUSSET_128K.get(),
                QuinceItems.CPU_68000.get(), HardwareEra.VINTAGE, true);
    }

    private static PersonalComputerBlockEntity powerG4(final GameTestHelper helper, final BlockPos at) {
        return built(helper, at, ComputingModule.RUSSET_POWER_G4.get(), QuinceItems.BOARD_POWER_G4.get(),
                QuinceItems.CPU_G4.get(), HardwareEra.LEGACY, true);
    }

    /*
     * A machine assembled on {@code board}, switched off: the processor given, or the best of the board's era for
     * null, a memory module of the board's when asked, its era's supply and disk. Each part goes in the slot the
     * machine's own map has for it, a Quince case having no bays to count.
     */
    private static PersonalComputerBlockEntity built(final GameTestHelper helper, final BlockPos at, final Block block,
                                                     final Item board, final Item cpu, final Era era,
                                                     final boolean module) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        world.setBlock(at, block);
        final PersonalComputerBlockEntity machine = world.blockEntity(at, PersonalComputerBlockEntity.class);
        final ItemStackHandler hardware = machine.getHardware();
        final ComputerHardwareLayout map = machine.hardwareLayout();
        hardware.setStackInSlot(map.motherboardSlot(), new ItemStack(board));
        hardware.setStackInSlot(map.cpuSlot(0), new ItemStack(cpu == null ? EraParts.cpuFor(board) : cpu));
        if (module) {
            hardware.setStackInSlot(map.ramSlot(0), new ItemStack(EraParts.ramFor(board)));
        }
        hardware.setStackInSlot(map.psuSlot(), new ItemStack(EraParts.psuFor(era)));
        hardware.setStackInSlot(map.diskStart(), new ItemStack(EraParts.diskFor(era)));
        return machine;
    }
}
