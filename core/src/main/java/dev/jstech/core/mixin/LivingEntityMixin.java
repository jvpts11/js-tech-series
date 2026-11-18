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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
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
 * A mod's bed without the game's FACING stands its sleeper up by its own way. And a creature's walk swings at the
 * pace the pull where it stands sets ({@link IWalkPace}).
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    /*
     * Got up from a bed with nowhere to stand round it, as the game stands a sleeper: on its block's middle, a tenth
     * over the block above; and the game's quarter turn from a direction's angle to a heading.
     */
    private static final double HALF = 0.5;
    private static final double OVER_IT = 0.1;
    private static final double QUARTER_TURN = 90;

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

    /*
     * NeoForge lets a sleeper lie down in a mod's bed that has no FACING, as a ship's bunk, but the game stands them
     * up from a bed by that property alone and fails without it: from such a bed they stand up the game's way, by the
     * way the bed gives ({@code getBedDirection}), and beside it where it is in the world when it is aboard.
     */
    @Inject(method = "stopSleeping", at = @At("HEAD"), cancellable = true)
    private void jscore$standUpFromABedWithoutFacing(final CallbackInfo info) {
        final LivingEntity self = (LivingEntity) (Object) this;
        final Level level = self.level();
        final BlockPos bed = self.getSleepingPos().filter(level::hasChunkAt).orElse(null);
        if (bed == null) {
            return;
        }
        final BlockState state = level.getBlockState(bed);
        if (!state.isBed(level, bed, self) || state.hasProperty(BedBlock.FACING)) {
            return;
        }
        final Direction way = state.getBedDirection(level, bed);
        state.setBedOccupied(level, bed, self, false);
        final Vec3 stand = BedBlock.findStandUpPosition(self.getType(), level, bed, way, self.getYRot())
                .orElseGet(() -> new Vec3(bed.getX() + HALF, bed.getY() + 1 + OVER_IT, bed.getZ() + HALF));
        final Vec3 toward = Vec3.atBottomCenterOf(bed).subtract(stand).normalize();
        self.setPos(stand.x, stand.y, stand.z);
        self.setYRot((float) Mth.wrapDegrees(Mth.atan2(toward.z, toward.x) * Mth.RAD_TO_DEG - QUARTER_TURN));
        self.setXRot(0);
        self.setPose(Pose.STANDING);
        self.setPos(stand.x, stand.y, stand.z);
        self.clearSleepingPos();
        StructureSleeping.stoodUp(self);
        info.cancel();
    }

    @Inject(method = "stopSleeping", at = @At("TAIL"))
    private void jscore$standUpBesideABedAboard(final CallbackInfo info) {
        StructureSleeping.stoodUp((LivingEntity) (Object) this);
    }
}
