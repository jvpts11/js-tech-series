/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import dev.jstech.core.jstechseries.builder.BuildJobs;
import dev.jstech.core.jstechseries.builder.BuilderPlan;
import dev.jstech.core.jstechseries.builder.BuilderState;
import dev.jstech.core.jstechseries.projector.ProjectorSettings;
import dev.jstech.core.jstechseries.projector.ProjectorView;
import dev.jstech.core.jstechseries.projector.StructureProjector;
import dev.jstech.core.jstechseries.projector.StructureProjectorBlockEntity;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.multiblock.BuildBalance;
import dev.jstech.core.multiblock.BuildOrder;
import dev.jstech.core.multiblock.FormedPartBlock;
import dev.jstech.core.multiblock.FormedPartBlockEntity;
import dev.jstech.core.multiblock.FormedParts;
import dev.jstech.core.multiblock.LevelStructures;
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.core.multiblock.ShapePreview;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.core.multiblock.shape.FormedShapes;
import dev.jstech.space.api.ShipKit;
import dev.jstech.space.api.ShipKits;
import dev.jstech.space.api.ShipParts;
import dev.jstech.space.hangar.HangarBlocks;
import dev.jstech.space.ship.AirReserveEntity;
import dev.jstech.space.ship.IFixtureEntity;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.ShipFlight;
import dev.jstech.space.ship.ShipFlightModel;
import dev.jstech.space.ship.ShipKitBlocks;
import dev.jstech.space.ship.ShipPartModels;
import dev.jstech.space.ship.SmallShipFinishes;
import dev.jstech.space.ship.SmallShipParts;
import dev.jstech.space.shipyard.ShipyardBlocks;
import dev.jstech.space.shipyard.ShipyardLayout;
import dev.jstech.space.shipyard.ShipyardPlans;
import dev.jstech.space.shipyard.ShipyardStructure;
import dev.jstech.space.shipyard.ShipyardTerminalBlockEntity;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.space.TestShipyards;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.slf4j.Logger;

