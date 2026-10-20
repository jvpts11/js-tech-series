/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mixin;

import dev.jstech.core.moving.StructureCollisions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets the game see a moving structure's blocks when it asks whether a place is free: a player sneaking at a deck's
 * edge, a mob looking for room, an item checking where it may go. The game answers from the world's blocks alone; a
 * place it finds free is free only if no structure's block fills it too. A level with no structure answers as before.
 */
@Mixin(CollisionGetter.class)
public interface CollisionGetterMixin {

    @Inject(method = "noCollision(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Z",
            at = @At("RETURN"), cancellable = true)
    private void jscore$seeStructuresInTheWay(@Nullable final Entity entity, final AABB box,
                                             final CallbackInfoReturnable<Boolean> info) {
        if (info.getReturnValueZ() && this instanceof Level level && StructureCollisions.overlaps(level, entity, box)) {
            info.setReturnValue(false);
        }
    }
}
