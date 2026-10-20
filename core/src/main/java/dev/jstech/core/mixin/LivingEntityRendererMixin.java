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
import com.mojang.blaze3d.vertex.PoseStack;
import dev.jstech.core.client.moving.SleepersDrawn;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Lays a sleeper in a bed of a moving structure along the bed as it is drawn: the game turns a sleeper by the way
 * their bed looks, and moves them along it to its pillow, both by the bed's way in the yard; a structure turned has
 * its beds turned with it. Any other sleeper is drawn as before.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @WrapOperation(method = "setupRotations", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;sleepDirectionToRotation"
                    + "(Lnet/minecraft/core/Direction;)F"))
    private float jscore$turnWithTheBedAboard(final Direction facing, final Operation<Float> original,
                                              @Local(argsOnly = true) final LivingEntity sleeper,
                                              @Local(argsOnly = true, ordinal = 2) final float partialTick) {
        return original.call(facing) + SleepersDrawn.turnOf(sleeper, partialTick);
    }

    @WrapOperation(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V", ordinal = 0))
    private void jscore$toThePillowAboard(final PoseStack pose, final float x, final float y, final float z,
                                          final Operation<Void> original,
                                          @Local(argsOnly = true) final LivingEntity sleeper,
                                          @Local(argsOnly = true, ordinal = 1) final float partialTick) {
        final double turn = Math.toRadians(SleepersDrawn.turnOf(sleeper, partialTick));
        if (turn == 0) {
            original.call(pose, x, y, z);
            return;
        }
        // The game's way of a yaw is east less its sine, south its cosine: turned further, so is the way to the pillow.
        final double cos = Math.cos(turn);
        final double sin = Math.sin(turn);
        original.call(pose, (float) (x * cos - z * sin), y, (float) (x * sin + z * cos));
    }
}
