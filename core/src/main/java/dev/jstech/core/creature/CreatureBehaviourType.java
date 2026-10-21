/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.creature;

import com.mojang.serialization.MapCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * A kind of creature behaviour, by the id its data names as its type and the codec that reads and writes it.
 *
 * @param id    what a behaviour of this kind names as its type
 * @param codec how a behaviour of this kind is read and written
 * @param <B>   the behaviour's class
 */
public record CreatureBehaviourType<B extends ICreatureBehaviour>(ResourceLocation id, MapCodec<B> codec) {
}
