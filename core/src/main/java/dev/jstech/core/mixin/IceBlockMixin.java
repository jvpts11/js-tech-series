/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.jstech.core.heat.Thawing;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.IceBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The game's ice, melting by a torch or broken, becomes water only where water can stay liquid ({@link Thawing}): in
 * a vacuum, or in air as thin as Mars', it turns straight to vapour and is gone, as the game already has it do in the
 * Nether's heat. Where the air is thick, as on the Earth, it melts as before.
 */
@Mixin(IceBlock.class)
public abstract class IceBlockMixin {

    @ModifyExpressionValue(method = {"melt", "playerDestroy"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/dimension/DimensionType;ultraWarm()Z"))
    private boolean jscore$vapourInThinAir(final boolean ultraWarm, @Local(argsOnly = true) final Level level,
                                           @Local(argsOnly = true) final BlockPos pos) {
        return ultraWarm || !Thawing.meltsToWater(level, pos);
    }
}
