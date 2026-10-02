/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.MainframeBlock;
import dev.jstech.core.cable.CableEntry;
import dev.jstech.core.cable.CableType;
import dev.jstech.core.cable.Lane;
import dev.jstech.core.connect.IFaceConnector;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.network.DataLine;
import dev.jstech.core.network.DataLines;
import dev.jstech.core.network.DataLink;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The data cables as lines by job, a cable of each line for each era: what each one is, the machines that take their
 * era's cable and every earlier one, two eras of one line that never join directly, the backbone's fibre that runs
 * only straight, and the long distance line that runs between two ends and shares no block.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CableLineGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;

    /** Every data cable, with the line and era it is. */
    private static final List<Laid> CABLES = List.of(
            new Laid(ComputingModule.THIN_COAX_CABLE, DataLine.ACCESS, HardwareEra.VINTAGE),
            new Laid(ComputingModule.ETHERNET_CABLE, DataLine.ACCESS, HardwareEra.LEGACY),
            new Laid(ComputingModule.CAT5E_CABLE, DataLine.ACCESS, HardwareEra.TRANSITION),
            new Laid(ComputingModule.GIGABIT_CABLE, DataLine.ACCESS, HardwareEra.STANDARD),
            new Laid(ComputingModule.CAT6A_CABLE, DataLine.ACCESS, HardwareEra.ADVANCED),
            new Laid(ComputingModule.THICK_COAX_CABLE, DataLine.BACKBONE, HardwareEra.VINTAGE),
            new Laid(ComputingModule.HBW_CABLE, DataLine.BACKBONE, HardwareEra.LEGACY),
            new Laid(ComputingModule.CX4_CABLE, DataLine.BACKBONE, HardwareEra.TRANSITION),
            new Laid(ComputingModule.FIBRE_CABLE, DataLine.BACKBONE, HardwareEra.STANDARD),
            new Laid(ComputingModule.OM5_CABLE, DataLine.BACKBONE, HardwareEra.ADVANCED),
            new Laid(ComputingModule.TELEPHONE_LINE, DataLine.LONG_DISTANCE, HardwareEra.VINTAGE),
            new Laid(ComputingModule.LEASED_LINE, DataLine.LONG_DISTANCE, HardwareEra.LEGACY),
            new Laid(ComputingModule.T3_LINE, DataLine.LONG_DISTANCE, HardwareEra.TRANSITION),
            new Laid(ComputingModule.VLDC_CABLE, DataLine.LONG_DISTANCE, HardwareEra.STANDARD),
            new Laid(ComputingModule.DARK_FIBRE_CABLE, DataLine.LONG_DISTANCE, HardwareEra.ADVANCED),
            new Laid(ComputingModule.INFINIBAND_CABLE, DataLine.HPC, HardwareEra.TRANSITION),
            new Laid(ComputingModule.HPC_CABLE, DataLine.HPC, HardwareEra.STANDARD),
            new Laid(ComputingModule.OSFP_CABLE, DataLine.HPC, HardwareEra.ADVANCED),
            new Laid(ComputingModule.CRAFTING_CABLE, DataLine.CRAFTING, HardwareEra.VINTAGE));

    /** Each age's Mainframe and the backbone cable of each era. */
    private static final List<BlockEntry<MainframeBlock>> MAINFRAMES = List.of(ComputingModule.VINTAGE_MAINFRAME,
            ComputingModule.LEGACY_MAINFRAME, ComputingModule.TRANSITION_MAINFRAME, ComputingModule.MAINFRAME,
            ComputingModule.ADVANCED_MAINFRAME);
    private static final List<CableEntry> BACKBONES = List.of(ComputingModule.THICK_COAX_CABLE,
            ComputingModule.HBW_CABLE, ComputingModule.CX4_CABLE, ComputingModule.FIBRE_CABLE,
            ComputingModule.OM5_CABLE);

    private CableLineGameTests() {
    }

    @GameTest(template = ARENA)
    public static void cables_eachLineHasACableInEachEraItExistsIn(final GameTestHelper helper) {
        for (final Laid laid : CABLES) {
            final CableType type = laid.cable().get();
            final DataLink link = new DataLink(laid.line(), laid.era());
            helper.assertValueEqual(DataLines.linkOf(type.line()), link, type.id() + " is its line in its era");
            helper.assertTrue(type.throughput() == link.throughput() && type.range() == link.range(),
                    type.id() + " carries and reaches what its link does");
            if (laid.line() == DataLine.LONG_DISTANCE) {
                helper.assertTrue(type.alone() && type.thickness() == 6 && type.mostJoins() == DataLink.ENDS,
                        type.id() + " is six pixels, shares no block and joins two ends");
            } else {
                helper.assertTrue(type.lane().isPresent() && type.thickness() == 4,
                        type.id() + " takes its line's lane and is four pixels");
            }
            helper.assertTrue(type.runsStraight() == link.straight(),
                    type.id() + (link.straight() ? " runs only straight" : " bends"));
        }
        helper.assertValueEqual(ComputingModule.HBW_CABLE.get().lane().orElseThrow(),
                ComputingModule.FIBRE_CABLE.get().lane().orElseThrow(), "every era of a line takes one lane");
        helper.assertValueEqual(ComputingModule.ETHERNET_CABLE.get().lane().orElseThrow(), Lane.TOP_LEFT,
                "the access line top left");
        helper.succeed();
    }

    /** A machine takes its era's cable and every earlier one's, never a later one's. */
    @GameTest(template = ARENA)
    public static void ports_takeTheirErasCableAndEveryEarlierOne(final GameTestHelper helper) {
        final BlockPos at = new BlockPos(2, 2, 2);
        for (int machine = 0; machine < MAINFRAMES.size(); machine++) {
            helper.setBlock(at, MAINFRAMES.get(machine).get());
            final BlockState state = helper.getBlockState(at);
            final IFaceConnector ports = (IFaceConnector) state.getBlock();
            for (int cable = 0; cable < BACKBONES.size(); cable++) {
                final boolean takes = ports.accepts(state, Direction.EAST, BACKBONES.get(cable).get().line());
                helper.assertTrue(takes == cable <= machine, MAINFRAMES.get(machine).getId() + " and "
                        + BACKBONES.get(cable).get().id() + ": " + (takes ? "taken" : "refused"));
            }
            helper.setBlock(at, Blocks.AIR);
        }
        helper.succeed();
    }

    /** Two eras of one line do not join directly: they meet at a router or a repeater of the newer era. */
    @GameTest(template = ARENA)
    public static void twoEras_ofOneLineDoNotJoinDirectly(final GameTestHelper helper) {
        TestCables.lay(helper, new BlockPos(1, 2, 2), ComputingModule.ETHERNET_CABLE);
        TestCables.lay(helper, new BlockPos(2, 2, 2), ComputingModule.ETHERNET_CABLE);
        TestCables.lay(helper, new BlockPos(3, 2, 2), ComputingModule.CAT5E_CABLE);
        helper.runAfterDelay(SETTLE, () -> {
            helper.assertTrue(TestCables.joined(helper, new BlockPos(1, 2, 2), new BlockPos(2, 2, 2)),
                    "two Ethernet cables join");
            helper.assertFalse(TestCables.joined(helper, new BlockPos(2, 2, 2), new BlockPos(3, 2, 2)),
                    "Ethernet and Cat 5e touch and do not join");
            helper.succeed();
        });
    }

    /** The backbone's fibre runs only straight: a branch off its side is left out, a run on in line joins. */
    @GameTest(template = ARENA)
    public static void fibre_runsOnlyStraight(final GameTestHelper helper) {
        TestCables.lay(helper, new BlockPos(1, 2, 2), ComputingModule.FIBRE_CABLE);
        TestCables.lay(helper, new BlockPos(2, 2, 2), ComputingModule.FIBRE_CABLE);
        helper.runAfterDelay(SETTLE, () -> {
            TestCables.lay(helper, new BlockPos(2, 2, 3), ComputingModule.FIBRE_CABLE);
            TestCables.lay(helper, new BlockPos(3, 2, 2), ComputingModule.FIBRE_CABLE);
        });
        helper.runAfterDelay(SETTLE * 2, () -> {
            helper.assertTrue(TestCables.joined(helper, new BlockPos(1, 2, 2), new BlockPos(3, 2, 2)),
                    "a straight run of fibre joins");
            helper.assertFalse(TestCables.joined(helper, new BlockPos(2, 2, 2), new BlockPos(2, 2, 3)),
                    "fibre does not turn the corner");
            helper.succeed();
        });
    }

    /** The long distance line's dark fibre bends like the other long distance cables. */
    @GameTest(template = ARENA)
    public static void darkFibre_bends(final GameTestHelper helper) {
        TestCables.lay(helper, new BlockPos(1, 2, 2), ComputingModule.DARK_FIBRE_CABLE);
        TestCables.lay(helper, new BlockPos(2, 2, 2), ComputingModule.DARK_FIBRE_CABLE);
        TestCables.lay(helper, new BlockPos(2, 2, 3), ComputingModule.DARK_FIBRE_CABLE);
        helper.runAfterDelay(SETTLE, () -> {
            helper.assertTrue(TestCables.joined(helper, new BlockPos(1, 2, 2), new BlockPos(2, 2, 3)),
                    "dark fibre turns the corner");
            helper.succeed();
        });
    }

    /** A long distance run goes between two ends: a third end laid against it is refused. */
    @GameTest(template = ARENA)
    public static void longDistance_takesNoThirdEnd(final GameTestHelper helper) {
        TestCables.lay(helper, new BlockPos(1, 2, 2), ComputingModule.TELEPHONE_LINE);
        TestCables.lay(helper, new BlockPos(2, 2, 2), ComputingModule.TELEPHONE_LINE);
        TestCables.lay(helper, new BlockPos(3, 2, 2), ComputingModule.TELEPHONE_LINE);
        helper.runAfterDelay(SETTLE, () -> TestCables.lay(helper, new BlockPos(2, 2, 3),
                ComputingModule.TELEPHONE_LINE));
        helper.runAfterDelay(SETTLE * 2, () -> {
            helper.assertTrue(TestCables.joined(helper, new BlockPos(1, 2, 2), new BlockPos(3, 2, 2)),
                    "the line runs end to end");
            helper.assertFalse(TestCables.joined(helper, new BlockPos(2, 2, 2), new BlockPos(2, 2, 3)),
                    "a third end is refused");
            helper.succeed();
        });
    }

    /** A block holding a long distance cable holds nothing else. */
    @GameTest(template = ARENA)
    public static void longDistance_sharesNoBlock(final GameTestHelper helper) {
        TestCables.lay(helper, new BlockPos(2, 2, 2), ComputingModule.LEASED_LINE);
        helper.assertFalse(TestCables.lay(helper.getLevel(), helper.absolutePos(new BlockPos(2, 2, 2)),
                ComputingModule.ETHERNET_CABLE.get()), "no Ethernet in a leased line's block");
        TestCables.lay(helper, new BlockPos(4, 2, 2), ComputingModule.ETHERNET_CABLE);
        helper.assertFalse(TestCables.lay(helper.getLevel(), helper.absolutePos(new BlockPos(4, 2, 2)),
                ComputingModule.LEASED_LINE.get()), "and no leased line in an Ethernet's block");
        helper.succeed();
    }

    /** A cable as the test lays it, with the line and era it is. */
    private record Laid(CableEntry cable, DataLine line, HardwareEra era) {
    }
}
