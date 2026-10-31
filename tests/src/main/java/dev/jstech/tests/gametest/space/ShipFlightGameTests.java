/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.dimension.DimensionEffects;
import dev.jstech.core.jstechseries.persistor.SpacePersistor;
import dev.jstech.core.jstechseries.persistor.SpacePersistorBlockEntity;
import dev.jstech.core.moving.BlockAboard;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureControls;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.moving.StructureSeatEntity;
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.core.region.ChunkLoaders;
import dev.jstech.core.team.Ownership;
import dev.jstech.space.api.ShipParts;
import dev.jstech.space.ship.AirReserveEntity;
import dev.jstech.space.ship.ShipBoost;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.ShipFlight;
import dev.jstech.space.ship.ShipFlightModel;
import dev.jstech.space.ship.ShipGround;
import dev.jstech.space.ship.ShipReadings;
import dev.jstech.space.ship.ShipKitBlocks;
import dev.jstech.space.ship.SmallShipParts;
import dev.jstech.space.shipyard.ShipyardPlans;
import dev.jstech.space.shipyard.ShipyardStructure;
import dev.jstech.space.shipyard.ShipyardTerminalBlockEntity;
import dev.jstech.space.universe.SpaceClimbs;
import dev.jstech.space.universe.StarSystems;
import dev.jstech.space.universe.SystemSpaces;
import dev.jstech.testkit.gametest.GameTestPlayers;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.space.TestShipyards;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A small ship let go from its dock flies as a moving structure: its pilot seat is its helm; taken up, it goes along
 * its nose as its pilot opens its throttle, and turns and pitches its nose up to climb as they hold those controls; it
 * weighs what its parts weigh; fast enough, its wings hold it up, and too slow, nobody at the helm, it sinks onto the
 * land under it and rests there; its boost takes it past its cruise across an empty stretch; parked, it stays where it
 * is; what works on the ground works aboard, a furnace set down in its quarters smelting; a Space Persistor set
 * down aboard keeps it loaded; and pitched up over the build limit it climbs on into space, its pilot at its helm
 * across the line, breathing its cockpit's air there.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ShipFlightGameTests {

    private static final String ARENA = "shipyard";
    private static final String BATCH = "jsspace_ship_flight";
    /* The pattern's lowest corner, the pit floor's west front corner, a ground of stone under it. */
    private static final BlockPos CORNER = new BlockPos(1, 2, 1);
    /* How long the pilot holds a control, how long a ship is watched at rest and boosted, in ticks. */
    private static final int HELD = 30;
    private static final int PARKED = 100;
    private static final int BOOSTED = 20;
    /*
     * How far north of the dock the land is kept loaded, in chunks, as a pilot aboard keeps the land round them, and
     * how far west for a ship that turns and for one that flies straight: the ship flies out of the arena, which alone
     * is kept loaded for a test.
     */
    private static final int FLOWN_CHUNKS = 5;
    private static final int TURNING_WEST = 5;
    private static final int STRAIGHT_WEST = 1;
    /* The tickets each test keeps its land by, and how far each reaches: two keeps its chunk ticking. */
    private static final TicketType<BlockPos> LAND = TicketType.create("jstests_ship_flight", Vec3i::compareTo);
    private static final int TICKING = 2;
    /*
     * How far a ship goes along its nose with its throttle held that long, at least, in blocks: from a hover, its
     * nozzles holding it up take part of its thrust.
     */
    private static final double FLOWN = 6;
    private static final double STILL = 1.0e-6;
    /*
     * How high a ship is taken up to fly, clear of any test's blocks round it, and let go over its cradle to sink,
     * below its dock's crane, in blocks.
     */
    private static final double HIGH = 80;
    private static final double LOW = 2;
    /* A cruise at half again its stall at sea level, and its full cruise in the air, in metres a second. */
    private static final double CRUISE = 30;
    private static final double CRUISING = 40;
    /* The test ship's mass, its eleven parts' sum, in kilograms; what its delta wings hold, in newtons. */
    private static final double SHIP_KILOGRAMS = 39_600;
    private static final double DELTA_NEWTONS = 400_000;
    /* How long its pilot holds up to rise, and it hovers, and holds forward on the land, in ticks. */
    private static final int RISING = 50;
    private static final int HOVERED = 20;
    private static final int ROLLED = 10;
    /* How far a hovering ship may drift up or down, and roll on wheels at least, in blocks; its nozzles, in degrees. */
    private static final double HOVER_DRIFT = 0.05;
    private static final double ROLLS = 0.3;
    private static final int HOVER_NOZZLES = 45;
    /*
     * How far under the line to space a climbing ship is put, in blocks; how far its nose is pitched up, in degrees;
     * how long it is watched across the line, in ticks; how far open its throttle stays, its pilot's hold carried.
     */
    private static final int UNDER_THE_LINE = 80;
    private static final double CLIMBING_PITCH = 60;
    private static final int CROSSED = 10;
    private static final double HELD_OPEN = 0.95;
    private static final ShipDesign LEGS_SHIP = new ShipDesign("JS-04", List.of(part("orbiter_cockpit"),
            part("airlock"), part("quarters"), part("workshop"), part("reactor_flush")), List.of(part("twin_bells"),
            part("side_intakes"), part("blended_delta"), part("twin_canted_fins"), part("blisters"), part("legs")));
    private static final ShipDesign SHIP = new ShipDesign("JS-02", List.of(part("orbiter_cockpit"), part("airlock"),
            part("quarters"), part("workshop"), part("reactor_flush")), List.of(part("twin_bells"),
            part("side_intakes"), part("blended_delta"), part("twin_canted_fins"), part("blisters"), part("wheels")));

    private ShipFlightGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 600)
    public static void flight_theShipFliesAlongItsNoseTurnsAndClimbsAsItsPilotHolds(final GameTestHelper helper) {
        final MovingStructure[] ship = new MovingStructure[1];
        final StructurePose[] from = new StructurePose[1];
        final GameTestSequence sequence = flying(helper, ship, TURNING_WEST).thenExecute(() -> {
            final BlockPos seat = aboard(helper, ship[0], plan(helper).where(spot(helper,
                    ShipKitBlocks.PILOT_SEAT.getId())));
            helper.assertTrue(BlockAboard.at(helper.getLevel(), seat).seat().orElseThrow().helm(), "its pilot"
                    + " seat is its helm");
            aloft(ship[0], HIGH);
            from[0] = ship[0].pose();
        });
        hold(helper, sequence, ship, StructureControls.ControlAxis.FORWARD).thenExecute(() -> {
            final StructurePose now = ship[0].pose();
            helper.assertTrue(from[0].z() - now.z() > FLOWN && Math.abs(now.x() - from[0].x()) < 1, "it flies"
                    + " north along its nose: " + (from[0].z() - now.z()) + " blocks");
            from[0] = now;
        });
        hold(helper, sequence, ship, StructureControls.ControlAxis.LEFT).thenExecute(() -> {
            helper.assertTrue(Math.abs(ship[0].pose().yawDegrees() - from[0].yawDegrees()) > 1, "it turns as its"
                    + " pilot holds left");
            from[0] = ship[0].pose();
        });
        hold(helper, sequence, ship, StructureControls.ControlAxis.UP).thenExecute(() -> helper.assertTrue(
                ship[0].pose().y() > from[0].y() + 1, "it pitches its nose up and climbs as its pilot holds up"))
                .thenExecute(() -> {
                    MovingStructures.takeAway(ship[0]);
                    keep(helper, false, TURNING_WEST);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 600)
    public static void flight_fastItsWingsHoldItUpAndTooSlowItSinksOntoTheLand(final GameTestHelper helper) {
        final MovingStructure[] ship = new MovingStructure[1];
        final StructurePose[] from = new StructurePose[2];
        final GameTestSequence sequence = flying(helper, ship, STRAIGHT_WEST).thenExecute(() -> {
            final ShipFlight flight = (ShipFlight) ship[0].motion();
            helper.assertTrue(flight.ship().equals(ShipFlight.performance(SHIP, ShipParts.all(helper.getLevel())))
                            && Math.abs(flight.ship().mass() - SHIP_KILOGRAMS) < 1
                            && flight.ship().wing() == DELTA_NEWTONS,
                    "it weighs what its parts weigh and its delta wings hold what theirs do: " + flight.ship());
            from[0] = ship[0].pose();
            aloft(ship[0], HIGH);
            // Flying at 30 m/s, half again its stall in this air, its throttle where its pilot left it.
            MovingStructures.move(ship[0], flight.with(new ShipFlightModel.State(CRUISE, 0, CRUISE / CRUISING, 1, 0,
                    0)));
        });
        cruise(helper, sequence, ship, HELD).thenExecute(() -> {
            helper.assertTrue(ship[0].pose().y() > from[0].y() + HIGH - 1, "fast enough, its wings hold it up: "
                    + (ship[0].pose().y() - from[0].y()));
            final ShipReadings shown = console(helper, ship[0]).flight();
            helper.assertTrue(!shown.gearDown() && !shown.lifting() && shown.nozzles() == 0 && shown.speed() > 1,
                    "its console shows it flying on its wings, high, its gear up and its nozzles back: " + shown);
        })
                .thenExecute(() -> {
                    // Back over its dock, a little over its cradle, nobody at the helm, at rest: it sinks onto it.
                    final StructurePose now = ship[0].pose();
                    ship[0].teleport(now.movedTo(from[0].x(), from[0].y() + LOW, from[0].z()));
                    final ShipFlight flight = (ShipFlight) ship[0].motion();
                    MovingStructures.move(ship[0], flight.with(ShipFlightModel.State.RESTING));
                })
                .thenWaitUntil(() -> {
                    final StructurePose now = ship[0].pose();
                    final boolean resting = now.distanceTo(from[1] == null ? now : from[1]) < STILL
                            && ((ShipFlight) ship[0].motion()).state().sink() == 0
                            && ShipGround.meet(helper.getLevel(), ship[0], ship[0].frame()).touching();
                    from[1] = now;
                    helper.assertTrue(resting && now.y() < from[0].y() + LOW - 1, "too slow, it sinks onto its"
                            + " dock's cradle and rests there: " + (now.y() - from[0].y()) + " over where it stood");
                })
                .thenExecute(() -> {
                    MovingStructures.takeAway(ship[0]);
                    keep(helper, false, STRAIGHT_WEST);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 600)
    public static void flight_theBoostTakesItPastItsCruiseAcrossAnEmptyStretch(final GameTestHelper helper) {
        final MovingStructure[] ship = new MovingStructure[1];
        final GameTestSequence sequence = flying(helper, ship, STRAIGHT_WEST).thenExecute(() -> {
            aloft(ship[0], HIGH);
            final ShipFlight flight = (ShipFlight) ship[0].motion();
            MovingStructures.move(ship[0], flight.with(new ShipFlightModel.State(CRUISING, 0, 1, 1, 0, 0)));
        });
        for (int tick = 0; tick < BOOSTED; tick++) {
            sequence.thenExecuteAfter(1, () -> {
                pilot(helper, ship[0], Map.of());
                ShipBoost.hold(ship[0], helper.getLevel().getGameTime());
            });
        }
        sequence.thenExecute(() -> {
            final ShipFlightModel.State now = ((ShipFlight) ship[0].motion()).state();
            helper.assertTrue(now.speed() > CRUISING + 1 && now.charge() < 1, "boosted over an empty stretch, it"
                    + " goes past its cruise and spends its boost: " + now);
        }).thenExecute(() -> {
            MovingStructures.takeAway(ship[0]);
            keep(helper, false, STRAIGHT_WEST);
        }).thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 600)
    public static void flight_itRisesStraightOffItsCradleHoversAndComesBackDown(final GameTestHelper helper) {
        final MovingStructure[] ship = new MovingStructure[1];
        final StructurePose[] from = new StructurePose[2];
        final GameTestSequence sequence = letGo(helper, ship, SHIP).thenExecute(() -> from[0] = ship[0].pose());
        hold(helper, sequence, ship, StructureControls.ControlAxis.UP, RISING).thenExecute(() -> {
            final StructurePose now = ship[0].pose();
            helper.assertTrue(now.y() > from[0].y() + 1 && Math.abs(now.x() - from[0].x()) < STILL
                    && Math.abs(now.z() - from[0].z()) < STILL && now.angleTo(from[0]) < STILL, "held up, it rises"
                    + " straight off its cradle, level: " + (now.y() - from[0].y()) + " blocks");
        });
        cruise(helper, sequence, ship, HOVERED).thenExecute(() -> from[1] = ship[0].pose());
        cruise(helper, sequence, ship, HOVERED).thenExecute(() -> {
            helper.assertTrue(Math.abs(ship[0].pose().y() - from[1].y()) < HOVER_DRIFT, "let go of up, it hovers"
                    + " where it is: " + (ship[0].pose().y() - from[1].y()));
            final ShipReadings shown = console(helper, ship[0]).flight();
            helper.assertTrue(shown.lifting() && shown.gearDown() && shown.nozzles() > HOVER_NOZZLES, "its"
                    + " console shows its nozzles turned down holding it, its gear down: " + shown);
        });
        hold(helper, sequence, ship, StructureControls.ControlAxis.DOWN, RISING * 2).thenWaitUntil(() -> {
            pilot(helper, ship[0], Map.of(StructureControls.ControlAxis.DOWN, 1F));
            helper.assertTrue(((ShipFlight) ship[0].motion()).grounded() && ship[0].pose().y() < from[1].y() - 1,
                    "held down, it comes back down onto its cradle");
        }).thenExecute(() -> MovingStructures.takeAway(ship[0])).thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 600)
    public static void flight_onTheLandItRollsOnWheels(final GameTestHelper helper) {
        final MovingStructure[] ship = new MovingStructure[1];
        final StructurePose[] from = new StructurePose[1];
        final GameTestSequence sequence = letGo(helper, ship, SHIP).thenExecute(() -> from[0] = ship[0].pose());
        hold(helper, sequence, ship, StructureControls.ControlAxis.FORWARD, ROLLED).thenExecute(() -> {
            helper.assertTrue(from[0].z() - ship[0].pose().z() > ROLLS, "on wheels, it rolls forward on its cradle:"
                    + " " + (from[0].z() - ship[0].pose().z()) + " blocks");
            MovingStructures.takeAway(ship[0]);
        }).thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 600)
    public static void flight_onTheLandItStandsOnFeet(final GameTestHelper helper) {
        final MovingStructure[] ship = new MovingStructure[1];
        final StructurePose[] from = new StructurePose[1];
        final GameTestSequence sequence = letGo(helper, ship, LEGS_SHIP).thenExecute(() -> {
            from[0] = ship[0].pose();
            helper.assertTrue(((ShipFlight) ship[0].motion()).ship().gear() == ShipFlightModel.Gear.LEGS, "it stands"
                    + " on feet");
        });
        hold(helper, sequence, ship, StructureControls.ControlAxis.FORWARD, ROLLED).thenExecute(() -> {
            helper.assertTrue(ship[0].pose().distanceTo(from[0]) < STILL, "on feet, it stands where it is with its"
                    + " throttle open: " + ship[0].pose().distanceTo(from[0]));
            MovingStructures.takeAway(ship[0]);
        }).thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 1600)
    public static void flight_itClimbsOverTheBuildLimitAndIntoSpaceWithItsPilotAtTheHelm(final GameTestHelper helper) {
        final MovingStructure[] ship = new MovingStructure[1];
        final ServerPlayer[] pilot = new ServerPlayer[1];
        final ServerLevel overworld = helper.getLevel();
        final ServerLevel space = SystemSpaces.spaceOf(overworld.getServer(), StarSystems.SOLAR_SYSTEM);
        final GameTestSequence sequence = letGo(helper, ship).thenExecute(() -> {
            final BlockPos seat = aboard(helper, ship[0], plan(helper).where(spot(helper,
                    ShipKitBlocks.PILOT_SEAT.getId())));
            // High over the build limit, under where the Overworld becomes its space, its nose pitched up, its
            // throttle open at its cruise.
            final StructurePose now = ship[0].pose();
            ship[0].teleport(now.movedTo(now.x(), SpaceClimbs.ceiling(overworld) - UNDER_THE_LINE, now.z())
                    .turnedBy(1, 0, 0, Math.toRadians(CLIMBING_PITCH)));
            MovingStructures.move(ship[0], ((ShipFlight) ship[0].motion()).with(new ShipFlightModel.State(CRUISING,
                    0, 1, 1, 0, 0)));
            pilot[0] = GameTestPlayers.connected(helper, "pilot");
            pilot[0].setGameMode(GameType.SURVIVAL);
            helper.assertTrue(MovingStructures.sit(pilot[0], seat), "the pilot sits at its helm");
        });
        sequence.thenWaitUntil(() -> {
            // Its pilot sits at the helm holding nothing, its throttle where they left it, open.
            final MovingStructure there = MovingStructures.byId(space, ship[0].id());
            if (there == null) {
                pilot(helper, ship[0], Map.of());
            }
            helper.assertTrue(there != null && there.ready(), "it climbs on, over the build limit and over the line,"
                    + " into the Solar System's space; at " + ship[0].pose().y());
        }).thenExecuteAfter(CROSSED, () -> {
            final MovingStructure there = MovingStructures.byId(space, ship[0].id());
            final ShipFlightModel.State state = ((ShipFlight) there.motion()).state();
            helper.assertTrue(state.speed() > CRUISE && state.throttle() > HELD_OPEN, "it flies on as it went, its"
                    + " pilot's hold carried across: " + state);
            helper.assertTrue(pilot[0].level() == space && pilot[0].getVehicle() instanceof StructureSeatEntity seat
                    && seat.structure() == there, "its pilot crossed with it, at its helm still");
            helper.assertTrue(console(helper, there).flight().speed() > CRUISE, "its console shows its flight");
        }).thenWaitUntil(() -> {
            // Its pilot at the helm still, the ship flying on: in space they breathe the cockpit's air.
            final MovingStructure there = MovingStructures.byId(space, ship[0].id());
            pilot(helper, there, Map.of());
            helper.assertTrue(DimensionEffects.weigh(pilot[0]).isEmpty(), "in space its pilot breathes its"
                    + " cockpit's air, kept warm, not the vacuum round it: " + DimensionEffects.weigh(pilot[0]));
        })
                .thenExecute(() -> {
                    GameTestPlayers.leave(pilot[0]);
                    MovingStructures.takeAway(MovingStructures.byId(space, ship[0].id()));
                }).thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 600)
    public static void flight_aParkedShipStaysWhereItIsWhatWorksAboardWorksAndAPersistorKeepsIt(
            final GameTestHelper helper) {
        final MovingStructure[] ship = new MovingStructure[1];
        final StructurePose[] parked = new StructurePose[1];
        final BlockPos[] furnace = new BlockPos[1];
        final BlockPos[] keeper = new BlockPos[1];
        letGo(helper, ship).thenExecute(() -> {
            parked[0] = ship[0].pose();
            // The quarters' bunk stands along their west side, the floor beside its foot free: a furnace goes there.
            final PlanSpot foot = plan(helper).plan().spots().stream().filter(each -> each.block().equals(
                    ShipKitBlocks.BUNK_FRAME.getId())).max(Comparator.comparingInt(each -> each.at().getZ()))
                    .orElseThrow();
            furnace[0] = aboard(helper, ship[0], plan(helper).where(foot).east());
            helper.getLevel().setBlockAndUpdate(furnace[0], Blocks.FURNACE.defaultBlockState());
            final AbstractFurnaceBlockEntity oven = (AbstractFurnaceBlockEntity) helper.getLevel()
                    .getBlockEntity(furnace[0]);
            oven.setItem(0, new ItemStack(Items.RAW_IRON));
            oven.setItem(1, new ItemStack(Items.COAL));
            // The airlock's floor beside its cycler is free: a Space Persistor goes there, its owner's, charged.
            keeper[0] = aboard(helper, ship[0], plan(helper).where(spot(helper,
                    ShipKitBlocks.AIRLOCK_CYCLER.getId())).south().east());
            helper.getLevel().setBlockAndUpdate(keeper[0], SpacePersistor.BLOCK.get().defaultBlockState());
            final SpacePersistorBlockEntity persistor = (SpacePersistorBlockEntity) helper.getLevel()
                    .getBlockEntity(keeper[0]);
            persistor.own(Ownership.of(UUID.randomUUID()));
            persistor.power().generate(SpacePersistorBlockEntity.CAPACITY);
        }).thenIdle(PARKED).thenWaitUntil(() -> {
            helper.assertTrue(ship[0].pose().distanceTo(parked[0]) < STILL, "parked, it stays where it is");
            helper.assertTrue(ship[0].keepsItsPlace(), "a Space Persistor set down in its airlock keeps it loaded");
            final AbstractFurnaceBlockEntity oven = (AbstractFurnaceBlockEntity) helper.getLevel()
                    .getBlockEntity(furnace[0]);
            helper.assertTrue(oven.getItem(2).is(Items.IRON_INGOT), "a furnace set down in its quarters smelts"
                    + " aboard");
        }).thenExecute(() -> {
            MovingStructures.takeAway(ship[0]);
            helper.assertTrue(ChunkLoaders.loadedFrom(helper.getLevel(), keeper[0]).isEmpty(), "taken away with its"
                    + " ship, the Space Persistor keeps no chunk loaded any more");
        }).thenSucceed();
    }

    /* The dock built, the test's ship built in it and let go, {@code ship} told it once its blocks came in. */
    private static GameTestSequence letGo(final GameTestHelper helper, final MovingStructure[] ship) {
        return letGo(helper, ship, SHIP);
    }

    /* The dock built, a ship of {@code design} built in it and let go, {@code ship} told it once it came in. */
    private static GameTestSequence letGo(final GameTestHelper helper, final MovingStructure[] ship,
                                          final ShipDesign design) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        return helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(level.getBlockEntity(terminal)
                        instanceof ShipyardTerminalBlockEntity dock && dock.formed(), "the dock forms"))
                .thenExecute(() -> {
                    final ShipyardTerminalBlockEntity dock = (ShipyardTerminalBlockEntity) level
                            .getBlockEntity(terminal);
                    dock.design(design);
                    helper.assertTrue(dock.confirm(), "the ship is sent");
                    final PlannedBuild plan = dock.build().orElseThrow();
                    for (final PlanSpot spot : plan.plan().spots()) {
                        level.setBlockAndUpdate(plan.where(spot), BuiltInRegistries.BLOCK.get(spot.block())
                                .defaultBlockState());
                    }
                    ship[0] = dock.release(null).orElseThrow();
                })
                .thenWaitUntil(() -> helper.assertTrue(ship[0].ready(), "its blocks come into the structure"));
    }

    /*
     * The ship let go as {@link #letGo} lets it go, then the land it flies over kept loaded: asked for only once the
     * ship stands, so its own blocks are not kept waiting behind it.
     */
    private static GameTestSequence flying(final GameTestHelper helper, final MovingStructure[] ship,
                                           final int west) {
        return letGo(helper, ship).thenExecute(() -> keep(helper, true, west)).thenWaitUntil(() -> {
            final ChunkPos dock = new ChunkPos(helper.absolutePos(CORNER));
            for (int x = dock.x - west; x <= dock.x + 1; x++) {
                for (int z = dock.z - FLOWN_CHUNKS; z <= dock.z + 1; z++) {
                    helper.assertTrue(helper.getLevel().getChunkSource().getChunkNow(x, z) != null, "the land it"
                            + " flies over loads");
                }
            }
        });
    }

    /* Its pilot holds {@code axis} a while, as their helm tells the structure each tick, then lets go. */
    private static GameTestSequence hold(final GameTestHelper helper, final GameTestSequence sequence,
                                         final MovingStructure[] ship, final StructureControls.ControlAxis axis) {
        return hold(helper, sequence, ship, axis, HELD);
    }

    /* Its pilot holds {@code axis} {@code ticks} ticks, as their helm tells the structure each tick, then lets go. */
    private static GameTestSequence hold(final GameTestHelper helper, final GameTestSequence sequence,
                                         final MovingStructure[] ship, final StructureControls.ControlAxis axis,
                                         final int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            sequence.thenExecuteAfter(1, () -> {
                final Map<StructureControls.ControlAxis, Float> held = new EnumMap<>(
                        StructureControls.ControlAxis.class);
                held.put(axis, 1F);
                pilot(helper, ship[0], held);
            });
        }
        return sequence;
    }

    /* Its pilot sits at the helm {@code ticks} ticks holding nothing, its throttle where they left it. */
    private static GameTestSequence cruise(final GameTestHelper helper, final GameTestSequence sequence,
                                           final MovingStructure[] ship, final int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            sequence.thenExecuteAfter(1, () -> pilot(helper, ship[0], Map.of()));
        }
        return sequence;
    }

    /* The helm tells {@code ship} its pilot holds {@code held} now. */
    private static void pilot(final GameTestHelper helper, final MovingStructure ship,
                              final Map<StructureControls.ControlAxis, Float> held) {
        ship.controls().pilot(helper.getLevel().getGameTime(), held, new Vec3(0, 0, -1), new Vec3(0, 1, 0));
    }

    /* Takes {@code ship} straight up by {@code blocks}, as its taking off will. */
    private static void aloft(final MovingStructure ship, final double blocks) {
        final StructurePose now = ship.pose();
        ship.teleport(now.movedTo(now.x(), now.y() + blocks, now.z()));
    }

    /*
     * Keeps the land round the dock and on to its north, and {@code west} chunks to its west, loaded and ticking by
     * this test's own tickets, or lets it go: a test beside it letting its own land go leaves this one's.
     */
    private static void keep(final GameTestHelper helper, final boolean kept, final int west) {
        final ChunkPos dock = new ChunkPos(helper.absolutePos(CORNER));
        final BlockPos own = helper.absolutePos(CORNER);
        for (int x = dock.x - west; x <= dock.x + 1; x++) {
            for (int z = dock.z - FLOWN_CHUNKS; z <= dock.z + 1; z++) {
                final ChunkPos chunk = new ChunkPos(x, z);
                if (kept) {
                    helper.getLevel().getChunkSource().addRegionTicket(LAND, chunk, TICKING, own);
                } else {
                    helper.getLevel().getChunkSource().removeRegionTicket(LAND, chunk, TICKING, own);
                }
            }
        }
    }

    /* {@code ship}'s flight console, which shows its flight. */
    private static AirReserveEntity console(final GameTestHelper helper, final MovingStructure ship) {
        return (AirReserveEntity) ship.level().getBlockEntity(((ShipFlight) ship.motion()).consoleIn(ship)
                .orElseThrow());
    }

    /* Where the block that stood at {@code built} in the dock is aboard {@code ship} now, in its yard. */
    private static BlockPos aboard(final GameTestHelper helper, final MovingStructure ship, final BlockPos built) {
        return BlockPos.containing(MovingStructures.toYard(ship, Vec3.atCenterOf(built)));
    }

    /* The plan of the ship in the dock, where it was built. */
    private static PlannedBuild plan(final GameTestHelper helper) {
        final BlockPos terminal = helper.absolutePos(CORNER).offset(21, 7, 23);
        return new PlannedBuild(ShipyardPlans.of(helper.getLevel(), SHIP, Orientation.NORTH),
                ShipyardStructure.shipAt(terminal, Orientation.NORTH));
    }

    /* The first place of the ship's plan that wants {@code block}. */
    private static PlanSpot spot(final GameTestHelper helper, final ResourceLocation block) {
        return plan(helper).plan().spots().stream().filter(each -> each.block().equals(block)).findFirst()
                .orElseThrow();
    }

    private static ResourceLocation part(final String path) {
        return SmallShipParts.id("small_" + path);
    }
}
