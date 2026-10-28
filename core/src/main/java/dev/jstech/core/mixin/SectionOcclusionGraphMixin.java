/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.jstech.core.client.sky.GroundCovers;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The game looks for the sections of blocks it draws only within its sight's reach of the eye up or down, so from high
 * over the land the ground falls out of sight; while a mod asks for the sight to reach down ({@link GroundCovers}), a
 * section's height from the eye stops mattering, and every loaded section in sight is drawn.
 */
@Mixin(SectionOcclusionGraph.class)
public abstract class SectionOcclusionGraphMixin {

    @WrapOperation(method = "getRelativeFrom", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;abs(I)I"))
    private int jscore$sightReachingDown(final int apart, final Operation<Integer> original) {
        return GroundCovers.reachingDown() ? 0 : original.call(apart);
    }
}
