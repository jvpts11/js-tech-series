/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.conveyor.ConveyorBlock;
import dev.jstech.core.conveyor.ConveyorBlockEntity;
import dev.jstech.core.heat.HeatClimate;
import dev.jstech.core.light.BeamLights;
import dev.jstech.core.light.Beams;
import dev.jstech.core.light.LampBlockEntity;
import dev.jstech.core.light.LightBlock;
import dev.jstech.core.mechanical.IMechanicalPart;
import dev.jstech.core.mechanical.MechanicalPorts;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructureEvent;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureBalance;
import dev.jstech.core.moving.StructureReadings;
import dev.jstech.core.moving.StructureResult;
import dev.jstech.core.moving.StructureYard;
import dev.jstech.core.moving.TowardMotion;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.core.moving.physics.ForceSum;
import dev.jstech.core.moving.physics.PhysicsMotion;
import dev.jstech.core.moving.physics.StructurePhysics;
import dev.jstech.core.multipart.CoreSpaces;
import dev.jstech.core.multipart.MicroShape;
import dev.jstech.core.multipart.Microblock;
import dev.jstech.core.multipart.SpaceBlockEntity;
import dev.jstech.core.multipart.SpacePlacement;
import dev.jstech.core.multipart.SpaceSlots;
import dev.jstech.core.region.Box;
import dev.jstech.core.signal.Band;
import dev.jstech.core.signal.RadioBlockEntity;
import dev.jstech.core.signal.Reading;
import dev.jstech.core.signal.SensorBlockEntity;
import dev.jstech.core.signal.Signals;
import dev.jstech.core.signal.Tuning;
import dev.jstech.core.team.Ownership;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.weather.Weathers;
import dev.jstech.core.worldgen.biome.BiomeClimates;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestAboard;
import dev.jstech.tests.TestConveyors;
import dev.jstech.tests.TestLights;
import dev.jstech.tests.TestMechanical;
import dev.jstech.tests.TestPhysics;
import dev.jstech.tests.TestRadios;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.joml.Vector3d;

