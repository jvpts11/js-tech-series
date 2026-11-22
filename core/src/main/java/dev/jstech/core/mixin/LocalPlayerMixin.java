/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mixin;

import dev.jstech.core.client.moving.SeatLookClient;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The view of a player sitting on a seat of a moving structure pitches smoothly between ticks: the structure pitching
 * them turns their look once a tick, and the game, which draws a rider's yaw between ticks, draws the player's own
 * pitch only where the last tick left it, a step every tick. Anyone else is drawn as the game draws them.
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    @Inject(method = "getViewXRot", at = @At("HEAD"), cancellable = true)
    private void jscore$pitchBetweenTicksInTheSeat(final float partialTick, final CallbackInfoReturnable<Float> pitch) {
        final LocalPlayer self = (LocalPlayer) (Object) this;
        if (SeatLookClient.seated(self)) {
            pitch.setReturnValue(Mth.lerp(partialTick, self.xRotO, self.getXRot()));
        }
    }
}
