/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.robot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;

/**
 * Every kind of robot task, the Core's own and those mods add: a mod declares its kinds from its constructor, and a
 * robot saves each task under its kind's id, so a world keeps its robots' work across restarts.
 */
public final class RobotTasks {

    private static final Map<ResourceLocation, RobotTaskType<?>> BY_ID = new ConcurrentHashMap<>();

    /** How any task is saved: its kind's id under {@code type}, then the kind's own fields. */
    public static final Codec<IRobotTask> CODEC = ResourceLocation.CODEC.comapFlatMap(RobotTasks::find,
                    RobotTaskType::id)
            .dispatch("type", IRobotTask::type, RobotTaskType::codec);

    private RobotTasks() {
    }

    /**
     * Declares a kind of task.
     *
     * @throws IllegalStateException when a kind of that id is declared already
     */
    public static <T extends IRobotTask> RobotTaskType<T> declare(final RobotTaskType<T> type) {
        if (BY_ID.putIfAbsent(type.id(), type) != null) {
            throw new IllegalStateException("the robot task " + type.id() + " is declared twice");
        }
        return type;
    }

    private static DataResult<RobotTaskType<?>> find(final ResourceLocation id) {
        final RobotTaskType<?> type = BY_ID.get(id);
        return type == null ? DataResult.error(() -> "no robot task is declared as " + id) : DataResult.success(type);
    }
}
