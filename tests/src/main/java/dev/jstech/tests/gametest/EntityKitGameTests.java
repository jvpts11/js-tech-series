/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.core.projectile.Projectiles;
import dev.jstech.core.robot.CoreRobotTasks;
import dev.jstech.core.robot.IRobotTask;
import dev.jstech.core.vehicle.DriverInput;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestEntities;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Core's robots, vehicles and projectiles, through the test mod's own: a robot that walks to a block, breaks it
 * and picks up what fell, and keeps its tasks when saved; a rover a robot drives ahead on its battery, charged
 * through the game's energy capability; and a bolt that harms what it hits and stops against a wall.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class EntityKitGameTests {

    private static final String ARENA = "empty";

    private EntityKitGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void robot_breaksABlockAndPicksUpWhatFell(final GameTestHelper helper) {
        final BlockPos stone = new BlockPos(3, 2, 3);
        helper.setBlock(stone, Blocks.COBBLESTONE);
        final TestEntities.Robot robot = helper.spawn(TestEntities.ROBOT.get(), new BlockPos(1, 2, 1));
        robot.queue(new CoreRobotTasks.BreakBlock(helper.absolutePos(stone)));
        // A dropped block lies still a moment before anything may pick it up.
        robot.queue(new CoreRobotTasks.Wait(15));
        robot.queue(new CoreRobotTasks.Collect(4));
        helper.succeedWhen(() -> {
            helper.assertBlockNotPresent(Blocks.COBBLESTONE, stone);
            helper.assertTrue(robot.tasks().isEmpty(), "every task is done; left " + robot.tasks().size());
            helper.assertTrue(robot.inventory().countItem(Items.COBBLESTONE) == 1,
                    "the robot carries the cobblestone it broke");
            robot.discard();
        });
    }

    @GameTest(template = ARENA)
    public static void robot_keepsItsTasksWhenSaved(final GameTestHelper helper) {
        final TestEntities.Robot robot = helper.spawn(TestEntities.ROBOT.get(), new BlockPos(1, 2, 1));
        robot.queue(new CoreRobotTasks.MoveTo(helper.absolutePos(new BlockPos(4, 2, 4))));
        robot.queue(new CoreRobotTasks.Drive(new DriverInput(1.0F, -0.5F, true, false), 40));
        robot.inventory().addItem(Items.REDSTONE.getDefaultInstance().copyWithCount(7));
        final CompoundTag saved = robot.saveWithoutId(new CompoundTag());
        final TestEntities.Robot back = TestEntities.ROBOT.get().create(helper.getLevel());
        back.load(saved);
        final List<IRobotTask> tasks = back.tasks();
        helper.assertTrue(tasks.size() == 2 && tasks.get(0) instanceof CoreRobotTasks.MoveTo move
                        && move.target().equals(helper.absolutePos(new BlockPos(4, 2, 4)))
                        && tasks.get(1) instanceof CoreRobotTasks.Drive drive && drive.ticks() == 40
                        && drive.input().strafe() == -0.5F, "the tasks come back as they were; got " + tasks);
        helper.assertTrue(back.inventory().countItem(Items.REDSTONE) == 7, "and so does what it carries");
        robot.discard();
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void rover_goesAheadWhenItsRobotDrivesIt(final GameTestHelper helper) {
        final TestEntities.Rover rover = helper.spawn(TestEntities.ROVER.get(), new BlockPos(2, 2, 2));
        rover.setYRot(0.0F);
        final IEnergyStorage battery = rover.getCapability(Capabilities.EnergyStorage.ENTITY, null);
        helper.assertTrue(battery != null && battery.receiveEnergy(5_000, false) == 5_000,
                "the rover's battery is charged through the energy capability");
        final TestEntities.Robot robot = helper.spawn(TestEntities.ROBOT.get(), new BlockPos(2, 2, 2));
        helper.assertTrue(robot.startRiding(rover, true), "the robot takes the driver's seat");
        // A short drive, so the rover stays within the test's own ground.
        robot.queue(new CoreRobotTasks.Drive(new DriverInput(1.0F, 0.0F, false, false), 15));
        final Vec3 start = rover.position();
        helper.runAfterDelay(25, () -> {
            final double moved = rover.position().subtract(start).horizontalDistance();
            helper.assertTrue(moved > 2.0, "the rover went ahead; it moved " + moved + " blocks");
            helper.assertTrue(battery.getEnergyStored() < 5_000, "spending its battery as it went");
            robot.discard();
            rover.discard();
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void bolt_harmsWhatItHitsAndStopsAgainstAWall(final GameTestHelper helper) {
        final Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 2, 1));
        final float health = pig.getHealth();
        helper.setBlock(new BlockPos(3, 2, 4), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 3, 4), Blocks.STONE);
        // Fired from inside the arena: the barriers round a test would stop a bolt fired from outside.
        final TestEntities.Bolt atPig = Projectiles.fire(helper.getLevel(), null, TestEntities.BOLT.get(),
                Vec3.atCenterOf(helper.absolutePos(new BlockPos(3, 2, 0))), new Vec3(0.0, 0.0, 1.0), 1.0F);
        final TestEntities.Bolt atWall = Projectiles.fire(helper.getLevel(), null, TestEntities.BOLT.get(),
                Vec3.atCenterOf(helper.absolutePos(new BlockPos(3, 3, 2))), new Vec3(0.0, 0.0, 1.0), 1.0F);
        helper.succeedWhen(() -> {
            helper.assertTrue(pig.getHealth() <= health - TestEntities.BOLT_SPEC.damage() + 0.01F,
                    "the pig took the bolt's harm; its health is " + pig.getHealth());
            helper.assertTrue(atWall.isRemoved(), "the bolt that met the wall stopped there");
            pig.discard();
            atPig.discard();
        });
    }
}
