/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mixin;

import dev.jstech.core.moving.StructureCollisions;
import dev.jstech.core.moving.StructureFooting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets entities collide with moving structures and stand on them: the game moves an entity against the world's blocks
 * and the boxes of the few entities one collides with, and offers no way to add others. The move the world allowed is
 * held to the blocks of the structures near, turned as they are ({@link StructureCollisions}); and an entity standing
 * on a structure has the block under it read from there, so it slides on a structure's ice and is hurt by its magma.
 * With no structure near, the move is the game's own.
 */
@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "collide", at = @At("RETURN"), cancellable = true)
    private void jscore$collideWithStructures(final Vec3 wanted, final CallbackInfoReturnable<Vec3> info) {
        info.setReturnValue(StructureCollisions.collide((Entity) (Object) this, wanted, info.getReturnValue()));
    }

    @Inject(method = "getOnPos(F)Lnet/minecraft/core/BlockPos;", at = @At("HEAD"), cancellable = true)
    private void jscore$standOnStructures(final float below, final CallbackInfoReturnable<BlockPos> info) {
        final BlockPos support = StructureFooting.support((Entity) (Object) this);
        if (support != null) {
            info.setReturnValue(support);
        }
    }
}
