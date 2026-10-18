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
import com.llamalad7.mixinextras.sugar.Local;
import dev.jstech.core.fire.Fires;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Lets the game's fire spread by the air and the wind, and leave what it burnt behind. The game spreads fire to each
 * place round it by odds of its own and eats what burns by its flammability, the same in any air and any wind, and a
 * block it burns away is simply gone. These scale the odds toward each place by the oxygen of the air and the wind
 * toward it, scale how fast it eats by the oxygen, and tell the fire kit of each block burnt away, which leaves its
 * remains there.
 */
@Mixin(FireBlock.class)
public abstract class FireBlockMixin {

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/FireBlock;"
            + "getIgniteOdds(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)I"))
    private int jscore$spreadByAirAndWind(final FireBlock fire, final LevelReader reader, final BlockPos target,
                                          final Operation<Integer> original,
                                          @Local(argsOnly = true) final ServerLevel level,
                                          @Local(argsOnly = true) final BlockPos pos) {
        final int odds = original.call(fire, reader, target);
        return odds <= 0 ? odds : (int) Math.round(odds * Fires.spread(level, pos, target));
    }

    @WrapOperation(method = "checkBurnOut", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/"
            + "state/BlockState;getFlammability(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;"
            + "Lnet/minecraft/core/Direction;)I"))
    private int jscore$burnByAir(final BlockState state, final BlockGetter getter, final BlockPos pos,
                                 final Direction face, final Operation<Integer> original) {
        final int flammability = original.call(state, getter, pos, face);
        return flammability <= 0 || !(getter instanceof ServerLevel level) ? flammability
                : (int) Math.round(flammability * Fires.burnRate(level, pos));
    }

    @WrapOperation(method = "checkBurnOut", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"))
    private boolean jscore$leaveRemains(final Level level, final BlockPos pos, final boolean moving,
                                        final Operation<Boolean> original) {
        final BlockState was = level.getBlockState(pos);
        final boolean removed = original.call(level, pos, moving);
        if (removed && level instanceof ServerLevel server) {
            Fires.burned(server, pos, was, false);
        }
        return removed;
    }

    @WrapOperation(method = "checkBurnOut", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;"
            + "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean jscore$rememberRemains(final Level level, final BlockPos pos, final BlockState fire,
                                           final int flags, final Operation<Boolean> original) {
        final BlockState was = level.getBlockState(pos);
        final boolean set = original.call(level, pos, fire, flags);
        if (set && level instanceof ServerLevel server) {
            Fires.burned(server, pos, was, true);
        }
        return set;
    }
}
