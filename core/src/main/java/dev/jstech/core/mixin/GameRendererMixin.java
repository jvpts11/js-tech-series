/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mixin;

import dev.jstech.core.client.content.StandingPartsAim;
import dev.jstech.core.client.moving.StructureAim;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets the crosshair land on a part standing out of a block, such as an open drive tray or a stick in a port: the
 * game aims only at a block's cube, so once it has aimed for the frame the parts are looked at too. And an aim at a
 * block of a moving structure names the place on that block in the structure's own places, as the server checks a
 * click. The game offers no event between its aim and the frame, so this is the one place it can be done.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "pick(F)V", at = @At("TAIL"))
    private void jscore$aimAtStandingParts(final float partialTicks, final CallbackInfo info) {
        StructureAim.afterPick(minecraft);
        StandingPartsAim.afterPick(minecraft, partialTicks);
    }
}
