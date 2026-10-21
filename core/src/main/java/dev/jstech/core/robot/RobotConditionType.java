/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.robot;

import com.mojang.serialization.MapCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * A kind of robot condition, by the id its saved conditions carry and the codec that reads and writes them.
 *
 * @param id    what a saved condition of this kind names as its type
 * @param codec how a condition of this kind is saved
 * @param <C>   the condition's class
 */
public record RobotConditionType<C extends IRobotCondition>(ResourceLocation id, MapCodec<C> codec) {
}
