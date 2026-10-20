/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mixin;

import dev.jstech.core.client.moving.ClientMovingStructures;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tells a moving structure's hand-off when the game begins to build a section of the world, and when a build is
 * drawn: the game builds a section from its blocks as they are when the build begins, lets go of any build begun
 * before it, and draws the section anew when a build is put on the graphics card. The first section drawn after a
 * build begun once the blocks changed hands is the one with them, so the structure is drawn until that frame and not
 * one frame less or more. Every section is built as before.
 */
@Mixin(SectionRenderDispatcher.RenderSection.class)
public abstract class RenderSectionMixin {

    @Shadow
    public abstract BlockPos getOrigin();

    @Inject(method = "createCompileTask", at = @At("RETURN"))
    private void jscore$noteBuildBegun(final RenderRegionCache cache, final CallbackInfoReturnable<?> info) {
        ClientMovingStructures.buildBegun(SectionPos.asLong(getOrigin()));
    }

    // On a thread of the game's builders for an empty section, on the render thread for any other.
    @Inject(method = "setCompiled", at = @At("HEAD"))
    private void jscore$noteBuilt(final SectionRenderDispatcher.CompiledSection compiled, final CallbackInfo info) {
        ClientMovingStructures.built(SectionPos.asLong(getOrigin()));
    }
}
