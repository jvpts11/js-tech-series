/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mixin;

import dev.jstech.core.moving.StructureRays;
import dev.jstech.core.multiblock.shape.FormedAim;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets arrows and every other ray hit the blocks of moving structures: the game follows a ray through the world's
 * blocks alone, and a structure's blocks are out in the yard. A ray that passes a structure is followed through it too
 * ({@link StructureRays}), and hits its block when that is nearer. A ray with no structure near hits as before. A ray
 * that finds a block of a formed structure whose model is known is followed again against the model's faces
 * ({@link FormedAim}).
 */
@Mixin(BlockGetter.class)
public interface BlockGetterMixin {

    @Inject(method = "clip", at = @At("RETURN"), cancellable = true)
    private void jscore$hitStructures(final ClipContext context, final CallbackInfoReturnable<BlockHitResult> info) {
        if (this instanceof Level level) {
            final BlockHitResult faced = FormedAim.clip(level, context, info.getReturnValue());
            if (faced != null) {
                info.setReturnValue(faced);
            }
            final BlockHitResult nearer = StructureRays.clip(level, context, info.getReturnValue());
            if (nearer != null) {
                info.setReturnValue(nearer);
            }
        }
    }
}