/**
 * Seeing, measuring and moving moving structures by hand, and what works aboard them, in a real world: each force and
 * the harm of impacts turned off by its setting; the wind stilled; a hard blow, a structure parted, and one docked and
 * let go each heard on the game's bus; a push, a freeze and a grip from outside; a flight sensor aboard reading its
 * height and the ground below in redstone; the air, the warmth and the weather aboard those of where the structure is;
 * a floodlight aboard lighting the world below it as it goes; a shaft turning a propeller aboard; a radio aboard heard
 * on the ground; belts carrying aboard into a chest beside a microblock; and all of it taken to the Nether.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MovingStructureToolsGameTests {

    private static final String ARENA = "empty";
    /* An arena 32 high, for what flies. */
    private static final String TALL = "tall";
    private static final String BATCH = "jscore_moving_tools";
    // Each switch is the server's, and would turn the force off for every structure of its batch too.
    private static final String GRAVITY_BATCH = "jscore_moving_switch_gravity";
    private static final String IMPACTS_BATCH = "jscore_moving_switch_impacts";
    private static final String WIND_BATCH = "jscore_moving_switch_wind";
    /* How close, in blocks, a resting structure is to where it should rest. */
    private static final double RESTS = 0.03;
    /* The floor's top, in the arena's places. */
    private static final int FLOOR = 2;
    /* How long the weather a test sets lasts, in ticks. */
    private static final int WEATHER = 12_000;
    private static final int DOWN = SpaceSlots.face(Direction.DOWN.get3DDataValue());

    private MovingStructureToolsGameTests() {
    }

    @GameTest(template = ARENA, batch = GRAVITY_BATCH, timeoutTicks = 100)
    public static void switches_gravityOffLeavesABlockHangingWhereItIs(final GameTestHelper helper) {
        final MovingStructure block = made(helper, List.of(new BlockPos(4, 5, 4)), Blocks.STONE);
        final double start = block.worldBox().minY;
        StructureBalance.gravity(false);
        block.move(PhysicsMotion.AT_REST);
        helper.runAfterDelay(30, () -> {
            final double now = block.worldBox().minY;
            StructureBalance.gravity(true);
            MovingStructures.takeAway(block);
            helper.assertTrue(Math.abs(now - start) < RESTS, "with no gravity it hangs where it was let go: " + now
                    + " from " + start);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, batch = IMPACTS_BATCH, timeoutTicks = 120)
    public static void switches_impactsOffLeaveTheGlassAHeavyBlockFallsOnWhole(final GameTestHelper helper) {
        helper.setBlock(new BlockPos(4, 2, 4), Blocks.GLASS);
        final MovingStructure iron = made(helper, List.of(new BlockPos(4, 5, 4)), Blocks.IRON_BLOCK);
        StructureBalance.impacts(false);
        iron.move(PhysicsMotion.AT_REST);
        helper.runAfterDelay(60, () -> {
            final double lies = iron.worldBox().minY;
            StructureBalance.impacts(true);
            MovingStructures.takeAway(iron);
            helper.assertBlockPresent(Blocks.GLASS, new BlockPos(4, 2, 4));
            helper.assertTrue(Math.abs(lies - y(helper, 3)) < RESTS, "it lies on the glass, unbroken: " + lies);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, batch = WIND_BATCH, timeoutTicks = 200)
    public static void switches_windOffStillsTheAirOfAStorm(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        level.setWeatherParameters(0, WEATHER, true, true);
        final List<Double> drag = new ArrayList<>();
        final MovingStructure blown = made(helper, List.of(new BlockPos(4, 4, 4)), TestPhysics.HOVER.get());
        blown.move(PhysicsMotion.AT_REST);
        helper.startSequence()
                .thenExecuteAfter(20, () -> {
                    drag.add(dragOn(blown));
                    MovingStructures.takeAway(blown);
                    StructureBalance.wind(false);
                })
                .thenExecuteAfter(2, () -> {
                    final MovingStructure still = made(helper, List.of(new BlockPos(4, 4, 4)),
                            TestPhysics.HOVER.get());
                    still.move(PhysicsMotion.AT_REST);
                    helper.runAfterDelay(20, () -> {
                        drag.add(dragOn(still));
                        StructureBalance.wind(true);
                        level.setWeatherParameters(WEATHER, 0, false, false);
                        MovingStructures.takeAway(still);
                    });
                })
                .thenWaitUntil(() -> helper.assertTrue(drag.size() == 2, "both are weighed"))
                .thenExecute(() -> {
                    helper.assertTrue(drag.get(0) > 0, "in a storm the wind drags a structure standing still: "
                            + drag.get(0) + " N");
                    helper.assertTrue(drag.get(1) < 1.0e-6, "with the wind off, the storm's air is still: "
                            + drag.get(1) + " N");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void events_aHardBlowIsHeardAsACollision(final GameTestHelper helper) {
        final MovingStructure iron = made(helper, List.of(new BlockPos(4, 6, 4)), Blocks.IRON_BLOCK);
        final List<MovingStructureEvent.Collision> heard = new CopyOnWriteArrayList<>();
        final Consumer<MovingStructureEvent.Collision> ear = event -> {
            if (event.structure() == iron) {
                heard.add(event);
            }
        };
        NeoForge.EVENT_BUS.addListener(MovingStructureEvent.Collision.class, ear);
        iron.move(PhysicsMotion.AT_REST);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(!heard.isEmpty(), "landing hard, the blow is heard"))
                .thenExecute(() -> {
                    NeoForge.EVENT_BUS.unregister(ear);
                    final MovingStructureEvent.Collision blow = heard.get(0);
                    helper.assertTrue(blow.other() == null, "against the world, not another structure");
                    helper.assertTrue(blow.speed() >= 1 && blow.energy() > 0, "closing at " + blow.speed()
                            + " m/s with " + blow.energy() + " J");
                    helper.assertTrue(Math.abs(blow.at().y - y(helper, FLOOR)) < 0.6, "at the floor: " + blow.at());
                    MovingStructures.takeAway(iron);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void events_aStructureBrokenInTwoIsHeardParted(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final MovingStructure bar = made(helper, List.of(new BlockPos(2, 5, 4), new BlockPos(3, 5, 4),
                new BlockPos(4, 5, 4), new BlockPos(5, 5, 4)), Blocks.STONE);
        final BlockPos middle = new BlockPos((bar.bounds().minX() + bar.bounds().maxX()) / 2, bar.bounds().minY(),
                bar.bounds().minZ());
        final List<MovingStructureEvent.Parted> heard = new CopyOnWriteArrayList<>();
        final Consumer<MovingStructureEvent.Parted> ear = event -> {
            if (event.structure() == bar) {
                heard.add(event);
            }
        };
        NeoForge.EVENT_BUS.addListener(MovingStructureEvent.Parted.class, ear);
        helper.startSequence()
                .thenExecute(() -> level.setBlock(middle, Blocks.AIR.defaultBlockState(), 3))
                .thenWaitUntil(() -> helper.assertTrue(!heard.isEmpty(), "the parting is heard"))
                .thenExecute(() -> {
                    NeoForge.EVENT_BUS.unregister(ear);
                    final List<MovingStructure> pieces = heard.get(0).pieces();
                    helper.assertTrue(pieces.size() == 1 && pieces.get(0) != bar, "with the piece it parted into");
                    for (final MovingStructure piece : pieces) {
                        MovingStructures.takeAway(piece);
                    }
                    MovingStructures.takeAway(bar);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 120)
    public static void events_aStructureDockedAndLetGoIsHeardBothTimes(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(2, 3, 4), TestAboard.CLAMP.get().defaultBlockState()
                .setValue(DirectionalBlock.FACING, Direction.EAST));
        helper.setBlock(new BlockPos(1, 3, 4), Blocks.REDSTONE_BLOCK);
        final MovingStructure host = assembled(helper, new BlockPos(1, 3, 4), new BlockPos(2, 3, 4));
        helper.setBlock(new BlockPos(4, 3, 4), TestAboard.CLAMP.get().defaultBlockState()
                .setValue(DirectionalBlock.FACING, Direction.WEST));
        final MovingStructure visitor = assembled(helper, new BlockPos(4, 3, 4), new BlockPos(4, 3, 4));
        final List<String> heard = new CopyOnWriteArrayList<>();
        final Consumer<MovingStructureEvent.Docked> docked = event -> {
            if (event.structure() == visitor && event.host() == host) {
                heard.add("docked");
            }
        };
        final Consumer<MovingStructureEvent.Undocked> undocked = event -> {
            if (event.structure() == visitor && event.host() == host) {
                heard.add("undocked");
            }
        };
        NeoForge.EVENT_BUS.addListener(MovingStructureEvent.Docked.class, docked);
        NeoForge.EVENT_BUS.addListener(MovingStructureEvent.Undocked.class, undocked);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(heard.contains("docked"), "the docking is heard"))
                .thenExecute(() -> {
                    final BlockPos power = find(level, host, state -> state.is(Blocks.REDSTONE_BLOCK));
                    level.setBlock(power, Blocks.STONE.defaultBlockState(), 3);
                })
                .thenWaitUntil(() -> helper.assertTrue(heard.contains("undocked"), "and the letting go"))
                .thenExecute(() -> {
                    NeoForge.EVENT_BUS.unregister(docked);
                    NeoForge.EVENT_BUS.unregister(undocked);
                    helper.assertTrue(heard.equals(List.of("docked", "undocked")), "each once, in turn: " + heard);
                    MovingStructures.takeAway(visitor);
                    MovingStructures.takeAway(host);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 120)
    public static void push_aBlowSendsAStructureAtRestTheWayItPushes(final GameTestHelper helper) {
        final MovingStructure block = made(helper, List.of(new BlockPos(3, 2, 4)), Blocks.OAK_PLANKS);
        final double start = block.worldBox().minX;
        final Vec3 middle = block.worldBox().getCenter();
        MovingStructures.push(block, new Vec3(2_000, 0, 0), middle);
        helper.startSequence()
                .thenExecuteAfter(10, () -> {
                    helper.assertTrue(block.physics() != null, "pushed, it moves by its physics");
                    helper.assertTrue(block.worldBox().minX > start + 0.3, "east, the way it was pushed: "
                            + block.worldBox().minX + " from " + start);
                    helper.assertTrue(Math.abs(block.worldBox().minZ - Math.floor(block.worldBox().minZ)) < 0.05
                            || Math.abs(block.worldBox().minZ - Math.ceil(block.worldBox().minZ)) < 0.05,
                            "and not aside: " + block.worldBox());
                    MovingStructures.takeAway(block);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void freeze_holdsAStructureInTheAirAndLetsItFallAgain(final GameTestHelper helper) {
        final MovingStructure block = made(helper, List.of(new BlockPos(4, 6, 4)), Blocks.STONE);
        block.move(PhysicsMotion.AT_REST);
        final double[] held = new double[1];
        helper.startSequence()
                .thenExecuteAfter(3, () -> {
                    MovingStructures.freeze(block, true);
                    held[0] = block.worldBox().minY;
                    helper.assertTrue(MovingStructures.frozen(block), "it is frozen");
                })
                .thenExecuteAfter(20, () -> {
                    helper.assertTrue(Math.abs(block.worldBox().minY - held[0]) < 1.0e-6,
                            "frozen, it stays where it was in the air: " + block.worldBox().minY + " from " + held[0]);
                    helper.assertTrue(held[0] > y(helper, FLOOR) + 1, "above the floor: " + held[0]);
                    MovingStructures.freeze(block, false);
                    helper.assertTrue(!MovingStructures.frozen(block), "and let go");
                })
                .thenWaitUntil(() -> helper.assertTrue(asleep(block)
                        && Math.abs(block.worldBox().minY - y(helper, FLOOR)) < RESTS, "it falls to the floor"))
                .thenExecute(() -> MovingStructures.takeAway(block))
                .thenSucceed();
    }

    @GameTest(template = TALL, batch = BATCH, timeoutTicks = 200)
    public static void hold_aGripFromOutsideCarriesAStructureUpAndBearsItsWeight(final GameTestHelper helper) {
        final MovingStructure block = made(helper, List.of(new BlockPos(4, 2, 4)), Blocks.STONE);
        final Box box = block.bounds();
        final Vec3 place = new Vec3(box.minX() + 0.5, box.minY() + 0.5, box.minZ() + 0.5);
        final Vec3 target = block.worldBox().getCenter().add(0, 3, 0);
        final boolean[] holding = {true};
        helper.onEachTick(() -> {
            if (holding[0]) {
                MovingStructures.hold(block, place, target, 200_000, helper.getLevel().getGameTime() + 3);
            }
        });
        helper.startSequence()
                .thenExecuteAfter(60, () -> {
                    final Vec3 at = block.worldBox().getCenter();
                    helper.assertTrue(at.distanceTo(target) < 0.25, "held at the target: " + at + " for " + target);
                    final StructurePhysics physics = block.physics();
                    helper.assertTrue(physics != null && physics.forces().of(ForceSum.ForceKind.HELD).y > 0,
                            "the grip bears its weight");
                    holding[0] = false;
                    MovingStructures.letGo(block);
                })
                .thenWaitUntil(() -> helper.assertTrue(asleep(block)
                        && Math.abs(block.worldBox().minY - y(helper, FLOOR)) < RESTS, "let go, it falls back"))
                .thenExecute(() -> MovingStructures.takeAway(block))
                .thenSucceed();
    }

    @GameTest(template = TALL, batch = BATCH, timeoutTicks = 120)
    public static void readings_aFlightSensorAboardReadsItsHeightAndTheGroundInRedstone(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(4, 8, 4), TestPhysics.FLIGHT_SENSOR.get());
        helper.setBlock(new BlockPos(5, 8, 4), Blocks.STONE);
        final MovingStructure craft = assembled(helper, new BlockPos(4, 8, 4), new BlockPos(5, 8, 4));
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    final BlockPos sensor = find(level, craft, state -> state.is(TestPhysics.FLIGHT_SENSOR.get()));
                    helper.assertTrue(sensor != null, "the sensor is aboard");
                    final List<Reading> readings = StructureReadings.of(level, sensor);
                    final double height = helper.absolutePos(new BlockPos(4, 8, 4)).getY() + 0.5;
                    helper.assertTrue(Math.abs(reading(readings, StructureReadings.ALTITUDE) - height) < 0.01,
                            "its altitude is where it is in the world: " + readings);
                    helper.assertTrue(Math.abs(reading(readings, StructureReadings.GROUND) - (height
                            - y(helper, FLOOR))) < 0.3, "the ground below, read through the structure: " + readings);
                    final SensorBlockEntity entity = (SensorBlockEntity) level.getBlockEntity(sensor);
                    entity.read();
                    helper.assertTrue(entity.signal() == readings.get(0).strength(),
                            "its redstone speaks its altitude: " + entity.signal());
                    helper.assertTrue(level.getCapability(Signals.OUTPUT, sensor, null) != null,
                            "and programs and the network read it too");
                    craft.move(new TowardMotion(craft.pose().movedTo(craft.pose().x(), craft.pose().y() + 4,
                            craft.pose().z()), 4, 0));
                })
                .thenExecuteAfter(5, () -> {
                    final BlockPos sensor = find(level, craft, state -> state.is(TestPhysics.FLIGHT_SENSOR.get()));
                    final List<Reading> readings = StructureReadings.of(level, sensor);
                    helper.assertTrue(reading(readings, StructureReadings.CLIMB) > 0.5
                            && reading(readings, StructureReadings.SPEED) > 0.5, "climbing, it reads its climb: "
                            + readings);
                    MovingStructures.takeAway(craft);
                })
                .thenSucceed();
    }

    @GameTest(template = TALL, batch = BATCH, timeoutTicks = 100)
    public static void queries_aboardAskTheAirWeatherAndWarmthOfWhereTheStructureIs(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final MovingStructure craft = made(helper, List.of(new BlockPos(4, 2, 4)), Blocks.STONE);
        craft.teleport(craft.pose().movedTo(craft.pose().x() + 3, craft.pose().y() + 24, craft.pose().z() - 2));
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    final BlockPos yard = find(level, craft, state -> state.is(Blocks.STONE));
                    final BlockPos world = MovingStructures.inWorld(level, yard);
                    helper.assertTrue(world.getY() != yard.getY() || world.getX() != yard.getX(),
                            "the yard is not where the structure is: " + yard + " and " + world);
                    helper.assertTrue(HeatClimate.air(level, yard).equals(HeatClimate.air(level, world)),
                            "the air aboard is that of where it is: " + HeatClimate.air(level, yard) + " and "
                                    + HeatClimate.air(level, world));
                    helper.assertTrue(BiomeClimates.celsius(level, yard) == BiomeClimates.celsius(level, world),
                            "and its warmth");
                    helper.assertTrue(Weathers.at(level, yard).equals(Weathers.at(level, world))
                            && Weathers.windAt(level, yard) == Weathers.windAt(level, world), "and its weather");
                    MovingStructures.takeAway(craft);
                })
                .thenSucceed();
    }

    @GameTest(template = TALL, batch = BATCH, timeoutTicks = 300)
    public static void lamps_aboardLightTheWorldBelowAsTheStructureGoes(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        // The arena's own floor lets light through, as a barrier does; the stone below it is what the beam meets.
        for (int x = 0; x <= 9; x++) {
            for (int z = 1; z <= 7; z++) {
                helper.setBlock(new BlockPos(x, FLOOR, z), Blocks.STONE);
            }
        }
        helper.setBlock(new BlockPos(3, 9, 4), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 8, 4), TestLights.FLOODLIGHT.get().defaultBlockState()
                .setValue(DirectionalBlock.FACING, Direction.DOWN));
        final MovingStructure craft = assembled(helper, new BlockPos(3, 8, 4), new BlockPos(3, 9, 4));
        final BlockPos first = helper.absolutePos(new BlockPos(3, FLOOR + 1, 4));
        final BlockPos second = helper.absolutePos(new BlockPos(6, FLOOR + 1, 4));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(lit(level, first), "the floodlight aboard lights the floor "
                        + "below it: " + level.getBlockState(first) + "; " + lamp(level, craft)))
                .thenExecute(() -> craft.move(new TowardMotion(craft.pose().movedTo(craft.pose().x() + 3,
                        craft.pose().y(), craft.pose().z()), 4, 0)))
                .thenWaitUntil(() -> helper.assertTrue(lit(level, second) && !lit(level, first),
                        "gone three blocks on, it lights where it is now and no longer where it was"))
                .thenExecute(() -> MovingStructures.takeAway(craft))
                .thenSucceed();
    }

    @GameTest(template = TALL, batch = BATCH, timeoutTicks = 200)
    public static void shafts_aboardTurnAPropellerThatPushesTheStructure(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(3, 8, 4), TestMechanical.CRANK.get().defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        helper.setBlock(new BlockPos(4, 8, 4), TestMechanical.PROPELLER.get().defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        final MovingStructure craft = assembled(helper, new BlockPos(3, 8, 4), new BlockPos(4, 8, 4));
        craft.move(PhysicsMotion.AT_REST);
        helper.startSequence()
                .thenWaitUntil(() -> {
                    final StructurePhysics physics = craft.physics();
                    final Vector3d thrust = physics == null ? new Vector3d()
                            : physics.forces().of(ForceSum.ForceKind.THRUST);
                    final BlockPos propeller = find(level, craft, state -> state.is(TestMechanical.PROPELLER.get()));
                    final IMechanicalPart turning = propeller == null ? null
                            : MechanicalPorts.at(level, propeller, null);
                    helper.assertTrue(thrust.z < -1_000, "the crank turns the shaft aboard, and its propeller pushes "
                            + "north: " + thrust + " at " + (turning == null ? "no part" : turning.rpm() + " rpm"));
                })
                .thenExecute(() -> MovingStructures.takeAway(craft))
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void radios_aboardAreHeardOnTheGroundFromWhereTheStructureIs(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final UUID owner = UUID.nameUUIDFromBytes("jstests-moving-radio".getBytes(StandardCharsets.UTF_8));
        final Tuning tuning = Tuning.on(Band.PRIVATE, 11);
        helper.setBlock(new BlockPos(2, 3, 4), TestRadios.TRANSMITTER.get());
        radio(helper, new BlockPos(2, 3, 4)).own(Ownership.of(owner));
        radio(helper, new BlockPos(2, 3, 4)).tune(tuning);
        helper.setBlock(new BlockPos(2, 2, 4), Blocks.STONE);
        final MovingStructure craft = assembled(helper, new BlockPos(2, 2, 4), new BlockPos(2, 3, 4));
        helper.setBlock(new BlockPos(7, 2, 4), TestRadios.RECEIVER.get());
        final RadioBlockEntity receiver = radio(helper, new BlockPos(7, 2, 4));
        receiver.own(Ownership.of(owner));
        receiver.tune(tuning);
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    final BlockPos aboard = find(level, craft, state -> state.is(TestRadios.TRANSMITTER.get()));
                    helper.assertTrue(aboard != null && level.getBlockEntity(aboard) instanceof RadioBlockEntity,
                            "the transmitter is aboard");
                    ((RadioBlockEntity) level.getBlockEntity(aboard)).send(Reading.FULL);
                })
                .thenWaitUntil(() -> helper.assertTrue(receiver.signal() == Reading.FULL,
                        "the receiver five blocks from the structure hears it: " + receiver.signal()))
                .thenExecute(() -> MovingStructures.takeAway(craft))
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 300)
    public static void belts_aboardCarryIntoAChestBesideAMicroblock(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final MovingStructure craft = rig(helper);
        craft.move(new TowardMotion(craft.pose().movedTo(craft.pose().x(), craft.pose().y(), craft.pose().z() + 2),
                1, 0));
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    helper.assertTrue(slabAboard(level, craft), "the microblock came aboard whole");
                    belts(level, craft).get(0).put(new ItemStack(Items.DIAMOND, 3), 0.0);
                })
                .thenWaitUntil(() -> helper.assertTrue(diamonds(level, craft) == 3,
                        "the belts aboard carry the diamonds into the chest, the structure going"))
                .thenExecute(() -> MovingStructures.takeAway(craft))
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 1200)
    public static void travel_takesBeltsAMicroblockAndARadioToTheNether(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final ServerLevel nether = level.getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "there is a Nether");
        final MovingStructure craft = rig(helper);
        final Vec3 there = new Vec3(helper.absolutePos(BlockPos.ZERO).getX() + 0.5, 170,
                helper.absolutePos(BlockPos.ZERO).getZ() + 20.5);
        final int landX = (int) Math.floor(there.x) >> 4;
        final int landZ = (int) Math.floor(there.z) >> 4;
        // Nobody is in the Nether to keep the land it arrives on loaded, and the structure on it; the test does.
        forced(nether, landX, landZ, true);
        helper.startSequence()
                .thenExecute(() -> {
                    final StructureResult sent = MovingStructures.travel(craft, nether, there);
                    helper.assertTrue(sent.ok(), "the structure is sent to the Nether: "
                            + (sent.refusal() == null ? "done" : sent.refusal().english()));
                })
                .thenWaitUntil(() -> {
                    final MovingStructure arrived = MovingStructures.byId(nether, craft.id());
                    helper.assertTrue(arrived != null && arrived.ready(), "it arrives in the Nether");
                })
                .thenExecute(() -> {
                    final MovingStructure arrived = MovingStructures.byId(nether, craft.id());
                    helper.assertTrue(slabAboard(nether, arrived), "with its microblock");
                    helper.assertTrue(find(nether, arrived, state -> state.is(TestRadios.TRANSMITTER.get())) != null,
                            "and its radio");
                    belts(nether, arrived).get(0).put(new ItemStack(Items.DIAMOND, 3), 0.0);
                })
                .thenWaitUntil(() -> {
                    final MovingStructure arrived = MovingStructures.byId(nether, craft.id());
                    helper.assertTrue(diamonds(nether, arrived) == 3, "and its belts carry there as they did; "
                            + carrying(nether, arrived));
                })
                .thenExecute(() -> {
                    MovingStructures.takeAway(MovingStructures.byId(nether, craft.id()));
                    forced(nether, landX, landZ, false);
                })
                .thenSucceed();
    }

    /* Keeps the chunks round {@code (x, z)} loaded, or lets them go. */
    private static void forced(final ServerLevel level, final int x, final int z, final boolean kept) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                level.setChunkForced(x + dx, z + dz, kept);
            }
        }
    }

    /*
     * A rig of three belts running east into a chest, on a stone base, a slab of oak on the chest and a transmitter
     * on the base: belts, a microblock and a radio in one structure.
     */
    private static MovingStructure rig(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        for (int x = 1; x <= 4; x++) {
            helper.setBlock(new BlockPos(x, 2, 2), Blocks.STONE);
        }
        for (int x = 1; x <= 3; x++) {
            helper.setBlock(new BlockPos(x, 3, 2), TestConveyors.BELT.get().defaultBlockState()
                    .setValue(ConveyorBlock.FACING, Direction.EAST));
        }
        helper.setBlock(new BlockPos(4, 3, 2), Blocks.CHEST);
        helper.setBlock(new BlockPos(5, 2, 2), TestRadios.TRANSMITTER.get());
        final BlockPos chest = helper.absolutePos(new BlockPos(4, 3, 2));
        final Microblock.MicroblockPiece slab = new Microblock.MicroblockPiece(MicroShape.FACE, MicroShape.SLAB,
                Blocks.OAK_PLANKS);
        final Optional<SpacePlacement.Placement> placed = SpacePlacement.target(level, chest,
                Vec3.atCenterOf(chest).add(0, 0.5, 0), Direction.UP, slab);
        helper.assertTrue(placed.isPresent() && SpacePlacement.place(level, placed.get()), "the slab is placed");
        helper.assertBlockPresent(CoreSpaces.BLOCK.get(), new BlockPos(4, 4, 2));
        return assembled(helper, new BlockPos(1, 2, 2), new BlockPos(5, 4, 2));
    }

    /* Whether the rig's slab is aboard, in its space's floor. */
    private static boolean slabAboard(final ServerLevel level, final MovingStructure craft) {
        final BlockPos space = find(level, craft, state -> state.is(CoreSpaces.BLOCK.get()));
        return space != null && level.getBlockEntity(space) instanceof SpaceBlockEntity parts
                && parts.parts().microblock(DOWN) != null;
    }

    /* The rig's belts, from the west, the first of the run first. */
    private static List<ConveyorBlockEntity> belts(final ServerLevel level, final MovingStructure craft) {
        final List<ConveyorBlockEntity> out = new ArrayList<>();
        for (final BlockPos at : findAll(level, craft, state -> state.is(TestConveyors.BELT.get()))) {
            if (level.getBlockEntity(at) instanceof ConveyorBlockEntity belt) {
                out.add(belt);
            }
        }
        out.sort((a, b) -> Integer.compare(a.getBlockPos().getX(), b.getBlockPos().getX()));
        if (out.isEmpty()) {
            throw new GameTestAssertException("no belts aboard");
        }
        return out;
    }

    /* What each belt aboard holds, and whether its place ticks, for a test's failure to tell. */
    private static String carrying(final ServerLevel level, final MovingStructure craft) {
        if (craft == null) {
            return "the structure is gone";
        }
        final StringBuilder said = new StringBuilder();
        for (final ConveyorBlockEntity belt : belts(level, craft)) {
            said.append(belt.getBlockPos().toShortString()).append(" holds ").append(belt.held()).append(", ticking ")
                    .append(level.shouldTickBlocksAt(belt.getBlockPos())).append("; ");
        }
        return said.append("the structure ready ").append(craft.ready()).append(" at ").append(craft.pose())
                .toString();
    }

    /* How many diamonds the chest aboard holds. */
    private static int diamonds(final ServerLevel level, final MovingStructure craft) {
        final BlockPos chest = craft == null ? null : find(level, craft, state -> state.is(Blocks.CHEST));
        return chest != null && level.getBlockEntity(chest) instanceof ChestBlockEntity box
                ? box.countItem(Items.DIAMOND) : 0;
    }

    private static RadioBlockEntity radio(final GameTestHelper helper, final BlockPos pos) {
        return (RadioBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
    }

    /* What the floodlight aboard is and lights, for a test's failure to tell. */
    private static String lamp(final ServerLevel level, final MovingStructure craft) {
        final BlockPos at = find(level, craft, state -> state.is(TestLights.FLOODLIGHT.get()));
        if (at == null || !(level.getBlockEntity(at) instanceof LampBlockEntity lamp)) {
            return "no lamp aboard";
        }
        return "the lamp " + (lamp.lit() ? "on" : "off") + " facing " + lamp.facing() + " at " + at + ", aboard "
                + MovingStructures.at(level, at) + ", lighting " + Beams.lamp(level, at, lamp.facing(), lamp.kind(),
                15).keySet();
    }

    /* Whether the floodlight's light stands at {@code at}, held by its beam. */
    private static boolean lit(final ServerLevel level, final BlockPos at) {
        return level.getBlockState(at).getBlock() instanceof LightBlock && BeamLights.holds(level, at);
    }

    /* The drag on a structure, in newtons, as its physics last summed it. */
    private static double dragOn(final MovingStructure structure) {
        final StructurePhysics physics = structure.physics();
        return physics == null ? 0 : physics.forces().of(ForceSum.ForceKind.DRAG).length();
    }

    /* The value of the reading named {@code name}. */
    private static double reading(final List<Reading> readings, final TextKey name) {
        for (final Reading each : readings) {
            if (each.name().equals(name)) {
                return each.value();
            }
        }
        throw new GameTestAssertException("no reading " + name + " in " + readings);
    }

    /* Makes {@code places} of {@code block} into a structure. */
    private static MovingStructure made(final GameTestHelper helper, final List<BlockPos> places, final Block block) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (final BlockPos place : places) {
            helper.setBlock(place, block);
            minX = Math.min(minX, place.getX());
            minY = Math.min(minY, place.getY());
            minZ = Math.min(minZ, place.getZ());
            maxX = Math.max(maxX, place.getX());
            maxY = Math.max(maxY, place.getY());
            maxZ = Math.max(maxZ, place.getZ());
        }
        return assembled(helper, new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ));
    }

    /* Makes every block of the arena's box from {@code from} to {@code to} into a structure. */
    private static MovingStructure assembled(final GameTestHelper helper, final BlockPos from, final BlockPos to) {
        warm(helper.getLevel());
        final StructureResult made = MovingStructures.assemble(helper.getLevel(), helper.absolutePos(from),
                helper.absolutePos(to));
        helper.assertTrue(made.ok() && made.structure() != null, "a structure is made: "
                + (made.refusal() == null ? "done" : made.refusal().english()));
        return made.structure();
    }

    /* Whether the structure moves by its physics and has fallen asleep, at rest. */
    private static boolean asleep(final MovingStructure structure) {
        final StructurePhysics physics = structure.physics();
        return physics != null && physics.asleep();
    }

    /* The world's height of the arena's {@code y}. */
    private static double y(final GameTestHelper helper, final int y) {
        return helper.absolutePos(new BlockPos(0, y, 0)).getY();
    }

    /* The first block of the structure's yard that is so, or null. */
    private static BlockPos find(final ServerLevel level, final MovingStructure structure,
                                 final Predicate<BlockState> wanted) {
        final List<BlockPos> all = findAll(level, structure, wanted);
        return all.isEmpty() ? null : all.get(0);
    }

    /* Every block of the structure's yard that is so. */
    private static List<BlockPos> findAll(final ServerLevel level, final MovingStructure structure,
                                          final Predicate<BlockState> wanted) {
        final List<BlockPos> found = new ArrayList<>();
        final Box box = structure.bounds();
        for (final BlockPos at : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(),
                box.maxZ())) {
            if (wanted.test(level.getBlockState(at))) {
                found.add(at.immutable());
            }
        }
        return found;
    }

    /* Loads the chunks round the yard's next plot, so a structure made comes into it at once. */
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
}
