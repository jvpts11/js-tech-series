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
import dev.jstech.space.ship.ShipStairGeometry;
import dev.jstech.space.ship.ShipStairs;
import dev.jstech.space.ship.SmallShipParts;
import dev.jstech.space.shipyard.ShipyardTerminalBlockEntity;
import dev.jstech.testkit.gametest.GameTestPlayers;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.space.TestShipyards;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = WAIT)
    public static void airlock_neverStandsOpenInsideAndOutAtOnce(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final PlannedBuild[] plan = new PlannedBuild[1];
        build(helper, plan).thenExecute(() -> {
            final ServerPlayer crew = GameTestPlayers.connected(helper, "jst-lock-crew");
            final FormedDoors.Door outer = door(level, plan[0], AIRLOCK, ShipKitBlocks.AIRLOCK_DOOR.getId(),
                    ShipDoorRules.OUTER_DOOR);
            final List<FormedDoors.Door> inner = ShipDoorRules.otherSide(level, outer);
            helper.assertTrue(inner.size() == 2 && FormedDoors.shut(level, outer), "its outer door stands shut as the"
                    + " ship forms, the airlock a door at each end: " + inner.size());
            helper.assertTrue(!FormedDoors.use(level, crew, outer) && FormedDoors.shut(level, outer), "its outer door"
                    + " stays shut while a door at its ends is open");
            inner.forEach(each -> FormedDoors.use(level, crew, each));
            helper.assertTrue(inner.stream().allMatch(each -> FormedDoors.shut(level, each)), "its inner doors shut");
            helper.assertTrue(FormedDoors.use(level, crew, outer) && !FormedDoors.shut(level, outer), "then its outer"
                    + " door opens");
            helper.assertTrue(!FormedDoors.use(level, crew, inner.getFirst()) && FormedDoors.shut(level,
                    inner.getFirst()), "and its inner doors stay shut while it is open");
            helper.assertTrue(FormedDoors.use(level, crew, outer) && FormedDoors.use(level, crew, inner.getFirst())
                    && !FormedDoors.shut(level, inner.getFirst()), "its outer door shut, an inner one opens");
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
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        at[0] = terminal;
        return helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(level.getBlockEntity(terminal)
                        instanceof ShipyardTerminalBlockEntity dock && dock.formed(), "the dock forms"))
                .thenExecute(() -> {
                    final ShipyardTerminalBlockEntity dock = (ShipyardTerminalBlockEntity) level
                            .getBlockEntity(terminal);
                    dock.design(SHIP);
                    helper.assertTrue(dock.confirm(), "the ship is sent to the dock");
                    plan[0] = dock.build().orElseThrow();
                    for (final PlanSpot spot : plan[0].plan().spots()) {
                        level.setBlockAndUpdate(plan[0].where(spot), BuiltInRegistries.BLOCK.get(spot.block())
                                .defaultBlockState());
                    }
                })
                .thenWaitUntil(() -> helper.assertTrue(plan[0].complete(), "the ship stands whole and formed"));
    }

    /* The door {@code piece} of the part numbered {@code part}, through a block of it that wants {@code block}. */
    private static FormedDoors.Door door(final ServerLevel level, final PlannedBuild plan, final int part,
                                         final ResourceLocation block, final String piece) {
        return named(level, plan.where(spot(plan, part, block)), piece);
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
     * The tops of the treads of the stair beside the airlock of {@code outer}, from the highest down, as its blocks
     * stand; none while its stair is not down.
     */
    private static List<Double> stairs(final ServerLevel level, final FormedDoors.Door outer) {
        final FormedPlace place = ((IFormedPlaced) level.getBlockEntity(outer.uses())).places().stream()
                .filter(each -> each.model().equals(outer.model())).findFirst().orElseThrow();
        final List<Double> tops = new ArrayList<>();
        for (final int[] cell : ShipStairGeometry.CELLS) {
            final BlockPos at = FormedDoors.blockOf(outer.uses(), place, MeshCells.key(cell[0], cell[1], cell[2]));
            if (!ShipStairs.down(level, at)) {
                continue;
            }
            for (final AABB box : level.getBlockState(at).getCollisionShape(level, at).toAabbs()) {
                final double top = at.getY() + box.maxY;
                if (tops.stream().noneMatch(seen -> Math.abs(seen - top) < 1.0e-6)) {
                    tops.add(top);
                }
            }
        }
        tops.sort((a, b) -> Double.compare(b, a));
        return tops;
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
