/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mixin;

import dev.jstech.core.moving.StructureSleeping;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets an entity sleep in a bed of a moving structure: the game lays a sleeper down in their bed, and stands them up
 * beside it, by the bed's own place, out in the yard, so a player sleeping on a ship would find themselves far out in
 * the empty yard. A bed of a structure is where it is in the world; any other bed as before.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "setPosToBed", at = @At("TAIL"))
    private void jscore$lieInABedAboard(final BlockPos bed, final CallbackInfo info) {
        StructureSleeping.laidDown((LivingEntity) (Object) this);
    }

    @Inject(method = "stopSleeping", at = @At("TAIL"))
    private void jscore$standUpBesideABedAboard(final CallbackInfo info) {
        StructureSleeping.stoodUp((LivingEntity) (Object) this);
    }
}
