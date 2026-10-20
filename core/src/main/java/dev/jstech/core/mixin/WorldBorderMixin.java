/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mixin;

import dev.jstech.core.moving.YardLayout;
import net.minecraft.world.level.border.WorldBorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps the yard inside the world's border, however small a server sets it: the game stops block entities ticking and
 * players using blocks outside the border, and a structure's blocks are out in the yard while the structure itself is
 * inside the border. The border still holds everything else as before.
 */
@Mixin(WorldBorder.class)
public abstract class WorldBorderMixin {

    @Inject(method = "isWithinBounds(DDD)Z", at = @At("HEAD"), cancellable = true)
    private void jscore$holdTheYard(final double x, final double z, final double offset,
                                    final CallbackInfoReturnable<Boolean> info) {
        if (YardLayout.isYard(x)) {
            info.setReturnValue(true);
        }
    }

    @Inject(method = "isWithinBounds(DDDD)Z", at = @At("HEAD"), cancellable = true)
    private void jscore$holdTheYardsBoxes(final double minX, final double minZ, final double maxX, final double maxZ,
                                          final CallbackInfoReturnable<Boolean> info) {
        if (YardLayout.isYard(minX)) {
            info.setReturnValue(true);
        }
    }
}
