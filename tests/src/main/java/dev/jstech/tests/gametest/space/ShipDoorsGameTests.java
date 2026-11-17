/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.air.AirMix;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.multiblock.FormedPartBlockEntity;
import dev.jstech.core.multiblock.damage.DamageStamps;
import dev.jstech.core.multiblock.damage.FormedDamage;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.core.multiblock.shape.FormedDoors;
import dev.jstech.core.multiblock.shape.FormedPlace;
import dev.jstech.core.multiblock.shape.IFormedPlaced;
import dev.jstech.core.multiblock.shape.MeshCells;
import dev.jstech.core.room.Room;
import dev.jstech.core.room.SealedRooms;
import dev.jstech.space.api.SpaceSuit;
import dev.jstech.space.api.SpaceSuits;
import dev.jstech.space.crew.ShipDoorRules;
import dev.jstech.space.life.LifeSupport;
import dev.jstech.space.life.LunarSuit;
import dev.jstech.space.ship.OxygenPanelBlock;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.ShipFlight;
import dev.jstech.space.ship.ShipKitBlocks;
import dev.jstech.space.ship.ShipRampGeometry;
import dev.jstech.space.ship.ShipStairGeometry;
import dev.jstech.space.ship.ShipStairs;
import dev.jstech.space.ship.SmallShipParts;
import dev.jstech.space.shipyard.ShipyardTerminalBlockEntity;
import dev.jstech.testkit.gametest.GameTestPlayers;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.space.TestShipyards;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A small ship's doors: a pressure door shuts and opens as a player uses it, its leaf solid in its doorway while it is
 * shut and gone from it while it is open; an airlock never stands open inside and out at once, its outer door shut as
 * the ship forms and opening only with the doors at its ends shut, either of those opening only with it shut; and,
 * the ship let go and standing on the land, its outer door opening lets its boarding stair down beside it, step by
 * step from near the land to its floor, and shutting takes it up. And the cabin's air: each section is a room of its
 * own, its air going through a pressure door only while it is open; the airlock's pump draws its air back into the
 * cabin before its outer door opens and fills it again from the cabin as it shuts; and its oxygen panel fills a suit
 * from the cabin's air.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ShipDoorsGameTests {

    private static final String ARENA = "shipyard";
    private static final String BATCH = "jsspace_ship_doors";
    private static final BlockPos CORNER = new BlockPos(1, 2, 1);
    private static final int WAIT = 600;
    private static final ShipDesign SHIP = new ShipDesign("JS-D1", List.of(part("orbiter_cockpit"), part("airlock"),
            part("workshop"), part("reactor_flush")), List.of(part("twin_bells"), part("blended_delta"),
            part("twin_canted_fins"), part("wheels")));
    /* A ship with a Ramp hold at its tail, its part's number; the ramp's deck over the hull's foot, in blocks. */
    private static final ShipDesign RAMP_SHIP = new ShipDesign("JS-D2", List.of(part("orbiter_cockpit"),
            part("airlock"), part("reactor_flush"), part("ramp")), List.of(part("magnetic_nozzle"),
            part("stub_wings"), part("legs")));
    private static final int RAMP_PART = 3;
    private static final double RAMP_DECK = 0.35;
    /* The spine's parts by their number: the airlock second, the workshop third, the reactor last. */
    private static final int AIRLOCK = 1;
    private static final int WORKSHOP = 2;
    private static final int REACTOR = 3;
    private static final int SECOND = 20;
    /* A room full of the sea's air and one emptied, in kilopascals; how near two rooms joined by a door even out. */
    private static final double FULL = 90;
    private static final double EMPTY = 5;
    private static final double EVEN = 5;
    /* How much of its air an airlock's chamber keeps as its pump draws it back, at most. */
    private static final double LEFT_IN_THE_CHAMBER = 0.05;
    /* How high a player steps up walking; the airlock's floor over its hull's foot; the land under the hull's foot. */
    private static final double STEP_UP = 0.6;
    private static final double FLOOR = 0.375;
    private static final double LAND = -2.0;
    /* How close to the land the stair's lowest tread comes, at most. */
    private static final double NEAR_THE_LAND = 0.6;
    /* How far round its terminal a dock's grid is looked over, in blocks. */
    private static final int DOCK_ROUND = 24;
    /* How wide a strike on a door's leaf is, in blocks, and the seed its dents are laid by. */
    private static final double STRUCK = 0.8;
    private static final long SEED = 7;
    /* A player's room in a doorway: half as wide as they are, as tall, and how far over its sill they stand. */
    private static final double HALF_A_PLAYER = 0.3;
    private static final double A_PLAYER_TALL = 1.8;
    private static final double OFF_THE_SILL = 0.01;
    /* How finely a door's blocks are searched for a point a click finds the door at, a side in so many parts. */
    private static final int SAMPLES = 32;
    private static final double HALF = 0.5;

    private ShipDoorsGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = WAIT)
    public static void pressureDoor_shutsAndOpensItsLeafSolidOnlyWhileShut(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final PlannedBuild[] plan = new PlannedBuild[1];
        build(helper, plan).thenExecute(() -> {
            final ServerPlayer crew = GameTestPlayers.connected(helper, "jst-door-crew");
            final FormedDoors.Door door = door(level, plan[0], WORKSHOP, ShipKitBlocks.PRESSURE_DOOR.getId(),
                    ShipDoorRules.PRESSURE_DOOR);
            final double open = volume(level, door);
            helper.assertTrue(!FormedDoors.shut(level, door), "a pressure door stands open as its ship forms");
            helper.assertTrue(FormedDoors.use(level, crew, door) && FormedDoors.shut(level, door)
                    && volume(level, door) > open, "used, it shuts, its leaf solid in its doorway: " + open + " to "
                    + volume(level, door));
            helper.assertTrue(FormedDoors.use(level, crew, door) && !FormedDoors.shut(level, door)
                    && Math.abs(volume(level, door) - open) < 1.0e-9, "used again, it opens, its leaf gone from it");
            GameTestPlayers.leave(crew);
        }).thenSucceed();
    }

    /*
     * Every door of every module, its pressure doors, its airlock's outer door and its ramp, in five ships that hold
     * every module between them: a player's click moves it once and a second click back, whichever hands their game
     * tries, and struck, its blocks dented, it still opens out of its doorway.
     */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = WAIT)
    public static void everyDoor_ofAShipOfThreeModules(final GameTestHelper helper) {
        everyDoor(helper, new ShipDesign("JS-E1", List.of(part("orbiter_cockpit"), part("airlock"), part("workshop"),
                part("lab"), part("quarters"), part("reactor_flush")), List.of(part("twin_bells"),
                part("blended_delta"), part("twin_canted_fins"), part("wheels"))));
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = WAIT)
    public static void everyDoor_ofAShipOfTheOtherModules(final GameTestHelper helper) {
        everyDoor(helper, new ShipDesign("JS-E2", List.of(part("cab_cockpit"), part("docking"), part("corridor"),
                part("computing"), part("small_cargo"), part("reactor_fins")), List.of(part("cluster"),
                part("stub_wings"), part("single_fin"), part("legs"))));
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = WAIT)
    public static void everyDoor_ofAShipWithASideDoorHold(final GameTestHelper helper) {
        everyDoor(helper, new ShipDesign("JS-E3", List.of(part("greenhouse_cockpit"), part("airlock"),
                part("side_door"), part("reactor_flush")), List.of(part("twin_bells"), part("swept_wings"),
                part("wheels"))));
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = WAIT)
    public static void everyDoor_ofAShipWithABay(final GameTestHelper helper) {
        everyDoor(helper, new ShipDesign("JS-E4", List.of(part("cab_cockpit"), part("docking"), part("bay"),
                part("reactor_fins")), List.of(part("cluster"), part("stub_wings"), part("legs"))));
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = WAIT)
    public static void everyDoor_ofAShipWithARamp(final GameTestHelper helper) {
        everyDoor(helper, new ShipDesign("JS-E5", List.of(part("orbiter_cockpit"), part("airlock"),
                part("reactor_flush"), part("ramp")), List.of(part("magnetic_nozzle"), part("stub_wings"),
                part("legs"))));
    }

    /* A door struck, its blocks dented, still lets a player through open: its leaf is out of its doorway. */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = WAIT)
    public static void pressureDoor_struckItsOpenLeafStaysOutOfItsDoorway(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final PlannedBuild[] plan = new PlannedBuild[1];
        build(helper, plan).thenExecute(() -> {
            final FormedDoors.Door door = door(level, plan[0], WORKSHOP, ShipKitBlocks.PRESSURE_DOOR.getId(),
                    ShipDoorRules.PRESSURE_DOOR);
            final AABB doorway = doorway(level, door);
            FormedDoors.set(level, door, false);
            helper.assertTrue(blocking(level, doorway).isEmpty(), "open, a player walks through it: "
                    + blocking(level, doorway));
            FormedDoors.set(level, door, true);
            helper.assertTrue(!blocking(level, doorway).isEmpty(), "shut, it stands in their way");
            FormedDoors.set(level, door, false);
            for (final BlockPos at : door.leaf()) {
                FormedDamage.strike(level, at, DamageStamps.IMPACT, Vec3.atCenterOf(at), new Vec3(0, 1, 0),
                        STRUCK, SEED);
            }
            helper.assertTrue(door.leaf().stream().anyMatch(at -> level.getBlockEntity(at)
                    instanceof FormedPartBlockEntity part && !part.damage().isEmpty()), "its leaf's blocks are struck");
            helper.assertTrue(blocking(level, doorway).isEmpty(), "struck and open, a player still walks through"
                    + " it: in their way " + blocking(level, doorway));
        }).thenSucceed();
    }

    /*
     * A player's click moves a door once: a player's game, its main hand's use of the door's block passing there, tries
     * the other hand too, and the second use must not move the door back.
     */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = WAIT)
    public static void click_aPlayersClickMovesADoorOnceThoughBothHandsAreTried(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final PlannedBuild[] plan = new PlannedBuild[1];
        build(helper, plan).thenExecute(() -> {
            final ServerPlayer crew = GameTestPlayers.connected(helper, "jst-click-crew");
            final FormedDoors.Door door = door(level, plan[0], WORKSHOP, ShipKitBlocks.PRESSURE_DOOR.getId(),
                    ShipDoorRules.PRESSURE_DOOR);
            final Vec3 at = pointOn(level, door);
            final BlockHitResult hit = new BlockHitResult(at, Direction.NORTH, BlockPos.containing(at), false);
            final boolean wasShut = FormedDoors.shut(level, door);
            crew.gameMode.useItemOn(crew, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, hit);
            crew.gameMode.useItemOn(crew, level, ItemStack.EMPTY, InteractionHand.OFF_HAND, hit);
            helper.assertTrue(FormedDoors.shut(level, door) != wasShut, "clicked once, both hands tried, the door "
                    + "moved: shut before " + wasShut + ", now " + FormedDoors.shut(level, door));
            GameTestPlayers.leave(crew);
        }).thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = WAIT)
    public static void airlock_cyclesShuttingOneSideAsTheOtherOpens(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final PlannedBuild[] plan = new PlannedBuild[1];
        build(helper, plan).thenExecute(() -> {
            final ServerPlayer crew = GameTestPlayers.connected(helper, "jst-lock-crew");
            final FormedDoors.Door outer = door(level, plan[0], AIRLOCK, ShipKitBlocks.AIRLOCK_DOOR.getId(),
                    ShipDoorRules.OUTER_DOOR);
            final List<FormedDoors.Door> inner = ShipDoorRules.otherSide(level, outer);
            helper.assertTrue(inner.size() == 2 && FormedDoors.shut(level, outer), "its outer door stands shut as the"
                    + " ship forms, the airlock a door at each end: " + inner.size());
            helper.assertTrue(FormedDoors.use(level, crew, outer) && !FormedDoors.shut(level, outer), "used from"
                    + " outside on a ship just formed, its outer door opens");
            helper.assertTrue(inner.stream().allMatch(each -> FormedDoors.shut(level, each)), "and its inner doors"
                    + " shut as it does, never open with it");
            helper.assertTrue(FormedDoors.use(level, crew, inner.getFirst()) && !FormedDoors.shut(level,
                    inner.getFirst()) && FormedDoors.shut(level, outer), "an inner door used opens, shutting the outer"
                    + " one");
            helper.assertTrue(FormedDoors.use(level, crew, outer) && !FormedDoors.shut(level, outer)
                    && inner.stream().allMatch(each -> FormedDoors.shut(level, each)), "the outer one used again"
                    + " opens, shutting the inner ones");
            GameTestPlayers.leave(crew);
        }).thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = WAIT)
    public static void stair_comesDownOnTheLandAsTheOuterDoorOpensAndUpAsItShuts(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final PlannedBuild[] plan = new PlannedBuild[1];
        final MovingStructure[] ship = new MovingStructure[1];
        final BlockPos[] terminal = new BlockPos[1];
        build(helper, plan, terminal).thenExecute(() -> ship[0] = ((ShipyardTerminalBlockEntity) level
                        .getBlockEntity(terminal[0])).release(null).orElseThrow())
                .thenWaitUntil(() -> helper.assertTrue(ship[0].ready() && ship[0].motion() instanceof ShipFlight
                        flight && flight.grounded(), "the ship let go stands on the land"))
                .thenExecute(() -> {
                    final ServerPlayer crew = GameTestPlayers.connected(helper, "jst-stair-crew");
                    final BlockPos built = plan[0].where(spot(plan[0], AIRLOCK, ShipKitBlocks.AIRLOCK_DOOR.getId()));
                    final BlockPos aboard = BlockPos.containing(MovingStructures.toYard(ship[0],
                            Vec3.atCenterOf(built)));
                    final FormedDoors.Door outer = named(level, aboard, ShipDoorRules.OUTER_DOOR);
                    ShipDoorRules.otherSide(level, outer).forEach(inner -> FormedDoors.set(level, inner, true));
                    helper.assertTrue(stairs(level, outer).isEmpty(), "its stair is folded away while its door is"
                            + " shut");
                    helper.assertTrue(FormedDoors.use(level, crew, outer), "its outer door opens");
                    final List<Double> treads = stairs(level, outer);
                    helper.assertTrue(treads.size() == ShipStairGeometry.steps().size(), "on the land, its stair"
                            + " comes down, every tread there: " + treads);
                    final double hull = outer.uses().getY() - 1;
                    double above = hull + FLOOR;
                    for (final double tread : treads) {
                        helper.assertTrue(above - tread <= STEP_UP + 1.0e-6, "each step a step down from the one"
                                + " over it, from the floor: " + treads + " under the floor at " + (hull + FLOOR));
                        above = tread;
                    }
                    helper.assertTrue(above - (hull + LAND) <= NEAR_THE_LAND, "down to near the land: " + above);
                    helper.assertTrue(FormedDoors.use(level, crew, outer) && stairs(level, outer).isEmpty(),
                            "shut again, its stair goes up");
                    GameTestPlayers.leave(crew);
                    MovingStructures.takeAway(ship[0]);
                })
                .thenSucceed();
    }

    /*
     * A ship with a ramp let go on the land: its ramp opens down onto the land, every step of it there, each a step
     * down from the one before from the deck to near the land, and they go up as it shuts; lifting off, the ship shuts
     * it, its steps going up with it.
     */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = WAIT)
    public static void ramp_comesDownOnTheLandAsItOpensAndTheShipShutsItLiftingOff(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final PlannedBuild[] plan = new PlannedBuild[1];
        final MovingStructure[] ship = new MovingStructure[1];
        final BlockPos[] terminal = new BlockPos[1];
        final FormedDoors.Door[] ramp = new FormedDoors.Door[1];
        build(helper, plan, terminal, RAMP_SHIP).thenExecute(() -> ship[0] = ((ShipyardTerminalBlockEntity) level
                        .getBlockEntity(terminal[0])).release(null).orElseThrow())
                .thenWaitUntil(() -> helper.assertTrue(ship[0].ready() && ship[0].motion() instanceof ShipFlight
                        flight && flight.grounded(), "the ship let go stands on the land"))
                .thenExecute(() -> {
                    final ServerPlayer crew = GameTestPlayers.connected(helper, "jst-ramp-crew");
                    final BlockPos built = plan[0].where(spot(plan[0], RAMP_PART, ShipKitBlocks.RAMP_PLATE.getId()));
                    final BlockPos aboard = BlockPos.containing(MovingStructures.toYard(ship[0],
                            Vec3.atCenterOf(built)));
                    ramp[0] = named(level, aboard, ShipDoorRules.RAMP);
                    helper.assertTrue(FormedDoors.shut(level, ramp[0]) && stairs(level, ramp[0]).isEmpty(), "its ramp"
                            + " stands shut, nothing let down");
                    helper.assertTrue(FormedDoors.use(level, crew, ramp[0]), "its ramp opens");
                    final List<Double> steps = stairs(level, ramp[0]);
                    helper.assertTrue(steps.size() == ShipRampGeometry.steps().size(), "on the land, every step of"
                            + " its ramp is there: " + steps);
                    final double hull = ramp[0].uses().getY() - 1;
                    double above = hull + RAMP_DECK;
                    for (final double step : steps) {
                        helper.assertTrue(above - step <= STEP_UP + 1.0e-6, "each step a step down from the one"
                                + " before, from the deck: " + steps);
                        above = step;
                    }
                    helper.assertTrue(above - (hull + LAND) <= NEAR_THE_LAND, "down to near the land: " + above);
                    helper.assertTrue(FormedDoors.use(level, crew, ramp[0]) && stairs(level, ramp[0]).isEmpty(),
                            "shut again, its steps go up");
                    helper.assertTrue(FormedDoors.use(level, crew, ramp[0]) && !stairs(level, ramp[0]).isEmpty(),
                            "and down as it opens again");
                    GameTestPlayers.leave(crew);
                    ShipStairs.groundChanged(level, ship[0], false);
                })
                .thenWaitUntil(() -> helper.assertTrue(FormedDoors.shut(level, ramp[0])
                        && stairs(level, ramp[0]).isEmpty(), "lifting off, the ship shuts its ramp, its steps up"))
                .thenExecute(() -> MovingStructures.takeAway(ship[0]))
                .thenSucceed();
    }

    /*
     * A ship standing on the land, its outer door open and its stair down, taken back by its dock: its stair goes up
     * first, not left on the grid as blocks of no plan, and the ship goes again whole.
     */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = WAIT)
    public static void stair_downAsItsDockTakesItBackGoesUpNotLeftOnTheGrid(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final PlannedBuild[] plan = new PlannedBuild[1];
        final MovingStructure[] ship = new MovingStructure[1];
        final BlockPos[] terminal = new BlockPos[1];
        build(helper, plan, terminal).thenExecute(() -> ship[0] = ((ShipyardTerminalBlockEntity) level
                        .getBlockEntity(terminal[0])).release(null).orElseThrow())
                .thenWaitUntil(() -> helper.assertTrue(ship[0].ready() && ship[0].motion() instanceof ShipFlight
                        flight && flight.grounded(), "the ship let go stands on the land"))
                .thenExecute(() -> {
                    final ServerPlayer crew = GameTestPlayers.connected(helper, "jst-dockstair");
                    final BlockPos built = plan[0].where(spot(plan[0], AIRLOCK, ShipKitBlocks.AIRLOCK_DOOR.getId()));
                    final FormedDoors.Door outer = named(level, BlockPos.containing(MovingStructures.toYard(ship[0],
                            Vec3.atCenterOf(built))), ShipDoorRules.OUTER_DOOR);
                    ShipDoorRules.otherSide(level, outer).forEach(inner -> FormedDoors.set(level, inner, true));
                    helper.assertTrue(FormedDoors.use(level, crew, outer) && !stairs(level, outer).isEmpty(),
                            "its outer door open on the land, its stair is down");
                    GameTestPlayers.leave(crew);
                    final ShipyardTerminalBlockEntity dock = (ShipyardTerminalBlockEntity) level
                            .getBlockEntity(terminal[0]);
                    helper.assertTrue(dock.seat(ship[0]), "its dock takes it back onto the grid");
                    final List<BlockPos> left = stairsAround(level, terminal[0]);
                    helper.assertTrue(left.isEmpty(), "its stair taken up first, none of it is left on the grid: "
                            + left);
                    final MovingStructure again = dock.release(null).orElseThrow();
                    MovingStructures.takeAway(again);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = WAIT)
    public static void pressureDoor_letsTheCabinsAirThroughOnlyWhileOpen(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final PlannedBuild[] plan = new PlannedBuild[1];
        final FormedDoors.Door[] door = new FormedDoors.Door[1];
        final List<BlockPos> sides = new ArrayList<>();
        build(helper, plan).thenExecute(() -> {
            // Every pressure door shut, so each section keeps its air to itself (the reactor's found by its drum).
            door[0] = door(level, plan[0], WORKSHOP, ShipKitBlocks.PRESSURE_DOOR.getId(), ShipDoorRules.PRESSURE_DOOR);
            for (final int part : List.of(AIRLOCK, WORKSHOP)) {
                FormedDoors.set(level, door(level, plan[0], part, ShipKitBlocks.PRESSURE_DOOR.getId(),
                        ShipDoorRules.PRESSURE_DOOR), true);
            }
            FormedDoors.set(level, door(level, plan[0], REACTOR, ShipKitBlocks.FUSION_CORE.getId(),
                    ShipDoorRules.PRESSURE_DOOR), true);
        }).thenWaitUntil(() -> {
            sides.clear();
            sides.addAll(sides(level, door[0]));
            helper.assertTrue(sides.size() == 2, "the sections either side of the shut door are a room each: "
                    + sides);
        }).thenExecute(() -> room(level, sides.getLast()).empty())
                .thenIdle(2 * SECOND)
                .thenExecute(() -> {
                    final double kept = room(level, sides.getFirst()).pressure();
                    final double emptied = room(level, sides.getLast()).pressure();
                    helper.assertTrue(kept > FULL && emptied < EMPTY, "shut, it keeps one side's air from the other,"
                            + " emptied: " + kept + " and " + emptied + " kPa");
                    FormedDoors.set(level, door[0], false);
                })
                .thenWaitUntil(() -> {
                    final double one = room(level, sides.getFirst()).pressure();
                    final double other = room(level, sides.getLast()).pressure();
                    helper.assertTrue(Math.abs(one - other) < EVEN && other > EMPTY, "open, the air goes through it"
                            + " and the two sides even out: " + one + " and " + other + " kPa");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = WAIT)
    public static void airlock_pumpsItsAirBackBeforeTheOuterDoorOpensAndFillsAgainAsItShuts(
            final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final PlannedBuild[] plan = new PlannedBuild[1];
        final FormedDoors.Door[] outer = new FormedDoors.Door[1];
        final BlockPos[] at = new BlockPos[2];
        build(helper, plan).thenExecute(() -> {
            outer[0] = door(level, plan[0], AIRLOCK, ShipKitBlocks.AIRLOCK_DOOR.getId(), ShipDoorRules.OUTER_DOOR);
            ShipDoorRules.otherSide(level, outer[0]).forEach(inner -> FormedDoors.set(level, inner, true));
        }).thenWaitUntil(() -> {
            // The airlock's chamber, inside its outer door, and the cabin beyond its first inner door.
            final List<BlockPos> chamber = sides(level, outer[0]);
            final List<BlockPos> beyond = sides(level, ShipDoorRules.otherSide(level, outer[0]).getFirst());
            helper.assertTrue(chamber.size() == 1 && beyond.size() == 2, "the airlock is a room, and the cabin "
                    + "beyond its inner door another: " + chamber + " and " + beyond);
            at[0] = chamber.getFirst();
            final long id = room(level, at[0]).id();
            at[1] = beyond.stream().filter(cell -> room(level, cell).id() != id).findFirst().orElseThrow();
        }).thenExecute(() -> {
            final double chamber = moles(room(level, at[0]));
            final double cabin = moles(room(level, at[1]));
            final ServerPlayer crew = GameTestPlayers.connected(helper, "jst-cycle-crew");
            helper.assertTrue(FormedDoors.use(level, crew, outer[0]) && !FormedDoors.shut(level, outer[0]),
                    "its outer door opens");
            helper.assertTrue(moles(room(level, at[0])) <= chamber * LEFT_IN_THE_CHAMBER
                            && moles(room(level, at[1])) >= cabin + chamber * (1 - LEFT_IN_THE_CHAMBER) - 1.0e-6,
                    "before it opens, the chamber's air is drawn back into the cabin: " + chamber + " to "
                            + moles(room(level, at[0])) + " mol, the cabin " + cabin + " to "
                            + moles(room(level, at[1])));
            // In space the open door leaves the chamber empty: so it stands as the door shuts.
            room(level, at[0]).empty();
            helper.assertTrue(FormedDoors.use(level, crew, outer[0]) && FormedDoors.shut(level, outer[0]),
                    "its outer door shuts");
            final double filled = room(level, at[0]).pressure();
            final double left = room(level, at[1]).pressure();
            helper.assertTrue(filled > EMPTY && Math.abs(filled - left) < EVEN, "shut, the chamber is filled again "
                    + "from the cabin: " + filled + " and " + left + " kPa");
            GameTestPlayers.leave(crew);
        }).thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = WAIT)
    public static void oxygenPanel_fillsASuitFromTheCabinsAir(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final PlannedBuild[] plan = new PlannedBuild[1];
        final BlockPos[] panel = new BlockPos[1];
        build(helper, plan).thenWaitUntil(() -> {
            panel[0] = plan[0].where(spot(plan[0], AIRLOCK, ShipKitBlocks.OXYGEN_PANEL.getId()));
            helper.assertTrue(SealedRooms.roomBeside(level, panel[0]).isPresent(), "the airlock is a room");
        }).thenExecute(() -> {
            final Room cabin = SealedRooms.roomBeside(level, panel[0]).orElseThrow();
            final double oxygen = cabin.moles(AirMix.OXYGEN);
            final ServerPlayer crew = GameTestPlayers.join(level, UUID.randomUUID(), "jst-panel-crew");
            suit(crew);
            final int filled = OxygenPanelBlock.fill(level, panel[0], crew);
            GameTestPlayers.leave(crew);
            helper.assertTrue(filled > 0 && cabin.moles(AirMix.OXYGEN) < oxygen, "the panel fills the suit's "
                    + "tanks from the cabin's air: " + filled + " seconds, " + oxygen + " to "
                    + cabin.moles(AirMix.OXYGEN) + " mol of oxygen");
        }).thenSucceed();
    }

    /* The ship built whole and formed in a dock, its plan kept in {@code plan}. */
    private static GameTestSequence build(final GameTestHelper helper, final PlannedBuild[] plan) {
        return build(helper, plan, new BlockPos[1]);
    }

    /* The ship built whole and formed in a dock, its plan kept in {@code plan}, the dock's terminal in {@code at}. */
    private static GameTestSequence build(final GameTestHelper helper, final PlannedBuild[] plan,
                                          final BlockPos[] at) {
        return build(helper, plan, at, SHIP);
    }

    /* The ship of {@code design} built and formed in a dock, its plan in {@code plan}, its terminal {@code at}. */
    private static GameTestSequence build(final GameTestHelper helper, final PlannedBuild[] plan, final BlockPos[] at,
                                          final ShipDesign design) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        at[0] = terminal;
        return helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(level.getBlockEntity(terminal)
                        instanceof ShipyardTerminalBlockEntity dock && dock.formed(), "the dock forms"))
                .thenExecute(() -> {
                    final ShipyardTerminalBlockEntity dock = (ShipyardTerminalBlockEntity) level
                            .getBlockEntity(terminal);
                    dock.design(design);
                    helper.assertTrue(dock.confirm(), "the ship " + design.name() + " is sent to the dock");
                    plan[0] = dock.build().orElseThrow();
                    for (final PlanSpot spot : plan[0].plan().spots()) {
                        level.setBlockAndUpdate(plan[0].where(spot), BuiltInRegistries.BLOCK.get(spot.block())
                                .defaultBlockState());
                    }
                })
                .thenWaitUntil(() -> helper.assertTrue(plan[0].complete(), "the ship stands whole and formed"));
    }

    /*
     * The ship of {@code design} built, then each of its doors clicked by a player, both hands tried, once and again,
     * and each struck open: every door that does not move once a click, or whose struck open leaf stands in its
     * doorway, named.
     */
    private static void everyDoor(final GameTestHelper helper, final ShipDesign design) {
        final ServerLevel level = helper.getLevel();
        final PlannedBuild[] plan = new PlannedBuild[1];
        build(helper, plan, new BlockPos[1], design).thenExecute(() -> {
            final ServerPlayer crew = GameTestPlayers.connected(helper, "jst-every-door");
            final List<FormedDoors.Door> doors = doorsOf(level, plan[0]);
            final List<String> wrong = new ArrayList<>();
            for (final FormedDoors.Door door : doors) {
                final Vec3 at = pointOn(level, door);
                final BlockHitResult hit = new BlockHitResult(at, Direction.NORTH, BlockPos.containing(at), false);
                final boolean was = FormedDoors.shut(level, door);
                click(crew, level, hit);
                if (FormedDoors.shut(level, door) == was) {
                    wrong.add(door.piece() + " at " + door.uses().toShortString() + " did not move on a click");
                }
                click(crew, level, hit);
                if (FormedDoors.shut(level, door) != was) {
                    wrong.add(door.piece() + " at " + door.uses().toShortString() + " did not move back");
                }
            }
            for (final FormedDoors.Door door : doors) {
                final String name = door.piece() + " at " + door.uses().toShortString();
                final AABB doorway = doorway(level, door);
                FormedDoors.set(level, door, false);
                if (!blocking(level, doorway).isEmpty()) {
                    wrong.add(name + " open is in a player's way: " + blocking(level, doorway));
                }
                FormedDoors.set(level, door, true);
                if (blocking(level, doorway).isEmpty()) {
                    wrong.add(name + " shut lets a player through");
                }
                FormedDoors.set(level, door, false);
                for (final BlockPos leaf : door.leaf()) {
                    FormedDamage.strike(level, leaf, DamageStamps.IMPACT, Vec3.atCenterOf(leaf), new Vec3(0, 1, 0),
                            STRUCK, SEED);
                }
                if (!blocking(level, doorway).isEmpty()) {
                    wrong.add(name + " struck open is in a player's way: " + blocking(level, doorway));
                }
            }
            GameTestPlayers.leave(crew);
            helper.assertTrue(!doors.isEmpty(), design.name() + " has doors");
            helper.assertTrue(wrong.isEmpty(), design.name() + "'s " + doors.size() + " doors: " + wrong);
        }).thenSucceed();
    }

    /* Every door of the ship of {@code plan}: its pressure doors, its airlock's outer door, its ramp and roof, once. */
    private static List<FormedDoors.Door> doorsOf(final ServerLevel level, final PlannedBuild plan) {
        final Map<BlockPos, FormedDoors.Door> out = new LinkedHashMap<>();
        for (final PlanSpot spot : plan.plan().spots()) {
            final String piece = spot.block().equals(ShipKitBlocks.PRESSURE_DOOR.getId()) ? ShipDoorRules.PRESSURE_DOOR
                    : spot.block().equals(ShipKitBlocks.AIRLOCK_DOOR.getId()) ? ShipDoorRules.OUTER_DOOR
                    : spot.block().equals(ShipKitBlocks.RAMP_PLATE.getId()) ? ShipDoorRules.RAMP
                    : spot.block().equals(ShipKitBlocks.HULL_PLATING.getId()) ? ShipDoorRules.BAY_DOORS : null;
            // A block a door's model makes its own: a ramp plate is the Side Door hold's door as well, no ramp of it.
            if (piece != null) {
                final BlockPos at = plan.where(spot);
                for (final FormedPlace place : ((IFormedPlaced) level.getBlockEntity(at)).places()) {
                    FormedDoors.named(level, at, place, piece).ifPresent(door -> out.putIfAbsent(door.uses(), door));
                }
            }
        }
        return List.copyOf(out.values());
    }

    /* {@code player} clicks where {@code hit} is, as their game does: the main hand, then the other too. */
    private static void click(final ServerPlayer player, final ServerLevel level, final BlockHitResult hit) {
        player.gameMode.useItemOn(player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, hit);
        player.gameMode.useItemOn(player, level, ItemStack.EMPTY, InteractionHand.OFF_HAND, hit);
    }

    /* The door {@code piece} of the part numbered {@code part}, through a block of it that wants {@code block}. */
    private static FormedDoors.Door door(final ServerLevel level, final PlannedBuild plan, final int part,
                                         final ResourceLocation block, final String piece) {
        return named(level, plan.where(spot(plan, part, block)), piece);
    }

    /* A point in {@code door}'s blocks where a click finds the door, as a player's would. */
    private static Vec3 pointOn(final ServerLevel level, final FormedDoors.Door door) {
        for (final BlockPos block : door.blocks()) {
            for (int x = 0; x < SAMPLES; x++) {
                for (int y = 0; y < SAMPLES; y++) {
                    for (int z = 0; z < SAMPLES; z++) {
                        final Vec3 at = new Vec3(block.getX() + (x + HALF) / SAMPLES, block.getY()
                                + (y + HALF) / SAMPLES, block.getZ() + (z + HALF) / SAMPLES);
                        if (FormedDoors.at(level, block, at).filter(found -> found.piece().equals(door.piece()))
                                .isPresent()) {
                            return at;
                        }
                    }
                }
            }
        }
        throw new IllegalStateException("no point of " + door.piece() + " finds it");
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

    private static PlanSpot spot(final PlannedBuild plan, final int part, final ResourceLocation block) {
        return plan.plan().spots().stream().filter(spot -> spot.part() == part && spot.block().equals(block))
                .findFirst().orElseThrow();
    }

    /*
     * Where a player walks through {@code door}: a player's width and height, standing on the foot of the room its
     * leaf fills shut and leaves open, in the middle of it, as deep as its leaf. Read with the door as it stands
     * intact, shut and then open; it is left as it stood.
     */
    private static AABB doorway(final ServerLevel level, final FormedDoors.Door door) {
        final boolean was = FormedDoors.shut(level, door);
        FormedDoors.set(level, door, true);
        final List<VoxelShape> shut = shapes(level, door);
        FormedDoors.set(level, door, false);
        final List<VoxelShape> open = shapes(level, door);
        FormedDoors.set(level, door, was);
        AABB leaf = null;
        for (int i = 0; i < shut.size(); i++) {
            final VoxelShape only = Shapes.join(shut.get(i), open.get(i), BooleanOp.ONLY_FIRST);
            if (!only.isEmpty()) {
                leaf = leaf == null ? only.bounds() : leaf.minmax(only.bounds());
            }
        }
        if (leaf == null) {
            throw new IllegalStateException(door.piece() + " at " + door.uses() + " fills nothing shut");
        }
        final Vec3 middle = leaf.getCenter();
        if (leaf.getYsize() < leaf.getXsize() && leaf.getYsize() < leaf.getZsize()) {
            // A door overhead, a hold's roof: its way is up through it, a player's width square, its leaf's depth.
            return new AABB(middle.x - HALF_A_PLAYER, leaf.minY, middle.z - HALF_A_PLAYER, middle.x + HALF_A_PLAYER,
                    leaf.maxY, middle.z + HALF_A_PLAYER);
        }
        // Across its way a player's width, along it its leaf's own depth: the doorway itself, whatever step lies
        // before or past it, a step a player climbs.
        final boolean alongX = leaf.getXsize() < leaf.getZsize();
        final double halfX = alongX ? leaf.getXsize() / 2 : HALF_A_PLAYER;
        final double halfZ = alongX ? HALF_A_PLAYER : leaf.getZsize() / 2;
        // Its foot: the top of the floor under it, open, a step over or under where its leaf comes down to.
        FormedDoors.set(level, door, false);
        final AABB foot = new AABB(middle.x - halfX, leaf.minY - STEP_UP, middle.z - halfZ, middle.x + halfX,
                leaf.minY + STEP_UP, middle.z + halfZ);
        double floor = foot.minY;
        for (final BlockPos at : BlockPos.betweenClosed(BlockPos.containing(foot.minX, foot.minY, foot.minZ),
                BlockPos.containing(foot.maxX, foot.maxY, foot.maxZ))) {
            final VoxelShape under = Shapes.join(level.getBlockState(at).getCollisionShape(level, at)
                    .move(at.getX(), at.getY(), at.getZ()), Shapes.create(foot), BooleanOp.AND);
            if (!under.isEmpty()) {
                floor = Math.max(floor, under.max(Direction.Axis.Y));
            }
        }
        FormedDoors.set(level, door, was);
        return new AABB(middle.x - halfX, floor + OFF_THE_SILL, middle.z - halfZ, middle.x + halfX,
                floor + OFF_THE_SILL + A_PLAYER_TALL, middle.z + halfZ);
    }

    /* The shapes of {@code door}'s leaf's blocks as they stand, where they stand in the world. */
    private static List<VoxelShape> shapes(final ServerLevel level, final FormedDoors.Door door) {
        final List<VoxelShape> out = new ArrayList<>();
        for (final BlockPos at : door.leaf()) {
            out.add(level.getBlockState(at).getCollisionShape(level, at).move(at.getX(), at.getY(), at.getZ()));
        }
        return out;
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
                final List<String> boxes = new ArrayList<>();
                for (final AABB in : Shapes.join(shape, room, BooleanOp.AND).toAabbs()) {
                    boxes.add(String.format("%.3f %.3f %.3f to %.3f %.3f %.3f", in.minX - at.getX(),
                            in.minY - at.getY(), in.minZ - at.getZ(), in.maxX - at.getX(), in.maxY - at.getY(),
                            in.maxZ - at.getZ()));
                }
                out.add(at.toShortString() + " " + boxes);
            }
        }
        return out;
    }

    /* How much its leaf's blocks stand as, all their boxes' volumes added up. */
    private static double volume(final ServerLevel level, final FormedDoors.Door door) {
        double out = 0;
        for (final BlockPos at : door.leaf()) {
            final VoxelShape shape = level.getBlockState(at).getCollisionShape(level, at);
            for (final AABB box : shape.toAabbs()) {
                out += box.getXsize() * box.getYsize() * box.getZsize();
            }
        }
        return out;
    }

    /*
     * The tops of the treads of the way down of {@code outer}, the stair beside its airlock or its ramp behind the
     * tail, from the highest down, as its blocks stand; none while it is not down.
     */
    private static List<Double> stairs(final ServerLevel level, final FormedDoors.Door outer) {
        final FormedPlace place = ((IFormedPlaced) level.getBlockEntity(outer.uses())).places().stream()
                .filter(each -> each.model().equals(outer.model())).findFirst().orElseThrow();
        final List<int[]> cells = ShipDoorRules.RAMP.equals(outer.piece()) ? ShipRampGeometry.CELLS
                : ShipStairGeometry.CELLS;
        // Its blocks' shapes as one, so a tread standing across two blocks is one tread with one top.
        VoxelShape all = Shapes.empty();
        for (final int[] cell : cells) {
            final BlockPos at = FormedDoors.blockOf(outer.uses(), place, MeshCells.key(cell[0], cell[1], cell[2]));
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

    /* The blocks of a boarding stair standing on the grid round the dock's terminal at {@code terminal}. */
    private static List<BlockPos> stairsAround(final ServerLevel level, final BlockPos terminal) {
        final List<BlockPos> found = new ArrayList<>();
        for (final BlockPos at : BlockPos.betweenClosed(terminal.offset(-DOCK_ROUND, -DOCK_ROUND, -DOCK_ROUND),
                terminal.offset(DOCK_ROUND, DOCK_ROUND, DOCK_ROUND))) {
            if (ShipStairs.down(level, at)) {
                found.add(at.immutable());
            }
        }
        return found;
    }

    /* A block of each room beside {@code door}'s leaf, round it level with it: one for each side a room is on. */
    private static List<BlockPos> sides(final ServerLevel level, final FormedDoors.Door door) {
        final Set<Long> seen = new HashSet<>();
        final List<BlockPos> out = new ArrayList<>();
        for (final BlockPos leaf : door.leaf()) {
            for (final Direction side : Direction.Plane.HORIZONTAL) {
                final BlockPos beside = leaf.relative(side);
                SealedRooms.roomAt(level, beside).filter(room -> seen.add(room.id())).ifPresent(room -> out.add(
                        beside));
            }
        }
        return out;
    }

    private static Room room(final ServerLevel level, final BlockPos at) {
        return SealedRooms.roomAt(level, at).orElseThrow(() -> new IllegalStateException("no room at " + at));
    }

    /* How much gas {@code room} holds, every gas added up, in moles. */
    private static double moles(final Room room) {
        return room.gases().values().stream().mapToDouble(Double::doubleValue).sum();
    }

    /* Puts the lunar suit on {@code player}, its tanks empty. */
    private static void suit(final ServerPlayer player) {
        final SpaceSuit suit = SpaceSuits.declared().get(LunarSuit.ID);
        final ItemStack chest = new ItemStack(LunarSuit.SUIT.get());
        LifeSupport.setAir(player.level(), chest, suit, 0);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(LunarSuit.HELMET.get()));
        player.setItemSlot(EquipmentSlot.CHEST, chest);
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(LunarSuit.TROUSERS.get()));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(LunarSuit.BOOTS.get()));
    }

    private static ResourceLocation part(final String path) {
        return SmallShipParts.id("small_" + path);
    }
}