/**
 * The Small Shipyard: a projector shows it dug into the ground it stands on; built whole, dug in, it forms, its blocks
 * merge into its model and take its shapes (a walkway its grating, a tower its lattice), its item port staying
 * itself; a block in its pit undoes it, and
 * clearing the pit forms it again; a block of it taken away undoes it and its other blocks come back as they were;
 * and cleared all at once, as a blast or a fill clears it, it is gone in a moment. A ship sent to it is laid out over
 * its pit as the dock stands, and built by hand: each part forms by itself as its last block goes in, shaped as its
 * model, and holds round a hole where one is taken away, mended as it is put back. Built, it goes as one structure;
 * the dock takes it back onto the grid to change it, and lets it go again; and aboard, wherever it is, a hole is
 * mended the same way.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ShipyardGameTests {

    private static final String ARENA = "shipyard";
    private static final String BATCH = "jsspace_shipyard";
    /* The pattern's lowest corner, the pit floor's west front corner, a ground of stone under it. */
    private static final BlockPos CORNER = new BlockPos(1, 2, 1);
    /* Places of the dock, in its own blocks: a tower's foot, a walkway, the item port, the pit, the stair's foot. */
    private static final BlockPos TOWER = dock(-11, 1, -13);
    private static final BlockPos WALKWAY = dock(-10, 3, 0);
    private static final BlockPos PORT = dock(10, 0, 9);
    private static final BlockPos PIT = dock(-5, -2, -9);
    /* Over half the block: a deck lies at its block's top. */
    private static final double DECK = 0.5;
    /* The pattern's size, less a block each way: from its lowest corner to its highest. */
    private static final BlockPos SIZE = new BlockPos(22, 14, 26);
    /*
     * How long clearing the whole formed dock in one tick may take, in milliseconds: far over what it takes, a few
     * hundred, and far under the minutes it took while each loss worked the struck model out again.
     */
    private static final long CLEARED_WITHIN = 10_000;
    private static final long NANOS_A_MILLI = 1_000_000;
    /* Corridors enough to make a ship longer than the pit, which flies all the same. */
    private static final int LONG_SHIP = 6;
    /* A tick, in milliseconds: what letting a ship go, and its blocks coming in, must each fit in. */
    private static final long ONE_TICK = 50;
    /* How many ships are let go and timed after the first. */
    private static final int TIMED = 3;
    /* How far from its dock's terminal a ship let go stands, at most, in blocks. */
    private static final double SHIP_REACH = 32;
    /* One block of a part's dozens holed, the ship still weighs nearly all it did. */
    private static final double HOLED_AT_LEAST = 0.9;
    private static final Logger LOGGER = LogUtils.getLogger();
    /* A dock turned a quarter, in an arena as wide as the dock is long. */
    private static final String TURNED = "shipyard_turned";
    /*
     * A projector on an empty arena's floor, as a player south of it places it; the dock's pit, in layers dug under
     * the ground, and its terminal's height over the ground.
     */
    private static final String EMPTY = "empty";
    private static final BlockPos PROJECTOR = new BlockPos(2, 2, 5);
    private static final int PIT_LAYERS = 3;
    private static final int BOOTH_ABOVE = 4;
    /*
     * The builder's tests: a ground of earth four blocks deep, grass on top; a projector on it at the arena's south
     * side, the dock north of it, in the arena as long as the dock stands along it or across it; the build's pace
     * while they run, high enough for a dock of some sixteen hundred blocks taken out and put in to go in seconds.
     */
    private static final int EARTH_TOP = 4;
    private static final BlockPos PROJECTOR_ALONG = new BlockPos(12, 5, 28);
    private static final BlockPos PROJECTOR_ACROSS = new BlockPos(14, 5, 24);
    private static final int ALONG_WIDE = 26;
    private static final int ACROSS_WIDE = 30;
    private static final int DOCK_PACE = 1_000;
    private static final int BUILD_TIME = 1_200;
    /* A ship ready to fly with a workshop, whose kit has blocks of the game in it. */
    private static final ShipDesign WORKSHOP_SHIP = new ShipDesign("JS-02", List.of(part("orbiter_cockpit"),
            part("airlock"), part("quarters"), part("workshop"), part("reactor_flush")), List.of(part("twin_bells"),
            part("side_intakes"), part("blended_delta"), part("twin_canted_fins"), part("blisters"), part("wheels")));

    private ShipyardGameTests() {
    }

    @GameTest(template = EMPTY)
    public static void shipyard_isProjectedDugIntoTheGroundTheProjectorStandsOn(final GameTestHelper helper) {
        helper.setBlock(PROJECTOR, StructureProjector.BLOCK.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        if (!(helper.getBlockEntity(PROJECTOR) instanceof StructureProjectorBlockEntity projector)) {
            throw new IllegalStateException("no projector where the test put it");
        }
        projector.apply(ProjectorSettings.FRESH.choosing(ShipyardStructure.ID));
        final ShapePreview preview = projector.preview().orElseThrow();
        helper.assertTrue(preview.ground() - preview.minY() == PIT_LAYERS,
                "the dock's pit is three layers dug under the ground; got " + (preview.ground() - preview.minY()));
        final int above = projector.terminalOf(preview).getY() - helper.absolutePos(PROJECTOR).getY();
        helper.assertTrue(above == BOOTH_ABOVE, "its booth's terminal stands four blocks over the ground the projector"
                + " stands on, not over the pit's floor; got " + above);
        helper.assertTrue(!preview.clears().isEmpty() && preview.clears().stream()
                .allMatch(place -> place.y() < preview.ground()), "and its pit, under the ground, is to be left empty");
        helper.succeed();
    }

    /*
     * The Multiblock Builder, aimed at a projector showing the dock on a ground of earth, digs its pit, puts its rim
     * and its cradle's feet in the earth in place of what stood there, and builds the rest, its terminal last, turned
     * as the dock stands; and the dock forms, whichever of the four ways the projector turns it. Each in a batch of its
     * own, as each turns the build's pace up while it runs.
     */
    @GameTest(template = ARENA, batch = "jsspace_dock_north", timeoutTicks = BUILD_TIME)
    public static void builder_buildsTheDockAProjectorShowsTurnedNorth(final GameTestHelper helper) {
        buildsTheDock(helper, 0, PROJECTOR_ALONG);
    }

    @GameTest(template = TURNED, batch = "jsspace_dock_east", timeoutTicks = BUILD_TIME)
    public static void builder_buildsTheDockAProjectorShowsTurnedEast(final GameTestHelper helper) {
        buildsTheDock(helper, 1, PROJECTOR_ACROSS);
    }

    @GameTest(template = ARENA, batch = "jsspace_dock_south", timeoutTicks = BUILD_TIME)
    public static void builder_buildsTheDockAProjectorShowsTurnedSouth(final GameTestHelper helper) {
        buildsTheDock(helper, 2, PROJECTOR_ALONG);
    }

    @GameTest(template = TURNED, batch = "jsspace_dock_west", timeoutTicks = BUILD_TIME)
    public static void builder_buildsTheDockAProjectorShowsTurnedWest(final GameTestHelper helper) {
        buildsTheDock(helper, 3, PROJECTOR_ACROSS);
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void shipyard_formsWhenBuiltWholeAndItsBlocksTakeTheDocksShapes(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        helper.succeedWhen(() -> {
            helper.assertTrue(formed(level, terminal), "the dock forms whole");
            helper.assertBlockPresent(FormedParts.BLOCK.get(), TOWER);
            helper.assertBlockPresent(FormedParts.BLOCK.get(), WALKWAY);
            helper.assertBlockPresent(HangarBlocks.ITEM_PORT.get(), PORT);
            final BlockPos walkway = helper.absolutePos(WALKWAY);
            final VoxelShape deck = level.getBlockState(walkway).getCollisionShape(level, walkway);
            helper.assertTrue(!deck.isEmpty() && deck.min(Direction.Axis.Y) > DECK,
                    "a walkway is its grating at its block's top: " + deck.bounds());
            final BlockPos tower = helper.absolutePos(TOWER);
            final VoxelShape lattice = level.getBlockState(tower).getCollisionShape(level, tower);
            helper.assertTrue(!lattice.isEmpty() && !Block.isShapeFullBlock(lattice),
                    "a tower is its lattice, not a whole block");
        });
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void shipyard_aBlockInItsPitUndoesItAndClearingThePitFormsItAgain(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(formed(level, terminal), "the dock forms"))
                .thenExecute(() -> helper.setBlock(PIT, Blocks.DIRT))
                .thenWaitUntil(() -> helper.assertTrue(!formed(level, terminal), "a block in the pit undoes it"))
                .thenExecute(() -> helper.setBlock(PIT, Blocks.AIR))
                .thenWaitUntil(() -> helper.assertTrue(formed(level, terminal), "the pit cleared, it forms again"))
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void shipyard_aBlockTakenAwayUndoesItAndItsBlocksComeBack(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(formed(level, terminal), "the dock forms"))
                .thenExecute(() -> helper.destroyBlock(TOWER))
                .thenWaitUntil(() -> {
                    helper.assertTrue(!formed(level, terminal), "a tower block taken away undoes it");
                    helper.assertBlockPresent(ShipyardBlocks.WALKWAY.get(), WALKWAY);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void shipyard_clearedAllAtOnceIsClearedAtOnce(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(formed(level, terminal), "the dock forms"))
                .thenExecute(() -> {
                    // Every block of the formed dock taken in one tick, as a blast or a fill takes them: each lost
                    // block marks the ones beside it, which must not work the struck model out again each time.
                    final long start = System.nanoTime();
                    for (final BlockPos at : BlockPos.betweenClosed(helper.absolutePos(CORNER),
                            helper.absolutePos(CORNER.offset(SIZE)))) {
                        level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                    final long millis = (System.nanoTime() - start) / NANOS_A_MILLI;
                    helper.assertTrue(millis < CLEARED_WITHIN, "the dock cleared in " + millis + " ms");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void shipyard_keepsItsDesignSavesItByNameAndSendsOnlyAReadyShipThatFits(
            final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        final ShipDesign ready = new ShipDesign("JS-01", List.of(part("orbiter_cockpit"), part("airlock"),
                part("quarters"), part("reactor_flush")), List.of(part("twin_bells"), part("side_intakes"),
                part("blended_delta"), part("twin_canted_fins"), part("blisters"), part("wheels")));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(formed(level, terminal), "the dock forms"))
                .thenExecute(() -> {
                    final ShipyardTerminalBlockEntity dock = (ShipyardTerminalBlockEntity) level
                            .getBlockEntity(terminal);
                    dock.design(ready.withHung(List.of()));
                    helper.assertTrue(!dock.confirm() && dock.plan().isEmpty(),
                            "a ship with no engines, wings or gear is not sent");
                    dock.design(ready);
                    helper.assertTrue(dock.design().equals(ready), "it keeps the design on its screen");
                    helper.assertTrue(dock.save() && dock.plans().size() == 1 && dock.save()
                            && dock.plans().size() == 1, "saved twice by its name, it is one plan");
                    final List<ResourceLocation> longer = new ArrayList<>(ready.spine());
                    for (int k = 0; k < LONG_SHIP; k++) {
                        longer.add(1, part("corridor"));
                    }
                    final ShipDesign tooLong = ready.withSpine(longer, List.of());
                    helper.assertTrue(tooLong.problems(ShipParts.all(level)).isEmpty()
                            && !ShipyardTerminalBlockEntity.ready(level, tooLong), "a ship longer than the pit"
                            + " flies but does not fit the dock");
                    helper.assertTrue(dock.confirm() && dock.plan().orElseThrow().equals(ready),
                            "the ready ship that fits is sent to the dock");
                    final ShipDesign flipped = ready.withSpine(ready.spine(), List.of(1));
                    helper.assertTrue(ShipyardLayout.laid(ShipParts.all(level), ShipKits.all(level), flipped)
                            .cells().size() == ShipyardLayout.laid(ShipParts.all(level), ShipKits.all(level), ready)
                            .cells().size(), "a section flipped left for right lays as many blocks");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void shipyard_buildsTheShipSentByHandEachPartFormingAsItsLastBlockGoesIn(
            final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(formed(level, terminal), "the dock forms"))
                .thenExecute(() -> {
                    final ShipyardTerminalBlockEntity dock = (ShipyardTerminalBlockEntity) level
                            .getBlockEntity(terminal);
                    dock.design(WORKSHOP_SHIP);
                    helper.assertTrue(dock.confirm(), "a ready ship that fits is sent to the dock");
                    final PlannedBuild plan = dock.build().orElseThrow();
                    helper.assertTrue(plan.plan().parts() == WORKSHOP_SHIP.spine().size()
                            + WORKSHOP_SHIP.hung().size() && dock.formedParts().isEmpty(), "its plan counts every part"
                            + " of the ship, none formed");
                    for (final PlanSpot spot : plan.plan().spots()) {
                        helper.assertTrue(level.getBlockState(plan.where(spot)).isAir(), "each of its blocks goes"
                                + " in the bay's clear room: " + plan.where(spot));
                    }
                    final List<PlanSpot> cockpit = spots(plan, 0);
                    for (int i = 0; i < cockpit.size() - 1; i++) {
                        lay(level, plan, cockpit.get(i));
                    }
                    helper.assertTrue(!plan.formed(0) && plan.missing(0) == 1, "a block short, the cockpit waits");
                    lay(level, plan, cockpit.getLast());
                    helper.assertTrue(plan.formed(0) && dock.formedParts().equals(List.of(0))
                            && dock.missingParts().getFirst() == 0, "its last block in, the cockpit forms by itself,"
                            + " and the dock's players are told: " + dock.formedParts());
                    helper.assertTrue(cockpit.stream().anyMatch(spot -> level.getBlockEntity(plan.where(spot))
                            instanceof FormedPartBlockEntity merged && merged.place() != null
                            && !Block.isShapeFullBlock(merged.shape())), "its blocks merged into its model, shaped as"
                            + " their parts of it");
                    final PlanSpot taken = cockpit.stream().filter(spot -> spot.place().isPresent()).findFirst()
                            .orElseThrow();
                    level.setBlockAndUpdate(plan.where(taken), Blocks.AIR.defaultBlockState());
                    final int hole = plan.plan().spots().indexOf(taken);
                    helper.assertTrue(plan.formed(0) && dock.holes().equals(List.of(hole)), "a block taken away,"
                            + " the cockpit holds round the hole: " + dock.holes());
                    lay(level, plan, taken);
                    helper.assertTrue(plan.formed(0) && dock.holes().isEmpty() && level.getBlockState(plan.where(
                            taken)).getBlock() instanceof FormedPartBlock, "the block put back merges and mends it");
                    for (final PlanSpot spot : plan.plan().spots()) {
                        lay(level, plan, spot);
                    }
                    helper.assertTrue(plan.complete() && dock.built(), "every block in, the whole ship is formed");
                    final PlanSpot table = plan.plan().spots().stream().filter(spot -> spot.block().equals(
                            ShipKitBlocks.SHIP_CRAFTING_TABLE.getId())).findFirst().orElseThrow();
                    helper.assertTrue(level.getBlockState(plan.where(table)).is(ShipKitBlocks.SHIP_CRAFTING_TABLE.get())
                            && level.getBlockEntity(plan.where(table)) instanceof IFixtureEntity fixture
                            && fixture.places().equals(List.of(table.place().orElseThrow())), "the workshop's"
                            + " crafting table keeps itself, standing as its block of the workshop's model");
                    level.destroyBlock(terminal, false);
                    helper.assertTrue(cockpit.stream().noneMatch(spot -> level.getBlockState(plan.where(spot))
                            .getBlock() instanceof FormedPartBlock), "the terminal broken, the ship comes apart");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void shipyard_keepsCountingItsShipAfterTheDockChecksItselfAgain(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        final PlannedBuild[] plan = new PlannedBuild[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(formed(level, terminal), "the dock forms"))
                .thenExecute(() -> {
                    final ShipyardTerminalBlockEntity dock = (ShipyardTerminalBlockEntity) level
                            .getBlockEntity(terminal);
                    dock.design(WORKSHOP_SHIP);
                    helper.assertTrue(dock.confirm(), "the ship is sent");
                    plan[0] = dock.build().orElseThrow();
                    // A block of the dock taken and put back: the dock comes apart and checks itself again.
                    helper.setBlock(TOWER, Blocks.AIR);
                })
                .thenWaitUntil(() -> helper.assertTrue(!formed(level, terminal), "the dock comes apart"))
                .thenExecute(() -> helper.setBlock(TOWER, ShipyardBlocks.TOWER.get()))
                .thenWaitUntil(() -> helper.assertTrue(formed(level, terminal), "and forms again"))
                .thenExecute(() -> {
                    for (final PlanSpot spot : spots(plan[0], 0)) {
                        lay(level, plan[0], spot);
                    }
                    helper.assertTrue(plan[0].formed(0), "the ship's plan still counts what goes in: its cockpit"
                            + " forms as its last block goes in");
                })
                .thenSucceed();
    }

    /*
     * In a batch of its own: the tick its ship's blocks come in is timed whole, so no other test may run in it and be
     * charged to the ship, as on a slower machine it was. The first ship goes as the game first does it; of the three
     * after it the fastest is held to a tick, so a pause of the machine's own, a collection or another program, is
     * not charged to the ship either.
     */
    @GameTest(template = ARENA, batch = BATCH + "_timed", timeoutTicks = 1000)
    public static void shipyard_letsTheBuiltShipGoAsOneStructureOfItsPlansBlocks(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        final GameTestSequence sequence = helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(formed(level, terminal), "the dock forms"));
        final long[] first = new long[2];
        final long[][] then = new long[TIMED][2];
        letGo(helper, terminal, sequence, first);
        for (final long[] each : then) {
            letGo(helper, terminal, sequence, each);
        }
        sequence.thenExecute(() -> {
            final long released = Arrays.stream(then).mapToLong(each -> each[0]).min().orElse(Long.MAX_VALUE);
            final long blocks = Arrays.stream(then).mapToLong(each -> each[1]).min().orElse(Long.MAX_VALUE);
            LOGGER.info("A ship let go in {} ms, its blocks in a tick of {} ms; the fastest after in {} ms and {} ms",
                    first[0], first[1], released, blocks);
            helper.assertTrue(released < ONE_TICK && blocks < ONE_TICK, "a ship goes within a tick: let go in "
                    + released + " ms, its blocks in a tick of " + blocks + " ms, the fastest of " + TIMED);
        }).thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 600)
    public static void shipyard_takesItsShipBackToChangeItAndAHoleAboardIsMended(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        final GameTestSequence sequence = helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(formed(level, terminal), "the dock forms"));
        letGo(helper, terminal, sequence, new long[2]);
        final MovingStructure[] ship = new MovingStructure[1];
        final BlockPos[] hull = new BlockPos[1];
        final BlockState[] block = new BlockState[1];
        sequence.thenExecute(() -> {
            // A hull block of the cockpit broken aboard, where the ship's blocks stand in its structure.
            final PlannedBuild laid = new PlannedBuild(ShipyardPlans.of(level, WORKSHOP_SHIP, Orientation.NORTH),
                    ShipyardStructure.shipAt(terminal, Orientation.NORTH));
            final PlanSpot spot = laid.plan().spots().stream().filter(each -> each.part() == 0
                    && each.place().isPresent()).findFirst().orElseThrow();
            // This dock's ship, the one standing where its cockpit was built, not a ship of a test beside it.
            final Vec3 built = Vec3.atCenterOf(laid.where(spot));
            ship[0] = (MovingStructure) MovingStructures.near(level, new AABB(terminal).inflate(SHIP_REACH)).stream()
                    .filter(each -> each instanceof MovingStructure && each.worldBox().inflate(1).contains(built))
                    .findFirst().orElseThrow();
            hull[0] = BlockPos.containing(MovingStructures.toYard(ship[0], built));
            block[0] = FormedPartBlock.originalAt(level, hull[0], level.getBlockState(hull[0]));
        }).thenWaitUntil(() -> helper.assertTrue(console(level, ship[0]).holes().isEmpty()
                && console(level, ship[0]).ship().isPresent(), "the ship's console watches its plan aboard"))
                .thenExecute(() -> level.setBlockAndUpdate(hull[0], Blocks.AIR.defaultBlockState()))
                .thenWaitUntil(() -> helper.assertTrue(console(level, ship[0]).holes().size() == 1
                        && console(level, ship[0]).ship().orElseThrow().formed().contains(0), "a block broken"
                        + " aboard, its part holds round the hole"))
                .thenExecute(() -> {
                    final ShipFlightModel.Performance whole = ShipFlight.performance(WORKSHOP_SHIP,
                            ShipParts.all(level));
                    final ShipFlightModel.Performance holed = console(level, ship[0]).performance().orElseThrow();
                    helper.assertTrue(holed.mass() < whole.mass() && holed.mass() > whole.mass() * HOLED_AT_LEAST,
                            "the holed part counts for the share of its blocks still in it: " + holed.mass() + " of "
                                    + whole.mass());
                    level.setBlockAndUpdate(hull[0], block[0]);
                })
                .thenWaitUntil(() -> helper.assertTrue(console(level, ship[0]).holes().isEmpty()
                        && console(level, ship[0]).performance().isEmpty(), "the block put back aboard mends it, and "
                        + "the ship does all it did"))
                .thenExecute(() -> {
                    final ShipyardTerminalBlockEntity dock = (ShipyardTerminalBlockEntity) level
                            .getBlockEntity(terminal);
                    // Its reactor's deuterium burnt away in flight.
                    console(level, ship[0]).burn(1);
                    helper.assertTrue(dock.seat(ship[0]), "the dock takes its ship back onto the grid");
                    final PlannedBuild plan = dock.build().orElseThrow();
                    final AirReserveEntity docked = plan.plan().spots().stream().filter(spot -> spot.block()
                            .equals(ShipKitBlocks.FLIGHT_CONSOLE.getId())).map(spot -> level.getBlockEntity(
                            plan.where(spot))).filter(AirReserveEntity.class::isInstance)
                            .map(AirReserveEntity.class::cast).findFirst().orElseThrow();
                    helper.assertTrue(docked.deuterium() == 1, "the dock fills its reactor's deuterium again");
                    helper.assertTrue(dock.plan().orElseThrow().equals(WORKSHOP_SHIP) && plan.complete()
                            && dock.built(), "set down where its plan goes, the ship stands whole, its plan the"
                            + " dock's again");
                    dock.design(WORKSHOP_SHIP.painted(SmallShipFinishes.id("enamel")));
                    helper.assertTrue(dock.confirm() && dock.built(), "changed, its parts form again over its"
                            + " blocks");
                    helper.assertTrue(dock.release(null).isPresent(), "and it goes again, a structure once more");
                })
                .thenSucceed();
    }

    @GameTest(template = TURNED, batch = BATCH, timeoutTicks = 400)
    public static void shipyard_turnedLaysItsShipOverItsPitAsItStands(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER), Orientation.EAST);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(formed(level, terminal), "the dock turned forms"))
                .thenExecute(() -> {
                    final ShipyardTerminalBlockEntity dock = (ShipyardTerminalBlockEntity) level
                            .getBlockEntity(terminal);
                    dock.design(WORKSHOP_SHIP);
                    helper.assertTrue(dock.confirm() && dock.planTurn().equals(Orientation.EAST),
                            "a ship sent to a dock turned is turned as it stands");
                    final PlannedBuild plan = dock.build().orElseThrow();
                    for (final PlanSpot spot : plan.plan().spots()) {
                        helper.assertTrue(level.getBlockState(plan.where(spot)).isAir(), "each of its blocks goes"
                                + " in the bay's clear room: " + plan.where(spot));
                    }
                    for (final PlanSpot spot : spots(plan, 0)) {
                        lay(level, plan, spot);
                    }
                    helper.assertTrue(plan.formed(0), "its cockpit forms as in a dock facing north");
                })
                .thenSucceed();
    }

    @GameTest(template = "empty")
    public static void shapes_everyPieceOfTheShipsKitsHasItsShapeFile(final GameTestHelper helper) {
        ShipKits.all(helper.getLevel()).forEach((part, kit) -> {
            for (final ShipKit.Piece piece : kit.pieces()) {
                helper.assertTrue(FormedShapes.get(helper.getLevel(), ShipPartModels.shape(part, kit, piece))
                        .isPresent(), "the piece " + ShipPartModels.shape(part, kit, piece) + " has its shapes");
            }
        });
        helper.succeed();
    }

    /*
     * Builds the workshop ship in the dock and lets it go, checking it goes whole as one structure of its plan's
     * blocks, the dock staying; {@code millis} told how long letting it go took, and the tick its blocks came in.
     */
    private static void letGo(final GameTestHelper helper, final BlockPos terminal, final GameTestSequence sequence,
                              final long[] millis) {
        final ServerLevel level = helper.getLevel();
        final List<BlockPos> places = new ArrayList<>();
        final MovingStructure[] ship = new MovingStructure[1];
        final long[] clock = new long[1];
        sequence.thenExecute(() -> {
            final ShipyardTerminalBlockEntity dock = (ShipyardTerminalBlockEntity) level.getBlockEntity(terminal);
            dock.design(WORKSHOP_SHIP);
            helper.assertTrue(dock.confirm(), "the ship is sent");
            final PlannedBuild plan = dock.build().orElseThrow();
            helper.assertTrue(dock.release(null).isEmpty(), "a ship not yet whole does not go");
            for (final PlanSpot spot : plan.plan().spots()) {
                lay(level, plan, spot);
                places.add(plan.where(spot));
            }
            final BlockPos console = places.get(plan.plan().spots().indexOf(plan.plan().spots().stream()
                    .filter(spot -> spot.block().equals(ShipKitBlocks.FLIGHT_CONSOLE.getId())).findFirst()
                    .orElseThrow()));
            helper.assertTrue(terminal.equals(LevelStructures.of(level).terminalHolding(console)),
                    "its flight console knows the dock that holds it");
            final long start = System.nanoTime();
            ship[0] = dock.release(null).orElseThrow();
            millis[0] = (System.nanoTime() - start) / NANOS_A_MILLI;
            clock[0] = System.nanoTime();
            // Its blocks come in at once where its plot's land is already loaded, a plot another ship gave back.
            helper.assertTrue(dock.plan().isEmpty() && (ship[0].arriving() || ship[0].ready()), "the whole ship"
                    + " goes, a structure from now on, and the dock forgets it");
        }).thenWaitUntil(() -> {
            final long now = System.nanoTime();
            if (ship[0].arriving()) {
                clock[0] = now;
                helper.fail("its blocks to come into the structure");
            }
            millis[1] = (now - clock[0]) / NANOS_A_MILLI;
        }).thenExecute(() -> {
            helper.assertTrue(ship[0].blocks() == places.size(), "every block of its plan went, and none other: "
                    + ship[0].blocks() + " of " + places.size());
            helper.assertTrue(places.stream().allMatch(place -> level.getBlockState(place).isAir()),
                    "its places in the bay are empty");
            helper.assertTrue(formed(level, terminal) && level.getBlockState(helper.absolutePos(TOWER))
                    .is(FormedParts.BLOCK.get()), "the dock stays, formed, its blocks where they stood");
        });
    }

    /* The flight console of {@code ship}, which keeps its plan aboard. */
    private static AirReserveEntity console(final ServerLevel level, final MovingStructure ship) {
        for (final BlockPos at : BlockPos.betweenClosed(ship.bounds().minX(), ship.bounds().minY(),
                ship.bounds().minZ(), ship.bounds().maxX(), ship.bounds().maxY(), ship.bounds().maxZ())) {
            if (level.getBlockEntity(at) instanceof AirReserveEntity keeper) {
                return keeper;
            }
        }
        throw new IllegalStateException("a ship with no flight console");
    }

    private static boolean formed(final ServerLevel level, final BlockPos terminal) {
        return level.getBlockEntity(terminal) instanceof ShipyardTerminalBlockEntity dock && dock.formed();
    }

    /*
     * Lays a ground of earth over the arena's floor, a projector on it at {@code at} turning the dock {@code turn}
     * quarters, and a player in creative beside it with the Multiblock Builder aimed at the projector; once the dock
     * the builder built forms, its terminal must face the booth's back as the dock stands.
     */
    private static void buildsTheDock(final GameTestHelper helper, final int turn, final BlockPos at) {
        final ServerLevel level = helper.getLevel();
        // The arena is as long, along z, as the projector stands from its north side, and a block more.
        final int deep = at.getZ() + 2;
        final int wide = deep == PROJECTOR_ALONG.getZ() + 2 ? ALONG_WIDE : ACROSS_WIDE;
        for (int x = 0; x < wide; x++) {
            for (int z = 0; z < deep; z++) {
                for (int y = 1; y <= EARTH_TOP; y++) {
                    level.setBlock(helper.absolutePos(new BlockPos(x, y, z)), (y == EARTH_TOP ? Blocks.GRASS_BLOCK
                            : Blocks.DIRT).defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
        helper.setBlock(at, StructureProjector.BLOCK.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        if (!(helper.getBlockEntity(at) instanceof StructureProjectorBlockEntity projector)) {
            throw new IllegalStateException("no projector where the test put it");
        }
        projector.apply(new ProjectorSettings(Optional.of(ShipyardStructure.ID), turn, false, 0, false, true));
        final BlockPos terminal = projector.terminalOf(projector.preview().orElseThrow());
        final FakePlayer player = FakePlayerFactory.get(level, new GameProfile(UUID.nameUUIDFromBytes(
                ("dock-builder-" + turn).getBytes(StandardCharsets.UTF_8)), "dock-builder-" + turn));
        player.getAbilities().instabuild = true;
        final BlockPos stand = helper.absolutePos(at.south());
        player.setPos(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5);
        final BuilderPlan plan = BuilderPlan.of(level, helper.absolutePos(at), player, BuilderState.DEFAULT)
                .orElseThrow();
        final BuildOrder.Want last = plan.wants().getLast();
        helper.assertTrue(new BlockPos(last.x(), last.y(), last.z()).equals(terminal),
                "its terminal is the last block it places, the dock round it first");
        helper.assertTrue(!plan.toTakeOut().isEmpty(), "the earth where its pit and its rim go is to be taken out");
        final int pace = BuildBalance.blocksPerSecond();
        BuildBalance.setBlocksPerSecond(DOCK_PACE);
        BuildJobs.start(player, helper.absolutePos(at), BuilderState.DEFAULT);
        // Should it never form, the pace and the job are let go just before the test runs out, not left to the next.
        helper.runAfterDelay(BUILD_TIME - 5, () -> {
            BuildBalance.setBlocksPerSecond(pace);
            BuildJobs.stop(player);
        });
        final int[] back = Orientation.turns().get(turn).apply(1, 0, 0);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(formed(level, terminal), "the dock the builder built forms"))
                .thenExecute(() -> {
                    BuildBalance.setBlocksPerSecond(pace);
                    helper.assertTrue(level.getBlockState(terminal).getValue(HorizontalDirectionalBlock.FACING)
                            == Direction.fromDelta(back[0], back[1], back[2]),
                            "its terminal faces the booth's back as the dock stands");
                    helper.assertTrue(BuildJobs.building(player).isEmpty(), "and the builder is done with it");
                    final ProjectorView seen = ProjectorView.of(level, projector).orElseThrow();
                    helper.assertTrue(seen.missing() == 0 && seen.wrong() == 0 && seen.toClear() == 0,
                            "the projector sees it whole, its merged blocks as the blocks they were: "
                                    + seen.missing() + " missing, " + seen.wrong() + " wrong, " + seen.toClear()
                                    + " to clear");
                })
                .thenSucceed();
    }

    /* The places of the part {@code part} of {@code plan}. */
    private static List<PlanSpot> spots(final PlannedBuild plan, final int part) {
        return plan.plan().spots().stream().filter(spot -> spot.part() == part).toList();
    }

    /* Lays the block {@code plan} wants at {@code spot}. */
    private static void lay(final ServerLevel level, final PlannedBuild plan, final PlanSpot spot) {
        level.setBlockAndUpdate(plan.where(spot), BuiltInRegistries.BLOCK.get(spot.block()).defaultBlockState());
    }

    private static ResourceLocation part(final String path) {
        return SmallShipParts.id("small_" + path);
    }

    /* A place of the dock in its own blocks, across from the bay's middle, up from the ground, along from the front. */
    private static BlockPos dock(final int x, final int y, final int z) {
        return CORNER.offset(x + 11, y + 3, z + 13);
    }
}
