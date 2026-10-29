/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mixin;

import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * How long a furnace's fuel still burns, which the game keeps to itself: a furnace where the air runs out of oxygen
 * has its flame put out, its fuel done at its next tick.
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public interface AbstractFurnaceBlockEntityAccessor {

    /** The ticks its fuel still burns. */
    @Accessor("litTime")
    int jscore$litTime();

    /** Sets the ticks its fuel still burns. */
    @Accessor("litTime")
    void jscore$setLitTime(int ticks);
}
