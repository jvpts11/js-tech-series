/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.jstechseries.persistor.SpacePersistor;
import dev.jstech.core.jstechseries.persistor.SpacePersistorBlockEntity;
import dev.jstech.core.moving.BlockAboard;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureControls;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.core.team.Ownership;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.ShipKitBlocks;
import dev.jstech.space.ship.SmallShipParts;
import dev.jstech.space.shipyard.ShipyardPlans;
import dev.jstech.space.shipyard.ShipyardStructure;
import dev.jstech.space.shipyard.ShipyardTerminalBlockEntity;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.space.TestShipyards;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A small ship let go from its dock flies as a moving structure: its pilot seat is its helm; while its pilot holds the
 * throttle it goes along its nose, and turns and climbs as they hold those controls; with nobody at the helm it slows
 * to a stop and stays where it is; what works on the ground works aboard, a furnace set down in its quarters
 * smelting; and a Space Persistor set down aboard keeps it loaded.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ShipFlightGameTests {

    private static final String ARENA = "shipyard";
    private static final String BATCH = "jsspace_ship_flight";
    /* The pattern's lowest corner, the pit floor's west front corner, a ground of stone under it. */
    private static final BlockPos CORNER = new BlockPos(1, 2, 1);
    /* How long the pilot holds a control, and how long a ship is watched at rest, in ticks. */
    private static final int HELD = 40;
    private static final int PARKED = 100;
    /*
     * How far round the dock the land is kept loaded, in chunks, as a pilot aboard keeps the land round them: the
     * ship flies north out of the arena, which alone is kept loaded for a test.
     */
    private static final int FLOWN_CHUNKS = 4;
    /* How far a ship goes along its nose with its throttle held that long, at least, in blocks. */
    private static final double FLOWN = 10;
    private static final double STILL = 1.0e-6;
    private static final ShipDesign SHIP = new ShipDesign("JS-02", List.of(part("orbiter_cockpit"), part("airlock"),
            part("quarters"), part("workshop"), part("reactor_flush")), List.of(part("twin_bells"),
            part("side_intakes"), part("blended_delta"), part("twin_canted_fins"), part("blisters"), part("wheels")));

    private ShipFlightGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 600)
    public static void flight_theShipFliesAlongItsNoseTurnsAndClimbsAsItsPilotHolds(final GameTestHelper helper) {
        final MovingStructure[] ship = new MovingStructure[1];
        final StructurePose[] from = new StructurePose[1];
        keep(helper, true);
        final GameTestSequence sequence = letGo(helper, ship).thenExecute(() -> {
            from[0] = ship[0].pose();
            final BlockPos seat = aboard(helper, ship[0], plan(helper).where(spot(helper,
                    ShipKitBlocks.PILOT_SEAT.getId())));
            helper.assertTrue(BlockAboard.at(helper.getLevel(), seat).seat().orElseThrow().helm(), "its pilot"
                    + " seat is its helm");
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
                ship[0].pose().y() > from[0].y() + 1, "it climbs as its pilot holds up"))
                .thenExecute(() -> {
                    MovingStructures.takeAway(ship[0]);
                    keep(helper, false);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 600)
    public static void flight_aParkedShipStaysWhereItIsWhatWorksAboardWorksAndAPersistorKeepsIt(
            final GameTestHelper helper) {
        final MovingStructure[] ship = new MovingStructure[1];
        final StructurePose[] parked = new StructurePose[1];
        final BlockPos[] furnace = new BlockPos[1];
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
            final BlockPos keeper = aboard(helper, ship[0], plan(helper).where(spot(helper,
                    ShipKitBlocks.AIRLOCK_CYCLER.getId())).south().east());
            helper.getLevel().setBlockAndUpdate(keeper, SpacePersistor.BLOCK.get().defaultBlockState());
            final SpacePersistorBlockEntity persistor = (SpacePersistorBlockEntity) helper.getLevel()
                    .getBlockEntity(keeper);
            persistor.own(Ownership.of(UUID.randomUUID()));
            persistor.power().generate(SpacePersistorBlockEntity.CAPACITY);
        }).thenIdle(PARKED).thenWaitUntil(() -> {
            helper.assertTrue(ship[0].pose().distanceTo(parked[0]) < STILL, "parked, it stays where it is");
            helper.assertTrue(ship[0].keepsItsPlace(), "a Space Persistor set down in its airlock keeps it loaded");
            final AbstractFurnaceBlockEntity oven = (AbstractFurnaceBlockEntity) helper.getLevel()
                    .getBlockEntity(furnace[0]);
            helper.assertTrue(oven.getItem(2).is(Items.IRON_INGOT), "a furnace set down in its quarters smelts"
                    + " aboard");
        }).thenExecute(() -> MovingStructures.takeAway(ship[0])).thenSucceed();
    }

    /* The dock built, the ship built in it and let go, {@code ship} told it once its blocks came in. */
    private static GameTestSequence letGo(final GameTestHelper helper, final MovingStructure[] ship) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        return helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(level.getBlockEntity(terminal)
                        instanceof ShipyardTerminalBlockEntity dock && dock.formed(), "the dock forms"))
                .thenExecute(() -> {
                    final ShipyardTerminalBlockEntity dock = (ShipyardTerminalBlockEntity) level
                            .getBlockEntity(terminal);
                    dock.design(SHIP);
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

    /* Its pilot holds {@code axis} a while, as their helm tells the structure each tick, then lets go. */
    private static GameTestSequence hold(final GameTestHelper helper, final GameTestSequence sequence,
                                         final MovingStructure[] ship, final StructureControls.ControlAxis axis) {
        for (int tick = 0; tick < HELD; tick++) {
            sequence.thenExecuteAfter(1, () -> {
                final Map<StructureControls.ControlAxis, Float> held = new EnumMap<>(
                        StructureControls.ControlAxis.class);
                held.put(axis, 1F);
                ship[0].controls().pilot(helper.getLevel().getGameTime(), held, new Vec3(0, 0, -1),
                        new Vec3(0, 1, 0));
            });
        }
        return sequence;
    }

    /* Keeps the land round the dock and north of it loaded, or lets it go. */
    private static void keep(final GameTestHelper helper, final boolean kept) {
        final ChunkPos dock = new ChunkPos(helper.absolutePos(CORNER));
        for (int x = dock.x - 1; x <= dock.x + 2; x++) {
            for (int z = dock.z - FLOWN_CHUNKS; z <= dock.z + 2; z++) {
                helper.getLevel().setChunkForced(x, z, kept);
            }
        }
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
