/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.grid.CoreGrids;
import dev.jstech.core.grid.GridPlace;
import dev.jstech.core.jstechseries.energy.SharedEnergy;
import dev.jstech.core.machine.MachineStructure;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.moving.StructureResult;
import dev.jstech.core.moving.StructureYard;
import dev.jstech.core.moving.TowardMotion;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.core.multiblock.FormedPartBlock;
import dev.jstech.core.multiblock.FormedPartBlockEntity;
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.core.overhead.HungWire;
import dev.jstech.core.overhead.OverheadLines;
import dev.jstech.core.overhead.WireEnd;
import dev.jstech.core.region.Box;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestEnergies;
import dev.jstech.tests.TestModels;
import dev.jstech.tests.testkit.TestCables;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Core's own kits working aboard a moving structure as they work on the ground: a multiblock that crosses into
 * the yard formed and merged, and stands formed again set down turned, with nothing given back where it stood; blocks
 * that tick on their own, crops' farmland drying and a furnace smelting, with no player near; cables that leave
 * their grids where they stood; overhead wires that go with the insulators they hang from, and fall when one stays;
 * and many structures moving at once, worked out beside the server's thread.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MovingStructureAboardGameTests {

    private static final String ARENA = "empty";
    private static final String TICKS = "jscore_moving_aboard_ticks";
    /* The turbine's terminal, in the middle of the south side of a turbine built facing north. */
    private static final BlockPos TERMINAL = new BlockPos(4, 3, 5);
    /* The game's own number of blocks a section ticks at random a tick, which the test server sets to none. */
    private static final int RANDOM_TICKS = 3;
    /* The brightest light, of the open sky or a lantern itself. */
    private static final int LIGHTEST = 15;

    private MovingStructureAboardGameTests() {
    }

    /* The test server ticks no block at random; the batch that shows blocks aboard ticking does, as a world does. */
    @BeforeBatch(batch = TICKS)
    public static void randomTicksOn(final ServerLevel level) {
        level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(RANDOM_TICKS, level.getServer());
    }

    @AfterBatch(batch = TICKS)
    public static void randomTicksOff(final ServerLevel level) {
        level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, level.getServer());
    }

    @GameTest(template = ARENA, batch = "jscore_moving_aboard_multiblock", timeoutTicks = 400)
    public static void multiblock_crossesFormedAndStandsFormedSetDownTurned(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        turbine(helper);
        final List<MovingStructure> made = new ArrayList<>();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(turbineAt(helper, helper.absolutePos(TERMINAL)).merged(),
                        "the turbine forms and merges its iron"))
                .thenExecute(() -> {
                    warm(level);
                    final StructureResult result = MovingStructures.assemble(level,
                            helper.absolutePos(new BlockPos(3, 3, 3)), helper.absolutePos(new BlockPos(5, 4, 5)));
                    helper.assertTrue(result.ok() && result.blocks() == 18 && result.structure().ready(),
                            "the turbine is made a structure: " + refusal(result));
                    made.add(result.structure());
                })
                .thenExecuteAfter(20, () -> {
                    final MovingStructure structure = made.getFirst();
                    final BlockPos terminal = find(level, structure, state -> state.is(TestModels.TURBINE_BLOCK
                            .get()));
                    final MachineStructure turbine = turbineAt(helper, terminal);
                    helper.assertTrue(turbine.formed() && turbine.merged(),
                            "in the yard it stays formed and merged, never formed again");
                    final List<BlockPos> parts = all(level, structure, state -> state.getBlock()
                            instanceof FormedPartBlock);
                    helper.assertTrue(parts.size() == 17, "every iron block still merged: " + parts.size());
                    for (final BlockPos part : parts) {
                        helper.assertTrue(level.getBlockEntity(part) instanceof FormedPartBlockEntity formed
                                && terminal.equals(formed.terminal()) && formed.original().is(Blocks.IRON_BLOCK),
                                "each held by its terminal where it is now, and still iron: " + part);
                    }
                    for (final BlockPos at : BlockPos.betweenClosed(helper.absolutePos(new BlockPos(3, 3, 3)),
                            helper.absolutePos(new BlockPos(5, 4, 5)))) {
                        helper.assertTrue(level.getBlockState(at).isAir(),
                                "nothing is given back where it stood: " + level.getBlockState(at) + " at " + at);
                    }
                    final StructurePose at = structure.pose();
                    structure.teleport(StructurePose.turned(at.x(), at.y(), at.z(), 0, 1, 0, -Math.PI / 2));
                    final StructureResult down = MovingStructures.setDown(structure);
                    helper.assertTrue(down.ok() && down.blocks() == 18, "it is set down turned: " + refusal(down));
                })
                .thenExecuteAfter(20, () -> {
                    BlockPos terminal = null;
                    for (final BlockPos each : BlockPos.betweenClosed(helper.absolutePos(new BlockPos(0, 1, 0)),
                            helper.absolutePos(new BlockPos(8, 6, 8)))) {
                        if (level.getBlockState(each).is(TestModels.TURBINE_BLOCK.get())) {
                            terminal = each.immutable();
                        }
                    }
                    helper.assertTrue(terminal != null, "the turbine is on the ground again");
                    final MachineStructure turbine = turbineAt(helper, terminal);
                    helper.assertTrue(turbine.formed() && turbine.merged(), "formed and merged as it lands");
                    helper.assertTrue(turbine.orientation().orElse(Orientation.NORTH) != Orientation.NORTH,
                            "and standing turned as the structure was: " + turbine.orientation());
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = TICKS, timeoutTicks = 600)
    public static void blocks_aboardTickWithNoPlayerNear(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        for (int x = 0; x < 8; x++) {
            for (int z = 0; z < 8; z++) {
                helper.setBlock(new BlockPos(x, 3, z), Blocks.FARMLAND.defaultBlockState()
                        .setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE));
            }
        }
        helper.setBlock(new BlockPos(0, 3, 0), Blocks.FURNACE);
        final FurnaceBlockEntity furnace = (FurnaceBlockEntity) level.getBlockEntity(helper.absolutePos(
                new BlockPos(0, 3, 0)));
        helper.assertTrue(furnace != null, "a furnace is set down");
        furnace.setItem(0, new ItemStack(Items.RAW_IRON));
        furnace.setItem(1, new ItemStack(Items.COAL));
        warm(level);
        final StructureResult made = MovingStructures.assemble(level, helper.absolutePos(new BlockPos(0, 3, 0)),
                helper.absolutePos(new BlockPos(7, 3, 7)));
        helper.assertTrue(made.ok() && made.blocks() == 64 && made.structure().ready(),
                "the farmland and the furnace are made a structure: " + refusal(made));
        final MovingStructure structure = made.structure();
        helper.startSequence()
                .thenWaitUntil(() -> {
                    final BlockPos smelter = find(level, structure, state -> state.is(Blocks.FURNACE));
                    helper.assertTrue(smelter != null && level.getBlockEntity(smelter) instanceof FurnaceBlockEntity
                            box && box.getItem(2).is(Items.IRON_INGOT), "the furnace aboard smelts its iron");
                    helper.assertTrue(!all(level, structure, state -> state.is(Blocks.FARMLAND)
                            && state.getValue(FarmBlock.MOISTURE) < FarmBlock.MAX_MOISTURE).isEmpty(),
                            "and farmland aboard dries in the ticks that grow crops, far from any player");
                })
                .thenExecute(() -> MovingStructures.takeAway(structure))
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "jscore_moving_aboard_light", timeoutTicks = 200)
    public static void light_aboardIsTheSkysAndItsLamps(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        for (int x = 3; x <= 5; x++) {
            for (int z = 3; z <= 5; z++) {
                helper.setBlock(new BlockPos(x, 3, z), Blocks.STONE);
            }
        }
        helper.setBlock(new BlockPos(3, 4, 3), Blocks.LANTERN);
        warm(level);
        final StructureResult made = MovingStructures.assemble(level, helper.absolutePos(new BlockPos(3, 3, 3)),
                helper.absolutePos(new BlockPos(5, 4, 5)));
        helper.assertTrue(made.ok() && made.structure().ready(), "a floor with a lantern is made: " + refusal(made));
        final MovingStructure structure = made.structure();
        helper.startSequence()
                .thenWaitUntil(() -> {
                    final BlockPos lantern = find(level, structure, state -> state.is(Blocks.LANTERN));
                    final BlockPos open = lantern.offset(2, 0, 2);
                    helper.assertTrue(level.getBrightness(LightLayer.SKY, open) == LIGHTEST,
                            "the sky lights the floor aboard: " + level.getBrightness(LightLayer.SKY, open));
                    helper.assertTrue(level.getBrightness(LightLayer.BLOCK, lantern.east()) >= LIGHTEST - 2,
                            "and its lantern the place beside it: "
                                    + level.getBrightness(LightLayer.BLOCK, lantern.east()));
                })
                .thenExecute(() -> MovingStructures.takeAway(structure))
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "jscore_moving_aboard_cables", timeoutTicks = 200)
    public static void cables_leaveTheirGridsWhereTheyStood(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        TestCables.lay(helper, new BlockPos(3, 3, 4), SharedEnergy.INSULATED_COPPER_CABLE);
        TestCables.lay(helper, new BlockPos(4, 3, 4), SharedEnergy.INSULATED_COPPER_CABLE);
        final GridPlace was = TestCables.cable(helper, new BlockPos(3, 3, 4))
                .place(SharedEnergy.INSULATED_COPPER_CABLE.get());
        final List<MovingStructure> made = new ArrayList<>();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(contains(level, was), "the cable is in its grid"))
                .thenExecute(() -> {
                    warm(level);
                    final StructureResult result = MovingStructures.assemble(level,
                            helper.absolutePos(new BlockPos(3, 3, 4)), helper.absolutePos(new BlockPos(4, 3, 4)));
                    helper.assertTrue(result.ok() && result.structure().ready(),
                            "the cables are made a structure: " + refusal(result));
                    made.add(result.structure());
                    helper.assertTrue(!contains(level, was), "no wire is left in its grid where the cable stood");
                })
                .thenWaitUntil(() -> {
                    final BlockPos cable = find(level, made.getFirst(), state -> !state.isAir());
                    helper.assertTrue(cable != null && contains(level, GridPlace.wire(cable.asLong(),
                            was.lane())), "and the cable is in its grid where it is now, in the yard");
                })
                .thenExecute(() -> MovingStructures.takeAway(made.getFirst()))
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "jscore_moving_aboard_wires", timeoutTicks = 200)
    public static void wires_goWithTheirInsulatorsAndFallWhenOneStays(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(2, 3, 4), Blocks.CUT_COPPER);
        helper.setBlock(new BlockPos(6, 3, 4), Blocks.CUT_COPPER);
        helper.setBlock(new BlockPos(2, 3, 7), Blocks.CUT_COPPER);
        helper.assertTrue(hang(helper, new BlockPos(2, 3, 4), new BlockPos(6, 3, 4)).hung(),
                "a wire hangs between two insulators");
        helper.assertTrue(hang(helper, new BlockPos(2, 3, 4), new BlockPos(2, 3, 7)).hung(),
                "and one from the first to one that will stay");
        final BlockPos stays = helper.absolutePos(new BlockPos(2, 3, 7));
        warm(level);
        final StructureResult made = MovingStructures.assemble(level, helper.absolutePos(new BlockPos(2, 3, 4)),
                helper.absolutePos(new BlockPos(6, 3, 4)));
        helper.assertTrue(made.ok() && made.blocks() == 2 && made.structure().ready(),
                "the two insulators are made a structure: " + refusal(made));
        final List<HungWire> wires = OverheadLines.WIRES.get(level).wires();
        final List<HungWire> aboard = wires.stream().filter(wire -> YardLayout.isYardBlock(wire.from().pos().getX())
                && YardLayout.isYardBlock(wire.to().pos().getX())).toList();
        helper.assertTrue(aboard.size() == 1, "the wire between them goes with them: " + wires);
        helper.assertTrue(wires.stream().noneMatch(wire -> wire.from().pos().equals(stays)
                || wire.to().pos().equals(stays)), "the one to the insulator that stayed falls");
        final List<ItemEntity> dropped = level.getEntitiesOfClass(ItemEntity.class, new AABB(stays).inflate(2));
        helper.assertTrue(!dropped.isEmpty(), "its wire left at the insulator that stayed");
        for (final ItemEntity item : dropped) {
            item.discard();
        }
        OverheadLines.unhang(level, aboard.getFirst(), null);
        MovingStructures.takeAway(made.structure());
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "jscore_moving_aboard_many", timeoutTicks = 400)
    public static void many_moveAtOnceBesideTheServersThread(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final List<MovingStructure> many = new ArrayList<>();
        final List<StructurePose> goals = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            final BlockPos at = new BlockPos(1 + i % 6, 3, 2 + 3 * (i / 6));
            helper.setBlock(at, Blocks.STONE);
            warm(level);
            final StructureResult made = MovingStructures.assemble(level, helper.absolutePos(at),
                    helper.absolutePos(at));
            helper.assertTrue(made.ok() && made.structure().ready(), "a block is made a structure: " + refusal(made));
            final MovingStructure structure = made.structure();
            final StructurePose goal = structure.pose().movedTo(structure.pose().x(), structure.pose().y() + 3,
                    structure.pose().z());
            structure.move(new TowardMotion(goal, 2, 0));
            many.add(structure);
            goals.add(goal);
        }
        helper.startSequence()
                .thenWaitUntil(() -> {
                    for (int i = 0; i < many.size(); i++) {
                        helper.assertTrue(many.get(i).resting() && many.get(i).pose().distanceTo(goals.get(i)) < 1e-3,
                                "every one of the twelve arrives where it was sent: " + i + " at "
                                        + many.get(i).pose());
                    }
                })
                .thenExecute(() -> {
                    for (final MovingStructure structure : many) {
                        MovingStructures.takeAway(structure);
                    }
                })
                .thenSucceed();
    }

    /* The turbine: two layers of iron three by three, its terminal in the middle of the south side. */
    private static void turbine(final GameTestHelper helper) {
        for (int y = 0; y < 2; y++) {
            for (int a = -1; a <= 1; a++) {
                for (int b = 0; b <= 2; b++) {
                    final BlockPos at = TERMINAL.offset(a, y, -b);
                    if (!at.equals(TERMINAL)) {
                        helper.setBlock(at, Blocks.IRON_BLOCK);
                    }
                }
            }
        }
        helper.setBlock(TERMINAL, TestModels.TURBINE_BLOCK.get());
    }

    private static MachineStructure turbineAt(final GameTestHelper helper, final BlockPos absolute) {
        if (!(helper.getLevel().getBlockEntity(absolute) instanceof TestModels.TurbineEntity turbine)) {
            throw new IllegalStateException("no turbine at " + absolute);
        }
        return turbine.structure();
    }

    private static boolean contains(final ServerLevel level, final GridPlace place) {
        return CoreGrids.contains(level, SharedEnergy.INSULATED_COPPER_CABLE.get().grid(), place);
    }

    private static OverheadLines.Outcome hang(final GameTestHelper helper, final BlockPos from, final BlockPos to) {
        return OverheadLines.hang(null, helper.getLevel(), new WireEnd(helper.absolutePos(from), 0),
                new WireEnd(helper.absolutePos(to), 0), TestEnergies.HUNG_LINE.get(), new ItemStack(Items.STRING, 64));
    }

    /*
     * Loads the chunks round the middle of the plot the next structure gets, as the test's own doing, so a small
     * structure made next has its blocks come into the plot at once. The middle is on a chunk's edge, and the chunks
     * round a structure's are waited for too: two each way.
     */
    private static void warm(final ServerLevel level) {
        final int plot = StructureYard.nextPlot(level);
        final int cx = YardLayout.centreX(plot) >> 4;
        final int cz = YardLayout.centreZ(plot) >> 4;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.getChunk(cx + dx, cz + dz);
            }
        }
    }

    /* The first block of the structure's yard that is so, or null. */
    private static BlockPos find(final ServerLevel level, final MovingStructure structure,
                                 final Predicate<BlockState> wanted) {
        final List<BlockPos> found = all(level, structure, wanted);
        return found.isEmpty() ? null : found.getFirst();
    }

    /* Every block of the structure's yard that is so. */
    private static List<BlockPos> all(final ServerLevel level, final MovingStructure structure,
                                      final Predicate<BlockState> wanted) {
        final Box box = structure.bounds();
        final List<BlockPos> found = new ArrayList<>();
        for (final BlockPos at : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(),
                box.maxZ())) {
            if (wanted.test(level.getBlockState(at))) {
                found.add(at.immutable());
            }
        }
        return found;
    }

    private static String refusal(final StructureResult result) {
        return result.refusal() == null ? "done" : result.refusal().english();
    }
}
