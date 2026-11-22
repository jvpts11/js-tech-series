/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mixin;

import dev.jstech.core.client.moving.SeatLookClient;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The mouse turns a player sitting on a seat of a moving structure that leans or pitches, or walking on its floor in
 * its frame, in the structure's own frame, about its up and across its right, as far up and down as its floor has
 * it; anyone else, as the game turns them.
 */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {

    @Redirect(method = "turnPlayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void jscore$turnInTheSeat(final LocalPlayer player, final double yaw, final double pitch) {
        if (!SeatLookClient.turn(player, yaw, pitch)) {
            player.turn(yaw, pitch);
        }
    }
}
