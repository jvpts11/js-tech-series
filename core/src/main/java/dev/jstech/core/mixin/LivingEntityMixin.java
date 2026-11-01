/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mixin;

import dev.jstech.core.dimension.DimensionRulesData;
import dev.jstech.core.dimension.IWalkPace;
import dev.jstech.core.moving.StructureSeatEntity;
import dev.jstech.core.moving.StructureSleeping;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets an entity sleep in a bed of a moving structure: the game lays a sleeper down in their bed, and stands them up
 * beside it, by the bed's own place, out in the yard, so a player sleeping on a ship would find themselves far out in
 * the empty yard. A bed of a structure is where it is in the world; any other bed as before. One lying on a seat of a
 * structure, as a capsule's crew lie on their couches, lies the way a sleeper does, their head the way the seat says.
 * And a creature's walk swings at the pace the pull where it stands sets ({@link IWalkPace}).
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    /* Before its walk is worked out, it takes its pace from the pull where the creature stands. */
    @Inject(method = "calculateEntityAnimation", at = @At("HEAD"))
    private void jscore$walkAtThePull(final boolean includeHeight, final CallbackInfo info) {
        final LivingEntity self = (LivingEntity) (Object) this;
        ((IWalkPace) self.walkAnimation).jscore$pace(IWalkPace.of(DimensionRulesData.of(self.level())
                .gravityAt(self.getY())));
    }

    @Inject(method = "getBedOrientation", at = @At("HEAD"), cancellable = true)
    private void jscore$headOnACouchAboard(final CallbackInfoReturnable<Direction> info) {
        if (((LivingEntity) (Object) this).getVehicle() instanceof StructureSeatEntity seat) {
            final Direction head = seat.lyingHead();
            if (head != null) {
                info.setReturnValue(head);
            }
        }
    }

    @Inject(method = "setPosToBed", at = @At("TAIL"))
    private void jscore$lieInABedAboard(final BlockPos bed, final CallbackInfo info) {
        StructureSleeping.laidDown((LivingEntity) (Object) this);
    }

    @Inject(method = "stopSleeping", at = @At("TAIL"))
    private void jscore$standUpBesideABedAboard(final CallbackInfo info) {
        StructureSleeping.stoodUp((LivingEntity) (Object) this);
    }
}
