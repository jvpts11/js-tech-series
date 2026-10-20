/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mixin;

import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.core.util.Loaded;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets the rain fall on a moving structure where it is: the game asks whether it rains on a block to put out a fire,
 * wet farmland and fill a cauldron, and a structure's block is out in the yard, under the void's dry sky. It rains on
 * a block of a structure when the block sees the sky past the structure's own blocks and it rains where the block is
 * in the world. Every other block is asked as before.
 */
@Mixin(Level.class)
public abstract class LevelMixin {

    @Inject(method = "isRainingAt", at = @At("HEAD"), cancellable = true)
    private void jscore$rainWhereTheStructureIs(final BlockPos pos, final CallbackInfoReturnable<Boolean> info) {
        if (!YardLayout.isYardBlock(pos.getX())) {
            return;
        }
        final Level level = (Level) (Object) this;
        final BlockPos there = MovingStructures.inWorld(level, pos);
        // Where the land under the structure is not loaded nothing is read of it, and no rain is known to fall.
        info.setReturnValue(!YardLayout.isYardBlock(there.getX()) && level.isRaining() && level.canSeeSky(pos)
                && Loaded.at(level, there) && level.isRainingAt(there));
    }
}
