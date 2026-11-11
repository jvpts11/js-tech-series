/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.machine.MachinePrograms;
import dev.jstech.computers.sigma.SigmaCompiler;
import dev.jstech.computers.sigma.SourceFile;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureAutopilot;
import dev.jstech.core.moving.StructureResult;
import dev.jstech.core.moving.StructureYard;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestAboard;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A program flying the moving structure its computer stands on through System.Navigation: on the ground it reads where
 * it stands and has nothing to fly; aboard, what it tells the autopilot is what J's Core's autopilot is set to.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SigmaNavigationGameTests {

    private static final String ARENA = "empty";
    /** Their own batch, so no other test's structures share the yard while these fly. */
    private static final String BATCH = "jsc_navigation";
    private static final int SETTLE = 2;
    /** The namespaces a program brings in, on one line so its source keeps its line numbers. */
    private static final String PRELUDE = "using System.*; using System.IO.*; using System.Collections.*; "
            + "using System.Navigation.*; namespace Programs; ";
    private static final BlockPos DECK = new BlockPos(2, 2, 2);
    private static final BlockPos HOST_CLAMP = new BlockPos(5, 2, 5);

    private SigmaNavigationGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void onTheGround_aProgramReadsWhereItStandsAndHasNothingToFly(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer =
                TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(DECK);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final List<String> said = run(helper, computer, """
                            class Pilot {
                                static void Main() {
                                    Console.PrintLine(Navigation.Aboard ? "aboard" : "on the ground");
                                    Console.PrintLine("" + Navigation.Altitude);
                                    Console.PrintLine(Navigation.Speed < 0.001 ? "still" : "moving");
                                    Console.PrintLine(Navigation.GoTo(0, 100, 0, 5) ? "flying" : "nothing to fly");
                                    Console.PrintLine(Navigation.Release() ? "let go" : "nothing to let go");
                                    try {
                                        Navigation.GoTo(0, 100, 0, 0);
                                    } catch (ArgumentOutOfRangeException failed) {
                                        Console.PrintLine("no such speed");
                                    }
                                }
                            }
                            """);
                    helper.assertTrue(said.size() == 6, "it said six lines: " + said);
                    helper.assertValueEqual(said.getFirst(), "on the ground", "where it is");
                    helper.assertTrue(Math.abs(Double.parseDouble(said.get(1)) - (computer.getBlockPos().getY()
                            + 0.5)) < 1.0e-6, "its altitude is the middle of its block: " + said.get(1));
                    helper.assertValueEqual(said.subList(2, 6), List.of("still", "nothing to fly", "nothing to let go",
                            "no such speed"), "what it was told");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 120)
    public static void aboard_aProgramSetsAndLetsGoOfItsStructuresAutopilot(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        helper.setBlock(DECK, Blocks.STONE);
        TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(DECK.above());
        final MovingStructure craft = assembled(helper, DECK, DECK.above());
        final PersonalComputerBlockEntity computer = computerOn(level, craft);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertValueEqual(run(helper, computer, """
                            class Pilot {
                                static void Main() {
                                    Console.PrintLine(Navigation.Aboard ? "aboard" : "on the ground");
                                    List<double> route = new List<double>();
                                    route.Add(Navigation.X);
                                    route.Add(Navigation.Altitude + 4);
                                    route.Add(Navigation.Z);
                                    route.Add(Navigation.X + 4);
                                    route.Add(Navigation.Altitude + 4);
                                    route.Add(Navigation.Z);
                                    Console.PrintLine(Navigation.FollowRoute(route, 2, true) ? "routed" : "not routed");
                                    Console.PrintLine(Navigation.Piloted ? "piloted" : "not piloted");
                                }
                            }
                            """), List.of("aboard", "routed", "piloted"), "what the program said");
                    final StructureAutopilot pilot = craft.autopilot();
                    helper.assertTrue(pilot != null && pilot.speed() == 2
                                    && pilot.goal() instanceof StructureAutopilot.Route route
                                    && route.points().size() == 2 && route.loop(),
                            "the autopilot follows the route it was given: " + pilot);
                    helper.assertValueEqual(run(helper, computer, """
                            class Pilot {
                                static void Main() {
                                    Console.PrintLine(Navigation.Release() ? "let go" : "kept");
                                    Console.PrintLine(Navigation.Piloted ? "piloted" : "not piloted");
                                }
                            }
                            """), List.of("let go", "not piloted"), "what the program said");
                    helper.assertTrue(craft.autopilot() == null, "nothing flies it now");
                    MovingStructures.takeAway(craft);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 120)
    public static void dock_aProgramAimsItsStructureAtAnotherStructuresClamp(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        helper.setBlock(HOST_CLAMP, TestAboard.CLAMP.get().defaultBlockState()
                .setValue(DirectionalBlock.FACING, Direction.WEST));
        final MovingStructure host = assembled(helper, HOST_CLAMP, HOST_CLAMP);
        helper.setBlock(DECK, Blocks.STONE);
        TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(DECK.above());
        final MovingStructure craft = assembled(helper, DECK, DECK.above());
        final PersonalComputerBlockEntity computer = computerOn(level, craft);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final Vec3 clamp = MovingStructures.inWorld(level, Vec3.atCenterOf(
                            find(level, host, state -> state.is(TestAboard.CLAMP.get()))));
                    final String at = String.format(Locale.ROOT, "%.3f, %.3f, %.3f", clamp.x, clamp.y, clamp.z);
                    helper.assertValueEqual(run(helper, computer, """
                            class Pilot {
                                static void Main() {
                                    Console.PrintLine(Navigation.Dock(%s, 1) ? "docking" : "no clamp");
                                    Console.PrintLine(Navigation.Dock(%s + 40, 1) ? "docking" : "no clamp");
                                }
                            }
                            """.formatted(at, at)), List.of("docking", "no clamp"), "what the program said");
                    final StructureAutopilot pilot = craft.autopilot();
                    helper.assertTrue(pilot != null && pilot.goal() instanceof StructureAutopilot.Dock dock
                            && dock.host().equals(host.id()), "the autopilot docks to the host: " + pilot);
                    craft.fly(null);
                    MovingStructures.takeAway(craft);
                    MovingStructures.takeAway(host);
                })
                .thenSucceed();
    }

    /* The computer among the structure's blocks, as it stands in the yard. */
    private static PersonalComputerBlockEntity computerOn(final ServerLevel level, final MovingStructure structure) {
        for (final BlockPos pos : BlockPos.betweenClosed(structure.bounds().minX(), structure.bounds().minY(),
                structure.bounds().minZ(), structure.bounds().maxX(), structure.bounds().maxY(),
                structure.bounds().maxZ())) {
            final BlockEntity block = level.getBlockEntity(pos);
            if (block instanceof PersonalComputerBlockEntity computer) {
                return computer;
            }
        }
        throw new IllegalStateException("no computer aboard the structure");
    }

    /* The first block of the structure's yard that is so. */
    private static BlockPos find(final ServerLevel level, final MovingStructure structure,
                                 final Predicate<BlockState> wanted) {
        for (final BlockPos at : BlockPos.betweenClosed(structure.bounds().minX(), structure.bounds().minY(),
                structure.bounds().minZ(), structure.bounds().maxX(), structure.bounds().maxY(),
                structure.bounds().maxZ())) {
            if (wanted.test(level.getBlockState(at))) {
                return at.immutable();
            }
        }
        throw new IllegalStateException("nothing so among the structure's blocks");
    }

    /* Makes every block of the arena's box from {@code from} to {@code to} into a structure. */
    private static MovingStructure assembled(final GameTestHelper helper, final BlockPos from, final BlockPos to) {
        final ServerLevel level = helper.getLevel();
        final int plot = StructureYard.nextPlot(level);
        // The chunks round the yard's next plot are loaded, so the structure made comes into it at once.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.getChunk((YardLayout.centreX(plot) >> 4) + dx, (YardLayout.centreZ(plot) >> 4) + dz);
            }
        }
        final StructureResult made = MovingStructures.assemble(level, helper.absolutePos(from),
                helper.absolutePos(to));
        helper.assertTrue(made.ok() && made.structure() != null, "a structure is made: "
                + (made.refusal() == null ? "done" : made.refusal().english()));
        return made.structure();
    }

    /* Runs {@code source} on {@code computer} to its end and gives back what it printed. */
    private static List<String> run(final GameTestHelper helper, final AbstractComputerBlockEntity computer,
                                    final String source) {
        final SigmaCompiler.Result built = SigmaCompiler.compile(List.of(new SourceFile("Script.sgs",
                PRELUDE + source)));
        helper.assertTrue(built.ok(), "the script compiles: " + String.join("\n", built.lines()));
        final MachinePrograms.Started started = computer.programs().start("pilot.asm", built.assembly(), 1,
                computer);
        helper.assertTrue(started.ok(), "the program starts: " + started.message());
        computer.programs().tick(100000);
        return computer.programs().byId(started.id()).process().console();
    }
}
