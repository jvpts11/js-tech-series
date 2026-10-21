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
import dev.jstech.core.protection.ProtectedAction;
import dev.jstech.core.protection.Protection;
import dev.jstech.core.vehicle.DriverInput;
import java.util.List;
import java.util.Optional;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.jetbrains.annotations.Nullable;

/**
 * The tasks every robot knows: go to a place, break a block there, pick up what lies around, wait, and drive the
 * vehicle it sits in; go to or break the nearest block of a kind; recharge at the nearest station; and the tasks with
 * logic, which repeat other tasks, choose between them by a condition ({@link IRobotCondition}), wait for one, or wait
 * for a time of day. A task that repeats or chooses runs fresh copies of its tasks, one at a time, and a step that
 * fails ends it, so a loop never spins on what cannot be done.
 */
public final class CoreRobotTasks {

    /** How close, in blocks, a robot has to be to a block to work on it. */
    public static final double REACH = 3.0;
    /** The widest a robot gathers in, in blocks. */
    public static final int MOST_RADIUS = 16;
    /** How far a robot looks for a charging station, in blocks, at the most. */
    public static final int MOST_STATION_RADIUS = 64;
    private static final int DAY = 24_000;

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
    public static final RobotTaskType<GoToNearest> GO_TO_NEAREST = RobotTasks.declare(new RobotTaskType<>(
            id("go_to_nearest"), RecordCodecBuilder.mapCodec(instance -> instance.group(
                    BlockPredicate.CODEC.fieldOf("block").forGetter(GoToNearest::block),
                    Codec.intRange(1, MOST_RADIUS).fieldOf("radius").forGetter(GoToNearest::radius))
                    .apply(instance, GoToNearest::new))));
    public static final RobotTaskType<BreakNearest> BREAK_NEAREST = RobotTasks.declare(new RobotTaskType<>(
            id("break_nearest"), RecordCodecBuilder.mapCodec(instance -> instance.group(
                    BlockPredicate.CODEC.fieldOf("block").forGetter(BreakNearest::block),
                    Codec.intRange(1, MOST_RADIUS).fieldOf("radius").forGetter(BreakNearest::radius))
                    .apply(instance, BreakNearest::new))));
    public static final RobotTaskType<Recharge> RECHARGE = RobotTasks.declare(new RobotTaskType<>(id("recharge"),
            Codec.intRange(1, MOST_STATION_RADIUS).fieldOf("radius").xmap(Recharge::new, Recharge::radius)));
    public static final RobotTaskType<Repeat> REPEAT = RobotTasks.declare(new RobotTaskType<>(id("repeat"),
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    RobotTasks.CODEC.listOf().fieldOf("tasks").forGetter(Repeat::body),
                    Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("times", 0).forGetter(Repeat::times))
                    .apply(instance, Repeat::new))));
    public static final RobotTaskType<If> IF = RobotTasks.declare(new RobotTaskType<>(id("if"),
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    RobotConditions.CODEC.fieldOf("condition").forGetter(If::condition),
                    RobotTasks.CODEC.listOf().optionalFieldOf("then", List.of()).forGetter(If::then),
                    RobotTasks.CODEC.listOf().optionalFieldOf("else", List.of()).forGetter(If::otherwise))
                    .apply(instance, If::new))));
    public static final RobotTaskType<WaitUntil> WAIT_UNTIL = RobotTasks.declare(new RobotTaskType<>(
            id("wait_until"), RecordCodecBuilder.mapCodec(instance -> instance.group(
                    RobotConditions.CODEC.fieldOf("condition").forGetter(WaitUntil::condition),
                    Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("timeout", 0).forGetter(WaitUntil::timeout))
                    .apply(instance, WaitUntil::new))));
    public static final RobotTaskType<AtTime> AT_TIME = RobotTasks.declare(new RobotTaskType<>(id("at_time"),
            Codec.intRange(0, DAY - 1).fieldOf("time").xmap(AtTime::new, AtTime::time)));

    /* How long it may take to reach a place before the task is given up, in ticks. */
    private static final int GIVE_UP_AFTER = 1200;
    /* What a robot spends digging, in joules a tick: 800 W. */
    private static final int DIGGING_COST = 40;
    /* A robot at a station that gains nothing for this long, in ticks, gives up recharging. */
    private static final int NO_CHARGE_FOR = 200;
    /* A robot recharges until its battery is this full. */
    private static final float FULL = 0.99F;

    private CoreRobotTasks() {
    }

    /** Declares the Core's tasks, from the Core's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path);
    }

    /*
     * Goes towards {@code target}, beside it when it is a block a robot cannot stand in; true once within reach of its
     * middle, false while still on the way.
     */
    private static boolean reach(final RobotEntity robot, final BlockPos target, final double within) {
        final Vec3 middle = Vec3.atCenterOf(target);
        if (robot.position().distanceTo(middle) <= within) {
            robot.getNavigation().stop();
            return true;
        }
        if (robot.getNavigation().isDone()) {
            final Path path = robot.getNavigation().createPath(target, 1);
            if (path != null) {
                robot.getNavigation().moveTo(path, 1.0);
            } else {
                robot.getNavigation().moveTo(middle.x, middle.y, middle.z, 1.0);
            }
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
     * Breaks a block, going to it first, as its owner would: a protected place that would not let the robot's owner
     * break the block does not let the robot either. It digs with the best tool it carries for the block, at the pace a
     * player would with it, the tool wearing as it does and its enchantments working on what drops; without one it
     * digs with its own arm and still gets what the block drops.
     */
    public static final class BreakBlock implements IRobotTask {

        private final BlockPos target;
        private int ticks;
        private int dug;
        private int needed;

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
            if (needed == 0) {
                needed = RobotTools.digTicks(hardness, RobotTools.takeBest(robot, state), state);
            }
            if (!robot.spend(DIGGING_COST)) {
                return Status.RUNNING;
            }
            if (++dug < needed) {
                return Status.RUNNING;
            }
            // The claims and protections installed weigh its owner; then the game's own event, as for a player.
            if (!Protection.may(robot.actor(), ProtectedAction.BREAK, level, target)) {
                return Status.FAILED;
            }
            final BlockEvent.BreakEvent breaking = new BlockEvent.BreakEvent(level, target, state,
                    robot.actingPlayer());
            if (NeoForge.EVENT_BUS.post(breaking).isCanceled()) {
                return Status.FAILED;
            }
            final ItemStack tool = robot.getMainHandItem();
            final BlockEntity entity = level.getBlockEntity(target);
            if (!level.destroyBlock(target, false, robot)) {
                return Status.FAILED;
            }
            Block.dropResources(state, level, target, entity, robot, tool);
            if (!tool.isEmpty()) {
                tool.hurtAndBreak(1, robot, EquipmentSlot.MAINHAND);
            }
            return Status.DONE;
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

    /** Goes to the nearest block of a kind within {@code radius}, as it stands when the task begins. */
    public static final class GoToNearest implements IRobotTask {

        private final BlockPredicate block;
        private final int radius;
        private @Nullable MoveTo going;

        public GoToNearest(final BlockPredicate block, final int radius) {
            this.block = block;
            this.radius = radius;
        }

        public BlockPredicate block() {
            return block;
        }

        public int radius() {
            return radius;
        }

        @Override
        public RobotTaskType<GoToNearest> type() {
            return GO_TO_NEAREST;
        }

        @Override
        public Status tick(final RobotEntity robot) {
            if (going == null) {
                final Optional<BlockPos> found = CoreRobotConditions.nearest(robot, block, radius);
                if (found.isEmpty()) {
                    return Status.FAILED;
                }
                going = new MoveTo(found.get());
            }
            return going.tick(robot);
        }
    }

    /** Breaks the nearest block of a kind within {@code radius}, as it stands when the task begins. */
    public static final class BreakNearest implements IRobotTask {

        private final BlockPredicate block;
        private final int radius;
        private @Nullable BreakBlock breaking;

        public BreakNearest(final BlockPredicate block, final int radius) {
            this.block = block;
            this.radius = radius;
        }

        public BlockPredicate block() {
            return block;
        }

        public int radius() {
            return radius;
        }

        @Override
        public RobotTaskType<BreakNearest> type() {
            return BREAK_NEAREST;
        }

        @Override
        public Status tick(final RobotEntity robot) {
            if (breaking == null) {
                final Optional<BlockPos> found = CoreRobotConditions.nearest(robot, block, radius);
                if (found.isEmpty()) {
                    return Status.FAILED;
                }
                breaking = new BreakBlock(found.get());
            }
            return breaking.tick(robot);
        }
    }

    /**
     * Goes to the nearest charging station within {@code radius} that is working, and waits beside it until its
     * battery is full; it gives up when there is none, or when the station gives nothing for ten seconds. A robot that
     * runs on nothing is done at once.
     */
    public static final class Recharge implements IRobotTask {

        private final int radius;
        private @Nullable BlockPos station;
        private int ticks;
        private int idle;
        private int last = -1;

        public Recharge(final int radius) {
            this.radius = radius;
        }

        public int radius() {
            return radius;
        }

        @Override
        public RobotTaskType<Recharge> type() {
            return RECHARGE;
        }

        @Override
        public Status tick(final RobotEntity robot) {
            if (robot.batteryCapacity() <= 0 || robot.chargeShare() >= FULL) {
                return Status.DONE;
            }
            if (station == null) {
                station = RobotChargers.nearest(robot.level(), robot.blockPosition(), radius).orElse(null);
                if (station == null) {
                    return Status.FAILED;
                }
            }
            if (!reach(robot, station, RobotCharger.REACH)) {
                return ++ticks > GIVE_UP_AFTER ? Status.FAILED : Status.RUNNING;
            }
            idle = robot.charge() > last ? 0 : idle + 1;
            last = robot.charge();
            return idle > NO_CHARGE_FOR ? Status.FAILED : Status.RUNNING;
        }
    }

    /**
     * Works through {@code body} in order, {@code times} times, or for ever with 0; a step that fails ends it, failed.
     */
    public static final class Repeat implements IRobotTask {

        private final List<IRobotTask> body;
        private final int times;
        private final Steps steps;
        private int rounds;

        public Repeat(final List<IRobotTask> body, final int times) {
            this.body = List.copyOf(body);
            this.times = times;
            this.steps = new Steps(this.body);
        }

        public List<IRobotTask> body() {
            return body;
        }

        public int times() {
            return times;
        }

        @Override
        public RobotTaskType<Repeat> type() {
            return REPEAT;
        }

        @Override
        public Status tick(final RobotEntity robot) {
            if (body.isEmpty()) {
                return Status.DONE;
            }
            final Status step = steps.tick(robot);
            if (step != Status.DONE) {
                return step;
            }
            steps.restart();
            return times > 0 && ++rounds >= times ? Status.DONE : Status.RUNNING;
        }
    }

    /**
     * Works through {@code then} when {@code condition} holds as it begins, and through {@code otherwise} when it does
     * not; a step that fails ends it, failed.
     */
    public static final class If implements IRobotTask {

        private final IRobotCondition condition;
        private final List<IRobotTask> then;
        private final List<IRobotTask> otherwise;
        private @Nullable Steps chosen;

        public If(final IRobotCondition condition, final List<IRobotTask> then, final List<IRobotTask> otherwise) {
            this.condition = condition;
            this.then = List.copyOf(then);
            this.otherwise = List.copyOf(otherwise);
        }

        public IRobotCondition condition() {
            return condition;
        }

        public List<IRobotTask> then() {
            return then;
        }

        public List<IRobotTask> otherwise() {
            return otherwise;
        }

        @Override
        public RobotTaskType<If> type() {
            return IF;
        }

        @Override
        public Status tick(final RobotEntity robot) {
            if (chosen == null) {
                chosen = new Steps(condition.test(robot) ? then : otherwise);
            }
            return chosen.tick(robot);
        }
    }

    /** Waits until {@code condition} holds; given up after {@code timeout} ticks, or never with 0. */
    public static final class WaitUntil implements IRobotTask {

        private final IRobotCondition condition;
        private final int timeout;
        private int waited;

        public WaitUntil(final IRobotCondition condition, final int timeout) {
            this.condition = condition;
            this.timeout = timeout;
        }

        public IRobotCondition condition() {
            return condition;
        }

        public int timeout() {
            return timeout;
        }

        @Override
        public RobotTaskType<WaitUntil> type() {
            return WAIT_UNTIL;
        }

        @Override
        public Status tick(final RobotEntity robot) {
            if (condition.test(robot)) {
                return Status.DONE;
            }
            return timeout > 0 && ++waited >= timeout ? Status.FAILED : Status.RUNNING;
        }
    }

    /**
     * Waits for the next time the day's clock comes to {@code time}, in the game's ticks of a day, 0 at sunrise and
     * 6000 at noon: a schedule, repeated, runs its tasks at the same hour each day. Now, when the clock is there.
     */
    public static final class AtTime implements IRobotTask {

        private final int time;
        private long until = -1;

        public AtTime(final int time) {
            this.time = time;
        }

        public int time() {
            return time;
        }

        @Override
        public RobotTaskType<AtTime> type() {
            return AT_TIME;
        }

        @Override
        public Status tick(final RobotEntity robot) {
            final long now = robot.level().getDayTime();
            if (until < 0) {
                until = now + Math.floorMod(time - Math.floorMod(now, (long) DAY), (long) DAY);
            }
            return now >= until ? Status.DONE : Status.RUNNING;
        }
    }

    /* A list of tasks worked through in order, each from a fresh copy, one step at a time. */
    private static final class Steps {

        private final List<IRobotTask> tasks;
        private int at;
        private @Nullable IRobotTask current;

        private Steps(final List<IRobotTask> tasks) {
            this.tasks = tasks;
        }

        /* One tick: running while a step runs, done past the last, failed with the first step that fails. */
        private IRobotTask.Status tick(final RobotEntity robot) {
            if (at >= tasks.size()) {
                return IRobotTask.Status.DONE;
            }
            if (current == null) {
                current = RobotTasks.fresh(tasks.get(at), robot.registryAccess());
            }
            final IRobotTask.Status step = current.tick(robot);
            if (step == IRobotTask.Status.FAILED) {
                return IRobotTask.Status.FAILED;
            }
            if (step == IRobotTask.Status.DONE) {
                current = null;
                at++;
            }
            return at >= tasks.size() ? IRobotTask.Status.DONE : IRobotTask.Status.RUNNING;
        }

        private void restart() {
            at = 0;
            current = null;
        }
    }
}
