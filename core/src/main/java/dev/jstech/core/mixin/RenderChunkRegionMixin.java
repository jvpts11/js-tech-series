/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mixin;

import dev.jstech.core.client.moving.StructureMeshes;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets a moving structure's section be built with the faces the game shades left unshaded: the game shades a face by
 * the side it faces where it is built, out in the yard, and a structure's faces face elsewhere as it turns, so the
 * structure shades them itself ({@link StructureMeshes}). Only a section the structure builds on its own thread, and
 * only once of the two times it builds it, is built so; every other section is shaded as before.
 */
@Mixin(RenderChunkRegion.class)
public abstract class RenderChunkRegionMixin {

    @Inject(method = "getShade(Lnet/minecraft/core/Direction;Z)F", at = @At("HEAD"), cancellable = true)
    private void jscore$shadeStructuresLater(final Direction direction, final boolean shade,
                                             final CallbackInfoReturnable<Float> info) {
        if (shade && StructureMeshes.shadingLater()) {
            info.setReturnValue(1.0F);
        }
    }

    // The way NeoForge's own lighting asks, by the face's normal.
    @Inject(method = "getShade(FFFZ)F", at = @At("HEAD"), cancellable = true)
    private void jscore$shadeStructuresLaterByNormal(final float normalX, final float normalY, final float normalZ,
                                                     final boolean shade, final CallbackInfoReturnable<Float> info) {
        if (shade && StructureMeshes.shadingLater()) {
            info.setReturnValue(1.0F);
        }
    }
}
