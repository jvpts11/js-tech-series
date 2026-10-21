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
 * Every kind of robot condition, the Core's own and those mods add: a mod declares its kinds from its constructor, and
 * a task saves each condition under its kind's id.
 */
public final class RobotConditions {

    private static final Map<ResourceLocation, RobotConditionType<?>> BY_ID = new ConcurrentHashMap<>();

    /** How any condition is saved: its kind's id under {@code type}, then the kind's own fields. */
    public static final Codec<IRobotCondition> CODEC = Codec.lazyInitialized(() -> ResourceLocation.CODEC
            .comapFlatMap(RobotConditions::find, RobotConditionType::id)
            .dispatch("type", IRobotCondition::type, RobotConditionType::codec));

    private RobotConditions() {
    }

    /**
     * Declares a kind of condition.
     *
     * @throws IllegalStateException when a kind of that id is declared already
     */
    public static <C extends IRobotCondition> RobotConditionType<C> declare(final RobotConditionType<C> type) {
        if (BY_ID.putIfAbsent(type.id(), type) != null) {
            throw new IllegalStateException("the robot condition " + type.id() + " is declared twice");
        }
        return type;
    }

    private static DataResult<RobotConditionType<?>> find(final ResourceLocation id) {
        final RobotConditionType<?> type = BY_ID.get(id);
        return type == null ? DataResult.error(() -> "no robot condition is declared as " + id)
                : DataResult.success(type);
    }
}
