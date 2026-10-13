/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.robot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.JsCore;
import dev.jstech.core.vehicle.DriverInput;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * The tasks every robot knows: go to a place, break a block there, pick up what lies around, wait, and drive the
 * vehicle it sits in.
 */
public final class CoreRobotTasks {

    /** How close, in blocks, a robot has to be to a block to work on it. */
    public static final double REACH = 3.0;
    /** The widest a robot gathers in, in blocks. */
    public static final int MOST_RADIUS = 16;

    public static final RobotTaskType<MoveTo> MOVE_TO = RobotTasks.declare(new RobotTaskType<>(id("move_to"),
            BlockPos.CODEC.fieldOf("target").xmap(MoveTo::new, MoveTo::target)));
    public static final RobotTaskType<BreakBlock> BREAK_BLOCK = RobotTasks.declare(new RobotTaskType<>(
            id("break_block"), BlockPos.CODEC.fieldOf("target").xmap(BreakBlock::new, BreakBlock::target)));
    public static final RobotTaskType<Collect> COLLECT = RobotTasks.declare(new RobotTaskType<>(id("collect"),
            Codec.intRange(1, MOST_RADIUS).fieldOf("radius").xmap(Collect::new, Collect::radius)));
    public static final RobotTaskType<Wait> WAIT = RobotTasks.declare(new RobotTaskType<>(id("wait"),
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("ticks").xmap(Wait::new, Wait::ticks)));
    public static final RobotTaskType<Drive> DRIVE = RobotTasks.declare(new RobotTaskType<>(id("drive"),
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.FLOAT.fieldOf("forward").forGetter(drive -> drive.input().forward()),
                    Codec.FLOAT.fieldOf("strafe").forGetter(drive -> drive.input().strafe()),
                    Codec.BOOL.fieldOf("up").forGetter(drive -> drive.input().up()),
                    Codec.BOOL.fieldOf("down").forGetter(drive -> drive.input().down()),
                    Codec.intRange(0, Integer.MAX_VALUE).fieldOf("ticks").forGetter(Drive::ticks))
                    .apply(instance, (forward, strafe, up, down, ticks) ->
                            new Drive(new DriverInput(forward, strafe, up, down), ticks)))));

    /* How long a robot spends breaking a block of hardness one: a stone pickaxe's pace. */
    private static final float TICKS_PER_HARDNESS = 15.0F;
    /* How long it may take to reach a place before the task is given up, in ticks. */
    private static final int GIVE_UP_AFTER = 1200;

    private CoreRobotTasks() {
    }

    /** Declares the Core's tasks, from the Core's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path);
    }

    /* Walks towards the middle of {@code target}; true once within reach of it, false while still on the way. */
    private static boolean reach(final RobotEntity robot, final BlockPos target, final double within) {
        final Vec3 middle = Vec3.atCenterOf(target);
        if (robot.position().distanceTo(middle) <= within) {
            robot.getNavigation().stop();
            return true;
        }
        if (robot.getNavigation().isDone()) {
            robot.getNavigation().moveTo(middle.x, middle.y, middle.z, 1.0);
        }
        return false;
    }

    /** Goes to a block, and stands beside it. */
    public static final class MoveTo implements IRobotTask {

        private final BlockPos target;
        private int ticks;

        public MoveTo(final BlockPos target) {
            this.target = target.immutable();
        }

        public BlockPos target() {
            return target;
        }

        @Override
        public RobotTaskType<MoveTo> type() {
            return MOVE_TO;
        }

        @Override
        public Status tick(final RobotEntity robot) {
            if (reach(robot, target, 1.5)) {
                return Status.DONE;
            }
            return ++ticks > GIVE_UP_AFTER ? Status.FAILED : Status.RUNNING;
        }
    }

    /**
     * Breaks a block, going to it first, in as long as its hardness takes, and as its owner would: a protected place
     * that would not let the robot's owner break the block does not let the robot either.
     */
    public static final class BreakBlock implements IRobotTask {

        private final BlockPos target;
        private int ticks;
        private int dug;

        public BreakBlock(final BlockPos target) {
            this.target = target.immutable();
        }

        public BlockPos target() {
            return target;
        }

        @Override
        public RobotTaskType<BreakBlock> type() {
            return BREAK_BLOCK;
        }

        @Override
        public Status tick(final RobotEntity robot) {
            final ServerLevel level = (ServerLevel) robot.level();
            final BlockState state = level.getBlockState(target);
            if (state.isAir()) {
                return Status.DONE;
            }
            final float hardness = state.getDestroySpeed(level, target);
            if (hardness < 0.0F) {
                return Status.FAILED;
            }
            if (!reach(robot, target, REACH)) {
                return ++ticks > GIVE_UP_AFTER ? Status.FAILED : Status.RUNNING;
            }
            robot.getLookControl().setLookAt(Vec3.atCenterOf(target));
            if (++dug < Math.max(1, (int) Math.ceil(hardness * TICKS_PER_HARDNESS))) {
                return Status.RUNNING;
            }
            final BlockEvent.BreakEvent breaking = new BlockEvent.BreakEvent(level, target, state,
                    robot.actingPlayer());
            if (NeoForge.EVENT_BUS.post(breaking).isCanceled()) {
                return Status.FAILED;
            }
            return level.destroyBlock(target, true, robot) ? Status.DONE : Status.FAILED;
        }
    }

    /** Picks up every item lying within {@code radius} blocks that its inventory has room for. */
    public static final class Collect implements IRobotTask {

        private final int radius;

        public Collect(final int radius) {
            this.radius = radius;
        }

        public int radius() {
            return radius;
        }

        @Override
        public RobotTaskType<Collect> type() {
            return COLLECT;
        }

        @Override
        public Status tick(final RobotEntity robot) {
            final List<ItemEntity> lying = robot.level().getEntitiesOfClass(ItemEntity.class,
                    new AABB(robot.blockPosition()).inflate(radius), item -> item.isAlive() && !item.hasPickUpDelay());
            for (final ItemEntity item : lying) {
                final ItemStack left = robot.inventory().addItem(item.getItem().copy());
                if (left.isEmpty()) {
                    item.discard();
                } else {
                    item.setItem(left);
                }
            }
            return Status.DONE;
        }
    }

    /** Waits so many ticks. */
    public static final class Wait implements IRobotTask {

        private final int ticks;
        private int waited;

        public Wait(final int ticks) {
            this.ticks = ticks;
        }

        public int ticks() {
            return ticks;
        }

        @Override
        public RobotTaskType<Wait> type() {
            return WAIT;
        }

        @Override
        public Status tick(final RobotEntity robot) {
            return ++waited >= ticks ? Status.DONE : Status.RUNNING;
        }
    }

    /** Drives the vehicle the robot sits in the front seat of, so for so many ticks. */
    public static final class Drive implements IRobotTask {

        private final DriverInput input;
        private final int ticks;
        private int driven;

        public Drive(final DriverInput input, final int ticks) {
            this.input = input;
            this.ticks = ticks;
        }

        public DriverInput input() {
            return input;
        }

        public int ticks() {
            return ticks;
        }

        @Override
        public RobotTaskType<Drive> type() {
            return DRIVE;
        }

        @Override
        public Status tick(final RobotEntity robot) {
            if (robot.getVehicle() == null) {
                robot.drive(DriverInput.NONE);
                return Status.FAILED;
            }
            if (driven++ >= ticks) {
                robot.drive(DriverInput.NONE);
                return Status.DONE;
            }
            robot.drive(input);
            return Status.RUNNING;
        }
    }
}
