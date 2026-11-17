/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.core.multiblock.shape.CellSpace;
import dev.jstech.core.multiblock.shape.FormedDoors;
import dev.jstech.core.multiblock.shape.FormedPlace;
import dev.jstech.core.multiblock.shape.IFormedPlaced;
import dev.jstech.core.multiblock.shape.MeshCells;
import dev.jstech.space.crew.ShipDoorRules;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.ShipFlight;
import dev.jstech.space.ship.ShipKitBlocks;
import dev.jstech.space.ship.ShipStairGeometry;
import dev.jstech.space.ship.ShipStairs;
import dev.jstech.space.ship.ShipWingStairGeometry;
import dev.jstech.space.ship.SmallShipParts;
import dev.jstech.space.shipyard.ShipyardTerminalBlockEntity;
import dev.jstech.testkit.gametest.GameTestPlayers;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.space.TestShipyards;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Side Door hold's ways out to the land: beside the wings, a crew member walks from its deck out of its door onto
 * the wing, up its steps inside first beside wings whose root is a block over the deck, and down the stair the wing
 * lets down from its trailing edge as the door opens on the land; away from the wings, down its own boarding stair
 * from under its sill. A stair goes up as the door shuts, and lifting off, the ship shuts the door.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ShipSideDoorGameTests {

    private static final String ARENA = "shipyard";
    private static final String BATCH = "jsspace_side_door";
    private static final BlockPos CORNER = new BlockPos(1, 2, 1);
    private static final int WAIT = 600;
    /*
     * The Side Door hold's part's number: the third of the spine, beside the wings' roots; the fourth, past a short
     * section at their roots; or the second, ahead of the section at their roots, away from them.
     */
    private static final int BESIDE_THE_WINGS = 2;
    private static final int PAST_A_SHORT_ROOT = 3;
    private static final int AHEAD_OF_THE_WINGS = 1;
    /* How high a player steps up walking; the land under the hull's foot; how near the land the stair comes. */
    private static final double STEP_UP = 0.6;
    private static final double LAND = -2.0;
    private static final double NEAR_THE_LAND = 0.6;
    /* How far under where a player stood the way may go down at one place, in blocks. */
    private static final double STEP_DOWN = 1.0;
    /* A player's room: half as wide as they are, as tall, and how far over what they stand on. */
    private static final double HALF_A_PLAYER = 0.3;
    private static final double A_PLAYER_TALL = 1.8;
    private static final double OFF = 0.01;
    /* The deck over the hull's foot. */
    private static final double DECK = 0.35;
    /*
     * Along the hold, the middle of its door; across it, a player walks out from the deck to the wing, standing at
     * each place so far apart.
     */
    private static final double DOOR_Z = 2.5;
    /* Along the hold past a short root section, where the wing's trailing part runs along its door. */
    private static final double ALONG_THE_TRAILING_PART = 1.9;
    private static final double[] WAY = {0.6, 2.5};
    private static final double WALKED = 0.05;
    /*
     * Away from the wings: along the hold, the middle of its boarding stair, under its door's front; across it, the
     * sill a player walks out to, and their place out on the stair, clear of the hull's side.
     */
    private static final double STAIR_Z = 2.0;
    private static final double SILL = 1.6;
    private static final double OUT_ON_THE_STAIR = 2.25;
    private static final double HALF = 0.5;

    private ShipSideDoorGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = WAIT)
    public static void wingStair_ofSweptWingsComesDownAndTheWayOutIsWalked(final GameTestHelper helper) {
        wingStair(helper, atTheRoots("swept_wings"), BESIDE_THE_WINGS, ShipWingStairGeometry.SWEPT, DOOR_Z);
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = WAIT)
    public static void wingStair_ofStubWingsComesDownAndTheWayOutIsWalked(final GameTestHelper helper) {
        wingStair(helper, atTheRoots("stub_wings"), BESIDE_THE_WINGS, ShipWingStairGeometry.STUB, DOOR_Z);
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = WAIT)
    public static void wingStair_ofTheBlendedDeltaComesDownAndTheWayOutIsWalked(final GameTestHelper helper) {
        wingStair(helper, atTheRoots("blended_delta"), BESIDE_THE_WINGS, ShipWingStairGeometry.DELTA, DOOR_Z);
    }

    /* Past a short section at the swept wings' roots, the hold's door opens along their trailing part. */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = WAIT)
    public static void wingStair_ofWingsRunningPastAShortRootComesDownByTheNextHold(final GameTestHelper helper) {
        wingStair(helper, new ShipDesign("JS-S3", List.of(part("greenhouse_cockpit"), part("airlock"),
                part("reactor_flush"), part("side_door")), List.of(part("twin_bells"), part("swept_wings"),
                part("wheels"))), PAST_A_SHORT_ROOT, ShipWingStairGeometry.SWEPT, ALONG_THE_TRAILING_PART);
    }

    /*
     * A ship whose Side Door hold stands away from its wings, let go on the land: its door opens and its own boarding
     * stair, the airlock's on its right flank, comes down from under its sill, every tread there, a step down each
     * from the deck to near the land; a player walks out from the deck through the door and down onto the stair, their
     * room clear. Shut, the stair goes up; lifting off, the ship shuts the door, the stair up.
     */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = WAIT)
    public static void boardingStair_awayFromTheWingsComesDownFromTheSillAndTheWayOutIsWalked(
            final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final PlannedBuild[] plan = new PlannedBuild[1];
        final MovingStructure[] ship = new MovingStructure[1];
        final FormedDoors.Door[] door = new FormedDoors.Door[1];
        final ShipDesign design = new ShipDesign("JS-S2", List.of(part("greenhouse_cockpit"), part("side_door"),
                part("airlock"), part("reactor_flush")), List.of(part("twin_bells"), part("swept_wings"),
                part("wheels")));
        final List<int[]> cells = ShipStairGeometry.MIRRORED_CELLS;
        landed(helper, design, plan, ship)
                .thenExecute(() -> {
                    final ServerPlayer crew = GameTestPlayers.connected(helper, "jst-side-stair");
                    door[0] = sideDoor(level, plan[0], ship[0], AHEAD_OF_THE_WINGS);
                    final FormedPlace hold = placeOf(level, door[0]);
                    helper.assertTrue(FormedDoors.shut(level, door[0]) && treads(level, door[0], hold, cells)
                            .isEmpty(), "its door stands shut, its stair folded away");
                    helper.assertTrue(FormedDoors.use(level, crew, door[0]), "its door opens");
                    final List<Double> tops = treads(level, door[0], hold, cells);
                    helper.assertTrue(tops.size() == ShipStairGeometry.mirroredSteps().size(), "on the land, every"
                            + " tread of its stair is there: " + tops);
                    final double hull = door[0].uses().getY() - 1;
                    double above = hull + DECK;
                    for (final double top : tops) {
                        helper.assertTrue(above - top <= STEP_UP + 1.0e-6, "each tread a step down from the one"
                                + " over it, from the deck: " + tops);
                        above = top;
                    }
                    helper.assertTrue(above - (hull + LAND) <= NEAR_THE_LAND, "down to near the land: " + above);
                    // From the deck out through the door, then down onto the stair beside the hull.
                    double stood = hull + DECK;
                    for (double across = WAY[0]; across <= SILL + 1.0e-9; across += WALKED) {
                        stood = standOn(helper, level, door[0], hold, new Vec3(across, 0, STAIR_Z), stood);
                    }
                    standOn(helper, level, door[0], hold, new Vec3(OUT_ON_THE_STAIR, 0, STAIR_Z), stood);
                    helper.assertTrue(FormedDoors.use(level, crew, door[0]) && treads(level, door[0], hold, cells)
                            .isEmpty(), "shut again, its stair goes up");
                    helper.assertTrue(FormedDoors.use(level, crew, door[0]) && !treads(level, door[0], hold, cells)
                            .isEmpty(), "and down as it opens again");
                    GameTestPlayers.leave(crew);
                    ShipStairs.groundChanged(level, ship[0], false);
                })
                .thenWaitUntil(() -> helper.assertTrue(FormedDoors.shut(level, door[0])
                        && treads(level, door[0], placeOf(level, door[0]), cells).isEmpty(), "lifting off, the ship"
                        + " shuts its door, its stair up"))
                .thenExecute(() -> MovingStructures.takeAway(ship[0]))
                .thenSucceed();
    }

    /*
     * A ship of {@code design} whose Side Door hold, its part numbered {@code part}, opens beside its wings, let go on
     * the land: its door opens, the stair {@code stair} under the wing on its side comes down, every tread there, a
     * step down each from the wing's top to near the land; a player walks out across the hold from its deck at
     * {@code along} its length, a step up at most at each place and their room clear, onto the wing, and stands on
     * the stair clear of the wing, a step under its top. Shut, the stair goes up; lifting off, the ship shuts the
     * door, the stair up.
     */
    private static void wingStair(final GameTestHelper helper, final ShipDesign design, final int part,
                                  final ShipWingStairGeometry stair, final double along) {
        final ServerLevel level = helper.getLevel();
        final PlannedBuild[] plan = new PlannedBuild[1];
        final MovingStructure[] ship = new MovingStructure[1];
        final FormedDoors.Door[] door = new FormedDoors.Door[1];
        final FormedDoors.Door[] wingStair = new FormedDoors.Door[1];
        final List<int[]> cells = stair.cells(false);
        landed(helper, design, plan, ship)
                .thenExecute(() -> {
                    final ServerPlayer crew = GameTestPlayers.connected(helper, "jst-side-door");
                    door[0] = sideDoor(level, plan[0], ship[0], part);
                    final FormedPlace hold = placeOf(level, door[0]);
                    // The stair is the wing's, counted from the wing's own blocks.
                    wingStair[0] = ShipStairs.wingStairOf(level, door[0]).orElseThrow();
                    final FormedPlace wingAt = placeOf(level, wingStair[0]);
                    helper.assertTrue(FormedDoors.shut(level, door[0]) && treads(level, wingStair[0], wingAt, cells)
                            .isEmpty() && FormedDoors.shut(level, wingStair[0]), "its door stands shut, the wing's"
                            + " stair up");
                    helper.assertTrue(FormedDoors.use(level, crew, door[0]), "its door opens");
                    helper.assertTrue(!FormedDoors.shut(level, wingStair[0]), "the wing's stair swings down");
                    final List<Double> tops = treads(level, wingStair[0], wingAt, cells);
                    helper.assertTrue(tops.size() == stair.steps().size(), "on the land, every tread of the wing's"
                            + " stair is there: " + tops);
                    final double hull = door[0].uses().getY() - 1;
                    double above = hull + stair.top();
                    for (final double top : tops) {
                        helper.assertTrue(above - top <= STEP_UP + 1.0e-6, "each tread a step down from the one"
                                + " over it, from the wing's top: " + tops + " under " + (hull + stair.top()));
                        above = top;
                    }
                    helper.assertTrue(above - (hull + LAND) <= NEAR_THE_LAND, "down to near the land: " + above);
                    // From the deck out of the door onto the wing, a little at a time as a player walks, a step up at
                    // most each place, the room clear.
                    double stood = hull + DECK;
                    for (double across = WAY[0]; across <= WAY[1] + 1.0e-9; across += WALKED) {
                        stood = standOn(helper, level, door[0], hold, new Vec3(across, 0, along), stood);
                    }
                    // Down off the wing onto the stair, just past the trailing edge: clear of the wing, a step under.
                    final double middle = (ShipWingStairGeometry.ACROSS_FROM + ShipWingStairGeometry.ACROSS_TO) / 2;
                    final double onTheStair = standOn(helper, level, wingStair[0], wingAt, new Vec3(middle, 0,
                            stair.hingeZ() + HALF_A_PLAYER), hull + stair.top());
                    helper.assertTrue(hull + stair.top() - onTheStair <= STEP_UP + 1.0e-6, "the stair's highest"
                            + " tread clear of the wing a step under its top: " + onTheStair);
                    helper.assertTrue(FormedDoors.use(level, crew, door[0]) && treads(level, wingStair[0], wingAt,
                            cells).isEmpty() && FormedDoors.shut(level, wingStair[0]), "shut again, the wing's stair"
                            + " goes up");
                    helper.assertTrue(FormedDoors.use(level, crew, door[0]) && !treads(level, wingStair[0], wingAt,
                            cells).isEmpty(), "and down as it opens again");
                    GameTestPlayers.leave(crew);
                    ShipStairs.groundChanged(level, ship[0], false);
                })
                .thenWaitUntil(() -> helper.assertTrue(FormedDoors.shut(level, door[0])
                        && FormedDoors.shut(level, wingStair[0]) && treads(level, wingStair[0],
                        placeOf(level, wingStair[0]), cells).isEmpty(), "lifting off, the ship shuts its door, the"
                        + " wing's stair up"))
                .thenExecute(() -> MovingStructures.takeAway(ship[0]))
                .thenSucceed();
    }

    /* The ship of {@code design} built in a dock, its plan in {@code plan}, let go, and standing on the land. */
    private static GameTestSequence landed(final GameTestHelper helper, final ShipDesign design,
                                           final PlannedBuild[] plan, final MovingStructure[] ship) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        return helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(level.getBlockEntity(terminal)
                        instanceof ShipyardTerminalBlockEntity dock && dock.formed(), "the dock forms"))
                .thenExecute(() -> {
                    final ShipyardTerminalBlockEntity dock = (ShipyardTerminalBlockEntity) level
                            .getBlockEntity(terminal);
                    dock.design(design);
                    helper.assertTrue(dock.confirm(), "the ship is sent to the dock");
                    plan[0] = dock.build().orElseThrow();
                    for (final PlanSpot spot : plan[0].plan().spots()) {
                        level.setBlockAndUpdate(plan[0].where(spot), BuiltInRegistries.BLOCK.get(spot.block())
                                .defaultBlockState());
                    }
                })
                .thenWaitUntil(() -> helper.assertTrue(plan[0] != null && plan[0].complete(), "the ship stands whole"
                        + " and formed"))
                .thenExecute(() -> ship[0] = ((ShipyardTerminalBlockEntity) level.getBlockEntity(terminal))
                        .release(null).orElseThrow())
                .thenWaitUntil(() -> helper.assertTrue(ship[0].ready() && ship[0].motion() instanceof ShipFlight
                        flight && flight.grounded(), "the ship let go stands on the land"));
    }

    /* The side door of the Side Door hold numbered {@code part} of the ship {@code ship} built by {@code plan}. */
    private static FormedDoors.Door sideDoor(final ServerLevel level, final PlannedBuild plan,
                                             final MovingStructure ship, final int part) {
        final PlanSpot spot = plan.plan().spots().stream().filter(each -> each.part() == part
                && each.block().equals(ShipKitBlocks.RAMP_PLATE.getId())).findFirst().orElseThrow();
        return named(level, BlockPos.containing(MovingStructures.toYard(ship, Vec3.atCenterOf(plan.where(spot)))),
                ShipDoorRules.SIDE_DOOR);
    }

    /*
     * Where a player stands at {@code at} of the hold's model ({@code hold}, its door {@code door}), having stood at
     * {@code stood}: on the highest top of what stands under their width there, no more than a step up and no lower
     * than a step down, their room over it clear.
     */
    private static double standOn(final GameTestHelper helper, final ServerLevel level, final FormedDoors.Door door,
                                  final FormedPlace hold, final Vec3 at, final double stood) {
        final Vec3 there = inYard(door, hold, at);
        final AABB column = new AABB(there.x - HALF_A_PLAYER, stood - STEP_DOWN, there.z - HALF_A_PLAYER,
                there.x + HALF_A_PLAYER, stood + STEP_UP, there.z + HALF_A_PLAYER);
        double top = Double.NEGATIVE_INFINITY;
        for (final BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(column.minX, column.minY, column.minZ),
                BlockPos.containing(column.maxX, column.maxY, column.maxZ))) {
            for (final AABB box : level.getBlockState(pos).getCollisionShape(level, pos).toAabbs()) {
                final AABB moved = box.move(pos);
                if (moved.intersects(column)) {
                    top = Math.max(top, moved.maxY);
                }
            }
        }
        helper.assertTrue(top > Double.NEGATIVE_INFINITY, "something to stand on at " + at + " (" + there + ")");
        helper.assertTrue(top - stood <= STEP_UP + 1.0e-6, "at " + at + " a step up at most: " + top + " over "
                + stood);
        final AABB room = new AABB(there.x - HALF_A_PLAYER, top + OFF, there.z - HALF_A_PLAYER,
                there.x + HALF_A_PLAYER, top + OFF + A_PLAYER_TALL, there.z + HALF_A_PLAYER);
        final List<String> blocking = blocking(level, room);
        helper.assertTrue(blocking.isEmpty(), "at " + at + " a player stands, their room clear: " + blocking);
        return top;
    }

    /* Where the point {@code at} of a model stands in the ship's yard, its block {@code hold} at {@code door}'s. */
    private static Vec3 inYard(final FormedDoors.Door door, final FormedPlace hold, final Vec3 at) {
        final double[] turned = new double[3];
        CellSpace.toWorld(new double[] {at.x - hold.x() - HALF, at.y - hold.y() - HALF, at.z - hold.z() - HALF},
                hold.turn(), turned);
        return Vec3.atCenterOf(door.uses()).add(turned[0], turned[1], turned[2]);
    }

    /* The blocks that stand in {@code box}, a player's room, each where it stands. */
    private static List<String> blocking(final ServerLevel level, final AABB box) {
        final List<String> out = new ArrayList<>();
        final VoxelShape room = Shapes.create(box);
        for (final BlockPos at : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ),
                BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            final VoxelShape shape = level.getBlockState(at).getCollisionShape(level, at)
                    .move(at.getX(), at.getY(), at.getZ());
            if (Shapes.joinIsNotEmpty(shape, room, BooleanOp.AND)) {
                final List<String> models = level.getBlockEntity(at) instanceof IFormedPlaced placed
                        ? placed.places().stream().map(place -> place.model().getPath() + " " + place.x() + ","
                        + place.y() + "," + place.z()).toList() : List.of(level.getBlockState(at).toString());
                out.add(at.toShortString() + " " + models + " " + Shapes.join(shape, room, BooleanOp.AND).bounds()
                        .move(-at.getX(), -at.getY(), -at.getZ()));
            }
        }
        return out;
    }

    /*
     * The tops of the treads of a stair standing in the hold's model's blocks {@code cells}, as those blocks stand by
     * the hold's door, from the highest down; none while it is up.
     */
    private static List<Double> treads(final ServerLevel level, final FormedDoors.Door door, final FormedPlace hold,
                                       final List<int[]> cells) {
        VoxelShape all = Shapes.empty();
        for (final int[] cell : cells) {
            final BlockPos at = FormedDoors.blockOf(door.uses(), hold, MeshCells.key(cell[0], cell[1], cell[2]));
            if (ShipStairs.down(level, at)) {
                all = Shapes.joinUnoptimized(all, level.getBlockState(at).getCollisionShape(level, at)
                        .move(at.getX(), at.getY(), at.getZ()), BooleanOp.OR);
            }
        }
        final List<Double> tops = new ArrayList<>();
        for (final AABB box : all.optimize().toAabbs()) {
            if (tops.stream().noneMatch(seen -> Math.abs(seen - box.maxY) < 1.0e-6)) {
                tops.add(box.maxY);
            }
        }
        tops.sort((a, b) -> Double.compare(b, a));
        return tops;
    }

    /* A ship whose Side Door hold, the third of its spine, stands at the roots of the wings {@code wing}. */
    private static ShipDesign atTheRoots(final String wing) {
        return new ShipDesign("JS-S1", List.of(part("greenhouse_cockpit"), part("airlock"), part("side_door"),
                part("reactor_flush")), List.of(part("twin_bells"), part(wing), part("wheels")));
    }

    /* The block of the hold's model the block its door works by stands as. */
    private static FormedPlace placeOf(final ServerLevel level, final FormedDoors.Door door) {
        return ((IFormedPlaced) level.getBlockEntity(door.uses())).places().stream()
                .filter(place -> place.model().equals(door.model())).findFirst().orElseThrow();
    }

    /* The door {@code piece} of the model the block at {@code pos} stands as. */
    private static FormedDoors.Door named(final ServerLevel level, final BlockPos pos, final String piece) {
        for (final FormedPlace place : ((IFormedPlaced) level.getBlockEntity(pos)).places()) {
            final Optional<FormedDoors.Door> door = FormedDoors.named(level, pos, place, piece);
            if (door.isPresent()) {
                return door.get();
            }
        }
        throw new IllegalStateException("no " + piece + " at " + pos);
    }

    private static ResourceLocation part(final String path) {
        return SmallShipParts.id("small_" + path);
    }
}
