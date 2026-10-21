/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.air.AirMix;
import dev.jstech.core.dimension.Climate;
import dev.jstech.core.dimension.DimensionEffects;
import dev.jstech.core.dimension.DimensionHazardEvent;
import dev.jstech.core.dimension.DimensionRulesData;
import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.core.gametest.GameTestPlayers;
import dev.jstech.core.physics.Laws;
import dev.jstech.core.physics.MatterLaws;
import dev.jstech.core.physics.Physics;
import dev.jstech.core.physics.PhysicsArea;
import dev.jstech.core.physics.Pull;
import dev.jstech.core.region.Box;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestKitMachine;
import dev.jstech.tests.TestKitMachine.KitMachineBlockEntity;
import dev.jstech.tests.TestWorldGen;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The physics kit and a dimension's air by height: a land's climate and its hazards change with height in bands, and
 * whether its air is breathed follows the oxygen's partial pressure; gravity pulls items as an area's law says
 * (weightless, towards a mass) and as their dimension does (half as hard in a land of half a g), and as an entity's
 * own laws say wherever it goes; time runs slower or faster for items and for the series' machines; space wraps round
 * and leads elsewhere, to another dimension too; explosions go out slowly and liquids rise where matter says so. They
 * change how entities move, so they share a batch of their own.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class PhysicsGameTests {

    private static final String ARENA = "empty";
    private static final String BATCH = "jscore_physics";
    private static final double STILL = 0.05;
    /* What an item keeps of its speed a tick in the air. */
    private static final double AIR_DRAG = 0.98;

    private PhysicsGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void climate_changesWithHeightInBands(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final ResourceKey<Level> key = key("bands_venus_land");
        final ServerLevel venus = RuntimeDimensions.getOrCreate(server, key, TestWorldGen.VENUS_LAND.id());
        final ServerPlayer player = GameTestPlayers.join(venus, UUID.randomUUID(), "climber");
        try {
            final Climate ground = DimensionRulesData.of(venus).at(0);
            final Climate middle = DimensionRulesData.of(venus).at(100);
            final Climate high = DimensionRulesData.of(venus).at(250);
            helper.assertTrue(ground.pressure() == 9_300 && ground.temperature() == 464 && !ground.breathable(),
                    "at the ground, the hell of Venus; got " + ground);
            helper.assertTrue(Math.abs(middle.pressure() - 4_700.65) < 0.01
                    && Math.abs(middle.temperature() - 242) < 0.01, "halfway up, halfway between; got " + middle);
            helper.assertTrue(high.pressure() == 101.3 && high.temperature() == 20 && high.breathable(),
                    "high in the clouds, the overworld's air; got " + high);
            player.setPos(8.5, 5, 8.5);
            final Set<DimensionHazardEvent.Hazard> below = DimensionEffects.weigh(player);
            player.setPos(8.5, 230, 8.5);
            final Set<DimensionHazardEvent.Hazard> above = DimensionEffects.weigh(player);
            helper.assertTrue(below.contains(DimensionHazardEvent.Hazard.HEAT)
                    && below.contains(DimensionHazardEvent.Hazard.AIR), "below, the heat burns and the air chokes");
            helper.assertTrue(above.isEmpty(), "above, nothing hurts; got " + above);
        } finally {
            GameTestPlayers.leave(player);
            RuntimeDimensions.remove(server, key);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void air_isBreathedByItsOxygensPartialPressure(final GameTestHelper helper) {
        helper.assertTrue(AirMix.EARTH.breathable(101.3), "the overworld's air at sea level");
        helper.assertTrue(!AirMix.EARTH.breathable(50), "but not at half the pressure");
        helper.assertTrue(new AirMix(Map.of(AirMix.OXYGEN, 1.0)).breathable(20),
                "pure oxygen at a fifth of an atmosphere, as in a suit");
        helper.assertTrue(Math.abs(AirMix.EARTH.partial(AirMix.OXYGEN, 100) - 21) < 1e-9,
                "the partial pressure is the share of the pressure");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void gravity_letsWhatIsInAWeightlessAreaFloat(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final String id = id(helper, "weightless");
        Physics.add(level, area(helper, id, new BlockPos(0, 1, 0), new BlockPos(3, 5, 8),
                gravity(new Pull.Scaled(0))));
        final ItemEntity floating = helper.spawnItem(Items.STONE, 1.5F, 4, 1.5F);
        final ItemEntity falling = helper.spawnItem(Items.DIRT, 6.5F, 4, 1.5F);
        final double from = floating.getY();
        final double fallFrom = falling.getY();
        helper.startSequence()
                .thenExecuteAfter(10, () -> {
                    Physics.remove(level, id);
                    helper.assertTrue(Math.abs(floating.getY() - from) < STILL, "the stone floats; it moved "
                            + (floating.getY() - from));
                    helper.assertTrue(fallFrom - falling.getY() > 0.3, "the dirt beside it falls");
                })
                .thenSucceed();
    }

    /* Two lands are made and their chunks generated first, which a loaded server takes long over: hence the time. */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void gravity_pullsWhatIsNotAliveAsHardAsItsDimension(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final ResourceKey<Level> halfKey = key("gravity_half_land");
        final ResourceKey<Level> fullKey = key("gravity_full_land");
        final ServerLevel half = RuntimeDimensions.getOrCreate(server, halfKey, TestWorldGen.HALF_G_LAND.id());
        final ServerLevel full = RuntimeDimensions.getOrCreate(server, fullKey, TestWorldGen.LOCKED_LAND.id());
        half.setChunkForced(0, 0, true);
        full.setChunkForced(0, 0, true);
        final ItemEntity light = drop(half, new Vec3(8.5, 60, 8.5));
        final ItemEntity heavy = drop(full, new Vec3(8.5, 60, 8.5));
        // A new land starts ticking its entities when its chunk is ready, so each item is weighed by its own ticks.
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(light.getAge() >= 10 && heavy.getAge() >= 10,
                        "both items have fallen ten ticks"))
                .thenExecute(() -> {
                    final double halfPull = pullOf(light);
                    final double fullPull = pullOf(heavy);
                    RuntimeDimensions.remove(server, halfKey);
                    RuntimeDimensions.remove(server, fullKey);
                    helper.assertTrue(fullPull > 0 && Math.abs(halfPull / fullPull - 0.5) < 0.02,
                            "in half a g an item is pulled half as hard: " + halfPull + " against " + fullPull);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void gravity_pullsTowardsAMassAndCurvesSpaceRoundIt(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final String id = id(helper, "mass");
        final Vec3 centre = helper.absoluteVec(new Vec3(4.5, 3.5, 4.5));
        Physics.mass(level, id, centre, 1.0, 2.0);
        final Laws near = Physics.lawsAt(level, centre.add(1.5, 0, 0));
        helper.assertTrue(near.gravity().orElse(null) instanceof Pull.Centre && near.timeRate() < 1
                && near.light().map(Laws.Light::lens).orElse(0.0) > 0,
                "near the mass things fall towards it, time runs slower and the view bends: " + near);
        final ItemEntity drawn = helper.spawnItem(Items.STONE, 7.5F, 3.5F, 4.5F);
        final double from = drawn.getX();
        helper.startSequence()
                .thenExecuteAfter(30, () -> {
                    Physics.remove(level, id);
                    helper.assertTrue(drawn.getX() < from - 0.3, "the stone is drawn towards the mass; it moved "
                            + (drawn.getX() - from));
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void ownLaws_goWithTheEntityWhereverItIs(final GameTestHelper helper) {
        final ItemEntity carried = helper.spawnItem(Items.FEATHER, 2.5F, 4, 2.5F);
        Physics.own(carried, gravity(new Pull.Scaled(0)));
        final double from = carried.getY();
        helper.startSequence()
                .thenExecuteAfter(10, () -> helper.assertTrue(Math.abs(carried.getY() - from) < STILL,
                        "a feather carrying its own weightlessness floats anywhere"))
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void time_runsSlowerAndFasterForWhatIsThere(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final String slowId = id(helper, "slow");
        final String fastId = id(helper, "fast");
        final String stoppedId = id(helper, "stopped");
        Physics.add(level, area(helper, slowId, new BlockPos(0, 1, 0), new BlockPos(1, 5, 1), time(0.5)));
        Physics.add(level, area(helper, fastId, new BlockPos(3, 1, 0), new BlockPos(4, 5, 1), time(2.0)));
        Physics.add(level, area(helper, stoppedId, new BlockPos(0, 1, 3), new BlockPos(1, 5, 4), time(0.0)));
        final ItemEntity slow = helper.spawnItem(Items.STONE, 1.0F, 2, 1.0F);
        final ItemEntity fast = helper.spawnItem(Items.DIRT, 4.0F, 2, 1.0F);
        final ItemEntity stopped = helper.spawnItem(Items.FEATHER, 1.0F, 4, 4.0F);
        final double stoppedFrom = stopped.getY();
        helper.startSequence()
                .thenExecuteAfter(40, () -> {
                    Physics.remove(level, slowId);
                    Physics.remove(level, fastId);
                    Physics.remove(level, stoppedId);
                    helper.assertTrue(Math.abs(slow.getAge() - 20) <= 2, "at half speed, half the age; "
                            + slow.getAge());
                    helper.assertTrue(Math.abs(fast.getAge() - 80) <= 4, "at twice the speed, twice; " + fast.getAge());
                    helper.assertTrue(stopped.getAge() <= 1 && Math.abs(stopped.getY() - stoppedFrom) < STILL,
                            "where time stands still, nothing moves or ages");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 160)
    public static void time_runsFasterForTheSeriesMachines(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final String id = id(helper, "fast_machine");
        Physics.add(level, area(helper, id, new BlockPos(0, 1, 0), new BlockPos(2, 4, 2), time(2.0)));
        final KitMachineBlockEntity quick = machine(helper, new BlockPos(1, 2, 1));
        final KitMachineBlockEntity usual = machine(helper, new BlockPos(5, 2, 1));
        helper.startSequence()
                .thenExecuteAfter(TestKitMachine.TICKS * 3 + 5, () -> {
                    Physics.remove(level, id);
                    final int quickMade = quick.getInventory().getStackInSlot(TestKitMachine.OUT).getCount();
                    final int usualMade = usual.getInventory().getStackInSlot(TestKitMachine.OUT).getCount();
                    helper.assertTrue(quickMade >= 2 * usualMade - 1 && quickMade > usualMade,
                            "where time runs twice as fast the machine makes twice as much: " + quickMade + " against "
                                    + usualMade);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void space_wrapsRound(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final String id = id(helper, "wrap");
        final PhysicsArea wrap = area(helper, id, new BlockPos(1, 2, 1), new BlockPos(5, 4, 3), new Laws(
                Optional.of(new Pull.Scaled(0)), Optional.empty(), Optional.empty(), Optional.of(new Laws.Space(
                Laws.Crossing.WRAP, Optional.empty(), Optional.empty(), false)), Optional.empty()));
        Physics.add(level, wrap);
        final ItemEntity flying = helper.spawnItem(Items.STONE, 3.5F, 3.5F, 2.5F);
        flying.setDeltaMovement(0.4, 0, 0);
        helper.startSequence()
                .thenExecuteAfter(40, () -> {
                    Physics.remove(level, id);
                    helper.assertTrue(wrap.contains(flying.position()) && flying.getDeltaMovement().x > 0,
                            "flying out one side it came back in the other, still flying on; at " + flying.position());
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void space_leadsAShortcutToItsTarget(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final String id = id(helper, "shortcut");
        final BlockPos far = helper.absolutePos(new BlockPos(7, 1, 4));
        Physics.add(level, area(helper, id, new BlockPos(2, 1, 0), new BlockPos(2, 5, 4), new Laws(Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.of(new Laws.Space(Laws.Crossing.ENTER, Optional.empty(),
                Optional.of(Box.between(far.getX(), far.getY(), far.getZ(), far.getX(), far.getY() + 4,
                        far.getZ() + 4)), true)), Optional.empty())));
        final ItemEntity going = helper.spawnItem(Items.STONE, 0.5F, 3, 2.5F);
        going.setDeltaMovement(0.5, 0.1, 0);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(going.getX() >= far.getX() && going.getZ() >= far.getZ(),
                        "the stone came out at " + going.position()))
                .thenExecute(() -> {
                    Physics.remove(level, id);
                    going.discard();
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 120)
    public static void space_leadsThroughADoorBiggerInsideToAnotherDimension(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final MinecraftServer server = level.getServer();
        final ResourceKey<Level> pocketKey = key("space_pocket");
        final ServerLevel pocket = RuntimeDimensions.getOrCreate(server, pocketKey, TestWorldGen.LOCKED_LAND.id());
        pocket.setChunkForced(0, 0, true);
        final String id = id(helper, "door");
        final Box inside = Box.between(0, 10, 0, 15, 20, 15);
        Physics.add(level, area(helper, id, new BlockPos(2, 1, 1), new BlockPos(3, 4, 3), new Laws(Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.of(new Laws.Space(Laws.Crossing.ENTER,
                Optional.of(pocketKey), Optional.of(inside), false)), Optional.empty())));
        final ItemEntity going = helper.spawnItem(Items.STONE, 0.5F, 3, 2.0F);
        going.setDeltaMovement(0.4, 0.05, 0);
        final AABB room = new AABB(inside.minX(), inside.minY() - 12, inside.minZ(), inside.maxX() + 1,
                inside.maxY() + 1, inside.maxZ() + 1);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(going.isRemoved()
                        && !pocket.getEntitiesOfClass(ItemEntity.class, room).isEmpty(),
                        "the stone went through the small door into the large room of the pocket; it is at "
                                + going.position() + (going.isRemoved() ? ", gone" : "") + ", the pocket holds "
                                + pocket.getEntitiesOfClass(ItemEntity.class, room).size()))
                .thenExecute(() -> {
                    Physics.remove(level, id);
                    RuntimeDimensions.remove(server, pocketKey);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 120)
    public static void matter_letsAnExplosionGoOutSlowly(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final String id = id(helper, "slow_blast");
        Physics.add(level, area(helper, id, new BlockPos(0, 1, 0), new BlockPos(8, 5, 8), new Laws(Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(new Laws.Matter(false, 2.0)))));
        final BlockPos near = new BlockPos(6, 3, 4);
        helper.setBlock(near, Blocks.DIRT);
        final Vec3 centre = helper.absoluteVec(new Vec3(4.5, 3.5, 4.5));
        level.explode(null, centre.x, centre.y, centre.z, 2.0F, Level.ExplosionInteraction.BLOCK);
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    helper.assertTrue(MatterLaws.pending(level) > 0, "the explosion has blocks still to reach");
                    helper.assertBlockPresent(Blocks.DIRT, near);
                })
                .thenWaitUntil(() -> helper.assertTrue(MatterLaws.pending(level) == 0, "it reached them all"))
                .thenExecute(() -> {
                    Physics.remove(level, id);
                    helper.assertBlockNotPresent(Blocks.DIRT, near);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 120)
    public static void matter_letsLiquidsRise(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final String id = id(helper, "rising");
        Physics.add(level, area(helper, id, new BlockPos(2, 2, 2), new BlockPos(2, 6, 2), new Laws(Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(new Laws.Matter(true, 0)))));
        helper.setBlock(new BlockPos(2, 2, 2), Blocks.WATER);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(level.getFluidState(helper.absolutePos(new BlockPos(2, 6, 2)))
                        .isSource(), "the water rose to the top of the area"))
                .thenExecute(() -> {
                    Physics.remove(level, id);
                    helper.setBlock(new BlockPos(2, 6, 2), Blocks.AIR);
                })
                .thenSucceed();
    }

    private static Laws gravity(final Pull pull) {
        return new Laws(Optional.of(pull), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    private static Laws time(final double rate) {
        return new Laws(Optional.empty(), Optional.of(rate), Optional.empty(), Optional.empty(), Optional.empty());
    }

    /* An area of the arena from {@code low} to {@code high}, in the arena's own places. */
    private static PhysicsArea area(final GameTestHelper helper, final String id, final BlockPos low,
                                    final BlockPos high, final Laws laws) {
        final BlockPos a = helper.absolutePos(low);
        final BlockPos b = helper.absolutePos(high);
        return new PhysicsArea(id, Box.between(a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ()), laws, 0);
    }

    /* An id of its own for an area of this test's arena. */
    private static String id(final GameTestHelper helper, final String name) {
        return name + "@" + helper.absolutePos(BlockPos.ZERO).toShortString();
    }

    /* How hard an item falling freely since it was dropped is pulled a tick: its speed over the drag of its ticks. */
    private static double pullOf(final ItemEntity item) {
        double dragged = 0;
        for (int tick = 1; tick <= item.getAge(); tick++) {
            dragged += Math.pow(AIR_DRAG, tick);
        }
        return -item.getDeltaMovement().y / dragged;
    }

    private static ItemEntity drop(final ServerLevel level, final Vec3 at) {
        final ItemEntity item = new ItemEntity(level, at.x, at.y, at.z, new ItemStack(Items.STONE));
        item.setDeltaMovement(Vec3.ZERO);
        level.addFreshEntity(item);
        return item;
    }

    private static KitMachineBlockEntity machine(final GameTestHelper helper, final BlockPos at) {
        helper.setBlock(at, TestKitMachine.BLOCK.get());
        final KitMachineBlockEntity machine = (KitMachineBlockEntity) helper.getLevel().getBlockEntity(
                helper.absolutePos(at));
        machine.getInventory().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 64));
        machine.power().portStore().setStored(TestKitMachine.CAPACITY);
        return machine;
    }

    private static ResourceKey<Level> key(final String path) {
        return ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(JsTests.MODID, path));
    }
}
