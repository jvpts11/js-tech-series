/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.module;

import com.mojang.serialization.MapCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * A kind of module effect, by the id its data names as its type and the codec that reads and writes it.
 *
 * @param id    what an effect of this kind names as its type
 * @param codec how an effect of this kind is read and written
 * @param <E>   the effect's class
 */
public record ModuleEffectType<E extends IModuleEffect>(ResourceLocation id, MapCodec<E> codec) {
}
