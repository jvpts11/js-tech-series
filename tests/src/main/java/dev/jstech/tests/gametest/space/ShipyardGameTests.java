/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import com.mojang.logging.LogUtils;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.multiblock.FormedPartBlock;
import dev.jstech.core.multiblock.FormedPartBlockEntity;
import dev.jstech.core.multiblock.FormedParts;
import dev.jstech.core.multiblock.LevelStructures;
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.core.multiblock.shape.FormedShapes;
import dev.jstech.space.api.ShipKit;
import dev.jstech.space.api.ShipKits;
import dev.jstech.space.api.ShipParts;
import dev.jstech.space.hangar.HangarBlocks;
import dev.jstech.space.ship.AirReserveEntity;
import dev.jstech.space.ship.ShipDesign;
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
import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.slf4j.Logger;

/**
 * The Small Shipyard: built whole, dug into the ground, it forms, its blocks merge into its model and take its shapes
 * (a walkway its grating, a tower its lattice), its item port staying itself; a block in its pit undoes it, and
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
    /* How far from its dock's terminal a ship let go stands, at most, in blocks. */
    private static final double SHIP_REACH = 32;
    private static final Logger LOGGER = LogUtils.getLogger();
    /* A dock turned a quarter, in an arena as wide as the dock is long. */
    private static final String TURNED = "shipyard_turned";
    /* A ship ready to fly with a workshop, whose kit has blocks of the game in it. */
    private static final ShipDesign WORKSHOP_SHIP = new ShipDesign("JS-02", List.of(part("orbiter_cockpit"),
            part("airlock"), part("quarters"), part("workshop"), part("reactor_flush")), List.of(part("twin_bells"),
            part("side_intakes"), part("blended_delta"), part("twin_canted_fins"), part("blisters"), part("wheels")));

    private ShipyardGameTests() {
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
                    final PlanSpot table = plan.plan().spots().stream().filter(spot -> spot.place().isEmpty())
                            .findFirst().orElseThrow();
                    helper.assertTrue(level.getBlockState(plan.where(table)).is(BuiltInRegistries.BLOCK.get(
                            table.block())), "a block of the game in the workshop's kit keeps itself");
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

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void shipyard_letsTheBuiltShipGoAsOneStructureOfItsPlansBlocks(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        final GameTestSequence sequence = helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(formed(level, terminal), "the dock forms"));
        // Twice over: the first ship goes as the game first does it, the second as it goes from then on.
        final long[] first = new long[2];
        final long[] then = new long[2];
        letGo(helper, terminal, sequence, first);
        letGo(helper, terminal, sequence, then);
        sequence.thenExecute(() -> {
            LOGGER.info("A ship let go in {} ms, its blocks in a tick of {} ms; the next in {} ms and {} ms",
                    first[0], first[1], then[0], then[1]);
            helper.assertTrue(then[0] < ONE_TICK && then[1] < ONE_TICK, "a ship goes within a tick: let go in "
                    + then[0] + " ms, its blocks in a tick of " + then[1] + " ms");
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
            ship[0] = (MovingStructure) MovingStructures.near(level, new AABB(terminal).inflate(SHIP_REACH)).stream()
                    .filter(MovingStructure.class::isInstance).findFirst().orElseThrow();
            // A hull block of the cockpit broken aboard, where the ship's blocks stand in its structure.
            final PlannedBuild laid = new PlannedBuild(ShipyardPlans.of(level, WORKSHOP_SHIP, Orientation.NORTH),
                    ShipyardStructure.shipAt(terminal, Orientation.NORTH));
            final PlanSpot spot = laid.plan().spots().stream().filter(each -> each.part() == 0
                    && each.place().isPresent()).findFirst().orElseThrow();
            hull[0] = BlockPos.containing(MovingStructures.toYard(ship[0], Vec3.atCenterOf(laid.where(spot))));
            block[0] = FormedPartBlock.originalAt(level, hull[0], level.getBlockState(hull[0]));
        }).thenWaitUntil(() -> helper.assertTrue(console(level, ship[0]).holes().isEmpty()
                && console(level, ship[0]).ship().isPresent(), "the ship's console watches its plan aboard"))
                .thenExecute(() -> level.setBlockAndUpdate(hull[0], Blocks.AIR.defaultBlockState()))
                .thenWaitUntil(() -> helper.assertTrue(console(level, ship[0]).holes().size() == 1
                        && console(level, ship[0]).ship().orElseThrow().formed().contains(0), "a block broken"
                        + " aboard, its part holds round the hole"))
                .thenExecute(() -> level.setBlockAndUpdate(hull[0], block[0]))
                .thenWaitUntil(() -> helper.assertTrue(console(level, ship[0]).holes().isEmpty(), "the block put"
                        + " back aboard mends it"))
                .thenExecute(() -> {
                    final ShipyardTerminalBlockEntity dock = (ShipyardTerminalBlockEntity) level
                            .getBlockEntity(terminal);
                    helper.assertTrue(dock.seat(ship[0]), "the dock takes its ship back onto the grid");
                    final PlannedBuild plan = dock.build().orElseThrow();
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
            helper.assertTrue(dock.plan().isEmpty() && ship[0].arriving(), "the whole ship goes, a structure from"
                    + " now on, and the dock forgets it");
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
