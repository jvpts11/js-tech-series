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
 * A kind of robot task, by the id its saved tasks carry and the codec that reads and writes them.
 *
 * @param id    what a saved task of this kind names as its type
 * @param codec how a task of this kind is saved
 * @param <T>   the task's class
 */
public record RobotTaskType<T extends IRobotTask>(ResourceLocation id, MapCodec<T> codec) {
}
