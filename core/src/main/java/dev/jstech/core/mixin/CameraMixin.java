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
import dev.jstech.core.client.moving.DeckWalk;
import net.minecraft.client.Camera;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The camera of a player walking on a structure's floor in its frame stands at their eyes over their feet along the
 * floor's up ({@link DeckWalk}): the game sets it up the world from the feet, by the eyes' height eased between ticks,
 * which on a floor rolled on its side is beside the head, at the floor. Anyone else's camera is the game's own.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {

    @WrapOperation(method = "setup", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Camera;setPosition(DDD)V"))
    private void jscore$eyesAlongTheFloorsUp(final Camera camera, final double x, final double y, final double z,
                                             final Operation<Void> original,
                                             @Local(argsOnly = true) final Entity entity,
                                             @Local(argsOnly = true) final float partialTick) {
        if (entity instanceof LocalPlayer player) {
            // The eyes' height the game eased between ticks is what it laid over the feet it eased the same way.
            final double feet = player.yo + (player.getY() - player.yo) * partialTick;
            final Vec3 eye = DeckWalk.eye(player, partialTick, y - feet);
            if (eye != null) {
                original.call(camera, eye.x, eye.y, eye.z);
                return;
            }
        }
        original.call(camera, x, y, z);
    }
}
