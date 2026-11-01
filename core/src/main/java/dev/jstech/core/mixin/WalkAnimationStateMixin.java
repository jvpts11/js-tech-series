/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mixin;

import dev.jstech.core.dimension.IWalkPace;
import net.minecraft.world.entity.WalkAnimationState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A walk swinging at its pace ({@link IWalkPace}): each tick the swing goes on by the walk's speed times its pace, and
 * between two ticks it is drawn on the same way, so the limbs move smoothly at the slower pace; how wide they swing,
 * the walk's speed, is the game's own.
 */
@Mixin(WalkAnimationState.class)
public abstract class WalkAnimationStateMixin implements IWalkPace {

    @Shadow
    private float speed;
    @Shadow
    private float position;
    @Unique
    private float jscore$pace = 1F;

    @Override
    public void jscore$pace(final float pace) {
        this.jscore$pace = pace;
    }

    /* The game went on by the walk's speed; it goes on by its speed times its pace. */
    @Inject(method = "update", at = @At("TAIL"))
    private void jscore$goOnAtThePace(final float newSpeed, final float partialTick, final CallbackInfo info) {
        this.position += this.speed * (this.jscore$pace - 1F);
    }

    /* Between two ticks the swing is where it was plus the share of the tick gone of the step it took. */
    @Inject(method = "position(F)F", at = @At("HEAD"), cancellable = true)
    private void jscore$drawnAtThePace(final float partialTick, final CallbackInfoReturnable<Float> info) {
        info.setReturnValue(this.position - this.speed * this.jscore$pace * (1F - partialTick));
    }
}
