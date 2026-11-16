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
import dev.jstech.core.moving.StructureControls;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.space.ship.AirReserveEntity;
import dev.jstech.space.ship.PulseCourse;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.ShipFlight;
import dev.jstech.space.ship.ShipFlightModel;
import dev.jstech.space.ship.ShipPulse;
import dev.jstech.space.ship.ShipReadings;
import dev.jstech.space.ship.SmallShipParts;
import dev.jstech.space.shipyard.ShipyardTerminalBlockEntity;
import dev.jstech.space.universe.SolarBody;
import dev.jstech.space.universe.StarSystems;
import dev.jstech.space.universe.SystemLayout;
import dev.jstech.space.universe.SystemSpaces;
import dev.jstech.testkit.gametest.GameTestPlayers;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.space.TestShipyards;
import java.util.List;
import java.util.Map;
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
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A starship's pulse jump in the Solar System's space: its key held, its drive spools and it jumps, standing still
 * while its jump goes on along its nose; its pilot holding back, it drops out and is set where the jump took it; coming
 * over the Moon's low orbit it drops out there by itself; and over a world's low orbit, or with no deuterium left, its
 * drive does not spool.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ShipPulseGameTests {

    private static final String ARENA = "shipyard";
    /* Each its own batch: the ships go to the same places of space, the Moon's and the Overworld's. */
    private static final String BATCH = "jsspace_ship_pulse";
    private static final String MOON_BATCH = "jsspace_ship_pulse_moon";
    private static final String ORBIT_BATCH = "jsspace_ship_pulse_orbit";
    private static final String FUEL_BATCH = "jsspace_ship_pulse_fuel";
    /* Where the dock's lowest corner stands in the arena. */
    private static final BlockPos CORNER = new BlockPos(1, 2, 1);
    private static final TicketType<BlockPos> SPACE = TicketType.create("jstests_ship_pulse", Vec3i::compareTo);
    private static final int TICKING = 2;
    private static final int SPACE_CHUNKS = 3;
    /* How far east of the ship the player keeping the space ticking stands, in blocks. */
    private static final double WATCHER_ASIDE = 40;
    /* How far north of the dock, in blocks, the ship is sent into space, far from every world. */
    private static final int FAR_NORTH = 448;
    /* How fast the ship flies as it is set going in space, in metres a second. */
    private static final double CRUISING = 40;
    /* How many ticks it jumps before its pilot holds back; how near, in blocks, it comes out where its jump took it. */
    private static final int JUMPED = 20;
    private static final double THERE = 1;
    /* How far short of the Moon's low orbit the ship starts its jump towards it, in blocks. */
    private static final double SHORT_OF_THE_MOON = 2_000;
    /* How long its key is held where its drive should not spool, in ticks. */
    private static final int HELD = ShipPulse.SPOOL_TICKS + 10;
    /* How far from the Overworld's middle, north or south, a ship over its low orbit stands, in degrees of latitude. */
    private static final double LATITUDE = 30;
    private static final double SPEED_A_TICK = PulseCourse.SPEED * ShipFlightModel.TICK;
    private static final ShipDesign SHIP = new ShipDesign("JS-04", List.of(part("orbiter_cockpit"),
            part("airlock"), part("quarters"), part("workshop"), part("reactor_flush")), List.of(part("twin_bells"),
            part("side_intakes"), part("blended_delta"), part("twin_canted_fins"), part("blisters"), part("legs")));

    private ShipPulseGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 3000)
    public static void pulse_spooledItJumpsStandingStillAndHoldingBackItIsSetWhereTheJumpTookIt(
            final GameTestHelper helper) {
        final BlockPos far = helper.absolutePos(CORNER).offset(0, 0, -FAR_NORTH);
        final MovingStructure[] ship = new MovingStructure[1];
        final ServerPlayer[] watcher = new ServerPlayer[1];
        final StructurePose[] began = new StructurePose[1];
        final ShipPulse.Jump[] jumped = new ShipPulse.Jump[1];
        final GameTestSequence sequence = inSpace(helper, ship, new Vec3(far.getX() + 0.5, SystemLayout.ORBIT_Y,
                far.getZ() + 0.5), watcher, "pulse_watcher");
        spool(helper, sequence, ship).thenExecute(() -> {
            helper.assertTrue(flight(ship[0]).pulse().isPresent(), "its drive spooled a second and a half, it jumps");
            began[0] = ship[0].pose();
        });
        for (int tick = 0; tick < JUMPED; tick++) {
            sequence.thenExecuteAfter(1, () -> pilot(helper, ship[0], Map.of()));
        }
        sequence.thenExecute(() -> {
            jumped[0] = flight(ship[0]).pulse().orElseThrow();
            helper.assertTrue(ship[0].pose().distanceTo(began[0]) < THERE, "the ship stands still: "
                    + ship[0].pose().distanceTo(began[0]));
            final double gone = Math.hypot(jumped[0].dx(), jumped[0].dz());
            helper.assertTrue(gone > (JUMPED - 2) * SPEED_A_TICK && jumped[0].dz() < 0, "its jump goes on north, as"
                    + " its nose points, at the jump's speed: " + jumped[0]);
            helper.assertTrue(console(ship[0]).flight().pulse().on() && console(ship[0]).flight().speed()
                    == PulseCourse.SPEED, "its console shows it jumping: " + console(ship[0]).flight());
        }).thenWaitUntil(() -> {
                    pilot(helper, ship[0], Map.of(StructureControls.ControlAxis.BACK, 1F));
                    helper.assertTrue(flight(ship[0]).pulse().isEmpty(), "holding back, it drops out");
                })
                // Its console shows it from its first tick of flight after.
                .thenExecuteAfter(1, () -> {
                    final StructurePose now = ship[0].pose();
                    final ShipPulse.Jump last = jumped[0];
                    helper.assertTrue(Math.abs(now.z() - began[0].z() - last.dz()) < 2 * SPEED_A_TICK
                            && Math.abs(now.x() - began[0].x() - last.dx()) < THERE, "it is set where its jump took"
                            + " it: " + now + " from " + began[0] + " by " + last);
                    helper.assertTrue(console(ship[0]).flight().pulse().dropped().equals(ShipPulse.Drop.PILOT),
                            "its console shows it dropped out: " + console(ship[0]).flight().pulse());
                    helper.assertTrue(flight(ship[0]).state().speed() >= CRUISING, "flying on at its own speed, as"
                            + " it was before it jumped: " + flight(ship[0]).state());
                    done(helper, ship[0], far, watcher[0]);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = MOON_BATCH, timeoutTicks = 3000)
    public static void pulse_comingOverTheMoonsLowOrbitItDropsOutThereByItself(final GameTestHelper helper) {
        final double[] moon = SystemLayout.centre(SolarBody.MOON);
        final double reach = SystemLayout.radius(SolarBody.MOON) * (1 + SystemLayout.LOW_ORBIT);
        // South of the Moon, its nose north towards it.
        final Vec3 start = new Vec3(moon[0], SystemLayout.ORBIT_Y, moon[2] + reach + SHORT_OF_THE_MOON);
        final MovingStructure[] ship = new MovingStructure[1];
        final ServerPlayer[] watcher = new ServerPlayer[1];
        final GameTestSequence sequence = inSpace(helper, ship, start, watcher, "moon_pulse_watcher");
        spool(helper, sequence, ship).thenWaitUntil(() -> {
            pilot(helper, ship[0], Map.of());
            helper.assertTrue(flight(ship[0]).pulse().isEmpty() && console(ship[0]).flight().pulse().dropped()
                    .equals("moon"), "coming over the Moon's low orbit, it drops out by itself");
        }).thenExecute(() -> {
            final StructurePose now = ship[0].pose();
            helper.assertTrue(Math.abs(now.z() - (moon[2] + reach)) < 2 * SPEED_A_TICK && Math.abs(now.x()
                    - moon[0]) < THERE, "set at the edge of the Moon's low orbit, ready to go down: " + now);
            done(helper, ship[0], BlockPos.containing(start), watcher[0]);
        }).thenSucceed();
    }

    @GameTest(template = ARENA, batch = ORBIT_BATCH, timeoutTicks = 3000)
    public static void pulse_overAWorldsLowOrbitHeadingInItsDriveDoesNotSpoolAndItsHudSaysWhy(
            final GameTestHelper helper) {
        // South of the Overworld's middle, over its low orbit, its nose north, back over the world.
        final double[] spot = SystemLayout.orbitSpot(SolarBody.OVERWORLD, 0, -LATITUDE);
        final Vec3 over = new Vec3(spot[0] + 0.5, SystemLayout.ORBIT_Y, spot[1] + 0.5);
        final MovingStructure[] ship = new MovingStructure[1];
        final ServerPlayer[] watcher = new ServerPlayer[1];
        held(helper, inSpace(helper, ship, over, watcher, "orbit_pulse_watcher"), ship).thenExecute(() -> {
            final ShipReadings.Pulse shown = console(ship[0]).flight().pulse();
            helper.assertTrue(flight(ship[0]).pulse().isEmpty() && shown.spool() == 0, "heading back over the "
                    + "Overworld its drive does not spool: " + shown);
            helper.assertTrue(shown.dropped().equals("overworld"), "and its HUD says it is near it: " + shown);
            done(helper, ship[0], BlockPos.containing(over), watcher[0]);
        }).thenSucceed();
    }

    @GameTest(template = ARENA, batch = ORBIT_BATCH, timeoutTicks = 3000)
    public static void pulse_overAWorldsLowOrbitHeadingOutItJumpsAtOnce(final GameTestHelper helper) {
        // North of the Overworld's middle, over its low orbit, its nose north, away from the world.
        final double[] spot = SystemLayout.orbitSpot(SolarBody.OVERWORLD, 0, LATITUDE);
        final Vec3 over = new Vec3(spot[0] + 0.5, SystemLayout.ORBIT_Y, spot[1] + 0.5);
        final MovingStructure[] ship = new MovingStructure[1];
        final ServerPlayer[] watcher = new ServerPlayer[1];
        spool(helper, inSpace(helper, ship, over, watcher, "orbit_out_pulse_watcher"), ship).thenExecute(() -> {
            helper.assertTrue(flight(ship[0]).pulse().isPresent(), "come up from the Overworld and heading away, it "
                    + "jumps at once: " + console(ship[0]).flight().pulse());
            done(helper, ship[0], BlockPos.containing(over), watcher[0]);
        }).thenSucceed();
    }

    @GameTest(template = ARENA, batch = FUEL_BATCH, timeoutTicks = 3000)
    public static void pulse_withNoDeuteriumLeftItsDriveDoesNotSpool(final GameTestHelper helper) {
        final BlockPos far = helper.absolutePos(CORNER).offset(0, 0, -FAR_NORTH);
        final MovingStructure[] ship = new MovingStructure[1];
        final ServerPlayer[] watcher = new ServerPlayer[1];
        final GameTestSequence sequence = inSpace(helper, ship, new Vec3(far.getX() + 0.5, SystemLayout.ORBIT_Y,
                far.getZ() + 0.5), watcher, "fuel_pulse_watcher").thenExecute(() -> console(ship[0]).burn(1));
        held(helper, sequence, ship).thenExecute(() -> {
            helper.assertTrue(flight(ship[0]).pulse().isEmpty(), "with no deuterium left it does not jump");
            done(helper, ship[0], far, watcher[0]);
        }).thenSucceed();
    }

    /*
     * A ship built and let go, sent into the Solar System's space at {@code at}, a player beside it keeping the space
     * ticking, and set flying there.
     */
    private static GameTestSequence inSpace(final GameTestHelper helper, final MovingStructure[] ship, final Vec3 at,
                                            final ServerPlayer[] watcher, final String watcherName) {
        final ServerLevel space = SystemSpaces.spaceOf(helper.getLevel().getServer(), StarSystems.SOLAR_SYSTEM);
        final BlockPos kept = BlockPos.containing(at);
        return letGo(helper, ship).thenExecute(() -> {
            keepSpace(helper, space, kept, true);
            watcher[0] = GameTestPlayers.connected(helper, watcherName);
            watcher[0].setGameMode(GameType.CREATIVE);
            watcher[0].teleportTo(space, at.x + WATCHER_ASIDE, at.y, at.z, 0, 0);
            helper.assertTrue(MovingStructures.travel(ship[0], space, at).ok(), "the ship goes to space");
        }).thenWaitUntil(() -> {
            final MovingStructure there = MovingStructures.byId(space, ship[0].id());
            helper.assertTrue(there != null && there.ready(), "and arrives there");
            ship[0] = there;
        }).thenExecute(() -> MovingStructures.move(ship[0], flight(ship[0]).with(new ShipFlightModel.State(CRUISING,
                0, 1, 0, 0))));
    }

    /* Its pilot at the helm holds its pulse jump's key till its drive has spooled. */
    private static GameTestSequence spool(final GameTestHelper helper, final GameTestSequence sequence,
                                          final MovingStructure[] ship) {
        for (int tick = 0; tick <= ShipPulse.SPOOL_TICKS + 1; tick++) {
            sequence.thenExecuteAfter(1, () -> {
                pilot(helper, ship[0], Map.of());
                ShipPulse.hold(ship[0], ship[0].level().getGameTime());
            });
        }
        return sequence;
    }

    /* Its pilot at the helm holds its pulse jump's key {@link #HELD} ticks. */
    private static GameTestSequence held(final GameTestHelper helper, final GameTestSequence sequence,
                                         final MovingStructure[] ship) {
        for (int tick = 0; tick < HELD; tick++) {
            sequence.thenExecuteAfter(1, () -> {
                pilot(helper, ship[0], Map.of());
                ShipPulse.hold(ship[0], ship[0].level().getGameTime());
            });
        }
        return sequence;
    }

    /* The ship taken away, the player who watched it gone, and the space round {@code kept} let go. */
    private static void done(final GameTestHelper helper, final MovingStructure ship, final BlockPos kept,
                             final ServerPlayer watcher) {
        final ServerLevel space = (ServerLevel) ship.level();
        MovingStructures.takeAway(ship);
        GameTestPlayers.leave(watcher);
        keepSpace(helper, space, kept, false);
    }

    /* A ship built in the dock and let go, {@code ship} told it once it came in. */
    private static GameTestSequence letGo(final GameTestHelper helper, final MovingStructure[] ship) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        return helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(level.getBlockEntity(terminal)
                        instanceof ShipyardTerminalBlockEntity dock && dock.formed(), "the dock forms"))
                .thenExecute(() -> {
                    final ShipyardTerminalBlockEntity dock = (ShipyardTerminalBlockEntity) level.getBlockEntity(
                            terminal);
                    dock.design(SHIP);
                    helper.assertTrue(dock.confirm(), "the ship is sent");
                    final PlannedBuild plan = dock.build().orElseThrow();
                    for (final PlanSpot spot : plan.plan().spots()) {
                        level.setBlockAndUpdate(plan.where(spot), BuiltInRegistries.BLOCK.get(spot.block())
                                .defaultBlockState());
                    }
                })
                .thenExecuteAfter(2, () -> ship[0] = ((ShipyardTerminalBlockEntity) level.getBlockEntity(terminal))
                        .release(null).orElseThrow())
                .thenWaitUntil(() -> helper.assertTrue(ship[0].ready(), "its blocks come into the structure"));
    }

    /* Keeps space round {@code at} loaded and ticking, or lets it go. */
    private static void keepSpace(final GameTestHelper helper, final ServerLevel space, final BlockPos at,
                                  final boolean kept) {
        final ChunkPos middle = new ChunkPos(at);
        final BlockPos own = helper.absolutePos(CORNER);
        for (int x = middle.x - SPACE_CHUNKS; x <= middle.x + SPACE_CHUNKS; x++) {
            for (int z = middle.z - SPACE_CHUNKS; z <= middle.z + SPACE_CHUNKS; z++) {
                if (kept) {
                    space.getChunkSource().addRegionTicket(SPACE, new ChunkPos(x, z), TICKING, own);
                } else {
                    space.getChunkSource().removeRegionTicket(SPACE, new ChunkPos(x, z), TICKING, own);
                }
            }
        }
    }

    /* The helm tells {@code ship} its pilot holds {@code held} now. */
    private static void pilot(final GameTestHelper helper, final MovingStructure ship,
                              final Map<StructureControls.ControlAxis, Float> held) {
        ship.controls().pilot(ship.level().getGameTime(), held, new Vec3(0, 0, -1), new Vec3(0, 1, 0));
    }

    private static ShipFlight flight(final MovingStructure ship) {
        return (ShipFlight) ship.motion();
    }

    /* {@code ship}'s flight console, which shows its flight. */
    private static AirReserveEntity console(final MovingStructure ship) {
        return (AirReserveEntity) ship.level().getBlockEntity(flight(ship).consoleIn(ship).orElseThrow());
    }

    private static ResourceLocation part(final String path) {
        return SmallShipParts.id("small_" + path);
    }
}
