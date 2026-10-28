/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * How far a living thing's swing has come back since it last struck, which the game keeps to itself: a player made
 * anew in a seamless passage between dimensions takes the old one's, so their hand does not drop as if they had just
 * swung.
 */
@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {

    /** The ticks since the last swing, up to the swing's whole strength. */
    @Accessor("attackStrengthTicker")
    int jscore$attackStrengthTicker();

    /** Sets the ticks since the last swing. */
    @Accessor("attackStrengthTicker")
    void jscore$setAttackStrengthTicker(int ticks);
}
