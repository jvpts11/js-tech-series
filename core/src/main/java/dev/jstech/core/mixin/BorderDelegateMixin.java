/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mixin;

import dev.jstech.core.dimension.DimensionBorders;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.border.WorldBorder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Leaves a dimension that keeps its own border ({@link DimensionBorders}) out of the overworld's: the game hands every
 * change of the overworld's border's size and centre on to every other dimension's, with no way to keep one apart.
 * Every other dimension still follows the overworld's border as before.
 */
@Mixin(BorderChangeListener.DelegateBorderChangeListener.class)
public abstract class BorderDelegateMixin {

    @Shadow
    @Final
    private WorldBorder worldBorder;

    @Inject(method = "onBorderSizeSet", at = @At("HEAD"), cancellable = true)
    private void jscore$keepOwnSize(final WorldBorder border, final double size, final CallbackInfo callback) {
        if (DimensionBorders.owns(this.worldBorder)) {
            callback.cancel();
        }
    }

    @Inject(method = "onBorderSizeLerping", at = @At("HEAD"), cancellable = true)
    private void jscore$keepOwnLerp(final WorldBorder border, final double from, final double to, final long time,
                                    final CallbackInfo callback) {
        if (DimensionBorders.owns(this.worldBorder)) {
            callback.cancel();
        }
    }

    @Inject(method = "onBorderCenterSet", at = @At("HEAD"), cancellable = true)
    private void jscore$keepOwnCentre(final WorldBorder border, final double x, final double z,
                                      final CallbackInfo callback) {
        if (DimensionBorders.owns(this.worldBorder)) {
            callback.cancel();
        }
    }
}
