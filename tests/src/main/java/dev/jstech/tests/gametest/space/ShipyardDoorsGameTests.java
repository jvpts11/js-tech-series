/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.moving.IStructureBody;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureControls;
import dev.jstech.core.moving.StructureYard;
import dev.jstech.core.multiblock.FormedPartBlock;
import dev.jstech.core.multiblock.FormedPartBlockEntity;
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.core.protection.PlayerLimits;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.space.ship.AirReserveEntity;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.ShipFlight;
import dev.jstech.space.ship.ShipKitBlocks;
import dev.jstech.space.ship.ShipSequence;
import dev.jstech.space.ship.SmallShipParts;
import dev.jstech.space.shipyard.ShipDesignMenu;
import dev.jstech.space.shipyard.ShipDesigner;
import dev.jstech.space.shipyard.ShipyardBlocks;
import dev.jstech.space.shipyard.ShipyardDoorTexts;
import dev.jstech.space.shipyard.ShipyardDoors;
import dev.jstech.space.shipyard.ShipyardSounds;
import dev.jstech.space.shipyard.ShipyardStructure;
import dev.jstech.space.shipyard.ShipyardTerminalBlockEntity;
import dev.jstech.testkit.gametest.GameTestPlayers;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.space.TestShipyards;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;

/**
 * The Small Shipyard opens round a finished ship its pilot starts and closes round one taken back: its crane rolls
 * back along its rails to the back of the bay and its floor rises to the ground's top as its cradle folds into it,
 * then the ship goes, standing on its gear on the open floor, whole; a ship on wheels rolls out of its open front, one
 * on legs rises out of its open top. A ship taken back closes it round itself again, its plan the dock's; a dock empty
 * opens by itself, and a design sent to it closes it for the arms to build. It starts nothing it cannot finish: not
 * with something in its crane's way, nor twice at once, while a creature in its pit never holds it; saved halfway it
 * finishes; broken halfway it drops its cradle; and a ship that cannot go at the end has the dock close back round
 * it, its plan kept.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ShipyardDoorsGameTests {

    private static final String ARENA = "shipyard";
    private static final String BATCH = "jsspace_shipyard_doors";
    /* The pit's floor's west front corner of the box the dock stands in, its deck under it, a ground of stone. */
    private static final BlockPos CORNER = new BlockPos(1, 2, 1);
    /* How far a ship on legs rises out of the dock, at least: its take-off's height, less a little. */
    private static final double RISES = ShipSequence.RISES - 1;
    /* The chunks north of the dock a ship leaving it goes over, kept ticking, and the ticket that keeps them. */
    private static final int OUT_CHUNKS = 2;
    /* How far past the dock its test's walls and roof are taken away, in blocks, for a ship leaving it. */
    private static final int OUT = 24;
    /*
     * A ship leaving its dock goes past its test's walls, into where the tests beside it stand when they run with it:
     * each that leaves runs in a batch of its own, none beside it.
     */
    private static final String LEAVING_BY_THE_FRONT = BATCH + "_leaving_front";
    private static final String LEAVING_BY_THE_TOP = BATCH + "_leaving_top";
    private static final TicketType<BlockPos> LAND = TicketType.create("jstests_shipyard_doors", Vec3i::compareTo);
    private static final int TICKING = 2;
    /* How long a dock left empty is watched, in ticks: well past when one just formed opens by itself. */
    private static final int STAYS = 300;
    /* How long a dock waits with its floor not loaded, and tries a blocked closing again, in ticks, in the tests. */
    private static final int UNLOADED_WAIT = 200;
    private static final int RETRIES = 100;
    /* How far a ship standing still may seem to move, in blocks; how many structures a pilot is given at most. */
    private static final double STILL = 0.1;
    private static final int MOST_TAKEN = 1000;
    /* Half a block: a player set on the middle of the terminal's block, to use its screen. */
    private static final double HALF = 0.5;
    /* How long it opens, at the least, in ticks: its crane's seconds and its floor's. */
    private static final long OPENS_IN = (long) ((ShipyardDoors.CRANE_SECONDS + ShipyardDoors.FLOOR_SECONDS) * 20);
    private static final ShipDesign WHEELS = new ShipDesign("JS-02", List.of(part("orbiter_cockpit"),
            part("airlock"), part("quarters"), part("workshop"), part("reactor_flush")), List.of(part("twin_bells"),
            part("side_intakes"), part("blended_delta"), part("twin_canted_fins"), part("blisters"), part("wheels")));
    private static final ShipDesign LEGS = new ShipDesign("JS-04", List.of(part("orbiter_cockpit"),
            part("airlock"), part("quarters"), part("workshop"), part("reactor_flush")), List.of(part("twin_bells"),
            part("side_intakes"), part("blended_delta"), part("twin_canted_fins"), part("blisters"), part("legs")));

    private ShipyardDoorsGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 1200)
    public static void open_rollsTheCraneBackRaisesTheFloorAndLetsTheShipGoOnItsGear(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        final int[] blocks = new int[1];
        final long[] began = new long[1];
        final MovingStructure[] ship = new MovingStructure[1];
        // Whether its floor's running is heard while the floor rises, and while anything else of it moves.
        final boolean[] heard = new boolean[2];
        built(helper, terminal, WHEELS, blocks)
                .thenExecute(() -> {
                    helper.assertTrue(dock(level, terminal).launch(null, why -> { }), "started from its console, the"
                            + " dock begins to open");
                    began[0] = level.getGameTime();
                })
                .thenWaitUntil(() -> {
                    final ShipyardTerminalBlockEntity dock = dock(level, terminal);
                    heard[dock.doors() == ShipyardDoors.State.FLOOR_UP ? 0 : 1] |= dock.loops().stream()
                            .anyMatch(loop -> loop.sound() == ShipyardSounds.DECK);
                    helper.assertTrue(dock.doors() == ShipyardDoors.State.OPEN && dock.open(), "the dock stands open: "
                            + dock.doors());
                })
                .thenExecute(() -> {
                    final long took = level.getGameTime() - began[0];
                    helper.assertTrue(took >= OPENS_IN, "its crane rolls back in three seconds, then its floor rises in"
                            + " five: " + took + " ticks");
                    helper.assertTrue(heard[0] && !heard[1], "its floor is heard running as it rises, and only then: "
                            + heard[0] + ", " + heard[1]);
                })
                .thenWaitUntil(() -> {
                    ship[0] = shipIn(level, terminal);
                    helper.assertTrue(ship[0] != null && ship[0].ready(), "the ship is let go");
                })
                .thenExecute(() -> {
                    helper.assertTrue(standing(level, ShipyardStructure.cranePlaces(terminal, Orientation.NORTH, true))
                            && !standing(level, ShipyardStructure.cranePlaces(terminal, Orientation.NORTH, false)),
                            "its crane stands at the back of the bay");
                    helper.assertTrue(standing(level, ShipyardStructure.deckPlaces(terminal, Orientation.NORTH, true))
                            && !standing(level, ShipyardStructure.deckPlaces(terminal, Orientation.NORTH, false)),
                            "its floor stands at the ground's top");
                    // The cradle's beam stood in the layer the floor rises to: there the floor stands now.
                    final List<BlockPos> deck = ShipyardStructure.deckPlaces(terminal, Orientation.NORTH, true);
                    helper.assertTrue(ShipyardStructure.cradlePlaces(terminal, Orientation.NORTH).stream()
                            .filter(place -> !deck.contains(place)).allMatch(level::isEmptyBlock),
                            "its cradle folded away");
                    helper.assertTrue(ship[0].blocks() == blocks[0], "the ship goes whole: " + ship[0].blocks()
                            + " of " + blocks[0]);
                    // Its console aboard keeps its plan, to draw its parts and to be taken back by a dock.
                    final BlockPos console = ((ShipFlight) ship[0].motion()).consoleIn(ship[0]).orElseThrow();
                    helper.assertTrue(level.getBlockEntity(console) instanceof AirReserveEntity aboard
                            && aboard.ship().isPresent(), "its console aboard keeps its plan");
                    MovingStructures.takeAway(ship[0]);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = LEAVING_BY_THE_FRONT, timeoutTicks = 1600)
    public static void open_aShipOnWheelsRollsOutOfTheFront(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        final MovingStructure[] ship = new MovingStructure[1];
        final double front = ShipyardStructure.bay(terminal, Orientation.NORTH).minZ;
        opened(helper, terminal, WHEELS, ship)
                .thenExecute(() -> {
                    keep(helper, true);
                    unwall(level, ship[0].worldBox().expandTowards(0, 0, -OUT).inflate(1, 0, 1));
                })
                .thenWaitUntil(() -> {
                    pilot(helper, ship[0], StructureControls.ControlAxis.FORWARD);
                    helper.assertTrue(ship[0].worldBox().maxZ < front, "on its wheels it rolls out of the open front:"
                            + " " + (ship[0].worldBox().maxZ - front) + " blocks to go, " + ship[0].motion());
                })
                .thenExecute(() -> {
                    MovingStructures.takeAway(ship[0]);
                    keep(helper, false);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = LEAVING_BY_THE_TOP, timeoutTicks = 1600)
    public static void open_aShipOnLegsRisesOutOfTheTop(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        final MovingStructure[] ship = new MovingStructure[1];
        final double[] from = new double[1];
        opened(helper, terminal, LEGS, ship)
                .thenExecute(() -> {
                    keep(helper, true);
                    unwall(level, ship[0].worldBox().expandTowards(0, OUT, 0).inflate(1, 0, 1));
                })
                .thenWaitUntil(() -> helper.assertTrue(((ShipFlight) ship[0].motion()).grounded(), "let go, it stands"
                        + " on its legs on the open floor: " + ship[0].motion()))
                .thenExecute(() -> from[0] = ship[0].pose().y())
                .thenWaitUntil(() -> {
                    pilot(helper, ship[0], StructureControls.ControlAxis.UP);
                    helper.assertTrue(((ShipFlight) ship[0].motion()).sequence().isPresent(), "UP held standing, it"
                            + " begins its way up: " + ship[0].motion());
                })
                .thenWaitUntil(() -> {
                    pilot(helper, ship[0], null);
                    final ShipFlight flight = (ShipFlight) ship[0].motion();
                    helper.assertTrue(!flight.grounded() && flight.sequence().isEmpty(), "it goes up by itself: "
                            + flight);
                })
                .thenExecute(() -> {
                    helper.assertTrue(ship[0].pose().y() > from[0] + RISES, "on its legs it rose out of the open top: "
                            + (ship[0].pose().y() - from[0]) + " blocks");
                    MovingStructures.takeAway(ship[0]);
                    keep(helper, false);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 1600)
    public static void back_aShipOnTheOpenFloorIsTakenBackAndTheDockClosesRoundIt(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        final MovingStructure[] ship = new MovingStructure[1];
        opened(helper, terminal, WHEELS, ship)
                .thenExecute(() -> helper.assertTrue(dock(level, terminal).seat(ship[0]), "its pilot in the open bay,"
                        + " the dock takes the ship back: formed " + dock(level, terminal).formed() + ", open "
                        + dock(level, terminal).open() + ", " + dock(level, terminal).doors() + ", plan "
                        + dock(level, terminal).plan().isPresent() + ", " + ship[0].motion() + " at "
                        + ship[0].worldBox().move(-terminal.getX(), -terminal.getY(), -terminal.getZ())))
                .thenWaitUntil(() -> helper.assertTrue(dock(level, terminal).doors() == ShipyardDoors.State.CLOSED
                        && dock(level, terminal).formed() && !dock(level, terminal).open(), "and closes round it"))
                .thenExecute(() -> {
                    helper.assertTrue(standing(level, ShipyardStructure.cranePlaces(terminal, Orientation.NORTH, false))
                            && standing(level, ShipyardStructure.deckPlaces(terminal, Orientation.NORTH, false))
                            && ShipyardStructure.cradlePlaces(terminal, Orientation.NORTH).stream()
                            .noneMatch(level::isEmptyBlock), "its crane over the bay, its floor down, its cradle up");
                    helper.assertTrue(dock(level, terminal).plan().isPresent() && dock(level, terminal).built(),
                            "the ship its plan again, whole");
                })
                .thenSucceed();
    }

    /*
     * A ship let go is taken back only when someone asks: its pilot using its flight console in the open bay has the
     * dock take nothing, the ship flying on; Take ship in, asked by its pilot from the dock's screen, has the dock take
     * it and close round it; asked again with no ship in its bay, the dock says so.
     */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 1600)
    public static void back_aShipLetGoIsTakenBackOnlyWhenAsked(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        final ServerPlayer pilot = GameTestPlayers.connected(helper, "dock-pilot");
        final MovingStructure[] ship = new MovingStructure[1];
        final List<Text> told = new ArrayList<>();
        opened(helper, terminal, WHEELS, ship)
                .thenExecute(() -> {
                    final BlockPos console = ((ShipFlight) ship[0].motion()).consoleIn(ship[0]).orElseThrow();
                    level.getBlockState(console).useWithoutItem(level, pilot, new BlockHitResult(
                            Vec3.atCenterOf(console), Direction.UP, console, false));
                    helper.assertTrue(ship[0].motion() instanceof ShipFlight && dock(level, terminal).plan().isEmpty(),
                            "its pilot's hand on its console takes nothing back");
                    helper.assertTrue(dock(level, terminal).takeIn(pilot, told::add)
                            && told.getLast().equals(ShipyardDoorTexts.TAKEN_IN.text()), "Take ship in has the dock "
                            + "take it");
                })
                .thenWaitUntil(() -> helper.assertTrue(dock(level, terminal).doors() == ShipyardDoors.State.CLOSED
                        && dock(level, terminal).plan().isPresent(), "and close round it, the ship its plan again"))
                .thenExecute(() -> {
                    helper.assertTrue(!dock(level, terminal).takeIn(pilot, told::add)
                            && told.getLast().equals(ShipyardDoorTexts.NO_SHIP.text()), "asked with no ship in its "
                            + "bay, it says so");
                    GameTestPlayers.leave(pilot);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 1600)
    public static void empty_aDockOpensByItselfAndADesignSentClosesIt(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(dock(level, terminal).open(), "empty, it opens by itself"))
                .thenExecute(() -> {
                    dock(level, terminal).design(WHEELS);
                    helper.assertTrue(dock(level, terminal).confirm(), "a design sent to it is taken");
                })
                .thenWaitUntil(() -> helper.assertTrue(dock(level, terminal).doors() == ShipyardDoors.State.CLOSED
                        && dock(level, terminal).build().isPresent(), "and it closes for the arms to build it"))
                .thenSucceed();
    }

    /*
     * Only a dock just formed with nothing to build opens by itself: one whose ship was let go from it as it stood
     * closed, the ship flown off, stays closed.
     */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 800)
    public static void empty_aDockWhoseShipWasLetGoClosedStaysClosed(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        built(helper, terminal, WHEELS, new int[1])
                .thenExecute(() -> MovingStructures.takeAway(dock(level, terminal).release(null).orElseThrow()))
                .thenIdle(STAYS)
                .thenExecute(() -> helper.assertTrue(dock(level, terminal).doors() == ShipyardDoors.State.CLOSED
                        && !dock(level, terminal).open(), "its ship let go and gone, it stays closed: "
                        + dock(level, terminal).doors()))
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 800)
    public static void blocked_somethingInTheCranesWayStartsNothing(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        final List<Text> told = new ArrayList<>();
        final BlockPos crane = ShipyardStructure.cranePlaces(terminal, Orientation.NORTH, false).getFirst();
        final BlockPos inTheWay = crane.offset(0, 0, 4);
        built(helper, terminal, WHEELS, new int[1])
                .thenExecute(() -> {
                    level.setBlockAndUpdate(inTheWay, Blocks.STONE.defaultBlockState());
                    helper.assertFalse(dock(level, terminal).launch(null, told::add), "a stone in its crane's way,"
                            + " it does not open");
                    helper.assertTrue(dock(level, terminal).doors() == ShipyardDoors.State.CLOSED && !told.isEmpty()
                            && GameText.resolve(told.getFirst()).contains(inTheWay.toShortString()), "and says why,"
                            + " naming the place: " + told.stream().map(GameText::resolve).toList());
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 1200)
    public static void again_theConsoleUsedAgainWhileItMovesStartsNothingMore(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        final List<Text> told = new ArrayList<>();
        built(helper, terminal, WHEELS, new int[1])
                .thenExecute(() -> {
                    helper.assertTrue(dock(level, terminal).launch(null, why -> { }), "started, it opens");
                    helper.assertFalse(dock(level, terminal).launch(null, told::add), "started again while it"
                            + " moves, nothing more starts");
                    helper.assertTrue(!told.isEmpty() && dock(level, terminal).confirm() == false, "it says it is"
                            + " moving, and takes no design meanwhile: " + told);
                })
                .thenWaitUntil(() -> helper.assertTrue(shipIn(level, terminal) != null, "and lets the ship go once"))
                .thenExecute(() -> MovingStructures.takeAway(shipIn(level, terminal)))
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 1200)
    public static void saved_aDockSavedHalfOpenFinishesOpening(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        built(helper, terminal, WHEELS, new int[1])
                .thenExecute(() -> dock(level, terminal).launch(null, why -> { }))
                .thenWaitUntil(() -> helper.assertTrue(dock(level, terminal).doors() == ShipyardDoors.State.FLOOR_UP,
                        "its floor rising"))
                .thenExecute(() -> {
                    // As the world saved and opened again: its terminal read back from what it saved.
                    final ShipyardTerminalBlockEntity dock = dock(level, terminal);
                    dock.loadWithComponents(dock.saveWithFullMetadata(level.registryAccess()), level.registryAccess());
                })
                .thenWaitUntil(() -> helper.assertTrue(shipIn(level, terminal) != null && dock(level, terminal).open(),
                        "read back halfway, it finishes opening and lets the ship go"))
                .thenExecute(() -> MovingStructures.takeAway(shipIn(level, terminal)))
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 1200)
    public static void broken_aDockBrokenWhileItMovesDropsItsCradle(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        final int cradle = ShipyardStructure.cradlePlaces(terminal, Orientation.NORTH).size();
        built(helper, terminal, WHEELS, new int[1])
                .thenExecute(() -> dock(level, terminal).launch(null, why -> { }))
                .thenWaitUntil(() -> helper.assertTrue(dock(level, terminal).doors() == ShipyardDoors.State.FLOOR_UP,
                        "its floor rising, its cradle off the grid"))
                .thenExecute(() -> level.destroyBlock(terminal, false))
                .thenWaitUntil(() -> {
                    final int dropped = level.getEntitiesOfClass(ItemEntity.class, new AABB(terminal).inflate(2))
                            .stream().filter(item -> item.getItem().is(ShipyardBlocks.CRADLE.get().asItem()))
                            .mapToInt(item -> item.getItem().getCount()).sum();
                    helper.assertTrue(dropped == cradle, "broken halfway, it drops its cradle: " + dropped + " of "
                            + cradle);
                })
                .thenSucceed();
    }

    /*
     * The builder screen's Release button lets the ship built whole go as its flight console does: its menu says the
     * ship is whole, and its call opens the dock round it, which lets the ship go on its gear.
     */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 1200)
    public static void release_theDesignScreensButtonLetsAWholeShipGo(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        built(helper, terminal, WHEELS, new int[1])
                .thenExecute(() -> {
                    final ServerPlayer pilot = GameTestPlayers.connected(helper, "release-pilot");
                    try {
                        pilot.teleportTo(level, terminal.getX() + HALF, terminal.getY() + 1, terminal.getZ() + HALF,
                                0, 0);
                        ShipDesigner.open(pilot, dock(level, terminal));
                        helper.assertTrue(pilot.containerMenu instanceof ShipDesignMenu menu && menu.whole(),
                                "its builder's screen says the ship is whole");
                        helper.assertTrue(ShipDesigner.RELEASE.receive(pilot, true), "its Release is taken");
                        helper.assertTrue(dock(level, terminal).doors() != ShipyardDoors.State.CLOSED, "the dock"
                                + " opens round the ship: " + dock(level, terminal).doors());
                    } finally {
                        pilot.closeContainer();
                        GameTestPlayers.leave(pilot);
                    }
                })
                .thenWaitUntil(() -> helper.assertTrue(shipIn(level, terminal) != null, "and lets the ship go"))
                .thenExecute(() -> MovingStructures.takeAway(shipIn(level, terminal)))
                .thenSucceed();
    }

    /* A ship sent to the dock, not yet built whole: the screen's menu says it is not, and its Release opens nothing. */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void release_aShipNotYetWholeIsNotLetGo(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(dock(level, terminal).formed(), "the dock forms"))
                .thenExecute(() -> {
                    final ShipyardTerminalBlockEntity dock = dock(level, terminal);
                    dock.design(WHEELS);
                    helper.assertTrue(dock.confirm(), "the ship is sent");
                    final ServerPlayer pilot = GameTestPlayers.connected(helper, "release-early");
                    try {
                        pilot.teleportTo(level, terminal.getX() + HALF, terminal.getY() + 1, terminal.getZ() + HALF,
                                0, 0);
                        ShipDesigner.open(pilot, dock);
                        helper.assertTrue(pilot.containerMenu instanceof ShipDesignMenu menu && !menu.whole(),
                                "its builder's screen says the ship is not whole yet");
                        ShipDesigner.RELEASE.receive(pilot, true);
                        helper.assertTrue(dock.doors() == ShipyardDoors.State.CLOSED, "its Release opens nothing: "
                                + dock.doors());
                    } finally {
                        pilot.closeContainer();
                        GameTestPlayers.leave(pilot);
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 1600)
    public static void release_aShipThatCannotGoAtTheEndHasTheDockCloseBack(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        built(helper, terminal, WHEELS, new int[1])
                .thenExecute(() -> dock(level, terminal).launch(null, why -> { }))
                .thenWaitUntil(() -> helper.assertTrue(dock(level, terminal).doors() == ShipyardDoors.State.FLOOR_UP,
                        "its floor rising"))
                .thenExecute(() -> level.setBlockAndUpdate(console(level, terminal), Blocks.AIR.defaultBlockState()))
                .thenWaitUntil(() -> helper.assertTrue(dock(level, terminal).doors() == ShipyardDoors.State.CLOSED
                        && dock(level, terminal).plan().isPresent() && shipIn(level, terminal) == null, "its console"
                        + " gone, the ship cannot go: the dock closes back round it, its plan kept"))
                .thenSucceed();
    }

    /*
     * The ship is never left in the air: opening, the floor rises to meet its gear with the cradle still holding it,
     * and only then does the cradle let go and fold into the floor; closing, the cradle holds it first, and only then
     * does the floor go down from under its gear.
     */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 1600)
    public static void holds_theCradleHoldsTheShipWhileTheFloorMovesUnderIt(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        final MovingStructure[] ship = new MovingStructure[1];
        final List<String> seen = new ArrayList<>();
        // Why the dock, taking the ship back, could not close round it yet, for a failure to say.
        final List<String> refused = new ArrayList<>();
        final GameTestSequence sequence = built(helper, terminal, WHEELS, new int[1])
                .thenExecute(() -> dock(level, terminal).launch(null, why -> { }))
                .thenWaitUntil(() -> {
                    note(seen, dock(level, terminal).doors().serializedName());
                    ship[0] = shipIn(level, terminal);
                    helper.assertTrue(ship[0] != null && ship[0].ready(), "the ship let go");
                })
                .thenExecute(() -> {
                    helper.assertTrue(seen.equals(List.of("closed", "crane_back", "floor_up", "cradle_folds", "open"))
                            || seen.equals(List.of("crane_back", "floor_up", "cradle_folds", "open")), "opening, the"
                            + " floor rises under the ship held, then the cradle folds: " + seen);
                    seen.clear();
                    helper.assertTrue(dock(level, terminal).seat(ship[0], why -> refused.add(why.english())),
                            "the ship taken back");
                });
        sequence.thenWaitUntil(() -> {
            note(seen, dock(level, terminal).doors().serializedName());
            helper.assertTrue(dock(level, terminal).doors() == ShipyardDoors.State.CLOSED, "the dock closed: through "
                    + seen + ", the cradle back " + ShipyardStructure.cradlePlaces(terminal, Orientation.NORTH).stream()
                    .noneMatch(level::isEmptyBlock) + ", refused " + refused + ", structures in the bay "
                    + MovingStructures.near(level, ShipyardStructure.bay(terminal, Orientation.NORTH)).size());
        }).thenExecute(() -> helper.assertTrue(seen.equals(List.of("cradle_holds", "floor_down", "crane_in", "closed"))
                || seen.equals(List.of("open", "cradle_holds", "floor_down", "crane_in", "closed")), "closing, the"
                + " cradle holds the ship, then the floor goes down: " + seen)).thenSucceed();
    }

    /*
     * Its cradle taken off the grid as it opens and put back as it closes, the dock is none the worse: none of its
     * blocks carries the mark of a block lost, as a block broken out of it would.
     */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 1600)
    public static void cycle_openedAndClosedTheDockCarriesNoMarkOfALostBlock(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        final MovingStructure[] ship = new MovingStructure[1];
        opened(helper, terminal, WHEELS, ship)
                .thenExecute(() -> helper.assertTrue(dock(level, terminal).seat(ship[0]), "the ship taken back"))
                .thenWaitUntil(() -> helper.assertTrue(dock(level, terminal).doors() == ShipyardDoors.State.CLOSED
                        && dock(level, terminal).formed() && !dock(level, terminal).open(), "the dock closed again"))
                .thenExecute(() -> {
                    final List<BlockPos> dockBlocks = new ArrayList<>(ShipyardStructure.deckPlaces(terminal,
                            Orientation.NORTH, false));
                    dockBlocks.addAll(ShipyardStructure.cradlePlaces(terminal, Orientation.NORTH));
                    dockBlocks.addAll(ShipyardStructure.cranePlaces(terminal, Orientation.NORTH, false));
                    final long marked = dockBlocks.stream().filter(place -> level.getBlockEntity(place)
                            instanceof FormedPartBlockEntity part && !part.damage().isEmpty()).count();
                    helper.assertTrue(marked == 0, "none of its blocks marked as a block lost: " + marked);
                    // The cradle's blocks put back where the floor stood open, too, not left plain blocks.
                    final List<BlockPos> plain = dockBlocks.stream().filter(place -> !(level.getBlockState(place)
                            .getBlock() instanceof FormedPartBlock)).toList();
                    helper.assertTrue(plain.isEmpty(), "every block of its crane, floor and cradle merged into its"
                            + " model again: " + plain.size() + " plain, " + plain.stream().limit(4).map(place
                            -> level.getBlockState(place).getBlock()).toList());
                })
                .thenSucceed();
    }

    /*
     * Its moving floor not loaded as the dock ticks, as after the world is opened again with the floor's land not yet
     * loaded, the dock waits for it: it is not taken to have arrived, and the ship is not let go onto nothing.
     */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 1200)
    public static void unloaded_aMovingFloorNotLoadedIsWaitedFor(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        final int[] plot = new int[1];
        final int deck = ShipyardStructure.deckPlaces(terminal, Orientation.NORTH, false).size();
        built(helper, terminal, WHEELS, new int[1])
                .thenExecute(() -> dock(level, terminal).launch(null, why -> { }))
                .thenWaitUntil(() -> helper.assertTrue(dock(level, terminal).doors() == ShipyardDoors.State.FLOOR_UP
                        && dock(level, terminal).doorsProgress(level.getGameTime()) > 0, "its floor rising"))
                .thenExecute(() -> {
                    final MovingStructure floor = MovingStructures.near(level, ShipyardStructure.bay(terminal,
                            Orientation.NORTH).inflate(2)).stream().filter(MovingStructure.class::isInstance)
                            .map(MovingStructure.class::cast).filter(body -> body.blocks() == deck).findFirst()
                            .orElseThrow();
                    plot[0] = floor.plot();
                    floor.entity().remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
                })
                .thenIdle(UNLOADED_WAIT)
                .thenExecute(() -> {
                    helper.assertTrue(dock(level, terminal).doors() == ShipyardDoors.State.FLOOR_UP
                            && shipIn(level, terminal) == null, "it waits for its floor, the ship kept: "
                            + dock(level, terminal).doors());
                    StructureYard.giveBack(level, plot[0]);
                })
                .thenSucceed();
    }

    /*
     * A design sent to an open dock whose crane's way is blocked: the player is told what is in the way and where, and
     * the dock closes for it of itself once the way is clear.
     */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 1200)
    public static void closing_refusedIsToldAndTriedAgainOnceClear(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        final List<Text> told = new ArrayList<>();
        final BlockPos inTheWay = ShipyardStructure.cranePlaces(terminal, Orientation.NORTH, true).getFirst()
                .offset(0, 0, -4);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(dock(level, terminal).open(), "empty, it opens by itself"))
                .thenExecute(() -> {
                    level.setBlockAndUpdate(inTheWay, Blocks.STONE.defaultBlockState());
                    dock(level, terminal).design(WHEELS);
                    helper.assertTrue(dock(level, terminal).confirm(told::add), "the design is taken");
                    helper.assertTrue(!told.isEmpty() && GameText.resolve(told.getFirst()).contains(inTheWay
                            .toShortString()), "the player told what is in the crane's way, and where: "
                            + told.stream().map(GameText::resolve).toList());
                })
                .thenIdle(RETRIES)
                .thenExecute(() -> {
                    helper.assertTrue(dock(level, terminal).doors() == ShipyardDoors.State.OPEN, "it stays open"
                            + " while the way is blocked");
                    level.setBlockAndUpdate(inTheWay, Blocks.AIR.defaultBlockState());
                })
                .thenWaitUntil(() -> helper.assertTrue(dock(level, terminal).doors() == ShipyardDoors.State.CLOSED
                        && dock(level, terminal).build().isPresent(), "the way clear, it closes of itself and builds"))
                .thenSucceed();
    }

    /* A ship standing on the open floor is never dropped into the pit: a design sent meanwhile waits for it to go. */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 1600)
    public static void closing_aShipStandingOnTheOpenFloorKeepsTheDockOpen(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        final MovingStructure[] ship = new MovingStructure[1];
        final double[] standing = new double[1];
        final List<Text> told = new ArrayList<>();
        opened(helper, terminal, WHEELS, ship)
                .thenExecute(() -> {
                    standing[0] = ship[0].pose().y();
                    dock(level, terminal).design(LEGS);
                    helper.assertTrue(dock(level, terminal).confirm(told::add) && !told.isEmpty(), "a design sent"
                            + " with a ship on the floor: the player told why it waits: " + told);
                })
                .thenIdle(RETRIES)
                .thenExecute(() -> {
                    helper.assertTrue(dock(level, terminal).doors() == ShipyardDoors.State.OPEN, "the dock stays open"
                            + " under the ship: " + dock(level, terminal).doors());
                    helper.assertTrue(Math.abs(ship[0].pose().y() - standing[0]) < STILL, "the ship still on the"
                            + " floor: " + (ship[0].pose().y() - standing[0]));
                    MovingStructures.takeAway(ship[0]);
                })
                .thenSucceed();
    }

    /*
     * A creature in the pit never holds the dock: it opens round its ship, the floor rising whatever stands in the pit,
     * and closes again round it; one in the way gets hurt as it would by any moving block.
     */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 2400)
    public static void pit_aCreatureInThePitNeverHoldsTheDock(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        final AABB pit = ShipyardStructure.bay(terminal, Orientation.NORTH);
        final MovingStructure[] ship = new MovingStructure[1];
        built(helper, terminal, WHEELS, new int[1])
                .thenExecute(() -> {
                    EntityType.PIG.spawn(level, BlockPos.containing(pit.minX + 1, pit.minY, pit.minZ + 1), null);
                    helper.assertTrue(dock(level, terminal).launch(null, why -> { }), "a pig in its pit, it opens");
                })
                .thenWaitUntil(() -> {
                    ship[0] = shipIn(level, terminal);
                    helper.assertTrue(dock(level, terminal).open() && ship[0] != null && ship[0].ready(), "the floor "
                            + "rises and the ship goes: " + dock(level, terminal).doors());
                })
                .thenExecute(() -> {
                    EntityType.PIG.spawn(level, BlockPos.containing(pit.minX + 1, pit.maxY, pit.minZ + 1), null);
                    helper.assertTrue(dock(level, terminal).seat(ship[0]), "a pig on its floor, it takes the ship "
                            + "back");
                })
                .thenWaitUntil(() -> helper.assertTrue(dock(level, terminal).doors() == ShipyardDoors.State.CLOSED,
                        "and is closed: " + dock(level, terminal).doors()))
                .thenSucceed();
    }

    /* A pilot at their limit of moving structures is told so before the dock opens, which then does not open. */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 800)
    public static void launch_aPilotAtTheirLimitIsToldBeforeTheDockOpens(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        final UUID pilot = UUID.randomUUID();
        final List<Text> told = new ArrayList<>();
        built(helper, terminal, WHEELS, new int[1])
                .thenExecute(() -> {
                    int taken = 0;
                    while (taken < MOST_TAKEN && PlayerLimits.take(level.getServer(), pilot,
                            PlayerLimits.MOVING_STRUCTURES)) {
                        taken++;
                    }
                    try {
                        helper.assertFalse(dock(level, terminal).launch(pilot, told::add), "at their limit, the"
                                + " dock does not open");
                        helper.assertTrue(dock(level, terminal).doors() == ShipyardDoors.State.CLOSED
                                && !told.isEmpty(), "and the pilot is told: " + told);
                    } finally {
                        for (int each = 0; each < taken; each++) {
                            PlayerLimits.giveBack(level.getServer(), pilot, PlayerLimits.MOVING_STRUCTURES);
                        }
                    }
                })
                .thenSucceed();
    }

    /* The dock formed, {@code design} sent and its blocks laid whole in it; {@code blocks} told how many. */
    private static GameTestSequence built(final GameTestHelper helper, final BlockPos terminal,
                                          final ShipDesign design, final int[] blocks) {
        final ServerLevel level = helper.getLevel();
        return helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(dock(level, terminal).formed(), "the dock forms"))
                .thenExecute(() -> {
                    final ShipyardTerminalBlockEntity dock = dock(level, terminal);
                    dock.design(design);
                    helper.assertTrue(dock.confirm(), "the ship is sent");
                    final PlannedBuild plan = dock.build().orElseThrow();
                    for (final PlanSpot spot : plan.plan().spots()) {
                        level.setBlockAndUpdate(plan.where(spot), BuiltInRegistries.BLOCK.get(spot.block())
                                .defaultBlockState());
                    }
                    blocks[0] = plan.plan().spots().size();
                    helper.assertTrue(dock.built(), "the ship stands whole in the dock");
                });
    }

    /* The dock opened round {@code design} built whole in it, {@code ship} told the ship let go once it is ready. */
    private static GameTestSequence opened(final GameTestHelper helper, final BlockPos terminal,
                                           final ShipDesign design, final MovingStructure[] ship) {
        final ServerLevel level = helper.getLevel();
        return built(helper, terminal, design, new int[1])
                .thenExecute(() -> helper.assertTrue(dock(level, terminal).launch(null, why -> { }), "it opens"))
                .thenWaitUntil(() -> {
                    ship[0] = shipIn(level, terminal);
                    helper.assertTrue(ship[0] != null && ship[0].ready() && dock(level, terminal).open(), "open, the"
                            + " ship let go on its gear");
                });
    }

    /*
     * Keeps the land round the dock and on to its north, which a ship leaving it goes over, loaded and ticking by this
     * test's own tickets, or lets it go: a ship waits where it is till the land ahead of it ticks.
     */
    private static void keep(final GameTestHelper helper, final boolean kept) {
        final ChunkPos dock = new ChunkPos(helper.absolutePos(CORNER));
        final BlockPos own = helper.absolutePos(CORNER);
        for (int x = dock.x - 1; x <= dock.x + 1; x++) {
            for (int z = dock.z - OUT_CHUNKS; z <= dock.z + 1; z++) {
                if (kept) {
                    helper.getLevel().getChunkSource().addRegionTicket(LAND, new ChunkPos(x, z), TICKING, own);
                } else {
                    helper.getLevel().getChunkSource().removeRegionTicket(LAND, new ChunkPos(x, z), TICKING, own);
                }
            }
        }
    }

    /* The helm tells {@code ship} its pilot holds {@code axis} this tick, or nothing. */
    private static void pilot(final GameTestHelper helper, final MovingStructure ship,
                              final @Nullable StructureControls.ControlAxis axis) {
        ship.controls().pilot(helper.getLevel().getGameTime(), axis == null ? Map.of() : Map.of(axis, 1F),
                new Vec3(0, 0, -1), new Vec3(0, 1, 0));
    }

    /* The ship flying in the dock's bay, or none. */
    private static MovingStructure shipIn(final ServerLevel level, final BlockPos terminal) {
        for (final IStructureBody body : MovingStructures.near(level, ShipyardStructure.bay(terminal,
                Orientation.NORTH).inflate(2))) {
            if (body instanceof MovingStructure structure && structure.motion() instanceof ShipFlight) {
                return structure;
            }
        }
        return null;
    }

    /* The flight console of the ship planned in the dock. */
    private static BlockPos console(final ServerLevel level, final BlockPos terminal) {
        final PlannedBuild plan = dock(level, terminal).build().orElseThrow();
        return plan.plan().spots().stream().filter(spot -> spot.block().equals(ShipKitBlocks.FLIGHT_CONSOLE.getId()))
                .map(plan::where).findFirst().orElseThrow();
    }

    /*
     * Takes away the barriers the test's own box is walled and roofed with inside {@code box}: a ship leaving the dock
     * goes on past where its test ends, and one flown into a barrier stops dead.
     */
    private static void unwall(final ServerLevel level, final AABB box) {
        BlockPos.betweenClosedStream(box).forEach(at -> {
            if (level.getBlockState(at).is(Blocks.BARRIER)) {
                level.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
            }
        });
    }

    /* Notes {@code state} in {@code seen} when it is not the last one noted. */
    private static void note(final List<String> seen, final String state) {
        if (seen.isEmpty() || !seen.getLast().equals(state)) {
            seen.add(state);
        }
    }

    /* Whether a block stands at every one of {@code places}. */
    private static boolean standing(final ServerLevel level, final List<BlockPos> places) {
        return places.stream().noneMatch(level::isEmptyBlock);
    }

    private static ShipyardTerminalBlockEntity dock(final ServerLevel level, final BlockPos terminal) {
        return (ShipyardTerminalBlockEntity) level.getBlockEntity(terminal);
    }

    private static ResourceLocation part(final String path) {
        return SmallShipParts.id("small_" + path);
    }
}
