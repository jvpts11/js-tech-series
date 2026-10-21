/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.robot;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Robots commanded from outside, as a computer's program commands them: finding a robot by its id or its owner, giving
 * it a program of tasks written as data, and reading how it stands. How each task ends is told on the Core's events
 * ({@link RobotTaskEvent}).
 *
 * <pre>{@code
 * Robots.read(JsonOps.INSTANCE, program).result().ifPresent(tasks -> Robots.program(robot, tasks));
 * }</pre>
 */
public final class Robots {

    /* The robots in a loaded part of a world now, on the server. */
    private static final Set<RobotEntity> LOADED = Collections.newSetFromMap(new WeakHashMap<>());

    private Robots() {
    }

    /** Gives {@code robot} a new program: every task it had forgotten, these in their place. */
    public static void program(final RobotEntity robot, final List<IRobotTask> tasks) {
        robot.clearTasks();
        tasks.forEach(robot::queue);
    }

    /**
     * Reads a program written as data, a list of tasks each saved by the codec of its kind: {@code [{"type":
     * "jscore:move_to", "target": [0, 64, 0]}, ...]}. Blocks, items and creatures in it are named by id or tag, so
     * the ops given should know the game's registries when it names any.
     */
    public static <T> DataResult<List<IRobotTask>> read(final DynamicOps<T> ops, final T program) {
        return RobotTasks.CODEC.listOf().parse(ops, program);
    }

    /** The loaded robot of the id {@code id}, or none. */
    public static Optional<RobotEntity> find(final UUID id) {
        synchronized (LOADED) {
            for (final RobotEntity robot : LOADED) {
                if (robot.getUUID().equals(id) && !robot.isRemoved()) {
                    return Optional.of(robot);
                }
            }
        }
        return Optional.empty();
    }

    /** Every loaded robot {@code owner} owns. */
    public static List<RobotEntity> ownedBy(final UUID owner) {
        final List<RobotEntity> out = new ArrayList<>();
        synchronized (LOADED) {
            for (final RobotEntity robot : LOADED) {
                if (!robot.isRemoved() && owner.equals(robot.owner())) {
                    out.add(robot);
                }
            }
        }
        return out;
    }

    /** How {@code robot} stands now. */
    public static Status status(final RobotEntity robot) {
        final List<ResourceLocation> tasks = new ArrayList<>();
        for (final IRobotTask task : robot.tasks()) {
            tasks.add(task.type().id());
        }
        return new Status(robot.getUUID(), robot.level().dimension(), robot.position(), robot.charge(),
                robot.batteryCapacity(), tasks, robot.inventory().getContainerSize());
    }

    /* The robot is in a loaded part of a world on the server now, or no longer. */
    static void loaded(final RobotEntity robot, final boolean now) {
        synchronized (LOADED) {
            if (now) {
                LOADED.add(robot);
            } else {
                LOADED.remove(robot);
            }
        }
    }

    /**
     * How a robot stands.
     *
     * @param id        its id
     * @param dimension the dimension it is in
     * @param position  where it is
     * @param charge    the charge in its battery, in joules
     * @param capacity  what its battery holds at most; 0 for a robot that runs on nothing
     * @param tasks     the kinds of the tasks it has, the one at hand first
     * @param slots     how many slots its inventory has
     */
    public record Status(UUID id, ResourceKey<Level> dimension, Vec3 position, int charge, int capacity,
                         List<ResourceLocation> tasks, int slots) {

        public Status {
            tasks = List.copyOf(tasks);
        }
    }
}
