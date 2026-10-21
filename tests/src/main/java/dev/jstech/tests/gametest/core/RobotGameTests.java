/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.JsCore;
import dev.jstech.core.robot.CoreRobotConditions;
import dev.jstech.core.robot.CoreRobotTasks;
import dev.jstech.core.robot.IRobotTask;
import dev.jstech.core.robot.RobotEntity;
import dev.jstech.core.robot.RobotTaskEvent;
import dev.jstech.core.robot.Robots;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestEntities;
import dev.jstech.tests.TestModules;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Core's robots, through the test mod's own: one that flies up to a place above it and one that swims across a
 * pool; a battery robot that stands still empty, is charged at a station and goes on, and one that recharges at the
 * nearest station by itself; digging faster with a tool, which wears; breaking the nearest block of a kind; choosing,
 * repeating and waiting; a program written as data; its logic tasks kept when saved; its inventory by its size; and
 * robots found by their id and their owner.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class RobotGameTests {

    private static final String ARENA = "empty";
    private static final String SKY = "tall";
    private static final String BATCH = "jscore_robots";

    private RobotGameTests() {
    }

    @GameTest(template = SKY, batch = BATCH, timeoutTicks = 400)
    public static void flyer_fliesUpToAPlaceAboveIt(final GameTestHelper helper) {
        final TestEntities.Flyer flyer = helper.spawn(TestEntities.FLYER.get(), new BlockPos(6, 2, 6));
        flyer.energy().receiveEnergy(TestEntities.BATTERY, false);
        final BlockPos above = helper.absolutePos(new BlockPos(6, 12, 6));
        flyer.queue(new CoreRobotTasks.MoveTo(above));
        helper.succeedWhen(() -> {
            helper.assertTrue(flyer.tasks().isEmpty(), "the flyer reaches the place ten blocks up; it is at "
                    + flyer.position());
            helper.assertTrue(flyer.charge() < TestEntities.BATTERY, "spending its battery as it flew");
            flyer.discard();
        });
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void diver_swimsAcrossAPool(final GameTestHelper helper) {
        for (int x = 0; x <= 8; x++) {
            for (int z = 0; z <= 8; z++) {
                final boolean edge = x == 0 || z == 0 || x == 8 || z == 8;
                helper.setBlock(new BlockPos(x, 2, z), edge ? Blocks.GLASS : Blocks.WATER);
                helper.setBlock(new BlockPos(x, 3, z), edge ? Blocks.GLASS : Blocks.WATER);
            }
        }
        final TestEntities.Diver diver = helper.spawn(TestEntities.DIVER.get(), new BlockPos(1, 2, 4));
        diver.queue(new CoreRobotTasks.MoveTo(helper.absolutePos(new BlockPos(7, 2, 4))));
        helper.succeedWhen(() -> {
            helper.assertTrue(diver.tasks().isEmpty(), "the diver swims across the pool; it is at "
                    + diver.position());
            diver.discard();
        });
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void worker_standsStillEmptyAndGoesOnCharged(final GameTestHelper helper) {
        final TestEntities.Worker worker = helper.spawn(TestEntities.WORKER.get(), new BlockPos(2, 2, 2));
        worker.queue(new CoreRobotTasks.MoveTo(helper.absolutePos(new BlockPos(6, 2, 6))));
        final Vec3 start = worker.position();
        helper.startSequence()
                .thenExecuteAfter(30, () -> {
                    helper.assertTrue(worker.position().distanceTo(start) < 0.3 && worker.tasks().size() == 1,
                            "with an empty battery it stands still");
                    helper.setBlock(new BlockPos(3, 2, 2), TestModules.CHARGER.get());
                    final IEnergyStorage station = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK,
                            helper.absolutePos(new BlockPos(3, 2, 2)), null);
                    helper.assertTrue(station != null && station.receiveEnergy(TestModules.CHARGER_ENERGY, false) > 0,
                            "a cable fills the station beside it");
                })
                .thenWaitUntil(() -> helper.assertTrue(worker.tasks().isEmpty(), "charged, it goes on: it is at "
                        + worker.position() + " with " + worker.charge() + " J"))
                .thenExecute(worker::discard)
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 600)
    public static void worker_rechargesAtTheNearestStation(final GameTestHelper helper) {
        helper.setBlock(new BlockPos(6, 2, 6), TestModules.CHARGER.get());
        final IEnergyStorage station = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK,
                helper.absolutePos(new BlockPos(6, 2, 6)), null);
        helper.assertTrue(station != null, "the station takes charge from cables");
        station.receiveEnergy(TestModules.CHARGER_ENERGY, false);
        final TestEntities.Worker worker = helper.spawn(TestEntities.WORKER.get(), new BlockPos(1, 2, 1));
        worker.energy().receiveEnergy(TestEntities.BATTERY / 10, false);
        helper.startSequence()
                // The station tells where it is as it works, every half second.
                .thenExecuteAfter(12, () -> worker.queue(new CoreRobotTasks.If(
                        new CoreRobotConditions.ChargeBelow(0.5), List.of(new CoreRobotTasks.Recharge(16)),
                        List.of())))
                .thenWaitUntil(() -> helper.assertTrue(worker.tasks().isEmpty() && worker.chargeShare() >= 0.99F,
                        "low, it goes to the station and fills up: " + worker.charge() + " J, at "
                                + worker.position()))
                .thenExecute(worker::discard)
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void robot_digsFasterWithAToolThatWears(final GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 2, 3), Blocks.STONE);
        helper.setBlock(new BlockPos(6, 2, 3), Blocks.STONE);
        final TestEntities.Robot handed = helper.spawn(TestEntities.ROBOT.get(), new BlockPos(2, 2, 2));
        final TestEntities.Robot bare = helper.spawn(TestEntities.ROBOT.get(), new BlockPos(6, 2, 2));
        handed.inventory().addItem(new ItemStack(Items.DIAMOND_PICKAXE));
        handed.queue(new CoreRobotTasks.BreakBlock(helper.absolutePos(new BlockPos(2, 2, 3))));
        bare.queue(new CoreRobotTasks.BreakBlock(helper.absolutePos(new BlockPos(6, 2, 3))));
        helper.runAfterDelay(14, () -> {
            helper.assertBlockNotPresent(Blocks.STONE, new BlockPos(2, 2, 3));
            helper.assertBlockPresent(Blocks.STONE, new BlockPos(6, 2, 3));
            final ItemStack pickaxe = handed.getItemInHand(InteractionHand.MAIN_HAND);
            helper.assertTrue(pickaxe.is(Items.DIAMOND_PICKAXE) && pickaxe.getDamageValue() == 1,
                    "it took the pickaxe into its hand, which wore: " + pickaxe);
            helper.assertTrue(!helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(
                    helper.absolutePos(new BlockPos(2, 2, 3))).inflate(1.5), item -> item.getItem()
                    .is(Items.COBBLESTONE)).isEmpty(), "and the stone gave cobblestone");
            handed.discard();
            bare.discard();
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 300)
    public static void robot_breaksTheNearestBlockOfAKind(final GameTestHelper helper) {
        // Blocks the test world has none of under the arena, where a robot would find its stone and dirt.
        helper.setBlock(new BlockPos(3, 2, 2), Blocks.GOLD_BLOCK);
        helper.setBlock(new BlockPos(6, 2, 2), Blocks.EMERALD_BLOCK);
        final TestEntities.Robot robot = helper.spawn(TestEntities.ROBOT.get(), new BlockPos(2, 2, 2));
        final BlockPredicate emerald = BlockPredicate.Builder.block().of(Blocks.EMERALD_BLOCK).build();
        helper.assertTrue(CoreRobotConditions.nearest(robot, emerald, 8).map(helper.absolutePos(
                new BlockPos(6, 2, 2))::equals).orElse(false), "the robot finds the emerald block");
        robot.queue(new CoreRobotTasks.BreakNearest(emerald, 8));
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getBlockState(new BlockPos(6, 2, 2)).isAir(), "it breaks it; the robot is at "
                    + robot.position() + " with " + robot.tasks());
            helper.assertBlockPresent(Blocks.GOLD_BLOCK, new BlockPos(3, 2, 2));
            robot.discard();
        });
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 300)
    public static void robot_choosesRepeatsAndWaits(final GameTestHelper helper) {
        final TestEntities.Robot robot = helper.spawn(TestEntities.ROBOT.get(), new BlockPos(4, 2, 4));
        robot.inventory().addItem(new ItemStack(Items.DIRT, 2));
        final BlockPos yes = helper.absolutePos(new BlockPos(1, 2, 1));
        final BlockPos no = helper.absolutePos(new BlockPos(7, 2, 7));
        final List<IRobotTask.Status> repeated = new ArrayList<>();
        final Consumer<RobotTaskEvent> heard = event -> {
            if (event.robot() == robot && event.task() instanceof CoreRobotTasks.Repeat) {
                repeated.add(event.status());
            }
        };
        JsCore.events().subscribe(RobotTaskEvent.class, heard);
        robot.queue(new CoreRobotTasks.Repeat(List.of(new CoreRobotTasks.Wait(2)), 3));
        robot.queue(new CoreRobotTasks.If(new CoreRobotConditions.Holding(ItemPredicate.Builder.item().of(Items.DIRT)
                .build(), 2), List.of(new CoreRobotTasks.MoveTo(yes)), List.of(new CoreRobotTasks.MoveTo(no))));
        robot.queue(new CoreRobotTasks.WaitUntil(new CoreRobotConditions.Not(new CoreRobotConditions.Holding(
                ItemPredicate.Builder.item().of(Items.DIRT).build(), 3)), 0));
        helper.succeedWhen(() -> {
            helper.assertTrue(robot.tasks().isEmpty(), "it works through its tasks; left " + robot.tasks());
            helper.assertTrue(robot.position().distanceTo(Vec3.atCenterOf(yes)) < 2.0,
                    "holding two dirt it went the first way: it is at " + robot.position());
            helper.assertTrue(repeated.equals(List.of(IRobotTask.Status.DONE)),
                    "and the repeat was told done, once: " + repeated);
            JsCore.events().unsubscribe(RobotTaskEvent.class, heard);
            robot.discard();
        });
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 300)
    public static void program_writtenAsData_isReadAndRun(final GameTestHelper helper) {
        final TestEntities.Robot robot = helper.spawn(TestEntities.ROBOT.get(), new BlockPos(2, 2, 2));
        final BlockPos target = helper.absolutePos(new BlockPos(6, 2, 6));
        helper.setBlock(new BlockPos(6, 2, 2), Blocks.STONE);
        final String program = "[{\"type\": \"jscore:wait\", \"ticks\": 2},"
                + " {\"type\": \"jscore:if\", \"condition\": {\"type\": \"jscore:block_near\","
                + " \"block\": {\"blocks\": \"minecraft:stone\"}, \"radius\": 8},"
                + " \"then\": [{\"type\": \"jscore:move_to\", \"target\": [" + target.getX() + ", " + target.getY()
                + ", " + target.getZ() + "]}]}]";
        final RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE,
                helper.getLevel().registryAccess());
        final List<IRobotTask> tasks = Robots.read(ops, JsonParser.parseString(program)).getOrThrow();
        helper.assertTrue(tasks.size() == 2 && tasks.get(0) instanceof CoreRobotTasks.Wait
                && tasks.get(1) instanceof CoreRobotTasks.If, "the program is read: " + tasks);
        Robots.program(robot, tasks);
        helper.succeedWhen(() -> {
            helper.assertTrue(robot.tasks().isEmpty() && robot.position().distanceTo(Vec3.atCenterOf(target)) < 2.0,
                    "and run: stone being near, it went to the place; it is at " + robot.position());
            robot.discard();
        });
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void robot_keepsItsLogicTasksWhenSaved(final GameTestHelper helper) {
        final TestEntities.Robot robot = helper.spawn(TestEntities.ROBOT.get(), new BlockPos(2, 2, 2));
        robot.queue(new CoreRobotTasks.Repeat(List.of(
                new CoreRobotTasks.BreakNearest(BlockPredicate.Builder.block().of(Blocks.STONE).build(), 6),
                new CoreRobotTasks.WaitUntil(new CoreRobotConditions.Not(new CoreRobotConditions.InventoryFull()),
                        40)), 0));
        robot.queue(new CoreRobotTasks.AtTime(6000));
        final CompoundTag saved = robot.saveWithoutId(new CompoundTag());
        final TestEntities.Robot back = TestEntities.ROBOT.get().create(helper.getLevel());
        back.load(saved);
        final List<IRobotTask> tasks = back.tasks();
        helper.assertTrue(tasks.size() == 2 && tasks.get(0) instanceof CoreRobotTasks.Repeat repeat
                        && repeat.times() == 0 && repeat.body().size() == 2
                        && repeat.body().get(1) instanceof CoreRobotTasks.WaitUntil until
                        && until.condition() instanceof CoreRobotConditions.Not
                        && tasks.get(1) instanceof CoreRobotTasks.AtTime at && at.time() == 6000,
                "its tasks, their conditions and their blocks come back as they were: " + tasks);
        robot.discard();
        back.discard();
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void inventory_isAsBigAsTheRobot(final GameTestHelper helper) {
        final RobotEntity walker = helper.spawn(TestEntities.ROBOT.get(), new BlockPos(2, 2, 2));
        final RobotEntity flyer = helper.spawn(TestEntities.FLYER.get(), new BlockPos(4, 2, 4));
        final RobotEntity diver = helper.spawn(TestEntities.DIVER.get(), new BlockPos(6, 2, 6));
        helper.assertTrue(walker.inventory().getContainerSize() == 9 && flyer.inventory().getContainerSize() == 6
                        && diver.inventory().getContainerSize() == 7,
                "a robot carries as its size lets it: " + walker.inventory().getContainerSize() + ", "
                        + flyer.inventory().getContainerSize() + ", " + diver.inventory().getContainerSize());
        walker.discard();
        flyer.discard();
        diver.discard();
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void robots_areFoundByTheirIdAndTheirOwner(final GameTestHelper helper) {
        final TestEntities.Robot robot = helper.spawn(TestEntities.ROBOT.get(), new BlockPos(2, 2, 2));
        final UUID owner = UUID.randomUUID();
        helper.assertTrue(robot.giveTo(owner), "the robot is given to its owner");
        helper.assertTrue(Robots.find(robot.getUUID()).orElse(null) == robot, "it is found by its id");
        helper.assertTrue(Robots.ownedBy(owner).contains(robot), "and among its owner's robots");
        final Robots.Status status = Robots.status(robot);
        helper.assertTrue(status.id().equals(robot.getUUID()) && status.slots() == 9 && status.capacity() == 0,
                "its status says how it stands: " + status);
        robot.discard();
        helper.succeed();
    }
}
